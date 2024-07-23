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
package com.here.naksha.lib.core.models.geojson.coordinates.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;

import naksha.geo.GeometryProxy;
import naksha.geo.ProxyGeoUtil;
import naksha.model.NakshaFeatureProxy;
import org.junit.jupiter.api.Test;

public class JTSConverterTest {

  @Test
  public void test() throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(JTSConverterTest.class
        .getResource("/com/here/xyz/test/geometries.json")
        .toURI()));
    String featureText = new String(bytes);
    NakshaFeatureProxy feature = new ObjectMapper().readValue(featureText, NakshaFeatureProxy.class);

    GeometryProxy sourceGeometry = feature.getGeometry();
    org.locationtech.jts.geom.Geometry jtsGeometry = ProxyGeoUtil.toJtsGeometry(sourceGeometry);
    GeometryProxy targetGeometry = ProxyGeoUtil.toProxyGeometry(jtsGeometry);

    assertNotNull(targetGeometry);
  }
}
