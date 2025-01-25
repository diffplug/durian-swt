/*
 * Copyright (C) 2020-2025 DiffPlug
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

import static java.util.Objects.requireNonNull;

import com.diffplug.common.debug.JuxtaProfiler;
import com.diffplug.common.debug.LapTimer;
import com.diffplug.common.rx.Rx;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.MutableSharedFlow;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.junit.Test;

/** Some simple profiles to make sure tha danger is worth it. */
public class SwtExecProfile {
	/** MUST BE 20 if you want good results, but 1 for quick CI tests. */
	static final int NUM_TRIALS = 20;

	/**
	 * For 1,000,000 events, the overhead is:
	 *
	 * control:              20ms
	 * completelyUnchecked:  20ms
	 * sameThread:           20ms
	 * swtOnly:              120ms
	 * immediateWithSync:    450ms
	 * immediateWithoutSync: 340ms
	 *
	 * So, what does this tell us?  First off, for SwtExec.async(),
	 * SwtExec.blocking(), and SwtExec.immediate(), having this:
	 *
	 * ```
	 * if (!display.isDisposed()) {
	 *     if (Thread.currentThread() == display.getThread()) {
	 *     
	 *   vs
	 *
	 * if (Thread.currentThread() == swtThread) {
	 * ```
	 *
	 * Results in .45ms vs .34ms per 1,000 events (30% speed increase).
	 *
	 * The second thing it tells us is that SwtScheduler vs
	 * Rx.sameThreadExecutor() is huge, about 6x slower.  There must
	 * be some optimizations that check for the built-in
	 * Schedulers.immediate().
	 */
	@Test
	public void testImmediatePerformance() {
		InteractiveTest.testCoat("", cmp -> {
			SwtExecProfiler profiler = new SwtExecProfiler();
			Display display = cmp.getDisplay();
			Thread swtThread = display.getThread();
			profiler.addSameThreadExecutor("completelyUnchecked", runnable -> {
				runnable.run();
			});
			profiler.addSwtExec("sameThread", SwtExec.sameThread());
			profiler.addSwtExec("sameThreadSwtOnly", SwtExec.swtOnly());
			profiler.addSwtScheduler("immediateWithSync", runnable -> {
				requireNonNull(runnable);
				if (!display.isDisposed()) {
					if (Thread.currentThread() == display.getThread()) {
						runnable.run();
					} else {
						display.asyncExec(runnable);
					}
				} else {
					throw new RejectedExecutionException();
				}
			});
			profiler.addSwtScheduler("immediateWithoutSync", runnable -> {
				requireNonNull(runnable);
				if (Thread.currentThread() == swtThread) {
					runnable.run();
				} else {
					display.asyncExec(runnable);
				}
			});

			profiler.run(cmp);
			InteractiveTest.closeAndPass(cmp);
		});
	}

	static class SwtExecProfiler {
		static final int EVENTS_TO_PUSH = 1_000_000;

		Map<String, SwtExec> toProfile = new LinkedHashMap<>();

		public void addSwtScheduler(String name, Consumer<Runnable> onRun) {
			addSwtExec(name, new SwtExec() {
				@Override
				public void execute(Runnable runnable) {
					onRun.accept(runnable);
				}
			});
		}

		public void addSameThreadExecutor(String name, Consumer<Runnable> onRun) {
			addSwtExec(name, new SwtExec(unused -> Rx.sameThreadExecutor()) {
				@Override
				public void execute(Runnable runnable) {
					onRun.accept(runnable);
				}
			});
		}

		public void addSwtExec(String name, SwtExec underTest) {
			toProfile.put(name, underTest);
		}

		public void run(Widget guard) {
			JuxtaProfiler profiler = new JuxtaProfiler();
			profiler.addTestNanoWrap2Sec("control", () -> {
				MutableSharedFlow<Integer> subject = Rx.createEmitFlow();
				drain(subject);
			});
			toProfile.forEach((name, underTest) -> {
				profiler.addTest(name, new JuxtaProfiler.InitTimedCleanup(LapTimer.createNanoWrap2Sec()) {
					MutableSharedFlow<Integer> subject;
					Job sub;

					@Override
					protected void init() throws Throwable {
						subject = Rx.createEmitFlow();
						sub = underTest.guardOn(guard).subscribeDisposable(subject, val -> {});
					}

					@Override
					protected void timed() throws Throwable {
						drain(subject);
					}

					@Override
					protected void cleanup() throws Throwable {
						sub.cancel(null);
						subject = null;
						sub = null;
					}
				});
			});
			profiler.runRandomTrials(NUM_TRIALS);
		}

		private static void drain(MutableSharedFlow<Integer> subject) {
			for (int i = 0; i < EVENTS_TO_PUSH; ++i) {
				Rx.emit(subject, 0);
			}
		}
	}

	/**
	 * Thread.currentThread() appears to be instantaneous,
	 * according to this benchmark.  I'm not convinced I'm doing it right...
	 */
	@Test
	public void testThreadCurrentThreadPerformance() {
		int NUM_PER_LOOP = 1_000_000_000;

		Thread currentThread = null;
		JuxtaProfiler profiler = new JuxtaProfiler();
		profiler.addTestMs("control", () -> {
			for (int i = 0; i < NUM_PER_LOOP; ++i) {
				if (currentThread != null) {
					break;
				}
			}
		});
		profiler.addTestMs("Thread.currentThread()", () -> {
			for (int i = 0; i < NUM_PER_LOOP; ++i) {
				if (Thread.currentThread() == null) {
					break;
				}
			}
		});
		profiler.runRandomTrials(NUM_TRIALS);
	}
}
