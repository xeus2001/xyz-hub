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

import com.here.naksha.storage.http.RequestSender.KeyProperties;
import com.here.naksha.storage.http.cache.RequestSenderCache;
import kotlin.reflect.KClass;
import naksha.base.Action;
import naksha.base.JvmBoxingUtil;
import naksha.base.Platform;
import naksha.base.TupleNumber;
import naksha.base.Version;
import naksha.jbon.JbDictionary;
import naksha.model.*;
import naksha.model.objects.NakshaStorage;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static naksha.base.NakshaExceptionKt.unsupportedOp;


public class HttpStorage extends AbstractStorage<NakshaStorage> {

  private  KeyProperties defaultKeyProperties;

  private  NakshaStorage storageConfig;
  private  HttpStorageProperties httpStorageProperties;

  public HttpStorage() {

  }

  private static @NotNull HttpStorageProperties getProperties(@NotNull NakshaStorage storage) {
    HttpStorageProperties storageProperties = JvmBoxingUtil.box(storage.getProperties(), HttpStorageProperties.class);
    if (storageProperties == null) {
      throw new IllegalArgumentException("A HTTP storage must have properties containing a 'url'");
    }
    return storageProperties;
  }

  @Override
  protected void initStorage(@NotNull NakshaStorage config, @Nullable Boolean create, @Nullable Boolean upgrade) {
    this.storageConfig = config;
    this.httpStorageProperties = getProperties(config);
    if (httpStorageProperties == null || httpStorageProperties.getUrl() == null) {
      throw new IllegalArgumentException("A HTTP storage must have properties containing a 'url'");
    }
    this.defaultKeyProperties = KeyProperties.fromHttpStorageProperties(config.getId(), httpStorageProperties);
  }

  @NotNull
  @Override
  public IReadSession newReadSession(@Nullable SessionOptions options) {
    useInitialized();
    if (defaultKeyProperties == null) {
      throw new IllegalStateException("HttpStorage is not initialized.");
    }

    final RequestSender requestSender = RequestSenderCache.getInstance()
            .getSenderWith(new KeyProperties(
                    getId(),
                    defaultKeyProperties.getHostUrl(),
                    defaultKeyProperties.getDefaultHeaders(),
                    defaultKeyProperties.getConnectionTimeoutSec(),
                    defaultKeyProperties.getSocketTimeoutSec(),
                    httpStorageProperties.getMaxRetries()
            ));
    return new HttpStorageReadSession(
            NakshaContext.currentContext(), this, requestSender, httpStorageProperties.getProtocol());
  }

  @NotNull
  @Override
  public IWriteSession newWriteSession(@Nullable SessionOptions options) {
    useInitialized();
    if (defaultKeyProperties == null) {
      throw new IllegalStateException("HttpStorage is not initialized.");
    }

    final RequestSender requestSender = RequestSenderCache.getInstance()
            .getSenderWith(new KeyProperties(
                    getId(),
                    defaultKeyProperties.getHostUrl(),
                    defaultKeyProperties.getDefaultHeaders(),
                    defaultKeyProperties.getConnectionTimeoutSec(),
                    defaultKeyProperties.getSocketTimeoutSec(),
                    httpStorageProperties.getMaxRetries()
            ));
    return new HttpStorageWriteSession(
            NakshaContext.currentContext(), this, requestSender, httpStorageProperties.getProtocol());
  }

  @NotNull
  @Override
  public String getId() {
    return defaultKeyProperties.getName();
  }

  @NotNull
  Version allocateVirtualVersion() {
    return super.newVirtualVersion();
  }

  @NotNull
  TupleNumber createVirtualTupleNumber(
      @NotNull String catalogId,
      @NotNull String collectionId,
      @NotNull String featureId,
      @NotNull Version version) {
    return super.newVirtualTupleNumber(catalogId, collectionId, featureId, version.number, Action.VERSION);
  }

  @Override
  public int getHardCap() {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public long getNumber() {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  public @NotNull naksha.model.DataEncoding getDataEncoding(@Nullable Object feature, @Nullable Object context) {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @Nullable JbDictionary getDictionary(@NotNull String id) {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  public @Nullable JbDictionary getEncodingDictionary(@Nullable Object feature, @Nullable Object context) {
    throw new NotImplementedException("Not supported by HTTP storage");
  }

  @Override
  protected void afterInit() {
    // Nothing to do
  }

  @Override
  protected void shutdownStorage(boolean dropCache) {
    // Nothing to do
  }

  @Override
  public @NotNull KClass<NakshaStorage> getConfigKlass() {
    return Platform.klassFor(NakshaStorage.class);
  }

  @Override
  public @NotNull IStreamSession newStreamSession(@Nullable SessionOptions options) {
    throw unsupportedOp("newStreamSession");
  }
}