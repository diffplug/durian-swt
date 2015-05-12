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

import org.eclipse.swt.widgets.Control;

/** Wraps an SWT Control to hide most of its API. */
public class ControlWrapper<T extends Control> {
	protected final T control;

	public ControlWrapper(T control) {
		this.control = control;
	}

	/** Sets the layoutData for this control. */
	public void setLayoutData(Object layoutData) {
		control.setLayoutData(layoutData);
	}

	/** Returns the underlying control. */
	public T getControl() {
		return control;
	}
}
