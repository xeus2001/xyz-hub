/*
 * Copyright (C) 2017-2021 HERE Europe B.V.
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

package com.here.xyz.hub;

import com.here.xyz.httpconnector.PsqlHttpConnectorVerticle;
import com.here.xyz.httpconnector.config.MaintenanceClient;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;

public class HttpConnector extends Core {

  private static final Logger logger = LogManager.getLogger();

  protected HttpConnector(@Nullable VertxOptions vertxOptions) throws IOException {
    super(vertxOptions);
  }

  public static void main(String[] args) {
    VertxOptions vertxOptions = new VertxOptions()
            .setWorkerPoolSize(NumberUtils.toInt(System.getenv(Core.VERTX_WORKER_POOL_SIZE), 128))
            .setPreferNativeTransport(true)
            .setBlockedThreadCheckInterval(TimeUnit.MINUTES.toMillis(15));
    //initialize(vertxOptions, false, "connector-config.json", HttpConnector::onConfigLoaded );
  }

  private static void onConfigLoaded(JsonObject jsonConfig) {
    final DeploymentOptions options = new DeploymentOptions()
            .setConfig(jsonConfig)
            .setWorker(false)
            .setInstances(Runtime.getRuntime().availableProcessors() * 2);

//    vertx.deployVerticle(PsqlHttpConnectorVerticle.class, options, result -> {
//      if (result.failed()) {
//        logger.error("Unable to deploy the verticle.");
//        System.exit(1);
//      }
//      logger.info("The http-connector is up and running on port " + jsonConfig.getInteger("HTTP_PORT") );
//    });
  }
}
