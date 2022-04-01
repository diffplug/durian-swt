/*
 * Copyright (C) 2020-2022 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.common.swt;


import com.diffplug.common.base.Preconditions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

/**
 * Provides fluent utilities for manipulating SWT layouts. Serves as the entry point to {@link LayoutsFillLayout},
 * {@link LayoutsGridLayout}, {@link LayoutsGridData}, {@link LayoutsRowLayout}, and {@link LayoutsRowData}.
 * <p> 
 * SWT's layouts don't have consistent defaults for margin and spacing.  All layouts created
 * by this class (via {@link #setFill(Composite)}, {@link #setGrid(Composite)}, and
 * {@link #setRow(Composite)} have all of their margins and spacing values set to 
 * {@link #defaultMargin()}.
 * <p>
 * `FillLayout` only supports `marginHeight` and `marginWidth`, but `GridLayout` and
 * `RowLayout` also support `marginTop/Bottom/Left/Right`.  These are additive - the margin on the
 * top side will be `marginHeight + marginTop`.  When setting all margins at once using
 * {@link LayoutWrapper#margin(int)}, height/width are set to the given value, and top/bottom/left/right are all
 * set to 0.
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
		LayoutsFillLayout wrapper = new LayoutsFillLayout(fillLayout).margin(defaultMargin()).spacing(defaultMargin());
		wrapper.setMarginAndSpacingToDefault();
		return wrapper;
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
		LayoutsGridLayout wrapper = new LayoutsGridLayout(gridLayout);
		wrapper.setMarginAndSpacingToDefault();
		return wrapper;
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

	/** Creates an invisible `org.eclipse.swt.widgets.Label`, and returns an API for setting its GridData. Useful for filling spots in a GridLayout. */
	public static LayoutsGridData newGridPlaceholder(Composite parent) {
		Label placeholder = new Label(parent, SWT.NONE);
		return setGridData(placeholder);
	}

	/**
	 * Quick shortcut to add a row to to this parent.
	 * 
	 * - parent must have a GridLayout
	 * - creates a new Composite that will take the full width of the parent, with no margins
	 * - populates the new Composite using the Coat
	 * - sets the layout on the new Composite to be a GridLayout with no margins and as many columns as there are child controls
	 * - returns the new Composite
	 */
	public static Composite newGridRow(Composite parent, Coat coat) {
		Composite cmp = new Composite(parent, SWT.NONE);
		Layouts.setGridData(cmp)
				.grabHorizontal()
				.horizontalSpan(Layouts.modifyGrid(parent).getRaw().numColumns);
		LayoutsGridLayout gridData = Layouts.setGrid(cmp).margin(0);
		coat.putOn(cmp);
		gridData.numColumns(cmp.getChildren().length);
		return cmp;
	}

	///////////////
	// RowLayout //
	///////////////
	/** Sets the composite to have a standard RowLayout, and returns an API for modifying it. */
	public static LayoutsRowLayout setRow(Composite composite) {
		RowLayout rowLayout = new RowLayout();
		composite.setLayout(rowLayout);
		LayoutsRowLayout wrapper = new LayoutsRowLayout(rowLayout);
		wrapper.setMarginAndSpacingToDefault();
		return wrapper;
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

	/** Creates an invisible `org.eclipse.swt.widgets.Label`, and returns an API for setting its RowData. Useful for filling spots in a RowLayout. */
	public static LayoutsRowData newRowPlaceholder(Composite parent) {
		Label placeholder = new Label(parent, SWT.NONE);
		return setRowData(placeholder);
	}

	////////////
	// Single //
	////////////
	public static void setSingle(Composite composite) {
		Single.child(composite);
		composite.setLayout(standardMargin);
	}

	public static void setSingleNoMargin(Composite composite) {
		Single.child(composite);
		composite.setLayout(noMargin);
	}

	private static final Single noMargin = new Single();
	private static final Single standardMargin = new Single();

	private static class Single extends Layout {
		private static Control child(Composite composite) {
			Control[] children = composite.getChildren();
			if (children.length == 1) {
				return children[0];
			} else {
				throw new IllegalArgumentException("Must have 1 child, had " + children.length);
			}
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Point child = child(composite).computeSize(wHint, hHint, flushCache);
			if (this == noMargin) {
				return child;
			} else {
				child.x += Layouts.defaultMargin() * 2;
				child.y += Layouts.defaultMargin() * 2;
				return child;
			}
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Control child = child(composite);
			Rectangle area = composite.getClientArea();
			if (this == standardMargin) {
				area.x += Layouts.defaultMargin();
				area.y += Layouts.defaultMargin();
				area.width -= 2 * Layouts.defaultMargin();
				area.height -= 2 * Layouts.defaultMargin();
				area.width = Math.max(area.width, 1);
				area.height = Math.max(area.height, 1);
			}
			child.setBounds(area);
		}
	}
}
