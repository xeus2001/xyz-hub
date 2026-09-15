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
package com.here.naksha.lib.view;

import naksha.model.ILock;
import naksha.model.IWriteSession;
import naksha.model.MemberProcessorMap;
import naksha.model.SessionOptions;
import naksha.model.objects.NakshaTx;
import naksha.model.request.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * It writes value only to top priority layer (storage).
 */
public class ViewWriteSession extends ViewReadSession implements IWriteSession {

  IWriteSession session;
  ViewLayer writeLayer;
  SessionOptions options;

  public ViewWriteSession(@NotNull View view, @Nullable SessionOptions options) {
    super(view, options);
    this.options = options;
  }

  public ViewWriteSession withWriteLayer(ViewLayer viewLayer) {
    if (this.session != null) {
      throw new RuntimeException("Write session initiated with " + this.writeLayer.getCollectionId());
    }
    this.writeLayer = viewLayer;
    return this;
  }

  @Override
  public @NotNull Response executeRead(@NotNull ReadRequest request) {
    ensureSessionInitialized();
    final ReadFeatures readFeatures = (ReadFeatures) request;
    readFeatures.setCatalogId(writeLayer.getMapId());
    readFeatures.setCollectionId(writeLayer.getCollectionId());
    return this.session.executeRead(request);
  }

  @Override
  public @NotNull Response executeWrite(@NotNull WriteRequest request) {
    ensureSessionInitialized();
    for (Write write : request.getWrites()) {
      if(write.getOp().equals(WriteOp.UPDATE)){
        write.withOp(WriteOp.UPSERT);
      }
      write.withMapId(writeLayer.getMapId());
      write.withCollectionId(writeLayer.getCollectionId());
    }
    return this.session.executeWrite(request);
  }

  @Override
  public @NotNull NakshaTx useTransaction() {
    return this.session.useTransaction();
  }

  public void commit() {
    getSession().commit();
  }

  public void rollback() {
    getSession().rollback();
  }

  public void close() {
    super.close();
    getSession().close();
  }

  private IWriteSession getSession() {
    ensureSessionInitialized();
    return this.session;
  }

  private void ensureSessionInitialized() {
    if (this.session == null) {
      if (this.writeLayer == null) {
        this.writeLayer = this.view.getViewCollection().getTopPriorityLayer();
      }
      this.session = this.writeLayer.getStorage().newWriteSession(this.options);
    }
  }

  @Override
  public @NotNull ILock acquireSessionLock(@NotNull String lockId) {
    return getSession().acquireSessionLock(lockId);
  }

  @Override
  public @NotNull ILock acquireTransactionLock(@NotNull String lockId) {
    return getSession().acquireTransactionLock(lockId);
  }

  @Override
  public @Nullable NakshaTx getTransaction() {
    return getSession().getTransaction();
  }

  private final @NotNull MemberProcessorMap processors = new MemberProcessorMap();

  @Override
  public @NotNull MemberProcessorMap getProcessors() {
    return processors;
  }
}
