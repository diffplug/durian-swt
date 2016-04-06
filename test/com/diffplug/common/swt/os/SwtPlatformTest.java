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
package com.diffplug.common.swt.os;

import org.junit.Assert;
import org.junit.Test;

public class SwtPlatformTest {
	@Test
	public void test() {
		SwtPlatform platform = SwtPlatform.parseWsOsArch("win32.win32.x86");
		Assert.assertEquals("win32", platform.getWs());
		Assert.assertEquals("win32", platform.getOs());
		Assert.assertEquals("x86", platform.getArch());

		Assert.assertEquals("windows-x86_32", platform.getWuffString());
		Assert.assertEquals("(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86) )", platform.platformFilter());
	}
}
