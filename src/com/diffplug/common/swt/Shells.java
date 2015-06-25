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

/** A fluent builder for creating SWT {@link Shell}s. */
public class Shells {
	private final Coat coat;
	private final int style;
	private String title = "";
	private Image image;
	private Point size;
	private Point openPosition;

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

	/** Opens the shell on this parent shell. */
	public Shell openOn(Shell parent) {
		Preconditions.checkNotNull(parent);
		Shell shell = new Shell(parent, style);
		setupShell(shell);
		return shell;
	}

	/** Opens the shell on this parent and blocks. */
	public void openOnAndBlock(Shell parent) {
		SwtMisc.loopUntilDisposed(openOn(parent));
	}

	/** Opens the shell on the currently active shell. */
	public Shell openOnActive() {
		Shell shell;
		Shell parent = Display.getCurrent().getActiveShell();
		if (parent == null) {
			shell = new Shell(Display.getCurrent(), style);
		} else {
			shell = new Shell(parent, style);
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
		Shell shell = new Shell(Display.getCurrent(), style);
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

		// set the opening position
		if (openPosition == null) {
			if (shell.getParent() != null) {
				openPosition = shell.getParent().getLocation();
				final int SHELL_MARGIN = SwtMisc.systemFontHeight();
				openPosition.x += SHELL_MARGIN;
				openPosition.y += SHELL_MARGIN;
			} else {
				openPosition = Display.getCurrent().getCursorLocation();
			}
		}
		shell.pack(true);

		// constrain the position by the Display's bounds
		Rectangle bounds = Display.getCurrent().getBounds();
		openPosition.x = Math.max(openPosition.x, bounds.x);
		openPosition.y = Math.max(openPosition.y, bounds.y);
		openPosition.x = Math.min(openPosition.x + size.x, bounds.x + bounds.width) - size.x;
		openPosition.y = Math.min(openPosition.y + size.y, bounds.y + bounds.height) - size.y;

		// set the location and open it up!
		shell.setLocation(openPosition);
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
