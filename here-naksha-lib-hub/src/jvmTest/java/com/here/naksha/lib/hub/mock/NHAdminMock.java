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

import static com.here.naksha.lib.core.HubInternalIdentifiers.ALL_HUB_INTERNAL_COLLECTIONS;
import static com.here.naksha.lib.core.HubInternalIdentifiers.CONFIGS;
import static com.here.naksha.lib.core.exceptions.UncheckedException.unchecked;
import static naksha.base.NakshaExceptionKt.unsupportedOp;
import static naksha.model.util.RequestHelper.createFeatureRequest;

import com.here.naksha.lib.hub.NakshaHubAdminStorageIdentifiers;
import com.here.naksha.lib.hub.NakshaHubConfig;
import com.here.naksha.lib.hub.mock.NHAdminMock.Config;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.reflect.KClass;
import naksha.base.Platform;
import naksha.base.PlatformLock;
import naksha.jbon.JbDictionary;
import naksha.model.*;
import naksha.base.NakshaError;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaFeature;
import naksha.model.objects.NakshaStorage;
import naksha.model.request.ErrorResponse;
import naksha.model.request.Response;
import naksha.model.request.Write;
import naksha.model.request.WriteRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHAdminMock extends AbstractStorage<Config> {

  protected static @NotNull Map<String, TreeMap<String, NakshaFeature>> mockCollection;
  protected static @NotNull NakshaHubConfig nakshaHubConfig;

  @Override
  public @NotNull KClass<Config> getConfigKlass() {
    return Platform.klassFor(Config.class);
  }

  @Override
  protected void initStorage(@NotNull NHAdminMock.Config config, @Nullable Boolean create, @Nullable Boolean upgrade) {
    // empty on purpose
  }

  @Override
  protected void afterInit() {
    // empty on purpose
  }

  @Override
  protected void shutdownStorage(boolean dropCache) {
    // empty on purpose
  }

  @Override
  public @NotNull IStreamSession newStreamSession(@Nullable SessionOptions options) {
    throw unsupportedOp("newStreamSession");
  }

  public static class Config extends NakshaStorage {

  }

  public NHAdminMock() {
    // this constructor is only to support Platform-based instantiation
    if (this.mockCollection == null) {
      this.mockCollection = new ConcurrentHashMap<>();
      setupCollections();
    }
  }

  public NHAdminMock(
      final @NotNull Map<String, TreeMap<String, Object>> mockCollection,
      final @NotNull NakshaHubConfig customCfg) {
    throw new UnsupportedOperationException(
        "NHAdminMock storage should not be used"); // comment to use mock in local env
    /*this.mockCollection = mockCollection;
    this.nakshaHubConfig = customCfg;
    this.initStorage();
    this.setupConfig();*/
  }

  private void setupConfig() {
    // Add custom config in naksha:configs
    final NakshaContext ctx = NakshaContext.newInstance("naksha_mock");
    ctx.attachToCurrentThread();
    runInWriteSession(SessionOptions.from(ctx, true), admin -> {
      final Response response = admin.execute(
          createFeatureRequest(NakshaHubAdminStorageIdentifiers.getHubAdminMapId(), CONFIGS, nakshaHubConfig));
      if (response instanceof ErrorResponse errorResponse) {
        admin.rollback();
        throw unchecked(
            new Exception("Unable to add custom config in Mock storage (code: " + errorResponse.getError().getCode() + " )",
                errorResponse.getError().getCause()));
      }
      admin.commit();
    });
  }

  @NotNull
  @Override
  public String getId() {
    throw new UnsupportedOperationException("Not yet supported by NHAdminMock");
  }

  @Override
  public int getHardCap() {
    throw new UnsupportedOperationException("Not yet supported by NHAdminMock");
  }

  private void setupCollections() {
    final NakshaContext ctx = NakshaContext.newInstance("naksha_mock");
    ctx.attachToCurrentThread();

    // Create all admin collections
    runInWriteSession(SessionOptions.from(ctx, true), admin -> {
      WriteRequest writeAdminCollections = new WriteRequest();
      for (final String name : ALL_HUB_INTERNAL_COLLECTIONS) {
        Write write = new Write().createCollection(new NakshaCollection(name, NakshaHubAdminStorageIdentifiers.getHubAdminMapId()));
        writeAdminCollections.add(write);
      }
      final Response response = admin.execute(writeAdminCollections);
      if (response instanceof ErrorResponse errorResponse) {
        admin.rollback();
        NakshaError error = errorResponse.getError();
        throw unchecked(
            new Exception("Unable to create Admin collections in Mock storage (code: " + error.getCode() + " )", error.getCause()));
      }
      admin.commit();
    });
  }

  @NotNull
  @Override
  public IWriteSession newWriteSession(@Nullable SessionOptions options) {
    return new NHAdminWriterMock(mockCollection);
  }

  @NotNull
  @Override
  public IReadSession newReadSession(@Nullable SessionOptions options) {
    return new NHAdminReaderMock(mockCollection);
  }

  @Override
  public @NotNull PlatformLock getLock() {
    return Platform.newLock();
  }

  @Override
  public long getNumber() {
    throw new UnsupportedOperationException("Not yet supported by NHAdminMock");
  }

  @Override
  public @Nullable JbDictionary getDictionary(@NotNull String id) {
    throw new UnsupportedOperationException("Not yet supported by NHAdminMock");
  }

  @Override
  public @Nullable JbDictionary getEncodingDictionary(@Nullable Object feature, @Nullable Object context) {
    throw new UnsupportedOperationException("Not yet supported by NHAdminMock");
  }
}
