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

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.google.common.base.Preconditions;

import com.diffplug.common.swt.SwtMisc;

/** Provides a fluent interface for constructing actions. */
public class Actions {
	/** Style enum for various kinds of IAction. */
	public enum Style {
		// @formatter:off
		PUSH(IAction.AS_PUSH_BUTTON),
		CHECK(IAction.AS_CHECK_BOX),
		RADIO(IAction.AS_RADIO_BUTTON),
		MENU(IAction.AS_DROP_DOWN_MENU);
		// @formatter:on

		public final int jfaceStyle;

		private Style(int jfaceStyle) {
			this.jfaceStyle = jfaceStyle;
		}

		/** Returns the Style of the given IAction. */
		public static Style of(IAction action) {
			return Arrays.asList(Style.values()).stream()
					.filter(style -> style.jfaceStyle == action.getStyle())
					.findFirst().get();
		}
	}

	/** Concise method for creating a push action. */
	public static IAction create(String text, Runnable action) {
		return builder().setText(text).setRunnable(action).build();
	}

	/** Concise method for creating a push action. */
	public static IAction create(String text, ImageDescriptor image, Runnable action) {
		return builder().setText(text).setImage(image).setRunnable(action).build();
	}

	/** Concise method for creating a push action. */
	public static IAction create(String text, ImageDescriptor image, int accelerator, Runnable action) {
		return builder().setText(text).setImage(image).setAccelerator(accelerator).setRunnable(action).build();
	}

	/** Concise method for creating a push action. */
	public static IAction create(String text, Listener listener) {
		return builder().setText(text).setListener(listener).build();
	}

	/** Concise method for creating a push action. */
	public static IAction create(String text, ImageDescriptor image, Listener listener) {
		return builder().setText(text).setImage(image).setListener(listener).build();
	}

	/** Concise method for creating a push action. */
	public static IAction create(String text, ImageDescriptor image, int accelerator, Listener listener) {
		return builder().setText(text).setImage(image).setAccelerator(accelerator).setListener(listener).build();
	}

	/** Defaults to a push style with no text, accelerator, or action. */
	public static Actions builder() {
		return new Actions();
	}

	/** Action builder that starts with all default values. */
	private Actions() {}

	private Style style = Style.PUSH;
	private String text = "";
	private String tooltip = "";
	private BiConsumer<IAction, Event> callback;
	private int accelerator = SWT.NONE;
	private ImageDescriptor img = null;

	/** Copies all behavior from the given action. */
	public static Actions builderCopy(IAction action) {
		return new Actions(action);
	}

	private Actions(IAction action) {
		this.text = action.getText();
		this.style = Style.of(action);
		if (action instanceof ActionImp) {
			callback = ((ActionImp) action).callback;
		} else {
			callback = (a, e) -> {
				if (e == null) {
					action.run();
				} else {
					action.runWithEvent(e);
				}
			};
		}
		this.img = action.getImageDescriptor();
		this.accelerator = action.getAccelerator();
		this.tooltip = action.getToolTipText();

		if (accelerator != SWT.NONE) {
			// the toolTip might have had an accelerator added,
			// which we'll want to strip so it doesn't get doubled
			String hint = getAcceleratorHint(accelerator);
			if (tooltip.endsWith(hint)) {
				tooltip = tooltip.substring(0, tooltip.length() - hint.length());
			}
		}
	}

	/** Sets the text and tooltip. */
	public Actions setText(String text) {
		this.text = text;
		this.tooltip = text;
		return this;
	}

