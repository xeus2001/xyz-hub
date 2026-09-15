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
package com.here.naksha.lib.view;

import static com.here.naksha.lib.view.PsqlTests.TEST_MAP_ID;
import static com.here.naksha.lib.view.Sample.sampleXyzResponse;
import static com.here.naksha.lib.view.Sample.sampleXyzWriteResponse;
import static java.util.Collections.emptyList;
import static naksha.model.util.CustomStoragePropertiesUtil.getConnectTimeoutMs;
import static naksha.model.util.CustomStoragePropertiesUtil.getSocketTimeoutMs;
import static naksha.model.util.CustomStoragePropertiesUtil.getStmtTimeoutMs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.here.naksha.lib.core.AbstractTask;
import com.here.naksha.lib.core.DefaultRequestLimitManager;
import com.here.naksha.lib.core.IRequestLimitManager;
import com.here.naksha.lib.core.SimpleTask;
import com.here.naksha.lib.core.exceptions.TooManyTasks;
import com.here.naksha.lib.core.exceptions.UncheckedException;
import com.here.naksha.lib.view.concurrent.LayerReadRequest;
import com.here.naksha.lib.view.concurrent.ParallelQueryExecutor;
import com.here.naksha.lib.view.merge.MergeByStoragePriority;
import com.here.naksha.lib.view.missing.IgnoreMissingResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import naksha.base.MapProxy;
import naksha.base.Action;
import naksha.model.IReadSession;
import naksha.model.IStorage;
import naksha.model.IWriteSession;
import naksha.model.NakshaContext;
import naksha.model.SessionOptions;
import naksha.model.objects.NakshaFeature;
import naksha.model.objects.NakshaProperties;
import naksha.model.objects.NakshaStorage;
import naksha.model.request.FeatureTuple;
import naksha.model.request.FeatureTupleList;
import naksha.model.request.ReadFeatures;
import naksha.model.request.RequestQuery;
import naksha.model.request.Response;
import naksha.model.request.SuccessResponse;
import naksha.model.request.Write;
import naksha.model.request.WriteRequest;
import naksha.model.request.query.POr;
import naksha.model.request.query.PQuery;
import naksha.model.request.query.Property;
import naksha.model.request.query.StringOp;
import naksha.model.util.RequestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ViewTest {

  private NakshaContext nc = NakshaContext.currentContext().withAppId("VIEW_API_TEST").withAuthor("VIEW_API_AUTHOR");
  private SessionOptions sessionOptions = new SessionOptions();
  private final Write write = new Write();
  private final static String TOPO = "topologies";

  @Test
  void testReadApiNotation() {
    // given
    IStorage storage = mock(IStorage.class);
    ViewLayer topologiesDS = new ViewLayer(storage, TEST_MAP_ID, "topologies");
    ViewLayer buildingsDS = new ViewLayer(storage, TEST_MAP_ID, "buildings");
    ViewLayer topologiesCS = new ViewLayer(storage, TEST_MAP_ID, "topologies");

    // each layer is going to return 3 same records
    var results = sampleXyzResponse(3, storage);
    when(storage.newReadSession(sessionOptions)).thenReturn(new MockReadSession(results));

    ViewLayerCollection viewLayerCollection = new ViewLayerCollection("myCollection", topologiesDS, buildingsDS, topologiesCS);

    View view = new View(viewLayerCollection);

    MergeOperation customMergeOperation = new MergeByStoragePriority();
    MissingIdResolver skipFetchingResolver = new IgnoreMissingResolver();

    // when
    ViewReadSession readSession = view.newReadSession(sessionOptions);
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.setQueryHistory(true);
    Response result = readSession.executeReadFeatures(
        readFeatures, customMergeOperation, skipFetchingResolver);
    assertInstanceOf(SuccessResponse.class, result);

    // then
    List<FeatureTuple> allFeatures = ((SuccessResponse) result).getFeatureTupleList();
    assertEquals(3, allFeatures.size());
    assertTrue(allFeatures.containsAll(results));
  }

  @Test
  void testWriteApiNotation() {
    final String VIEW_COLLECTION = "myCollection";
    IStorage storage = mock(IStorage.class);
    MapProxy map = mock(MapProxy.class);
    IWriteSession session = mock(IWriteSession.class);

    ViewLayer topologiesDS = new ViewLayer(storage, TEST_MAP_ID, "topologies");
    ViewLayerCollection viewLayerCollection = new ViewLayerCollection(VIEW_COLLECTION, topologiesDS);
    View view = new View(viewLayerCollection);
    when(storage.newWriteSession(sessionOptions)).thenReturn(session);
//    when(storage.getMapId(any(Integer.class))).thenReturn(VIEW_COLLECTION);
    when(storage.getId()).thenReturn("Mock Storage");
//    when(storage.get(any())).thenReturn(map);
//    when(map.getCollectionId(any())).thenReturn("Mock Collection");

    final WriteRequest request = new WriteRequest();
    final NakshaFeature feature = new NakshaFeature("0");
    request.add(write.createFeature(TEST_MAP_ID, "", feature));
//    when(storage.tupleToFeature(any())).thenReturn(feature);

    Response success = new SuccessResponse(sampleXyzWriteResponse(1, Action.CREATE));
    when(session.executeWrite(request)).thenReturn(success);
    ViewWriteSession writeSession = view.newWriteSession(sessionOptions);
    Response response = writeSession.execute(request);
    assertInstanceOf(SuccessResponse.class, response);
    SuccessResponse successResponse = (SuccessResponse) response;
    assertEquals(feature.getId(), successResponse.getFeatures().get(0).getId());
    assertEquals(Action.CREATE, successResponse.getFeatureTupleList().get(0).getFeature().getProperties().getXyz().getAction());
    writeSession.commit();
  }

  @Test
  void testDeleteApiNotation() {
    final String VIEW_COLLECTION = "myCollection";
    IStorage storage = mock(IStorage.class);
    IWriteSession session = mock(IWriteSession.class);

    ViewLayer topologiesDS = new ViewLayer(storage, TEST_MAP_ID, "topologies");
    ViewLayerCollection viewLayerCollection = new ViewLayerCollection(VIEW_COLLECTION, topologiesDS);
    View view = new View(viewLayerCollection);
    when(storage.newWriteSession(sessionOptions)).thenReturn(session);

    final WriteRequest request = new WriteRequest();
    final NakshaFeature feature = new NakshaFeature("0");
    request.add(write.deleteFeatureById(topologiesDS.getMapId(), topologiesDS.getCollectionId(), feature.getId()));
    SuccessResponse successResponse1 = new SuccessResponse(sampleXyzWriteResponse(1, Action.DELETE));
    when(session.executeWrite(request)).thenReturn(successResponse1);
    ViewWriteSession writeSession = view.newWriteSession(sessionOptions);

    Response response = writeSession.execute(request);
    assertInstanceOf(SuccessResponse.class, response);
    SuccessResponse successResponse = (SuccessResponse) response;
    assertEquals(feature.getId(), successResponse.getFeatureTupleList().get(0).getId());
    assertEquals(Action.DELETE, successResponse.getFeatureTupleList().get(0).getFeature().getProperties().getXyz().getAction());
    writeSession.commit();
  }

  @Test
  void testExceptionInOneOfTheThreads() {
    // given
    IReadSession readSession = mock(IReadSession.class);
    when(readSession.execute(any())).thenThrow(RuntimeException.class);

    IStorage topologiesStorage = mock(IStorage.class);
    IStorage buildingsStorage = mock(IStorage.class);
    ViewLayer topologiesDS = new ViewLayer(topologiesStorage, TEST_MAP_ID, "topologies");
    ViewLayer buildingsDS = new ViewLayer(buildingsStorage, TEST_MAP_ID, "buildings");

    var results = sampleXyzResponse(3, topologiesStorage);
    when(topologiesStorage.newReadSession(sessionOptions)).thenReturn(new MockReadSession(results));
    when(buildingsStorage.newReadSession(sessionOptions)).thenReturn(readSession);

    ViewLayerCollection viewLayerCollection = new ViewLayerCollection("myCollection", topologiesDS, buildingsDS);
    View view = new View(viewLayerCollection);

    // expect
    assertThrows(UncheckedException.class, () -> view.newReadSession(sessionOptions).execute(new ReadFeatures()));
  }

  @Test
  void shouldNotQueryForMissingIfOriginalRequestWasOnlyById() {
    // given
    IStorage topologiesStorage_1 = mock(IStorage.class);
    IStorage topologiesStorage_2 = mock(IStorage.class);
    IReadSession readSession = mock(IReadSession.class);
    when(readSession.execute(any())).thenReturn(new SuccessResponse(emptyList()));

    ViewLayer topologiesDS_1 = new ViewLayer(topologiesStorage_1, TEST_MAP_ID, TOPO);
    ViewLayer topologiesDS_2 = new ViewLayer(topologiesStorage_2, TEST_MAP_ID, TOPO);

    var results = sampleXyzResponse(3, topologiesStorage_2);
    when(topologiesStorage_1.newReadSession(sessionOptions)).thenReturn(readSession);
    when(topologiesStorage_2.newReadSession(sessionOptions)).thenReturn(new MockReadSession(results));

    ViewLayerCollection viewLayerCollection = new ViewLayerCollection("myCollection", topologiesDS_1, topologiesDS_2);
    View view = new View(viewLayerCollection);

    // when only by id
    ReadFeatures request1 = RequestHelper.readFeaturesByIdsRequest(TEST_MAP_ID, TOPO, List.of("1"));
    SuccessResponse response = (SuccessResponse) view.newReadSession(sessionOptions).execute(request1);
    assertNotNull(response.getFeatures());
    // then
    verify(readSession, times(1)).execute(any());

    // when not only by id
    clearInvocations(readSession);
    ReadFeatures request2 = new ReadFeatures();
    POr propQuery = new POr(new PQuery(new Property("id"), StringOp.EQUALS, "1"),
        new PQuery(new Property("app_id"), StringOp.EQUALS, "app"));
    RequestQuery requestQuery = new RequestQuery();
    requestQuery.setProperties(propQuery);
    request2.setQuery(requestQuery);
    SuccessResponse response2 = (SuccessResponse) view.newReadSession(sessionOptions).execute(request2);
    assertNotNull(response2.getFeatures());
    verify(readSession, times(2)).execute(any());
  }

  @Test
  void testTimeoutExceptionInOneOfTheThreads() {
    IStorage topologiesStorage = mock(IStorage.class);
    IStorage buildingsStorage = mock(IStorage.class);
    ViewLayer topologiesDS = new ViewLayer(topologiesStorage, TEST_MAP_ID, "topologies");
    ViewLayer buildingsDS = new ViewLayer(buildingsStorage, TEST_MAP_ID, "buildings");

    // given
    IReadSession topoReadSession = mock(IReadSession.class);
    IReadSession buildReadSession = mock(IReadSession.class);

    when(topoReadSession.execute(any())).thenThrow(new RuntimeException(new TimeoutException()));
    SuccessResponse successResponse = new SuccessResponse(sampleXyzResponse(1, buildingsStorage));
    when(buildReadSession.execute(any())).thenReturn(successResponse);

    when(topologiesStorage.newReadSession(sessionOptions)).thenReturn(topoReadSession);
    when(buildingsStorage.newReadSession(sessionOptions)).thenReturn(buildReadSession);

    ViewLayerCollection viewLayerCollection = new ViewLayerCollection("myCollection", buildingsDS, topologiesDS);
    View view = new View(viewLayerCollection);
    final ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.setQueryHistory(true);
    Throwable exception = assertThrows(UncheckedException.class, () -> view.newReadSession(sessionOptions).execute(readFeatures));
    assertTrue(exception.getMessage().contains("TimeoutException"));
    verify(topoReadSession, times(1)).execute(any());
    verify(buildReadSession, times(1)).execute(any());
  }

  @Test
  void shouldThrowTooManyTasksException() {
    IStorage mockStorage = mock(IStorage.class);
    IRequestLimitManager requestLimitManager = new DefaultRequestLimitManager(30, 100);
    AbstractTask.setConcurrencyLimitManager(requestLimitManager);
    long limit = requestLimitManager.getInstanceLevelLimit();
    ViewLayer[] layerDS = new ViewLayer[(int) (limit + 10)];
    //Create ThreadFactory Limit + 10 layers
    for (int ind = 0; ind < layerDS.length; ind++) {
      layerDS[ind] = new ViewLayer(mockStorage, TEST_MAP_ID, "collection" + ind);
    }
    ViewLayerCollection viewLayerCollection = new ViewLayerCollection("myCollection", layerDS);
    View view = new View(viewLayerCollection);

    List<SimpleTask> tasks = new ArrayList<>();
    try (MockedConstruction<ParallelQueryExecutor> queryExecutor = mockConstruction(ParallelQueryExecutor.class, (mock, context) -> {
      when(mock.queryInParallel(any())).thenAnswer(new Answer<Object>() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
          List<LayerReadRequest> requests = invocation.getArgument(0);
          for (LayerReadRequest layerReadRequest : requests) {
            SimpleTask singleTask = new SimpleTask<>(null, nc);
            tasks.add(singleTask);
            singleTask.start(() -> {
              Thread.sleep(1000);
              return null;
            });
          }
          return Collections.emptyMap();
        }
      });
    })) {

      ViewReadSession viewReadSession = view.newReadSession(sessionOptions);
      Throwable ex = assertThrows(TooManyTasks.class, () -> viewReadSession.execute(new ReadFeatures()));
      assertTrue(ex.getMessage().contains("Maximum number of concurrent tasks"));
    }
    //Interrupt sleeping threads in this group to end.
    Optional<SimpleTask> activeTask = tasks.stream().filter(thread -> thread.getThread() != null).findFirst();
    if (activeTask.isPresent()) {
      ThreadGroup threadGroup = activeTask.get().getThread().getThreadGroup();
      threadGroup.interrupt();
      assertEquals(limit, threadGroup.activeCount());
    }
  }

  @Test
  void shouldApplyCustomTimeoutsPerLayer() {
    // Given
    NakshaStorage firstConfig = customStorageConfig(Map.of(
        "socketTimeout", 1000,
        "connectTimeout", 1010,
        "stmtTimeout", 1111
    ));
    NakshaStorage secondConfig = customStorageConfig(Map.of(
        "socketTimeout", 2000,
        "connectTimeout", 2020,
        "stmtTimeout", 2222
    ));
    NakshaStorage thirdConfig = customStorageConfig(Map.of(
        "socketTimeout", 3000,
        "connectTimeout", 3030,
        "stmtTimeout", 3333
    ));

    // And
    IStorage firstStorage = mockStorageFor(firstConfig);
    IStorage secondStorage = mockStorageFor(secondConfig);
    IStorage thirdStorage = mockStorageFor(thirdConfig);

    // And
    ViewLayer firstLayer = new ViewLayer(firstStorage, TEST_MAP_ID, "first_col");
    ViewLayer secondLayer = new ViewLayer(secondStorage, TEST_MAP_ID, "second_col");
    ViewLayer thirdLayer = new ViewLayer(thirdStorage, TEST_MAP_ID, "third_col");

    // And
    ViewLayerCollection viewLayerCollection = new ViewLayerCollection("test_custom_layered_collection", firstLayer, secondLayer, thirdLayer);

    // And
    ReadFeatures readFeatures = new ReadFeatures();
    readFeatures.setCatalogId(TEST_MAP_ID);
    readFeatures.setCollectionId(firstLayer.getCollectionId());

    // When
    new View(viewLayerCollection).newReadSession(sessionOptions).execute(readFeatures);

    // Then
    ArgumentCaptor<SessionOptions> sessionCaptor = ArgumentCaptor.forClass(SessionOptions.class);
    verify(firstStorage).newReadSession(sessionCaptor.capture());
    verify(secondStorage).newReadSession(sessionCaptor.capture());
    verify(thirdStorage).newReadSession(sessionCaptor.capture());

    // And
    List<SessionOptions> capturedSessions = sessionCaptor.getAllValues();
    Assertions.assertEquals(3, capturedSessions.size());
    Assertions.assertEquals(getSocketTimeoutMs(firstConfig), capturedSessions.get(0).socketTimeout);
    Assertions.assertEquals(getConnectTimeoutMs(firstConfig), capturedSessions.get(0).connectTimeout);
    Assertions.assertEquals(getStmtTimeoutMs(firstConfig), capturedSessions.get(0).stmtTimeout);
    Assertions.assertEquals(getSocketTimeoutMs(secondConfig), capturedSessions.get(1).socketTimeout);
    Assertions.assertEquals(getConnectTimeoutMs(secondConfig), capturedSessions.get(1).connectTimeout);
    Assertions.assertEquals(getStmtTimeoutMs(secondConfig), capturedSessions.get(1).stmtTimeout);
    Assertions.assertEquals(getSocketTimeoutMs(thirdConfig), capturedSessions.get(2).socketTimeout);
    Assertions.assertEquals(getConnectTimeoutMs(thirdConfig), capturedSessions.get(2).connectTimeout);
    Assertions.assertEquals(getStmtTimeoutMs(thirdConfig), capturedSessions.get(2).stmtTimeout);
  }

  private static IStorage mockStorageFor(NakshaStorage config) {
    IStorage storage = mock(IStorage.class);
    when(storage.getConfig()).thenReturn(config);
    when(storage.newReadSession(any())).thenReturn(new MockReadSession(new FeatureTupleList()));
    return storage;
  }

  private static NakshaStorage customStorageConfig(Map<String, Object> customProps) {
    NakshaStorage storageConfig = new NakshaStorage();
    NakshaProperties storageProperties = storageConfig.getProperties();
    customProps.forEach(storageProperties::put);
    return storageConfig;
  }
}
