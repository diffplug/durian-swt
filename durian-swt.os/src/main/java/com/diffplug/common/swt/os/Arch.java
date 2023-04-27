/*
 * Copyright (C) 2020-2023 DiffPlug
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
package com.diffplug.common.swt.os;

/** Enum for handling different processor architectures supported by SWT. */
public enum Arch {
	x86, x64, arm64, unknown;

	/** Returns the appropriate value depending on the arch. */
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

	/** Returns the appropriate value depending on the arch. */
	public <T> T x64arm64(T val64, T arm64) {
		switch (this) {
		case x64:
			return val64;
		case arm64:
			return arm64;
		default:
			throw unsupportedException(this);
		}
	}

	/** Returns the appropriate value depending on the arch. */
	public <T> T x86x64arm64(T val86, T val64, T arm64) {
		switch (this) {
		case x86:
			return val86;
		case x64:
			return val64;
		case arm64:
			return arm64;
		default:
			throw unsupportedException(this);
		}
	}

	/** Returns the appropriate value depending on the arch. */
	public <T> T x86x64arm64unknown(T val86, T val64, T arm64, T unknown) {
		switch (this) {
		case x86:
			return val86;
		case x64:
			return val64;
		case arm64:
			return arm64;
		case unknown:
			return unknown;
		default:
			throw unsupportedException(this);
		}
	}

	/** Returns the Arch for the native platform: 32-bit JVM on 64-bit Windows returns Arch.x64. */
	public static Arch getNative() {
		return OS.getNative().getArch();
	}

	/** Returns the Arch for the native platform: 32-bit JVM on 64-bit Windows returns Arch.x86. */
	public static Arch getRunning() {
		return OS.getRunning().getArch();
	}

	/** Returns an UnsupportedOperationException for the given arch. */
	public static UnsupportedOperationException unsupportedException(Arch arch) {
		return new UnsupportedOperationException("Arch '" + arch + "' is unsupported.");
	}
}
