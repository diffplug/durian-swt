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

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Provides fluent utilities for manipulating SWT layouts.
 * 
 * Serves as the entry point to LayoutsFillLayout, LayoutsGridLayout, and LayoutsGridData.
 */
public class Layouts {
	public static final int DEFAULT_MARGIN = 5;

	/////////////
	// Layouts //
	/////////////
	public static FillLayoutUtil setFill(Composite cmp) {
		return FillLayoutUtil.set(cmp);
	}

	public static FillLayoutUtil modifyFill(Composite cmp) {
		return FillLayoutUtil.modify(cmp);
	}

	public static GridLayoutUtil setGrid(Composite cmp) {
		return GridLayoutUtil.set(cmp);
	}

	public static GridLayoutUtil modifyGrid(Composite cmp) {
		return GridLayoutUtil.modify(cmp);
	}

	///////////////
	// Grid data //
	///////////////
	public static GridDataUtil setGridData(Control control) {
		return GridDataUtil.set(control);
	}

	public static GridDataUtil setGridData(ControlWrapper<?> control) {
		return GridDataUtil.set(control);
	}

	public static GridDataUtil modifyGridData(Control control) {
		return GridDataUtil.modify(control);
	}

	public static GridDataUtil modifyGridData(ControlWrapper<?> control) {
		return GridDataUtil.modify(control);
	}

	public static GridDataUtil newGridPlaceholder(Composite parent) {
		return GridDataUtil.newPlaceholder(parent);
	}

	/////////////////////////////////
	// Modifiers for raw instances //
	/////////////////////////////////
	public static FillLayoutUtil wrap(FillLayout fillLayout) {
		return new FillLayoutUtil(fillLayout);
	}

	public static GridLayoutUtil wrap(GridLayout gridLayout) {
		return new GridLayoutUtil(gridLayout);
	}

	public static GridDataUtil wrap(GridData gridData) {
		return new GridDataUtil(gridData);
	}
}
