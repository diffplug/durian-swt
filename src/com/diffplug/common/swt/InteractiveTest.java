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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Unhandled;

/**
 * InteractiveTest opens a {@link Coat} or {@link Shell}, and displays instructions
 * for a human tester to determine whether the test passed or failed.  This makes
 * it extremely easy to create and specify a UI test, which can be converted into
 * an automated UI test at a later date.
 * <p>
 * If the system property {@code com.diffplug.InteractiveTest.autoclose.milliseconds}
 * is set, then the tests will open and then automatically pass after the specified
 * timeout. This lets a headless CI server ensure that the tests are in working order,
 * although a meatbag is still required for full validation.
 */
public class InteractiveTest {
	private InteractiveTest() {}

	/** Returns an optional of the proper autoclose delay. */
	private static Optional<Integer> autoCloseMs() {
		String value = System.getProperty(AUTOCLOSE_KEY);
		if (value == null) {
			return Optional.empty();
		} else {
			return Errors.log().getWithDefault(() -> {
				int intValue = Integer.parseInt(value);
				Preconditions.checkArgument(intValue > 0, "%s should be positive or non-existent, this was %s", AUTOCLOSE_KEY, intValue);
				return Optional.of(intValue);
			}, Optional.<Integer> empty());
		}
	}

	/** Key for specifying that autoclose should be used. */
	public static final String AUTOCLOSE_KEY = "com.diffplug.test.autoclose.milliseconds";

	/** Marker class for interactive tests that aren't compatible with auto-close. */
	public static class FailsWithoutUser {}

	/** Default width of testCoat(). */
	public static final int DEFAULT_COLS = 60;
	/** Default height of testCoat(). */
	public static final int DEFAULT_ROWS = 40;

	/**
	 * @param instructions Instructions for the user to follow.
	 * @param coat A function to populate the test composite.
	 */
	public static void testCoat(String instructions, Coat coat) {
		testCoat(instructions, DEFAULT_COLS, DEFAULT_ROWS, coat);
	}

	/**
	 * @param instructions Instructions for the user to follow.
	 * @param cols Width of the test composite (in multiples of the system font height).
	 * @param rows Height of the test composite (in multiples of the system font height). 
	 * @param coat A function to populate the test composite.
	 */
	public static void testCoat(String instructions, int cols, int rows, Coat coat) {
		Point size = null;
		if (cols > 0 || rows > 0) {
			size = SwtMisc.scaleByFontHeight(cols, rows);
		}
		testCoat(instructions, size, coat);
	}

	/**
	 * @param instructions Instructions for the user to follow.
	 * @param size Width and height of the test composite (in multiples of the system font height).
	 * @param coat A function to populate the test composite.
	 */
	public static void testCoat(String instructions, Point size, Coat coat) {
		testShell(instructions, display -> {
			return Shells.builder(SWT.SHELL_TRIM, coat)
					.setTitle("UnderTest")
					.setSize(size)
					.openOnDisplay();
		});
	}

	/** A map containing the shells for the currently running test, mapped to the box which holds its result. */
	private static Map<Shell, Box<TestResult>> shellToResult = new HashMap<>();

	/**
	 * @param instructions Instructions for the user to follow.
	 * @param harness A function which takes a Display and returns a Shell to test.
	 */
	public static void testShell(String instructions, Function<Display, Shell> harness) {
		SwtExec.blocking().execute(() -> {
			Display display = SwtMisc.assertUI();

			// the test is a failure until the user tells us otherwise
			Box<TestResult> result = Box.ofVolatile(TestResult.FAIL);

			try {
				// create the shell under test
				Shell underTest = harness.apply(display);
				underTest.setLocation(10, 10);
				shellToResult.put(underTest, result);

				// create the test dialog
				Shell instructionsDialog = openInstructions(underTest, instructions, result);
				// if the user closes the instructions, close the test too
				instructionsDialog.addListener(SWT.Dispose, e -> underTest.dispose());
				// make sure underTest has focus
				underTest.setActive();

				// if we're in autoclose mode, then we'll dispose the test after a timeout
				autoCloseMs().ifPresent(autoCloseMs -> {
					SwtExec.async().guardOn(underTest).timerExec(autoCloseMs, () -> {
						// dispose the test shell (we'll let the instructions shell dispose itself)
						underTest.dispose();
						// set the result to be a pass
						result.set(TestResult.PASS);
					});
				});

				// wait for the result
				SwtMisc.loopUntilDisposed(underTest);

				// take the appropriate action for that result
				switch (result.get()) {
				case PASS:
					return;
				case FAIL:
					throw new AssertionError(instructions);
				default:
					throw Unhandled.enumException(result.get());
				}
			} finally {
				// dispose everything at the end
				for (Shell shell : display.getShells()) {
					shell.dispose();
					shellToResult.remove(shell);
				}
			}
		});
	}

