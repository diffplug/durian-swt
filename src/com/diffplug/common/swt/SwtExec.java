/*
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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ListenableFuture;

import com.diffplug.common.base.Box.Nullable;
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxSubscriber;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * {link ExecutorService}s which execute on the SWT UI thread.
 * <p>
 * There are three "kinds" of {@code SwtExec}:
 * <ul>
 * <li>{@link #async} -> performs actions using  {@link Display#asyncExec}</li>
 * <li>{@link #immediate} -> performs actions immediately if called from a UI thread, otherwise delegates to  {@link Display#asyncExec}.</li>
 * <li>{@link #blocking} -> performs actions immediately if called from a UI thread, else delegates to  {@link Display#syncExec}.</li>
 * </ul>
 * <p>
 * In addition to the standard executor methods, each {@code SwtExec} also has a method {@link #guardOn}, which
 * returns a {@link Guarded} instance - the cure for "Widget is disposed" errors.  {@code Guarded} has methods like
 * {@link Guarded#wrap} and {@link Guarded#execute}, whose contents are <i>only executed if the guarded widget is not disposed</i>.
 * <p>
 * {@link Guarded} also contains the full API of {@link com.diffplug.common.rx.Rx},
 * which allows subscribing to {@link rx.Observable}s and {@link com.google.common.util.concurrent.ListenableFuture}s while guarding on a given widget.
 */
public class SwtExec extends AbstractExecutorService implements ScheduledExecutorService, Rx.HasRxExecutor {
	/** Global executor for async. */
	private static SwtExec async;

	/**
	 * Returns an "async" SwtExecutor.
	 * <p>
	 * When {@code execute(Runnable)} is called, the {@code Runnable} will be passed to {@link Display#asyncExec Display.asyncExec}.
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, as explained in the comment below.")
	public static SwtExec async() {
		if (async == null) {
			// There is an acceptable race condition here.  See SwtExec.blocking() for details.
			async = new SwtExec(Display.getDefault());
		}
		return async;
	}

	/** Global executor for immediate. */
	private static SwtExec immediate;

	/**
	 * Returns an "immediate" SwtExecutor.
	 * <ul>
	 * <li>When {@code execute(Runnable)} is called from the SWT thread, the {@code Runnable} will be executed immediately.</li>
	 * <li>Else, the {@code Runnable} will be passed to {@link Display#asyncExec Display.asyncExec}.</li>
	 * </ul>
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, as explained in the comment below.")
	public static SwtExec immediate() {
		if (immediate == null) {
			// There is an acceptable race condition here.  See SwtExec.blocking() for details.
			immediate = new SwtExec(Display.getDefault()) {
				@Override
				public void execute(Runnable runnable) {
					Preconditions.checkNotNull(runnable);
					if (!display.isDisposed()) {
						if (Thread.currentThread() == display.getThread()) {
							runnable.run();
						} else {
							display.asyncExec(runnable);
						}
					} else {
						throw new RejectedExecutionException();
					}
				}
			};
		}
		return immediate;
	}

	/** Global executor for blocking. */
	private static Blocking blocking;

	/**
	 * Returns a "blocking" SwtExecutor.
	 * <ul>
	 * <li>When {@code execute(Runnable)} is called from the SWT thread, the {@code Runnable} will be executed immediately.</li>
	 * <li>Else, the {@code Runnable} will be passed to {@link Display#syncExec Display.syncExec}.</li>
	 * </ul>
	 * This instance also has a blocking {@link Blocking#get get()} method for doing a get in the UI thread.
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, as explained in the comment below.")
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
	 * An SwtExec (obtained via {@link SwtExec#blocking()}) which adds a blocking {@link Blocking#get get()} method.
	 * <ul>
	 * <li>When {@code execute(Runnable)} is called from the SWT thread, the {@code Runnable} will be executed immediately.</li>
	 * <li>Else, the {@code Runnable} will be passed to {@link Display#syncExec Display.syncExec}.</li>
	 * </ul>
	 * @see SwtExec#blocking
	 */
	public static class Blocking extends SwtExec {
		private Blocking() {
			super(Display.getDefault());
		}

		@Override
		public void execute(Runnable runnable) {
			Preconditions.checkNotNull(runnable);
			if (!display.isDisposed()) {
				if (Thread.currentThread() == display.getThread()) {
					runnable.run();
				} else {
					display.syncExec(runnable);
				}
			} else {
				throw new RejectedExecutionException();
			}
		}

