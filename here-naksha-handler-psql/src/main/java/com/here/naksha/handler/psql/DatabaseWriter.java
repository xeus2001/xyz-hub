/*
 * Copyright (C) 2017-2020 HERE Europe B.V.
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

package com.here.naksha.handler.psql;

import static com.here.naksha.lib.core.NakshaContext.currentLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.here.naksha.lib.core.models.geojson.implementation.Feature;
import com.here.naksha.lib.core.models.geojson.implementation.FeatureCollection;
import com.here.naksha.lib.core.models.geojson.implementation.Geometry;
import com.vividsolutions.jts.geom.Coordinate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PGobject;

public class DatabaseWriter {
    public static final String UPDATE_ERROR_GENERAL = "Update has failed";
    public static final String UPDATE_ERROR_NOT_EXISTS = UPDATE_ERROR_GENERAL + " - Object does not exist";
    public static final String UPDATE_ERROR_UUID = UPDATE_ERROR_GENERAL + " - Object does not exist or UUID mismatch";
    public static final String UPDATE_ERROR_ID_MISSING = UPDATE_ERROR_GENERAL + " - Feature Id is missing";
    public static final String UPDATE_ERROR_PUUID_MISSING = UPDATE_ERROR_GENERAL + " -  Feature puuid is missing";

    public static final String DELETE_ERROR_GENERAL = "Delete has failed";
    public static final String DELETE_ERROR_NOT_EXISTS = DELETE_ERROR_GENERAL + " - Object does not exist";
    public static final String DELETE_ERROR_UUID = DELETE_ERROR_GENERAL + " - Object does not exist or UUID mismatch";

    public static final String INSERT_ERROR_GENERAL = "Insert has failed";

    protected static final String TRANSACTION_ERROR_GENERAL = "Transaction has failed";

    public static final String LOG_EXCEPTION_INSERT = "insert";
    public static final String LOG_EXCEPTION_UPDATE = "update";
    public static final String LOG_EXCEPTION_DELETE = "delete";

    protected static PGobject featureToPGobject(final Feature feature, Integer version) throws SQLException {
        final Geometry geometry = feature.getGeometry();
        feature.setGeometry(null); // Do not serialize the geometry in the JSON object

        final String json;

        try {
            if (version != null) feature.getProperties().getXyzNamespace().setVersion(version);
            json = feature.serialize();
        } finally {
            feature.setGeometry(geometry);
        }

        final PGobject jsonbObject = new PGobject();
        jsonbObject.setType("jsonb");
        jsonbObject.setValue(json);
        return jsonbObject;
    }

    protected static boolean getDeletedFlagFromFeature(Feature f) {
        return f.getProperties() == null
                ? false
                : f.getProperties().getXyzNamespace() == null
                        ? false
                        : f.getProperties().getXyzNamespace().isDeleted();
    }

    protected static PreparedStatement createStatement(Connection connection, String statement) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(statement);
        return preparedStatement;
    }

    protected static PreparedStatement createInsertStatement(
            Connection connection, String schema, String table, boolean withDeletedColumn) throws SQLException {
        return createStatement(connection, SQLQueryBuilder.insertStmtSQL(schema, table, withDeletedColumn));
    }

    protected static PreparedStatement createInsertWithoutGeometryStatement(
            Connection connection, String schema, String table, boolean withDeletedColumn) throws SQLException {
        return createStatement(
                connection, SQLQueryBuilder.insertWithoutGeometryStmtSQL(schema, table, withDeletedColumn));
    }

    protected static PreparedStatement createUpdateStatement(
            Connection connection, String schema, String table, boolean handleUUID, boolean withDeletedColumn)
            throws SQLException {
        return createStatement(connection, SQLQueryBuilder.updateStmtSQL(schema, table, handleUUID, withDeletedColumn));
    }

    protected static PreparedStatement createUpdateWithoutGeometryStatement(
            Connection connection, String schema, String table, boolean handleUUID, boolean withDeletedColumn)
            throws SQLException {
        return createStatement(
                connection, SQLQueryBuilder.updateWithoutGeometryStmtSQL(schema, table, handleUUID, withDeletedColumn));
    }

    protected static PreparedStatement deleteStmtSQLStatement(
            Connection connection, String schema, String table, boolean handleUUID) throws SQLException {
        return createStatement(connection, SQLQueryBuilder.deleteStmtSQL(schema, table, handleUUID));
    }

    protected static PreparedStatement versionedDeleteStmtSQLStatement(
            Connection connection, String schema, String table, boolean handleUUID) throws SQLException {
        return createStatement(connection, SQLQueryBuilder.versionedDeleteStmtSQL(schema, table, handleUUID));
    }

    protected static void setAutocommit(Connection connection, boolean isActive) throws SQLException {
        connection.setAutoCommit(isActive);
    }

    protected static FeatureCollection insertFeatures(
            @NotNull PsqlHandler processor,
            FeatureCollection collection,
            List<FeatureCollection.ModificationFailure> fails,
            List<Feature> inserts,
            Connection connection,
            boolean transactional,
            Integer version,
            boolean forExtendedSpace)
            throws SQLException, JsonProcessingException {
        if (transactional) {
            setAutocommit(connection, false);
            return DatabaseTransactionalWriter.insertFeatures(
                    processor, collection, fails, inserts, connection, version, forExtendedSpace);
        }
        setAutocommit(connection, true);
        return DatabaseStreamWriter.insertFeatures(processor, collection, fails, inserts, connection, forExtendedSpace);
    }

    protected static FeatureCollection updateFeatures(
            @NotNull PsqlHandler processor,
            FeatureCollection collection,
            List<FeatureCollection.ModificationFailure> fails,
            List<Feature> updates,
            Connection connection,
            boolean transactional,
            boolean handleUUID,
            Integer version,
            boolean forExtendedSpace)
            throws SQLException, JsonProcessingException {
        if (transactional) {
            setAutocommit(connection, false);
            return DatabaseTransactionalWriter.updateFeatures(
                    processor, collection, fails, updates, connection, handleUUID, version, forExtendedSpace);
        }
        setAutocommit(connection, true);
        return DatabaseStreamWriter.updateFeatures(
                processor, collection, fails, updates, connection, handleUUID, forExtendedSpace);
    }

    protected static void deleteFeatures(
            @NotNull PsqlHandler processor,
            List<FeatureCollection.ModificationFailure> fails,
            Map<String, String> deletes,
            Connection connection,
            boolean transactional,
            boolean handleUUID,
            Integer version)
            throws SQLException {
        if (transactional) {
            setAutocommit(connection, false);
            DatabaseTransactionalWriter.deleteFeatures(processor, fails, deletes, connection, handleUUID, version);
            return;
        }
        setAutocommit(connection, true);
        DatabaseStreamWriter.deleteFeatures(processor, fails, deletes, connection, handleUUID);
    }

    protected static void assure3d(Coordinate[] coords) {
        for (Coordinate coord : coords) {
            if (Double.valueOf(coord.z).isNaN()) coord.z = 0;
        }
    }

    protected static void logException(
            @Nullable Exception e, @NotNull PsqlHandler processor, @Nullable String action, @NotNull String table) {
        if (e != null && e.getMessage() != null && e.getMessage().contains("does not exist")) {
            /* If table not yet exist */
            currentLogger().info("{Failed to perform {} - table {} does not exists {}", action, table, e);
        } else currentLogger().info("Failed to perform {} on table {} {}", action, table, e);
    }
}