	private static void setResult(Control ctl, TestResult result) {
		Shell shell = ctl.getShell();
		SwtExec.async().guardOn(ctl).execute(() -> {
			Box<TestResult> resultBox = shellToResult.remove(shell);
			Objects.requireNonNull(resultBox, "No test shell for control.");
			resultBox.set(result);
			shell.dispose();
		});
	}

	/** Closes the test for the given control, and passes. */
	public static void closeAndPass(Control ctl) {
		setResult(ctl, TestResult.PASS);
	}

	/** Closes the test for the given control, and fails. */
	public static void closeAndFail(Control ctl) {
		setResult(ctl, TestResult.FAIL);
	}

	/**
	 * Same as testShell, but for situations where it is impossible to return the shell handle.
	 * 
	 * @param instructions Instructions for the user to follow.
	 * @param harness A function which takes a Display and returns a Shell to test.  The instructions will pop-up next to the test shell.
	 */
	public static void testShellWithoutHandle(String instructions, Consumer<Display> harness) {
		testShell(instructions, display -> {
			// initiate the thing that should create the dialog
			harness.accept(display);

			// wait until this dialog is created
			SwtMisc.loopUntil(d -> d.getShells().length > 0);

			// return the dialog that was created
			Shell[] shells = display.getShells();
			if (shells.length >= 1) {
				return shells[0];
			} else {
				throw new IllegalArgumentException("The test harness didn't create a shell.");
			}
		});
	}

	/** The result of a TestDialog. */
	private enum TestResult {
		PASS, FAIL
	}

	/** Opens the instructions dialog. */
	private static Shell openInstructions(Shell underTest, String instructions, Box<TestResult> result) {
		Shell instructionsShell = Shells.builder(SWT.TITLE | SWT.BORDER, cmp -> {
			Layouts.setGrid(cmp).numColumns(3);

			// show the instructions
			Text text = new Text(cmp, SWT.WRAP);
			Layouts.setGridData(text).horizontalSpan(3).grabAll();
			text.setEditable(false);
			text.setText(instructions);

			// pass / fail buttons
			Layouts.newGridPlaceholder(cmp).grabHorizontal();

			Consumer<TestResult> buttonCreator = val -> {
				Button btn = new Button(cmp, SWT.PUSH);
				btn.setText(val.name());
				btn.addListener(SWT.Selection, e -> {
					result.set(val);
					cmp.getShell().dispose();
				});
				Layouts.setGridData(btn).widthHint(SwtMisc.defaultButtonWidth());
			};
			buttonCreator.accept(TestResult.PASS);
			buttonCreator.accept(TestResult.FAIL);
		})
				.setTitle("PASS / FAIL")
				.setSize(SwtMisc.scaleByFontHeight(18, 0))
				.openOn(underTest);

		// put the instructions to the right of the dialog under test 
		Rectangle instructionsBounds = instructionsShell.getBounds();
		Rectangle underTestBounds = underTest.getBounds();
		instructionsBounds.x = underTestBounds.x + underTestBounds.width + HORIZONTAL_SEP;
		instructionsBounds.y = underTestBounds.y;
		instructionsShell.setBounds(instructionsBounds);

		// return the value
		return instructionsShell;
	}

	/** Spacing between the Shell under test and the instructions shell. */
	private static final int HORIZONTAL_SEP = 15;
}
