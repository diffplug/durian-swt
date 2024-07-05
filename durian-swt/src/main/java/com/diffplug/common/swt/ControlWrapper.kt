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
package com.diffplug.common.swt

import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell


/**
 * Wraps an SWT [Control] to encapsulate its API.
 *
 *
 * The traditional way to make a custom class is this: `class CustomControl extends [Composite]`
 *
 *
 * This has three main problems:
 *
 *  1. Users can add random widgets to your "Control" because it exposes the [Composite] interface.
 *  1. Users can set the layout to your "Control" because it exposes the [Composite] interface.
 *  1. Users can add random listeners to your "Control", and overridding [Widget.addListener][org.eclipse.swt.widgets.Widget.addListener] to intercept them is a **very dangerous plan**.
 *
 *
 *
 * ControlWrapper fixes this by providing an low-overhead skeleton which encapsulates the
 * SWT Control that you're using as the base of your custom control, which allows you to only
 * expose the APIs that are appropriate.
 */
interface ControlWrapper {
    var layoutData: Any?
        /** Returns the LayoutData for this control.  */
        get() = rootControl.layoutData
        /** Sets the LayoutData for this control.  */
        set(layoutData) {
            rootControl.layoutData = layoutData
        }

    val parent: Composite
        /** Returns the parent of this Control.  */
        get() = rootControl.parent

    val shell: Shell
        /** Returns the parent Shell of this Control.  */
        get() = rootControl.shell

    /** Disposes the underlying control.  */
    fun dispose() {
        rootControl.dispose()
    }

    val isDisposed: Boolean
        /** Returns true iff the underlying control is disposed.  */
        get() = rootControl.isDisposed

    /**
     * Changes the parent of the widget to be the one provided.
     * Returns `true` if the parent is successfully changed
     */
    fun setParent(parent: Composite): Boolean {
        return rootControl.setParent(parent)
    }

    /**
     * Returns the wrapped [Control] (only appropriate for limited purposes!).
     *
     *
     * The implementor of this ControlWrapper is free to change the wrapped Control
     * as she sees fit, and she doesn't have to tell you about it!  You shouldn't rely
     * on this control being anything in particular.
     *
     *
     * You *can* rely on this Control for:
     *
     *  1. Managing lifetimes: `wrapped.getRootControl().addListener(SWT.Dispose, ...`
     *
     *
     *
     * But that's all. If you use it for something else, it's on you when it breaks.
     */
	val rootControl: Control

    /** Default implementation of a [ControlWrapper] which wraps a [Control].  */
    open class AroundControl<T : Control>
    /** Creates a ControlWrapper which wraps the given control.  */(
        /** The wrapped control.  */
        @JvmField protected val wrapped: T
    ) : ControlWrapper {
        override val rootControl: T
                get() = wrapped
    }

    /** Default implementation of a [ControlWrapper] which wraps some other form of `ControlWrapper` with a new interface.  */
    open class AroundWrapper<T : ControlWrapper>(
        @JvmField
        protected val wrapped: T
    ) : ControlWrapper {
        override val rootControl: Control
            get() = wrapped.rootControl
    }

    /** Creates a ControlWrapper which wraps the given control.  */
    class Transparent<T : Control>(
        override val rootControl: T
    ) : ControlWrapper

    companion object {
        /** Most-efficient way to transparently pass a Control to a ControlWrapper API.  */
        @JvmStatic
        fun <T : Control> transparent(control: T): Transparent<T> {
            return Transparent(control)
        }
    }
}
