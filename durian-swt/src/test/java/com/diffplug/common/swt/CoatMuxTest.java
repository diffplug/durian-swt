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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.swt.jface.Actions;

@Category(InteractiveTest.class)
public class CoatMuxTest {
	@Test
	public void simpleTest() {
		InteractiveTest.testCoat("Test coat mux layout behavior", this::randomColorMux);
	}

	private void randomColorMux(Composite cmp) {
		Layouts.setGrid(cmp);
		CoatMux mux = new CoatMux(cmp, SWT.BORDER);
		Layouts.setGridData(mux).grabAll();

		ToolBarManager manager = new ToolBarManager();
		manager.add(Actions.create("empty", () -> mux.setEmpty()));
		manager.add(create(mux, "red", 255, 0, 0));
		manager.add(create(mux, "green", 0, 255, 0));
		manager.add(create(mux, "blue", 0, 0, 255));
		Layouts.setGridData(manager.createControl(cmp)).grabHorizontal();
	}

	private IAction create(CoatMux mux, String name, int r, int g, int b) {
		return Actions.create(name, () -> {
			mux.setCoat(cmp -> {
				int numCol = (int) (Math.random() * 4 + 1);
				int numRow = (int) (Math.random() * 4 + 1);
				Layouts.setGrid(cmp).numColumns(numCol);
				Color color = ColorPool.forWidget(mux).getColor(new RGB(r, g, b));
				for (int i = 0; i < numRow * numCol; ++i) {
					Composite child = new Composite(cmp, SWT.BORDER);
					Layouts.setGridData(child).grabAll();
					child.setBackground(color);
				}
			});
		});
	}

	@Test
	public void ctabFolderTest() {
		InteractiveTest.testCoat("Test coat mux layout behavior inside a CTabFolder.", cmp -> {
			Layouts.setGrid(cmp);
			CTabFolder folder = new CTabFolder(cmp, SWT.BORDER | SWT.CLOSE);
			Layouts.setGridData(folder).grabAll();

			ToolBarManager manager = new ToolBarManager();
			manager.add(Actions.create("Add", () -> {
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("New control");
				Composite parent = new Composite(folder, SWT.NONE);
				randomColorMux(parent);
				item.setControl(parent);
				item.addListener(SWT.Dispose, e -> item.getControl().dispose());
				folder.setSelection(item);
			}));
			Layouts.setGridData(manager.createControl(cmp)).grabHorizontal();
		});
	}
}
