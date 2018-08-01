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

import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(InteractiveTest.class)
public class ShellsTest {
	private static final int UNIT = SwtMisc.systemFontWidth() * 20;

	@Test
	public void testPack() {
		testCase("The shell should be a rectangle just large enough to see all the content.", shells -> {
			shells.setSize(null);
		});
	}

	@Test
	public void testSquare() {
		testCase("The shell should be a square too small to see all the content.", shells -> {
			shells.setSize(new Point(UNIT, UNIT));
		});
	}

	@Test
	public void testWidthFixed() {
		testCase("The shell should be a skinny rectangle just big enough to see all the content.", shells -> {
			shells.setSize(new Point(UNIT, 0));
		});
	}

	@Test
	public void testHeightFixed() {
		testCase("The shell should be a really wide rectangle too short to see all the content.", shells -> {
			shells.setSize(new Point(0, UNIT));
		});
	}

	private void testCase(String instructions, Consumer<Shells> setSize) {
		InteractiveTest.testShell(instructions, display -> {
			Shells includesTrim = Shells.builder(SWT.DIALOG_TRIM, this::longText);
			setSize.accept(includesTrim);
			includesTrim.setPositionIncludesTrim(true);
			includesTrim.setTitle("Includes trim");
			Shell includesTrimResult = includesTrim.openOnDisplay();

			Shells notIncludesTrim = Shells.builder(SWT.DIALOG_TRIM, this::longText);
			setSize.accept(notIncludesTrim);
			notIncludesTrim.setPositionIncludesTrim(false);
			notIncludesTrim.setTitle("Excludes trim");
			notIncludesTrim.setLocation(Corner.TOP_LEFT, Corner.BOTTOM_LEFT.getPosition(includesTrimResult));
			notIncludesTrim.openOn(includesTrimResult);

			return includesTrimResult;
		});
	}

	private void longText(Composite parent) {
		Layouts.setFill(parent);

		Text text = new Text(parent, SWT.WRAP);
		text.setEditable(false);
		text.setText("The shell size can be set in the following ways:\n\n" + "- setSize(null): the shell is packed as tight as possible\n" + "- setSize(x, y): the shell is set to size (x, y)\n" + "- setSize(x, 0): the shell is set to width x, and its height is packed as tight as possible\n" + "- setSize(0, y): the shell is set to height y, and its width is packed as tight as possible\n");
	}
}
