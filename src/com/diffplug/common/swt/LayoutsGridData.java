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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;

/**
 * A fluent api for setting and modifying a {@link GridData}, created by {@link Layouts}.
 * 
 * Inspired by <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">Moritz Post's blog post.</a>.
 */
public class LayoutsGridData {
	private final GridData gridData;

	LayoutsGridData(GridData gridData) {
		this.gridData = gridData;
	}

	/** Returns the raw GridData. */
	public GridData getRaw() {
		return gridData;
	}

	/** The GridData will grab space in all directions. */
	public LayoutsGridData grabAll() {
		grabHorizontal();
		grabVertical();
		return this;
	}

	/** The GridData will grab space horizontally. */
	public LayoutsGridData grabHorizontal() {
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		return this;
	}

	/** The GridData will grab space vertically. */
	public LayoutsGridData grabVertical() {
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		return this;
	}

	////////////////////////////
	// Setters for all fields //
	////////////////////////////
	public LayoutsGridData grabExcessHorizontalSpace(boolean grabExcessHorizontalSpace) {
		gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
		return this;
	}

	public LayoutsGridData grabExcessVerticalSpace(boolean grabExcessVerticalSpace) {
		gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
		return this;
	}

	public LayoutsGridData horizontalSpan(int horizontalSpan) {
		gridData.horizontalSpan = horizontalSpan;
		return this;
	}

	public LayoutsGridData verticalSpan(int verticalSpan) {
		gridData.verticalSpan = verticalSpan;
		return this;
	}

	public LayoutsGridData minimumHeight(int minimumHeight) {
		gridData.minimumHeight = minimumHeight;
		return this;
	}

	public LayoutsGridData minimumWidth(int minimumWidth) {
		gridData.minimumWidth = minimumWidth;
		return this;
	}

	public LayoutsGridData minimumSize(Point size) {
		gridData.minimumWidth = size.x;
		gridData.minimumHeight = size.y;
		return this;
	}

	public LayoutsGridData verticalIndent(int verticalIndent) {
		gridData.verticalIndent = verticalIndent;
		return this;
	}

	public LayoutsGridData horizontalIndent(int horizontalIndent) {
		gridData.horizontalIndent = horizontalIndent;
		return this;
	}

	public LayoutsGridData heightHint(int heightHint) {
		gridData.heightHint = heightHint;
		return this;
	}

	public LayoutsGridData widthHint(int widthHint) {
		gridData.widthHint = widthHint;
		return this;
	}

	public LayoutsGridData sizeHint(Point sizeHint) {
		gridData.widthHint = sizeHint.x;
		gridData.heightHint = sizeHint.y;
		return this;
	}

	public LayoutsGridData verticalAlignment(int verticalAlignment) {
		gridData.verticalAlignment = verticalAlignment;
		return this;
	}

	public LayoutsGridData horizontalAlignment(int horizontalAlignment) {
		gridData.horizontalAlignment = horizontalAlignment;
		return this;
	}

	public LayoutsGridData exclude(boolean exclude) {
		gridData.exclude = exclude;
		return this;
	}
}
