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

package com.here.xyz.models.geojson.implementation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.here.xyz.Extensible;
import com.vividsolutions.jts.awt.PointShapeFactory.X;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for more specialized properties that hold the standard XYZ namespace.
 *
 * @param <SELF> this.
 */
public abstract class XyzProperties<SELF extends XyzProperties<SELF>> extends Extensible<SELF> {

  protected XyzProperties() {
    xyzNamespace = new XyzNamespace();
  }

  @JsonProperty(XyzNamespace.XYZ_NAMESPACE)
  @JsonInclude(Include.NON_NULL)
  private @NotNull XyzNamespace xyzNamespace;

  /**
   * Returns the XYZ namespace.
   *
   * @return The XYZ namespace.
   */
  public @NotNull XyzNamespace getXyzNamespace() {
    return xyzNamespace;
  }

  /**
   * Sets the XYZ namespace.
   *
   * @param xyzNamespace The XYZ namespace.
   */
  public void setXyzNamespace(@NotNull XyzNamespace xyzNamespace) {
    this.xyzNamespace = xyzNamespace;
  }

  /**
   * Sets the XYZ namespace.
   *
   * @param xyzNamespace The namespace to set.
   * @return this.
   */
  public @NotNull SELF withXyzNamespace(@NotNull XyzNamespace xyzNamespace) {
    this.xyzNamespace = xyzNamespace;
    return self();
  }

}
