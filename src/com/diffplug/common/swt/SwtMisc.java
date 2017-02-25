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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.Lists;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.swt.os.OS;
import com.diffplug.common.tree.TreeDef;
import com.diffplug.common.tree.TreeIterable;
import com.diffplug.common.tree.TreeQuery;
import com.diffplug.common.tree.TreeStream;
import com.diffplug.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Miscellaneous SWT functions. */
public class SwtMisc {
	/////////////////////
	// True miscellany //
	/////////////////////
	/** Returns true if `flag` is set in `style`. */
	public static boolean flagIsSet(int flag, int style) {
		return (style & flag) == flag;
	}

	/** Returns true if `flag` is set in the style value of `widget`. */
	public static boolean flagIsSet(int flag, Widget widget) {
		return flagIsSet(flag, widget.getStyle());
	}

	/** Sets whether the flag is set for the given style. */
	public static int setFlag(int flag, boolean isSet, int style) {
		return isSet ? (style | flag) : (style & ~flag);
	}

	/** Converts a {@link Runnable} into a {@link Listener}. */
	public static Listener asListener(Runnable runnable) {
		return e -> runnable.run();
	}

	/** Returns the Display instance, asserting that the method was called from the UI thread. */
	public static Display assertUI() {
		// returns the system display, creating it if necessary
		synchronized (Device.class) {
			// Display.getDefault() and display.getThread() both synchronize on
			// Device.class.  By synchronizing ourselves, we minimize contention
			Display display = Display.getDefault();
			Preconditions.checkArgument(display.getThread() == Thread.currentThread(), "Must be called only from UI thread");
			return display;
		}
	}

	/** Asserts that the user didn't call this from the UI thread. */
	public static void assertNotUI() {
		Display current = Display.getCurrent();
		Preconditions.checkArgument(current == null, "Must not be called from the UI thread.");
	}

	/** Returns the given system color. */
	public static Color getSystemColor(int code) {
		return assertUI().getSystemColor(code);
	}

	/**
	 * @deprecated As of SWT 4.6 (Neon), this functionality is {@link Control#requestLayout()}.
	 */
	@Deprecated
	public static void requestLayout(Control control) {
		if (control instanceof Shell) {
			((Shell) control).layout(null, SWT.DEFER);
		} else {
			control.getShell().layout(new Control[]{control}, SWT.DEFER);
		}
	}

	/**
	 * Performs an asynchronous layout on the given composite.
	 * <p>
	 * Oftentimes, a layout will not be successful unless it is performed in
	 * a {@link Display#asyncExec(Runnable)} call, because the current list of events must be
	 * processed before the layout can take place. 
	 */
	public static void asyncLayout(Composite cmp) {
		SwtExec.async().guardOn(cmp).execute(() -> cmp.layout(true, true));
	}

	/**
	 * Performs an asynchronous layout on the given composite anytime that it is resized.
	 * <p>
	 * This can often fix graphical glitches with resize-to-fit layouts, such as a `TableColumnLayout`.
	 */
	public static void asyncLayoutOnResize(Composite cmp) {
		cmp.addListener(SWT.Resize, e -> asyncLayout(cmp));
		// trigger the first one by hand
		asyncLayout(cmp);
	}

	/** Disposes all children of the given composite, and sets the layout to null. */
	public static void clean(Composite cmp) {
		for (Control child : cmp.getChildren()) {
			child.dispose();
		}
		cmp.setLayout(null);
	}

	/** Throws an {@link IllegalArgumentException} iff the given `Composite` has any children or a non-null layout. */
	public static void assertClean(Composite cmp) {
		Preconditions.checkArgument(cmp.getChildren().length == 0, "The composite should have no children, this had %s.", cmp.getChildren().length);
		Preconditions.checkArgument(cmp.getLayout() == null, "The composite should have no layout, this had %s.", cmp.getLayout());
	}

	/** Returns a deep copy of the given SWT event. */
	public static Event copyEvent(Event event) {
		Event copy = new Event();
		copy.display = event.display;
		copy.widget = event.widget;
		copy.type = event.type;
		copy.detail = event.detail;
		copy.item = event.item;
		copy.index = event.index;
		copy.gc = event.gc;
		copy.x = event.x;
		copy.y = event.y;
		copy.width = event.width;
		copy.height = event.height;
		copy.count = event.count;
		copy.time = event.time;
		copy.button = event.button;
		copy.character = event.character;
		copy.keyCode = event.keyCode;
		copy.keyLocation = event.keyLocation;
		copy.stateMask = event.stateMask;
		copy.start = event.start;
		copy.end = event.end;
		copy.text = event.text;
		copy.segments = event.segments;
		copy.segmentsChars = event.segmentsChars;
		copy.doit = event.doit;
		copy.data = event.data;
		copy.touches = event.touches;
		copy.xDirection = event.xDirection;
		copy.yDirection = event.yDirection;
		copy.magnification = event.magnification;
		copy.rotation = event.rotation;
		return copy;
	}

