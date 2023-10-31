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
package com.here.naksha.lib.hub2.space;

import com.here.naksha.lib.core.IEventHandler;
import com.here.naksha.lib.core.storage.IStorage;
import com.here.naksha.lib.hub2.EventPipelineFactory;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SpaceStorage implements IStorage {

  protected @Nullable Map<String, List<IEventHandler>> virtualSpaces;

  protected @Nullable EventPipelineFactory eventPipelineFactory;

  public void setVirtualSpaces(@NotNull Map<String, List<IEventHandler>> virtualSpaces) {
    this.virtualSpaces = virtualSpaces;
  }

  public void setEventPipelineFactory(@NotNull EventPipelineFactory eventPipelineFactory) {
    this.eventPipelineFactory = eventPipelineFactory;
  }
}
