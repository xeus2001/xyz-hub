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
package com.here.naksha.storage.http;

import com.here.naksha.lib.core.models.storage.ReadFeaturesProxyWrapper;
import com.here.naksha.storage.http.connector.ConnectorInterfaceReadExecute;
import com.here.naksha.storage.http.ffw.FfwInterfaceReadExecute;
import naksha.base.*;
import naksha.model.IReadSession;
import naksha.model.IStorage;
import naksha.model.NakshaContext;
import naksha.model.SessionOptions;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaCatalog;
import naksha.model.objects.NakshaFeature;
import naksha.model.objects.NakshaFeatureList;
import naksha.model.request.*;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static naksha.base.Platform.javaProxy;

public class HttpStorageReadSession implements IReadSession {

  private static final Logger log = LoggerFactory.getLogger(HttpStorageReadSession.class);
  private static final String DEFAULT_VIRTUAL_CATALOG_ID = "0";

  @NotNull
  private final NakshaContext context;

  @NotNull
  private final RequestSender requestSender;

  @NotNull
  private final HttpStorage storage;

  @NotNull
  private final HttpInterface httpInterface;

  HttpStorageReadSession(
      @Nullable NakshaContext context,
      @NotNull HttpStorage storage,
      @NotNull RequestSender requestSender,
      @NotNull HttpInterface httpInterface) {
    this.context = context == null ? NakshaContext.currentContext() : context;
    this.storage = storage;
    this.requestSender = requestSender;
    this.httpInterface = httpInterface;
  }

  public @NotNull NakshaContext getNakshaContext() {
    return context;
  }

  @Override
  public @NotNull Response executeRead(@NotNull ReadRequest request) {
    try {
      final ReadFeaturesProxyWrapper requestWrapper = request instanceof ReadFeaturesProxyWrapper
            ? (ReadFeaturesProxyWrapper) request : requireNonNull(javaProxy(request, ReadFeaturesProxyWrapper.class));
      final Response response;
      switch (httpInterface) {
        case ffwAdapter:
          response = FfwInterfaceReadExecute.execute(context, requestWrapper, requestSender);
          break;
        case dataHubConnector:
          response = ConnectorInterfaceReadExecute.execute(context, requestWrapper, requestSender);
          break;
        default:
          throw new IllegalStateException("Unsupported HTTP interface: " + httpInterface);
      }
      return attachVirtualTupleNumbers(response, requestWrapper);
    } catch (NakshaException exception) {
      log.info("Unexpected error while executing read", exception);
      return new ErrorResponse(exception.getError());
    } catch (Exception exception) {
      log.warn("We got exception while executing Read request.", exception);
      return new ErrorResponse(NakshaError.EXCEPTION, exception.getMessage(), exception);
    }
  }

  @Override
  public void close() {}

  @Override
  public int getSocketTimeout() {
    return requestSender.keyProps.getSocketTimeoutSec();
  }

  @Override
  public void setSocketTimeout(int i) {
    throw new IllegalStateException("Can only be set when creating the session");
  }

  @Override
  public int getStmtTimeout() {
    throw new NotImplementedException("Not supported for HTTP storage");
  }

  @Override
  public void setStmtTimeout(int i) {
    throw new NotImplementedException("Not supported for HTTP storage");
  }

  @Override
  public int getLockTimeout() {
    throw new NotImplementedException("Not supported for HTTP storage");
  }

  @Override
  public void setLockTimeout(int i) {
    throw new NotImplementedException("Not supported for HTTP storage");
  }

  @Override
  public boolean isClosed() {
    return false;
    // TODO
  }

  @Override
  public @NotNull IStorage getStorage() {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @Nullable NakshaCatalog getCatalogById(@NotNull String catalogId, boolean allowTombstone) {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @Nullable NakshaCatalog getCatalogByNumber(int catalogNumber, boolean allowTombstone) {
    return null;
  }

  @Override
  public void loadTuples(@NotNull List<? extends FeatureTuple> featureTuples, int from, int to) {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @Nullable NakshaCollection getCollectionByNumber(
      @NotNull NakshaCatalog catalog,
      int collectionNumber,
      boolean allowTombstone) {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @NotNull SessionOptions getOptions() {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @Nullable NakshaCollection getCollectionById(
      @NotNull NakshaCatalog map,
      @NotNull String collectionId,
      boolean allowTombstone) {
    // TODO: Technically, this translates into creating an ReadCollections query!
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @NotNull
  RequestSender getRequestSender() {
    return requestSender;
  }

  @NotNull Response attachVirtualTupleNumbers(
      @NotNull Response response, @NotNull ReadFeaturesProxyWrapper request) {
    // Virtual tuple numbers are transient adapter identities and are not reloadable or persistable.
    if (!(response instanceof SuccessResponse)) {
      return response;
    }

    final SuccessResponse success = (SuccessResponse) response;
    final NakshaFeatureList features = success.getFeatures();
    final FeatureTupleList featureTuples = new FeatureTupleList();
    featureTuples.setCapacity(features.size());
    if (features.isEmpty()) {
      return new SuccessResponse(featureTuples);
    }

    final String requestedCatalogId = request.getCatalogId();
    // HTTP storage schemas are optional; catalog number 0 is the documented default scope.
    final String catalogId = requestedCatalogId == null ? DEFAULT_VIRTUAL_CATALOG_ID : requestedCatalogId;
    final String collectionId = requireCollectionId(request.getCollectionId());
    final Version version = storage.allocateVirtualVersion();
    for (final NakshaFeature feature : features) {
      if (feature == null) {
        continue;
      }
      final String featureId = requireFeatureId(feature);
      final TupleNumber tupleNumber = storage.createVirtualTupleNumber(
          catalogId, collectionId, featureId, version);
      final FeatureTuple featureTuple = new FeatureTuple(tupleNumber, null);
      featureTuple.setFeature(feature);
      featureTuples.add(featureTuple);
    }
    return new SuccessResponse(featureTuples);
  }

  private static @NotNull String requireFeatureId(@NotNull NakshaFeature feature) {
    final Object rawId = feature.getRaw(NakshaFeature.ID_KEY);
    if (!(rawId instanceof String) || ((String) rawId).isBlank()) {
      throw new NakshaException(
          NakshaError.ILLEGAL_ARGUMENT, "HTTP response contains a feature without a non-empty 'id'");
    }
    return (String) rawId;
  }

  private static @NotNull String requireCollectionId(@Nullable String collectionId) {
    if (collectionId == null || collectionId.isBlank()) {
      throw new NakshaException(NakshaError.ILLEGAL_ARGUMENT, "collectionId must be non-empty");
    }
    return collectionId;
  }
}
