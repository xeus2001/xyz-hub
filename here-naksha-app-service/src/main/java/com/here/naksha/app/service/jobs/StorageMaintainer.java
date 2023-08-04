/*
 * Copyright (C) 2017-2023 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.here.naksha.app.service.jobs;

import static com.here.naksha.lib.core.exceptions.UncheckedException.unchecked;

import com.here.naksha.app.service.NakshaHubConfig;
import com.here.naksha.app.service.models.MaintenanceTrigger;
import com.here.naksha.lib.core.NakshaAdminCollection;
import com.here.naksha.lib.core.models.features.Connector;
import com.here.naksha.lib.core.models.features.Space;
import com.here.naksha.lib.core.models.features.Storage;
import com.here.naksha.lib.core.storage.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background job thread, responsible for invoking maintenance process on all applicable Naksha storages
 * (AdminDB and SpaceDBs) at pre-configured frequency.
 */
public class StorageMaintainer {

  private static final Logger logger = LoggerFactory.getLogger(StorageMaintainer.class);

  /**
   * NakshaHub configuration to be used by this job (e.g. execution frequency)
   */
  private final @NotNull NakshaHubConfig config;
  /**
   * Default Naksha Admin DB Storage object
   */
  private final @NotNull Storage defStorage;
  /**
   * Naksha AdminDB Storage implementation class, to fetch collection details.
   */
  private final @NotNull IStorage adminStorageImpl;
  /**
   * Transaction Settings to be used while creating new DB connection
   */
  private @NotNull ITransactionSettings txSettings;

  /**
   * Thread pool to manage concurrent execution of Maintenance triggers
   */
  private ThreadPoolExecutor maintTriggerHandlingPool;

  /**
   * Thread pool to manage concurrent execution of Maintenance triggers
   */
  public static final @NotNull String LOCK_KEY_PREFIX = "maintenance-job";

  /**
   * Create a new Storage Maintainer instance, which once initiated, will run as a background job
   * to invoke maintenance on all Naksha associated storages (adminDB and spaceDBs).
   * Ideally should be instantiated only once per Naksha instance.
   *
   * @param config        The NakshaHub configuration to read desired job frequency/interval
   * @param adminStorage  The Naksha AdminDB Storage implementation class, used to fetch collection details
   * @param txSettings    The transaction settings to be used on creating DB connection running maintenance.
   */
  public StorageMaintainer(
      final @NotNull NakshaHubConfig config,
      final @NotNull Storage storage,
      final @NotNull IStorage adminStorage,
      final @NotNull ITransactionSettings txSettings) {
    this.config = config;
    this.defStorage = storage;
    this.adminStorageImpl = adminStorage;
    this.txSettings = txSettings;
  }

  /**
   * The only method, which should be invoked for setting up the job schedule at preconfigured
   * delay and interval. Ideally, this should be invoked while starting Naksha service.
   * This should be invoked only once per Naksha instance.
   * Internally, it will handle the multi-instance concurrency.
   */
  public void scheduleJob() {
    final int initialDelay = config.maintenanceInitialDelayInMins;
    final int subsequentDelay = config.maintenanceIntervalInMins;
    // Schedule maintenance job as per configured frequency (e.g. 12 hours)
    new ScheduledThreadPoolExecutor(1)
        .scheduleWithFixedDelay(this::startJobNow, initialDelay, subsequentDelay, TimeUnit.MINUTES);
    logger.info(
        "Storage Maintenance job is set to start after {} mins with subsequent delay of {} mins.",
        initialDelay,
        subsequentDelay);
  }

