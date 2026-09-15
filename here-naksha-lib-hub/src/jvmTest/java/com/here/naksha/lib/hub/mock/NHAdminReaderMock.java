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
package com.here.naksha.lib.hub.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import naksha.geo.ProxyGeoUtil;
import naksha.geo.SpGeometry;
import naksha.model.IReadSession;
import naksha.model.IStorage;
import naksha.model.MemberProcessorMap;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.SessionOptions;
import naksha.model.TagMap;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaFeature;
import naksha.model.objects.NakshaFeatureList;
import naksha.model.objects.NakshaCatalog;
import naksha.model.request.*;
import naksha.model.request.query.ISpatialQuery;
import naksha.model.request.query.ITagQuery;
import naksha.model.request.query.SpAnd;
import naksha.model.request.query.SpIntersects;
import naksha.model.request.query.SpNot;
import naksha.model.request.query.SpOr;
import naksha.model.request.query.SpRefInHereTile;
import naksha.model.request.query.TagAnd;
import naksha.model.request.query.TagExists;
import naksha.model.request.query.TagNot;
import naksha.model.request.query.TagOr;
import naksha.model.request.query.TagValueIsBool;
import naksha.model.request.query.TagValueIsDouble;
import naksha.model.request.query.TagValueIsNull;
import naksha.model.request.query.TagValueIsString;
import naksha.model.request.query.TagValueMatches;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHAdminReaderMock implements IReadSession {

  protected static @NotNull Map<String, TreeMap<String, NakshaFeature>> mockCollection;

  public NHAdminReaderMock() {
    throw new UnsupportedOperationException(
        "NHAdminReaderMock storage should not be used"); // comment to use mock in local env
  }

  public NHAdminReaderMock(final @NotNull Map<String, TreeMap<String, NakshaFeature>> mockCollection) {
    this.mockCollection = mockCollection;
  }

  @Override
  public @NotNull Response executeRead(@NotNull ReadRequest request) {
    if (request instanceof ReadFeatures rf) {
      return executeReadFeatures(rf);
    }
    return new ErrorResponse(
            new NakshaError(NakshaError.ILLEGAL_STATE, "ReadRequest type " + request.getClass().getName() + " not supported"));
  }

  protected @NotNull Response executeReadFeatures(@NotNull ReadFeatures rf) {
    List<NakshaFeature> features = getFeatures(rf.getCollectionId(), rf.getFeatureIds(), rf.getQuery());
    SuccessResponse response = new SuccessResponse();
    response.setFeatures(NakshaFeatureList.fromList(features));
    return response;
  }

  private List<NakshaFeature> getFeatures(String collectionId, List<String> featureIds, RequestQuery query) {
    if (query.getProperties() != null || !query.getRefTiles().isEmpty()) {
      throw new NakshaException(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Mock supports only tags and spatial query"));
    }
    final List<NakshaFeature> allFeaturesFromCollections = getAllFeaturesFromCollections(collectionId);
    if (featureIds == null || featureIds.isEmpty()) {
      return allFeaturesFromCollections;
    }
    Stream<NakshaFeature> featureStream = allFeaturesFromCollections.stream()
        .filter(nf -> featureIds.contains(nf.getId()));

    ITagQuery tagQuery = query.getTags();
    if (tagQuery != null) {
      Predicate<NakshaFeature> tagBasedPredicate = tagBasedPredicate(tagQuery);
      featureStream = featureStream.filter(tagBasedPredicate);
    }

    ISpatialQuery spatialQuery = query.getSpatial();
    if (spatialQuery != null) {
      Predicate<NakshaFeature> spatialPredicate = spatialPredicate(spatialQuery);
      featureStream = featureStream.filter(spatialPredicate);
    }

    return featureStream.toList();
  }

  private List<NakshaFeature> getAllFeaturesFromCollections(String collectionId) {
    List<NakshaFeature> features = new ArrayList<>();
    if (mockCollection.get(collectionId) == null) {
      throw new NakshaException(new NakshaError(
          NakshaError.COLLECTION_NOT_FOUND,
          "Collection " + collectionId + " not found!"
      ));
    }
    features.addAll(mockCollection.get(collectionId).values());
    return features;
  }

  private Predicate<NakshaFeature> tagBasedPredicate(ITagQuery tagQuery) {
    if (tagQuery instanceof TagNot notQuery) {
      return Predicate.not(tagBasedPredicate(notQuery.getQuery()));
    } else if (tagQuery instanceof TagOr orQuery) {
      if (orQuery.isEmpty()) {
        throw new NakshaException(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Empty OR tagQuery"));
      }
      Predicate<NakshaFeature> combinedOr = nakshaFeature -> false;
      for (ITagQuery nestedCondition : orQuery) {
        combinedOr = combinedOr.or(tagBasedPredicate(nestedCondition));
      }
      return combinedOr;
    } else if (tagQuery instanceof TagAnd tagAnd) {
      if (tagAnd.isEmpty()) {
        throw new NakshaException(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Empty AND tagQuery"));
      }
      Predicate<NakshaFeature> combinedAnd = nakshaFeature -> true;
      for (ITagQuery nestedCondition : tagAnd) {
        combinedAnd = combinedAnd.or(tagBasedPredicate(nestedCondition));
      }
      return combinedAnd;
    } else if (tagQuery instanceof TagExists tagExists) {
      return nakshaFeature -> tagMapOf(nakshaFeature).containsKey(tagExists.getName());
    } else if (tagQuery instanceof TagValueIsNull tagValueIsNull) {
      return nakshaFeature -> tagMapOf(nakshaFeature).get(tagValueIsNull.getName()) == null;
    } else if (tagQuery instanceof TagValueIsBool tagValueIsBool) {
      return nakshaFeature -> tagMapOf(nakshaFeature).get(tagValueIsBool.getName()) == Boolean.valueOf(tagValueIsBool.getValue());
    } else if (tagQuery instanceof TagValueIsDouble tagValueIsDouble) {
      return nakshaFeature -> Objects.equals(tagMapOf(nakshaFeature).get(tagValueIsDouble.getName()), tagValueIsDouble.getValue());
    } else if (tagQuery instanceof TagValueIsString tagValueIsString) {
      return nakshaFeature -> Objects.equals(tagMapOf(nakshaFeature).get(tagValueIsString.getName()), tagValueIsString.getValue());
    } else if (tagQuery instanceof TagValueMatches tagValueMatches) {
      return nakshaFeature -> ((String) tagMapOf(nakshaFeature).get(tagValueMatches.getName())).matches(tagValueMatches.getRegex());
    } else {
      throw new NakshaException(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Unknown tag query type: " + tagQuery.getClass().getName()));
    }
  }

  private TagMap tagMapOf(NakshaFeature nakshaFeature) {
    return nakshaFeature.getProperties().getXyz().getTags().toTagMap();
  }

  private Predicate<NakshaFeature> spatialPredicate(ISpatialQuery spatialQuery) {
    if (spatialQuery instanceof SpNot spNot) {
      return Predicate.not(spatialPredicate(spNot.getQuery()));
    } else if (spatialQuery instanceof SpAnd spAnd) {
      if (spAnd.isEmpty()) {
        throw new NakshaException(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Empty OR tag query"));
      }
      Predicate<NakshaFeature> combinedAnd = nakshaFeature -> true;
      for (ISpatialQuery nestedCondition : spAnd) {
        combinedAnd = combinedAnd.and(spatialPredicate(nestedCondition));
      }
      return combinedAnd;
    } else if (spatialQuery instanceof SpOr spOr) {
      if (spOr.isEmpty()) {
        throw new NakshaException(new NakshaError(NakshaError.ILLEGAL_ARGUMENT, "Empty AND spatial query"));
      }
      Predicate<NakshaFeature> combinedOr = nakshaFeature -> false;
      for (ISpatialQuery nestedCondition : spOr) {
        combinedOr = combinedOr.or(spatialPredicate(nestedCondition));
      }
      return combinedOr;
    } else if (spatialQuery instanceof SpIntersects spIntersects) {
      return nakshaFeature -> {
        SpGeometry featureGeometry = nakshaFeature.getGeometry();
        if (featureGeometry != null) {
          return ProxyGeoUtil.toJtsGeometry(featureGeometry)
              .intersects(ProxyGeoUtil.toJtsGeometry(spIntersects.getGeometry()));
        }
        return false;
      };
    } else if (spatialQuery instanceof SpRefInHereTile spRefInHereTile) {
      throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "SpRefInHereTile is not supported by mock yet"));
    }
    throw new NakshaException(
        new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported spatial query: " + spatialQuery.getClass().getName()));
  }

  /**
   * Closes the session, returns the underlying connection back to the connection pool. Any method of the session will from now on throw an
   * {@link IllegalStateException}.
   */
  @Override
  public void close() {
  }

  @Override
  public int getSocketTimeout() {
    return 0;
  }

  @Override
  public void setSocketTimeout(int i) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public int getStmtTimeout() {
    return 0;
  }

  @Override
  public void setStmtTimeout(int i) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public int getLockTimeout() {
    return 0;
  }

  @Override
  public void setLockTimeout(int i) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public boolean isClosed() {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public @NotNull IStorage getStorage() {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public @NotNull SessionOptions getOptions() {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public @Nullable NakshaCatalog getCatalogById(@NotNull String catalogId, boolean allowTombstone) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public @Nullable NakshaCatalog getCatalogByNumber(int catalogNumber, boolean allowTombstone) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public @Nullable NakshaCollection getCollectionById(
      @NotNull NakshaCatalog map,
      @NotNull String collectionId,
      boolean allowTombstone) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public @Nullable NakshaCollection getCollectionByNumber(
      @NotNull NakshaCatalog catalog,
      int collectionNumber,
      boolean allowTombstone) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }

  @Override
  public void loadTuples(@NotNull List<? extends FeatureTuple> featureTuples, int from, int to) {
    throw new NakshaException(new NakshaError(NakshaError.UNSUPPORTED_OPERATION, "Not supported by mock yet"));
  }
}
