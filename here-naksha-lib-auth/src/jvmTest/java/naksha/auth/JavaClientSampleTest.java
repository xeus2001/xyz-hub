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
package naksha.auth;

import static naksha.base.Platform.newList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import naksha.auth.action.ReadCollections;
import naksha.auth.attribute.CollectionAttributes;
import naksha.auth.attribute.NakshaAttributes;
import naksha.auth.attribute.ResourceAttributes;
import naksha.base.AnyListProxy;
import naksha.base.ListProxy;
import org.junit.jupiter.api.Test;

class JavaClientSampleTest {

  /** Demo on how to parse and deal with following ARM:
   *
   * {code @formatter:off}
   * {
   *    "someService": {
   *       "readCollections": [
   *          {
   *             "id": "someCollection",
   *             "storageId": "someStorage"
   *          },
   *          {
   *             "id": "otherCollection",
   *             "storageId": "otherStorage",
   *             "tags": ["tag1", "tag2"]
   *          }
   *       ]
   *    }
   * }
   * {@code @formatter:on}
   */
  @Test
  void armConstructionSample() {
    // Given
    AccessRightsMatrix arm = new AccessRightsMatrix()
        .withService(
            "someService",
            new ServiceAccessRights()
                .withAction(new ReadCollections()
                    .withAttributes(
                        new CollectionAttributes()
                            .id("someCollection")
                            .storageId("someStorage"),
                        new CollectionAttributes()
                            .id("otherCollection")
                            .storageId("otherStorage")
                            .tags("tag1", "tag2"))));

    // When
    ServiceAccessRights armService = arm.useService("someService");

    // Then:
    assertNotNull(armService);
    ListProxy<ResourceAttributes> attributes = armService.getResourceAttributesForAction(ReadCollections.NAME);
    assertNotNull(attributes);
    assertEquals(2, attributes.size());
    assertEquals("someCollection", attributes.get(0).get(NakshaAttributes.ID_KEY));
    assertEquals("someStorage", attributes.get(0).get(CollectionAttributes.STORAGE_ID_KEY));
    assertEquals("otherCollection", attributes.get(1).get(NakshaAttributes.ID_KEY));
    assertEquals("otherStorage", attributes.get(1).get(CollectionAttributes.STORAGE_ID_KEY));
    final ListProxy<Object> tags =
        assertInstanceOf(AnyListProxy.class, attributes.get(1).get(CollectionAttributes.TAGS_KEY));
    assertEquals(newList("tag1", "tag2"), tags.platformObject());
  }
}