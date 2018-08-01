/*
 * Copyright 2018 DiffPlug
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.BiMap;
import com.diffplug.common.collect.HashBiMap;
import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.collect.Maps;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.Coat;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.SwtExec;

public class RadioGroup<T> {
	private final Optional<RxBox<T>> source;
	private LinkedHashMap<String, T> options = Maps.newLinkedHashMap();

	/** Guava-style constructor. */
	public static <T> RadioGroup<T> create() {
		return new RadioGroup<T>(Optional.empty());
	}

	/** Guava-style constructor. */
	public static <T> RadioGroup<T> create(RxBox<T> source) {
		return new RadioGroup<T>(Optional.of(source));
	}

	protected RadioGroup(Optional<RxBox<T>> source) {
		this.source = source;
	}

	public RadioGroup<T> addOption(T value, String name) {
		options.put(name, value);
		return this;
	}

	public RadioGroup<T> addOptions(Iterable<T> values, Function<T, String> labelProvider) {
		for (T value : values) {
			addOption(value, labelProvider.apply(value));
		}
		return this;
	}

	public Coat.Returning<RxBox<T>> getCoat() {
		Preconditions.checkArgument(options.size() > 0, "Must be at least one option!");
		ImmutableMap<String, T> options = ImmutableMap.copyOf(this.options);
		return new Coat.Returning<RxBox<T>>() {
			@Override
			public RxBox<T> putOn(Composite cmp) {
				// object which will hold the current selection
				RxBox<T> selection = source.orElseGet(() -> RxBox.of(Iterables.get(options.values(), 0)));

				// mapping from button to objects
				BiMap<Button, T> mapping = HashBiMap.create();

				Layouts.setFill(cmp).vertical().margin(0);
				for (Map.Entry<String, T> entry : options.entrySet()) {
					// create a button
					Button btn = new Button(cmp, SWT.RADIO);
					btn.setText(entry.getKey());
					// update the mapping
					mapping.put(btn, entry.getValue());
					// update selection when a button is clicked
					btn.addListener(SWT.Selection, e -> {
						selection.set(mapping.get(btn));
					});
				}

				// when the selection is changed, set the button
				Button firstBtn = Iterables.get(mapping.keySet(), 0);
				SwtExec.immediate().guardOn(firstBtn).subscribe(selection.asObservable(), obj -> {
					Button selectedBtn = mapping.inverse().get(obj);
					for (Button btn : mapping.keySet()) {
						btn.setSelection(btn == selectedBtn);
					}
				});
				return selection;
			}
		};
	}
}
