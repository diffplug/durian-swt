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

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.Shells;

@Category(InteractiveTest.class)
public class ShellsTest {
	@Test
	public void testPack() {
		testCase("The shell should be a rectangle just large enough to see all the content.", shells -> {
			shells.setSize(null);
		});
	}

	@Test
	public void testSquare() {
		testCase("The shell should be a square too small to see all the content.", shells -> {
			shells.setSize(new Point(100, 100));
		});
	}

	@Test
	public void testWidthFixed() {
		testCase("The shell should be a skinny rectangle just big enough to see all the content.", shells -> {
			shells.setSize(new Point(100, 0));
		});
	}

	@Test
	public void testHeightFixed() {
		testCase("The shell should be a really wide rectangle too short to see all the content.", shells -> {
			shells.setSize(new Point(0, 50));
		});
	}

	private void testCase(String instructions, Consumer<Shells> setSize) {
		InteractiveTest.testShell(instructions, display -> {
			Shells underTest = Shells.create(SWT.DIALOG_TRIM, this::longText);
			setSize.accept(underTest);
			return underTest.openOnDisplay();
		});
	}

	private void longText(Composite parent) {
		Layouts.setFill(parent);

		Text text = new Text(parent, SWT.WRAP);
		text.setEditable(false);
		text.setText("The shell size can be set in the following ways:\n\n" + "- setSize(null): the shell is packed as tight as possible\n" + "- setSize(x, y): the shell is set to size (x, y)\n" + "- setSize(x, 0): the shell is set to width x, and its height is packed as tight as possible\n" + "- setSize(0, y): the shell is set to height y, and its width is packed as tight as possible\n");
	}
}
