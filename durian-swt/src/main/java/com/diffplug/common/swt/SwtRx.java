/*
 * Copyright (C) 2020-2022 DiffPlug
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


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.primitives.Ints;
import com.diffplug.common.rx.Chit;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableSharedFlow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/** Utilities that convert SWT events into Rx-friendly Observables. */
public class SwtRx {
	/** Subscribes to the given widget and pipes the events to an {@link Flow}<{@link Event}>. */
	public static @SwtThread Flow<Event> addListener(Widget widget, int... events) {
		return addListener(widget, Ints.asList(events));
	}

	/** Subscribes to the given widget and pipes the events to an {@link Flow}<{@link Event}>. */
	public static @SwtThread Flow<Event> addListener(Widget widget, Collection<Integer> events) {
		return addListener(widget, events.stream());
	}

	/** Subscribes to the given widget and pipes the events to an {@link Flow}<{@link Event}>. */
	public static @SwtThread Flow<Event> addListener(Widget widget, Stream<Integer> events) {
		MutableSharedFlow<Event> observable = Rx.INSTANCE.createEmitFlow();
		events.forEach(event -> widget.addListener(event, e -> {
			Rx.INSTANCE.emit(observable, e);
		}));
		return observable;
	}

	/** Returns an {@link Flow}<{@link Point}> of the right-click mouse-up on the given control, in global coordinates. */
	public static @SwtThread Flow<Point> rightClickGlobal(Control ctl) {
		MutableSharedFlow<Point> observable = Rx.INSTANCE.createEmitFlow();
		ctl.addListener(MouseClick.RIGHT_CLICK_EVENT, e -> {
			if (e.button == MouseClick.RIGHT.code()) {
				Rx.INSTANCE.emit(observable, ctl.toDisplay(e.x, e.y));
			}
		});
		return observable;
	}

	/** Returns an {@link Flow}<{@link Point}> of the right-click mouse-up on the given control, in local coordinates. */
	public static @SwtThread Flow<Point> rightClickLocal(Control ctl) {
		MutableSharedFlow<Point> observable = Rx.INSTANCE.createEmitFlow();
		ctl.addListener(MouseClick.RIGHT_CLICK_EVENT, e -> {
			if (e.button == MouseClick.RIGHT.code()) {
				Rx.INSTANCE.emit(observable, new Point(e.x, e.y));
			}
		});
		return observable;
	}

	/** Returns an RxBox<String> which contains the content of the text box. */
	public static @SwtThread RxBox<String> textImmediate(Text text) {
		return textImp(text, SWT.Modify);
	}

	/**
	 * Returns an RxBox<String> which contains the content of the text box
	 * only when it has been confirmed by:
	 * <ul>
	 * <li>programmer setting the RxBox</li>
	 * <li>user hitting enter</li>
	 * <li>focus leaving the text</li>
	 * </ul>
	 */
	public static @SwtThread RxBox<String> textConfirmed(Text text) {
		return textImp(text, SWT.DefaultSelection, SWT.FocusOut);
	}

	private static RxBox<String> textImp(Text text, int... events) {
		RxBox<String> box = RxBox.of(text.getText());
		// set the text when the box changes
		SwtExec.immediate().guardOn(text).subscribe(box, newStr -> {
			String oldStr = text.getText();
			if (oldStr.equals(newStr)) {
				return;
			}
			Point selection = text.getSelection();
			// if the cursor is at the end of the text, it should stick to that as the text changes
			boolean startsAtEnd = selection.x == oldStr.length();
			boolean endsAtEnd = selection.y == oldStr.length();

			text.setText(newStr);

			// @formatter: off
			if (startsAtEnd) {
				selection.x = newStr.length();
			}
			if (endsAtEnd) {
				selection.y = newStr.length();
			}
			if (selection.x > newStr.length()) {
				selection.x = newStr.length();
			}
			if (selection.y > newStr.length()) {
				selection.y = newStr.length();
			}
			text.setSelection(selection);
		});
		// set the box when the text changes
		Listener listener = e -> box.set(text.getText());
		for (int event : events) {
			text.addListener(event, listener);
		}
		return box;
	}

	/**
	 * Returns an `RxBox<Boolean>` for the toggle state of the given button as an RxBox.
	 * <p>
	 * Applicable to SWT.TOGGLE, SWT.CHECK, and SWT.RADIO.
	 */
	public static RxBox<Boolean> toggle(Button btn) {
		Preconditions.checkArgument(SwtMisc.flagIsSet(SWT.TOGGLE, btn) || SwtMisc.flagIsSet(SWT.CHECK, btn) || SwtMisc.flagIsSet(SWT.RADIO, btn));
		RxBox<Boolean> box = RxBox.of(btn.getSelection());
		// update the box when a click happens
		btn.addListener(SWT.Selection, e -> {
			box.set(!box.get());
		});
		// update the button when the box happens
		SwtExec.immediate().guardOn(btn).subscribe(box, btn::setSelection);
		return box;
	}

	/**
	 * Populates a Combo returns an `RxBox<T>` which is bidirectionally bound
	 * to the given combo.
	 *
	 * @param combo  The combo which is being bound.
	 * @param values The values which the RxBox can take on.  Must match combo.getItems().
	 * @return An `RxBox<T>` which is bound bidirectionally to the given combo.
	 */
	public static <T> RxBox<T> combo(Combo combo, ImmutableList<T> values, Function<T, String> converter) {
		RxBox<T> box = RxBox.of(values.get(Math.max(0, combo.getSelectionIndex())));
		combo(combo, values, converter, box);
		return box;
	}

	/**
	 * Populates a Combo and bidirectionally binds it to an `RxBox<T>`.
	 *
	 * @param combo       The combo which is being bound.
	 * @param values      The values which the RxBox can take on.
	 * @param converter   A function for mapping values to strings.
	 * @param values      The values which the RxBox can take on.
	 * @return An `RxBox<T>` which will be bound bidirectionally to the given combo.
	 */
	public static <T> void combo(Combo combo, ImmutableList<T> values, Function<T, String> converter, RxBox<T> box) {
		// setup the combo
		combo.removeAll();
		for (T value : values) {
			combo.add(converter.apply(value));
		}
		// bind it to a box
		combo.addListener(SWT.Selection, e -> {
			box.set(values.get(combo.getSelectionIndex()));
		});
		SwtExec.immediate().guardOn(combo).subscribe(box, mdlValue -> {
			combo.select(values.indexOf(mdlValue));
		});
	}

	/** Wraps the given {@link ControlWrapper} in an {@link Chit}. */
	public static Chit chit(ControlWrapper wrapper) {
		return chit(wrapper.getRootControl());
	}

	/** Wraps the given {@link Widget} in an {@link Chit}. */
	public static Chit chit(Widget guard) {
		if (guard.isDisposed()) {
			return Chit.alreadyDisposed();
		} else {
			Chit.Settable settable = Chit.settable();
			if (SwtExec.isRunningOnUI()) {
				hook(guard, settable);
			} else {
				SwtExec.async().execute(() -> {
					if (guard.isDisposed()) {
						settable.dispose();
					} else {
						hook(guard, settable);
					}
				});
			}
			return settable;
		}
	}

	private static void hook(Widget guard, Chit.Settable disposable) {
		guard.addListener(SWT.Dispose, e -> disposable.dispose());
	}
}
