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
 * Wraps an SWT Control to encapsulate its API.
 * <p>
 * The traditional way to make a custom class is this: {@code class CustomControl extends Composite}
 * <p>
 * This has three main problems:
 * <ol>
 * <li>Users can add random widgets to your "Control" because it exposes the {@code Composite} interface.</li>
 * <li>Users can set the layout to your "Control" because it exposes the {@code Composite} interface.</li>
 * <li>Users can add random listeners to your "Control", and overridding {@code addListener} to intercept them is a <b>very dangerous plan</b>.</li>
 * </ol>
 * <p>
 * ControlWrapper fixes this by providing an low-overhead skeleton which encapsulates the
 * SWT Control that you're using as the base of your custom control, which allows you to only
 * expose the APIs that are appropriate.
 */
public class ControlWrapper<T extends Control> {
	/** The wrapped control. */
	protected final T wrapped;

	/** Creates a ControlWrapper which wraps the given control. */
	public ControlWrapper(T wrapped) {
		this.wrapped = wrapped;
	}

	/** Sets the LayoutData for this control. */
	public final void setLayoutData(Object layoutData) {
		wrapped.setLayoutData(layoutData);
	}

	/** Returns the LayoutData for this control. */
	public final Object getLayoutData() {
		return wrapped.getLayoutData();
	}

	/** Returns the parent of this Control. */
	public final Composite getParent() {
		return wrapped.getParent();
	}

	/**
	 * Returns the wrapped Control (only appropriate for limited purposes!).
	 * <p>
	 * The implementor of this ControlWrapper is free to change the wrapped Control
	 * as she sees fit, and she doesn't have to tell you about it!  You shouldn't rely
	 * on this control being anything in particular.
	 * <p>
	 * You <i>can</i> rely on this Control for:
	 * <ol>
	 * <li>Managing lifetimes: {@code wrapped.getRootControl().addListener(SWT.Dispose, ...}</li>
	 * </ol>
	 * <p>
	 * But that's all. If you use it for something else, it's on you when it breaks.
	 */
	public Control getRootControl() {
		return wrapped;
	}
}
