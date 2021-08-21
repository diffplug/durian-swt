/*
 * Copyright (C) 2020-2021 DiffPlug
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
package com.diffplug.common.swt.jface;


import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import java.util.Arrays;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(InteractiveTest.class)
public class ColumnViewerFormatTest {
	@Test
	public void testCopy() {
		InteractiveTest.testCoat("Column viewer", 10, 10, cmp -> {
			Layouts.setFill(cmp);
			buildTableWithCheck(new Composite(cmp, SWT.BORDER), true);
			buildTableWithCheck(new Composite(cmp, SWT.BORDER), false);
		});
	}

	private void buildTableWithCheck(Composite cmp, boolean hasCheck) {
		ColumnViewerFormat<String> format = ColumnViewerFormat.builder();
		format.setStyle(SWT.FULL_SELECTION | (hasCheck ? SWT.CHECK : SWT.NONE));
		format.addColumn().setLabelProviderText(s -> s);
		TableViewer viewer = format.buildTable(cmp);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(Arrays.asList("Onesy twosy threesy foursy", "red orange yellow green blue purple"));
	}
}
