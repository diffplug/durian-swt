/**
 * Copyright 2015 DiffPlug
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
package com.diffplug.common.swt;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

import com.google.common.collect.Maps;

/** Caches colors, and automatically manages their disposal. */
public class ColorPool {
	private final HashMap<RGB, Color> colorTable = Maps.newHashMap();
	private final Display display;

	private ColorPool(Widget parent) {
		display = parent.getDisplay();
		parent.addListener(SWT.Dispose, e -> colorTable.values().forEach(Color::dispose));
	}

	/** Returns a Color for the given RGB value. */
	public Color getColor(RGB rgb) {
		Color color = colorTable.get(rgb);
		if (color == null) {
			color = new Color(display, rgb);
			colorTable.put(rgb, color);
		}
		return color;
	}

	/** Returns a ColorPool for the given Widget, creating one if necessary. */
	public static ColorPool forWidget(Widget widget) {
		return onePerWidget.get(widget);
	}

	private static OnePerWidget<Widget, ColorPool> onePerWidget = OnePerWidget.from(ColorPool::new);
}
