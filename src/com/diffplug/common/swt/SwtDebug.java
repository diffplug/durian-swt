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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.common.debug.FieldsAndGetters;
import com.diffplug.common.rx.Rx;

/**
 * Helpful utilities for debugging SWT.
 */
public class SwtDebug {
	/** Dumps the given SWT event to System.out. */
	public static void dumpEvent(String name, Event e) {
		dumpEvent(name, e, StringPrinter.systemOut());
	}

	/** Dumps the given SWT event to the given StringPrinter. */
	public static void dumpEvent(String name, Event e, StringPrinter to) {
		// print the name
		to.println(name + ": " + eventType(e));
		// print the non-null / non-zero fields
		FieldsAndGetters.fields(e).filter(entry -> {
			Object value = entry.getValue();
			if (value == null) {
				return false;
			}
			if (value instanceof Number) {
				if (((Number) value).intValue() == 0) {
					return false;
				}
			}
			return true;
		}).forEach(entry -> {
			to.println("\t" + entry.getKey().getName() + " = " + entry.getValue());
		});
		// print the nulls
		to.println("\tnull = " + FieldsAndGetters.fields(e)
				.filter(entry -> entry.getValue() == null)
				.map(entry -> entry.getKey().getName())
				.collect(Collectors.joining(", ")));
		// print the zeroes
		to.println("\t0 = " + FieldsAndGetters.fields(e)
				.filter(entry -> {
					if (entry.getValue() instanceof Number) {
						return ((Number) entry.getValue()).intValue() == 0;
					} else {
						return false;
					}
				})
				.map(entry -> entry.getKey().getName())
				.collect(Collectors.joining(", ")));
	}

	/** Dumps all events from the given widget to System.out. */
	public static void dumpEvents(String name, Widget widget) {
		dumpEvents(name, widget, StringPrinter.systemOut());
	}

	/** Dumps all events from the given widget to the given StringPrinter. */
	public static void dumpEvents(String name, Widget widget, StringPrinter to) {
		dumpEvents(name, widget, to, events.keySet().stream());
	}

	/** Dumps the given events on the widget to the given StringPrinter. */
	public static void dumpEvents(String name, Widget widget, StringPrinter to, Integer... toSubscribe) {
		dumpEvents(name, widget, to, Arrays.asList(toSubscribe));
	}

	/** Dumps the given events on the widget to the given StringPrinter. */
	public static void dumpEvents(String name, Widget widget, StringPrinter to, Collection<Integer> toSubscribe) {
		dumpEvents(name, widget, to, toSubscribe.stream());
	}

	/** Dumps the given events on the widget to the given StringPrinter. */
	public static void dumpEvents(String name, Widget widget, StringPrinter to, Stream<Integer> events) {
		Rx.subscribe(SwtRx.addListener(widget, events), event -> dumpEvent(name, event, to));
	}

	/** Returns the name for the given SWT `event`. */
	public static String eventType(Event event) {
		return eventType(event.type);
	}

	/** Returns the name for the given SWT `eventType`. */
	public static String eventType(int type) {
		String name = events.get(type);
		if (name != null) {
			return name;
		} else {
			return "Unknown event type " + type + ".";
		}
	}

	/** Returns a map from the SWT event code to its name, for all SWT events. */
	public static ImmutableMap<Integer, String> allEvents() {
		return events;
	}

	private static final ImmutableMap<Integer, String> events;

	static {
		ImmutableMap.Builder<Integer, String> builder = ImmutableMap.builder();
		builder.put(SWT.None, "None"); // 0
		builder.put(SWT.KeyDown, "KeyDown"); // 1
		builder.put(SWT.KeyUp, "KeyUp"); // 2
		builder.put(SWT.MouseDown, "MouseDown"); // 3
		builder.put(SWT.MouseUp, "MouseUp"); // 4
		builder.put(SWT.MouseMove, "MouseMove"); // 5
		builder.put(SWT.MouseEnter, "MouseEnter"); // 6
		builder.put(SWT.MouseExit, "MouseExit"); // 7
		builder.put(SWT.MouseDoubleClick, "MouseDoubleClick");// 8
		builder.put(SWT.Paint, "Paint"); // 9
		builder.put(SWT.Move, "Move"); // 10
		builder.put(SWT.Resize, "Resize"); // 11
		builder.put(SWT.Dispose, "Dispose"); // 12
		builder.put(SWT.Selection, "Selection"); // 13
		builder.put(SWT.DefaultSelection, "DefaultSelection");// 14
		builder.put(SWT.FocusIn, "FocusIn"); // 15
		builder.put(SWT.FocusOut, "FocusOut"); // 16
		builder.put(SWT.Expand, "Expand"); // 17
		builder.put(SWT.Collapse, "Collapse"); // 18
		builder.put(SWT.Iconify, "Iconify"); // 19
		builder.put(SWT.Deiconify, "Deiconify"); // 20
		builder.put(SWT.Close, "Close"); // 21
		builder.put(SWT.Show, "Show"); // 22
		builder.put(SWT.Hide, "Hide"); // 23
		builder.put(SWT.Modify, "Modify"); // 24
		builder.put(SWT.Verify, "Verify"); // 25
		builder.put(SWT.Activate, "Activate"); // 26
		builder.put(SWT.Deactivate, "Deactivate"); // 27
		builder.put(SWT.Help, "Help"); // 28
		builder.put(SWT.DragDetect, "DragDetect"); // 29
		builder.put(SWT.Arm, "Arm"); // 30
		builder.put(SWT.Traverse, "Traverse"); // 31
		builder.put(SWT.MouseHover, "MouseHover"); // 32
		builder.put(SWT.HardKeyDown, "HardKeyDown"); // 33
		builder.put(SWT.HardKeyUp, "HardKeyUp"); // 34
		builder.put(SWT.MenuDetect, "MenuDetect"); // 35
		builder.put(SWT.SetData, "SetData"); // 36
		builder.put(SWT.MouseVerticalWheel, "MouseVerticalWheel"); // 37 (MouseWHeel)
		builder.put(SWT.MouseHorizontalWheel, "MouseHorizontalWheel");// 38
		builder.put(SWT.Settings, "Settings"); // 39
		builder.put(SWT.EraseItem, "EraseItem"); // 40
		builder.put(SWT.MeasureItem, "MeasureItem"); // 41
		builder.put(SWT.PaintItem, "PaintItem"); // 42
		builder.put(SWT.ImeComposition, "ImeComposition"); // 43
		builder.put(SWT.OrientationChange, "OrientationChange"); // 44
		builder.put(SWT.Skin, "Skin"); // 45
		builder.put(SWT.OpenDocument, "OpenDocument"); // 46
		builder.put(SWT.Touch, "Touch"); // 47
		builder.put(SWT.Gesture, "Gesture"); // 48
		builder.put(SWT.Segments, "Segments"); // 49
		builder.put(SWT.PreEvent, "Gesture"); // 50
		builder.put(SWT.PostEvent, "PostEvent"); // 51
		@SuppressWarnings("deprecation")
		int sleep = SWT.Sleep;
		builder.put(sleep, "PreExternalEventDispatch"); // 52
		@SuppressWarnings("deprecation")
		int wakeup = SWT.Wakeup;
		builder.put(wakeup, "PostExternalEventDispatch"); // 53
		events = builder.build();
	}
}
