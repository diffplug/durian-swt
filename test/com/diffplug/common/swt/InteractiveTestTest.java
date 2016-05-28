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

import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.collect.Range;
import com.diffplug.common.swt.InteractiveTest.FailsWithoutUser;
import com.diffplug.common.util.concurrent.Uninterruptibles;

@Category(InteractiveTest.class)
public class InteractiveTestTest {
	@Category(FailsWithoutUser.class)
	@Test(expected = AssertionError.class)
	public void testFail() {
		InteractiveTest.testCoat("A blank dialog should be open to the left, press Fail.", 30, 0, this::dummyCoat);
	}

	@Category(FailsWithoutUser.class)
	@Test(expected = AssertionError.class)
	public void testClose() {
		InteractiveTest.testCoat("A blank dialog should be open to the left, close it (which counts as Fail).", 30, 0, this::dummyCoat);
	}

	@Test
	public void testPass() {
		InteractiveTest.testCoat("A blank dialog should be open to the left, press Pass.", 30, 0, this::dummyCoat);
	}

	@Test
	public void testShell() {
		InteractiveTest.testShell("A blank dialog should be open to the left, press Pass", display -> {
			return Shells.builder(SWT.SHELL_TRIM, this::dummyCoat).setSize(SwtMisc.scaleByFontHeight(30, 0)).openOnDisplay();
		});
	}

	@Test
	public void testShellWithoutHandle() {
		InteractiveTest.testShellWithoutHandle("A blank dialog should be open to the left, press Pass", display -> {
			new Thread(() -> {
				// the test harness will spin the SWT Display loop until a Shell is opened
				Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
				// now we'll open it
				SwtExec.async().execute(() -> {
					Shells.builder(SWT.SHELL_TRIM, this::dummyCoat).setSize(SwtMisc.scaleByFontHeight(30, 0)).openOnDisplay();
				});
			}, "testShellWithoutHandle").start();
		});
	}

	@Test
	public void testAutoclose() {
		long AUTOCLOSE_TEST_DUR = 2000;
		long TOLERANCE = 1000;

		// store the current AUTOCLOSE_KEY value so we can restore it after the test
		String autocloseValue = System.getProperty(InteractiveTest.AUTOCLOSE_KEY);
		try {
			System.setProperty(InteractiveTest.AUTOCLOSE_KEY, Long.toString(AUTOCLOSE_TEST_DUR));

			long testStart = System.currentTimeMillis();
			InteractiveTest.testCoat("This test should automatically pass after " + (AUTOCLOSE_TEST_DUR / 1000) + " second...", 30, 0, this::dummyCoat);
			long testElapsed = System.currentTimeMillis() - testStart;

			Range<Long> validRange = Range.closed(AUTOCLOSE_TEST_DUR - TOLERANCE, AUTOCLOSE_TEST_DUR + TOLERANCE);
			Assert.assertTrue("Elapsed " + testElapsed + " exceeds range " + validRange, validRange.contains(testElapsed));
		} finally {
			// restore the previous value for AUTOCLOSE_KEY
			if (autocloseValue == null) {
				System.clearProperty(InteractiveTest.AUTOCLOSE_KEY);
			} else {
				System.setProperty(InteractiveTest.AUTOCLOSE_KEY, autocloseValue);
			}
		}
	}

	@Test
	public void testCloseAndPass() {
		InteractiveTest.testCoat("Should pass itself very quickly.", cmp -> {
			InteractiveTest.closeAndPass(cmp);
		});
	}

	@Test(expected = AssertionError.class)
	public void testCloseAndFail() {
		InteractiveTest.testCoat("Should fail itself very quickly.", cmp -> {
			InteractiveTest.closeAndFail(cmp);
		});
	}

	/** The simple little coat which is displayed in each test. */
	private void dummyCoat(Composite parent) {
		Layouts.setFill(parent);

		Text text = new Text(parent, SWT.WRAP);
		text.setEditable(false);
		text.setText("Any function which takes a Composite and returns void is a Coat." + "\n\n" + "InteractiveTest opens a Coat or a Shell, and displays instructions for the " + "tester to determine whether the test passed or failed." + "\n\n" + "If the system property '" + InteractiveTest.AUTOCLOSE_KEY + "' is set, then " + "the tests will open and then automatically pass after the specified timeout.  " + "This lets a headless environment keep the tests in working order, although a " + "human is required for full validation.");
	}
}
