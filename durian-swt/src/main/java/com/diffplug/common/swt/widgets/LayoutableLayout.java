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
package com.diffplug.common.swt.widgets;


import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/** A Layout which allows anyone to call its layout method manually. */
public abstract class LayoutableLayout extends Layout {
	@Override
	public abstract void layout(Composite composite, boolean flushCache);
}
