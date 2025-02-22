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

import com.diffplug.common.base.Box.Nullable;
import com.diffplug.common.primitives.Ints;
import com.diffplug.common.rx.Chit;
import com.diffplug.common.rx.GuardedExecutor;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxExecutor;
import com.diffplug.common.rx.RxSubscriber;
import com.diffplug.common.util.concurrent.MoreExecutors;
import com.diffplug.common.util.concurrent.Runnables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.MainCoroutineDispatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Executor Executors} which execute on the SWT UI thread.
 *
 * There are two primary kinds of `SwtExec`:
 *
 * - {@link #async()} -> performs actions using {@link Display#asyncExec(Runnable) Display.asyncExec}
 * - {@link #immediate()} -> performs actions immediately if called from a UI thread, otherwise delegates to `asyncExec`.
 *
 * In addition to the standard executor methods, each `SwtExec` also has a method {@link #guardOn(ControlWrapper) guardOn}, which
 * returns a {@link Guarded} instance - the cure for "Widget is disposed" errors.  `Guarded` is an {@link Executor} and {@link RxSubscriber} which
 * stops running tasks and cancels all subscriptions and futures when the guard widget is disposed.
 *
 * ```java
 * SwtExec.immediate().guardOn(myWidget).subscribe(someFuture, value -> myWidget.setContentsTo(value));
 * ```
 *
 * In the example above, if the widget is disposed before the future completes, that's fine!  No "widget is disposed" errors.
 *
 * {@link #blocking()} is similar to `async()` and `immediate()`, but it doesn't support `guard` - it's just a simple `Executor`.
 * It performs actions immediately if called from a UI thread, else delegates to the blocking {@link Display#syncExec}.  It also
 * has the {@link Blocking#get(Supplier)} method, which allows you to easily get a value using a function which must be called
 * on the SWT thread.
 *
 * In the rare scenario where you need higher performance, it is possible to get similar behavior as {@link #immediate()} but with
 * less overhead (and safety) in {@link #swtOnly()} and {@link SwtExec#sameThread()}.  It is very rarely worth this sacrifice.
 */
public class SwtExec extends AbstractExecutorService implements ScheduledExecutorService, RxExecutor.Has {
	/** Returns true iff called from the UI thread. */
	public static boolean isRunningOnUI() {
		initSwtThreads();
		return Thread.currentThread() == swtThread;
	}

	private static Display display;
	private static Thread swtThread;

	@SuppressFBWarnings(value = {"LI_LAZY_INIT_STATIC", "LI_LAZY_INIT_UPDATE_STATIC"}, justification = "This race condition is fine, see comment in SwtExec.blocking()")
	static void initSwtThreads() {
		if (display == null) {
			display = Display.getDefault();
			swtThread = display.getThread();
		}
	}

	/** Global executor for async. */
	private static SwtExec async;

	/**
	 * Returns an "async" SwtExecutor.
	 * <p>
	 * When `execute(Runnable)` is called, the `Runnable` will be passed to {@link Display#asyncExec Display.asyncExec}.
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, see comment in SwtExec.blocking()")
	public static SwtExec async() {
		if (async == null) {
			async = new SwtExec();
		}
		return async;
	}

	/** Global executor for immediate. */
	private static SwtExec immediate;

	/**
	 * Returns an "immediate" SwtExecutor.
	 *
	 * - When `execute(Runnable)` is called from the SWT thread, the `Runnable` will be executed immediately.
	 * - Else, the `Runnable` will be passed to {@link Display#asyncExec(Runnable) Display.asyncExec}.
	 *
	 * In the rare case that `immediate()` only ever receives events on the SWT thread, there are faster options:
	 *
	 * - {@link #swtOnly} is about 3x faster, and will throw an error if you call it from somewhere besides an SWT thread.
	 * - {@link #sameThread} is about 15x faster, and will not throw an error if you call it from somewhere besides an SWT thread (but your callback probably will).
	 *
	 * It is very rare that sacrificing the safety of `immediate()` is worth it.  Here is the approximate throughput
	 * of the three options on a Win 10, i7-2630QM machine.
	 *
	 * - `immediate()`  - 2.9 million events per second
	 * - `swtOnly()`    - 8.3 million events per second
	 * - `sameThread()` - 50 million events per second
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, see comment in SwtExec.blocking()")
	public static SwtExec immediate() {
		if (immediate == null) {
			immediate = new SwtExec() {
				@Override
				public void execute(Runnable runnable) {
					if (Thread.currentThread() == swtThread) {
						runnable.run();
					} else {
						requireNonNull(runnable);
						display.asyncExec(runnable);
					}
				}
			};
		}
		return immediate;
	}

	/** Global executor for blocking. */
	private static Blocking blocking;

	/**
	 * Returns a "blocking" Executor for the SWT thread.
	 * <ul>
	 * <li>When `execute(Runnable)` is called from the SWT thread, the `Runnable` will be executed immediately.</li>
	 * <li>Else, the `Runnable` will be passed to {@link Display#syncExec Display.syncExec}.</li>
	 * </ul>
	 * This instance also has a blocking {@link Blocking#get get()} method for doing a get in the UI thread.
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, see comment in SwtExec.blocking()")
	public static Blocking blocking() {
		// There is an acceptable race condition here - blocking might get set multiple times.
		// This would happen if multiple threads called blocking() at the same time
		// during initialization, and this is likely to actually happen in practice.
		//
		// It is important for this method to be fast, so it's better to accept
		// that blocking() might return different instances (which each have the
		// same behavior), rather than to incur the cost of some type of synchronization.
		if (blocking == null) {
			blocking = new Blocking();
		}
		return blocking;
	}

	/**
	 * An Executor (obtained via {@link SwtExec#blocking()}) which adds a blocking {@link Blocking#get get()} method.
	 * <ul>
	 * <li>When `execute(Runnable)` is called from the SWT thread, the `Runnable` will be executed immediately.</li>
	 * <li>Else, the `Runnable` will be passed to {@link Display#syncExec Display.syncExec}.</li>
	 * </ul>
	 * @see SwtExec#blocking
	 */
	public static class Blocking implements Executor {
		final Display display;
		final Thread swtThread;

		private Blocking() {
			display = Display.getDefault();
			swtThread = display.getThread();
		}

		/** Returns an executor which will only execute if the given guard hasn't been disposed. */
		public Executor guardOn(Control guard) {
			Objects.requireNonNull(guard);
			return runnable -> {
				execute(() -> {
					if (!guard.isDisposed()) {
						runnable.run();
					}
				});
			};
		}

		/** Returns an executor which will only execute if the given guard hasn't been disposed. */
		public Executor guardOn(ControlWrapper guard) {
			return guardOn(guard.getRootControl());
		}

		@Override
		public void execute(Runnable runnable) {
			if (Thread.currentThread() == swtThread) {
				runnable.run();
			} else {
				requireNonNull(runnable);
				display.syncExec(runnable);
			}
		}

		/**
		 * Performs a blocking get in the UI thread.
		 *
		 * @param supplier will be executed in the UI thread.
		 * @return the value which was returned by supplier.
		 */
		public <T> T get(Supplier<T> supplier) {
			if (Thread.currentThread() == swtThread) {
				return supplier.get();
			} else {
				Nullable<T> holder = Nullable.ofVolatileNull();
				display.syncExec(() -> holder.set(supplier.get()));
				return holder.get();
			}
		}
	}

	/** Executes the given runnable in the UI thread after the given delay. */
	public static void timerExec(int ms, Runnable runnable) {
		initSwtThreads();
		display.timerExec(ms, runnable);
	}

	/** Returns an API for performing actions which are guarded on the given Widget. */
	public Guarded guardOn(Chit chit) {
		return new Guarded(this, chit);
	}

	/** Returns an API for performing actions which are guarded on the given Widget. */
	public Guarded guardOn(Widget widget) {
		return guardOn(SwtRx.chit(widget));
	}

	/** Returns an API for performing actions which are guarded on the given ControlWrapper. */
	public Guarded guardOn(ControlWrapper wrapper) {
		return guardOn(wrapper.getRootControl());
	}

	/**
	 * {@link Executor} and {@link com.diffplug.common.rx.Rx} for conducting actions which are guarded on an SWT widget.
	 * Obtained via {@link SwtExec#guardOn(Widget) SwtExec.guardOn(Widget)} or {@link SwtExec#guardOn(ControlWrapper) SwtExec.guardOn(ControlWrapper)}.
	 * <p>
	 * <pre>
	 * SwtExec.Guarded guarded = SwtExec.immediate().guardOn(textBox);
	 * // guaranteed to not cause "Widget is disposed" errors
	 * guarded.subscribe(serverResponse, txt -> textBox.setText(text));
	 * </pre>
	 * @see com.diffplug.common.rx.Rx
	 */
	public static class Guarded extends GuardedExecutor {
		private Guarded(SwtExec parent, Chit chit) {
			super(parent.rxExecutor, chit);
		}

		/** Runs the given runnable after the given delay iff the guard widget is not disposed. */
		public void timerExec(int delayMs, Runnable runnable) {
			display.timerExec(delayMs, getGuard().guard(runnable));
		}

		/**
		 * Same as {@link SwtExec#schedule(Runnable, long, TimeUnit)} but automatically
		 * cancels when the guard is disposed.  Identical behavior for `immediate()`,
		 * `async()`, etc.
		 */
		public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
			return hook(SwtExec.async().schedule(getGuard().guard(command), delay, unit));
		}

		/**
		 * Same as {@link SwtExec#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} but automatically
		 * cancels when the guard is disposed.  Identical behavior for `immediate()`,
		 * `async()`, etc.
		 */
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
			return hook(SwtExec.async().scheduleWithFixedDelay(getGuard().guard(command), initialDelay, delay, unit));
		}

		/**
		 * Same as {@link SwtExec#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} but automatically
		 * cancels when the guard is disposed.  Identical behavior for `immediate()`,
		 * `async()`, etc.
		 */
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long delay, TimeUnit unit) {
			return hook(SwtExec.async().scheduleAtFixedRate(getGuard().guard(command), initialDelay, delay, unit));
		}

		private ScheduledFuture<?> hook(ScheduledFuture<?> future) {
			getGuard().runWhenDisposed(() -> future.cancel(true));
			return future;
		}
	}

	protected final RxExecutor rxExecutor;

	/** Returns an instance of {@link com.diffplug.common.rx.RxExecutor}. */
	@Override
	public RxExecutor getRxExecutor() {
		return rxExecutor;
	}

	SwtExec() {
		this(exec -> Rx.callbackOn(exec, new SwtDispatcher(exec)));
	}

	SwtExec(Function<SwtExec, RxExecutor> rxExecutorCreator) {
		initSwtThreads();
		this.rxExecutor = rxExecutorCreator.apply(this);
	}

	//////////////
	// Executor //
	//////////////
	/**
	 * Executes the given command at some time in the future. The command
	 * may execute in a new thread, in a pooled thread, or in the calling
	 * thread, at the discretion of the <tt>Executor</tt> implementation.
	 *
	 * @param runnable
	 *            the runnable task
	 * @throws RejectedExecutionException
	 *             if this task cannot be
	 *             accepted for execution.
	 * @throws NullPointerException
	 *             if command is null
	 */
	@Override
	public void execute(Runnable runnable) {
		requireNonNull(runnable);
		display.asyncExec(runnable);
	}

	////////////////////////////////////////////////////////
	// ExecutorService shutdown stuff (all unimplemented) //
	////////////////////////////////////////////////////////
	/**
	 * Initiates an orderly shutdown in which previously submitted
	 * tasks are executed, but no new tasks will be accepted.
	 * Invocation has no additional effect if already shut down.
	 *
	 * @throws SecurityException
	 *             if a security manager exists and
	 *             shutting down this ExecutorService may manipulate
	 *             threads that the caller is not permitted to modify
	 *             because it does not hold {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
	 *             or the security manager's <tt>checkAccess</tt> method
	 *             denies access.
	 */
	@Deprecated
	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Attempts to stop all actively executing tasks, halts the
	 * processing of waiting tasks, and returns a list of the tasks that were
	 * awaiting execution.
	 *
	 * <p>
	 * There are no guarantees beyond best-effort attempts to stop processing actively executing tasks. For example, typical implementations will cancel via {@link Thread#interrupt}, so any task that fails to respond to interrupts may never terminate.
	 *
	 * @return list of tasks that never commenced execution
	 * @throws SecurityException
	 *             if a security manager exists and
	 *             shutting down this ExecutorService may manipulate
	 *             threads that the caller is not permitted to modify
	 *             because it does not hold {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
	 *             or the security manager's <tt>checkAccess</tt> method
	 *             denies access.
	 */
	@Deprecated
	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns <tt>true</tt> if this executor has been shut down.
	 *
	 * @return <tt>true</tt> if this executor has been shut down
	 */
	@Deprecated
	@Override
	public boolean isShutdown() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns <tt>true</tt> if all tasks have completed following shut down.
	 * Note that <tt>isTerminated</tt> is never <tt>true</tt> unless
	 * either <tt>shutdown</tt> or <tt>shutdownNow</tt> was called first.
	 *
	 * @return <tt>true</tt> if all tasks have completed following shut down
	 */
	@Deprecated
	@Override
	public boolean isTerminated() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown
	 * request, or the timeout occurs, or the current thread is
	 * interrupted, whichever happens first.
	 *
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return <tt>true</tt> if this executor terminated and <tt>false</tt> if the timeout elapsed before termination
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 */
	@Deprecated
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////
	// ScheduledExecutorService //
	//////////////////////////////
	/**
	 * Creates and executes a one-shot action that becomes enabled
	 * after the given delay.
	 *
	 * @param command
	 *            the task to execute
	 * @param delay
	 *            the time from now to delay execution
	 * @param unit
	 *            the time unit of the delay parameter
	 * @return a ScheduledFuture representing pending completion of
	 *         the task and whose <tt>get()</tt> method will return <tt>null</tt> upon completion
	 * @throws RejectedExecutionException
	 *             if the task cannot be
	 *             scheduled for execution
	 * @throws NullPointerException
	 *             if command is null
	 */
	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
		return submitFuture(new RunnableScheduledFuture<>(newTaskFor(command, null), delayMs));
	}

	/**
	 * Creates and executes a ScheduledFuture that becomes enabled after the
	 * given delay.
	 *
	 * @param callable
	 *            the function to execute
	 * @param delay
	 *            the time from now to delay execution
	 * @param unit
	 *            the time unit of the delay parameter
	 * @return a ScheduledFuture that can be used to extract result or cancel
	 * @throws RejectedExecutionException
	 *             if the task cannot be
	 *             scheduled for execution
	 * @throws NullPointerException
	 *             if callable is null
	 */
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
		return submitFuture(new RunnableScheduledFuture<>(newTaskFor(callable), delayMs));
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first
	 * after the given initial delay, and subsequently with the given
	 * period; that is executions will commence after <tt>initialDelay</tt> then <tt>initialDelay+period</tt>, then <tt>initialDelay + 2 * period</tt>, and so on.
	 * If any execution of the task
	 * encounters an exception, subsequent executions are suppressed.
	 * Otherwise, the task will only terminate via cancellation or
	 * termination of the executor. If any execution of this task
	 * takes longer than its period, then subsequent executions
	 * may start late, but will not concurrently execute.
	 *
	 * @param command
	 *            the task to execute
	 * @param initialDelay
	 *            the time to delay first execution
	 * @param period
	 *            the period between successive executions
	 * @param unit
	 *            the time unit of the initialDelay and period parameters
	 * @return a ScheduledFuture representing pending completion of
	 *         the task, and whose <tt>get()</tt> method will throw an
	 *         exception upon cancellation
	 * @throws RejectedExecutionException
	 *             if the task cannot be
	 *             scheduled for execution
	 * @throws NullPointerException
	 *             if command is null
	 * @throws IllegalArgumentException
	 *             if period less than or equal to zero
	 */
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		long initialDelayMs = TimeUnit.MILLISECONDS.convert(initialDelay, unit);
		long periodMs = TimeUnit.MILLISECONDS.convert(period, unit);
		return submitFuture(new RunnableScheduledFuture<>(newTaskFor(Runnables.doNothing(), null), command, initialDelayMs, periodMs));
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first
	 * after the given initial delay, and subsequently with the
	 * given delay between the termination of one execution and the
	 * commencement of the next. If any execution of the task
	 * encounters an exception, subsequent executions are suppressed.
	 * Otherwise, the task will only terminate via cancellation or
	 * termination of the executor.
	 *
	 * @param command
	 *            the task to execute
	 * @param initialDelay
	 *            the time to delay first execution
	 * @param delay
	 *            the delay between the termination of one
	 *            execution and the commencement of the next
	 * @param unit
	 *            the time unit of the initialDelay and delay parameters
	 * @return a ScheduledFuture representing pending completion of
	 *         the task, and whose <tt>get()</tt> method will throw an
	 *         exception upon cancellation
	 * @throws RejectedExecutionException
	 *             if the task cannot be
	 *             scheduled for execution
	 * @throws NullPointerException
	 *             if command is null
	 * @throws IllegalArgumentException
	 *             if delay less than or equal to zero
	 */
	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		long initialDelayMs = TimeUnit.MILLISECONDS.convert(initialDelay, unit);
		long periodMs = -TimeUnit.MILLISECONDS.convert(delay, unit);
		return submitFuture(new RunnableScheduledFuture<>(newTaskFor(Runnables.doNothing(), null), command, initialDelayMs, periodMs));
	}

	static <T> ScheduledFuture<T> submitFuture(RunnableScheduledFuture<T> future) {
		if (Thread.currentThread() == swtThread) {
			display.timerExec(future.delayInt(), future);
		} else {
			display.asyncExec(() -> {
				int delay = future.delayInt();
				if (delay <= 0) {
					future.run();
				} else {
					display.timerExec(delay, future);
				}
			});
		}
		return future;
	}

	/** Simple little mixin for making RunnableFutures schedulable. */
	@SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS", justification = "changes as it runs")
	private static class RunnableScheduledFuture<T> implements Runnable, ScheduledFuture<T> {
		private RunnableFuture<T> cancelDelegate;
		private Runnable toRun;
		private long time;
		/**
		 * = 0 -> no period
		 * > 0 -> fixedRate
		 * < 0 -> fixedDelay
		 */
		private long periodMs;

		private RunnableScheduledFuture(RunnableFuture<T> runnableFuture, long delayMs) {
			this.cancelDelegate = runnableFuture;
			this.toRun = runnableFuture;
			this.time = System.currentTimeMillis() + delayMs;
			this.periodMs = 0;
		}

		private RunnableScheduledFuture(RunnableFuture<T> runnableFuture, Runnable toRun, long delayMs, long periodMs) {
			this.cancelDelegate = runnableFuture;
			this.toRun = toRun;
			this.time = System.currentTimeMillis() + delayMs;
			this.periodMs = periodMs;
		}

		int delayInt() {
			return Ints.saturatedCast(time - System.currentTimeMillis());
		}

		// Runnable, overridden
		@Override
		public void run() {
			if (cancelDelegate.isCancelled()) {
				return;
			}
			if (periodMs > 0) {
				// fixedRate
				time += periodMs;
			}
			toRun.run();
			if (periodMs < 0) {
				// fixedDelay
				time = System.currentTimeMillis() - periodMs;
			}
			if (periodMs != 0) {
				long now = System.currentTimeMillis();
				// if it's periodic, we need to reschedule
				int delay = Ints.saturatedCast(time - now);
				if (delay < 0) {
					// if we blew a deadline, reset the schedule
					time = now;
					display.asyncExec(this);
				} else {
					display.timerExec(delay, this);
				}
			}
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed other) {
			if (other instanceof RunnableScheduledFuture) {
				return Ints.saturatedCast(time - ((RunnableScheduledFuture<?>) other).time);
			} else {
				int otherDelay = Ints.saturatedCast(other.getDelay(TimeUnit.MILLISECONDS));
				return delayInt() - otherDelay;
			}
		}

		// ScheduledFuture, delegated
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return cancelDelegate.cancel(mayInterruptIfRunning);
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return cancelDelegate.get();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return cancelDelegate.get(timeout, unit);
		}

		@Override
		public boolean isCancelled() {
			return cancelDelegate.isCancelled();
		}

		@Override
		public boolean isDone() {
			return cancelDelegate.isDone();
		}
	}

	static final class SwtDispatcher extends MainCoroutineDispatcher {
		private final SwtExec exec;

		public SwtDispatcher(SwtExec exec) {
			this.exec = exec;
		}

		public boolean isDispatchNeeded(CoroutineContext context) {
			if (exec == SwtExec.immediate && swtThread == Thread.currentThread()) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
			display.asyncExec(runnable);
		}

		@NotNull
		@Override
		public MainCoroutineDispatcher getImmediate() {
			if (exec == SwtExec.immediate()) {
				return this;
			} else {
				return (SwtDispatcher) SwtExec.immediate().rxExecutor.getDispatcher();
			}
		}
	}

	/** Global executor for actions which should only execute immediately on the SWT thread. */
	private static SwtExec swtOnly;

	/**
	 * UNLESS YOU HAVE PERFORMANCE PROBLEMS, USE {@link #immediate()} INSTEAD.
	 *
	 * Returns an SwtExecutor which can only be called from the SWT
	 * thread, and runs actions immediately.  Has the same behavior
	 * as {@link #immediate()} for callbacks on the SWT
	 * thread.  For values not on the SWT thread, `immediate()` behaves
	 * likes {@link #async()}, while `swtOnly()` throws an exception.
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, see comment in SwtExec.blocking()")
	public static SwtExec swtOnly() {
		if (swtOnly == null) {
			swtOnly = new SwtExec(exec -> Rx.callbackOn(exec, new SwtOnlyDispatcher())) {
				@Override
				public void execute(Runnable runnable) {
					requireNonNull(runnable);
					if (Thread.currentThread() == swtThread) {
						runnable.run();
					} else {
						SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
					}
				}
			};
		}
		return swtOnly;
	}

	static class SwtOnlyDispatcher extends CoroutineDispatcher {
		@Override
		public boolean isDispatchNeeded(CoroutineContext context) {
			return false;
		}

		@Override
		public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
			if (Thread.currentThread() == swtThread) {
				runnable.run();
			} else {
				SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
			}
		}
	}

	private static class SameThreadCoroutineDispatcher extends CoroutineDispatcher {
		@Override
		public void dispatch(@NotNull CoroutineContext coroutineContext, @NotNull Runnable runnable) {
			runnable.run();
		}
	}

	private static final SwtExec sameThread = new SwtExec(exec -> Rx.callbackOn(MoreExecutors.directExecutor(), new SameThreadCoroutineDispatcher())) {
		@Override
		public void execute(Runnable runnable) {
			requireNonNull(runnable);
			runnable.run();
		}
	};

	/**
	 * UNLESS YOU HAVE PERFORMANCE PROBLEMS, USE {@link #immediate()} INSTEAD.
	 *
	 * Returns an SwtExec which runs actions immediately, without checking
	 * whether they were called from the SWT thread or not.
	 */
	public static SwtExec sameThread() {
		return sameThread;
	}
}
