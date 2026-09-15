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

import static com.here.naksha.lib.core.HubInternalIdentifiers.ALL_HUB_INTERNAL_COLLECTIONS;
import static com.here.naksha.lib.core.exceptions.UncheckedException.unchecked;
import static naksha.base.NakshaExceptionKt.unsupportedOp;

import com.here.naksha.lib.core.HubInternalIdentifiers;
import com.here.naksha.lib.core.IEventHandler;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.handlers.AuthorizationEventHandler;
import com.here.naksha.lib.handlers.internal.IntHandlerForConfigs;
import com.here.naksha.lib.handlers.internal.IntHandlerForEventHandlerConfigs;
import com.here.naksha.lib.handlers.internal.IntHandlerForExtensions;
import com.here.naksha.lib.handlers.internal.IntHandlerForSpaces;
import com.here.naksha.lib.handlers.internal.IntHandlerForStorageConfigs;
import com.here.naksha.lib.handlers.internal.IntHandlerForSubscriptions;
import com.here.naksha.lib.hub.EventPipelineFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import naksha.base.PlatformLock;
import naksha.base.fn.Fn1;
import naksha.base.fn.Fx1;
import naksha.jbon.JbDictionary;
import naksha.model.*;
import naksha.model.objects.NakshaStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHSpaceStorage implements IStorage {

  protected final @NotNull INaksha nakshaHub;

  /**
   * List of Admin virtual spaces with relevant event handlers required to support event processing
   */
  protected final @NotNull Map<String, List<IEventHandler>> virtualSpaces;

  protected final @NotNull EventPipelineFactory pipelineFactory;

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  public NHSpaceStorage(final @NotNull INaksha hub, final @NotNull EventPipelineFactory pipelineFactory) {
    this.nakshaHub = hub;
    this.pipelineFactory = pipelineFactory;
    this.virtualSpaces = configureVirtualSpaces(hub);
  }

  private @NotNull Map<String, List<IEventHandler>> configureVirtualSpaces(final @NotNull INaksha hub) {
    final Map<String, List<IEventHandler>> adminSpaces = new HashMap<>();
    // common auth handler
    final IEventHandler authHandler = new AuthorizationEventHandler(hub);
    // add event handlers for each admin space
    for (final String spaceId : ALL_HUB_INTERNAL_COLLECTIONS) {
      adminSpaces.put(
          spaceId,
          switch (spaceId) {
            case HubInternalIdentifiers.CONFIGS -> List.of(authHandler, new IntHandlerForConfigs(hub));
            case HubInternalIdentifiers.SPACES -> List.of(authHandler, new IntHandlerForSpaces(hub));
            case HubInternalIdentifiers.SUBSCRIPTIONS -> List.of(
                authHandler, new IntHandlerForSubscriptions(hub));
            case HubInternalIdentifiers.EVENT_HANDLERS -> List.of(
                authHandler, new IntHandlerForEventHandlerConfigs(hub));
            case HubInternalIdentifiers.STORAGES -> List.of(authHandler, new IntHandlerForStorageConfigs(hub));
            case HubInternalIdentifiers.EXTENSIONS -> List.of(authHandler, new IntHandlerForExtensions(hub));
            default -> throw unchecked(new Exception("Unsupported virtual space " + spaceId));
          });
    }
    return adminSpaces;
  }

  @NotNull
  @Override
  public String getId() {
    return nakshaHub.getAdminStorage().getId();
  }


  @Override
  public int getHardCap() {
    return nakshaHub.getAdminStorage().getHardCap();
  }


  @NotNull
  @Override
  public IWriteSession newWriteSession(@Nullable SessionOptions options) {
    return new NHSpaceStorageWriter(nakshaHub, virtualSpaces, pipelineFactory, options);
  }

  @NotNull
  @Override
  public IReadSession newReadSession(@Nullable SessionOptions options) {
    return new NHSpaceStorageReader(nakshaHub, virtualSpaces, pipelineFactory, options);
  }

  @Override
  public @NotNull PlatformLock getLock() {
    throw new UnsupportedOperationException("Unsupported by NHSpaceStorage");
  }

  @Override
  public @NotNull NakshaStorage getConfig() {
    throw new UnsupportedOperationException("Unsupported by NHSpaceStorage");
  }

  @Override
  public long getNumber() {
    throw new UnsupportedOperationException("Unsupported by NHSpaceStorage");
  }

  public @NotNull DataEncoding getDataEncoding(@Nullable Object feature, @Nullable Object context) {
    throw new UnsupportedOperationException("Unsupported by NHSpaceStorage");
  }

  @Override
  public @Nullable JbDictionary getDictionary(@NotNull String id) {
    throw new UnsupportedOperationException("Unsupported by NHSpaceStorage");
  }

  @Override
  public @Nullable JbDictionary getEncodingDictionary(@Nullable Object feature, @Nullable Object context) {
    throw new UnsupportedOperationException("Unsupported by NHSpaceStorage");
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
