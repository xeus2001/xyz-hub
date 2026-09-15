package com.here.naksha.cli.copy.service;

import naksha.model.*;
import naksha.base.TupleNumber;
import naksha.base.Version;
import naksha.model.objects.NakshaFeature;
import naksha.model.request.*;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class CopyServiceTestUtils {
    private CopyServiceTestUtils() {
    }

    public static IWriteSession createThrowingWriteSessionForStorage(IStorage storage, SessionOptions sessionOptions) {
        IWriteSession writeSession = createWriteSessionForStorage(storage, sessionOptions);
        when(writeSession.execute(any())).thenThrow(new RuntimeException());
        return writeSession;
    }

    public static IWriteSession createWriteSessionForStorageReturningErrorResponse(IStorage storage, SessionOptions sessionOptions) {
        IWriteSession writeSession = createWriteSessionForStorage(storage, sessionOptions);
        when(writeSession.execute(any())).thenReturn(new ErrorResponse());
        return writeSession;
    }

    public static IWriteSession createWriteSessionForStorageReturningUnexpectedResponse(IStorage storage, SessionOptions sessionOptions) {
        IWriteSession writeSession = createWriteSessionForStorage(storage, sessionOptions);
        when(writeSession.execute(any())).thenReturn(new Response());
        return writeSession;
    }

    public static FeatureTupleList nakshaFeatureListToFeatureTupleList(List<NakshaFeature> nakshaFeatures) {
        FeatureTupleList featureTuples = new FeatureTupleList();
        featureTuples.setCapacity(nakshaFeatures.size());
        TupleNumber dummyTupleNumber = new TupleNumber(
                0L, 0, 0, 0L, Version.HEAD.number
        );
        for (NakshaFeature feature : nakshaFeatures) {
            FeatureTuple featureTuple = new FeatureTuple(dummyTupleNumber, null);
            featureTuple.setFeature(feature);
            featureTuples.add(featureTuple);
        }
        return featureTuples;
    }

    public static IWriteSession createWriteSessionForStorageReturningSuccessResponse(IStorage storage, SessionOptions sessionOptions) {
        IWriteSession writeSession = createWriteSessionForStorage(storage, sessionOptions);
        when(writeSession.execute(any())).thenReturn(new SuccessResponse());
        return writeSession;
    }

    public static IWriteSession createWriteSessionForStorage(IStorage storage, SessionOptions sessionOptions) {
        IWriteSession writeSession = mock();
        when(storage.newWriteSession(sessionOptions)).thenReturn(writeSession);
        return writeSession;
    }

    public static List<Write> captureWrites(IWriteSession writeSession) {
        return writeRequestsToWrites(captureRequestsOfType(writeSession, WriteRequest.class));
    }

    public static List<Write> writeRequestsToWrites(List<WriteRequest> writeRequests) {
        return writeRequests.stream()
                .flatMap(wr -> wr.getWrites().stream())
                .toList();
    }

    public static IStorage createTargetStorage(SessionOptions sessionOptions) {
        IStorage targetStorage = mock();
        when(targetStorage.useWriteSession(eq(sessionOptions), any())).thenCallRealMethod();
        return targetStorage;
    }

    public static IStorage createSourceStorage(SessionOptions sessionOptions) {
        IStorage sourceStorage = mock();
        when(sourceStorage.useReadSession(eq(sessionOptions), any())).thenCallRealMethod();
        return sourceStorage;
    }

    public static <T extends Request, S extends IReadSession> List<T> captureRequestsOfType(S session, Class<T> type) {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(session, atLeastOnce()).execute(captor.capture());
        return captor.getAllValues().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    public static void assertCreateFeaturesWrites(List<Write> writes, List<NakshaFeature> expectedFeatures, CopyElement target) {
        List<NakshaFeature> actualFeatures = writes.stream()
                .map(w -> {
                    assertCreateWrite(w, target);
                    return w.getFeature();
                })
                .toList();

        assertEquals(
                expectedFeatures.size(), actualFeatures.size(),
                "Features sizes do not match"
        );
        assertTrue(
                actualFeatures.containsAll(expectedFeatures) && expectedFeatures.containsAll(actualFeatures),
                "Feature lists do not contain the same elements"
        );
    }

    private static void assertCreateWrite(Write w, CopyElement target) {
        assertEquals(WriteOp.CREATE, w.getOp(), "Every write operation should be CREATE");
        assertEquals(target.getCollectionId(), w.getCollectionId(),
                "Every write Collection ID should match target Collection ID"
        );
        assertEquals(target.getMapId(), w.getCatalogId(),
                "Every write Map ID should match target Map ID"
        );
    }
}
