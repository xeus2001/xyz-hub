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

import java.util.List;

import naksha.model.IReadSession;
import naksha.model.IStorage;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.NakshaVersion;
import naksha.model.SessionOptions;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaCatalog;
import naksha.model.request.FeatureTuple;
import naksha.model.request.ReadRequest;
import naksha.model.request.Request;
import naksha.model.request.Response;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHAdminStorageReader implements IReadSession {

  /**
   * Current session, all read storage operations should be executed against
   */
  final @NotNull IReadSession session;

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  protected NHAdminStorageReader(final @NotNull IReadSession reader) {
    this.session = reader;
  }

  @Override
  public int getSocketTimeout() {
    return session.getSocketTimeout();
  }

  @Override
  public void setSocketTimeout(int i) {
    session.setSocketTimeout(i);
  }

  @Override
  public int getStmtTimeout() {
    return session.getStmtTimeout();
  }

  @Override
  public void setStmtTimeout(int i) {
    session.setStmtTimeout(i);
  }

  @Override
  public int getLockTimeout() {
    return session.getLockTimeout();
  }

  @Override
  public void setLockTimeout(int i) {
    session.setLockTimeout(i);
  }

  @Override
  public @NotNull Response executeRead(@NotNull ReadRequest request) {
    return session.executeRead(request);
  }

  @Override
  public boolean isClosed() {
    return session.isClosed();
  }

  @Override
  public void close() {
    session.close();
  }

  @Override
  public @NotNull IStorage getStorage() {
    return session.getStorage();
  }

  @Override
  public @NotNull SessionOptions getOptions() {
    return session.getOptions();
  }

  @Override
  public @Nullable NakshaCatalog getCatalogById(@NotNull String catalogId, boolean allowTombstone) {
    return session.getCatalogById(catalogId, allowTombstone);
  }

  @Override
  public @Nullable NakshaCatalog getCatalogByNumber(int catalogNumber, boolean allowTombstone) {
    return session.getCatalogByNumber(catalogNumber, allowTombstone);
  }

  @Override
  public @Nullable NakshaCollection getCollectionById(
      @NotNull NakshaCatalog map,
      @NotNull String collectionId,
      boolean allowTombstone) {
    return session.getCollectionById(map, collectionId, allowTombstone);
  }

  @Override
  public void loadTuples(@NotNull List<? extends FeatureTuple> featureTuples, int from, int to) {
    session.loadTuples(featureTuples, from, to);
  }

  @Override
  public @Nullable NakshaCollection getCollectionByNumber(
      @NotNull NakshaCatalog catalog,
      int collectionNumber,
      boolean allowTombstone) {
    return session.getCollectionByNumber(catalog, collectionNumber, allowTombstone);
  }
}
