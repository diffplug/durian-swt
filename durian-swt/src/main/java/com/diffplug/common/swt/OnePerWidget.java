/*
 * Copyright 2020 DiffPlug
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


import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Widget;

/**
 * Maintains a cache of values which are mapped to SWT widgets.  The
 * cache is automatically updated as these widgets are disposed.
 * 
 * Useful for implementing resource managers, such as {@link ColorPool}.
 */
public abstract class OnePerWidget<WidgetType extends Widget, T> {
	/** Creates a OnePerWidget instance where objects are created using the given function. */
	public static <WidgetType extends Widget, T> OnePerWidget<WidgetType, T> from(Function<? super WidgetType, ? extends T> creator) {
		return new OnePerWidget<WidgetType, T>() {
			@Override
			protected T create(WidgetType widget) {
				return creator.apply(widget);
			}
		};
	}

	private Map<WidgetType, T> map = new HashMap<>();

	/** Returns the object for the given control. */
	public T forWidget(WidgetType ctl) {
		T value = map.get(ctl);
		if (value == null) {
			value = create(ctl);
			map.put(ctl, value);
			ctl.addListener(SWT.Dispose, e -> {
				map.remove(ctl);
			});
		}
		return value;
	}

	/** Creates a new object for the control. */
	protected abstract T create(WidgetType ctl);
}