	/** Runs some function using a temporary GC. */
	public static void withGcRun(Consumer<GC> consumer) {
		withGcCompute(gc -> {
			consumer.accept(gc);
			return null;
		});
	}

	/** Computes some function using a temporary GC. */
	public static <T> T withGcCompute(Function<GC, T> function) {
		// create a tiny image to bind our GC to (not that it can't be size 0)
		Image dummyImg = new Image(assertUI(), 1, 1);
		GC gc = new GC(dummyImg);
		try {
			return function.apply(gc);
		} finally {
			gc.dispose();
			dummyImg.dispose();
		}
	}

	////////////////////////////////////////
	// Run the SWT display loop until ... //
	////////////////////////////////////////
	/** Runs the display loop until the given `Predicate<Display>` returns false. */
	public static void loopUntil(Predicate<Display> until) {
		Display display = assertUI();
		while (!until.test(display)) {
			try {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			} catch (Throwable e) {
				Errors.log().accept(e);
			}
		}
	}

	/** Runs the display loop until the `widget` has been disposed. */
	public static void loopUntilDisposed(Widget widget) {
		loopUntil(display -> widget.isDisposed());
	}

	/** Runs the display loop until the given future has returned. */
	public static <T> T loopUntilGet(ListenableFuture<T> future) throws Throwable {
		Box.Nullable<T> result = Box.Nullable.ofNull();
		Box.Nullable<Throwable> error = Box.Nullable.ofNull();
		Rx.subscribe(future, Rx.onValueOnFailure(result::set, error::set));

		loopUntil(display -> future.isDone());

		if (error.get() != null) {
			throw error.get();
		} else {
			return result.get();
		}
	}

	/** Runs the display loop until the given future has returned. */
	@SuppressWarnings("unchecked")
	public static <T, E extends Throwable> T loopUntilGetChecked(ListenableFuture<T> future, Class<E> clazz) throws E {
		try {
			return loopUntilGet(future);
		} catch (Throwable error) {
			if (clazz.isAssignableFrom(error.getClass())) {
				throw (E) error;
			} else {
				throw Errors.asRuntime(error);
			}
		}
	}

	/** Runs the display loop until the given future has returned. */
	public static <T> T loopUntilGet(CompletionStage<T> future) throws Throwable {
		Box.Nullable<T> result = Box.Nullable.ofNull();
		Box.Nullable<Throwable> error = Box.Nullable.ofNull();
		Rx.subscribe(future, Rx.onValueOnFailure(result::set, error::set));

		CompletableFuture<?> actualFuture = future.toCompletableFuture();
		loopUntil(display -> actualFuture.isDone());

		if (error.get() != null) {
			throw error.get();
		} else {
			return result.get();
		}
	}

	/** Runs the display loop until the given future has returned. */
	@SuppressWarnings("unchecked")
	public static <T, E extends Throwable> T loopUntilGetChecked(CompletionStage<T> future, Class<E> clazz) throws E {
		try {
			return loopUntilGet(future);
		} catch (Throwable error) {
			if (clazz.isAssignableFrom(error.getClass())) {
				throw (E) error;
			} else {
				throw Errors.asRuntime(error);
			}
		}
	}

	////////////////////////////////////////
	// Thread-safe blocking notifications //
	////////////////////////////////////////
	/** Blocks to notify about a success. Can be called from any thread. */
	public static void blockForSuccess(String title, String msg, @Nullable Shell parent) {
		blockForMessageBox(title, msg, parent, SWT.ICON_INFORMATION | SWT.OK);
	}

	/** Blocks to notify about a success. Can be called from any thread. */
	public static void blockForSuccess(String title, String msg) {
		blockForSuccess(title, msg, null);
	}

	/** Blocks to notify about an error. Can be called from any thread. */
	public static void blockForError(String title, String msg, @Nullable Shell parent) {
		blockForMessageBox(title, msg, parent, SWT.ICON_ERROR | SWT.OK);
	}

	/** Blocks to notify about an error. Can be called from any thread. */
	public static void blockForError(String title, String msg) {
		blockForError(title, msg, null);
	}

	/** Blocks to ask a yes/no question.  Can be called from any thread. */
	public static boolean blockForQuestion(String title, String message, @Nullable Shell parent) {
		return blockForMessageBox(title, message, parent, SWT.ICON_QUESTION | SWT.YES | SWT.NO) == SWT.YES;
	}

	/** Blocks to ask a yes/no question.  Can be called from any thread. */
	public static boolean blockForQuestion(String title, String message) {
		return blockForQuestion(title, message, null);
	}

