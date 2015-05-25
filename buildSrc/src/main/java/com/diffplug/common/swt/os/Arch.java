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
package com.diffplug.common.swt.os;

/** Enum for handling different processor architectures. Just x86 and x64 for the foreseeable future. */
public enum Arch {
	x86, x64;

	/** Pattern-match style thingy. */
	public <T> T x86x64(T val86, T val64) {
		switch (this) {
		case x86:
			return val86;
		case x64:
			return val64;
		default:
			throw unsupportedException(this);
		}
	}

	/** Returns an UnsupportedOperationException for the given arch. */
	public static UnsupportedOperationException unsupportedException(Arch arch) {
		return new UnsupportedOperationException("Arch '" + arch + "' is unsupported.");
	}
}
