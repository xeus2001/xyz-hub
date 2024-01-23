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
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceIdHandler extends AbstractEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(SourceIdHandler.class);
  private static final String TAG_PREFIX = "xyz_source_id_";
  private static final String NS_COM_HERE_MOM_META = "@ns:com:here:mom:meta";
  private static final String SOURCE_ID = "sourceId";

  protected @NotNull EventHandler eventHandler;
  protected @NotNull EventTarget<?> eventTarget;
  protected @NotNull EventHandlerProperties properties;

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
      Optional.ofNullable(readRequest.getPropertyOp())
          .ifPresent(t -> this.replaceSourceIdPropertyWithTagSearch(t, Collections.emptyList(), -1));
    } else if (request instanceof WriteXyzFeatures writeRequest) {
      writeRequest.features.stream()
          .map(XyzFeatureCodec::getFeature)
          .filter(Objects::nonNull)
          .peek(this::removeSourceIdTagsIfAny)
          .forEachOrdered(this::setSourceIdTags);
    }

    return event.sendUpstream(request);
  }

  private void setSourceIdTags(XyzFeature feature) {
    XyzProperties properties = feature.getProperties();
    getSourceIdFromFeature(properties)
        .ifPresent(sourceId -> properties.getXyzNamespace().addTag(TAG_PREFIX + sourceId, false));
  }

  private void removeSourceIdTagsIfAny(XyzFeature feature) {
    feature.getProperties().getXyzNamespace().removeTagsWithPrefix(TAG_PREFIX);
  }

  private Optional<String> getSourceIdFromFeature(XyzProperties properties) {
    try {
      Map<String, Object> momMetaNs = (Map<String, Object>) properties.get(NS_COM_HERE_MOM_META);
      Object sourceId = momMetaNs.get(SOURCE_ID);
      return Optional.ofNullable(sourceId).map(Object::toString);
    } catch (ClassCastException exception) {
      return Optional.empty();
    }
  }

  private void replaceSourceIdPropertyWithTagSearch(POp propertyOperation, List<POp> parentCollection, int index) {

    if (propertyOperation.getPropertyRef() == null
        && propertyOperation.children() != null
        && !propertyOperation.children().isEmpty()
        && !OpType.NOT.equals(propertyOperation.op())) {

      List<@NotNull POp> children = propertyOperation.children();

      for (int i = 0; i < children.size(); i++) {
        replaceSourceIdPropertyWithTagSearch(children.get(i), children, i);
      }

    } else {
      handlePropertyOperationLeaf(propertyOperation).ifPresent(pOp -> parentCollection.set(index, pOp));
    }
  }

  private Optional<POp> handlePropertyOperationLeaf(POp propertyOperation) {
    if (OpType.NOT.equals(propertyOperation.op())
        && propertyOperation.children() != null
        && !propertyOperation.children().isEmpty()) {

      POp nestedPropertyOperation = propertyOperation.children().get(0);
      POp negatedPropertyOperation = transformOperationIntoTagOperation(nestedPropertyOperation);

      return Optional.of(POp.not(negatedPropertyOperation));

    } else if (propertyReferenceEqualsSourceId(propertyOperation.getPropertyRef())
        && propertyOperation.getValue() != null) {

      return Optional.of(transformOperationIntoTagOperation(propertyOperation));
    }
    return Optional.empty();
  }

  private boolean propertyReferenceEqualsSourceId(PRef pRef) {
    List<@NotNull String> path = pRef.getPath();
    return path.size() == 3 && path.containsAll(List.of("properties", NS_COM_HERE_MOM_META, SOURCE_ID));
  }

  private POp transformOperationIntoTagOperation(POp propertyOperation) {

    if (propertyOperation.op().equals(POpType.EQ) || propertyOperation.op().equals(POpType.CONTAINS)) {

      return POp.exists(PRef.tag(TAG_PREFIX + propertyOperation.getValue()));
    }

    return propertyOperation;
  }
}
