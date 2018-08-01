/*
 * Copyright 2018 DiffPlug
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

import org.eclipse.swt.layout.RowData;

/**
 * A fluent api for setting and modifying a {@link RowData}, created by {@link Layouts}.
 * 
 * Inspired by <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">Moritz Post's blog post.</a>.
 */
public class LayoutsRowData {
	private final RowData rowData;

	LayoutsRowData(RowData rowData) {
		this.rowData = rowData;
	}

	/** Returns the raw GridData. */
	public RowData getRaw() {
		return rowData;
	}

	public LayoutsRowData width(int width) {
		rowData.width = width;
		return this;
	}

	public LayoutsRowData height(int height) {
		rowData.height = height;
		return this;
	}

	public LayoutsRowData exclude(boolean exclude) {
		rowData.exclude = exclude;
		return this;
	}
}
