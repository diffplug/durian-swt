/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.common.swt.dnd;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.jface.Actions;
import com.diffplug.common.util.concurrent.Runnables;

@Category(InteractiveTest.class)
public class ToolBarDropTest {
	@Test
	public void testDrop() {
		InteractiveTest.testCoat("Should be able to drag the text onto the button", cmp -> {
			Layouts.setGrid(cmp);
			ToolBarManager manager = new ToolBarManager();
			IAction action = Actions.builder()
					.setText("Drag text from below and drop here")
					.setRunnable(Runnables.doNothing())
					.build();
			manager.add(action);
			ToolBar toolBar = manager.createControl(cmp);
			Layouts.setGridData(toolBar).grabHorizontal();

			Text text = new Text(cmp, SWT.BORDER | SWT.MULTI);
			Layouts.setGridData(text).grabAll();
			text.setText("LAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			text.setDragDetect(true);
			StructuredDrag drag = new StructuredDrag();
			drag.addText(event -> text.getSelectionText());
			drag.applyTo(text, DndOp.COPY, DndOp.MOVE);

			StructuredDrop drop = new StructuredDrop();
			drop.addText(new StructuredDrop.AbstractTypedDropHandler<String>(DndOp.COPY) {
				@Override
				protected boolean accept(String value) {
					return !value.isEmpty();
				}

				@Override
				protected void drop(DropTargetEvent event, String value, boolean moved) {
					text.setText(text.getText() + "\n\n Dropped: " + value);
				}
			});
			ToolBarDrop.addDropSupport(manager, action, drop.getListener());
		});
	}
}
