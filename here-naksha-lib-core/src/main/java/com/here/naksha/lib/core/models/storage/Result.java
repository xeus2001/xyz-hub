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

import static com.here.naksha.lib.core.LibraryConstants.DEFAULT_HEAP_CACHE_CURSOR_LIMIT;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.here.naksha.lib.core.exceptions.NoCursor;
import com.here.naksha.lib.core.models.Typed;
import com.here.naksha.lib.core.models.geojson.implementation.XyzFeature;
import com.here.naksha.lib.core.models.naksha.XyzCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All results must extend this abstract base class.
 */
public abstract class Result implements Typed, AutoCloseable {

  /**
   * Member variable that can be set to the cursor.
   */
  @JsonIgnore
  protected @Nullable ForwardCursor<?, ?> cursor;

  /**
   * Return the cursor using a custom codec. If no cursor is available, throws an exception to handle result. This is
   * to simplify result processing when needed: <pre>{@code
   * try (ForwardCursor<Foo, FooCodec> cursor = session.execute(request).cursor(FooFactory.get())) {
   *   for (Foo feature : cursor) {
   *     ...
   *   }
   * } catch (NoCursor e) {
   *   if (e.result instanceof SuccessResult) ...
   *   if (e.result instanceof ErrorResult) ...
   * }}</pre>
   *
   * @param codecFactory The factory of the codec to use.
   * @param <FEATURE>    The feature-type the codec produces.
   * @param <CODEC>      The codec-type to use.
   * @return the cursor.
   */
  @JsonIgnore
  public <FEATURE, CODEC extends FeatureCodec<FEATURE, CODEC>> @NotNull ForwardCursor<FEATURE, CODEC> cursor(
      @NotNull FeatureCodecFactory<FEATURE, CODEC> codecFactory) throws NoCursor {
    if (cursor != null) {
      if (cursor instanceof HeapCacheCursor) {
        cursor = ((HeapCacheCursor<?, ?>) cursor).getOriginalCursor();
      }
      return cursor.withCodecFactory(codecFactory, true);
    }
    throw new NoCursor(this);
  }

  /**
   * Converts current cursor to {@link MutableCursor} and loads next data from point where current cursor is.
   * Calling it multiple times will return same cursor with same cached data.
   *
   * @param codecFactory
   * @return
   * @param <FEATURE>
   * @param <CODEC>
   * @throws NoCursor
   */
  @SuppressWarnings("unchecked")
  @JsonIgnore
  public <FEATURE, CODEC extends FeatureCodec<FEATURE, CODEC>> @NotNull MutableCursor<FEATURE, CODEC> mutableCursor(
      @NotNull FeatureCodecFactory<FEATURE, CODEC> codecFactory, long limit) throws NoCursor {
    if (cursor != null) {
      if (!(cursor instanceof HeapCacheCursor)) {
        cursor = new HeapCacheCursor<>(codecFactory, limit, cursor);
      }
      return (MutableCursor<FEATURE, CODEC>) cursor;
    }
    throw new NoCursor(this);
  }

  /**
   * {@link MutableCursor} with original cursor codec factory.
   * @see #mutableCursor(FeatureCodecFactory, long)
   *
   * @return
   * @param <FEATURE>
   * @param <CODEC>
   * @throws NoCursor
   */
  @SuppressWarnings("unchecked")
  @JsonIgnore
  public <FEATURE, CODEC extends FeatureCodec<FEATURE, CODEC>> @NotNull MutableCursor<FEATURE, CODEC> mutableCursor(
      long limit) throws NoCursor {
    if (cursor != null) {
      return (MutableCursor<FEATURE, CODEC>) mutableCursor(cursor.codecFactory, limit);
    }
    throw new NoCursor(this);
  }

  /**
   * {@link MutableCursor} with original cursor codec factory and {@link com.here.naksha.lib.core.LibraryConstants#DEFAULT_HEAP_CACHE_CURSOR_LIMIT} rows cached.
   * If default is not suitable for you, please change {@link com.here.naksha.lib.core.LibraryConstants#DEFAULT_HEAP_CACHE_CURSOR_LIMIT},
   * or use {@link #mutableCursor(long)} to declare different cache size.
   *
   * @return
   * @param <FEATURE>
   * @param <CODEC>
   * @throws NoCursor
   */
  @SuppressWarnings("unchecked")
  @JsonIgnore
  public <FEATURE, CODEC extends FeatureCodec<FEATURE, CODEC>> @NotNull MutableCursor<FEATURE, CODEC> mutableCursor()
      throws NoCursor {
    if (cursor != null) {
      return (MutableCursor<FEATURE, CODEC>) mutableCursor(cursor.codecFactory, DEFAULT_HEAP_CACHE_CURSOR_LIMIT);
    }
    throw new NoCursor(this);
  }

