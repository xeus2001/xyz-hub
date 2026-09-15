package com.here.naksha.cli.storages;

import com.here.naksha.cli.utils.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.here.naksha.cli.utils.JsonParserException;
import kotlin.reflect.KClass;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.base.Platform;
import naksha.base.StringList;
import naksha.model.*;
import naksha.model.objects.NakshaFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static naksha.base.NakshaExceptionKt.unsupportedOp;

public final class GeneratingStorage extends AbstractStorage<GeneratingStorageConfig> {
    public static final String DEFAULT_IDS_PREFIX = "gen";
    private final JsonParser jsonParser = new JsonParser();
    private int numOfFeaturesToGenerate;
    private String idsPrefix;
    private List<String> tileIds;
    private GeneratingStorageService service;
    @Nullable
    private String templateFeatureString;

    @NotNull
    @Override
    public KClass<GeneratingStorageConfig> getConfigKlass() {
        return Platform.klassFor(GeneratingStorageConfig.class);
    }

    @NotNull
    @Override
    public IWriteSession newWriteSession(@Nullable SessionOptions options) {
        throw new NakshaException(NakshaError.UNSUPPORTED_OPERATION, "Read-only storage!");
    }

    @NotNull
    @Override
    public IReadSession newReadSession(@Nullable SessionOptions sessionOptions) {
        try {
            if (sessionOptions == null) {
                sessionOptions = SessionOptions.from(NakshaContext.currentContext());
            }
            NakshaFeature templateFeature = switch (templateFeatureString) {
                case null -> new NakshaFeature();
                default -> jsonParser.parse(templateFeatureString, NakshaFeature.class);
            };
            return new GeneratingSession(
                    this,
                    sessionOptions,
                    templateFeature
            );
        }catch (JsonParserException e) {
            throw new NakshaException(NakshaError.UNINITIALIZED, "Failed to parse feature's template string!", e);
        }
    }

    @Override
    public int getHardCap() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void initStorage(
        @NotNull GeneratingStorageConfig storageConfig,
        @Nullable Boolean create,
        @Nullable Boolean upgrade
    ) {
        try {
            GeneratingStorageConfigProperties configProperties = storageConfig.getProperties();
            numOfFeaturesToGenerate = requireCount(configProperties.getCount());
            idsPrefix = getIdsPrefixOrDefault(configProperties, DEFAULT_IDS_PREFIX);
            tileIds = requireTileIds(configProperties);
            templateFeatureString = loadTemplateFeatureString(configProperties.getFeatureTemplateFilePath()).orElse(null);
            service = new GeneratingStorageService();
        }catch (Exception e) {
            throw new NakshaException(NakshaError.INITIALIZATION_FAILED, "Failed to init GeneratingStorage!", e);
        }
    }

    @Override
    protected void afterInit() {
        // nothing to do
    }

    @Override
    protected void shutdownStorage(boolean dropCache) {
        // nothing to do
    }

    int getNumOfFeaturesToGenerate() {
        return numOfFeaturesToGenerate;
    }

    String getIdsPrefix() {
        return idsPrefix;
    }

    List<String> getTileIds() {
        return tileIds;
    }

    GeneratingStorageService getService() {
        return service;
    }

    private int requireCount(@Nullable Integer count) {
        if (count == null) {
            throw new NakshaException(NakshaError.ILLEGAL_ARGUMENT, "Provide count in the config properties.");
        }
        return count;
    }

    private String getIdsPrefixOrDefault(GeneratingStorageConfigProperties configProperties, String defaultPrefix) {
        String idsPrefix = configProperties.getIdsPrefix();
        if (idsPrefix == null) {
            return defaultPrefix;
        }
        return idsPrefix;
    }

    private Optional<String> loadTemplateFeatureString(@Nullable String featureTemplateFilePath) {
        if (featureTemplateFilePath != null) {
            try {
                Path path = Path.of(featureTemplateFilePath);
                return Optional.of(Files.readString(path));
            } catch (IOException e) {
                throw new NakshaException(NakshaError.EXCEPTION, "Problem while loading the feature template!", e);
            }
        }
        return Optional.empty();
    }

    private StringList requireTileIds(GeneratingStorageConfigProperties configProperties) {
        if (configProperties.getTileIds() == null && configProperties.getTileIdsCsvFilePath() == null) {
            throw new NakshaException(NakshaError.ILLEGAL_ARGUMENT, "Provide tileIds in the config properties.");
        }
        if (configProperties.getTileIds() != null && configProperties.getTileIdsCsvFilePath() != null) {
            throw new NakshaException(NakshaError.ILLEGAL_ARGUMENT, "Provide only one source of tileIds.");
        }

        StringList tileIds;
        if (configProperties.getTileIdsCsvFilePath() != null) {
            tileIds = StringList.fromList(loadTileIdsFromCsv(configProperties.getTileIdsCsvFilePath()));
        } else {
            tileIds = configProperties.getTileIds();
        }

        if (tileIds.isEmpty()) {
            throw new NakshaException(NakshaError.ILLEGAL_ARGUMENT, "Should be at least one tileId.");
        }
        return tileIds;
    }

    private List<String> loadTileIdsFromCsv(String pathToFile) {
        try {
            Path path = Paths.get(pathToFile);
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new NakshaException(NakshaError.EXCEPTION, "Problem while loading tileIds from CSV file!", e);
        }
    }

    @Override
    public @NotNull IStreamSession newStreamSession(@Nullable SessionOptions options) {
        throw unsupportedOp("newStreamSession");
    }
}
