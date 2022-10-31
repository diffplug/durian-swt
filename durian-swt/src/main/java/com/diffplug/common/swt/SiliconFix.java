/*
 * Copyright (C) 2022 DiffPlug
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


import com.diffplug.common.swt.os.Arch;
import com.diffplug.common.swt.os.OS;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/**
 * Call these methods whenever a `Table`, `Tree`, or `List` is instantiated and it will do nothing,
 * excpet on Mac Apple Silicon where it will call `setFont(null)` as a workaround for
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=575696
 */
public class SiliconFix {
	public static boolean APPLY_FIX = OS.getRunning().isMac() && Arch.getRunning() == Arch.arm64;

	public static void fix(Table table) {
		if (APPLY_FIX) {
			table.setFont(null);
		}
	}

	public static void fix(Tree tree) {
		if (APPLY_FIX) {
			tree.setFont(null);
		}
	}

	public static void fix(List list) {
		if (APPLY_FIX) {
			list.setFont(null);
		}
	}
}
