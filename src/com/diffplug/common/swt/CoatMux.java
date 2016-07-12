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

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.rx.RxGetter;

/** A widget that switches between multiple `Coat`s. */
public class CoatMux extends ControlWrapper.AroundControl<Composite> {
	/** The StackLayout for switching between layers. */
	private StackLayout stack = new StackLayout();
	/** The currently displayed layer (if any). */
	private RxBox<Optional<Layer<?>>> currentLayer = RxBox.of(Optional.empty());

	public CoatMux(Composite parent) {
		this(parent, SWT.NONE);
	}

	public CoatMux(Composite parent, int style) {
		super(new Composite(parent, style));
		wrapped.setLayout(stack);
		// when the current layer changes, set the topControl appropriately
		Rx.subscribe(currentLayer, opt -> {
			stack.topControl = opt.map(layer -> layer.control).orElse(null);
			wrapped.layout(true, true);
		});
	}

	/** The current layer (if any). */
	public RxGetter<Optional<Layer<?>>> rxCurrent() {
		return currentLayer;
	}

	/** The Control at the top of the stack (possibly null). */
	public Control getTopControl() {
		return stack.topControl;
	}

	/** Represents a persistent layer within this `CoatMux`. It can be shown, hidden, and disposed. */
	public class Layer<T> {
		final Control control;
		final T handle;
		final RxGetter<Boolean> rxCurrent = currentLayer.map(opt -> opt.isPresent() && opt.get() == this);

		private Layer(Control control, T handle) {
			this.control = control;
			this.handle = handle;
		}

		/** {@link RxGetter} which keeps track of whether this `Layer` is currently on top. */
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
			SwtExec.immediate().execute(control::dispose);
		}
	}

	/** Adds a persistent {@link Layer} which will be populated immediately by the given `Coat`, using `value` as the key. */
	public <T> Layer<T> addCoat(Coat coat, @Nullable T value) {
		return addCoat(Coat.Returning.fromNonReturning(coat, value));
	}

	/** Adds a persistent {@link Layer} which will be populated immediately by the given `Coat.Returning`, using the return value as the key. */
	public <T> Layer<T> addCoat(Coat.Returning<T> coat) {
		Composite composite = new Composite(wrapped, SWT.NONE);
		return new Layer<T>(composite, coat.putOn(composite));
	}

	private static final String RAW = "The function must create exactly one child of the composite which gets passed in";

	/**
	 * Adds a persistent {@link Layer} which will be populated immediately by the given `Function<Composite, Control>`, with the returned control as the key.
	 * <p>
	 * The function must create exactly one child of the composite, and must return that child.
	 */
	public <T extends Control> Layer<T> addControl(Function<Composite, T> creator) {
		int before = wrapped.getChildren().length;
		T control = creator.apply(wrapped);
		int after = wrapped.getChildren().length;
		Preconditions.checkArgument(control.getParent() == wrapped, RAW);
		Preconditions.checkArgument(before + 1 == after, RAW);
		return new Layer<T>(control, control);
	}

	/**
	 * Adds a persistent {@link Layer} which will be populated immediately by the given `Function<Composite, Control>`, with the returned control as the key.
	 * <p>
	 * The function must create exactly one child of the composite, and it must return that child.
	 */
	public <T extends ControlWrapper> Layer<T> addWrapper(Function<Composite, T> creator) {
		int before = wrapped.getChildren().length;
		T wrapper = creator.apply(wrapped);
		int after = wrapped.getChildren().length;
		Preconditions.checkArgument(wrapper.getParent() == wrapped, RAW);
		Preconditions.checkArgument(before + 1 == after, RAW);
		return new Layer<T>(wrapper.getRootControl(), wrapper);
	}

	private <T> T makeTemporary(Layer<T> layer) {
		// bring it to the top
		layer.bringToTop();
		// dispose the layer when it's no longer current
		SwtExec.immediate().guardOn(layer.control).subscribe(layer.rxCurrent, isCurrent -> {
			if (!isCurrent) {
				layer.dispose();
			}
		});
		// return the handle that the coat created
		return layer.handle;
	}

	/** Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer. */
	public <T> T setCoat(Coat coat, @Nullable T value) {
		return setCoat(Coat.Returning.fromNonReturning(coat, value));
	}

	/** Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer. */
	public <T> T setCoat(Coat.Returning<T> coat) {
		return makeTemporary(addCoat(coat));
	}

	/** Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer. */
	public <T extends Control> T setControl(Function<Composite, T> creator) {
		return makeTemporary(addControl(creator));
	}

	/** Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer. */
	public <T extends ControlWrapper> T setWrapper(Function<Composite, T> creator) {
		return makeTemporary(addWrapper(creator));
	}

	//////////////////////
	// Deprecated stuff //
	//////////////////////
	/**
	 * Adds a persistent {@link Layer} which will be populated immediately by the given `Coat`, with the layer containing the given value.
	 * @deprecated use {@link #add(Coat, Object)} instead
	 */
	public <T> Layer<T> add(Coat coat, T value) {
		return addCoat(coat, value);
	}

	/**
	 * Adds a persistent {@link Layer} which will be populated immediately by the given `Coat`, with the layer containing the given value.
	 * @deprecated use {@link #addCoat(com.diffplug.common.swt.Coat.Returning)} instead
	 */
	public <T> Layer<T> add(Coat.Returning<T> coat) {
		return addCoat(coat);
	}

	/**
	 * Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer.
	 * @deprecated use {@link #setCoat(com.diffplug.common.swt.Coat.Returning)} instead
	 */
	public <T> T set(Coat.Returning<T> coat) {
		return makeTemporary(add(coat));
	}
}
