/*
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

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

import com.diffplug.common.rx.RxGetter;
import com.diffplug.common.rx.RxOptional;

/** A widget that switches between multiple {@code Coat}s. */
public class CoatMux extends ControlWrapper<Composite> {
	/** The StackLayout for switching between layers. */
	private StackLayout stack = new StackLayout();
	/** The currently displayed layer (if any). */
	private RxOptional<Layer<?>> currentLayer = RxOptional.ofEmpty();

	public CoatMux(Composite parent) {
		super(new Composite(parent, SWT.NONE));
		wrapped.setLayout(stack);
		// when the current layer changes, set the topControl appropriately
		SwtExec.immediate().guardOn(wrapped).subscribe(currentLayer, opt -> {
			stack.topControl = opt.map(layer -> layer.composite).orElse(null);
			wrapped.layout(true, true);
		});
	}

	/** Represents a persistent layer within this {@code CoatMux}. It can be shown, hidden, and disposed. */
	public class Layer<T> {
		final Composite composite;
		final T handle;
		final RxGetter<Boolean> rxCurrent = currentLayer.map(opt -> opt.isPresent() && opt.get() == this);

		private Layer(Coat.Returning<T> coat) {
			composite = new Composite(wrapped, SWT.NONE);
			handle = coat.putOn(composite);
		}

		/** {@link RxGetter} which keeps track of whether this {@code Layer} is currently on top. */
		public RxGetter<Boolean> rxOnTop() {
			return rxCurrent;
		}

		/** The handle which was returned by the {@link Coat.Returning}. */
		public T getHandle() {
			return handle;
		}

		/** Brings this layer to the top. */
		public void bringToTop() {
			currentLayer.set(Optional.of(this));
		}

		/** Disposes the contents of this layer. */
		public void dispose() {
			if (rxCurrent.get()) {
				currentLayer.set(Optional.empty());
			}
			SwtExec.immediate().execute(composite::dispose);
		}
	}

	/** Adds a persistent {@link Layer} which will be populated immediately by the given {@code Coat}. */
	public <T> Layer<T> add(Coat.Returning<T> coat) {
		return new Layer<T>(coat);
	}

	/** Sets the current content of this {@code CoatMux}, gets disposed as soon as anything else becomes the top layer. */
	public <T> T set(Coat.Returning<T> coat) {
		// create the layer
		Layer<T> layer = add(coat);
		// bring it to the top
		layer.bringToTop();
		// dispose the layer when it's no longer current
		SwtExec.immediate().guardOn(layer.composite).subscribe(layer.rxCurrent, isCurrent -> {
			if (!isCurrent) {
				layer.dispose();
			}
		});
		// return the handle that the coat created
		return layer.handle;
	}
}
