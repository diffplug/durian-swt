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

	/** Sets all margins to the given value. */
	@Override
	public LayoutsGridLayout margin(int margin) {
		wrapped.marginWidth = margin;
		wrapped.marginHeight = margin;
		wrapped.marginLeft = margin;
		wrapped.marginRight = margin;
		wrapped.marginTop = margin;
		wrapped.marginBottom = margin;
		return this;
	}

	/** Sets the margins to zero. */
	public LayoutsGridLayout noMargin() {
		return margin(0);
	}

	/** Sets all margins to the given value. */
	@Override
	public LayoutsGridLayout spacing(int spacing) {
		wrapped.verticalSpacing = spacing;
		wrapped.horizontalSpacing = spacing;
		return this;
	}

	/** Sets the spacing to zero. */
	public LayoutsGridLayout noSpacing() {
		return spacing(0);
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
