/*
 * Copyright 2018 DiffPlug
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.Maps;
import com.diffplug.common.swt.os.WS;
import com.diffplug.common.tree.TreeIterable;
import com.diffplug.common.tree.TreeQuery;
import com.diffplug.common.tree.TreeStream;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

/** A fluent builder for creating SWT {@link Shell}s. */
public class Shells {
	private final Coat coat;
	private final int style;
	private String title = "";
	private Image image;
	private int alpha = SWT.DEFAULT;
	private final Point size = new Point(SWT.DEFAULT, SWT.DEFAULT);
	private boolean positionIncludesTrim = true;
	private Map.Entry<Corner, Point> location = null;
	private boolean dontOpen = false;
	private boolean closeOnEscape = false;

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

	/** Sets the alpha for this Shell. */
	public Shells setAlpha(int alpha) {
		this.alpha = alpha;
		return this;
	}

	/**
	 * Sets the full bounds for this shell.
	 * Delegates to {@link #setRectangle(Rectangle)} and {@link #setPositionIncludesTrim(true)}.
	 */
	public Shells setBounds(Rectangle bounds) {
		setRectangle(bounds);
		setPositionIncludesTrim(true);
		return this;
	}

	/** Calls {@link #setBounds(Rectangle)} to match this control. */
	public Shells setBounds(Control control) {
		return setBounds(SwtMisc.globalBounds(control));
	}

	/** Calls {@link #setBounds(Rectangle)} to match this control. */
	public Shells setBounds(ControlWrapper wrapper) {
		return setBounds(SwtMisc.globalBounds(wrapper.getRootControl()));
	}

	/**
	 * Calls {@link #setLocation(Point)} and {@link #setSize(Point)} in one line.
	 */
	public Shells setRectangle(Rectangle rect) {
		return setLocation(new Point(rect.x, rect.y)).setSize(new Point(rect.width, rect.height));
	}

	/**
	 * Sets the size for this Shell.
	 * <ul>
	 * <li>If `size` is null, or both components are `<= 0`, the shell will be packed as tightly as possible.</li>
	 * <li>If both components are `> 0`, the shell will be set to that size.</li>
	 * <li>If <i>one</i> component is `<= 0`, the positive dimension will be constrained and the other dimension will be packed as tightly as possible.</li>
	 * </ul>
	 * @throws IllegalArgumentException if size is non-null and both components are negative
	 */
	public Shells setSize(@Nullable Point size) {
		if (size == null) {
			this.size.x = SWT.DEFAULT;
			this.size.y = SWT.DEFAULT;
		} else {
			setSize(size.x, size.y);
		}
		return this;
	}

	/** @see #setSize(Point) */
	public Shells setSize(int x, int y) {
		this.size.x = sanitizeToDefault(x);
		this.size.y = sanitizeToDefault(y);
		return this;
	}

