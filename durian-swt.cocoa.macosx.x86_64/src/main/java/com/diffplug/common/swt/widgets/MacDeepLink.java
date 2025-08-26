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
package com.diffplug.common.swt.widgets;

import com.diffplug.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class MacDeepLink {
	private static volatile @Nullable Consumer<String> urlHandler;
	private static final AtomicReference<@Nullable List<String>> backlogUrls = new AtomicReference<>();

	static {
		// Load the native library - try multiple strategies
		String libPath = System.getProperty("durian-swt.library.path");
		if (libPath != null) {
			System.load(libPath + "/durian-swt-natives/DeepLinkBridge.dylib");
		} else {
			throw new IllegalArgumentException("You need to set 'durian-swt.library.path'");
		}
	}

	public static void setURLHandler(Consumer<String> handler) {
		Preconditions.checkArgument(urlHandler == null, "URL handler can only be set once");
		urlHandler = handler;
	}

	public static void applicationStartBeforeSwt() {
		Preconditions.checkArgument(urlHandler != null, "Call `setURLHandler()` first");
		backlogUrls.set(new ArrayList<>());
		nativeBeforeSwt();
	}

	public static void applicationStartAfterSwt() {
		Preconditions.checkArgument(backlogUrls.get() != null, "Call `applicationStartBeforeSwt()` first.");
		nativeAfterSwt();
		var accumulatedBacklog = backlogUrls.getAndSet(null);
		accumulatedBacklog.forEach(urlHandler);
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
	public static void deliverURL(String url) {
		var backlog = backlogUrls.get();
		if (backlog != null) {
			backlog.add(url);
		} else {
			urlHandler.accept(url);
		}
	}
}
