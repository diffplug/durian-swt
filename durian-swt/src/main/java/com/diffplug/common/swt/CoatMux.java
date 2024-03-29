/*
 * Copyright (C) 2020-2022 DiffPlug
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


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.rx.Chit;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.rx.RxGetter;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** A widget that switches between multiple `Coat`s. */
@SwtThread
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
			wrapped.layout();
			wrapped.requestLayout();
		});
	}

	/** The current layer (if any). */
	public RxGetter<Optional<Layer<?>>> rxCurrent() {
		return currentLayer;
	}

	/** Sets the mux to be empty. */
	public void setEmpty() {
		currentLayer.set(Optional.empty());
	}

	/** The Control at the top of the stack (possibly null). */
	public Control getTopControl() {
		return stack.topControl;
	}

	/** Represents a persistent layer within this `CoatMux`. It can be shown, hidden, and disposed. */
	public class Layer<T> {
		final Control control;
		final T handle;

		private Layer(Control control, T handle) {
			this.control = control;
			this.handle = handle;
		}

		/** {@link RxGetter} which keeps track of whether this `Layer` is currently on top. */
		public RxGetter<Boolean> rxOnTop() {
			return currentLayer.map(opt -> opt.isPresent() && opt.get() == this);
		}

		/** The control at the root of this layer. */
		public Control getControl() {
			return control;
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
			if (rxOnTop().get()) {
				currentLayer.set(Optional.empty());
			}
			SwtExec.immediate().execute(control::dispose);
		}

		public Chit chit() {
			return SwtRx.chit(control);
		}
	}

	/** Adds a persistent {@link Layer} which will be populated immediately by the given `Coat`, using `value` as the key. */
	public <T> Layer<T> addCoat(Coat coat, @Nullable T value) {
		return addCoat(Coat.Returning.Companion.fromNonReturning(coat, value));
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
		SwtExec.immediate().guardOn(layer.control).subscribe(layer.rxOnTop(), isCurrent -> {
			if (!isCurrent) {
				layer.dispose();
			}
		});
		// return the handle that the coat created
		return layer.handle;
	}

	/** Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer. */
	public void setCoat(Coat coat) {
		setCoatReturning(Coat.Returning.Companion.fromNonReturning(coat, null));
	}

	/** Sets the current content of this `CoatMux`, gets disposed as soon as anything else becomes the top layer. */
	public <T> T setCoatReturning(Coat.Returning<T> coat) {
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
}
