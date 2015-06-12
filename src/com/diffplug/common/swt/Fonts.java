/*
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

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import com.google.common.base.Preconditions;

import com.diffplug.common.base.Errors;
import com.diffplug.common.swt.os.OS;

/** Registry of fonts, especially system fonts. */
public class Fonts {
	private static Fonts instance;

	private static Fonts getInstance() {
		Display display = SwtMisc.assertUI();
		if (instance == null) {
			instance = new Fonts(display);
		}
		return instance;
	}

	private final FontRegistry fontRegistry;

	private Fonts(Display display) {
		this.fontRegistry = new FontRegistry(display);
	}

	private Font getFont(String name, int size, int style) {
		String key = getFontKey(name, size, style);
		if (!fontRegistry.hasValueFor(key)) {
			fontRegistry.put(key, new FontData[]{new FontData(name, size, style)});
		}
		return fontRegistry.get(key);
	}

	private String getFontKey(String name, int size, int style) {
		return name + "_" + Integer.toString(size) + "_" + Integer.toString(style);
	}

	/** Returns the given font. */
	public static Font get(String name, int size, int style) {
		Preconditions.checkArgument(size > 0, "Size must be greater than 0.  Did you switch the size and the style? size=%s style=%s", size, style);
		return getInstance().getFont(name, size, style);
	}

	/** Returns the default font for this system. */
	public static Font getSystem() {
		FontData font = SwtMisc.assertUI().getSystemFont().getFontData()[0];
		return get(font.getName(), font.getHeight(), SWT.NONE);
	}

	/** Returns the default bold font for this system. */
	public static Font getSystemBold() {
		FontData font = SwtMisc.assertUI().getSystemFont().getFontData()[0];
		return get(font.getName(), font.getHeight(), SWT.BOLD);
	}

	/** Returns a monospace font for this system. */
	public static Font getSystemMonospace() {
		FontData monospace = getSystemMonospaceFontData();
		return get(monospace.getName(), monospace.getHeight(), SWT.NONE);
	}

	/** The cached value of the best monospace font on this system. */
	private static FontData bestSystemMonospaceFontData;

	/** Calculates the best monospaced font available on this system, can be called from any thread. */
	public static FontData getSystemMonospaceFontData() {
		if (bestSystemMonospaceFontData != null) {
			return bestSystemMonospaceFontData;
		}

		// get the default fonts for each OS
		String defaultFonts = OS.getNative().winMacLinux(
				"Consolas:10;Courier New:10",
				"Monaco:11;Courier:12;Courier New:12",
				"Monospace:10");

		Display display = Display.getDefault();
		String[] fonts = defaultFonts.split(";");
		for (String font : fonts) {
			// parse out each of the suggested fonts
			String[] pieces = font.split(":");
			String fontName = pieces[0];
			int fontSize = Integer.parseInt(pieces[1]);

			// get the available fonts
			FontData[] available = display.getFontList(fontName, true);
			if (available != null && available.length > 0) {
				// it was available, so we'll return it
				bestSystemMonospaceFontData = new FontData(fontName, fontSize, SWT.NONE);
				return bestSystemMonospaceFontData;
			}
		}

		// our lists didn't work, so we'll fall back to the system font
		Errors.log().handle(new IllegalArgumentException("Couldn't find a good monospaced font."));
		bestSystemMonospaceFontData = display.getSystemFont().getFontData()[0];
		return bestSystemMonospaceFontData;
	}
}
