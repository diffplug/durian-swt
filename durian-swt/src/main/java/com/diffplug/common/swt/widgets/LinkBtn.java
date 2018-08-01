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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.swt.ControlWrapper;

/** Added a control that uses an SWT Link to implement the standard Button interface. */
public class LinkBtn extends ControlWrapper.AroundControl<Link> {
	public LinkBtn(Composite parent, int style) {
		super(new Link(parent, SWT.NONE));
		Preconditions.checkArgument(style == SWT.PUSH, "LinkBtn only supports SWT.PUSH");
	}

	public void setText(String text) {
		wrapped.setText("<a>" + text + "</a>");
	}

	/** Returns the text. */
	public String getText() {
		String txt = wrapped.getText();
		return txt.substring("<a>".length(), txt.length() - "</a>".length());
	}

	/** Adds a listener. */
	public void addListener(int eventType, Listener listener) {
		wrapped.addListener(eventType, listener);
	}

	/** Removes a listener. */
	public void removeListener(int eventType, Listener listener) {
		wrapped.removeListener(eventType, listener);
	}

	/** Determines whether this FlatBtn is enabled or not. */
	public void setEnabled(boolean enabled) {
		wrapped.setEnabled(enabled);
	}
}
