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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

import com.google.common.base.Preconditions;

/**
 * Provides fluent utilities for manipulating SWT layouts.
 * <p> 
 * Serves as the entry point to {@link LayoutsFillLayout}, {@link LayoutsGridLayout}, {@link LayoutsGridData},
 * {@link LayoutsRowLayout}, and {@link LayoutsRowData}. 
 */
public class Layouts {
	private Layouts() {}

	/** Returns the default margin for layouts. */
	public static int defaultMargin() {
		return 5;
	}

	/** Checks that the layout of the composite has the proper class, then returns the cast layout. */
	@SuppressWarnings("unchecked")
	private static <T extends Layout> T getLayout(Composite composite, Class<T> clazz) {
		Layout layout = composite.getLayout();
		if (layout == null || !clazz.isAssignableFrom(layout.getClass())) {
			throw new IllegalArgumentException("Expected parent to have layout " + clazz.getName() + ", but was " + layout + ".");
		}
		return (T) layout;
	}

	////////////////
	// FillLayout //
	////////////////
	/** Sets the composite to have a standard FillLayout, and returns an API for modifying it. */
	public static LayoutsFillLayout setFill(Composite composite) {
		FillLayout fillLayout = new FillLayout();
		composite.setLayout(fillLayout);
		return new LayoutsFillLayout(fillLayout).margin(defaultMargin()).spacing(defaultMargin());
	}

	/** Returns an API for modifying the already-existing FillLayout on the given Composite. */
	public static LayoutsFillLayout modifyFill(Composite composite) {
		return new LayoutsFillLayout(getLayout(composite, FillLayout.class));
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
		composite.setLayout(gridLayout);
		return new LayoutsGridLayout(gridLayout).margin(defaultMargin()).spacing(defaultMargin());
	}

	/** Returns an API for modifying the already-existing GridLayout on the given Composite. */
	public static LayoutsGridLayout modifyGrid(Composite composite) {
		return new LayoutsGridLayout(getLayout(composite, GridLayout.class));
	}

	/** Returns an API for modifying the given GridLayout. */
	public static LayoutsGridLayout wrap(GridLayout gridLayout) {
		return new LayoutsGridLayout(gridLayout);
	}

	//////////////
	// GridData //
	//////////////
	/** Sets the layouData on the Control to a new GridData, and returns an API for modifying it. */
	public static LayoutsGridData setGridData(Control control) {
		getLayout(control.getParent(), GridLayout.class);
		GridData gridData = new GridData();
		control.setLayoutData(gridData);
		return new LayoutsGridData(gridData);
	}

	/** Sets the layoutData on the ControlWrapper to a new GridData, and returns an API for modifying it. */
	public static LayoutsGridData setGridData(ControlWrapper wrapper) {
		return setGridData(wrapper.getRootControl());
	}

	/** Returns an API for modifying the already-existing GridData which has been set on the given Control. */
	public static LayoutsGridData modifyGridData(Control control) {
		return modifyGridData(control.getLayoutData());
	}

	/** Returns an API for modifying the already-existing GridData which has been set on the given ControlWrapper. */
	public static LayoutsGridData modifyGridData(ControlWrapper wrapper) {
		return modifyGridData(wrapper.getLayoutData());
	}

	private static LayoutsGridData modifyGridData(Object layoutData) {
		Preconditions.checkArgument(layoutData instanceof GridData, "Control must have GridData, but has %s.", layoutData);
		return new LayoutsGridData((GridData) layoutData);
	}

	/** Returns an API for modifying the given GridData. */
	public static LayoutsGridData wrap(GridData gridData) {
		return new LayoutsGridData(gridData);
	}

	/** Creates an invisible {@code org.eclipse.swt.widgets.Label}, and returns an API for setting its GridData. Useful for filling spots in a GridLayout. */
	public static LayoutsGridData newGridPlaceholder(Composite parent) {
		Label placeholder = new Label(parent, SWT.NONE);
		return setGridData(placeholder);
	}

	///////////////
	// RowLayout //
	///////////////
	/** Sets the composite to have a standard RowLayout, and returns an API for modifying it. */
	public static LayoutsRowLayout setRow(Composite composite) {
		RowLayout rowLayout = new RowLayout();
		composite.setLayout(rowLayout);
		return new LayoutsRowLayout(rowLayout).margin(defaultMargin()).spacing(defaultMargin());
	}

	/** Returns an API for modifying the already-existing RowLayout on the given Composite. */
	public static LayoutsRowLayout modifyRow(Composite composite) {
		return new LayoutsRowLayout(getLayout(composite, RowLayout.class));
	}

	/** Returns an API for modifying the given RowLayout. */
	public static LayoutsRowLayout wrap(RowLayout rowLayout) {
		return new LayoutsRowLayout(rowLayout);
	}

	/////////////
	// RowData //
	/////////////
	/** Sets the layouData on the Control to a new GridData, and returns an API for modifying it. */
	public static LayoutsRowData setRowData(Control control) {
		getLayout(control.getParent(), RowLayout.class);
		RowData rowData = new RowData();
		control.setLayoutData(rowData);
		return new LayoutsRowData(rowData);
	}

	/** Sets the layoutData on the ControlWrapper to a new RowData, and returns an API for modifying it. */
	public static LayoutsRowData setRowData(ControlWrapper wrapper) {
		return setRowData(wrapper.getRootControl());
	}

	/** Returns an API for modifying the already-existing RowData which has been set on the given Control. */
	public static LayoutsRowData modifyRowData(Control control) {
		return modifyRowData(control.getLayoutData());
	}

	/** Returns an API for modifying the already-existing RowData which has been set on the given ControlWrapper. */
	public static LayoutsRowData modifyRowData(ControlWrapper wrapper) {
		return modifyRowData(wrapper.getLayoutData());
	}

	private static LayoutsRowData modifyRowData(Object layoutData) {
		Preconditions.checkArgument(layoutData instanceof RowData, "Control must have RowData, but has %s.", layoutData);
		return new LayoutsRowData((RowData) layoutData);
	}

	/** Returns an API for modifying the given RowData. */
	public static LayoutsRowData wrap(RowData gridData) {
		return new LayoutsRowData(gridData);
	}

	/** Creates an invisible {@code org.eclipse.swt.widgets.Label}, and returns an API for setting its RowData. Useful for filling spots in a RowLayout. */
	public static LayoutsRowData newRowPlaceholder(Composite parent) {
		Label placeholder = new Label(parent, SWT.NONE);
		return setRowData(placeholder);
	}
}
