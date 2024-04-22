-- noinspection SqlResolveForFile @ routine/"ST_GeomFromTWKB"
-- noinspection SqlResolveForFile @ routine/"ST_GeomFromEWKB"
-- noinspection SqlResolveForFile @ routine/"ST_GeomFromWKB"

-- Read: https://www.postgresql.org/docs/current/sql-createfunction.html
--
-- Note: We need to set search_path on functions that need a specific one, otherwise
--       autovacuum (automatic analyze job needed for statistics) will raise an error,
--       because it executes the functions under a restrictive search_path setting,
--       being just pg_catalog, in fact!
--
-- RETURNS NULL ON NULL INPUT or STRICT
--   indicates that the function always returns null whenever any of its arguments are null.
--   If this parameter is specified, the function is not executed when there are null arguments;
--   instead a null result is assumed automatically.
--
-- PARALLEL UNSAFE indicates that the function can't be executed in parallel mode and the
--   presence of such a function in an SQL statement forces a serial execution plan. This is the default.
-- PARALLEL RESTRICTED indicates that the function can be executed in parallel mode, but
--   the execution is restricted to parallel group leader.
-- PARALLEL SAFE indicates that the function is safe to run in parallel mode without restriction.
CREATE EXTENSION IF NOT EXISTS plv8;

CREATE SCHEMA IF NOT EXISTS public;
CREATE SCHEMA IF NOT EXISTS topology;
CREATE SCHEMA IF NOT EXISTS "${schema}";

-- Set search path and install postgis.
SET SESSION search_path TO "${schema}", public, topology;
CREATE EXTENSION IF NOT EXISTS btree_gist SCHEMA public;
CREATE EXTENSION IF NOT EXISTS btree_gin SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis_topology SCHEMA topology;

-- Restore search_path (postgis_topology modifies it), then install hint-plan.
SET SESSION search_path TO "${schema}", public, topology;
CREATE SCHEMA IF NOT EXISTS hint_plan;
CREATE EXTENSION IF NOT EXISTS pg_hint_plan SCHEMA hint_plan;
COMMIT;

-- Restore search_path (hint_plan modifies it), install functions.
SET SESSION search_path TO "${schema}", topology, hint_plan, public;

-- Returns the packed Naksha extension version: 16 bit major, 16 bit minor, 16 bit revision, 8 bit pre-release tag, 8 bit pre-release version.
CREATE OR REPLACE FUNCTION naksha_version() RETURNS int8
LANGUAGE 'plpgsql'
IMMUTABLE
PARALLEL SAFE
AS $$ BEGIN
  RETURN ${version};
END $$;

-- Returns the storage-id of this storage, this is created when the Naksha extension is installed and never changes.
CREATE OR REPLACE FUNCTION naksha_storage_id() RETURNS text
LANGUAGE 'plpgsql'
IMMUTABLE
PARALLEL SAFE
AS $$ BEGIN
  RETURN '${storage_id}';
END $$;

-- Returns the schema of this storage, this is created when the Naksha extension is installed and never changes.
CREATE OR REPLACE FUNCTION naksha_schema() RETURNS text
LANGUAGE 'plpgsql'
IMMUTABLE
PARALLEL SAFE
AS $$ BEGIN
  RETURN '${schema}';
END $$;

CREATE OR REPLACE FUNCTION naksha_start_session(app_name text, stream_id text, app_id text, author text)
RETURNS void
LANGUAGE 'plv8'
VOLATILE
PARALLEL UNSAFE
SET search_path FROM CURRENT
AS $$
  if (typeof require !== "function") {
    var commonjs2_init = plv8.find_function("commonjs2_init");
    commonjs2_init();
    if (typeof require !== "function") {
      plv8.elog(ERROR, "Failed to initialize module system");
    }
  }
  let naksha = require("naksha");
  naksha.JsPlv8Env.Companion.initialize();
  let jb = require("jbon");
  // always create new session as previous one could be for different storage or schema
  session = new naksha.NakshaSession(new naksha.JsPlv8Sql(), '${schema}', '${storage_id}', app_name, stream_id, app_id, author);
  jb.JbSession.Companion.threadLocal.set(session);
