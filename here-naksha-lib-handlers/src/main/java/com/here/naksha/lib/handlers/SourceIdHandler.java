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
package com.here.naksha.lib.handlers;

import com.here.naksha.lib.core.IEvent;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.models.geojson.implementation.XyzFeature;
import com.here.naksha.lib.core.models.geojson.implementation.XyzProperties;
import com.here.naksha.lib.core.models.naksha.EventHandler;
import com.here.naksha.lib.core.models.naksha.EventHandlerProperties;
import com.here.naksha.lib.core.models.naksha.EventTarget;
import com.here.naksha.lib.core.models.storage.*;
import com.here.naksha.lib.core.util.json.JsonSerializable;
import com.here.naksha.lib.handlers.util.PropertyOperationUtil;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceIdHandler extends AbstractEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(SourceIdHandler.class);
  private static final String TAG_PREFIX = "xyz_source_id_";
  private static final String SOURCE_ID = "sourceId";
  public static final int PREF_PATHS_SIZE = 3;

  private @NotNull EventHandler eventHandler;
  private @NotNull EventTarget<?> eventTarget;
  private @NotNull EventHandlerProperties properties;

  public SourceIdHandler(
      final @NotNull EventHandler eventHandler,
      final @NotNull INaksha hub,
      final @NotNull EventTarget<?> eventTarget) {
    super(hub);
    this.eventHandler = eventHandler;
    this.eventTarget = eventTarget;
    this.properties = JsonSerializable.convert(eventHandler.getProperties(), EventHandlerProperties.class);
  }

  @Override
  public @NotNull Result processEvent(@NotNull IEvent event) {

    final Request<?> request = event.getRequest();
    logger.info("Handler received request {}", request.getClass().getSimpleName());
    if (request instanceof ReadFeatures readRequest) {
      transformPropertyOperation(readRequest);
    } else if (request instanceof WriteXyzFeatures writeRequest) {
      writeRequest.features.stream()
          .map(XyzFeatureCodec::getFeature)
          .filter(Objects::nonNull)
          .forEachOrdered(this::setSourceIdTags);
    }

    return event.sendUpstream(request);
  }

  private void transformPropertyOperation(ReadFeatures readRequest) {

    if (readRequest.getPropertyOp() == null) {
      return;
    }

    POp propertyOp = readRequest.getPropertyOp();

    if (propertyOp.children() != null) {
      PropertyOperationUtil.transformPropertyInPropertyOperationTree(
          propertyOp, SourceIdHandler::mapIntoTagOperation);
    } else {
      mapIntoTagOperation(propertyOp).ifPresent(readRequest::setPropertyOp);
    }
  }

  private void setSourceIdTags(XyzFeature feature) {
    XyzProperties properties = feature.getProperties();
    getSourceIdFromFeature(properties).ifPresent(sourceId -> updateTagsWithSourceIdProperty(properties, sourceId));
  }

  private void updateTagsWithSourceIdProperty(XyzProperties properties, String sourceId) {
    properties.getXyzNamespace().removeTagsWithPrefix(TAG_PREFIX);
    properties.getXyzNamespace().addTag(TAG_PREFIX + sourceId, false);
  }

  private Optional<String> getSourceIdFromFeature(XyzProperties properties) {
    try {
      Map<String, Object> momMetaNs = (Map<String, Object>) properties.get(XyzProperties.HERE_META_NS);
      Object sourceId = momMetaNs.get(SOURCE_ID);
      return Optional.ofNullable(sourceId).map(Object::toString);
    } catch (ClassCastException exception) {
      return Optional.empty();
    }
  }

  public static Optional<POp> mapIntoTagOperation(POp propertyOperation) {

    if (sourceIdTransformationCapable(propertyOperation) && operationTypeAllowed(propertyOperation)) {
      return Optional.of(POp.exists(PRef.tag(TAG_PREFIX + propertyOperation.getValue())));
    }

    return Optional.empty();
  }

  private static boolean propertyReferenceEqualsSourceId(PRef pRef) {
    List<@NotNull String> path = pRef.getPath();
    return path.size() == PREF_PATHS_SIZE
        && path.containsAll(List.of(XyzFeature.PROPERTIES, XyzProperties.HERE_META_NS, SOURCE_ID));
  }

  private static boolean sourceIdTransformationCapable(POp propertyOperation) {
    return propertyReferenceEqualsSourceId(propertyOperation.getPropertyRef())
        && propertyOperation.getValue() != null
        && propertyOperation.children() == null;
  }

  private static boolean operationTypeAllowed(POp propertyOperation) {
    return propertyOperation.op().equals(POpType.EQ)
        || propertyOperation.op().equals(POpType.CONTAINS);
  }
}