		/**
		 * Performs a blocking get in the UI thread.
		 * 
		 * @param supplier will be executed in the UI thread.
		 * @return the value which was returned by supplier.
		 */
		public <T> T get(Supplier<T> supplier) {
			Display current = Display.getCurrent();
			if (current != null) {
				return supplier.get();
			} else {
				Nullable<T> holder = Nullable.ofNull();
				execute(() -> holder.set(supplier.get()));
				return holder.get();
			}
		}

		// The schedule methods don't sync with the SwtExec.blocking() semantics.
		@Override
		public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
			throw Unhandled.operationException();
		}

		@Override
		public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
			throw Unhandled.operationException();
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
			throw Unhandled.operationException();
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
			throw Unhandled.operationException();
		}
	}

	/** Executes the given runnable in the UI thread after the given delay. */
	public static void timerExec(int ms, Runnable runnable) {
		async().display.timerExec(ms, runnable);
	}

	/** Returns an API for performing actions which are guarded on the given Widget. */
	public Guarded guardOn(Widget widget) {
		return new Guarded(this, Objects.requireNonNull(widget));
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
			this.guard = guard;
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
			parent.display.timerExec(delayMs, guardedRunnable(runnable));
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
		public <T> Subscription subscribe(Observable<? extends T> observable, Rx<T> listener) {
			return subscribe(() -> parent.rxExecutor.subscribe(observable, listener));
		}

		@Override
		public <T> Subscription subscribe(ListenableFuture<? extends T> future, Rx<T> listener) {
			return subscribe(() -> parent.rxExecutor.subscribe(future, listener));
		}

		@Override
		public <T> Subscription subscribe(CompletionStage<? extends T> future, Rx<T> listener) {
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

	protected final Display display;
	protected final Scheduler scheduler;
	protected final Rx.RxExecutor rxExecutor;

	/** Returns an instance of {@link com.diffplug.common.rx.Rx.RxExecutor}. */
	@Override
	public Rx.RxExecutor getRxExecutor() {
		return rxExecutor;
	}

	/** Returns a Scheduler appropriate for Rx. */
	public Scheduler getRxScheduler() {
		return scheduler;
	}

	private SwtExec(Display display) {
		this.display = display;
		this.scheduler = new Scheduler() {
			@Override
			public Worker createWorker() {
				return new Worker() {
					private BooleanSubscription workerSub = BooleanSubscription.create();

					@Override
					public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
						if (isUnsubscribed()) {
							return Subscriptions.unsubscribed();
						}
						if (delayTime <= 0) {
							return schedule(action);
						} else {
							ScheduledFuture<?> future = SwtExec.this.schedule(() -> {
								if (!workerSub.isUnsubscribed()) {
									action.call();
								}
							}, delayTime, unit);
							Subscription sub = Subscriptions.create(() -> {
								future.cancel(true);
							});
							return sub;
						}
					}

					@Override
					public Subscription schedule(Action0 action) {
						if (isUnsubscribed()) {
							return Subscriptions.unsubscribed();
						}
						BooleanSubscription sub = BooleanSubscription.create();
						execute(() -> {
							if (!sub.isUnsubscribed() && !workerSub.isUnsubscribed()) {
								action.call();
							}
						});
						return sub;
					}

					@Override
					public void unsubscribe() {
						workerSub.unsubscribe();
					}

					@Override
					public boolean isUnsubscribed() {
						return workerSub.isUnsubscribed();
					}
				};
			}
		};
		this.rxExecutor = Rx.on(this, scheduler);
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
		Preconditions.checkNotNull(runnable);
		if (!display.isDisposed()) {
			display.asyncExec(runnable);
		} else {
			throw new RejectedExecutionException();
		}
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
		throw Unhandled.operationException();
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
		throw Unhandled.operationException();
	}

	/**
	 * Returns <tt>true</tt> if this executor has been shut down.
	 *
	 * @return <tt>true</tt> if this executor has been shut down
	 */
	public boolean isShutdown() {
		throw Unhandled.operationException();
	}

	/**
	 * Returns <tt>true</tt> if all tasks have completed following shut down.
	 * Note that <tt>isTerminated</tt> is never <tt>true</tt> unless
	 * either <tt>shutdown</tt> or <tt>shutdownNow</tt> was called first.
	 *
	 * @return <tt>true</tt> if all tasks have completed following shut down
	 */
	public boolean isTerminated() {
		throw Unhandled.operationException();
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
		throw Unhandled.operationException();
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
		throw Unhandled.operationException();
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
		throw Unhandled.operationException();
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
}
