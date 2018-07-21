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
package com.diffplug.common.swt;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.base.Box;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.rx.Chit;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;

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

	enum TestEnum {
		A, B, C
	}

	@Test
	public void testComboAsBox() {
		InteractiveTest.testCoat("The two combos should stay in sync.", cmp -> {
			ImmutableList<TestEnum> values = ImmutableList.copyOf(TestEnum.class.getEnumConstants());

			Supplier<Combo> creator = () -> {
				Combo combo = new Combo(cmp, SWT.DROP_DOWN);
				values.forEach(value -> combo.add(value.name()));
				combo.select(0);
				return combo;
			};
			Layouts.setGrid(cmp).numColumns(2);

			Combo comboA = creator.get();
			RxBox<TestEnum> boxA = SwtRx.combo(comboA, values, Enum::name);

			Combo comboB = creator.get();
			RxBox<TestEnum> boxB = SwtRx.combo(comboB, values, Enum::name);

			Rx.subscribe(boxA, boxB);
			Rx.subscribe(boxB, boxA);
		});
	}

	@Test
	public void testDisposableEar() {
		InteractiveTest.testCoat("Non-interactive, will pass itself", cmp -> {
			Shell underTest = new Shell(cmp.getShell(), SWT.NONE);
			Chit chit = SwtRx.chit(underTest);
			Assert.assertFalse(chit.isDisposed());

			Box<Boolean> hasBeenDisposed = Box.of(false);
			chit.runWhenDisposed(() -> hasBeenDisposed.set(true));

			Assert.assertFalse(hasBeenDisposed.get());
			underTest.dispose();
			Assert.assertTrue(hasBeenDisposed.get());
			Assert.assertTrue(chit.isDisposed());

			Box<Boolean> alreadyDisposed = Box.of(false);
			chit.runWhenDisposed(() -> alreadyDisposed.set(true));
			Assert.assertTrue(alreadyDisposed.get());

			InteractiveTest.closeAndPass(cmp);
		});
	}
}
