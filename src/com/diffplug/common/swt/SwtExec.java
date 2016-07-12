/*
 * Copyright 2016 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import rx.*;
import rx.Observable;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.*;

import com.diffplug.common.base.Box.Nullable;
import com.diffplug.common.primitives.Ints;
import com.diffplug.common.rx.*;
import com.diffplug.common.util.concurrent.ListenableFuture;
import com.diffplug.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
	public Guarded guardOn(Widget widget) {
		return new Guarded(this, widget);
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
	public static class Guarded implements Executor, RxSubscriber {
		private final SwtExec parent;
		private final Widget guard;

		private Guarded(SwtExec parent, Widget guard) {
			this.parent = parent;
			this.guard = requireNonNull(guard);
		}

		/** Returns the guard widget. */
		public Widget getGuard() {
			return guard;
		}

		/** Creates a runnable which runs on this Executor iff the guard widget is not disposed. */
		public Runnable wrap(Runnable runnable) {
			return () -> execute(runnable);
		}

		/** Runs the given runnable iff the guard widget is not disposed. */
		@Override
		public void execute(Runnable runnable) {
			parent.execute(guardedRunnable(runnable));
		}

		/** Runs the given runnable after the given delay iff the guard widget is not disposed. */
		public void timerExec(int delayMs, Runnable runnable) {
			display.timerExec(delayMs, guardedRunnable(runnable));
		}

		/** Returns a Runnable which guards on the guard widget. */
		private Runnable guardedRunnable(Runnable toGuard) {
			return () -> {
				if (!guard.isDisposed()) {
					toGuard.run();
				}
			};
		}

		@Override
		public <T> Subscription subscribe(Observable<? extends T> observable, RxListener<T> listener) {
			return subscribe(() -> parent.rxExecutor.subscribe(observable, listener));
		}

		@Override
		public <T> Subscription subscribe(ListenableFuture<? extends T> future, RxListener<T> listener) {
			return subscribe(() -> parent.rxExecutor.subscribe(future, listener));
		}

		@Override
		public <T> Subscription subscribe(CompletionStage<? extends T> future, RxListener<T> listener) {
			return subscribe(() -> parent.rxExecutor.subscribe(future, listener));
		}

		private Subscription subscribe(Supplier<Subscription> subscriber) {
			if (!guard.isDisposed()) {
				Subscription subscription = subscriber.get();
				SwtExec.immediate().execute(() -> {
					if (!guard.isDisposed()) {
						guard.addListener(SWT.Dispose, e -> subscription.unsubscribe());
					} else {
						subscription.unsubscribe();
					}
				});
				return subscription;
			} else {
				return Subscriptions.unsubscribed();
			}
		}
	}

	protected final RxExecutor rxExecutor;

	/** Returns an instance of {@link com.diffplug.common.rx.RxExecutor}. */
	@Override
	public RxExecutor getRxExecutor() {
		return rxExecutor;
	}

	SwtExec() {
		this(exec -> Rx.callbackOn(exec, new SwtScheduler(exec)));
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
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns <tt>true</tt> if this executor has been shut down.
	 *
	 * @return <tt>true</tt> if this executor has been shut down
	 */
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
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
		@SuppressWarnings({"rawtypes", "unchecked"})
		RunnableScheduledFuture<?> future = new RunnableScheduledFuture(newTaskFor(command, null), delayMs);
		display.timerExec(Ints.checkedCast(delayMs), future);
		return future;
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
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		long delayMs = TimeUnit.MILLISECONDS.convert(delay, unit);
		RunnableScheduledFuture<V> future = new RunnableScheduledFuture<V>(newTaskFor(callable), delayMs);
		display.timerExec(Ints.checkedCast(delayMs), future);
		return future;
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
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		throw new UnsupportedOperationException();
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
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		throw new UnsupportedOperationException();
	}

	/** Simple little mixin for making RunnableFutures schedulable. */
	private static class RunnableScheduledFuture<T> implements Runnable, ScheduledFuture<T> {
		private RunnableFuture<T> runnableFuture;
		private long delayMs;

		private RunnableScheduledFuture(RunnableFuture<T> runnableFuture, long delayMs) {
			this.runnableFuture = runnableFuture;
			this.delayMs = delayMs;
		}

		// Runnable, overridden
		@Override
		public void run() {
			runnableFuture.run();
		}

		// Delayed, implemented
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(delayMs, TimeUnit.MILLISECONDS);
		}

		// Comparable<Delayed>, implemented
		@Override
		public int compareTo(Delayed other) {
			return Ints.saturatedCast(delayMs - other.getDelay(TimeUnit.MILLISECONDS));
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Delayed) {
				return compareTo((Delayed) other) == 0;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(delayMs);
		}

		// ScheduledFuture, delegated
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return runnableFuture.cancel(mayInterruptIfRunning);
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return runnableFuture.get();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return runnableFuture.get(timeout, unit);
		}

		@Override
		public boolean isCancelled() {
			return runnableFuture.isCancelled();
		}

		@Override
		public boolean isDone() {
			return runnableFuture.isDone();
		}
	}

	/** Scheduler that runs tasks on Swt's event dispatch thread. */
	static final class SwtScheduler extends Scheduler {
		final SwtExec exec;

		public SwtScheduler(SwtExec exec) {
			this.exec = exec;
		}

		@Override
		public Worker createWorker() {
			return new SwtWorker(exec);
		}

		static final class SwtWorker extends Scheduler.Worker {
			final SwtExec exec;

			volatile boolean unsubscribed;

			/** Set of active tasks, guarded by this. */
			Set<SwtScheduledAction> tasks;

			public SwtWorker(SwtExec exec) {
				this.exec = exec;
				this.tasks = new HashSet<>();
			}

			@Override
			public void unsubscribe() {
				if (unsubscribed) {
					return;
				}
				unsubscribed = true;

				Set<SwtScheduledAction> set;
				synchronized (this) {
					set = tasks;
					tasks = null;
				}

				if (set != null) {
					for (SwtScheduledAction a : set) {
						a.cancelFuture();
					}
				}
			}

			void remove(SwtScheduledAction a) {
				if (unsubscribed) {
					return;
				}
				synchronized (this) {
					if (unsubscribed) {
						return;
					}

					tasks.remove(a);
				}
			}

			@Override
			public boolean isUnsubscribed() {
				return unsubscribed;
			}

			@Override
			public Subscription schedule(Action0 action) {
				if (unsubscribed) {
					return Subscriptions.unsubscribed();
				}

				SwtScheduledAction a = new SwtScheduledAction(action, this);

				synchronized (this) {
					if (unsubscribed) {
						return Subscriptions.unsubscribed();
					}

					tasks.add(a);
				}

				exec.execute(a);

				if (unsubscribed) {
					a.cancel();
					return Subscriptions.unsubscribed();
				}

				return a;
			}

			@Override
			public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
				if (unsubscribed) {
					return Subscriptions.unsubscribed();
				}

				SwtScheduledAction a = new SwtScheduledAction(action, this);

				synchronized (this) {
					if (unsubscribed) {
						return Subscriptions.unsubscribed();
					}

					tasks.add(a);
				}

				Future<?> f = exec.schedule(a, delayTime, unit);

				if (unsubscribed) {
					a.cancel();
					f.cancel(true);
					return Subscriptions.unsubscribed();
				}

				a.setFuture(f);

				return a;
			}

			/**
			 * Represents a cancellable asynchronous Runnable that wraps an action
			 * and manages the associated Worker lifecycle.
			 */
			static final class SwtScheduledAction implements Runnable, Subscription {
				final Action0 action;

				final SwtWorker parent;

				volatile Future<?> future;
				@SuppressWarnings("rawtypes")
				static final AtomicReferenceFieldUpdater<SwtScheduledAction, Future> FUTURE = AtomicReferenceFieldUpdater.newUpdater(SwtScheduledAction.class, Future.class, "future");

				static final Future<?> CANCELLED = new FutureTask<>(() -> {}, null);

				static final Future<?> FINISHED = new FutureTask<>(() -> {}, null);

				volatile int state;
				static final AtomicIntegerFieldUpdater<SwtScheduledAction> STATE = AtomicIntegerFieldUpdater.newUpdater(SwtScheduledAction.class, "state");

				static final int STATE_ACTIVE = 0;
				static final int STATE_FINISHED = 1;
				static final int STATE_CANCELLED = 2;

				public SwtScheduledAction(Action0 action, SwtWorker parent) {
					this.action = action;
					this.parent = parent;
				}

				@Override
				public void run() {
					if (!parent.unsubscribed && state == STATE_ACTIVE) {
						try {
							action.call();
						} finally {
							FUTURE.lazySet(this, FINISHED);
							if (STATE.compareAndSet(this, STATE_ACTIVE, STATE_FINISHED)) {
								parent.remove(this);
							}
						}
					}
				}

				@Override
				public boolean isUnsubscribed() {
					return state != STATE_ACTIVE;
				}

				@Override
				public void unsubscribe() {
					if (STATE.compareAndSet(this, STATE_ACTIVE, STATE_CANCELLED)) {
						parent.remove(this);
					}
					cancelFuture();
				}

				void setFuture(Future<?> f) {
					if (FUTURE.compareAndSet(this, null, f)) {
						if (future != FINISHED) {
							f.cancel(true);
						}
					}
				}

				void cancelFuture() {
					Future<?> f = future;
					if (f != CANCELLED && f != FINISHED) {
						f = FUTURE.getAndSet(this, CANCELLED);
						if (f != null && f != CANCELLED && f != FINISHED) {
							f.cancel(true);
						}
					}
				}

				void cancel() {
					state = STATE_CANCELLED;
				}
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
			swtOnly = new SwtExec(exec -> Rx.callbackOn(exec, new SwtOnlyScheduler())) {
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

	/**
	 * Copied straight from rx.schedulers.ImmediateScheduler,
	 * but checks for the SWT thread before running stuff,
	 * and handles future-scheduling correctly.
	 */
	static final class SwtOnlyScheduler extends Scheduler {
		@Override
		public Worker createWorker() {
			return new InnerImmediateScheduler();
		}

		private static final class InnerImmediateScheduler extends Scheduler.Worker {
			final BooleanSubscription innerSubscription = new BooleanSubscription();

			@Override
			public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
				BooleanSubscription actionSubscription = new BooleanSubscription();
				SwtExec.async().schedule(() -> {
					if (!actionSubscription.isUnsubscribed()) {
						action.call();
						actionSubscription.unsubscribe();
					}
				}, delayTime, unit);
				return actionSubscription;
			}

			@Override
			public Subscription schedule(Action0 action) {
				if (Thread.currentThread() == swtThread) {
					action.call();
				} else {
					SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
				}
				return Subscriptions.unsubscribed();
			}

			@Override
			public void unsubscribe() {
				innerSubscription.unsubscribe();
			}

			@Override
			public boolean isUnsubscribed() {
				return innerSubscription.isUnsubscribed();
			}
		}
	}

	private static final SwtExec sameThread = new SwtExec(exec -> Rx.callbackOn(MoreExecutors.directExecutor(), Schedulers.immediate())) {
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
