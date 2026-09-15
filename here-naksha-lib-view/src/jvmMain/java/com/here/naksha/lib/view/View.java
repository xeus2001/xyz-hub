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

import kotlin.reflect.KClass;
import naksha.base.Platform;
import naksha.jbon.JbDictionary;
import naksha.model.AbstractStorage;
import naksha.model.IStreamSession;
import naksha.model.SessionOptions;
import naksha.model.objects.NakshaStorage;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static naksha.base.NakshaExceptionKt.unsupportedOp;

// TODO: This should implement IStorage
public class View extends AbstractStorage<NakshaStorage> implements IView {

  private @NotNull ViewLayerCollection viewLayerCollection;

  public View() {

  }

  @Override
  public @NotNull String getId() {
    return viewLayerCollection.getName();
  }

  public View(@NotNull ViewLayerCollection viewLayerCollection) {
    this.viewLayerCollection = viewLayerCollection;
  }

  public @NotNull ViewLayerCollection getViewCollection() {
    return viewLayerCollection;
  }

  public @NotNull ViewReadSession newReadSession(@Nullable SessionOptions options) {
    return new ViewReadSession(this, options);
  }

  public @NotNull ViewWriteSession newWriteSession(@Nullable SessionOptions options) {
    return new ViewWriteSession(this, options);
  }

  public void setViewLayerCollection(@NotNull ViewLayerCollection viewLayerCollection) {
    this.viewLayerCollection = viewLayerCollection;
  }

  public @NotNull naksha.model.DataEncoding getDataEncoding(@Nullable Object feature, @Nullable Object context) {
    throw new NotImplementedException("Not supported by View storage");
  }

  @Override
  public @Nullable JbDictionary getDictionary(@NotNull String id) {
    throw new NotImplementedException("Not supported by View storage");
  }

  @Override
  public @Nullable JbDictionary getEncodingDictionary(@Nullable Object feature, @Nullable Object context) {
    throw new NotImplementedException("Not supported by View storage");
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
  protected void initStorage(@NotNull NakshaStorage nakshaStorage, @Nullable Boolean create, @Nullable Boolean upgrade) {
    // Nothing to do
  }

  @Override
  public @NotNull IStreamSession newStreamSession(@Nullable SessionOptions options) {
    throw unsupportedOp("newStreamSession");
  }
}
