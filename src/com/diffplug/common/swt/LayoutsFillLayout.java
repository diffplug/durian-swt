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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;

/**
 * A fluent api for setting and modifying a FillLayout.  Created by static methods in Layouts.
 * 
 * Inspired by Moritz Post: http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/
 */
public class LayoutsFillLayout {
	private final FillLayout fillLayout;

	LayoutsFillLayout(FillLayout fillLayout) {
		this.fillLayout = fillLayout;
	}

	/** Returns the raw FillLayout. */
	public FillLayout getRaw() {
		return fillLayout;
	}

	/** Sets the margins to zero. */
	public LayoutsFillLayout noMargin() {
		fillLayout.marginWidth = 0;
		fillLayout.marginHeight = 0;
		return this;
	}

	/** Sets the spacing to zero. */
	public LayoutsFillLayout noSpacing() {
		fillLayout.spacing = 0;
		return this;
	}

	public LayoutsFillLayout vertical() {
		fillLayout.type = SWT.VERTICAL;
		return this;
	}

	public LayoutsFillLayout horizontal() {
		fillLayout.type = SWT.HORIZONTAL;
		return this;
	}

	public LayoutsFillLayout marginWidth(int marginWidth) {
		fillLayout.marginWidth = marginWidth;
		return this;
	}

	public LayoutsFillLayout marginHeight(int marginHeight) {
		fillLayout.marginHeight = marginHeight;
		return this;
	}

	public LayoutsFillLayout spacing(int spacing) {
		fillLayout.spacing = spacing;
		return this;
	}
}
