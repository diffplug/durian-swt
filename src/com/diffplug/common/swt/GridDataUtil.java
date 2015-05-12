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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.google.common.base.Preconditions;

/**
 * Code from: https://gist.github.com/mpost/6077907
 * Post from: http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/
 * Many thanks to the author, Moritz Post
 * 
 * Modified for our purposes.
 */
public class GridDataUtil {
	private final GridData gridData;

	private GridDataUtil(GridData gridData) {
		this.gridData = gridData;
	}

	/** Creates a new Label and sets its GridData as a placeholder. */
	public static GridDataUtil newPlaceholder(Composite parent) {
		Label placeholder = new Label(parent, SWT.NONE);
		return set(placeholder);
	}

	public static GridDataUtil set(Control control) {
		GridData gridData = new GridData();
		control.setLayoutData(gridData);
		return new GridDataUtil(gridData);
	}

	public static GridDataUtil modify(Control control) {
		Object layoutData = control.getLayoutData();
		Preconditions.checkArgument(layoutData instanceof GridData, "Control must have GridData, but has %s.",
				layoutData);
		return new GridDataUtil((GridData) layoutData);
	}

	public static GridDataUtil modify(GridData gd) {
		return new GridDataUtil(gd);
	}

	public static GridDataUtil set(ControlWrapper<?> wrapper) {
		return set(wrapper.getControl());
	}

	public static GridDataUtil modify(ControlWrapper<?> wrapper) {
		return set(wrapper.getControl());
	}

	public GridDataUtil fillHorizontal() {
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = false;
		return this;
	}

	public GridDataUtil fillVertical() {
		gridData.horizontalAlignment = SWT.TOP;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.grabExcessVerticalSpace = true;
		return this;
	}

	public GridDataUtil fillAll() {
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		return this;
	}

	public GridDataUtil grabExcessHorizontalSpace(boolean grabExcessHorizontalSpace) {
		gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
		return this;
	}

	public GridDataUtil grabExcessVerticalSpace(boolean grabExcessVerticalSpace) {
		gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
		return this;
	}

	public GridDataUtil horizontalSpan(int horizontalSpan) {
		gridData.horizontalSpan = horizontalSpan;
		return this;
	}

	public GridDataUtil verticalSpan(int verticalSpan) {
		gridData.verticalSpan = verticalSpan;
		return this;
	}

	public GridDataUtil minimumHeight(int minimumHeight) {
		gridData.minimumHeight = minimumHeight;
		return this;
	}

	public GridDataUtil minimumWidth(int minimumWidth) {
		gridData.minimumWidth = minimumWidth;
		return this;
	}

	public GridDataUtil minimumSize(Point size) {
		gridData.minimumWidth = size.x;
		gridData.minimumHeight = size.y;
		return this;
	}

	public GridDataUtil verticalIndent(int verticalIndent) {
		gridData.verticalIndent = verticalIndent;
		return this;
	}

	public GridDataUtil horizontalIndent(int horizontalIndent) {
		gridData.horizontalIndent = horizontalIndent;
		return this;
	}

	public GridDataUtil heightHint(int heightHint) {
		gridData.heightHint = heightHint;
		return this;
	}

	public GridDataUtil widthHint(int widthHint) {
		gridData.widthHint = widthHint;
		return this;
	}

	public GridDataUtil sizeHint(Point sizeHint) {
		gridData.widthHint = sizeHint.x;
		gridData.heightHint = sizeHint.y;
		return this;
	}

	public GridDataUtil verticalAlignment(int verticalAlignment) {
		gridData.verticalAlignment = verticalAlignment;
		return this;
	}

	public GridDataUtil horizontalAlignment(int horizontalAlignment) {
		gridData.horizontalAlignment = horizontalAlignment;
		return this;
	}

	public GridDataUtil exclude(boolean exclude) {
		gridData.exclude = exclude;
		return this;
	}

	public int getHeightHint() {
		return gridData.heightHint;
	}

	public GridDataUtil grabAll() {
		grabHorizontal();
		grabVertical();
		return this;
	}

	public GridDataUtil grabHorizontal() {
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		return this;
	}

	public GridDataUtil grabVertical() {
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		return this;
	}
}