$$;

CREATE OR REPLACE FUNCTION naksha_clear_session() RETURNS void
LANGUAGE 'plv8'
VOLATILE
PARALLEL UNSAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let session = jb.JbSession.Companion.threadLocal.get();
  if (session != null) {
    session.clear();
  }
$$;

CREATE OR REPLACE FUNCTION naksha_txn() RETURNS int8
LANGUAGE 'plv8'
VOLATILE
PARALLEL UNSAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  return session.txn().value;
$$;

CREATE OR REPLACE FUNCTION naksha_trigger_before() RETURNS trigger
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  let mapi = require("jbon").Jb.map;
  let t = new naksha.PgTrigger(
    TG_OP, TG_NAME, TG_WHEN, TG_LEVEL, TG_RELID, TG_TABLE_NAME, TG_TABLE_SCHEMA,
    mapi.isMap(NEW) ? mapi.asMap(NEW) : null,
    mapi.isMap(OLD) ? mapi.asMap(OLD) : null
  );
  session.triggerBefore(t);
  if (TG_OP == "INSERT" || TG_OP == "UPDATE") {
    return NEW
  }
  return OLD;
$$;

CREATE OR REPLACE FUNCTION naksha_trigger_after() RETURNS trigger
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  let mapi = require("jbon").Jb.map;
  let t = new naksha.PgTrigger(
    TG_OP, TG_NAME, TG_WHEN, TG_LEVEL, TG_RELID, TG_TABLE_NAME, TG_TABLE_SCHEMA,
    mapi.isMap(NEW) ? mapi.asMap(NEW) : null,
    mapi.isMap(OLD) ? mapi.asMap(OLD) : null
  );
  session.triggerAfter(t);
  if (TG_OP == "INSERT" || TG_OP == "UPDATE") {
    return NEW
  }
  return OLD;
$$;

-- TODO: If needed, create a naksha_write_unordered_features!
-- This function expects that operations are ordered by id to avoid deadlocks
-- CREATED, UPDATED -> return null for tags, feature, geo_type and geo
-- DELETED, PURGED, ERROR -> return all data
CREATE OR REPLACE FUNCTION naksha_write_features(
  collection_id text,
  ops bytea[], -- XyzOp (op, id, uuid)
  features bytea[], -- JbFeature (without XZY namespace)
  geometries_type int2[],
  geometries_bytes bytea[], -- WKB, EWKB, TWKB
  tags bytea[] -- XyzTags
) RETURNS TABLE (op text, id text, xyz bytea, tags bytea, feature bytea, geo_type int2, geo bytea, err_no text, err_msg text)
LANGUAGE 'plv8'
VOLATILE
PARALLEL UNSAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  session.writeFeatures(collection_id, ops, features, geometries_type, geometries_bytes, tags, true);
$$;

-- CREATED, UPDATED -> return null for tags, feature, geo_type and geo
-- DELETED, PURGED, ERROR -> return all data
CREATE OR REPLACE FUNCTION naksha_write_collections(
  ops bytea[], -- XyzOp (op, id, uuid)
  features bytea[], -- JbFeature (without XZY namespace)
  geometries_type int2[],
  geometries_bytes bytea[], -- WKB, EWKB, TWKB
  tags bytea[] -- XyzTags
) RETURNS TABLE (op text, id text, xyz bytea, tags bytea, feature bytea, geo_type int2, geo bytea, err_no text, err_msg text)
LANGUAGE 'plv8'
VOLATILE
PARALLEL UNSAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  session.writeCollections(ops, features, geometries_type, geometries_bytes, tags);
$$;

CREATE OR REPLACE FUNCTION naksha_set_stack_trace(enable bool) RETURNS void
LANGUAGE 'plv8'
VOLATILE
PARALLEL UNSAFE
SET search_path FROM CURRENT
AS $$
  require("naksha").Static.PRINT_STACK_TRACES = enable;
$$;

