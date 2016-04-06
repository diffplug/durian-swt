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

import org.eclipse.swt.widgets.Layout;

/** Base class to Layouts{X}Layout. */
public abstract class LayoutWrapper<T extends Layout> {
	protected final T wrapped;

	protected LayoutWrapper(T wrapped) {
		this.wrapped = wrapped;
	}

	public T getRaw() {
		return wrapped;
	}

	/** Sets all margins to the given value. */
	public abstract LayoutWrapper<T> margin(int margin);

	/** Sets all spacing to the given value. */
	public abstract LayoutWrapper<T> spacing(int spacing);

	/** Sets the margin and spacing to {@link Layouts#defaultMargin()}. */
	public void setMarginAndSpacingToDefault() {
		margin(Layouts.defaultMargin());
		spacing(Layouts.defaultMargin());
	}
}
