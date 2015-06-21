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
import org.eclipse.swt.layout.RowLayout;

/**
 * A fluent api for setting and modifying a {@link RowLayout}, created by {@link Layouts}.
 * 
 * Inspired by <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">Moritz Post's blog post.</a>.
 */
public class LayoutsRowLayout implements LayoutWrapper {
	private final RowLayout rowLayout;

	LayoutsRowLayout(RowLayout rowLayout) {
		this.rowLayout = rowLayout;
	}

	/** Returns the raw GridLayout. */
	public RowLayout getRaw() {
		return rowLayout;
	}

	/** Sets all margins to the given value. */
	@Override
	public LayoutsRowLayout margin(int margin) {
		rowLayout.marginWidth = margin;
		rowLayout.marginHeight = margin;
		rowLayout.marginLeft = margin;
		rowLayout.marginRight = margin;
		rowLayout.marginTop = margin;
		rowLayout.marginBottom = margin;
		return this;
	}

	/** Sets the margins to zero. */
	public LayoutsRowLayout noMargin() {
		return margin(0);
	}

	/** Sets the spacing to zero. */
	public LayoutsRowLayout spacing(int spacing) {
		rowLayout.spacing = spacing;
		return this;
	}

	/** Sets the spacing to zero. */
	public LayoutsRowLayout noSpacing() {
		return spacing(0);
	}

	/** Makes this a vertical layout. */
	public LayoutsRowLayout vertical() {
		rowLayout.type = SWT.VERTICAL;
		return this;
	}

	/** Makes this a horizontal layout. */
	public LayoutsRowLayout horizontal() {
		rowLayout.type = SWT.HORIZONTAL;
		return this;
	}

	public LayoutsRowLayout marginWidth(int marginWidth) {
		rowLayout.marginWidth = marginWidth;
		return this;
	}

	public LayoutsRowLayout marginHeight(int marginHeight) {
		rowLayout.marginHeight = marginHeight;
		return this;
	}

	public LayoutsRowLayout marginLeft(int marginLeft) {
		rowLayout.marginLeft = marginLeft;
		return this;
	}

	public LayoutsRowLayout marginTop(int marginTop) {
		rowLayout.marginTop = marginTop;
		return this;
	}

	public LayoutsRowLayout marginRight(int marginRight) {
		rowLayout.marginRight = marginRight;
		return this;
	}

	public LayoutsRowLayout marginBottom(int marginBottom) {
		rowLayout.marginBottom = marginBottom;
		return this;
	}

	/**
	 * wrap specifies whether a control will be wrapped to the next
	 * row if there is insufficient space on the current row.
	 * <p>
	 * The default value is true.
	 */
	public LayoutsRowLayout wrap(boolean wrap) {
		rowLayout.wrap = wrap;
		return this;
	}

	/**
	 * pack specifies whether all controls in the layout take
	 * their preferred size.  If pack is false, all controls will 
	 * have the same size which is the size required to accommodate the 
	 * largest preferred height and the largest preferred width of all 
	 * the controls in the layout.
	 * <p>
	 * The default value is true.
	 */
	public LayoutsRowLayout pack(boolean pack) {
		rowLayout.pack = pack;
		return this;
	}

	/**
	 * fill specifies whether the controls in a row should be
	 * all the same height for horizontal layouts, or the same
	 * width for vertical layouts.
	 * <p>
	 * The default value is false.
	 */
	public LayoutsRowLayout fill(boolean fill) {
		rowLayout.fill = fill;
		return this;
	}

	/**
	 * center specifies whether the controls in a row should be
	 * centered vertically in each cell for horizontal layouts,
	 * or centered horizontally in each cell for vertical layouts.
	 * <p>
	 * The default value is false.
	 */
	public LayoutsRowLayout center(boolean center) {
		rowLayout.center = center;
		return this;
	}

	/**
	 * justify specifies whether the controls in a row should be
	 * fully justified, with any extra space placed between the controls.
	 * <p>
	 * The default value is false.
	 */
	public LayoutsRowLayout justify(boolean justify) {
		rowLayout.justify = justify;
		return this;
	}
}