	/** Blocks to ask an Ok/Cancel question. Can be called from any thread. */
	public static boolean blockForOkCancel(String title, String message, @Nullable Shell parent) {
		return blockForMessageBox(title, message, parent, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL) == SWT.OK;
	}

	/** Blocks to ask an Ok/Cancel question. Can be called from any thread. */
	public static boolean blockForOkCancel(String title, String message) {
		return blockForOkCancel(title, message, null);
	}

	/**
	 * Opens a message box with the given title, message, and style and returns its result.
	 * Can be called from any thread.
	 * 
	 * @param title The title of the box.
	 * @param message The message in the box.
	 * @param parent The parent shell. Null means it will be on the current active shell.
	 * @param style An OR'ed combination of SWT.YES/NO, SWT.OK/CANCEL, and SWT.ICON_*
	 * @return The button that was pressed (e.g. SWT.YES, SWT.OK, etc)
	 */
	public static int blockForMessageBox(String title, String message, @Nullable Shell parent, int style) {
		return SwtExec.blocking().get(() -> {
			Display display = assertUI();
			Shell parentInternal;

			if (parent == null) {
				// null equals display.getActiveShell()
				parentInternal = display.getActiveShell();
			} else {
				parentInternal = parent;
			}

			boolean parentWasNull = parentInternal == null;
			if (parentWasNull) {
				// if there wasn't an active shell, we'll create one, because
				// MessageBox requires a parent
				parentInternal = new Shell(Display.getCurrent(), SWT.APPLICATION_MODAL);
			}
			MessageBox questionBox = new MessageBox(parentInternal, style);
			questionBox.setMessage(message);
			questionBox.setText(title);
			int result = questionBox.open();
			if (parentWasNull) {
				// if we made an invisible parent, clean it up afterwards
				parentInternal.dispose();
			}
			return result;
		});
	}

	/**
	 * Opens a message box with the given title, message, and style and returns its result.
	 * Can be called from any thread.
	 * 
	 * @param title The title of the box.
	 * @param message The message in the box.
	 * @param style An OR'ed combination of SWT.YES/NO, SWT.OK/CANCEL, and SWT.ICON_*
	 * @return The button that was pressed (e.g. SWT.YES, SWT.OK, etc)
	 */
	public static int blockForMessageBox(String title, String message, int style) {
		return blockForMessageBox(title, message, null, style);
	}

	///////////////////
	// Scale by font //
	///////////////////
	/** The cached height of the system font. */
	static int systemFontHeight = 0;
	static int systemFontWidth = 0;

	/** Populates the height and width of the system font. */
	private static void populateSystemFont() {
		// create a tiny image to bind our GC to (not that it can't be size 0)
		Image dummyImg = new Image(assertUI(), 1, 1);
		GC gc = new GC(dummyImg);

		FontMetrics metrics = gc.getFontMetrics();
		systemFontHeight = metrics.getHeight();
		systemFontWidth = metrics.getAverageCharWidth();
		if (OS.getNative().isMac()) {
			// add 20% width on Mac
			systemFontWidth = (systemFontWidth * 12) / 10;
		}

		gc.dispose();
		dummyImg.dispose();
	}

	/** Returns the height of the system font. */
	public static int systemFontHeight() {
		if (systemFontHeight == 0) {
			populateSystemFont();
		}
		return systemFontHeight;
	}

	/** Returns the width of the system font. */
	public static int systemFontWidth() {
		if (systemFontWidth == 0) {
			populateSystemFont();
		}
		return systemFontWidth;
	}

	/** Returns a distance which is a snug fit for a line of text in the system font. */
	public static int systemFontSnug() {
		return systemFontHeight() + Layouts.defaultMargin();
	}

	/** Returns the default width of a button, scaled for the system font. */
	public static int defaultButtonWidth() {
		return systemFontWidth() * "   Cancel   ".length();
	}

	/** Returns the default width of a dialog. */
	public static int defaultDialogWidth() {
		return 50 * systemFontWidth();
	}

	/** Returns a size which is scaled by the system font's height. */
	public static Point scaleByFontHeight(int cols, int rows) {
		return new Point(cols * systemFontHeight(), rows * systemFontHeight());
	}

	/** Returns a dimension which is scaled by the system font's height. */
	public static int scaleByFontHeight(int rows) {
		return rows * systemFontHeight();
	}

	/** Returns a point that represents the size of a (cols x rows) grid of characters printed in the standard system font. */
	public static Point scaleByFont(int cols, int rows) {
		return new Point(cols * systemFontWidth(), rows * systemFontHeight());
	}

	/** Returns a dimension which is guaranteed to be comfortable for the given string. */
	public static Point scaleByFont(String str) {
		List<String> lines = splitLines(str);
		int maxLength = lines.stream().mapToInt(String::length).max().getAsInt();
		return scaleByFont(maxLength + 2, lines.size());
	}

