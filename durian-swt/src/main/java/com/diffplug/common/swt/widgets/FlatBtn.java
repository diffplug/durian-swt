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
package com.diffplug.common.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.diffplug.common.swt.ControlWrapper;

/** Creates a flat button that raises a little on mouseOver. */
public class FlatBtn extends ControlWrapper.AroundControl<ToolBar> {
	private final ToolItem item;

	public FlatBtn(Composite parent, int style) {
		super(new ToolBar(parent, SWT.FLAT));
		item = new ToolItem(wrapped, style);
	}

	public ToolItem getItem() {
		return item;
	}

	/** Sets the text. */
	public void setText(String text) {
		item.setText(text);
	}

	/** Returns the text. */
	public String getText() {
		return item.getText();
	}

	/** Sets the image. */
	public void setImage(Image image) {
		item.setImage(image);
	}

	/** Returns the image. */
	public Image getImage() {
		return item.getImage();
	}

	/** Adds a listener. */
	public void addListener(int eventType, Listener listener) {
		item.addListener(eventType, listener);
	}

	/** Removes a listener. */
	public void removeListener(int eventType, Listener listener) {
		item.removeListener(eventType, listener);
	}

	/** Determines whether this FlatBtn is enabled or not. */
	public void setEnabled(boolean enabled) {
		item.setEnabled(enabled);
	}
}
