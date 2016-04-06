/*
 * Copyright 2016 DiffPlug
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

import org.eclipse.swt.layout.GridLayout;

/**
 * A fluent api for setting and modifying a {@link GridLayout}, created by {@link Layouts}.
 * 
 * Inspired by <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">Moritz Post's blog post.</a>.
 */
public class LayoutsGridLayout extends LayoutWrapper<GridLayout> {
	LayoutsGridLayout(GridLayout gridLayout) {
		super(gridLayout);
	}

	/** Sets marginWidth and marginHeight to the given value, and left/right/top/bottom to 0. */
	@Override
	public LayoutsGridLayout margin(int margin) {
		wrapped.marginWidth = margin;
		wrapped.marginHeight = margin;
		wrapped.marginLeft = 0;
		wrapped.marginRight = 0;
		wrapped.marginTop = 0;
		wrapped.marginBottom = 0;
		return this;
	}

	/** Sets marginHeight to 0, and top / bottom to the given values. */
	public LayoutsGridLayout marginTopBottom(int top, int bottom) {
		wrapped.marginHeight = 0;
		wrapped.marginTop = top;
		wrapped.marginBottom = bottom;
		return this;
	}

	/** Sets marginWidth to 0, and left / right to the given values. */
	public LayoutsGridLayout marginLeftRight(int left, int right) {
		wrapped.marginWidth = 0;
		wrapped.marginLeft = left;
		wrapped.marginRight = right;
		return this;
	}

	/** Sets all margins to the given value. */
	@Override
	public LayoutsGridLayout spacing(int spacing) {
		wrapped.verticalSpacing = spacing;
		wrapped.horizontalSpacing = spacing;
		return this;
	}

	public LayoutsGridLayout numColumns(int numColumns) {
		wrapped.numColumns = numColumns;
		return this;
	}

	public LayoutsGridLayout columnsEqualWidth(boolean columnsEqualWidth) {
		wrapped.makeColumnsEqualWidth = columnsEqualWidth;
		return this;
	}

	public LayoutsGridLayout marginWidth(int marginWidth) {
		wrapped.marginWidth = marginWidth;
		return this;
	}

	public LayoutsGridLayout marginHeight(int marginHeight) {
		wrapped.marginHeight = marginHeight;
		return this;
	}

	public LayoutsGridLayout marginLeft(int marginLeft) {
		wrapped.marginLeft = marginLeft;
		return this;
	}

	public LayoutsGridLayout marginTop(int marginTop) {
		wrapped.marginTop = marginTop;
		return this;
	}

	public LayoutsGridLayout marginRight(int marginRight) {
		wrapped.marginRight = marginRight;
		return this;
	}

	public LayoutsGridLayout marginBottom(int marginBottom) {
		wrapped.marginBottom = marginBottom;
		return this;
	}

	public LayoutsGridLayout horizontalSpacing(int horizontalSpacing) {
		wrapped.horizontalSpacing = horizontalSpacing;
		return this;
	}

	public LayoutsGridLayout verticalSpacing(int verticalSpacing) {
		wrapped.verticalSpacing = verticalSpacing;
		return this;
	}
}
