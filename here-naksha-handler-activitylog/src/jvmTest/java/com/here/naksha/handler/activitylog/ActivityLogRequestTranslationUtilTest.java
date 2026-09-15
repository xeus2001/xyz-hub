package com.here.naksha.handler.activitylog;

import static com.here.naksha.handler.activitylog.ActivityLogRequestTranslationUtil.PROPERTY_ACTIVITY_LOG_ID;
import static com.here.naksha.handler.activitylog.ActivityLogRequestTranslationUtil.transformOriginalRequest;
import static com.here.naksha.handler.activitylog.GuidUtil.guid;
import static com.here.naksha.handler.activitylog.GuidUtil.randomVersion;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import naksha.base.StringList;
import naksha.base.Guid;
import naksha.base.Version;
import naksha.model.objects.StandardMembers;
import naksha.model.request.ReadFeatures;
import naksha.model.request.ops.*;
import naksha.model.request.query.IPropertyQuery;
import naksha.model.request.query.POr;
import naksha.model.request.query.PQuery;
import naksha.model.request.query.StringOp;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class ActivityLogRequestTranslationUtilTest {

  private static final String TEST_SPACE_ID = "test_space_id";

  @Test
  void shouldTranslateSingleGuidPassedAsFeatureId() {
    // Given: single GUID passed in ReadFeatures
    String featureId = "test_feature_id";
    Guid guid = guid(featureId, randomVersion());
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.setFeatureIds(StringList.of(guid.toString()));

    // When: translating the original request
    transformOriginalRequest(readFeatures, TEST_SPACE_ID);

    // Then: request will reach correct collection and history
    verifyAllHistoricalVersionsInCollection(readFeatures);

    // And: no feature ids are passed from original request
    assertTrue(readFeatures.getFeatureIds().isEmpty());

    // And: there is a single requested FeatureNumber + Version combo passed from original featureId
    Op op = readFeatures.getQueryMembers();
    checkOpIsUuidRequest(op, List.of(guid.tupleNumber.version));
  }

  private void checkOpIsIdRequest(Op op, List<String> ids) {
    final IsAnyOf featureIdsOp = assertInstanceOf(IsAnyOf.class, op);
    assertNotNull(featureIdsOp);
    var items = featureIdsOp.getItems();
    assertEquals(ids.size(), items.size());
    for (var id : ids) assertTrue(items.contains(id));
  }

  private void checkOpIsUuidAndIdRequest(final Op op, final List<Object> versions, final List<String> ids) {
    // We assume that op is Or(versions, ids)
    final And andOp = assertInstanceOf(And.class, op);
    assertEquals(2, andOp.getChildren().size());

    // First uuid's
    checkOpIsUuidRequest(andOp.getChildren().getFirst(), versions);

    // second ids
    checkOpIsIdRequest(andOp.getChildren().get(1), ids);
  }

  private void checkOpIsUuidRequest(final Op op, final List<Object> versions) {
    assertInstanceOf(Or.class, op);
    OpList orChildren = ((Or) op).getChildren();
    assertEquals(versions.size(), orChildren.getSize());
    List<Object> versionsFromOr = new ArrayList<>();
    for (int i = 0; i < orChildren.getSize(); i++) {
      Object child = orChildren.get(i);
      assertInstanceOf(And.class, child);
      And and = (And) child;
      OpList andChildren = and.getChildren();
      assertEquals(2, andChildren.getSize());

      Object a = andChildren.get(0);
      Object b = andChildren.get(1);
      assertTrue(a instanceof Equals && b instanceof Equals);
      Equals eqA = (Equals) a;
      Equals eqB = (Equals) b;

      // The two Equals may appear in either order; pick the one that targets Version
      Equals versionEq = null;
      if (StandardMembers.FeatureVersion.getName().equals(eqA.getAt())) versionEq = eqA;
      else if (StandardMembers.FeatureVersion.getName().equals(eqB.getAt())) versionEq = eqB;
      else fail("No Version equals in And clause");
      versionsFromOr.add(versionEq.getValue());
    }
    assertEquals(versions, versionsFromOr);
  }

  @Test
  void shouldTranslateMultipleGuidsPassedAsFeatureIds() {
    // Given: multiple GUIDs passed in ReadFeatures
    Version version1 = randomVersion();
    Version version2 = randomVersion();
    Version version3 = randomVersion();
    List<String> rawGuids = List.of(
        guid("f1", version1).toString(),
        guid("f2", version2).toString(),
        guid("f3", version3).toString()
    );
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.setFeatureIds(StringList.fromList(rawGuids));

    // When: translating the original request
    transformOriginalRequest(readFeatures, TEST_SPACE_ID);

    // Then: request will reach correct collection and history
    verifyAllHistoricalVersionsInCollection(readFeatures);

    // And: no max version is set - it will be covered in post filtering
    assertNull(readFeatures.getVersion());

    // And: translated request does not contain any feature id (no acitivytLog was defined)
    assertTrue(readFeatures.getFeatureIds().isEmpty());

    // And: all guids defined in featureIds were moved to ReadFeatures.guids
    Op op = readFeatures.getQueryMembers();
    checkOpIsUuidRequest(op, List.of(version1, version2, version3));
  }

  @Test
  void shouldTranslateActivityLogIdToFeatureId() {
    // Given:
    String featureId = "some_id";
    PQuery singleActivityLogIdQuery = new PQuery(PROPERTY_ACTIVITY_LOG_ID, StringOp.EQUALS, featureId);
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.getQuery().setProperties(singleActivityLogIdQuery);

    // When:
    transformOriginalRequest(readFeatures, TEST_SPACE_ID);

    // Then: request will reach correct collection and history
    verifyAllHistoricalVersionsInCollection(readFeatures);

    // And: no max version is set
    assertNull(readFeatures.getVersion());

    // And: there is a single featureId withing the request
    checkOpIsIdRequest(readFeatures.getQueryMembers(), List.of(featureId));

    // And:
    assertNull(readFeatures.getQuery().getProperties());
    assertTrue(readFeatures.getFeatureIds().isEmpty());
  }

  @Test
  void shouldTranslateActivityLogIdsToFeatureIds() {
    // Given:
    String firstId = "id_1";
    String secondId = "id_2";
    IPropertyQuery activityLogIdsQuery = new POr(
        new PQuery(PROPERTY_ACTIVITY_LOG_ID, StringOp.EQUALS, firstId),
        new PQuery(PROPERTY_ACTIVITY_LOG_ID, StringOp.EQUALS, secondId)
    );
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.getQuery().setProperties(activityLogIdsQuery);

    // When:
    transformOriginalRequest(readFeatures, TEST_SPACE_ID);

    // Then: request will reach correct collection and history
    verifyAllHistoricalVersionsInCollection(readFeatures);

    // And: all ids defined in AcitvityLogNs are now part of featureIds
    checkOpIsIdRequest(readFeatures.getQueryMembers(), List.of(firstId, secondId));

    // And: the pQuery left is effectively dead
    assertNull(readFeatures.getQuery().getProperties());
    assertTrue(readFeatures.getFeatureIds().isEmpty());
  }

  @Test
  void shouldApplyMixedTranslations() {
    // Given: guids passed in ReadFeatures.featureIds
    String id = "id";
    Version version = randomVersion();
    Guid guid = guid(id, version);
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.setFeatureIds(StringList.of(guid.toString()));

    // And: featureId passed as activity log ns prop
    String activityLogId = "activity_log_id";
    IPropertyQuery activityLogIdQuery = new PQuery(PROPERTY_ACTIVITY_LOG_ID, StringOp.EQUALS, activityLogId);
    readFeatures.withPropertyQuery(activityLogIdQuery);

    // When:
    transformOriginalRequest(readFeatures, TEST_SPACE_ID);

    // Then: request will reach correct collection and history
    verifyAllHistoricalVersionsInCollection(readFeatures);

    // And: uuid's and ids are populated from original request
    checkOpIsUuidAndIdRequest(readFeatures.getQueryMembers(), List.of(version), List.of(activityLogId));
  }

  private void verifyAllHistoricalVersionsInCollection(ReadFeatures readFeatures) {
    assertTrue(readFeatures.getQueryHistory());
    String collectionId = readFeatures.getCollectionId();
    assertEquals(TEST_SPACE_ID, collectionId);
    assertEquals(Integer.MAX_VALUE, readFeatures.getVersions());
  }
}