CREATE OR REPLACE FUNCTION naksha_err_no() RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  return session.errNo;
$$;

CREATE OR REPLACE FUNCTION naksha_err_msg() RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  return session.errMsg;
$$;

CREATE OR REPLACE FUNCTION naksha_partition_number(id text) RETURNS int4
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  return require("naksha").Static.partitionNumber(id);
$$;

CREATE OR REPLACE FUNCTION naksha_partition_id(id text) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  return require("naksha").Static.partitionNameForId(id);
$$;

CREATE OR REPLACE FUNCTION naksha_hst_partition_id(id text, txn_next int8) RETURNS int4
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let jbon = require("jbon");
  let session = naksha.NakshaSession.Companion.get();
  return naksha.Static.hstPartitionNameForId(id, new jbon.NakshaTxn(txn_next));
$$;

CREATE OR REPLACE FUNCTION naksha_geometry(geo_type int2, geo_bytes bytea) RETURNS geometry
LANGUAGE 'plpgsql'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
BEGIN
  IF geo_type = 1 THEN
    RETURN ST_GeomFromWKB(geo_bytes);
  ELSIF geo_type = 2 THEN
    RETURN ST_GeomFromEWKB(geo_bytes);
  ELSIF geo_type = 3 THEN
    RETURN ST_GeomFromTWKB(geo_bytes);
  ELSE
    RETURN null;
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION jsonb_to_op(op jsonb) RETURNS bytea
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let builder = jb.XyzBuilder.Companion.create();
  let opCode = jb.XyzOp.Companion.getOpCode(op["op"]);
  return builder.buildXyzOp(opCode, op["id"], op["uuid"]);
$$;

CREATE OR REPLACE FUNCTION json_to_op(op_json text) RETURNS bytea
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let builder = jb.XyzBuilder.Companion.create();
  let op = jb.Jb.env.parse(op_json);
  let opCode = jb.XyzOp.Companion.getOpCode(op["op"]);
  return builder.buildXyzOp(opCode, op["id"], op["uuid"]);
$$;

CREATE OR REPLACE FUNCTION op_to_jsonb(op bytea) RETURNS jsonb
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzOp = new jb.XyzOp();
  xyzOp.mapBytes(op);
  return xyzOp.toIMap();
$$;

