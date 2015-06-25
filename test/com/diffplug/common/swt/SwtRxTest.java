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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.SwtRx;

@Category(InteractiveTest.class)
public class SwtRxTest {
	@Test
	public void testToggleAsBox() {
		InteractiveTest.testCoat("The toggle button at top should stay in sync with the text and the other two buttons.", -1, -1, cmp -> {
			Layouts.setFill(cmp).vertical();

			Button toggleBtn = new Button(cmp, SWT.TOGGLE);
			toggleBtn.setText("Toggle");
			RxBox<Boolean> toggleBox = SwtRx.toggle(toggleBtn);

			new Label(cmp, SWT.SEPARATOR | SWT.HORIZONTAL);

			Text toggleState = new Text(cmp, SWT.SINGLE | SWT.BORDER);
			Rx.subscribe(toggleBox, val -> {
				toggleState.setText("State: " + val);
			});

			Button onBtn = new Button(cmp, SWT.PUSH);
			onBtn.setText("Set on");
			onBtn.addListener(SWT.Selection, e -> toggleBox.set(true));
			Button offBtn = new Button(cmp, SWT.PUSH);
			offBtn.setText("Set off");
			offBtn.addListener(SWT.Selection, e -> toggleBox.set(false));
		});
	}
}
