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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;

/**
 * A fluent api for setting and modifying a {@link FillLayout}, created by {@link Layouts}.
 * 
 * Inspired by <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">Moritz Post's blog post.</a>.
 */
public class LayoutsFillLayout extends LayoutWrapper<FillLayout> {
	LayoutsFillLayout(FillLayout fillLayout) {
		super(fillLayout);
	}

	/** Sets all margins to the given value */
	@Override
	public LayoutsFillLayout margin(int margin) {
		wrapped.marginWidth = margin;
		wrapped.marginHeight = margin;
		return this;
	}

	/** Sets the margins to zero. */
	public LayoutsFillLayout noMargin() {
		return margin(0);
	}

	@Override
	public LayoutsFillLayout spacing(int spacing) {
		wrapped.spacing = spacing;
		return this;
	}

	/** Sets the spacing to zero. */
	public LayoutsFillLayout noSpacing() {
		return spacing(0);
	}

	public LayoutsFillLayout vertical() {
		wrapped.type = SWT.VERTICAL;
		return this;
	}

	public LayoutsFillLayout horizontal() {
		wrapped.type = SWT.HORIZONTAL;
		return this;
	}

	public LayoutsFillLayout marginWidth(int marginWidth) {
		wrapped.marginWidth = marginWidth;
		return this;
	}

	public LayoutsFillLayout marginHeight(int marginHeight) {
		wrapped.marginHeight = marginHeight;
		return this;
	}
}
