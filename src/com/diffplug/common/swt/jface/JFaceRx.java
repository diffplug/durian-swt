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
package com.diffplug.common.swt.jface;

import org.eclipse.jface.action.IAction;

import com.google.common.base.Preconditions;

import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.SwtMisc;

/** Utilities that convert JFace events into Rx-friendly Observables. */
public class JFaceRx {
	/**
	 * Returns an {@code RxBox<Boolean>} for the toggle state of the given action as an RxBox.
	 * <p>
	 * Applicable to IAction.AS_CHECK_BOX and AS_RADIO_BUTTON.
	 */
	public static RxBox<Boolean> toggle(IAction action) {
		Preconditions.checkArgument(SwtMisc.flagIsSet(IAction.AS_CHECK_BOX, action.getStyle()) ||
				SwtMisc.flagIsSet(IAction.AS_RADIO_BUTTON, action.getStyle()));
		RxBox<Boolean> box = RxBox.of(action.isChecked());
		action.addPropertyChangeListener(e -> {
			if ("checked".equals(e.getProperty())) {
				box.set((Boolean) e.getNewValue());
			}
		});
		Rx.subscribe(box, isChecked -> {
			action.setChecked(isChecked);
		});
		return box;
	}
}
