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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

import com.google.common.base.Preconditions;

/**
 * A fluent api for setting and modifying a GridLayout.
 * 
 * Inspired by Moritz Post: http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/
 */
public class GridLayoutUtil {
	private final GridLayout gridLayout;

	GridLayoutUtil(GridLayout gridLayout) {
		this.gridLayout = gridLayout;
	}

	/** Sets the composite to have a standard GridLayout, and returns an API for modifying it. */
	public static GridLayoutUtil set(Composite composite) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = Layouts.DEFAULT_MARGIN;
		gridLayout.marginWidth = Layouts.DEFAULT_MARGIN;
		composite.setLayout(gridLayout);
		return new GridLayoutUtil(gridLayout);
	}

	/** Returns an API for modifying the already-existing GridLayout on the given Composite. */
	public static GridLayoutUtil modify(Composite composite) {
		Layout layout = composite.getLayout();
		Preconditions.checkArgument(layout instanceof GridLayout, "Composite must have GridLayout, but has %s.", layout);
		return new GridLayoutUtil((GridLayout) layout);
	}

	/** Returns an API for modifying the given GridLayout. */
	public static GridLayoutUtil wrap(GridLayout gridLayout) {
		return new GridLayoutUtil(gridLayout);
	}

	/** Returns the raw GridLayout. */
	public GridLayout getRaw() {
		return gridLayout;
	}

	/** Sets the margins to zero. */
	public GridLayoutUtil noBorder() {
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		return this;
	}

	/** Sets the spacing to zero. */
	public GridLayoutUtil tight() {
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		return this;
	}

	public GridLayoutUtil numColumns(int numColumns) {
		gridLayout.numColumns = numColumns;
		return this;
	}

	public GridLayoutUtil columnsEqualWidth(boolean columnsEqualWidth) {
		gridLayout.makeColumnsEqualWidth = columnsEqualWidth;
		return this;
	}

	public GridLayoutUtil horizontalSpacing(int horizontalSpacing) {
		gridLayout.horizontalSpacing = horizontalSpacing;
		return this;
	}

	public GridLayoutUtil verticalSpacing(int verticalSpacing) {
		gridLayout.verticalSpacing = verticalSpacing;
		return this;
	}

	public GridLayoutUtil marginWidth(int marginWidth) {
		gridLayout.marginWidth = marginWidth;
		return this;
	}

	public GridLayoutUtil marginHeight(int marginHeight) {
		gridLayout.marginHeight = marginHeight;
		return this;
	}

	public GridLayoutUtil marginTop(int marginTop) {
		gridLayout.marginTop = marginTop;
		return this;
	}

	public GridLayoutUtil marginBottom(int marginBottom) {
		gridLayout.marginBottom = marginBottom;
		return this;
	}

	public GridLayoutUtil marginLeft(int marginLeft) {
		gridLayout.marginLeft = marginLeft;
		return this;
	}

	public GridLayoutUtil marginRight(int marginRight) {
		gridLayout.marginRight = marginRight;
		return this;
	}
}