	/** Sets the tooltip. */
	public Actions setTooltip(String tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	/** Sets the style. */
	public Actions setStyle(Style style) {
		this.style = style;
		return this;
	}

	/** Sets the callback to be the given Runnable. */
	public Actions setRunnable(Runnable run) {
		return setCallback((action, event) -> run.run());
	}

	/** Sets the callback to be the given Listener.  It may receive a null event. */
	public Actions setListener(Listener listener) {
		return setCallback((action, event) -> listener.handleEvent(event));
	}

	/** Sets the callback to be the given BiConsumer.  Action will definitely be present, but event might be missing. */
	public Actions setCallback(BiConsumer<IAction, Event> callback) {
		this.callback = callback;
		return this;
	}

	/** Sets the keyboard accelerator. */
	public Actions setAccelerator(int accelerator) {
		this.accelerator = accelerator;
		return this;
	}

	/** Sets the runnable. */
	public Actions setImage(ImageDescriptor img) {
		this.img = img;
		return this;
	}

	/** Returns an action with the specified properties. */
	public IAction build() {
		ActionImp action = new ActionImp(text, style.jfaceStyle, callback);
		action.setImageDescriptor(img);
		action.setAccelerator(accelerator);
		setToolTipAccelAware(action, tooltip);
		return action;
	}

	/** A trivial Action class which delegates its run() method to a Runnable. */
	private static class ActionImp extends Action {
		private final BiConsumer<IAction, Event> callback;

		private ActionImp(String text, int style, BiConsumer<IAction, Event> callback) {
			super(text, style);
			this.callback = callback;
		}

		@Override
		public void run() {
			runWithEvent(null);
		}

		@Override
		public void runWithEvent(Event e) {
			callback.accept(this, e);
		}
	}

	/** Sets the tooltip text for the given action while remaing aware of its accelerator. */
	public static void setToolTipAccelAware(IAction action, String tooltip) {
		if (action.getAccelerator() == SWT.NONE) {
			action.setToolTipText(tooltip);
		} else {
			action.setToolTipText(tooltip + getAcceleratorHint(action.getAccelerator()));
		}
	}

	/** Returns a human-readable hint about the accelerator. Must not be passed NO_ACCELERATOR. */
	private static String getAcceleratorHint(int accelerator) {
		Preconditions.checkArgument(accelerator != SWT.NONE);
		return " [" + Actions.getAcceleratorString(accelerator) + "]";
	}

	/** Returns the given key accelerator as a string (including shift, control, command, etc). */
	public static String getAcceleratorString(int accelerator) {
		if (accelerator == SWT.NONE) {
			return "<none>";
		}

		StringBuilder builder = new StringBuilder(16);

		if (SwtMisc.flagIsSet(SWT.CTRL, accelerator)) {
			builder.append("Ctrl ");
			accelerator -= SWT.CTRL;
		}

		if (SwtMisc.flagIsSet(SWT.COMMAND, accelerator)) {
			builder.append(UC_CMD + " ");
			accelerator -= SWT.COMMAND;
		}

		if (SwtMisc.flagIsSet(SWT.ALT, accelerator)) {
			builder.append("Alt ");
			accelerator -= SWT.ALT;
		}

		if (SwtMisc.flagIsSet(SWT.SHIFT, accelerator)) {
			builder.append("Shift ");
			accelerator -= SWT.SHIFT;
		}

		final String end;

		if (SWT.F1 <= accelerator && accelerator <= SWT.F20) {
			int num = 1 + accelerator - SWT.F1;
			end = "F" + Integer.toString(num);
		} else {
			switch (accelerator) {
			case SWT.ARROW_UP:
				end = UC_ARROW_UP;
				break;
			case SWT.ARROW_DOWN:
				end = UC_ARROW_DOWN;
				break;
			case SWT.ARROW_LEFT:
				end = UC_ARROW_LEFT;
				break;
			case SWT.ARROW_RIGHT:
				end = UC_ARROW_RIGHT;
				break;
			case SWT.ESC:
				end = "Esc";
				break;
			default:
				end = Character.toString((char) accelerator).toUpperCase(Locale.getDefault());
				break;
			}
		}
		builder.append(end);

		return builder.toString();
	}

	/** Unicode for arrows. */
	public static final String UC_ARROW_UP = "\u2191";
	public static final String UC_ARROW_DOWN = "\u2193";
	public static final String UC_ARROW_LEFT = "\u2190";
	public static final String UC_ARROW_RIGHT = "\u2192";
	/** Unicode for the Mac cmd icon. */
	public static final String UC_CMD = "\u2318";

}
