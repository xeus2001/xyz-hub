package com.here.naksha.cli.storages;

import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.*;
import naksha.model.objects.NakshaCollection;
import naksha.model.objects.NakshaFeature;
import naksha.model.objects.NakshaCatalog;
import naksha.model.request.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class GeneratingSession implements IReadSession {
    private final GeneratingStorage storage;
    private final SessionOptions sessionOptions;
    private final NakshaFeature templateFeature;

    GeneratingSession(
        @NotNull GeneratingStorage storage,
        @NotNull SessionOptions sessionOptions,
        @NotNull NakshaFeature templateFeature
    ) {
        this.sessionOptions = sessionOptions;
        this.storage = storage;
        this.templateFeature = templateFeature;
    }

    @Override
    public @NotNull Response executeRead(@NotNull ReadRequest request) {
        GeneratingStorageService service = storage.getService();
        FeatureTupleList featureTuples = service.generateDummyFeatureTuples(storage.getNumber(), storage.getNumOfFeaturesToGenerate());
        return new SuccessResponse(featureTuples);
    }

    @Override
    public void loadTuples(@NotNull List<? extends FeatureTuple> featureTuples, int from, int to) {
        GeneratingStorageService service = storage.getService();
        List<NakshaFeature> generatedFeatures = service.generateFeatures(
            to - from,
            storage.getTileIds(),
            storage.getIdsPrefix(),
            templateFeature
        );
        for (int i = from; i < to; ++i) {
            FeatureTuple featureTuple = featureTuples.get(i);
            NakshaFeature feature = generatedFeatures.get(i);
            featureTuple.setFeature(feature);
        }
    }

    @NotNull
    @Override
    public IStorage getStorage() {
        return storage;
    }

    @NotNull
    @Override
    public SessionOptions getOptions() {
        return sessionOptions;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public int getSocketTimeout() {
        return 0;
    }

    @Override
    public void setSocketTimeout(int i) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    @Override
    public int getStmtTimeout() {
        return 0;
    }

    @Override
    public void setStmtTimeout(int i) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    @Override
    public int getLockTimeout() {
        return 0;
    }

    @Override
    public void setLockTimeout(int i) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    @Nullable
    @Override
    public NakshaCatalog getCatalogById(@NotNull String mapId, boolean allowTombstone) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    @Nullable
    @Override
    public NakshaCatalog getCatalogByNumber(int catalogNumber, boolean allowTombstone) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    @Nullable
    @Override
    public NakshaCollection getCollectionById(
        @NotNull NakshaCatalog map,
        @NotNull String collectionId,
        boolean allowTombstone
    ) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    @Nullable
    @Override
    public NakshaCollection getCollectionByNumber(
        @NotNull NakshaCatalog catalog,
        int collectionNumber,
        boolean allowTombstone
    ) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "");
    }

    private final MemberProcessorMap processors = new MemberProcessorMap();
}