CREATE OR REPLACE FUNCTION op_to_json(op bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzOp = new jb.XyzOp();
  xyzOp.mapBytes(op);
  return jb.Jb.env.stringify(xyzOp.toIMap());
$$;

CREATE OR REPLACE FUNCTION xyz_created_at(xyz bytea) RETURNS int8
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.createdAt();
$$;

CREATE OR REPLACE FUNCTION xyz_updated_at(xyz bytea) RETURNS int8
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.updatedAt();
$$;

CREATE OR REPLACE FUNCTION xyz_txn(xyz bytea) RETURNS int8
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.txn().value;
$$;

CREATE OR REPLACE FUNCTION xyz_version(xyz bytea) RETURNS int4
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.version();
$$;

CREATE OR REPLACE FUNCTION xyz_extent(xyz bytea) RETURNS int8
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.extent();
$$;

CREATE OR REPLACE FUNCTION xyz_author(xyz bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.author();
$$;

CREATE OR REPLACE FUNCTION xyz_author_ts(xyz bytea) RETURNS int8
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.authorTs();
$$;

CREATE OR REPLACE FUNCTION xyz_app_id(xyz bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.appId();
$$;

CREATE OR REPLACE FUNCTION xyz_uuid(xyz bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.uuid();
$$;

CREATE OR REPLACE FUNCTION xyz_puuid(xyz bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.puuid();
$$;

CREATE OR REPLACE FUNCTION xyz_action(xyz bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.actionAsString();
$$;

CREATE OR REPLACE FUNCTION xyz_grid(xyz bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  return xyzNs.grid();
$$;

CREATE OR REPLACE FUNCTION xyz_to_jsonb(xyz bytea, tags bytea) RETURNS jsonb
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let jb = require("jbon");
  let session = naksha.NakshaSession.Companion.get();
  let xyzNs = new jb.XyzNs();
  xyzNs.mapBytes(xyz);
  let xyzTags = new jb.XyzTags();
  xyzTags.mapBytes(tags);
  return xyzNs.toIMap(session.storageId, xyzTags.isMapped() ? xyzTags.tagsArray() : null);
$$;

-- query like:
-- where tags_to_jsonb(tags) @? '$.x?(@ starts with "Hello")'
-- where tags_to_jsonb(tags) @? '$.y?(@ >= 500)'
-- where tags_to_jsonb(tags) d @? '$.crid?(@ starts with "234")'
-- where tags_to_jsonb(tags) d @? '$.grid?(@ starts with "234")'
-- See:
-- https://www.postgresql.org/docs/16/functions-json.html#FUNCTIONS-SQLJSON-PATH
-- https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html
-- https://goessner.net/articles/JsonPath/
CREATE OR REPLACE FUNCTION tags_to_jsonb(tags bytea) RETURNS jsonb
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  if (tags == null) return {};
  let jb = require("jbon");
  let xyzTags = new jb.XyzTags();
  xyzTags.mapBytes(tags);
  return xyzTags.tagsMap();
$$;

CREATE OR REPLACE FUNCTION jsonb_to_tags(tags_json jsonb) RETURNS bytea
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  // TODO: Fix me!!!
  let jb = require("jbon");
  let builder = jb.XyzBuilder.Companion.create();
  let keys = Object.keys(tags_json);
  let i = 0;
  builder.startTags();
  while (i < keys.length) {
    builder.writeTag()
  }
  let opCode = jb.XyzOp.Companion.getOpCode(op["op"]);
  return builder.buildXyzOp(opCode, op["id"], op["uuid"]);
$$;

CREATE OR REPLACE FUNCTION feature_id(feature bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  return session.getFeatureId(feature);
$$;

CREATE OR REPLACE FUNCTION feature_type(feature bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  return session.getFeatureType(feature);
$$;

CREATE OR REPLACE FUNCTION jsonb_to_feature(feature jsonb) RETURNS bytea
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  return require("jbon").JbBuilder.Companion.create(1000).buildFeatureFromMap(feature);
$$;

CREATE OR REPLACE FUNCTION json_to_feature(feature text) RETURNS bytea
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  return require("jbon").JbBuilder.Companion.create(1000).buildFeatureFromMap(JSON.parse(feature));
$$;

CREATE OR REPLACE FUNCTION feature_to_jsonb(feature bytea) RETURNS jsonb
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let reader = new jb.JbMapFeature();
  reader.mapBytes(feature);
  let map = reader.root().toIMap();
  map["id"] = reader.id();
  return map;
$$;

CREATE OR REPLACE FUNCTION feature_to_json(feature bytea) RETURNS text
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let jb = require("jbon");
  let reader = new jb.JbMapFeature();
  reader.mapBytes(feature);
  let map = reader.root().toIMap();
  map["id"] = reader.id();
  return JSON.stringify(map);
$$;

CREATE OR REPLACE FUNCTION row_to_ns(created_at int8, updated_at int8, txn int8, action int2, version int4, author_ts int8,
 uid int4, app_id text, author text, geo_grid int4, puid int4, ptxn int8, collection_id text) RETURNS bytea
LANGUAGE 'plv8'
IMMUTABLE
PARALLEL SAFE
SET search_path FROM CURRENT
AS $$
  let naksha = require("naksha");
  let session = naksha.NakshaSession.Companion.get();
  let mapi = require("jbon").Jb.map;
  let map = mapi.newMap();
  map["created_at"] = created_at;
  map["updated_at"] = updated_at;
  map["txn"] = txn;
  map["action"] = action;
  map["version"] = version;
  map["author_ts"] = author_ts;
  map["uid"] = uid;
  map["puid"] = puid;
  map["ptxn"] = ptxn;
  map["app_id"] = app_id;
  map["author"] = author;
  map["geo_grid"] = geo_grid;
  return session.xyzNsFromRow(collection_id, map)
$$;
