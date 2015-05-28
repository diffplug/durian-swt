/**
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Wraps an SWT Control to hide its API.
 * 
 * The traditional way to make a custom class is this: `class CustomControl extends Composite`
 * 
 * This has three main problems:
 * - Users can add random widgets to your "Control" because it exposes the Composite interface.
 * - Users can set the layout oo your "Control" because it exposes the Composite interface.
 * - Users can add random listeners to your "Control", and overridding "addListener" to intercept them is a VERY DANGEROUS PLAN.
 * 
 * ControlWrapper fixes this by providing an extremely low-overhead skeleton which encapsulates the
 * SWT Control that you're using as the base of your custom control, which allows you to only
 * expose the APIs that are appropriate.
 */
public class ControlWrapper<T extends Control> {
	/** The wrapped control. */
	protected final T control;

	public ControlWrapper(T control) {
		this.control = control;
	}

	/** Sets the LayoutData for this control. */
	public void setLayoutData(Object layoutData) {
		control.setLayoutData(layoutData);
	}

	/** Returns the parent of the Control. */
	public Composite getParent() {
		return control.getParent();
	}

	/**
	 * Returns the wrapped widget as a raw Control. Useful for writing SWT code that needs the Control
	 * instance such as DND code.
	 * 
	 * The returned Control is not exposed as T on purpose.
	 */
	public Control asControl() {
		return control;
	}
}
