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

import com.google.common.collect.ImmutableList;

import com.diffplug.common.base.Unhandled;

/** Utility for detecting OS type. */
public enum OS {
	WIN_x64, WIN_x86, LINUX_x64, LINUX_x86, MAC_x64;

	public boolean isWindows() {
		return this == WIN_x64 || this == WIN_x86;
	}

	public boolean isLinux() {
		return this == LINUX_x64 || this == LINUX_x86;
	}

	public boolean isMac() {
		return this == MAC_x64;
	}

	public boolean isMacOrLinux() {
		return isMac() || isLinux();
	}

	/** Returns the appropriate value depending on the OS. */
	public <T> T winMacLinux(T win, T mac, T linux) {
		if (isWindows()) {
			return win;
		} else if (isMac()) {
			return mac;
		} else if (isLinux()) {
			return linux;
		} else {
			throw OS.unsupportedException(this);
		}
	}

	/** Returns the architecture of the given operating system. */
	public Arch getArch() {
		switch (this) {
		case WIN_x64:
		case LINUX_x64:
		case MAC_x64:
			return Arch.x64;
		case WIN_x86:
		case LINUX_x86:
			return Arch.x86;
		default:
			throw unsupportedException(this);
		}
	}

	/** Returns the native OS. e.g. running 32-bit JVM on 64-bit Windows returns OS.WIN_64. */
	public static OS getNative() {
		return NATIVE_OS;
	}

	/** Returns the running OS.  e.g. running 32-bit JVM on 64-bit Windows returns OS.WIN_32. */
	public static OS getRunning() {
		return RUNNING_OS;
	}

	private static final OS NATIVE_OS = calculateNative();

	/** Calculates the native OS. */
	private static OS calculateNative() {
		String OS = System.getProperty("os.name").toLowerCase();
		boolean isWin = OS.contains("win");
		boolean isMac = OS.contains("mac");
		boolean isLinux = ImmutableList.of("nix", "nux", "aix").stream().anyMatch(OS::contains);

		if (isMac) {
			return MAC_x64;
		} else if (isWin) {
			boolean is64bit = System.getenv("ProgramFiles(x86)") != null;
			return is64bit ? WIN_x64 : WIN_x86;
		} else if (isLinux) {
			String arch = System.getProperty("os.arch");
			switch (arch) {
			case "x86":
				return LINUX_x86;
			case "x86_64":
			case "amd64":
				return LINUX_x64;
			default:
				throw new UnsupportedOperationException("Unknown arch '" + arch + "'.");
			}
		} else {
			throw new UnsupportedOperationException("Unknown os '" + OS + "'.");
		}
	}

	private static final OS RUNNING_OS = calculateRunning();

	/** Calculates the running OS. */
	private static OS calculateRunning() {
		Arch runningArch = runningJvm();
		return NATIVE_OS.winMacLinux(runningArch.x86x64(OS.WIN_x86, OS.WIN_x64), OS.MAC_x64,
				runningArch.x86x64(OS.LINUX_x86, OS.LINUX_x64));
	}

	/** Returns the arch of the currently running JVM. */
	private static Arch runningJvm() {
		String sunArchDataModel = System.getProperty("sun.arch.data.model");
		switch (sunArchDataModel) {
		case "32":
			return Arch.x86;
		case "64":
			return Arch.x64;
		default:
			throw Unhandled.stringException(sunArchDataModel);
		}
	}

	/** Returns an UnsupportedOperationException for the given OS. */
	public static UnsupportedOperationException unsupportedException(OS os) {
		return new UnsupportedOperationException("Operating system '" + os + "' is not supported.");
	}
}
