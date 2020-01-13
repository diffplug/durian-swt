/*
 * Copyright 2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.common.swt.dnd;


import com.diffplug.common.swt.OnePerWidget;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

/**
 * Provides a mechanism for a single control to route its DragSourceEvents to various listeners.
 * 
 * There is only one active listener at a time.
 */
public final class DragSourceRouter {
	private final DragSource source;
	private DragSourceListener currentListener;

	/** Private constructor to force people to use the map. */
	private DragSourceRouter(Control ctl) {
		source = new DragSource(ctl, DndOp.dragAll());
		currentListener = null;
	}

	/** Sets the DragSourceListener which will get called for this DragSource. */
	public void setActiveListener(DragSourceListener newListener, Transfer[] transfers) {
		if (currentListener != null) {
			source.removeDragListener(currentListener);
		}
		currentListener = newListener;
		source.setTransfer(transfers);
		if (currentListener != null) {
			source.addDragListener(currentListener);
		}
	}

	/** Returns the MultipleDragSource for the given Control. */
	public static DragSourceRouter forControl(final Control ctl) {
		return onePerControl.forWidget(ctl);
	}

	private static OnePerWidget<Control, DragSourceRouter> onePerControl = OnePerWidget.from(DragSourceRouter::new);
}