  /**
   * Converts current cursor to {@link SeekableCursor} and loads next data from point where current cursor is.
   * Calling it multiple times will return same cursor with same cached data.
   *
   * @param codecFactory
   * @return
   * @param <FEATURE>
   * @param <CODEC>
   * @throws NoCursor
   */
  @SuppressWarnings("unchecked")
  @JsonIgnore
  public <FEATURE, CODEC extends FeatureCodec<FEATURE, CODEC>> @NotNull SeekableCursor<FEATURE, CODEC> seekableCursor(
      @NotNull FeatureCodecFactory<FEATURE, CODEC> codecFactory, long limit) throws NoCursor {
    if (cursor != null) {
      if (!(cursor instanceof HeapCacheCursor)) {
        cursor = new HeapCacheCursor<>(codecFactory, limit, cursor);
      }
      return (SeekableCursor<FEATURE, CODEC>) cursor;
    }
    throw new NoCursor(this);
  }

  /**
   * Converts current cursor to {@link SeekableCursor} and loads next data from point where current cursor is.
   * Calling it multiple times will return same cursor with same cached data.
   *
   * @return
   * @param <FEATURE>
   * @param <CODEC>
   * @throws NoCursor
   */
  @SuppressWarnings("unchecked")
  @JsonIgnore
  public <FEATURE, CODEC extends FeatureCodec<FEATURE, CODEC>> @NotNull SeekableCursor<FEATURE, CODEC> seekableCursor(
      long limit) throws NoCursor {
    if (cursor != null) {
      return (SeekableCursor<FEATURE, CODEC>) seekableCursor(cursor.codecFactory, limit);
    }
    throw new NoCursor(this);
  }

  /**
   * Return the cursor using the {@link XyzFeatureCodecFactory}, if any cursor is available. If no cursor is available, throws an exception
   * to handle result. This is to simplify result processing when needed: <pre>{@code
   * try (ForwardCursor<XyzFeature, XyzFeatureCodec> cursor = session.execute(request).getXyzFeatureCursor()) {
   *   for (XyzFeature feature : cursor) {
   *     ...
   *   }
   * } catch (NoCursor e) {
   *   if (e.result instanceof SuccessResult) ...
   *   if (e.result instanceof ErrorResult) ...
   * }}</pre>
   *
   * @return the cursor.
   */
  @JsonIgnore
  public @NotNull ForwardCursor<XyzFeature, XyzFeatureCodec> getXyzFeatureCursor() throws NoCursor {
    if (cursor != null) {
      if (cursor instanceof HeapCacheCursor) {
        cursor = ((HeapCacheCursor<?, ?>) cursor).getOriginalCursor();
      }
      return cursor.withCodecFactory(XyzFeatureCodecFactory.get(), false);
    }
    throw new NoCursor(this);
  }

  /**
   * Returns {@link MutableCursor} for {@link XyzFeature}.
   *
   * @param limit
   * @return
   * @throws NoCursor
   */
  @JsonIgnore
  public @NotNull MutableCursor<XyzFeature, XyzFeatureCodec> getXyzMutableCursor(long limit) throws NoCursor {
    if (cursor != null) {
      return mutableCursor(XyzFeatureCodecFactory.get(), limit);
    }
    throw new NoCursor(this);
  }

  /**
   * Returns {@link SeekableCursor} for {@link XyzFeature}.
   *
   * @param limit
   * @return
   * @throws NoCursor
   */
  @JsonIgnore
  public @NotNull SeekableCursor<XyzFeature, XyzFeatureCodec> getXyzSeekableCursor(long limit) throws NoCursor {
    if (cursor != null) {
      return seekableCursor(XyzFeatureCodecFactory.get(), limit);
    }
    throw new NoCursor(this);
  }

  /**
   * Return the cursor using the {@link XyzCollectionCodecFactory}, if any cursor is available. If no cursor is available, throws an
   * exception to handle result. This is to simplify result processing when needed: <pre>{@code
   * try (ForwardCursor<XyzCollection, XyzCollectionCodec> cursor = session.execute(request).getXyzCollectionCursor()) {
   *   for (XyzCollection collections : cursor) {
   *     ...
   *   }
   * } catch (NoCursor e) {
   *   if (e.result instanceof SuccessResult) ...
   *   if (e.result instanceof ErrorResult) ...
   * }}</pre>
   *
   * @return the cursor.
   */
  @JsonIgnore
  public @NotNull ForwardCursor<XyzCollection, XyzCollectionCodec> getXyzCollectionCursor() throws NoCursor {
    if (cursor != null) {
      if (cursor instanceof HeapCacheCursor) {
        cursor = ((HeapCacheCursor<?, ?>) cursor).getOriginalCursor();
      }
      return cursor.withCodecFactory(XyzCollectionCodecFactory.get(), false);
    }
    throw new NoCursor(this);
  }

  /**
   * Closes the result, this includes closing the cursor. After having called this method, calling {@link #cursor(FeatureCodecFactory)}
   * should always throw a {@link NoCursor} exception.
   */
  @Override
  public void close() {
    if (cursor != null) {
      try {
        cursor.close();
      } finally {
        cursor = null;
      }
    }
  }
}
