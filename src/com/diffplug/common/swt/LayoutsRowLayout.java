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
public class LayoutsRowLayout extends LayoutWrapper<RowLayout> {
	LayoutsRowLayout(RowLayout rowLayout) {
		super(rowLayout);
	}

	/** Sets marginWidth and marginHeight to the given value, and left/right/top/bottom to 0. */
	@Override
	public LayoutsRowLayout margin(int margin) {
		wrapped.marginWidth = margin;
		wrapped.marginHeight = margin;
		wrapped.marginLeft = 0;
		wrapped.marginRight = 0;
		wrapped.marginTop = 0;
		wrapped.marginBottom = 0;
		return this;
	}

	/** Sets marginHeight to 0, and top / bottom to the given values. */
	public LayoutsRowLayout marginTopBottom(int top, int bottom) {
		wrapped.marginHeight = 0;
		wrapped.marginTop = top;
		wrapped.marginBottom = bottom;
		return this;
	}

	/** Sets marginWidth to 0, and left / right to the given values. */
	public LayoutsRowLayout marginLeftRight(int left, int right) {
		wrapped.marginWidth = 0;
		wrapped.marginLeft = left;
		wrapped.marginRight = right;
		return this;
	}

	/** Sets the margins to zero. */
	public LayoutsRowLayout noMargin() {
		return margin(0);
	}

	/** Sets the spacing to zero. */
	public LayoutsRowLayout spacing(int spacing) {
		wrapped.spacing = spacing;
		return this;
	}

	/** Sets the spacing to zero. */
	public LayoutsRowLayout noSpacing() {
		return spacing(0);
	}

	/** Makes this a vertical layout. */
	public LayoutsRowLayout vertical() {
		wrapped.type = SWT.VERTICAL;
		return this;
	}

	/** Makes this a horizontal layout. */
	public LayoutsRowLayout horizontal() {
		wrapped.type = SWT.HORIZONTAL;
		return this;
	}

	public LayoutsRowLayout marginWidth(int marginWidth) {
		wrapped.marginWidth = marginWidth;
		return this;
	}

	public LayoutsRowLayout marginHeight(int marginHeight) {
		wrapped.marginHeight = marginHeight;
		return this;
	}

	public LayoutsRowLayout marginLeft(int marginLeft) {
		wrapped.marginLeft = marginLeft;
		return this;
	}

	public LayoutsRowLayout marginTop(int marginTop) {
		wrapped.marginTop = marginTop;
		return this;
	}

	public LayoutsRowLayout marginRight(int marginRight) {
		wrapped.marginRight = marginRight;
		return this;
	}

	public LayoutsRowLayout marginBottom(int marginBottom) {
		wrapped.marginBottom = marginBottom;
		return this;
	}

	/**
	 * wrap specifies whether a control will be wrapped to the next
	 * row if there is insufficient space on the current row.
	 * <p>
	 * The default value is true.
	 */
	public LayoutsRowLayout wrap(boolean wrap) {
		wrapped.wrap = wrap;
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
		wrapped.pack = pack;
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
		wrapped.fill = fill;
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
		wrapped.center = center;
		return this;
	}

	/**
	 * justify specifies whether the controls in a row should be
	 * fully justified, with any extra space placed between the controls.
	 * <p>
	 * The default value is false.
	 */
	public LayoutsRowLayout justify(boolean justify) {
		wrapped.justify = justify;
		return this;
	}
}