  /**
   * This method is internally invoked at every scheduled execution of a job.
   * It prepared list of all spaces across spaceDBs and adminDB for which maintenance is to be triggered.
   * It then distributes the list for processing using a thread pool.
   * It internally creates multi-instance lock to ensure only one instance of job is running
   * (except for an erroneous situation where previously submitted job is stuck for too long and
   * we have to still go ahead and trigger a new one)
   */
  protected void startJobNow() {
    final String lockKey = LOCK_KEY_PREFIX + "-main";

    Thread.currentThread().setName("maintenance");
    logger.info("Storage Maintenance job got triggered.");

    try (final IMasterTransaction tx = adminStorageImpl.openMasterTransaction(txSettings)) {
      // Acquire lock at job level in adminDB (to ensure only 1 job runs across instances)
      if (!tx.acquireLock(lockKey)) {
        logger.debug(
            "Couldn't acquire lock for maintenance job. Some other thread might be processing the same.");
        return;
      }

      // Fetch all Spaces
      final IResultSet<Space> rsp =
          tx.readFeatures(Space.class, NakshaAdminCollection.SPACES).getAll(0, Integer.MAX_VALUE);
      final List<@NotNull Space> spaces = rsp.toList(0, Integer.MAX_VALUE);
      rsp.close();

      // Fetch all Connectors
      final IResultSet<Connector> rc = tx.readFeatures(Connector.class, NakshaAdminCollection.CONNECTORS)
          .getAll(0, Integer.MAX_VALUE);
      final List<@NotNull Connector> connectors = rc.toList(0, Integer.MAX_VALUE);
      final Map<String, Connector> connectorMap =
          connectors.stream().collect(Collectors.toMap(Connector::getId, Function.identity()));
      rc.close();

      // Fetch all Storages
      final IResultSet<Storage> rst = tx.readFeatures(Storage.class, NakshaAdminCollection.STORAGES)
          .getAll(0, Integer.MAX_VALUE);
      final List<@NotNull Storage> storages = rst.toList(0, Integer.MAX_VALUE);
      final Map<String, Storage> storageMap =
          storages.stream().collect(Collectors.toMap(Storage::getId, Function.identity()));
      rst.close();

      /* Convert all spaces into Map of <triggerKey, MaintenanceTrigger>, but only for those connectors
       * that have associated storages.
       * TriggerKey is (likely the connectorId) to consolidate maintenance of all related resources under one thread.
       * We will also include adminDB collections as part of maintenance job (apart from spaceDB collections).
       */
      final Map<String, MaintenanceTrigger> triggerMap = new HashMap<>();
      // iterate through all existing spaces
      for (final Space space : spaces) {
        for (final String connectorId : space.getConnectorIds()) {
          // find associated Connector by Id
          final Connector connector = connectorMap.get(connectorId);
          if (connector == null || !connector.isActive() || connector.getStorageId() == null) {
            // this connector is removed/disabled or not associated with storageId (so, no need to run
            // maintenance)
            continue;
          }
          // find associated Storage by Id
          final Storage storage = storageMap.get(connector.getStorageId());
          if (storage == null) {
            // this storage is removed (for some reason). so, can't run maintenance
            continue;
          }
          // finally, add this space as eligible candidate in maintenance map
          final String triggerKey = connectorId;
          final CollectionInfo collectionInfo =
              new CollectionInfo(space.getCollectionId(), storage.getNumber());
          MaintenanceTrigger trigger = triggerMap.get(triggerKey);
          if (trigger == null) {
            trigger = new MaintenanceTrigger(
                triggerKey, connector, storage, null, new ArrayList<>(List.of(collectionInfo)));
            triggerMap.put(triggerKey, trigger);
          } else {
            trigger.collectionInfoList().add(collectionInfo);
          }
        }
      }
      // add Admin DB collections also for maintenance
      final MaintenanceTrigger trigger = new MaintenanceTrigger(
          "naksha-admin-db", null, defStorage, adminStorageImpl, NakshaAdminCollection.COLLECTION_INFO_LIST);
      triggerMap.put(trigger.key(), trigger);

      /*
       * distribute all Maintenance Trigger events to a thread pool for processing
       */
      distributeMaintenanceTriggerProcessing(triggerMap);

      // Release lock at job level
      if (!tx.releaseLock(lockKey)) {
        logger.warn(
            "Couldn't release lock for maintenance job. If problem persist, it might need manual intervention.");
      }
      tx.commit();
    } catch (Throwable t) {
      logger.error("NAKSHA_ERR_MAINT_JOB - Exception running maintenance job. ", t);
      throw unchecked(t);
    }

    logger.info("Storage Maintenance job got completed.");
  }