	/** Splits a string into a list of lines. Null returns an empty list. */
	private static List<String> splitLines(String orig) {
		if (orig == null) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(orig.replace("\\r\\n", "\n").split("\\n"));
		}
	}

	/////////////////////
	// Geometric stuff //
	/////////////////////
	/** Returns the bounds of the given control in global coordinates. */
	public static Rectangle globalBounds(Control control) {
		Point size = control.getSize();
		Point topLeft = control.toDisplay(0, 0);
		return new Rectangle(topLeft.x, topLeft.y, size.x, size.y);
	}

	/** Converts a rectangle to global coordinates using the given control as a reference frame. */
	public static Rectangle toDisplay(Control control, Rectangle rect) {
		Point topLeft = control.toDisplay(rect.x, rect.y);
		return new Rectangle(topLeft.x, topLeft.y, rect.width, rect.height);
	}

	/** Returns the monitor (if any) which contains the given point. */
	public static Optional<Monitor> monitorFor(Point p) {
		Monitor[] monitors = assertUI().getMonitors();
		for (Monitor monitor : monitors) {
			Rectangle bounds = monitor.getBounds();
			if (bounds.contains(p)) {
				return Optional.of(monitor);
			}
		}
		return Optional.empty();
	}

	//////////////////////
	// Tree-based stuff //
	//////////////////////
	/** Sets the enabled status of every child, grandchild, etc. of the given composite.  Skips plain-jane Composites. */
	public static void setEnabledDeep(Composite root, boolean enabled) {
		TreeStream.depthFirst(treeDefControl(), root)
				// skip plain-jane Composites
				.filter(ctl -> ctl.getClass().equals(Composite.class))
				// set the enabled flag
				.forEach(ctl -> ctl.setEnabled(enabled));
	}

	/** Calls the given consumer on the given composite and all of its children, recursively. */
	public static void forEachDeep(Composite root, Consumer<Control> ctlSetter) {
		TreeIterable.depthFirst(treeDefControl(), root).forEach(ctlSetter);
	}

	/** Sets the foreground color of the given composite and all of its children, recursively. */
	public static void setForegroundDeep(Composite root, Color foreground) {
		forEachDeep(root, ctl -> ctl.setForeground(foreground));
	}

	/** Sets the background color of the given composite and all of its children, recursively. */
	public static void setBackgroundDeep(Composite root, Color background) {
		forEachDeep(root, ctl -> ctl.setBackground(background));
	}

	/** Returns the root shell of the given control. */
	public static Shell rootShell(Control ctl) {
		Shell shell;
		if (ctl instanceof Shell) {
			shell = (Shell) ctl;
		} else {
			shell = ctl.getShell();
		}
		return TreeQuery.root(SwtMisc.treeDefShell(), shell);
	}

	/** {@link TreeDef} for {@link Composite}s. */
	public static TreeDef.Parented<Composite> treeDefComposite() {
		return COMPOSITE_TREE_DEF;
	}

	/** {@link TreeDef} for {@link Control}s. */
	public static TreeDef.Parented<Control> treeDefControl() {
		return CONTROL_TREE_DEF;
	}

	/** {@link TreeDef} for {@link Shell}s. */
	public static TreeDef.Parented<Shell> treeDefShell() {
		return SHELL_TREE_DEF;
	}

	private static final TreeDef.Parented<Composite> COMPOSITE_TREE_DEF = new TreeDef.Parented<Composite>() {
		@Override
		public List<Composite> childrenOf(Composite node) {
			Control[] rawChildren = node.getChildren();
			List<Composite> children = Lists.newArrayListWithCapacity(rawChildren.length);
			for (Control child : rawChildren) {
				if (child instanceof Composite) {
					children.add((Composite) child);
				}
			}
			return children;
		}

		@Override
		public Composite parentOf(Composite node) {
			return node.getParent();
		}
	};

	private static final TreeDef.Parented<Control> CONTROL_TREE_DEF = new TreeDef.Parented<Control>() {
		@Override
		public List<Control> childrenOf(Control node) {
			if (node instanceof Composite) {
				return Arrays.asList(((Composite) node).getChildren());
			} else {
				return Collections.emptyList();
			}
		}

		@Override
		public Control parentOf(Control node) {
			return node.getParent();
		}
	};

	private static final TreeDef.Parented<Shell> SHELL_TREE_DEF = new TreeDef.Parented<Shell>() {
		@Override
		public List<Shell> childrenOf(Shell node) {
			return Arrays.asList(node.getShells());
		}

		@SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "Parent of a shell is guaranteed to be either a Shell or null.")
		@Override
		public Shell parentOf(Shell node) {
			return (Shell) node.getParent();
		}
	};
}
