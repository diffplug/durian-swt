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
package com.diffplug.common.swt.os;

/** Models the windowing systems that we currently support. */
public enum WS {
	WIN, COCOA, GTK;

	public boolean isWin() {
		return this == WIN;
	}

	public boolean isCocoa() {
		return this == COCOA;
	}

	public boolean isGTK() {
		return this == GTK;
	}

	public <T> T winCocoaGtk(T win, T cocoa, T gtk) {
		// @formatter:off
		switch (this) {
		case WIN:	return win;
		case COCOA:	return cocoa;
		case GTK:	return gtk;
		default: throw unsupportedException(this);
		}
		// @formatter:on
	}

	private static final WS RUNNING_WS = OS.getRunning().winMacLinux(WIN, COCOA, GTK);

	public static WS getRunning() {
		return RUNNING_WS;
	}

	/** Returns an UnsupportedOperationException for the given arch. */
	public static UnsupportedOperationException unsupportedException(WS ws) {
		return new UnsupportedOperationException("Window system '" + ws + "' is unsupported.");
	}
}
