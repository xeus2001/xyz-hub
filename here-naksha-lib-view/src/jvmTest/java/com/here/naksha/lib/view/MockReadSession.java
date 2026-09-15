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
package com.here.naksha.lib.view;

import naksha.model.IReadSession;
import java.util.List;

import naksha.model.IStorage;
import naksha.model.MemberProcessorMap;
import naksha.model.SessionOptions;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaCatalog;
import naksha.model.request.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockReadSession implements IReadSession {

  FeatureTupleList results;

  public MockReadSession(FeatureTupleList results) {
    this.results = results;
  }

  @Override
  public void close() {}

  @Override
  public @NotNull Response executeRead(@NotNull ReadRequest request) {
    return new SuccessResponse(results);
  }

  private int socketTimeout = 0;

  @Override
  public int getSocketTimeout() {
    return socketTimeout;
  }

  @Override
  public void setSocketTimeout(int i) {
    socketTimeout = i;
  }

  private int stmtTimeout = 0;

  @Override
  public int getStmtTimeout() {
    return stmtTimeout;
  }

  @Override
  public void setStmtTimeout(int i) {
    stmtTimeout = i;
  }

  private int lockTimeout = 0;

  @Override
  public int getLockTimeout() {
    return lockTimeout;
  }

  @Override
  public void setLockTimeout(int i) {
    lockTimeout = i;
  }

  @Override
  public boolean isClosed() {
    return false;
  }

  private String map = "";

  @Override
  public @NotNull IStorage getStorage() {
    return null;
  }

  @Override
  public @Nullable NakshaCatalog getCatalogById(@NotNull String catalogId, boolean allowTombstone) {
    return null;
  }

  @Override
  public @Nullable NakshaCatalog getCatalogByNumber(int catalogNumber, boolean allowTombstone) {
    return null;
  }

  @Override
  public @Nullable NakshaCollection getCollectionById(
      @NotNull NakshaCatalog map,
      @NotNull String collectionId,
      boolean allowTombstone) {
    return null;
  }

  @Override
  public @Nullable NakshaCollection getCollectionByNumber(
      @NotNull NakshaCatalog catalog,
      int collectionNumber,
      boolean allowTombstone) {
    return null;
  }

  @Override
  public @NotNull SessionOptions getOptions() {
    return null;
  }

  @Override
  public void loadTuples(@NotNull List<? extends FeatureTuple> featureTuples, int from, int to) {
  }
}
