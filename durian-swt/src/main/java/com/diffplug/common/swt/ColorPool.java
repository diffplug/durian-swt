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


import com.diffplug.common.collect.Maps;
import java.util.HashMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/** Caches {@link Color}s, and automatically manages their disposal. */
public class ColorPool {
	private final HashMap<RGB, Color> colorTable = new HashMap<>();

	private ColorPool() {}

	/** Returns a Color for the given RGB value. */
	public Color getColor(RGB rgb) {
		return colorTable.computeIfAbsent(rgb, raw -> new Color(raw));
	}

	/** Returns a ColorPool for the given Widget, creating one if necessary. */
	public static ColorPool forWidget(Widget widget) {
		return onePerWidget.forWidget(widget);
	}

	/** Returns a ColorPool for the given ControlWrapper, creating one if necessary. */
	public static ColorPool forWidget(ControlWrapper wrapper) {
		return onePerWidget.forWidget(wrapper.getRootControl());
	}

	private static final OnePerWidget<Widget, ColorPool> onePerWidget = OnePerWidget.from(unused -> new ColorPool());
}
