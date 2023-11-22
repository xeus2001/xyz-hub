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
package com.here.naksha.lib.psql;

import com.here.naksha.lib.core.models.payload.events.QueryParameterList;
import org.jetbrains.annotations.NotNull;

public final class PsqlConfigBuilder extends PsqlAbstractConfigBuilder<PsqlConfig, PsqlConfigBuilder> {

  @SuppressWarnings("PatternVariableHidesField")
  @Override
  protected void setFromUrlParams(@NotNull QueryParameterList params) {
    super.setFromUrlParams(params);
    if (params.getValue("schema") instanceof String schema) {
      this.schema = schema;
    }
    if (params.getValue("app") instanceof String app) {
      this.appName = app;
    }
    if (params.getValue("appName") instanceof String appName) {
      this.appName = appName;
    }
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(@NotNull String schema) {
    this.schema = schema;
  }

  public @NotNull PsqlConfigBuilder withSchema(@NotNull String schema) {
    setSchema(schema);
    return this;
  }

  public @NotNull PsqlConfigBuilder withDefaultSchema(@NotNull String schema) {
    if (getSchema() == null) setSchema(schema);
    return this;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(@NotNull String appName) {
    this.appName = appName;
  }

  public @NotNull PsqlConfigBuilder withAppName(@NotNull String appName) {
    setAppName(appName);
    return this;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public @NotNull PsqlConfigBuilder withRole(String role) {
    setRole(role);
    return this;
  }

  public String getSearchPath() {
    return searchPath;
  }

  public void setSearchPath(String searchPath) {
    this.searchPath = searchPath;
  }

  public @NotNull PsqlConfigBuilder withSearchPath(String searchPath) {
    setSearchPath(searchPath);
    return this;
  }

  /** The database schema to use. */
  private String schema;

  /** The application name to set, when connecting to the database. */
  private String appName;

  /** The role to use after connection; if {@code null}, then the {@link #user} is used. */
  private String role;

  /** The search path to set; if {@code null}, automatically set. */
  private String searchPath;

  @Override
  public @NotNull PsqlConfig build() {
    if (schema == null) {
      throw new NullPointerException("schema");
    }
    if (appName == null) {
      throw new NullPointerException("appName");
    }
    if (db == null) {
      throw new NullPointerException("db");
    }
    if (user == null) {
      throw new NullPointerException("user");
    }
    if (password == null) {
      throw new NullPointerException("password");
    }
    return new PsqlConfig(
        host,
        port,
        db,
        user,
        password,
        connTimeout,
        stmtTimeout,
        lockTimeout,
        minPoolSize,
        maxPoolSize,
        idleTimeout,
        schema,
        appName,
        role,
        searchPath);
  }
}