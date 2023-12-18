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
package com.here.naksha.lib.handlers.val;

import com.here.naksha.lib.core.IEvent;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.NakshaContext;
import com.here.naksha.lib.core.exceptions.XyzErrorException;
import com.here.naksha.lib.core.models.XyzError;
import com.here.naksha.lib.core.models.geojson.implementation.XyzFeature;
import com.here.naksha.lib.core.models.naksha.EventHandler;
import com.here.naksha.lib.core.models.naksha.EventHandlerProperties;
import com.here.naksha.lib.core.models.naksha.EventTarget;
import com.here.naksha.lib.core.models.storage.*;
import com.here.naksha.lib.core.util.json.JsonSerializable;
import com.here.naksha.lib.handlers.AbstractEventHandler;
import com.here.naksha.lib.handlers.util.HandlerUtil;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoHandler extends AbstractEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(EchoHandler.class);
  protected @NotNull EventHandler eventHandler;
  protected @NotNull EventTarget<?> eventTarget;
  protected @NotNull EventHandlerProperties properties;

  public EchoHandler(
      final @NotNull EventHandler eventHandler,
      final @NotNull INaksha hub,
      final @NotNull EventTarget<?> eventTarget) {
    super(hub);
    this.eventHandler = eventHandler;
    this.eventTarget = eventTarget;
    this.properties = JsonSerializable.convert(eventHandler.getProperties(), EventHandlerProperties.class);
  }

  /**
   * The method invoked by the event-pipeline to process custom Storage specific read/write operations
   *
   * @param event the event to process.
   * @return the result.
   */
  @Override
  public @NotNull Result processEvent(@NotNull IEvent event) {
    final NakshaContext ctx = NakshaContext.currentContext();
    final Request<?> request = event.getRequest();

    logger.info("Handler received request {}", request.getClass().getSimpleName());
    return null;
  }

  protected @NotNull Result echoHandler(final @NotNull Request<?> request) {
    if (!(request instanceof ContextWriteFeatures<?, ?, ?, ?, ?> cwf))
      throw new XyzErrorException(
          XyzError.NOT_IMPLEMENTED,
          "Unsupported request type in echoHandler - "
              + request.getClass().getSimpleName());

    // Extract violations (if to be persisted separately)
    final List<?> inputViolations = cwf.getViolations();
    List<XyzFeature> outputViolations = null;
    if (inputViolations != null) {
      for (final Object obj : inputViolations) {
        if (!(obj instanceof XyzFeature violation))
          throw new XyzErrorException(
              XyzError.EXCEPTION,
              "Unexpected violation type while creating endorsement request - "
                  + obj.getClass().getSimpleName());
        if (outputViolations == null) outputViolations = new ArrayList<>();
        // Add violation to output list
        outputViolations.add(violation);
      }
    }

    // Prepare list of codecs (with op as UPDATED), as if features were persisted in DB
    final List<XyzFeature> features = new ArrayList<>();
    for (final Object obj : cwf.features) {
      if (!(obj instanceof XyzFeatureCodec codec)) {
        throw new XyzErrorException(
            XyzError.NOT_IMPLEMENTED,
            "Unsupported feature codec type in echo handler - "
                + obj.getClass().getSimpleName());
      }
      features.add(codec.getFeature());
    }
    return HandlerUtil.createContextResultFromFeatureList(features, cwf.getContext(), outputViolations);
  }
}
