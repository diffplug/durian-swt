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

import org.eclipse.swt.widgets.Composite;

import com.google.common.base.Preconditions;

/**
 * A function that can be "put on" a blank {@link Composite}.
 * <p>
 * An SWT Composite is a blank canvas.  As such, it's common to write functions
 * that look like {@code void initializeCmp(Composite cmp)}. In order to make higher-order
 * functionality, such as a utility for stacking {@code Composite}s, you need a way to pass
 * these kinds of functions as arguments. That's what {@code Coat} does.
 */
@FunctionalInterface
public interface Coat {
	/**
	 * Populates the given composite.
	 * Caller promises that the composite has no layout and contains no children.
	 */
	void putOn(Composite cmp);

	/** A Coat which does nothing. */
	public static Coat empty() {
		return cmp -> {};
	}

	/** A Coat which returns a handle to the content it created. */
	@FunctionalInterface
	public static interface Returning<T> {
		/**
		 * Populates the given composite, and returns a handle for communicating with the created GUI.
		 * Caller promises that the composite has no layout, and contains no children.
		 */
		T putOn(Composite cmp);

		/** Converts a non-returning Coat to a Coat.Returning. */
		public static <T> Returning<T> fromNonReturning(Coat coat, T returnValue) {
			return cmp -> {
				coat.putOn(cmp);
				return returnValue;
			};
		}
	}

	/** Throws an {@link IllegalArgumentException} iff the given {@code Composite} is ready for a coat to be put on. */
	public static void assertClean(Composite cmp) {
		Preconditions.checkArgument(cmp.getChildren().length == 0, "The composite should have no children, this had %s.", cmp.getChildren().length);
		Preconditions.checkArgument(cmp.getLayout() == null, "The composite should have no layout, this had %s.", cmp.getLayout());
	}
}
