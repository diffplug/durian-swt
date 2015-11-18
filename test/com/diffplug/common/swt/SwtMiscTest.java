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
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.swt.SwtMisc;

public class SwtMiscTest {
	@Test
	public void flagIsSet() {
		Assert.assertTrue(SwtMisc.flagIsSet(SWT.NONE, SWT.NONE));

		Assert.assertTrue(SwtMisc.flagIsSet(SWT.BOLD, SWT.BOLD));
		Assert.assertFalse(SwtMisc.flagIsSet(SWT.ITALIC, SWT.BOLD));
		Assert.assertFalse(SwtMisc.flagIsSet(SWT.BOLD, SWT.ITALIC));
		Assert.assertTrue(SwtMisc.flagIsSet(SWT.ITALIC, SWT.ITALIC));

		Assert.assertTrue(SwtMisc.flagIsSet(SWT.NONE, SWT.BOLD | SWT.ITALIC));
		Assert.assertTrue(SwtMisc.flagIsSet(SWT.BOLD, SWT.BOLD | SWT.ITALIC));
		Assert.assertTrue(SwtMisc.flagIsSet(SWT.ITALIC, SWT.BOLD | SWT.ITALIC));
		Assert.assertFalse(SwtMisc.flagIsSet(37, SWT.BOLD | SWT.ITALIC));
	}

	@Test
	public void setFlag() {
		Assert.assertEquals(SWT.BOLD, SwtMisc.setFlag(SWT.BOLD, true, SWT.NONE));
		Assert.assertEquals(SWT.BOLD, SwtMisc.setFlag(SWT.BOLD, true, SWT.BOLD));
		Assert.assertEquals(SWT.NONE, SwtMisc.setFlag(SWT.BOLD, false, SWT.BOLD));

		Assert.assertEquals(SWT.BOLD | SWT.ITALIC, SwtMisc.setFlag(SWT.ITALIC, true, SWT.BOLD));
		Assert.assertEquals(SWT.ITALIC, SwtMisc.setFlag(SWT.BOLD, false, SWT.ITALIC | SWT.BOLD));
		Assert.assertEquals(SWT.BOLD, SwtMisc.setFlag(SWT.ITALIC, false, SWT.ITALIC | SWT.BOLD));
		Assert.assertEquals(SWT.BOLD, SwtMisc.setFlag(SWT.ITALIC, false, SWT.BOLD));
	}
}
