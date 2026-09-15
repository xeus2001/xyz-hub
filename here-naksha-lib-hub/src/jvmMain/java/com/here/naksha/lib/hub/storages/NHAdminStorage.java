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

import naksha.base.PlatformLock;
import naksha.base.fn.Fn1;
import naksha.base.fn.Fx1;
import naksha.jbon.JbDictionary;
import naksha.model.*;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.objects.NakshaStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static naksha.base.NakshaExceptionKt.unsupportedOp;

public class NHAdminStorage implements IStorage {

  public static final String ID_PREFIX = "NH_ADMIN_STORAGE_";

  /**
   * Singleton instance of physical admin storage implementation
   */
  private final @NotNull IStorage psqlStorage;

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  public NHAdminStorage(final @NotNull IStorage psqlStorage) {
    this.psqlStorage = psqlStorage;
  }

  @NotNull
  @Override
  public IWriteSession newWriteSession(@Nullable SessionOptions options) {
    return new NHAdminStorageWriter(psqlStorage.newWriteSession(options));
  }

  @NotNull
  @Override
  public IReadSession newReadSession(@Nullable SessionOptions options) {
    return new NHAdminStorageReader(psqlStorage.newReadSession(options));
  }

  @NotNull
  @Override
  public String getId() {
    return ID_PREFIX + psqlStorage.getId();
  }

  @Override
  public int getHardCap() {
    return psqlStorage.getHardCap();
  }

  @Override
  public @NotNull PlatformLock getLock() {
    throw new NakshaException(new NakshaError(NakshaError.NOT_IMPLEMENTED, "getLock not supported"));
  }

  @Override
  public @NotNull NakshaStorage getConfig() {
    return psqlStorage.getConfig();
  }

  @Override
  public long getNumber() {
    return psqlStorage.getNumber();
  }

  public @NotNull DataEncoding getDataEncoding(@Nullable Object feature, @Nullable Object context) {
    throw new UnsupportedOperationException("Not supported by NHAdminStorage");
  }

  @Override
  public @Nullable JbDictionary getDictionary(@NotNull String id) {
    return psqlStorage.getDictionary(id);
  }

  @Override
  public @Nullable JbDictionary getEncodingDictionary(@Nullable Object feature, @Nullable Object context) {
    return psqlStorage.getEncodingDictionary(feature, context);
  }

  @Override
  public <T> T useWriteSession(@Nullable SessionOptions options, @NotNull Fn1<T, IWriteSession> lambda) {
    return IStorage.super.useWriteSession(options, lambda);
  }

  @Override
  public void runInWriteSession(@Nullable SessionOptions options, @NotNull Fx1<IWriteSession> lambda) {
    IStorage.super.runInWriteSession(options, lambda);
  }

  @Override
  public <T> T useReadSession(@Nullable SessionOptions options, @NotNull Fn1<T, IReadSession> lambda) {
    return IStorage.super.useReadSession(options, lambda);
  }

  @Override
  public void runInReadSession(@Nullable SessionOptions options, @NotNull Fx1<IReadSession> lambda) {
    IStorage.super.runInReadSession(options, lambda);
  }

  @Override
  public @NotNull IStreamSession newStreamSession(@Nullable SessionOptions options) {
    throw unsupportedOp("newStreamSession");
  }
}
