/*
 * Copyright (C) 2025 DiffPlug
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
package com.diffplug.common.swt;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(InteractiveTest.class)
public class Issue_2228 {
	@Test
	public void recreate() {
		// testCoat probably takes a title and a lambda that receives the parent Composite
		InteractiveTest.testCoat(
				"Show the difference between `.svg` and `@x2.png` rendering",
				(Composite parent) -> {
					// two-column grid
					Layouts.setGrid(parent).numColumns(2);

					// “.svg” label + image
					Label svgLabel = new Label(parent, SWT.NONE);
					svgLabel.setText(".svg");

					Label svgImage = new Label(parent, SWT.NONE);
					ImageDescriptor svgDesc = ImageDescriptor.createFromFile(
							Issue_2228.class, "/issue_2228/strikethrough.svg");
					svgImage.setImage(svgDesc.createImage());

					// “.png” label + image
					Label pngLabel = new Label(parent, SWT.NONE);
					pngLabel.setText(".png");

					Label pngImage = new Label(parent, SWT.NONE);
					ImageDescriptor pngDesc = ImageDescriptor.createFromFile(
							Issue_2228.class, "/issue_2228/strikethrough.png");
					pngImage.setImage(pngDesc.createImage());
				});
	}
}
