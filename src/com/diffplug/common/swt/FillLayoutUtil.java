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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

import com.google.common.base.Preconditions;

/**
 * A fluent api for setting and modifying a FillLayout.
 * 
 * Inspired by Moritz Post: http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/
 */
public class FillLayoutUtil {
	private final FillLayout fillLayout;

	FillLayoutUtil(FillLayout fillLayout) {
		this.fillLayout = fillLayout;
	}

	/** Sets the composite to have a standard FillLayout, and returns an API for modifying it. */
	public static FillLayoutUtil set(Composite composite) {
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = Layouts.DEFAULT_MARGIN;
		fillLayout.marginWidth = Layouts.DEFAULT_MARGIN;
		composite.setLayout(fillLayout);
		return new FillLayoutUtil(fillLayout);
	}

	/** Returns an API for modifying the already-existing FillLayout on the given Composite. */
	public static FillLayoutUtil modify(Composite composite) {
		Layout layout = composite.getLayout();
		Preconditions.checkArgument(layout instanceof FillLayout, "Composite must have FillLayout, but has %s.", layout);
		return new FillLayoutUtil((FillLayout) layout);
	}

	/** Returns an API for modifying the given FillLayout. */
	public static FillLayoutUtil wrap(FillLayout fillLayout) {
		return new FillLayoutUtil(fillLayout);
	}

	/** Returns the raw FillLayout. */
	public FillLayout getRaw() {
		return fillLayout;
	}

	/** Sets the margins to zero. */
	public FillLayoutUtil noBorder() {
		fillLayout.marginWidth = 0;
		fillLayout.marginHeight = 0;
		return this;
	}

	/** Sets the spacing to zero. */
	public FillLayoutUtil tight() {
		fillLayout.spacing = 0;
		return this;
	}

	public FillLayoutUtil vertical() {
		fillLayout.type = SWT.VERTICAL;
		return this;
	}

	public FillLayoutUtil horizontal() {
		fillLayout.type = SWT.HORIZONTAL;
		return this;
	}

	public FillLayoutUtil marginWidth(int marginWidth) {
		fillLayout.marginWidth = marginWidth;
		return this;
	}

	public FillLayoutUtil marginHeight(int marginHeight) {
		fillLayout.marginHeight = marginHeight;
		return this;
	}

	public FillLayoutUtil spacing(int spacing) {
		fillLayout.spacing = spacing;
		return this;
	}
}
