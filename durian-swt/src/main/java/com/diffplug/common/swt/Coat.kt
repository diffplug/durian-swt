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

import com.diffplug.common.swt.Coat.Returning
import org.eclipse.swt.widgets.Composite

/**
 * A function that can be "put on" a blank [Composite].
 *
 *
 * An SWT Composite is a blank canvas.  As such, it's common to write functions
 * that look like `void initializeCmp(Composite cmp)`. In order to make higher-order
 * functionality, such as a utility for stacking `Composite`s, you need a way to pass
 * these kinds of functions as arguments. That's what `Coat` does.
 */
fun interface Coat {
    /**
     * Populates the given composite.
     * Caller promises that the composite has no layout and contains no children.
     */
    fun putOn(cmp: Composite)

    /** A Coat which returns a handle to the content it created.  */
    fun interface Returning<T> {
        /**
         * Populates the given composite, and returns a handle for communicating with the created GUI.
         * Caller promises that the composite has no layout, and contains no children.
         */
        fun putOn(cmp: Composite): T

        companion object {
            /** Converts a non-returning Coat to a Coat.Returning.  */
            fun <T> fromNonReturning(coat: Coat, returnValue: T): Returning<T> {
                return Returning { cmp: Composite ->
                    coat.putOn(cmp)
                    returnValue
                }
            }
        }
    }

    companion object {
        /** A Coat which does nothing.  */
		@JvmStatic
		fun empty(): Coat {
            return EMPTY
        }

        val EMPTY = Coat { cmp: Composite -> }
    }
}