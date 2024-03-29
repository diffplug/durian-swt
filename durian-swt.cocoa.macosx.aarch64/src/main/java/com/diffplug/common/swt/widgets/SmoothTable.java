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
package com.diffplug.common.swt.widgets;


import org.eclipse.swt.internal.cocoa.NSPoint;
import org.eclipse.swt.widgets.Composite;

public final class SmoothTable extends AbstractSmoothTable.Scrollable {
	public SmoothTable(Composite parent, int tableStyle) {
		super(parent, tableStyle);
	}

	@Override
	protected void setTopPixelWithinTable(int topPixel) {
		NSPoint pt = new NSPoint();
		pt.x = 0;
		pt.y = topPixel;
		table.view.scrollPoint(pt);
	}
}