	private static int sanitizeToDefault(int val) {
		return val > 0 ? val : SWT.DEFAULT;
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

	/**
	 * If true, size and location will set the the "outside" of the Shell - including the trim.
	 * If false, it will set the "inside" of the Shell - not including the trim.
	 * Default value is true.
	 */
	public Shells setPositionIncludesTrim(boolean positionIncludesTrim) {
		this.positionIncludesTrim = positionIncludesTrim;
		return this;
	}

	/**
	 * If true, the "openOn" methods will create the shell but not actually open them.
	 * This is rare and a little awkward, might get changed someday: https://github.com/diffplug/durian-swt/issues/4
	 */
	public Shells setDontOpen(boolean dontOpen) {
		this.dontOpen = dontOpen;
		return this;
	}

	/**
	 * Determines whether the shell will close on escape, defaults to false.
	 */
	public Shells setCloseOnEscape(boolean closeOnEscape) {
		this.closeOnEscape = closeOnEscape;
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

	/**
	 * @deprecated for {@link #openOnBlocking(Shell)} - same behavior, but name is consistent with the others.
	 */
	@Deprecated
	public void openOnAndBlock(Shell parent) {
		openOnBlocking(parent);
	}

	/** Opens the shell on this parent and blocks. */
	public void openOnBlocking(Shell parent) {
		Preconditions.checkArgument(!dontOpen);
		SwtMisc.loopUntilDisposed(openOn(parent));
	}

	/**
	 * Returns the active shell using the following logic:
	 * 
	 * - the active shell needs to be visible, and it can't be a temporary pop-up (it needs to have a toolbar)
	 * - if it's invisible or temporary, we trust its top-left position as the "user position"
	 * - if there's no shell at all, we use the mouse cursor as the "user position"
	 * - we iterate over every shell, and find the ones that are underneath the "user position"
	 * - of the candidate shells, we return the one which is nested the deepest
	 * 
	 * on Windows and OS X, the active shell is the one that currently has user focus
	 * on Linux, the last created shell (even if it is invisible) will count as the active shell
	 *
	 * This is a problem because some things create a fake hidden shell to act as a parent for other
	 * operations (specifically our right-click infrastructure). This means that on linux, the user
	 * right-clicks, a fake shell is created to show a menu, the selected action opens a new shell
	 * which uses "openOnActive", then the menu closes and disposes its fake shell, which promptly
	 * closes the newly created shell.
	 *
	 * as a workaround, if an active shell is found, but it isn't visible, we count that as though
	 * there isn't an active shell
	 *
	 * we have a similar workaround for no-trim ON_TOP shells, which are commonly used for
	 * context-sensitive popups which may close soon after
	 */
	public static Shell active() {
		Display display = SwtMisc.assertUI();
		Shell active = display.getActiveShell();

		Point activeLocation;
		if (active == null) {
			activeLocation = display.getCursorLocation();
		} else {
			if (isValidActiveShell(active)) {
				return active;
			} else {
				activeLocation = active.getLocation();
			}
		}
		// we now have the location of the cursor, all we have to do is find which shell is underneath it

		// first we'll look at the direct ancestors of the active shell
		if (active != null) {
			Optional<Shell> validParentOfActive = TreeStream.toParent(SwtMisc.treeDefShell(), active)
					.filter(Shells::isValidActiveShell)
					.filter(shell -> shell.getBounds().contains(activeLocation))
					.findFirst();
			if (validParentOfActive.isPresent()) {
				return validParentOfActive.get();
			}
		}

		// then we'll look at every valid shell
		List<Shell> shellsUnderActiveLocation = new ArrayList<>();
		Shell[] shells = display.getShells();
		for (Shell rootShell : shells) {
			// only look at valid shells
			for (Shell shell : TreeIterable.breadthFirst(SwtMisc.treeDefShell()
					.filter(Shells::isValidActiveShell), rootShell)) {
				if (shell.getBounds().contains(activeLocation)) {
					shellsUnderActiveLocation.add(shell);
				}
			}
		}
		if (shellsUnderActiveLocation.isEmpty()) {
			return null;
		} else if (shellsUnderActiveLocation.size() == 1) {
			return shellsUnderActiveLocation.get(0);
		}
		// otherwise, we prefer the deepest shell
		Comparator<Shell> byDepth = Comparator.comparingInt(shell -> {
			return TreeQuery.toRoot(SwtMisc.treeDefShell(), shell).size();
		});
		return Collections.max(shellsUnderActiveLocation, byDepth);
	}

	private static boolean isValidActiveShell(Shell shell) {
		return shell.isVisible() && SwtMisc.flagIsSet(SWT.TITLE, shell);
	}

	/** Opens the shell on the currently active shell. */
	public Shell openOnActive() {
		Display display = SwtMisc.assertUI();
		Shell shell;
		Shell parent = Shells.active();
		if (parent == null) {
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

	private static final Map.Entry<Corner, Point> SENTINEL_DONT_SET_POSITION_OR_SIZE = Maps.immutableEntry(null, null);

	/** Prevents setting any size or position.  Does not prevent changing the location and size to ensure that the shell is on-screen. */
	public Shells dontSetPositionOrSize() {
		location = SENTINEL_DONT_SET_POSITION_OR_SIZE;
		return this;
	}

	/** Opens the shell on the currently active shell and blocks. */
	public void openOnActiveBlocking() {
		Preconditions.checkArgument(!dontOpen);
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
		Preconditions.checkArgument(!dontOpen);
		SwtMisc.loopUntilDisposed(openOnDisplay());
	}

	private void setupShell(Shell shell) {
		// set the text, image, and alpha
		if (title != null) {
			shell.setText(title);
		}
		if (image != null && WS.getRunning() != WS.COCOA) {
			shell.setImage(image);
		}
		if (alpha != SWT.DEFAULT) {
			shell.setAlpha(alpha);
		}
		// disable close on ESCAPE
		if (!closeOnEscape) {
			shell.addListener(SWT.Traverse, e -> {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
				}
			});
		}
		// find the composite we're going to draw on
		coat.putOn(shell);

		Rectangle bounds;
		if (location == SENTINEL_DONT_SET_POSITION_OR_SIZE) {
			bounds = shell.getBounds();
		} else {
			if (positionIncludesTrim) {
				Point computedSize;
				if (size.x == SWT.DEFAULT ^ size.y == SWT.DEFAULT) {
					// if we're specifying only one side or the other,
					// then we need to adjust for the trim
					Rectangle trimFor100x100 = shell.computeTrim(100, 100, 100, 100);
					int dwidth = trimFor100x100.width - 100;
					int dheight = trimFor100x100.height - 100;
					int widthHint = size.x == SWT.DEFAULT ? SWT.DEFAULT : size.x - dwidth;
					int heightHint = size.y == SWT.DEFAULT ? SWT.DEFAULT : size.y - dheight;
					computedSize = shell.computeSize(widthHint, heightHint);
				} else {
					if (size.x == SWT.DEFAULT) {
						// we're packing as tight as can
						Preconditions.checkState(size.y == SWT.DEFAULT);
						computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
					} else {
						// the size is specified completely
						Preconditions.checkState(size.y != SWT.DEFAULT);
						computedSize = size;
					}
				}
				Point topLeft = location.getKey().topLeftRequiredFor(new Rectangle(0, 0, computedSize.x, computedSize.y), location.getValue());
				bounds = new Rectangle(topLeft.x, topLeft.y, computedSize.x, computedSize.y);
			} else {
				Point computedSize = shell.computeSize(size.x, size.y, true);
				Point topLeft = location.getKey().topLeftRequiredFor(new Rectangle(0, 0, computedSize.x, computedSize.y), location.getValue());
				bounds = shell.computeTrim(topLeft.x, topLeft.y, computedSize.x, computedSize.y);
			}
		}

		// constrain the position by the Display's bounds (getClientArea() takes the Start bar into account)
		Rectangle monitorBounds = SwtMisc.monitorFor(Corner.CENTER.getPosition(bounds)).orElse(SwtMisc.assertUI().getMonitors()[0]).getClientArea();
		Rectangle inbounds = monitorBounds.intersection(bounds);
		if (!inbounds.equals(bounds)) {
			// push left if needed
			if (inbounds.x > bounds.x) {
				bounds.x = inbounds.x;
			}
			// push down if needed
			if (inbounds.y > bounds.y) {
				bounds.y = inbounds.y;
			}
			// push right, but not past the edge of the monitor (better to hang off to the right)
			int pushRight = bounds.x + bounds.width - (inbounds.x + inbounds.width);
			if (pushRight > 0) {
				bounds.x -= pushRight;
				if (bounds.x < monitorBounds.x) {
					bounds.x = monitorBounds.x;
				}
			}
			// push up, but not past the edge of the monitor (better to hang off to the bottom)
			int pushUp = bounds.y + bounds.height - (inbounds.y + inbounds.height);
			if (pushUp > 0) {
				bounds.y -= pushUp;
				if (bounds.y < monitorBounds.y) {
					bounds.y = monitorBounds.y;
				}
			}
		}

		// set the location and open it up!
		shell.setBounds(bounds);
		if (!dontOpen) {
			shell.open();
		}
	}

	/** Prevents the given shell from closing without prompting.  Returns a Subscription which can cancel this blocking. */
	public static Disposable confirmClose(Shell shell, String title, String question, Runnable runOnClose) {
		Listener listener = e -> {
			e.doit = SwtMisc.blockForQuestion(title, question, shell);
			if (e.doit) {
				runOnClose.run();
			}
		};
		shell.addListener(SWT.Close, listener);
		return Disposables.fromRunnable(() -> {
			SwtExec.immediate().guardOn(shell).execute(() -> {
				shell.removeListener(SWT.Close, listener);
			});
		});
	}
}
