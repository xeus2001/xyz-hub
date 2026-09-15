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

import static com.here.naksha.lib.core.HubInternalIdentifiers.EVENT_HANDLERS;
import static com.here.naksha.lib.core.HubInternalIdentifiers.SPACES;
import static naksha.base.NakshaExceptionKt.unsupportedOp;
import static naksha.model.util.RequestHelper.readFeaturesByIdRequest;
import static naksha.model.util.RequestHelper.readFeaturesByIdsRequest;
import static naksha.model.util.ResultHelper.readFeatureFromResponse;

import com.here.naksha.lib.core.EventPipeline;
import com.here.naksha.lib.core.IEventHandler;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.models.naksha.EventHandlerConfig;
import com.here.naksha.lib.core.models.naksha.Space;
import com.here.naksha.lib.handlers.AuthorizationEventHandler;
import com.here.naksha.lib.hub.EventPipelineFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import naksha.model.IReadSession;
import naksha.model.IStorage;
import naksha.model.NakshaContext;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.NakshaVersion;
import naksha.model.SessionOptions;
import naksha.model.StreamInfo;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaCatalog;
import naksha.model.request.*;
import naksha.model.util.ResultHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NHSpaceStorageReader implements IReadSession {

  private static final NakshaException NOT_SUPPORTED_ERROR = new NakshaException(
      new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Operation not supported by NHSpaceStorageReader"));

  private static final Logger logger = LoggerFactory.getLogger(NHSpaceStorageReader.class);

  /**
   * Singleton instance of NakshaHub storage implementation
   */
  protected final @NotNull INaksha nakshaHub;

  /**
   * List of Admin virtual spaces with relevant event handlers required to support event processing
   */
  protected final @NotNull Map<String, List<IEventHandler>> virtualSpaces;

  protected final @NotNull EventPipelineFactory pipelineFactory;

  /**
   * Runtime session information
   */
  protected final @Nullable SessionOptions sessionOptions;

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  public NHSpaceStorageReader(
      final @NotNull INaksha hub,
      final @NotNull Map<String, List<IEventHandler>> virtualSpaces,
      final @NotNull EventPipelineFactory pipelineFactory,
      final @Nullable SessionOptions sessionOptions) {
    this.nakshaHub = hub;
    this.virtualSpaces = virtualSpaces;
    this.pipelineFactory = pipelineFactory;
    this.sessionOptions =
        sessionOptions != null ? sessionOptions : SessionOptions.from(NakshaContext.currentContext(), false);
  }

  /**
   * Execute the given read-request.
   *
   * @param request input read request
   * @return the result.
   */
  @Override
  public @NotNull Response executeRead(@NotNull ReadRequest request) {
    if (request instanceof ReadFeatures readFeatures) {
      return executeReadFeatures(readFeatures);
    } else if (request instanceof ReadCollections readCollections) {
      return executeReadFeatures(readCollections.toReadFeatures());
    }
    throw unsupportedOp("Request with unsupported type " + request.getClass().getName());
  }

  private @NotNull Response executeReadFeatures(final @NotNull ReadFeatures rf) {
    String spaceId = rf.getCollectionId();
    logger.info("ReadFeatures Request against spaceId={}", spaceId);
    addSpaceIdToStreamInfo(spaceId);
    if (virtualSpaces.containsKey(spaceId)) {
      // Request is to read from Naksha Admin space
      return executeReadFeaturesFromAdminSpaces(rf, spaceId);
    } else {
      // Request is to read from Custom space
      return executeReadFeaturesFromCustomSpaces(rf);
    }
  }

  private @NotNull Response executeReadFeaturesFromAdminSpaces(
      final @NotNull ReadFeatures rf, final @NotNull String spaceId) {
    // Run pipeline against virtual space
    final EventPipeline pipeline = pipelineFactory.eventPipeline();
    final Response result = setupEventPipelineForAdminVirtualSpace(spaceId, pipeline);
    if (!(result instanceof SuccessResponse)) {
      return result;
    }
    return pipeline.sendEvent(rf);
  }

  protected @NotNull Response setupEventPipelineForAdminVirtualSpace(
      final @NotNull String spaceId, final @NotNull EventPipeline pipeline) {
    // add internal Admin resource specific event handlers
    final StringBuilder handlerTypes = new StringBuilder();
    for (final IEventHandler handler : virtualSpaces.get(spaceId)) {
      pipeline.addEventHandler(handler);
      if (handlerTypes.isEmpty()) {
        handlerTypes.append(handler.getClass().getSimpleName());
      } else {
        handlerTypes.append(",").append(handler.getClass().getSimpleName());
      }
    }
    logger.info("Handler types identified [{}]", handlerTypes);
    return new SuccessResponse();
  }

  private @NotNull Response executeReadFeaturesFromCustomSpaces(final @NotNull ReadFeatures rf) {
    final String spaceId = rf.getCollectionId();
    final EventPipeline eventPipeline = pipelineFactory.eventPipeline();
    final Response response = setupEventPipelineForSpaceId(spaceId, eventPipeline);
    if (!(response instanceof SuccessResponse)) {
      return response;
    }
    return eventPipeline.sendEvent(rf);
  }

  record SpaceAndHandlerConfigs(Space space, List<EventHandlerConfig> eventHandlerConfigs) {

  }

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  protected @NotNull Response setupEventPipelineForSpaceId(
      final @NotNull String spaceId,
      final @NotNull EventPipeline pipeline
  ) {
    Space space = null;
    Response spaceResponse = nakshaHub.getAdminStorage()
        .useReadSession(sessionOptions, reader -> reader.execute(readFeaturesByIdRequest(nakshaHub.getAdminMapId(), SPACES, spaceId)));
    if (spaceResponse instanceof ErrorResponse er) {
      return er;
    } else if (spaceResponse instanceof SuccessResponse successResponse) {
      space = readFeatureFromResponse(successResponse, Space.class);
    } else {
      return new ErrorResponse(
          NakshaError.ILLEGAL_STATE,
          "Unexpected response type: " + spaceResponse.getClass().getName());
    }
    if (space == null) {
      return new ErrorResponse(NakshaError.NOT_FOUND, "Space not found: " + spaceId);
    }
    List<String> eventHandlerIds = space.getEventHandlerIds();
    if (eventHandlerIds == null || eventHandlerIds.isEmpty()) {
      return new ErrorResponse(NakshaError.NOT_FOUND, "No associated handler");
    }
    logger.info("Handler IDs identified {}", eventHandlerIds.toArray());

    List<EventHandlerConfig> eventHandlers = null;
    Response handlersResponse = nakshaHub.getAdminStorage()
        .useReadSession(sessionOptions,
            reader -> reader.execute(readFeaturesByIdsRequest(nakshaHub.getAdminMapId(), EVENT_HANDLERS, eventHandlerIds)));
    if (handlersResponse instanceof ErrorResponse er) {
      return er;
    } else if (handlersResponse instanceof SuccessResponse successResponse) {
      try {
        eventHandlers = ResultHelper.extractResponseItems(successResponse, EventHandlerConfig.class);
        if (eventHandlers.size() != space.getEventHandlerIds().size()) {
          return new ErrorResponse(
              NakshaError.EXCEPTION, "Not all EventHandlers found for space : " + spaceId);
        }
      } catch (NoSuchElementException e) {
        return new ErrorResponse(NakshaError.EXCEPTION, "No handlers associated with space : " + spaceId);
      }
    } else {
      return new ErrorResponse(
          NakshaError.ILLEGAL_STATE,
          "Unexpected response type: " + handlersResponse.getClass().getName());
    }

    // Ensure the order of the event handlers is preserved
    final Space finalSpace = space;
    eventHandlers.sort(
        Comparator.comparingInt(o -> finalSpace.getEventHandlerIds().indexOf(o.getId())));

    // Instantiate IEventHandler (from EventHandler object), using NakshaHub and Space details
    final List<IEventHandler> handlerImpls = new ArrayList<>();
    for (final EventHandlerConfig eventHandler : eventHandlers) {
      if (!eventHandler.isActive()) {
        logger.warn("Skipping inactive event handler {}", eventHandler.getId());
        continue;
      }
      handlerImpls.add(eventHandler.newInstance(nakshaHub, space));
    }
    if (handlerImpls.isEmpty()) {
      return new ErrorResponse(NakshaError.NOT_FOUND, "No active EventHandlers found for space : " + spaceId);
    }

    // Create pipeline and add all applicable event handlers
    // TODO : AuthorizationHandler will need information about Space storageId as well
    pipeline.addEventHandler(new AuthorizationEventHandler(nakshaHub, space, eventHandlers));
    final StringBuilder handlerTypes = new StringBuilder();
    for (final IEventHandler handler : handlerImpls) {
      pipeline.addEventHandler(handler);
      if (handlerTypes.isEmpty()) {
        handlerTypes.append(handler.getClass().getSimpleName());
      } else {
        handlerTypes.append(",").append(handler.getClass().getSimpleName());
      }
    }
    logger.info("Handler types identified [{}]", handlerTypes);
    return new SuccessResponse();
  }


  /**
   * Closes the session, returns the underlying connection back to the connection pool. Any method of the session will from now on throw an
   * {@link IllegalStateException}.
   */
  @Override
  @ApiStatus.AvailableSince(NakshaVersion.v2_0_7)
  public void close() {
  }

  protected void addSpaceIdToStreamInfo(final @Nullable String spaceId) {
    final StreamInfo streamInfo = sessionOptions.streamInfo;
    if (streamInfo != null) {
      streamInfo.withSpaceIdIfMissing(spaceId);
    }
  }

  @Override
  public int getSocketTimeout() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public void setSocketTimeout(int i) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public int getStmtTimeout() {
    return 0;
  }

  @Override
  public void setStmtTimeout(int i) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public int getLockTimeout() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public void setLockTimeout(int i) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public boolean isClosed() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @NotNull IStorage getStorage() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @NotNull SessionOptions getOptions() {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @Nullable NakshaCatalog getCatalogById(@NotNull String catalogId, boolean allowTombstone) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @Nullable NakshaCatalog getCatalogByNumber(int catalogNumber, boolean allowTombstone) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @Nullable NakshaCollection getCollectionById(
      @NotNull NakshaCatalog map,
      @NotNull String collectionId,
      boolean allowTombstone) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public void loadTuples(@NotNull List<? extends FeatureTuple> featureTuples, int from, int to) {
    throw NOT_SUPPORTED_ERROR;
  }

  @Override
  public @Nullable NakshaCollection getCollectionByNumber(
      @NotNull NakshaCatalog catalog,
      int collectionNumber,
      boolean allowTombstone) {
    throw NOT_SUPPORTED_ERROR;
  }

}
