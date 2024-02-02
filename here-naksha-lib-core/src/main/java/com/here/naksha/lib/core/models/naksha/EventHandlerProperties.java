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
package com.here.naksha.lib.core.models.naksha;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.here.naksha.lib.core.NakshaVersion;
import com.here.naksha.lib.core.models.geojson.implementation.XyzProperties;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.Nullable;

/**
 * Default variant of EventHandler properties supported by Naksha - default storage handler
 */
@AvailableSince(NakshaVersion.v2_0_7)
public class EventHandlerProperties extends XyzProperties {

  @AvailableSince(NakshaVersion.v2_0_7)
  public static final String STORAGE_ID = "storageId";

  @AvailableSince(NakshaVersion.v2_0_7)
  public static final String COLLECTION = "collection";

  /**
   * To associate EventHandler with specific {@link Storage} that it should operate against.
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  @JsonProperty(STORAGE_ID)
  private @Nullable String storageId;

  /**
   * By default, the backend xyz collection details specified at EventHandler level is used,
   * ONLY if it is not available at {@link SpaceProperties} level.
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  @JsonProperty(COLLECTION)
  private @Nullable XyzCollection xyzCollection;

  /**
   * Create new EventHandler properties with storageId and collection details
   *
   * @param xyzCollection details of backend xyz collection
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  @JsonCreator
  public EventHandlerProperties(
      final @JsonProperty(STORAGE_ID) @Nullable String storageId,
      final @JsonProperty(COLLECTION) @Nullable XyzCollection xyzCollection) {
    this.storageId = storageId;
    this.xyzCollection = xyzCollection;
  }

  public @Nullable XyzCollection getXyzCollection() {
    return xyzCollection;
  }

  public void setXyzCollection(final @JsonProperty(COLLECTION) @Nullable XyzCollection xyzCollection) {
    this.xyzCollection = xyzCollection;
  }

  public @Nullable String getStorageId() {
    return storageId;
  }

  public void setStorageId(final @Nullable String storageId) {
    this.storageId = storageId;
  }
}