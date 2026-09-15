/*
 * Copyright (C) 2017-2024 HERE Europe B.V.
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
package com.here.naksha.lib.hub.storages;

import com.here.naksha.lib.core.EventPipeline;
import com.here.naksha.lib.core.IEventHandler;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.models.naksha.Space;
import com.here.naksha.lib.core.models.naksha.SpaceProperties;
import com.here.naksha.lib.hub.EventPipelineFactory;
import naksha.model.*;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaTx;
import naksha.model.request.ErrorResponse;
import naksha.model.request.Request;
import naksha.model.request.Response;
import naksha.model.request.SuccessResponse;
import naksha.model.request.Write;
import naksha.model.request.WriteOp;
import naksha.model.request.WriteRequest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.here.naksha.lib.core.HubInternalIdentifiers.SPACES;
import static com.here.naksha.lib.handlers.util.RequestTypesUtil.isOnlyWriteCollections;
import static com.here.naksha.lib.handlers.util.RequestTypesUtil.isOnlyWriteFeatures;
import static naksha.base.NakshaError.UNSUPPORTED_OPERATION;

public class NHSpaceStorageWriter extends NHSpaceStorageReader implements IWriteSession {

  private static final NakshaException NOT_SUPPORTED_ERROR = new NakshaException(
      new NakshaError(UNSUPPORTED_OPERATION, "Operation not supported by NHSpaceStorageWriter"));

  private static final Logger logger = LoggerFactory.getLogger(NHSpaceStorageWriter.class);

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  public NHSpaceStorageWriter(
      final @NotNull INaksha hub,
      final @NotNull Map<String, List<IEventHandler>> virtualSpaces,
      final @NotNull EventPipelineFactory pipelineFactory,
      final @Nullable SessionOptions sessionOptions) {
    super(hub, virtualSpaces, pipelineFactory, sessionOptions);
  }

  @Override
  public @NotNull Response executeWrite(@NotNull WriteRequest request) {
    if (isOnlyWriteCollections(request)) {
      return executeSingleCollectionWrite(request);
    } else if (isOnlyWriteFeatures(request)) {
      return executeWriteFeatures(request);
    } else {
      return new ErrorResponse(
        UNSUPPORTED_OPERATION,
        "Only single collection writes and pure write features writes are supported"
      );
    }
  }

  private @NotNull Response executeSingleCollectionWrite(final @NotNull WriteRequest writeRequest) {
    List<Write> collectionWrites = writeRequest.getWrites();
    if (collectionWrites.size() != 1) {
      throw new IllegalArgumentException(
          "Currently supporting WriteRequest for single collection only, got multiple: " + collectionWrites.size());
    }
    return executeSingleCollectionWrite(writeRequest, collectionWrites.get(0).getCollectionId());
  }

  private @NotNull Response executeSingleCollectionWrite(
      final @NotNull WriteRequest writeRequest, final @NotNull String spaceId) {
    if (virtualSpaces.containsKey(spaceId)) {
      logger.info("Single collection write request for {}, against Admin storage.", spaceId);
      return executeWriteToAdminSpaces(writeRequest, spaceId);
    } else {
      logger.info("Single collection write request {}, against Custom storage.", spaceId);
      return executeWriteToCustomSpaces(writeRequest, spaceId);
    }
  }

  private @NotNull Response executeWriteFeatures(final @NotNull WriteRequest writeRequest) {
    final String spaceId = singleCollectionIdFrom(writeRequest);
    logger.info("WriteRequest with writes against spaceId={}", spaceId);
    addSpaceIdToStreamInfo(spaceId);
    if (isDeleteSpaceRequest(writeRequest, spaceId)) {
      return executeDeleteSpace(writeRequest);
    } else if (isUpdateSpaceRequest(writeRequest, spaceId)) {
      return executeUpdateSpace(writeRequest);
    } else if (virtualSpaces.containsKey(spaceId)) {
      // Request is to write to Naksha Admin space
      return executeWriteToAdminSpaces(writeRequest, spaceId);
    } else {
      // Request is to write to Custom space
      return executeWriteToCustomSpaces(writeRequest, spaceId);
    }
  }

  private @NotNull Response executeWriteToAdminSpaces(@NotNull WriteRequest writeRequest, @NotNull String spaceId) {
    // Run pipeline against virtual space
    final EventPipeline pipeline = pipelineFactory.eventPipeline();
    final Response result = setupEventPipelineForAdminVirtualSpace(spaceId, pipeline);
    if (!(result instanceof SuccessResponse)) {
      return result;
    }
    return pipeline.sendEvent(writeRequest);
  }

  private @NotNull Response executeWriteToCustomSpaces(
      final @NotNull WriteRequest writeRequest, @NotNull String spaceId) {
    final EventPipeline eventPipeline = pipelineFactory.eventPipeline();
    final Response result = setupEventPipelineForSpaceId(spaceId, eventPipeline);
    if (!(result instanceof SuccessResponse)) {
      return result;
    }
    return eventPipeline.sendEvent(writeRequest);
  }

  private boolean isDeleteSpaceRequest(@NotNull WriteRequest writeRequest, @NotNull String spaceId) {
    if (SPACES.equals(spaceId)) {
      List<Write> writes = writeRequest.getWrites();
      if (writes.size() == 1) {
        Write write = writes.get(0);
        return WriteOp.DELETE.equals(write.getOp());
      }
    }
    return false;
  }

  private @NotNull Response executeDeleteSpace(@NotNull WriteRequest deleteSpaceEntryReq) {
    Write originalWrite = deleteSpaceEntryReq.getWrites().get(0);
    String spaceId = originalWrite.getId();
    WriteRequest deleteCollectionReq = new WriteRequest()
            .add(new Write().deleteCollection(new NakshaCollection(spaceId), false));
    Response deleteCollectionRes = executeSingleCollectionWrite(deleteCollectionReq, spaceId);
    if (deleteCollectionRes instanceof SuccessResponse) {
      return executeWriteToAdminSpaces(deleteSpaceEntryReq, originalWrite.getCollectionId());
    } else {
      return deleteCollectionRes;
    }
  }

  private boolean isUpdateSpaceRequest(@NotNull WriteRequest writeRequest, @NotNull String spaceId) {
    List<Write> writes = writeRequest.getWrites();
    return SPACES.equals(spaceId)
           && writes.size() == 1
           && WriteOp.UPDATE.equals(writes.get(0).getOp());
  }

  private @NotNull Response executeUpdateSpace(@NotNull WriteRequest updateSpaceEntryReq) {
    final Space space = ((Space) updateSpaceEntryReq.getWrites().get(0).getFeature());
    final SpaceProperties spaceProperties = space.getProperties();
    final NakshaCollection collection = spaceProperties.getCollection();
    Response upsertSpaceRes = null;
    if (collection != null) {
      // submit Update Collection request to Custom Space based pipeline
      WriteRequest upsertCollectionReq = new WriteRequest().add(new Write().upsertCollection(collection));
      upsertSpaceRes = executeSingleCollectionWrite(upsertCollectionReq, space.getId());
      if(upsertSpaceRes instanceof SuccessResponse) {
        // we update the space only if the collection was changed correctly
        return executeWriteToAdminSpaces(updateSpaceEntryReq, SPACES);
      }
      // changing collection failed - return failure
      return upsertSpaceRes;
    } else {
      // no collection in Space, we only update the space
      return executeWriteToAdminSpaces(updateSpaceEntryReq, SPACES);
    }
  }

  private String singleCollectionIdFrom(WriteRequest writeRequest) {
    List<String> distinctCollectionIds = writeRequest.getWrites().stream().map(Write::getCollectionId).distinct().toList();
    if (distinctCollectionIds.size() != 1) {
      throw new IllegalArgumentException(
          "Expected Writes of WriteRequest to indicate single collection, got multiple: " + distinctCollectionIds);
    }
    return Objects.requireNonNull(distinctCollectionIds.get(0), "Got empty (null) Write instruction within WriteRequest");
  }

  @Override
  public void commit() {
    // empty on purpose - commits will happen on the pipeline
  }

  @Override
  public void rollback() {
    // empty on purpose - rollbacks will happen on the pipeline
  }

  @Override
  public int getSocketTimeout() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public void setSocketTimeout(int i) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public int getStmtTimeout() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public void setStmtTimeout(int i) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public int getLockTimeout() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public void setLockTimeout(int i) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public boolean isClosed() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @NotNull ILock acquireSessionLock(@NotNull String lockId) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @NotNull ILock acquireTransactionLock(@NotNull String lockId) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @NotNull NakshaTx useTransaction() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @Nullable NakshaTx getTransaction() {
    throw NOT_SUPPORTED_ERROR;
  }

  private final @NotNull MemberProcessorMap processors = new MemberProcessorMap();

  @Override
  public @NotNull MemberProcessorMap getProcessors() {
    return processors;
  }
}
