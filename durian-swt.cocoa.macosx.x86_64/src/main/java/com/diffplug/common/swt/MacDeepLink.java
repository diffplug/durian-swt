/*
 * Copyright (C) 2025 DiffPlug
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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/**
 * - immediately on app startup, call `MacDeepLink.startCapturingBeforeSwt()`
 * - once SWT has initialized, call `MacDeepLink.swtHasInitializedBeginReceiving(Consumer<String>)`
 *   - all urls which were captured before SWT initialized will be passed immediately (on the SWT thread)
 *
 * That's all! Don't do anything else.
 */
public class MacDeepLink {
	/**
	 * state transitions are:
	 * - `null` on startup
	 * - `startCapturingBeforeSwt()` transitions to an `ArrayList<String>`, backlog urls get added to it
	 * - `swtHasInitializedBeginReceiving()` transitions to a `Consumer<String>`, all new urls go there
	 */
	private static final AtomicReference<@Nullable Object> state = new AtomicReference<>();

	public static void startCapturingBeforeSwt() {
		String libPath = System.getProperty("durian-swt.library.path");
		if (libPath != null) {
			System.load(libPath + "/durian-swt-natives/DeepLinkBridge.dylib");
		} else {
			throw new IllegalArgumentException("You need to set 'durian-swt.library.path'");
		}

		var was = state.getAndSet(new ArrayList<>());
		Preconditions.checkArgument(was == null, "`startCapturingBeforeSwt() should be called first`");
		nativeBeforeSwt();
	}

	public static void swtHasInitializedBeginReceiving(Consumer<String> handler) {
		SwtMisc.assertUI();
		var was = state.getAndSet(handler);
		Preconditions.checkArgument(was instanceof ArrayList<?>, "Call `applicationStartBeforeSwt()` first.");

		var backlog = (ArrayList<String>) was;
		backlog.forEach(handler);

		nativeAfterSwt();
	}

	// Native method declarations - implemented in DeepLinkBridge.m
	private static native void nativeBeforeSwt();

	private static native void nativeAfterSwt();

	/**
	 * Called from native code when a URL is received.
	 * This method is invoked on various threads by the native code.
	 *
	 * @param url The URL string received from the operating system
	 */
	public static void __internal_deliverUrl(String url) {
		var was = state.get();
		if (was instanceof Consumer) {
			((Consumer<String>) was).accept(url);
		} else if (was instanceof ArrayList<?>) {
			((ArrayList<String>) was).add(url);
		} else {
			throw new IllegalStateException("Expected Consumer or ArrayList, was " + was);
		}
	}
}
