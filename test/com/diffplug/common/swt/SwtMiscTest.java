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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.FieldsAndGetters;

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

	@Test
	public void testCopyEvent() {
		Event original = new Event();
		FieldsAndGetters.fields(original).map(Map.Entry::getKey).forEach(Errors.rethrow().wrap(field -> {
			if (field.getType().equals(boolean.class)) {
				field.set(original, Math.random() < 0.5 ? false : true);
			} else if (field.getType().equals(int.class)) {
				field.set(original, (int) (1000 * Math.random()));
			}
		}));
		Function<Event, List<Object>> getData = e -> FieldsAndGetters.fields(e).map(Map.Entry::getValue).collect(Collectors.toList());
		Event copy = SwtMisc.copyEvent(original);
		Assert.assertEquals(getData.apply(original), getData.apply(copy));
	}

	@Test
	public void testWithGc() {
		Point size = SwtMisc.withGcCompute(gc -> gc.textExtent("billy_bob"));
		Assert.assertTrue(size.y > 0);
		Assert.assertTrue(size.x > size.y);
	}
}
