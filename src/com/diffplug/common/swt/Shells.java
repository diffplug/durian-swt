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

import java.util.Map;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import rx.Subscription;
import rx.subscriptions.BooleanSubscription;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/** A fluent builder for creating SWT {@link Shell}s. */
public class Shells {
	private final Coat coat;
	private final int style;
	private String title = "";
	private Image image;
	private Point size;
	private Map.Entry<Corner, Point> location = null;

	private Shells(int style, Coat coat) {
		this.style = style;
		this.coat = coat;
	}

	/** Creates a new Shells for this Coat. */
	public static Shells builder(int style, Coat coat) {
		return new Shells(style, coat);
	}

	/** Sets the title for this Shell. */
	public Shells setTitle(String title) {
		this.title = title;
		return this;
	}

	/** Sets the title image for this Shell. */
	public Shells setImage(Image image) {
		this.image = image;
		return this;
	}

	/**
	 * Sets the size for this Shell.
	 * <ul>
	 * <li>If {@code size} is null, the shell will be packed as tightly as possible.</li>
	 * <li>If both components are {@code > 0}, the shell will be set to that size.</li>
	 * <li>If <i>one</i> component is {@code <= 0}, the positive dimension will be constrained and the other dimension will be packed as tightly as possible.</li>
	 * <li>If both components are {@code <= 0}, you'll get an {@code IllegalArgumentException}.</li>
	 * </ul>
	 * @throws IllegalArgumentException if size is non-null and both components are negative
	 */
	public Shells setSize(Point size) {
		if (size != null && size.x <= 0 && size.y <= 0) {
			throw new IllegalArgumentException("Size must either be null or have at least one positive dimension, this was: " + size);
		}
		this.size = size;
		return this;
	}

	/** @see #setSize(Point) */
	public Shells setSize(int x, int y) {
		return setSize(new Point(x, y));
	}

	/**
	 * Sets the absolute location of the top-left of this shell. If the value
	 * is null, the shell will open:
	 * <ul>
	 * <li>if there is a parent shell, below and to the right of the parent</li>
	 * <li>if there isn't a parent shell, at the current cursor position</li>
	 * </ul>
	 */
	public Shells setLocation(Point openPosition) {
		return setLocation(Corner.TOP_LEFT, openPosition);
	}

	/**
	 * Sets the absolute location of the the given corner of this shell. If the value
	 * is null, the shell will open:
	 * <ul>
	 * <li>if there is a parent shell, below and to the right of the parent</li>
	 * <li>if there isn't a parent shell, at the current cursor position</li>
	 * </ul>
	 */
	public Shells setLocation(Corner corner, Point position) {
		this.location = Maps.immutableEntry(Objects.requireNonNull(corner), Objects.requireNonNull(position));
		return this;
	}

	/** Opens the shell on this parent shell. */
	public Shell openOn(Shell parent) {
		Preconditions.checkNotNull(parent);
		Shell shell = new Shell(parent, style);
		if (location == null) {
			Point parentPos = shell.getParent().getLocation();
			int SHELL_MARGIN = SwtMisc.systemFontHeight();
			parentPos.x += SHELL_MARGIN;
			parentPos.y += SHELL_MARGIN;
			location = Maps.immutableEntry(Corner.TOP_LEFT, parentPos);
		}
		setupShell(shell);
		return shell;
	}

	/** Opens the shell on this parent and blocks. */
	public void openOnAndBlock(Shell parent) {
		SwtMisc.loopUntilDisposed(openOn(parent));
	}

	/** Opens the shell on the currently active shell. */
	public Shell openOnActive() {
		Display display = SwtMisc.assertUI();
		Shell parent = display.getActiveShell();
		// on Windows and OS X, the active shell is the one that currently has user focus
		// on Linux, the last created shell (even if it is invisible) will count as the active shell
		//
		// This is a problem because some things create a fake hidden shell to act as a parent for other
		// operations (specifically our right-click infrastructure). This means that on linux, the user
		// right-clicks, a fake shell is created to show a menu, the selected action opens a new shell
		// which uses "openOnActive", then the menu closes and disposes its fake shell, which promptly
		// closes the newly created shell.
		//
		// as a workaround, if an active shell is found, but it isn't visible, we count that as though
		// there isn't an active shell
		Shell shell;
		if (parent == null || parent.isVisible() == false) {
			shell = new Shell(display, style);
		} else {
			shell = new Shell(parent, style);
		}
		if (location == null) {
			location = Maps.immutableEntry(Corner.CENTER, display.getCursorLocation());
		}
		setupShell(shell);
		return shell;
	}

	/** Opens the shell on the currently active shell and blocks. */
	public void openOnActiveBlocking() {
		SwtMisc.loopUntilDisposed(openOnActive());
	}

	/** Opens the shell as a root shell. */
	public Shell openOnDisplay() {
		Display display = SwtMisc.assertUI();
		if (location == null) {
			location = Maps.immutableEntry(Corner.CENTER, display.getCursorLocation());
		}
		Shell shell = new Shell(display, style);
		setupShell(shell);
		return shell;
	}

	/** Opens the shell as a root shell and blocks. */
	public void openOnDisplayBlocking() {
		SwtMisc.loopUntilDisposed(openOnDisplay());
	}

	private void setupShell(Shell shell) {
		// set the text and image
		if (title != null) {
			shell.setText(title);
		}
		if (image != null) {
			shell.setImage(image);
		}
		// disable close on ESCAPE
		shell.addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				e.doit = false;
			}
		});
		// find the composite we're going to draw on
		Composite userCmp;
		if (size == null) {
			userCmp = shell;
			size = new Point(0, 0);
		} else {
			// if there's a specific size, then we'll create a fake one
			// and set its size appropriately
			Layouts.setGrid(shell).margin(0);
			userCmp = new Composite(shell, SWT.NONE);
			LayoutsGridData gdUtil = Layouts.setGridData(userCmp).grabAll();
			if (size.x > 0) {
				gdUtil.widthHint(size.x);
			}
			if (size.y > 0) {
				gdUtil.heightHint(size.y);
			}
		}
		// draw the composite
		coat.putOn(userCmp);
		shell.pack(true);

		// set the opening position
		Rectangle bounds = shell.getBounds();
		Point topLeft = location.getKey().topLeftRequiredFor(bounds, location.getValue());

		// constrain the position by the Display's bounds
		Rectangle monitorBounds = shell.getDisplay().getBounds();
		topLeft.x = Math.max(topLeft.x, monitorBounds.x);
		topLeft.y = Math.max(topLeft.y, monitorBounds.y);
		topLeft.x = Math.min(topLeft.x + bounds.x, monitorBounds.x + monitorBounds.width) - bounds.x;
		topLeft.y = Math.min(topLeft.y + bounds.y, monitorBounds.y + monitorBounds.height) - bounds.y;

		// set the location and open it up!
		shell.setLocation(topLeft);
		shell.open();
	}

	/** Prevents the given shell from closing without prompting.  Returns a Subscription which can cancel this blocking. */
	public static Subscription confirmClose(Shell shell, String title, String question, Runnable runOnClose) {
		Listener listener = e -> {
			e.doit = SwtMisc.blockForQuestion(title, question, shell);
			if (e.doit) {
				runOnClose.run();
			}
		};
		shell.addListener(SWT.Close, listener);
		return BooleanSubscription.create(() -> {
			SwtExec.immediate().guardOn(shell).execute(() -> {
				shell.removeListener(SWT.Close, listener);
			});
		});
	}
}
