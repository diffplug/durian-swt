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

import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import rx.Observable;
import rx.subjects.PublishSubject;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import com.diffplug.common.rx.RxBox;

/** Utilities that convert SWT events into Rx-friendly Observables. */
public class SwtRx {
	/** Subscribes to the given widget and pipes the events to an {@link Observable}<{@link Event}>. */
	public static Observable<Event> addListener(Widget widget, int... events) {
		return addListener(widget, Ints.asList(events));
	}

	/** Subscribes to the given widget and pipes the events to an {@link Observable}<{@link Event}>. */
	public static Observable<Event> addListener(Widget widget, Collection<Integer> events) {
		return addListener(widget, events.stream());
	}

	/** Subscribes to the given widget and pipes the events to an {@link Observable}<{@link Event}>. */
	public static Observable<Event> addListener(Widget widget, Stream<Integer> events) {
		PublishSubject<Event> subject = PublishSubject.create();
		events.forEach(event -> widget.addListener(event, subject::onNext));
		return subject.asObservable();
	}

	/** Returns an {@link Observable}<{@link Point}> of the right-click mouse-up on the given control, in global coordinates. */
	public static Observable<Point> rightClickGlobal(Control ctl) {
		return rightClickLocal(ctl).map(ctl::toDisplay);
	}

	/** Returns an {@link Observable}<{@link Point}> of the right-click mouse-up on the given control, in local coordinates. */
	public static Observable<Point> rightClickLocal(Control ctl) {
		PublishSubject<Point> observable = PublishSubject.create();
		ctl.addListener(SWT.MouseUp, e -> {
			if (e.button == 3) {
				observable.onNext(new Point(e.x, e.y));
			}
		});
		return observable;
	}

	/** Returns an RxBox<String> which contains the content of the text box. */
	public static RxBox<String> textImmediate(Text text) {
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
	public static RxBox<String> textConfirmed(Text text) {
		return textImp(text, SWT.DefaultSelection, SWT.FocusOut);
	}

	private static RxBox<String> textImp(Text text, int... events) {
		RxBox<String> box = RxBox.of(text.getText());
		// set the text when the box changes
		SwtExec.immediate().guardOn(text).subscribe(box, str -> {
			Point selection = text.getSelection();
			text.setText(str);
			text.setSelection(selection);
		});
		// set the box when the text changes
		Listener listener = e -> box.set(text.getText());
		for (int event : events) {
			text.addListener(event, listener);
		}
		return box;
	}

	/** Returns an {@code RxBox<Boolean>} for the toggle state of the given button as an RxBox. */
	public static RxBox<Boolean> toggle(Button btn) {
		Preconditions.checkArgument(SwtMisc.flagIsSet(SWT.TOGGLE, btn) || SwtMisc.flagIsSet(SWT.CHECK, btn));
		RxBox<Boolean> box = RxBox.of(btn.getSelection());
		// update the box when a click happens
		btn.addListener(SWT.Selection, e -> {
			box.set(!box.get());
		});
		// update the button when the box happens
		SwtExec.immediate().guardOn(btn).subscribe(box, btn::setSelection);
		return box;
	}
}
