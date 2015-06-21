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
public class LayoutsGridLayout implements LayoutWrapper {
	private final GridLayout gridLayout;

	LayoutsGridLayout(GridLayout gridLayout) {
		this.gridLayout = gridLayout;
	}

	/** Returns the raw GridLayout. */
	public GridLayout getRaw() {
		return gridLayout;
	}

	/** Sets all margins to the given value. */
	@Override
	public LayoutsGridLayout margin(int margin) {
		gridLayout.marginWidth = margin;
		gridLayout.marginHeight = margin;
		gridLayout.marginLeft = margin;
		gridLayout.marginRight = margin;
		gridLayout.marginTop = margin;
		gridLayout.marginBottom = margin;
		return this;
	}

	/** Sets the margins to zero. */
	public LayoutsGridLayout noMargin() {
		return margin(0);
	}

	/** Sets all margins to the given value. */
	@Override
	public LayoutsGridLayout spacing(int spacing) {
		gridLayout.verticalSpacing = spacing;
		gridLayout.horizontalSpacing = spacing;
		return this;
	}

	/** Sets the spacing to zero. */
	public LayoutsGridLayout noSpacing() {
		return spacing(0);
	}

	public LayoutsGridLayout numColumns(int numColumns) {
		gridLayout.numColumns = numColumns;
		return this;
	}

	public LayoutsGridLayout columnsEqualWidth(boolean columnsEqualWidth) {
		gridLayout.makeColumnsEqualWidth = columnsEqualWidth;
		return this;
	}

	public LayoutsGridLayout marginWidth(int marginWidth) {
		gridLayout.marginWidth = marginWidth;
		return this;
	}

	public LayoutsGridLayout marginHeight(int marginHeight) {
		gridLayout.marginHeight = marginHeight;
		return this;
	}

	public LayoutsGridLayout marginLeft(int marginLeft) {
		gridLayout.marginLeft = marginLeft;
		return this;
	}

	public LayoutsGridLayout marginTop(int marginTop) {
		gridLayout.marginTop = marginTop;
		return this;
	}

	public LayoutsGridLayout marginRight(int marginRight) {
		gridLayout.marginRight = marginRight;
		return this;
	}

	public LayoutsGridLayout marginBottom(int marginBottom) {
		gridLayout.marginBottom = marginBottom;
		return this;
	}

	public LayoutsGridLayout horizontalSpacing(int horizontalSpacing) {
		gridLayout.horizontalSpacing = horizontalSpacing;
		return this;
	}

	public LayoutsGridLayout verticalSpacing(int verticalSpacing) {
		gridLayout.verticalSpacing = verticalSpacing;
		return this;
	}
}
