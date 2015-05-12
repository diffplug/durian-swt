/**
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

import java.util.Objects;

import org.eclipse.swt.widgets.Composite;

/** For populating composites. */
@FunctionalInterface
public interface Coat {
	/**
	 * Populates the given composite.
	 * 
	 * Caller promises that the composite has no layout and contains no children.
	 */
	void putOn(Composite cmp);

	/** Coat which does nothing. */
	public static Coat empty() {
		return cmp -> {};
	}

	/** A Coat which returns a handle to the content it created. */
	@FunctionalInterface
	public static interface Returning<T> {
		/**
		 * Populates the given composite, and returns a handle for communicating with the created GUI.
		 * 
		 * Caller promises that the composite has no layout, and contains no children.
		 * Caller also promises isActive will be kept up-to-date.
		 */
		T putOn(Composite cmp);

		/** Converts a non-returning Coat to a Coat.Returning. */
		public static <T extends Coat> Returning<T> fromNonReturning(T client) {
			return cmp -> {
				client.putOn(cmp);
				return client;
			};
		}
	}

	/** A Coat with the ability to be hidden, redisplayed, and deduplicated. */
	public interface Reusable<T> extends Returning<T> {
		/** The key used to deuplicate this Coat. Cannot be null. */
		Object dedupKey();

		/** The class of T. Required to make sure that handles are legit. */
		Class<T> classOfT();

		/** Transforms a Coat into a CmpClientDeluxe. */
		public static <T> Reusable<T> fromReturning(Object dedupKey, Class<T> classOfT, Returning<T> client) {
			Objects.requireNonNull(dedupKey);
			Objects.requireNonNull(classOfT);
			Objects.requireNonNull(client);
			return new Reusable<T>() {
				@Override
				public Object dedupKey() {
					return dedupKey;
				}

				@Override
				public Class<T> classOfT() {
					return classOfT;
				}

				@Override
				public T putOn(Composite cmp) {
					return client.putOn(cmp);
				}
			};
		}
	}

	/** Transforms a Coat into a Coat.Reusable which promises to never be reused. */
	public static Reusable<Coat> asNeverReusedReusable(Coat client) {
		return Reusable.fromReturning(new Object(), Coat.class, cmp -> {
			client.putOn(cmp);
			return client;
		});
	}
}
