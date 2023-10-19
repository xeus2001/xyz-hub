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
 * A request to modify collections of the storage.
 *
 * @param <T> the collection-type to write.
 */
@AvailableSince(NakshaVersion.v2_0_7)
public class WriteCollections<T> extends WriteRequest<T, WriteCollections<T>> {

  /**
   * Creates a new empty write collections request.
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  public WriteCollections() {
    this(new ArrayList<>());
  }

  /**
   * Creates a new write collections request.
   *
   * @param modifies the operations to execute.
   */
  @AvailableSince(NakshaVersion.v2_0_7)
  public WriteCollections(@NotNull List<@NotNull ModifyQuery<T>> modifies) {
    super(modifies);
  }
}
