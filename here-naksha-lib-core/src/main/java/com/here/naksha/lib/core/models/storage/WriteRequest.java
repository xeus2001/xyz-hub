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
package com.here.naksha.lib.core.models.storage;

import com.here.naksha.lib.core.NakshaVersion;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * All write requests should extend this base class.
 *
 * @param <T> the object-type to write.
 * @param <SELF> the self-type.
 */
@AvailableSince(NakshaVersion.v2_0_7)
public abstract class WriteRequest<T, SELF extends WriteRequest<T, SELF>> extends Request<SELF> {

  /**
   * Creates a new write request.
   *
   * @param ops the operations to execute.
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  protected WriteRequest(@NotNull List<@NotNull WriteOp<T>> ops) {
    this.ops = new ArrayList<>();
  }

  /**
   * The operations to be executed.
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  public @NotNull List<@NotNull WriteOp<T>> ops;
}