  /**
   * Blocking function which distributes maintenance triggers to a thread pool and waits for all threads to complete
   *
   * @param  triggerMap     The map of all maintenance triggers to be processed
   */
  private void distributeMaintenanceTriggerProcessing(final Map<String, MaintenanceTrigger> triggerMap)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (maintTriggerHandlingPool == null) {
      // create thread pool (if doesn't exist already)
      maintTriggerHandlingPool = new ThreadPoolExecutor(
          config.maintenancePoolCoreSize,
          config.maintenancePoolMaxSize,
          60,
          TimeUnit.SECONDS,
          new SynchronousQueue<>(), // queue with zero capacity
          new ThreadPoolExecutor
              .CallerRunsPolicy()); // on reaching queue limit, caller thread itself is used for execution
    }

    // distribute to thread pool
    final List<Future> fList = new ArrayList<>(triggerMap.size());
    for (final MaintenanceTrigger trigger : triggerMap.values()) {
      final Future f = maintTriggerHandlingPool.submit(() -> {
        triggerMaintenance(trigger);
      });
      fList.add(f);
    }

    // wait for all jobs to complete processing (with max timeout)
    for (final Future f : fList) {
      f.get(10, TimeUnit.MINUTES);
    }
  }

  /**
   * This method is internally invoked in parallel against individual spaceDBs.
   * It instantiates appropriate storage implementation class and triggers maintenance() method
   * by passing list of collections to run maintenance for.
   *
   * @param trigger       The Maintenance Trigger details like target storage, spaces
   */
  protected void triggerMaintenance(final @NotNull MaintenanceTrigger trigger) {
    final String lockKey = LOCK_KEY_PREFIX + "-" + trigger.key();
    final String storageId = trigger.storage().getId();

    Thread.currentThread().setName("maintenance-" + trigger.key());

    logger.info("Storage Maintenance job got triggered for storageId {}.", storageId);

    try (final IMasterTransaction tx = adminStorageImpl.openMasterTransaction(this.txSettings)) {
      // Acquire lock at TriggerKey level in adminDB (to ensure only 1 trigger runs across instances)
      if (!tx.acquireLock(lockKey)) {
        logger.warn(
            "Couldn't acquire lock for maintenance job for storageId {}. Some other thread might be processing the same.",
            storageId);
        return;
      }

      // Invoke maintenance() using appropriate storage implementation class, by passing list of collections
      IStorage storageImpl = null;
      if (trigger.connector() != null) {
        // applicable when connector provided DBConfig is required for IStorage instantiation
        storageImpl = trigger.connector().newStorageImpl(trigger.storage());
      } else if (trigger.storageImpl() != null) {
        // applicable when IStorage implementation is already available (e.g. AdminDB)
        storageImpl = trigger.storageImpl();
      } else {
        // fallback to default instantiation (may be applicable for custom storages)
        storageImpl = trigger.storage().newInstance();
      }
      // TODO HP_QUERY : How do we mention where is naksha_tx table, while triggering maintenance?
      storageImpl.maintain(trigger.collectionInfoList());

      // Release lock at TriggerKey level
      if (!tx.releaseLock(lockKey)) {
        logger.warn(
            "Couldn't release lock for maintenance job for storageId {}. If problem persist, it might need manual intervention.",
            storageId);
      }

      tx.commit();
    } catch (Throwable t) {
      logger.error("NAKSHA_ERR_MAINT_JOB - Exception running maintenance job for storageId {}. ", storageId, t);
      throw unchecked(t);
    }

    logger.info("Storage Maintenance job got completed for storageId {}.", storageId);
  }
}
