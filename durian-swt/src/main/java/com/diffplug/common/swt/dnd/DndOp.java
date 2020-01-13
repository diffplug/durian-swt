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


import com.diffplug.common.base.Unhandled;
import com.diffplug.common.swt.SwtMisc;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;

public enum DndOp {
	COPY, MOVE;

	/** Returns DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE; */
	public static int dropAll() {
		return DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE;
	}

	/** Returns DND.DROP_COPY | DND.DROP_MOVE; */
	public static int dragAll() {
		return DND.DROP_COPY | DND.DROP_MOVE;
	}

	/** Return DND.DROP_COPY or DND.DROP_MOVE, as appropriate. */
	public int flag() {
		// @formatter:off
		switch (this) {
		case COPY: return DND.DROP_COPY;
		case MOVE: return DND.DROP_MOVE;
		default: throw Unhandled.enumException(this);
		}
		// @formatter:on
	}

	/** Attempts to set the detail of this event, if possible.  Returns true if it was possible. */
	public boolean trySetDetail(DropTargetEvent event) {
		if (SwtMisc.flagIsSet(flag(), event.operations)) {
			event.detail = flag();
			return true;
		} else {
			event.detail = DND.DROP_NONE;
			return false;
		}
	}
}
