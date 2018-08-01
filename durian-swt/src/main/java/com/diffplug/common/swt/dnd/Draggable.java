/*
 * Copyright 2018 DiffPlug
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
 */
package com.diffplug.common.swt.dnd;

import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;

/** Generic interface for a custom widget which supports dragging. */
public interface Draggable {
	void addDragSupport(DragSourceListener dragListener, Transfer[] transfers);

	default void addDragSupport(StructuredDrag drag) {
		addDragSupport(drag.getListener(), drag.getListener().transferArray());
	}
}
