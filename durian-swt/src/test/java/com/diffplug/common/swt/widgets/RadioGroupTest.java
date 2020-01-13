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
package com.diffplug.common.swt.widgets;


import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.SwtExec;
import java.util.Arrays;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(InteractiveTest.class)
public class RadioGroupTest {
	private enum Option {
		A, B, C, D
	}

	private Option initial = Option.C;

	@Test
	public void test() {
		InteractiveTest
				.testCoat(
						"There should be radio buttons A through D.\n\n" + "The initial selection should be C.\n\n"
								+ "You should be able to read and write the selection using the test boxes.",
						-1, -1, cmp -> {
							RadioGroup<Option> group = RadioGroup.create();
							Arrays.asList(Option.values()).forEach(option -> {
								group.addOption(option, option.name());
							});

							Layouts.setGrid(cmp);

							Composite radioParent = new Composite(cmp, SWT.NONE);
							Layouts.setGridData(radioParent).grabHorizontal();
							RxBox<Option> selection = group.getCoat().putOn(radioParent);
							selection.set(initial);

							Composite debugParent = new Composite(cmp, SWT.NONE);
							Layouts.setGridData(debugParent).grabAll();
							Layouts.setGrid(debugParent).numColumns(2).margin(0);

							// read the selection
							Label readLbl = new Label(debugParent, SWT.NONE);
							readLbl.setText("Read selection: ");

							Text readTxt = new Text(debugParent, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
							SwtExec.immediate().guardOn(readTxt).subscribe(selection.asObservable(), option -> {
								readTxt.setText(option.name());
							});

							// write the selection
							Label writeLbl = new Label(debugParent, SWT.NONE);
							writeLbl.setText("Write selection: ");

							Text writeTxt = new Text(debugParent, SWT.BORDER | SWT.SINGLE);
							Listener writeListener = e -> {
								try {
									Option value = Option.valueOf(writeTxt.getText());
									selection.set(value);
								} catch (Throwable error) {
									// do nothing
								}
							};
							writeTxt.addListener(SWT.DefaultSelection, writeListener);
							writeTxt.addListener(SWT.FocusOut, writeListener);
							writeTxt.addListener(SWT.Modify, writeListener);
						});
	}
}
