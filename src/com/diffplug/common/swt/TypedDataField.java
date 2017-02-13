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

import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.swt.widgets.Widget;

/**
 * Typed utility for getting and setting data using SWT's Widget getData() / setData() API.
 * 
 * ```
 * TypedDataField<Model> field = TypedDataField.create("model");
 * 
 * Model model = field.get(widget);
 * field.set(widget, model);
 * ```
 */
public class TypedDataField<T, W extends Widget> {
	public static <T, W extends Widget> TypedDataField<T, W> create(String key) {
		return new TypedDataField<>(key);
	}

	final String key;

	TypedDataField(String key) {
		this.key = Objects.requireNonNull(key);
	}

	public T get(Widget widget) {
		return Objects.requireNonNull(getNullable(widget));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public T getNullable(Widget widget) {
		return (T) widget.getData(key);
	}

	public void set(W widget, T value) {
		widget.setData(key, Objects.requireNonNull(value));
	}

	public void setNullable(W widget, @Nullable T value) {
		widget.setData(key, value);
	}
}
