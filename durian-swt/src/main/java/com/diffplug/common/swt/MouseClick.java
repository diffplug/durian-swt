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
package com.diffplug.common.swt;


import java.util.function.Predicate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

/** Enum to help model different mouse clicks. */
public enum MouseClick implements Predicate<Event> {
	LEFT, MIDDLE, RIGHT;

	public int code() {
		return ordinal() + 1;
	}

	@Override
	public boolean test(Event e) {
		return e.button == code();
	}

	/** Should we use MouseDown or MouseUp for right-clicks?  This (down) is the answer. */
	public static final int RIGHT_CLICK_EVENT = SWT.MouseDown;
}
