/*
 * Copyright (C) 2017-2024 HERE Europe B.V.
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
package com.here.naksha.lib.core.models.storage;

import java.util.List;
import naksha.model.NakshaContext;
import naksha.model.NakshaVersion;
import naksha.model.objects.NakshaFeature;
import naksha.model.request.WriteRequest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract class representing WriteFeatures request alongwith list of features as context and list of violations.
 * Implementing class will define actual data type of context and violations.
 */
@ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
public abstract class ContextWriteFeatures extends WriteRequest {

  /**
   * The list of features passed as context, as part of Write request
   */
  @ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
  private @Nullable List<@NotNull NakshaContext> context;

  /**
   * The list of violations passed as part of Write request
   */
  @ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
  private @Nullable List<@NotNull NakshaFeature> violations;

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
  public @Nullable List<NakshaContext> getContext() {
    return context;
  }

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
  public void setContext(@Nullable List<NakshaContext> context) {
    this.context = context;
  }

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
  public @Nullable List<NakshaFeature> getViolations() {
    return violations;
  }

  @ApiStatus.AvailableSince(NakshaVersion.v2_0_11)
  public void setViolations(@Nullable List<NakshaFeature> violations) {
    this.violations = violations;
  }
}
