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
package com.here.naksha.app.service;

import static com.here.naksha.app.common.CommonApiTestSetup.*;
import static com.here.naksha.app.common.ResponseAssertions.assertThat;
import static com.here.naksha.app.common.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

import com.here.naksha.app.common.ApiTest;
import com.here.naksha.app.common.NakshaTestWebClient;
import com.here.naksha.app.service.models.FeatureCollectionRequest;
import com.here.naksha.lib.core.models.geojson.implementation.XyzFeature;
import com.here.naksha.lib.core.models.geojson.implementation.XyzFeatureCollection;
import com.here.naksha.lib.core.models.geojson.implementation.XyzReference;
import com.here.naksha.lib.core.models.naksha.Space;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.ArraySizeComparator;

public class ValDryRunTest extends ApiTest {

  private static final NakshaTestWebClient nakshaClient = new NakshaTestWebClient();

  private static final String SPACE_ID = "local-space-4-val-dry-run";

  @BeforeAll
  static void setup() throws URISyntaxException, IOException, InterruptedException {
    createHandler(nakshaClient, "ValDryRun/setup", "create_context_loader_handler.json");
    createHandler(nakshaClient, "ValDryRun/setup", "create_validation_handler.json");
    createHandler(nakshaClient, "ValDryRun/setup", "create_endorsement_handler.json");
    createHandler(nakshaClient, "ValDryRun/setup", "create_echo_handler.json");
    createSpace(nakshaClient, "ValDryRun/setup");
  }

  private void additionalCustomAssertions_tc3000(final @NotNull String reqBody, final @NotNull String resBody)
      throws JSONException {
    final FeatureCollectionRequest collectionRequest = parseJson(reqBody, FeatureCollectionRequest.class);
    final XyzFeatureCollection collectionResponse = parseJson(resBody, XyzFeatureCollection.class);
    final List<String> updatedIds = collectionResponse.getUpdated();
    final List<XyzFeature> features = collectionResponse.getFeatures();
    final List<XyzFeature> violations = collectionResponse.getViolations();
    assertEquals(
        updatedIds.size(), features.size(), "Mismatch between updated and features list size in the response");
    final String newFeatureId = features.get(2).getId();
    assertNotNull(newFeatureId, "Feature Id must not be null");
    for (int i = 3; i <= 5; i++) {
      final XyzFeature violation = violations.get(i);
      final List<XyzReference> references = violation.getProperties().getReferences();
      assertNotNull(references, "References missing for violation at idx " + i);
      for (final XyzReference reference : references) {
        assertNotNull(reference.getId(), "Id missing in references for violation at idx " + i);
        assertEquals(newFeatureId, reference.getId(), "Violation referenced featured id doesn't match");
      }
    }
  }

  @Test
  void tc3000_testValDryRunReturningViolations() throws Exception {
    // Test API : POST /hub/spaces/{spaceId}/features
    // Validate features returned with mock violations
    final String streamId = UUID.randomUUID().toString();

    // Given: PUT features request
    final String bodyJson = loadFileOrFail("ValDryRun/TC3000_WithViolations/upsert_features.json");
    final String expectedBodyPart = loadFileOrFail("ValDryRun/TC3000_WithViolations/feature_response_part.json");

    // When: Request is submitted to NakshaHub Space Storage instance
    final HttpResponse<String> response =
        nakshaClient.post("hub/spaces/" + SPACE_ID + "/features", bodyJson, streamId);

    // Then: Perform standard assertions
    assertThat(response)
            .hasStatus(200)
            .hasStreamIdHeader(streamId)
            .hasJsonBody(expectedBodyPart, "Validation dry-run response body doesn't match");
    // Then: Perform additional custom assertions for matching violation references
    additionalCustomAssertions_tc3000(bodyJson, response.body());
  }

  @Test
  void tc3001_testValDryRunNoViolations() throws Exception {
    // Test API : POST /hub/spaces/{spaceId}/features
    // Validate features returned without any violations
    final String streamId = UUID.randomUUID().toString();

    // Given: PUT features request
    final String bodyJson = loadFileOrFail("ValDryRun/TC3001_WithoutViolations/upsert_features.json");
    final String expectedBodyPart = loadFileOrFail("ValDryRun/TC3001_WithoutViolations/feature_response_part.json");

    // When: Request is submitted to NakshaHub Space Storage instance
    final HttpResponse<String> response =
        nakshaClient.post("hub/spaces/" + SPACE_ID + "/features", bodyJson, streamId);

    // Then: Perform standard assertions
    assertThat(response)
            .hasStatus(200)
            .hasStreamIdHeader(streamId)
            .hasJsonBody(expectedBodyPart, "Validation dry-run response body doesn't match");
    // Then: Perform additional custom assertions to ensure no violations returned
    final XyzFeatureCollection collectionResponse = parseJson(response.body(), XyzFeatureCollection.class);
    assertNull(collectionResponse.getViolations(), "No violations were expected");
  }

  @Test
  void tc3002_testValDryRunUnsupportedOperation() throws Exception {
    // Test API : DELETE /hub/spaces/{spaceId}/features
    // Validate request gets rejected as validation is not supported for DELETE endpoint
    final String streamId = UUID.randomUUID().toString();

    // Given: DELETE features request
    final String expectedBodyPart =
        loadFileOrFail("ValDryRun/TC3002_UnsupportedOperation/feature_response_part.json");

    // When: Request is submitted to NakshaHub Space Storage instance
    final HttpResponse<String> response =
        nakshaClient.delete("hub/spaces/" + SPACE_ID + "/features?id=some-feature-id", streamId);

    // Then: Perform standard assertions
    assertThat(response)
            .hasStatus(501)
            .hasStreamIdHeader(streamId)
            .hasJsonBody(expectedBodyPart, "Validation dry-run response body doesn't match");
  }

}