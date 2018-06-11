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
package com.diffplug.common.swt.jface;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.Box;

public class ActionsTest {
	@Test
	public void testCopy() {
		// we'll set this variable to show that it's running as expected
		Box.Nullable<String> toSet = Box.Nullable.ofNull();

		// create an action
		IAction action = Actions.builder()
				.setText("Name")
				.setTooltip("Tooltip")
				.setAccelerator(SWT.SHIFT | 'a')
				.setRunnable(() -> toSet.set("WasRun")).build();

		// make sure it's doing what we expect
		Assert.assertEquals("Name", action.getText());
		Assert.assertEquals("Tooltip [Shift A]", action.getToolTipText());
		Assert.assertEquals(SWT.SHIFT | 'a', action.getAccelerator());
		Assert.assertEquals(null, toSet.get());
		action.run();
		Assert.assertEquals("WasRun", toSet.get());

		// copy that action
		IAction copy = Actions.builderCopy(action).setAccelerator(SWT.NONE)
				.setRunnable(() -> toSet.set("CopyWasRun")).build();

		Assert.assertEquals(SWT.NONE, copy.getAccelerator());
		// test that the tooltip was stripped correctly in the copy
		Assert.assertEquals("Tooltip", copy.getToolTipText());
		// make sure that the runnable took
		copy.run();
		Assert.assertEquals("CopyWasRun", toSet.get());
		// but didn't screw up the other one
		action.run();
		Assert.assertEquals("WasRun", toSet.get());
	}

	@Test
	public void testAcceleratorString() {
		testCase("X", 'x');
		testCase("1", '1');

		testCase("F5", SWT.F5);
		testCase("F12", SWT.F12);

		testCase(Actions.UC_ARROW_LEFT, SWT.ARROW_LEFT);
		testCase(Actions.UC_ARROW_RIGHT, SWT.ARROW_RIGHT);
		testCase(Actions.UC_ARROW_UP, SWT.ARROW_UP);
		testCase(Actions.UC_ARROW_DOWN, SWT.ARROW_DOWN);

		testCase("<none>", SWT.NONE);
		testCase("Esc", SWT.ESC);

		testCase("Ctrl X", SWT.CONTROL | 'x');
		testCase("Shift 1", SWT.SHIFT | '1');
		testCase(Actions.UC_CMD + " .", SWT.COMMAND | '.');
		testCase("Alt =", SWT.ALT | '=');

		testCase("Ctrl Shift X", SWT.CONTROL | SWT.SHIFT | 'x');
		testCase("Alt Shift 1", SWT.ALT | SWT.SHIFT | '1');
	}

	private void testCase(String expected, int accelerator) {
		Assert.assertEquals(expected, Actions.getAcceleratorString(accelerator));
	}
}
