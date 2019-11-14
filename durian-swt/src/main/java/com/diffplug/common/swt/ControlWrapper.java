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
package com.diffplug.common.swt;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Wraps an SWT {@link Control} to encapsulate its API.
 * <p>
 * The traditional way to make a custom class is this: <code>class CustomControl extends {@link Composite}</code>
 * <p>
 * This has three main problems:
 * <ol>
 * <li>Users can add random widgets to your "Control" because it exposes the {@link Composite} interface.</li>
 * <li>Users can set the layout to your "Control" because it exposes the {@link Composite} interface.</li>
 * <li>Users can add random listeners to your "Control", and overridding {@link org.eclipse.swt.widgets.Widget#addListener Widget.addListener} to intercept them is a <b>very dangerous plan</b>.</li>
 * </ol>
 * <p>
 * ControlWrapper fixes this by providing an low-overhead skeleton which encapsulates the
 * SWT Control that you're using as the base of your custom control, which allows you to only
 * expose the APIs that are appropriate.
 */
public interface ControlWrapper {
	/** Sets the LayoutData for this control. */
	default void setLayoutData(Object layoutData) {
		getRootControl().setLayoutData(layoutData);
	}

	/** Returns the LayoutData for this control. */
	default Object getLayoutData() {
		return getRootControl().getLayoutData();
	}

	/** Returns the parent of this Control. */
	default Composite getParent() {
		return getRootControl().getParent();
	}

	/** Returns the parent Shell of this Control. */
	default Shell getShell() {
		return getRootControl().getShell();
	}

	/** Disposes the underlying control. */
	default void dispose() {
		getRootControl().dispose();
	}

	/** Returns true iff the underlying control is disposed. */
	default boolean isDisposed() {
		return getRootControl().isDisposed();
	}

	/**
	 * Changes the parent of the widget to be the one provided.
	 * Returns <code>true</code> if the parent is successfully changed
	 */
	default boolean setParent(Composite parent) {
		return getRootControl().setParent(parent);
	}

	/**
	 * Returns the wrapped {@link Control} (only appropriate for limited purposes!).
	 * <p>
	 * The implementor of this ControlWrapper is free to change the wrapped Control
	 * as she sees fit, and she doesn't have to tell you about it!  You shouldn't rely
	 * on this control being anything in particular.
	 * <p>
	 * You <i>can</i> rely on this Control for:
	 * <ol>
	 * <li>Managing lifetimes: `wrapped.getRootControl().addListener(SWT.Dispose, ...`</li>
	 * </ol>
	 * <p>
	 * But that's all. If you use it for something else, it's on you when it breaks.
	 */
	public Control getRootControl();

	/** Default implementation of a {@link ControlWrapper} which wraps a {@link Control}. */
	public static class AroundControl<T extends Control> implements ControlWrapper {
		/** The wrapped control. */
		protected final T wrapped;

		/** Creates a ControlWrapper which wraps the given control. */
		public AroundControl(T wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public Control getRootControl() {
			return wrapped;
		}
	}

	/** Default implementation of a {@link ControlWrapper} which wraps some other form of `ControlWrapper` with a new interface. */
	public static class AroundWrapper<T extends ControlWrapper> implements ControlWrapper {
		/** The wrapped control. */
		protected final T wrapped;

		/** Creates a ControlWrapper which wraps the given control. */
		public AroundWrapper(T wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public Control getRootControl() {
			return wrapped.getRootControl();
		}
	}

	/** Most-efficient way to transparently pass a Control to a ControlWrapper API. */
	public static <T extends Control> Transparent<T> transparent(T control) {
		return new Transparent<>(control);
	}

	public static final class Transparent<T extends Control> implements ControlWrapper {
		/** The wrapped control. */
		protected final T wrapped;

		/** Creates a ControlWrapper which wraps the given control. */
		public Transparent(T wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public T getRootControl() {
			return wrapped;
		}
	}
}
