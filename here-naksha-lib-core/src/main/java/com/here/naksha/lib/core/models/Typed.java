/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
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

package com.here.naksha.lib.core.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.here.naksha.lib.core.extension.messages.ExtensionMessage;
import com.here.naksha.lib.core.models.geojson.implementation.Feature;
import com.here.naksha.lib.core.models.geojson.implementation.Geometry;
import com.here.naksha.lib.core.models.payload.events.clustering.Clustering;
import com.here.naksha.lib.core.models.payload.events.tweaks.Tweaks;
import com.here.naksha.lib.core.util.json.JsonSerializable;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Payload.class),
    @JsonSubTypes.Type(value = Geometry.class),
    @JsonSubTypes.Type(value = Clustering.class),
    @JsonSubTypes.Type(value = Tweaks.class),
    @JsonSubTypes.Type(value = Feature.class),
    @JsonSubTypes.Type(value = ExtensionMessage.class)
})
public interface Typed extends JsonSerializable {}
