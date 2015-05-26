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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

import com.google.common.base.Preconditions;

/**
 * Provides fluent utilities for manipulating SWT layouts.
 * 
 * Serves as the entry point to LayoutsFillLayout, LayoutsGridLayout, and LayoutsGridData.
 */
public class Layouts {
	private Layouts() {}

	public static final int DEFAULT_MARGIN = 5;

	////////////////
	// FillLayout //
	////////////////
	/** Sets the composite to have a standard FillLayout, and returns an API for modifying it. */
	public static LayoutsFillLayout setFill(Composite composite) {
		FillLayout fillLayout = new FillLayout();
		fillLayout.spacing = SWT.HORIZONTAL;
		fillLayout.marginHeight = DEFAULT_MARGIN;
		fillLayout.marginWidth = DEFAULT_MARGIN;
		fillLayout.spacing = DEFAULT_MARGIN;
		composite.setLayout(fillLayout);
		return new LayoutsFillLayout(fillLayout);
	}

	/** Returns an API for modifying the already-existing FillLayout on the given Composite. */
	public static LayoutsFillLayout modifyFill(Composite composite) {
		Layout layout = composite.getLayout();
		Preconditions.checkArgument(layout instanceof FillLayout, "Composite must have FillLayout, but has %s.", layout);
		return new LayoutsFillLayout((FillLayout) layout);
	}

	/** Returns an API for modifying the given FillLayout. */
	public static LayoutsFillLayout wrap(FillLayout fillLayout) {
		return new LayoutsFillLayout(fillLayout);
	}

	////////////////
	// GridLayout //
	////////////////
	/** Sets the composite to have a standard GridLayout, and returns an API for modifying it. */
	public static LayoutsGridLayout setGrid(Composite composite) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = DEFAULT_MARGIN;
		gridLayout.marginWidth = DEFAULT_MARGIN;
		gridLayout.horizontalSpacing = DEFAULT_MARGIN;
		gridLayout.verticalSpacing = DEFAULT_MARGIN;
		composite.setLayout(gridLayout);
		return new LayoutsGridLayout(gridLayout);
	}

	/** Returns an API for modifying the already-existing GridLayout on the given Composite. */
	public static LayoutsGridLayout modifyGrid(Composite composite) {
		Layout layout = composite.getLayout();
		Preconditions.checkArgument(layout instanceof GridLayout, "Composite must have GridLayout, but has %s.", layout);
		return new LayoutsGridLayout((GridLayout) layout);
	}

	/** Returns an API for modifying the given GridLayout. */
	public static LayoutsGridLayout wrap(GridLayout gridLayout) {
		return new LayoutsGridLayout(gridLayout);
	}

	//////////////
	// GridData //
	//////////////
	/** Sets the layout data on the Control be a new GridData, and returns an API for modifying it. */
	public static LayoutsGridData setGridData(Control control) {
		GridData gridData = new GridData();
		control.setLayoutData(gridData);
		return new LayoutsGridData(gridData);
	}

	/** Sets the layout data on the ControlWrapper be a new GridData, and returns an API for modifying it. */
	public static LayoutsGridData setGridData(ControlWrapper<?> wrapper) {
		return setGridData(wrapper.getControl());
	}

	/** Returns an API for modifying the already-existing GridData which has been set on the given Control. */
	public static LayoutsGridData modifyGridData(Control control) {
		Object layoutData = control.getLayoutData();
		Preconditions.checkArgument(layoutData instanceof GridData, "Control must have GridData, but has %s.", layoutData);
		return new LayoutsGridData((GridData) layoutData);
	}

	/** Returns an API for modifying the already-existing GridData which has been set on the given ControlWrapper. */
	public static LayoutsGridData modifyGridData(ControlWrapper<?> wrapper) {
		return setGridData(wrapper.getControl());
	}

	/** Returns an API for modifying the given GridData. */
	public static LayoutsGridData wrap(GridData gridData) {
		return new LayoutsGridData(gridData);
	}

	/** Creates an invisible Label, and returns an API for setting its GridData. Useful for filling spots in a GridLayout. */
	public static LayoutsGridData newGridPlaceholder(Composite parent) {
		Label placeholder = new Label(parent, SWT.NONE);
		return setGridData(placeholder);
	}
}
