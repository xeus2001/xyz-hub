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

import naksha.model.ILock;
import naksha.model.IWriteSession;
import naksha.model.NakshaVersion;
import naksha.model.objects.NakshaTx;
import naksha.model.request.Response;
import naksha.model.request.WriteRequest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHAdminStorageWriter extends NHAdminStorageReader implements IWriteSession {

  /**
   * Current session, all write storage operations should be executed against
   */
  final @NotNull IWriteSession session;

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  public NHAdminStorageWriter(final @NotNull IWriteSession writer) {
    super(writer);
    this.session = writer;
  }

  @Override
  public void commit() {
    session.commit();
  }

  @Override
  public void rollback() {
    session.rollback();
  }

  @Override
  public @NotNull ILock acquireSessionLock(@NotNull String lockId) {
    return session.acquireSessionLock(lockId);
  }

  @Override
  public @NotNull ILock acquireTransactionLock(@NotNull String lockId) {
    return session.acquireTransactionLock(lockId);
  }

  @Override
  public @NotNull NakshaTx useTransaction() {
    return session.useTransaction();
  }

  @Override
  public @Nullable NakshaTx getTransaction() {
    return session.getTransaction();
  }

  @Override
  public @NotNull naksha.model.MemberProcessorMap getProcessors() {
    return session.getProcessors();
  }

  @Override
  public @NotNull Response executeWrite(@NotNull WriteRequest request) {
    return session.executeWrite(request);
  }
}
