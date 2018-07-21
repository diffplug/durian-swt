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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.diffplug.common.primitives.Ints;

public class SwtExecSchedulingTest {
	static void testOffUiThread(Throwing.Runnable test) {
		InteractiveTest.testCoat("Running a headless test", cmp -> {
			new Thread() {
				@Override
				public void run() {
					try {
						test.run();
						InteractiveTest.closeAndPass(cmp);
					} catch (Throwable t) {
						InteractiveTest.closeAndFail(cmp, t);
					}
				}
			}.start();
		});
	}

	@Test
	public void scheduleCancelWorks() throws InterruptedException, ExecutionException {
		testOffUiThread(() -> {
			Box<Boolean> hasRun = Box.of(false);
			ScheduledFuture<?> future = SwtExec.async().schedule(() -> {
				hasRun.set(true);
			}, 100, TimeUnit.MILLISECONDS);
			Thread.sleep(10);
			future.cancel(true);
			Thread.sleep(200);
			try {
				future.get();
				Assert.fail();
			} catch (CancellationException e) {
				// we got the cancellation we expected
			}
			Assert.assertEquals(true, future.isCancelled());
			Assert.assertEquals(true, future.isDone());
			Assert.assertEquals(false, hasRun.get());
		});
	}

	@Test
	public void schedulePerformance() throws InterruptedException, ExecutionException {
		testOffUiThread(() -> {
			Box.Int delay = Box.Int.of(-1);
			long start = System.currentTimeMillis();
			ScheduledFuture<?> future = SwtExec.async().schedule(() -> {
				delay.set(Ints.saturatedCast(System.currentTimeMillis() - start));
			}, 1000, TimeUnit.MILLISECONDS);
			// block until finish
			future.get();
			Assertions.assertThat(delay.getAsInt()).isBetween(750, 1250);
		});
	}

	@Test
	public void scheduleFixedRate() throws InterruptedException, ExecutionException {
		testOffUiThread(() -> {
			Box.Int count = Box.Int.of(0);
			ScheduledFuture<?> future = SwtExec.async().scheduleAtFixedRate(() -> {
				count.set(count.getAsInt() + 1);
				// block UI thread for 400ms - very bad
				Errors.rethrow().run(() -> Thread.sleep(400));
			}, 500, 500, TimeUnit.MILLISECONDS);
			Thread.sleep(2250);
			// increment every 500ms for 2250ms, it should tick 4 times
			Assertions.assertThat(count.getAsInt()).isEqualTo(4);
			future.cancel(true);
			Thread.sleep(1000);
			Assertions.assertThat(count.getAsInt()).isEqualTo(4);
		});
	}

	@Test
	public void scheduleFixedDelay() throws InterruptedException, ExecutionException {
		testOffUiThread(() -> {
			Box.Int count = Box.Int.of(0);
			ScheduledFuture<?> future = SwtExec.async().scheduleWithFixedDelay(() -> {
				// block UI thread for 400ms - very bad
				Errors.rethrow().run(() -> Thread.sleep(400));
				count.set(count.getAsInt() + 1);
			}, 500, 500, TimeUnit.MILLISECONDS);
			Thread.sleep(2250);
			// increment every 500ms and burn 400ms for 2250ms, it should tick twice
			Assertions.assertThat(count.getAsInt()).isEqualTo(2);
			future.cancel(true);
			Thread.sleep(1000);
			Assertions.assertThat(count.getAsInt()).isEqualTo(2);
		});
	}
}
