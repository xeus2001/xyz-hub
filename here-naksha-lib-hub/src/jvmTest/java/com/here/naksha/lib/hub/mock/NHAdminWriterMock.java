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
package com.here.naksha.lib.hub.mock;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import naksha.model.ILock;
import naksha.model.IWriteSession;
import naksha.model.MemberProcessorMap;
import naksha.model.Naksha;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.objects.NakshaFeature;
import naksha.model.objects.NakshaTx;
import naksha.model.request.ErrorResponse;
import naksha.model.request.Request;
import naksha.model.request.Response;
import naksha.model.request.SuccessResponse;
import naksha.model.request.Write;
import naksha.model.request.WriteOp;
import naksha.model.request.WriteRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHAdminWriterMock extends NHAdminReaderMock implements IWriteSession {

  public NHAdminWriterMock(final @NotNull Map<String, TreeMap<String, NakshaFeature>> mockCollection) {
    super(mockCollection);
  }

  @Override
  public @NotNull Response executeWrite(@NotNull WriteRequest request) {
    for (Write write : request.getWrites()) {
      Response singularResponse;
      if (Naksha.COLLECTIONS_COL_ID.equals(write.getCollectionId())) {
        singularResponse = executeWriteCollection(write);
      } else {
        singularResponse = executeWriteFeature(write);
      }
      if (singularResponse instanceof ErrorResponse){
        return singularResponse;
      }
    }
    return new SuccessResponse();
  }

  private Response executeWriteCollection(Write write) {
    String collectionId = write.getId();
    WriteOp op = write.getOp();
    if (op.equals(WriteOp.CREATE)) {
      mockCollection.putIfAbsent(collectionId, new TreeMap<>());
      return new SuccessResponse();
    } else if (op.equals(WriteOp.DELETE)) {
      mockCollection.remove(collectionId);
      return new SuccessResponse();
    } else {
      return new ErrorResponse(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Mock can only CREATE and DELETE collection"));
    }
  }

  private Response executeWriteFeature(Write write) {
    String collectionId = write.getCollectionId();
    if (!mockCollection.containsKey(collectionId)) {
      return new ErrorResponse(new NakshaError(
          NakshaError.COLLECTION_NOT_FOUND,
          "Collection " + write.getCollectionId() + " doesn't exist."
      ));
    }

    WriteOp op = write.getOp();
    NakshaFeature feature = write.getFeature();
    if (op.equals(WriteOp.CREATE)) {
      return insertFeature(collectionId, feature);
    } else if (op.equals(WriteOp.UPDATE)) {
      return updateFeature(collectionId, feature);
    } else if (op.equals(WriteOp.UPSERT)) {
      return upsertFeature(collectionId, feature);
    } else if (op.equals(WriteOp.DELETE)) {
      return deleteFeature(collectionId, feature.getId());
    } else if (op.equals(WriteOp.PURGE)) {
      return new ErrorResponse(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "PurgeFeature not mocked yet"));
    } else {
      return new ErrorResponse(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, op.getValue() + " not mocked yet"));
    }
  }

  private Response insertFeature(
      final @NotNull String collectionId,
      final @NotNull NakshaFeature feature
  ) {
    if (mockCollection.get(collectionId).putIfAbsent(feature.getId(), setUuidFor(feature)) != null) {
      return new ErrorResponse(new NakshaError(NakshaError.CONFLICT, "Feature already exists: " + feature.getId()));
    }
    return new SuccessResponse();
  }

  private Response updateFeature(
      final @NotNull String collectionId,
      final @NotNull NakshaFeature feature
  ) {
    final AtomicReference<NakshaError> error = new AtomicReference<>();

    mockCollection.get(collectionId).compute(feature.getId(), (fId, oldF) -> {
      // no existing feature to update
      if (oldF == null) {
        error.set(new NakshaError(NakshaError.NOT_FOUND, "No feature found for id " + fId));
        return oldF;
      }
      // update if UUID matches (or overwrite if new uuid is missing)
      if ((Objects.equals(uuidOf(oldF), uuidOf(feature)) && uuidOf(feature) != null) || uuidOf(feature) == null) {
        return setUuidFor(feature);
      } else {
        // throw error if UUID mismatches
        error.set(new NakshaError(NakshaError.ILLEGAL_STATE, "Uuid " + uuidOf(oldF) + " mismatch for id " + fId));
        return oldF;
      }
    });
    if (error.get() != null) {
      return new ErrorResponse(error.get());
    }
    return new SuccessResponse();
  }

  private Response upsertFeature(
      final @NotNull String collectionId,
      final @NotNull NakshaFeature feature
  ) {
    final AtomicReference<NakshaFeature> result = new AtomicReference<>();
    final AtomicReference<NakshaError> error = new AtomicReference<>();

    mockCollection.get(collectionId).compute(feature.getId(), (fId, oldF) -> {
      // insert if missing
      if (oldF == null) {
        if (uuidOf(feature) == null) {
          setUuidFor(feature);
        }
        result.set(feature);
        return feature;
      }
      // update if UUID matches (or overwrite if new uuid is missing)
      if ((Objects.equals(uuidOf(oldF), uuidOf(feature)) && uuidOf(feature) != null) || uuidOf(feature) == null) {
        result.set(feature);
        return setUuidFor(feature);
      } else {
        // throw error if UUID mismatches
        error.set(new NakshaError(NakshaError.CONFLICT, "Uuid " + uuidOf(oldF) + " mismatch for id " + feature.getId()));
        return oldF;
      }
    });
    if (error.get() != null) {
      return new ErrorResponse(error.get());
    }
    return new SuccessResponse();
  }

  private Response deleteFeature(
      final @NotNull String collectionId,
      final @NotNull String id,
      final @Nullable String uuid
  ) {
    final AtomicReference<NakshaError> error = new AtomicReference<>();

    mockCollection.get(collectionId).compute(id, (fId, oldF) -> {
      // nothing to delete if it is already absent
      if (oldF == null) {
//        result.set(featureCodec(id, EExecutedOp.RETAINED)); TODO?
        return oldF;
      }
      // delete if UUID matches
      if ((Objects.equals(uuidOf(oldF), uuid)) || (uuid == null)) {
//        result.set(oldF); TODO
        return null;
      } else {
        // throw error if UUID mismatches
        error.set(new NakshaError(NakshaError.ILLEGAL_ARGUMENT,
            "Uuid " + uuidOf(oldF) + " mismatch for id " + id));
        return oldF;
      }
    });
    if (error.get() != null) {
      throw new NakshaException(error.get());
    }
    return new SuccessResponse();
  }

  private Response deleteFeature(
      final @NotNull String collectionId,
      final @Nullable String featureId
  ) {
    if (featureId == null) {
      return new ErrorResponse(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Can't delete feature without id"));
    }
    mockCollection.get(collectionId).remove(collectionId);
    return new SuccessResponse();
  }

  private @Nullable String uuidOf(final @NotNull NakshaFeature feature) {
    return feature.getProperties().getXyz().getUuid();
  }

  private NakshaFeature setUuidFor(final @NotNull NakshaFeature feature) {
    feature.getProperties().getXyz().setRaw("uuid", UUID.randomUUID());
    return feature;
  }

  @Override
  public void commit() {
    // do nothing
  }

  @Override
  public void rollback() {
    // do nothing
  }

  @Override
  public @NotNull ILock acquireSessionLock(@NotNull String lockId) {
    return null;
  }

  @Override
  public @NotNull ILock acquireTransactionLock(@NotNull String lockId) {
    return null;
  }

  @Override
  public @NotNull NakshaTx useTransaction() {
    return null;
  }

  @Override
  public @Nullable NakshaTx getTransaction() {
    return null;
  }

  private final @NotNull MemberProcessorMap processors = new MemberProcessorMap();

  @Override
  public @NotNull MemberProcessorMap getProcessors() {
    return processors;
  }
}
