/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.common.swt.jface;


import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.jface.Actions.Style;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(InteractiveTest.class)
public class JFaceRxTest {
	@Test
	public void testToggle() {
		InteractiveTest.testCoat("When you check and uncheck the action, the state of the selected text should change.", 20, SWT.DEFAULT, root -> {
			Layouts.setGrid(root);

			IAction action = Actions.builder()
					.setText("Action")
					.setStyle(Style.CHECK)
					.build();
			RxBox<Boolean> selection = JFaceRx.toggle(action);

			{
				Group cmp = new Group(root, SWT.SHADOW_ETCHED_IN);
				cmp.setText("Under test");
				Layouts.setGridData(cmp).grabHorizontal();
				Layouts.setFill(cmp);

				ToolBarManager manager = new ToolBarManager();
				manager.createControl(cmp);
				manager.add(action);
				manager.update(true);
			}
			{
				Group cmp = new Group(root, SWT.NONE);
				cmp.setText("Read");
				Layouts.setGridData(cmp).grabHorizontal();
				Layouts.setFill(cmp);

				Label selectedTxt = new Label(cmp, SWT.BORDER);
				Rx.subscribe(selection, s -> selectedTxt.setText(Boolean.toString(s)));
			}
			{
				Group cmp = new Group(root, SWT.NONE);
				cmp.setText("Write");
				Layouts.setGridData(cmp).grabHorizontal();
				Layouts.setFill(cmp);

				Button setFalse = new Button(cmp, SWT.PUSH);
				setFalse.setText("set false");
				setFalse.addListener(SWT.Selection, e -> selection.set(false));

				Button setTrue = new Button(cmp, SWT.PUSH);
				setTrue.setText("set true");
				setTrue.addListener(SWT.Selection, e -> selection.set(true));
			}
		});
	}
}
