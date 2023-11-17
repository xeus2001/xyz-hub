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
package com.here.naksha.lib.core.models.storage;

import static com.here.naksha.lib.core.exceptions.UncheckedException.unchecked;

import com.here.naksha.lib.core.util.json.Json;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import javax.annotation.concurrent.NotThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A codec that is able to encode and decode a feature from its basic parts. The implementation is not thread safe.
 *
 * @param <FEATURE> The feature type that can be processed by this codec.
 * @param <SELF>    The implementation type.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@NotThreadSafe
public abstract class FeatureCodec<FEATURE, SELF extends FeatureCodec<FEATURE, SELF>>
    extends FeatureBox<FEATURE, SELF> {

  /**
   * Tries to decode (disassemble) a feature set via {@link #setFeature(Object)} or {@link #withFeature(Object)} into its parts. Unless
   * decoding fails (raising an exception), the individual parts should thereafter be readable.
   *
   * <p>If the method raises an exception, the state is undefined and should be {@link #clear()}ed.
   *
   * @return this.
   */
  public abstract @NotNull SELF decodeParts();

  /**
   * Tries to encode (assemble) a new feature from the currently set parts. Unless encoding fails (raising an exception),
   * {@link #getFeature()} should return the new assembled feature.
   *
   * <p>If the method raises an exception, the state is undefined and should be {@link #clear()}ed.
   *
   * @return this.
   */
  public abstract @NotNull SELF encodeFeature();

  /**
   * Load the raw values (feature parts) for the given foreign codec into this code to re-encode.
   *
   * @param otherCodec The other codec from which to load the parts.
   * @return this.
   */
  public @NotNull SELF withParts(@NotNull FeatureCodec<?, ?> otherCodec) {
    op = otherCodec.op;
    id = otherCodec.id;
    uuid = otherCodec.uuid;
    featureType = otherCodec.featureType;
    propertiesType = otherCodec.propertiesType;
    json = otherCodec.json;
    wkb = otherCodec.wkb;
    geometry = otherCodec.geometry;
    return self();
  }

  /**
   * Clears the feature codec, that means all parts and the feature.
   *
   * @return this.
   */
  @Override
  public @NotNull SELF clear() {
    super.clear();
    id = null;
    uuid = null;
    featureType = null;
    propertiesType = null;
    wkb = null;
    geometry = null;
    return self();
  }

  /**
   * Clears all feature parts, but leaves the feature alive.
   *
   * @return this.
   */
  public @NotNull SELF clearParts() {
    id = null;
    uuid = null;
    featureType = null;
    propertiesType = null;
    wkb = null;
    geometry = null;
    return self();
  }

  /**
   * The {@code id} of the feature.
   */
  protected @Nullable String id;

  /**
   * The {@code uuid} of the feature.
   */
  protected @Nullable String uuid;
  /**
   * The {@code type} of the feature.
   */
  protected @Nullable String featureType;
  /**
   * The {@code type} of the feature properties.
   */
  protected @Nullable String propertiesType;
  /**
   * The JSON of the feature.
   */
  protected @Nullable String json;
  /**
   * The <link href="https://libgeos.org/specifications/wkb/">Extended WKB</b> encoded geometry.
   */
  protected byte @Nullable [] wkb;
  /**
   * The JTS geometry build from the {@link #wkb}.
   */
  protected @Nullable Geometry geometry;

  /**
   * Sets the given geometry and clears the WKB.
   *
   * @param geometry The geometry to set.
   * @return The previously set geometry.
   */
  @SuppressWarnings("unchecked")
  public <G extends Geometry> @Nullable G setGeometry(@Nullable Geometry geometry) {
    final Geometry old = getGeometry();
    this.geometry = geometry;
    this.wkb = null;
    return (G) old;
  }

  /**
   * Sets the given geometry and clears the WKB.
   *
   * @param geometry The geometry to load.
   * @return this.
   */
  public @NotNull SELF withGeometry(@Nullable Geometry geometry) {
    this.geometry = geometry;
    this.wkb = null;
    return self();
  }

  /**
   * Returns the decoded geometry.
   *
   * @param <G> The geometry-type being expected.
   * @return the decoded geometry.
   */
  @SuppressWarnings("unchecked")
  public <G extends Geometry> @Nullable G getGeometry() {
    if (geometry == null) {
      final byte[] wkb = this.wkb;
      if (wkb != null && wkb.length > 0) {
        try (final Json jp = Json.get()) {
          this.geometry = jp.wkbReader.read(wkb);
        } catch (ParseException e) {
          throw unchecked(e);
        }
      }
    }
    return (G) geometry;
  }

  /**
   * Sets the WKB and clears the geometry.
   *
   * @param wkb The <link href="https://libgeos.org/specifications/wkb/">Extended WKB</b> encoded geometry.
   * @return The previously set WKB.
   */
  public byte @Nullable [] setWkb(byte @Nullable [] wkb) {
    final byte[] old = this.wkb;
    this.geometry = null;
    this.wkb = wkb;
    return old;
  }

  /**
   * Sets the WKB and clears the geometry.
   *
   * @param wkb The <link href="https://libgeos.org/specifications/wkb/">Extended WKB</b> encoded geometry.
   * @return this.
   */
  public @NotNull SELF withWkb(byte @Nullable [] wkb) {
    setWkb(wkb);
    return self();
  }

  /**
   * Returns the geometry encoded as <link href="https://libgeos.org/specifications/wkb/">Extended WKB</b>.
   *
   * @return The <link href="https://libgeos.org/specifications/wkb/">Extended WKB</b> encoded geometry.
   */
  public byte @Nullable [] getWkb() {
    if (wkb == null) {
      final Geometry geometry = getGeometry();
      if (geometry != null) {
        try (final Json jp = Json.get()) {
          this.wkb = jp.wkbWriter.write(geometry);
        }
      }
    }
    return wkb;
  }

  /**
   * Returns the JSON of the feature.
   *
   * @return The JSON of the feature.
   */
  public @Nullable String getJson() {
    return json;
  }

  /**
   * Sets the JSON of the feature.
   *
   * @param json The JSON to set.
   * @return The previously set feature JSON.
   */
  public @Nullable String setJson(@Nullable String json) {
    final String old = this.json;
    this.json = json;
    return old;
  }

  /**
   * Sets the JSON of the feature.
   *
   * @param json The JSON to set.
   * @return this.
   */
  public @NotNull SELF withJson(@Nullable String json) {
    setJson(json);
    return self();
  }

  /**
   * Returns the {@code id} of the feature; if it has any.
   *
   * @return the {@code id} of the feature; if it has any.
   */
  public @Nullable String getId() {
    return id;
  }

  /**
   * Set the {@code id} of the feature. Often it is not necessary to set the {@code id}, because normally it is anyway encoded within the
   * JSON of the feature.
   *
   * @param id The {@code id} to set.
   * @return The previously set id.
   */
  public @Nullable String setId(@Nullable String id) {
    final String old = this.id;
    this.id = id;
    return old;
  }

  /**
   * Set the {@code id} of the feature. Often it is not necessary to set the {@code id}, because normally it is anyway encoded within the
   * JSON of the feature.
   *
   * @param id The {@code id} to set.
   * @return this.
   */
  public final @NotNull SELF withId(@Nullable String id) {
    setId(id);
    return self();
  }

  /**
   * Returns the {@code uuid} (unique state identifier) of the feature; if it has any.
   *
   * @return the {@code uuid} (unique state identifier) of the feature; if it has any.
   */
  public @Nullable String getUuid() {
    return uuid;
  }

  /**
   * Sets the {@code uuid} (unique state identifier) of the feature.
   *
   * @param uuid The new {@code uuid} to be set.
   * @return the previously set {@code uuid}.
   */
  public @Nullable String setUuid(@Nullable String uuid) {
    final String old = this.uuid;
    this.uuid = uuid;
    return old;
  }

  /**
   * Sets the {@code uuid} (unique state identifier) of the feature.
   *
   * @param uuid The new {@code uuid} to be set.
   * @return this.
   */
  public final @NotNull SELF withUuid(@Nullable String uuid) {
    setUuid(uuid);
    return self();
  }

  /**
   * Returns the feature type identifier, normally stored in the root of the feature ({@code "type"}).
   *
   * @return the feature type identifier.
   */
  public @Nullable String getFeatureType() {
    return featureType;
  }

  /**
   * Sets the feature type identifier, normally stored in the root of the feature ({@code "type"}).
   *
   * @param typeId The feature type identifier, normally stored in the root of the feature ({@code "type"}).
   * @return the previously set feature type identifier.
   */
  public @Nullable String setFeatureType(@Nullable String typeId) {
    final String old = this.featureType;
    this.featureType = typeId;
    return old;
  }

  /**
   * Sets the feature type identifier, normally stored in the root of the feature ({@code "type"}).
   *
   * @param type The feature type identifier, normally stored in the root of the feature ({@code "type"}).
   * @return this.
   */
  public final @NotNull SELF withFeatureType(@Nullable String type) {
    setFeatureType(type);
    return self();
  }

  /**
   * Returns the properties type identifier, normally stored in the properties of the feature ({@code "properties.type"}).
   *
   * @return the properties type identifier.
   */
  public @Nullable String getPropertiesType() {
    return propertiesType;
  }

  /**
   * Sets the properties type identifier, normally stored in the properties of the feature ({@code "properties.type"}).
   *
   * @param type The properties type identifier, normally stored in the properties of the feature ({@code "properties.type"}).
   * @return the previously set properties type identifier.
   */
  public @Nullable String setPropertiesType(@Nullable String type) {
    final String old = this.propertiesType;
    this.propertiesType = type;
    return old;
  }

  /**
   * Sets the properties type identifier, normally stored in the properties of the feature ({@code "properties.type"}).
   *
   * @param type The properties type identifier, normally stored in the properties of the feature ({@code "properties.type"}).
   * @return this.
   */
  public final @NotNull SELF withPropertiesType(@Nullable String type) {
    setPropertiesType(type);
    return self();
  }
}