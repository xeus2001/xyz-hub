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
package com.here.naksha.app.service.http.auth;

import static com.here.naksha.app.service.http.auth.XyzHubAttributeMap.LISTENERS;
import static com.here.naksha.app.service.http.auth.XyzHubAttributeMap.PACKAGES;
import static com.here.naksha.app.service.http.auth.XyzHubAttributeMap.PROCESSORS;
import static com.here.naksha.app.service.http.auth.XyzHubAttributeMap.SEARCHABLE_PROPERTIES;
import static com.here.naksha.app.service.http.auth.XyzHubAttributeMap.SORTABLE_PROPERTIES;
import static com.here.naksha.app.service.http.auth.XyzHubAttributeMap.STORAGE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class SpaceAuthorization extends Authorization {
  private static final Logger log = LoggerFactory.getLogger(SpaceAuthorization.class);

  public static List<String> basicEdit = Arrays.asList(
      "id",
      "title",
      "description",
      "client",
      "copyright",
      "license",
      "shared",
      "enableUUID",
      "enableHistory",
      "maxVersionCount",
      "cacheTTL",
      "readOnly",
      "extends",
      "revisionsToKeep",
      STORAGE,
      LISTENERS,
      PROCESSORS,
      SEARCHABLE_PROPERTIES,
      SORTABLE_PROPERTIES);

  public static List<String> packageEdit = Collections.singletonList(PACKAGES);

  //  public static void authorizeReadSpaces(MatrixReadQuery task, ICallback callback) {
  //    // Check if anonymous token is being used
  //    if (task.getJwt().anonymous) {
  //      callback.throwException(
  //          new HttpException(FORBIDDEN, "Accessing this resource with an anonymous token is not possible."));
  //    } else if (task.getJwt().getXyzHubMatrix() == null) {
  //      callback.throwException(
  //          new HttpException(FORBIDDEN, "Insufficient rights to read the requested resource."));
  //    } else {
  //      if (task.canReadConnectorsProperties) {
  //        final XyzHubActionMatrix connectorsReadMatrix =
  //            new XyzHubActionMatrix().accessConnectors(new XyzHubAttributeMap());
  //        task.canReadConnectorsProperties =
  //            task.getJwt().getXyzHubMatrix().matches(connectorsReadMatrix);
  //      }
  //
  //      /*
  //       * No further checks are necessary. Authenticated users generally have access to this resource.
  //       * The resulting list response will only contain spaces the token has access to.
  //       */
  //      callback.success(task);
  //    }
  //  }
  //
  //  public static void authorizeModifyOp(ConditionalOperation task, ICallback<ConditionalOperation> callback)
  //      throws Exception {
  //    final XyzHubActionMatrix tokenRights = task.getJwt().getXyzHubMatrix();
  //    final XyzHubActionMatrix requestRights = new XyzHubActionMatrix();
  //
  //    final FeatureModificationEntry<Space> entry = task.modifyOp.entries.get(0);
  //    final Map<String, Object> input = entry.input;
  //    final Space head = entry.head;
  //    final Space target = entry.result;
  //
  //    final Map templateAsMap = asMap(task.template);
  //    final Map inputAsMap = asMap(DatabindCodec.mapper().convertValue(input, Space.class));
  //    final Map targetAsMap = asMap(target);
  //    final Map headAsMap = asMap(head);
  //
  //    boolean isAdminEdit,
  //        isBasicEdit,
  //        isStorageEdit,
  //        isListenersEdit,
  //        isProcessorsEdit,
  //        isPackagesEdit,
  //        isSearchablePropertiesEdit,
  //        isSortablePropertiesEdit;
  //    final AttributeMap xyzhubFilter;
  //
  //    // Check if anonymous token is being used
  //    if (task.getJwt().anonymous) {
  //      callback.throwException(
  //          new HttpException(FORBIDDEN, "Accessing this resource with an anonymous token is not possible."));
  //      return;
  //    }
  //
  //    // CREATE
  //    if (task.isCreate()) {
  //      xyzhubFilter = new XyzHubAttributeMap()
  //          .withValue(OWNER, input.get("owner"))
  //          .withValue(SPACE, input.get("id"));
  //      isBasicEdit = isBasicEdit(templateAsMap, inputAsMap);
  //      isAdminEdit = isAdminEdit(templateAsMap, inputAsMap);
  //      isStorageEdit = isPropertyEdit(templateAsMap, inputAsMap, STORAGE);
  //      isListenersEdit = isPropertyEdit(templateAsMap, inputAsMap, LISTENERS);
  //      isProcessorsEdit = isPropertyEdit(templateAsMap, inputAsMap, PROCESSORS);
  //      isPackagesEdit = isPropertyEdit(templateAsMap, inputAsMap, PACKAGES);
  //      isSearchablePropertiesEdit = isPropertyEdit(templateAsMap, inputAsMap, SEARCHABLE_PROPERTIES);
  //      isSortablePropertiesEdit = isPropertyEdit(templateAsMap, inputAsMap, SORTABLE_PROPERTIES);
  //
  //      final XyzHubActionMatrix adminMatrix = new XyzHubActionMatrix().adminSpaces(xyzhubFilter);
  //      boolean hasAdminPermissions = tokenRights != null && tokenRights.matches(adminMatrix);
  //      // If the creation contains a different space ID than the randomly-generated one.
  //      if (!hasAdminPermissions
  //          && isPropertyEdit(templateAsMap, inputAsMap, ID)
  //          && tokenRights != null
  //          && tokenRights.containsKey(MANAGE_SPACES)) {
  //        tokenRights.put(
  //            MANAGE_SPACES,
  //            tokenRights.get(MANAGE_SPACES).stream()
  //                .filter(permission -> permission.containsKey(SPACE) || permission.isEmpty())
  //                .collect(Collectors.toList()));
  //      }
  //    }
  //    // READ, UPDATE, DELETE
  //    else {
  //      xyzhubFilter = new XyzHubAttributeMap()
  //          .withValue(OWNER, head.getOwner())
  //          .withValue(SPACE, head.getId())
  //          .withValue(PACKAGES, head.getPackages());
  //      isBasicEdit = isBasicEdit(targetAsMap, headAsMap);
  //      isAdminEdit = !task.isDelete() && isAdminEdit(targetAsMap, headAsMap);
  //      isStorageEdit = !task.isDelete() && isPropertyEdit(targetAsMap, headAsMap, STORAGE);
  //      isListenersEdit = !task.isDelete() && isPropertyEdit(targetAsMap, headAsMap, LISTENERS);
  //      isProcessorsEdit = !task.isDelete() && isPropertyEdit(targetAsMap, headAsMap, PROCESSORS);
  //      isPackagesEdit = !task.isDelete() && isPropertyEdit(targetAsMap, headAsMap, PACKAGES);
  //      isSearchablePropertiesEdit =
  //          !task.isDelete() && isPropertyEdit(targetAsMap, headAsMap, SEARCHABLE_PROPERTIES);
  //      isSortablePropertiesEdit = !task.isDelete() && isPropertyEdit(targetAsMap, headAsMap, SORTABLE_PROPERTIES);
  //    }
  //
  //    // Mark in the task, if the app is allowed to read the admin properties.
  //    final XyzHubActionMatrix adminMatrix = new XyzHubActionMatrix().adminSpaces(xyzhubFilter);
  //    task.canReadAdminProperties = tokenRights != null && tokenRights.matches(adminMatrix);
  //
  //    // Mark in the task, if the user is allowed to read the connectors on READ and WRITE operations.
  //    task.canReadConnectorsProperties = canReadConnectorProperties(tokenRights);
  //
  //    // On Read operations, any access to the space grants read access, this includes readFeatures, createFeatures,
  //    // etc.
  //    if (task.isRead()) {
  //      if (head.isShared()) {
  //        // User is trying to read a shared space this is allowed for any authenticated user
  //        callback.success(task);
  //        return;
  //      }
  //
  //      if (tokenRights == null
  //          || tokenRights.entrySet().stream()
  //              .flatMap(e -> e.getValue().stream())
  //              .noneMatch(f -> f.matches(xyzhubFilter))) {
  //        throw new HttpException(FORBIDDEN, "Insufficient rights to read the requested resource.");
  //      }
  //
  //      callback.success(task);
  //      return;
  //    }
  //
  //    List<CompletableFuture<Void>> futureList = new ArrayList<>();
  //    final XyzHubActionMatrix connectorsRights = new XyzHubActionMatrix();
  //
  //    // If this is an edit on storage, listeners or processors properties.
  //    if (isStorageEdit || isListenersEdit || isProcessorsEdit) {
  //
  //      Set<String> connectorIds = new HashSet<>();
  //
  //      // Check for storage.
  //      if (isStorageEdit) connectorIds.add(getStorageFromInput(entry));
  //
  //      // Check for listeners.
  //      if (isListenersEdit) connectorIds.addAll(getConnectorIds(LISTENERS, targetAsMap, headAsMap));
  //
  //      // Check for processors.
  //      if (isProcessorsEdit) connectorIds.addAll(getConnectorIds(PROCESSORS, targetAsMap, headAsMap));
  //
  //      connectorIds.forEach(id -> {
  //        CompletableFuture<Void> f = new CompletableFuture<>();
  //        futureList.add(f);
  //        Service.connectorConfigClient.get(task.routingContext.get("marker"), id, ar -> {
  //          if (ar.succeeded()) {
  //            Connector c = ar.result();
  //            connectorsRights.accessConnectors(XyzHubAttributeMap.forIdValues(c.owner, c.id));
  //          } else {
  //            // If connector does not exist.
  //            log.warn(
  //                (Marker) task.routingContext.get("marker"),
  //                "Tried to load non-existing connector '{}'.",
  //                id,
  //                ar.cause());
  //            connectorsRights.accessConnectors(XyzHubAttributeMap.forIdValues(id));
  //          }
  //          f.complete(null);
  //        });
  //      });
  //    }
  //
  //    CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]))
  //        .thenRun(() -> {
  //          if (connectorsRights.get("accessConnectors") != null
  //              && !connectorsRights.get("accessConnectors").isEmpty()) {
  //            task.canReadConnectorsProperties = tokenRights != null && tokenRights.matches(connectorsRights);
  //            if (!task.canReadConnectorsProperties) {
  //              callback.throwException(
  //                  new HttpException(FORBIDDEN, getForbiddenMessage(connectorsRights, tokenRights)));
  //              return;
  //            }
  //          }
  //
  //          // Either for admin or manage spaces, the packages access must be tested
  //          if (isPackagesEdit) {
  //            // Requester must hold manageSpaces permission when adding new packages to space
  //            if (task.isCreate() || isPackagesAdded(head, target)) {
  //              // to add or edit a package within a space, you must have space owner permissions
  //              requestRights.manageSpaces(xyzhubFilter);
  //              getPackagesFromInput(entry)
  //                  .forEach(packageId -> requestRights.managePackages(
  //                      XyzHubAttributeMap.forIdValues(target.getOwner(), packageId)));
  //            } else {
  //              boolean isOwner = tokenRights != null
  //                  && tokenRights.matches(new XyzHubActionMatrix().manageSpaces(xyzhubFilter));
  //              if (!isOwner) {
  //                // then it's just a package removal
  //                List<String> removedPackages = new ArrayList<>(getPackagesFromSpace(head));
  //                removedPackages.removeAll(getPackagesFromSpace(target));
  //                removedPackages.forEach(packageId -> requestRights.managePackages(
  //                    XyzHubAttributeMap.forIdValues(target.getOwner(), packageId)));
  //              }
  //            }
  //          }
  //
  //          // Checks if the user has useCapabilities: ['searchablePropertiesConfiguration']
  //          if (isSearchablePropertiesEdit || isSortablePropertiesEdit) {
  //            requestRights.useCapabilities(
  //                new AttributeMap().withValue(ID, "searchablePropertiesConfiguration"));
  //          }
  //
  //          // If this is an edit on admin properties.
  //          if (isAdminEdit) {
  //            boolean ownerChanged = !task.isCreate()
  //                && input.containsKey("owner")
  //                && !input.get("owner").equals(head.getOwner());
  //            if (ownerChanged) {
  //              XyzHubAttributeMap additionalNeededPermission = new XyzHubAttributeMap();
  //              additionalNeededPermission.withValue(SPACE, head.getId());
  //              additionalNeededPermission.withValue(OWNER, input.get("owner"));
  //              requestRights.adminSpaces(additionalNeededPermission);
  //            }
  //
  //            requestRights.adminSpaces(xyzhubFilter);
  //          }
  //
  //          if (isBasicEdit) {
  //            requestRights.manageSpaces(xyzhubFilter);
  //          }
  //
  //          evaluateRights(requestRights, tokenRights, task, callback);
  //        })
  //        .exceptionally(t -> {
  //          log.error(
  //              (Marker) task.routingContext.get("marker"),
  //              "Exception while checking connector permissions",
  //              t);
  //          callback.throwException(t);
  //          return null;
  //        });
  //  }
  //
  //  private static Set getConnectorIds(
  //      @Nonnull final String field, @Nonnull final Map target, @Nullable final Map head) {
  //    if (!target.containsKey(field)) return Collections.emptySet();
  //
  //    if (head == null) return ((Map) target.get(field)).keySet();
  //
  //    // check the difference between head & input and return the modified ones
  //    Difference diff = Patcher.getDifference(head, target);
  //    if (diff instanceof MapDiff && ((MapDiff) diff).containsKey(field)) {
  //      diff = ((MapDiff) diff).get(field);
  //      if (diff instanceof MapDiff) {
  //        return ((MapDiff) diff).keySet();
  //      }
  //    }
  //
  //    return Collections.emptySet();
  //  }
  //
  //  private static boolean isAdminEdit(Map state1, Map state2) {
  //    try {
  //      MapDiff diff = (MapDiff) Patcher.getDifference(state1, state2);
  //      final List<String> basicPlusPackagesList =
  //          Stream.concat(basicEdit.stream(), packageEdit.stream()).collect(Collectors.toList());
  //
  //      return diff != null && !basicPlusPackagesList.containsAll(diff.keySet());
  //    } catch (Exception e) {
  //      return true;
  //    }
  //  }
  //
  //  private static boolean isBasicEdit(Map state1, Map state2) {
  //    try {
  //      MapDiff diff = (MapDiff) Patcher.getDifference(state1, state2);
  //      return diff != null && basicEdit.containsAll(diff.keySet());
  //    } catch (Exception e) {
  //      return true;
  //    }
  //  }
  //
  //  private static boolean isPropertyEdit(Map state1, Map state2, String property) {
  //    try {
  //      MapDiff diff = (MapDiff) Patcher.getDifference(state1, state2);
  //      return diff != null && diff.containsKey(property);
  //    } catch (Exception e) {
  //      return true;
  //    }
  //  }
  //
  //  private static boolean canReadConnectorProperties(XyzHubActionMatrix tokenRights) {
  //    if (tokenRights == null) {
  //      return false;
  //    }
  //
  //    if (tokenRights.get(XyzHubActionMatrix.ACCESS_CONNECTORS) == null) {
  //      return false;
  //    }
  //
  //    return !tokenRights.get(XyzHubActionMatrix.ACCESS_CONNECTORS).isEmpty();
  //  }
  //
  //  private static String getStorageFromInput(FeatureModificationEntry<Space> entry) {
  //    return new JsonObject(entry.input).getJsonObject("storage").getString("id");
  //  }
  //
  //  private static List<String> getPackagesFromInput(FeatureModificationEntry<Space> entry) {
  //    if (entry.input.containsKey("packages")) {
  //      return new JsonObject(entry.input)
  //          .getJsonArray("packages").stream().map(Object::toString).collect(Collectors.toList());
  //    }
  //    return Collections.emptyList();
  //  }
  //
  //  private static List<String> getPackagesFromSpace(Space space) {
  //    if (space != null && space.getPackages() != null) {
  //      return space.getPackages();
  //    }
  //    return Collections.emptyList();
  //  }
  //
  //  private static boolean isPackagesAdded(Space head, Space target) {
  //    final List<String> targetPackages = getPackagesFromSpace(target);
  //    if (targetPackages.isEmpty()) {
  //      return false;
  //    }
  //
  //    final List<String> difference = new ArrayList<>(targetPackages);
  //    difference.removeAll(getPackagesFromSpace(head));
  //
  //    return !difference.isEmpty();
  //  }
  //
  //  private static Map asMap(Object object) {
  //    try {
  //      //noinspection unchecked
  //      return FeatureModificationList.filter(
  //          Json.decodeValue(
  //              DatabindCodec.mapper().writerWithView(Static.class).writeValueAsString(object), Map.class),
  //          ModifySpaceOp.metadataFilter);
  //    } catch (Exception e) {
  //      return Collections.emptyMap();
  //    }
  //  }
}