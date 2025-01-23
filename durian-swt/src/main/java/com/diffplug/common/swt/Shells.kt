/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.common.swt

import com.diffplug.common.base.Preconditions
import com.diffplug.common.collect.Maps
import com.diffplug.common.swt.os.WS
import com.diffplug.common.tree.TreeIterable
import com.diffplug.common.tree.TreeQuery
import com.diffplug.common.tree.TreeStream
import kotlinx.coroutines.Job
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Shell
import java.util.*


/** A fluent builder for creating SWT [Shell]s.  */
class Shells private constructor(private val style: Int, private val coat: Coat) {
    private var title: String? = null
    private var image: Image? = null
    private var alpha = SWT.DEFAULT
    private val size = Point(SWT.DEFAULT, SWT.DEFAULT)
    private var positionIncludesTrim = true
    private var location: Map.Entry<Corner, Point>? = null
    private var dontOpen = false
    private var closeOnEscape = false

    /** Sets the title for this Shell.  */
    fun setTitle(title: String): Shells {
        this.title = title
        return this
    }

    /** Sets the title image for this Shell.  */
    fun setImage(image: Image?): Shells {
        this.image = image
        return this
    }

    /** Sets the alpha for this Shell.  */
    fun setAlpha(alpha: Int): Shells {
        this.alpha = alpha
        return this
    }

    /**
     * Sets the full bounds for this shell.
     * Delegates to [.setRectangle] and [.setPositionIncludesTrim].
     */
    fun setBounds(bounds: Rectangle): Shells {
        setRectangle(bounds)
        setPositionIncludesTrim(true)
        return this
    }

    /** Calls [.setBounds] to match this control.  */
    fun setBounds(control: Control): Shells {
        return setBounds(SwtMisc.globalBounds(control))
    }

    /** Calls [.setBounds] to match this control.  */
    fun setBounds(wrapper: ControlWrapper): Shells {
        return setBounds(SwtMisc.globalBounds(wrapper.rootControl))
    }

    /**
     * Calls [.setLocation] and [.setSize] in one line.
     */
    fun setRectangle(rect: Rectangle): Shells {
        return setLocation(Point(rect.x, rect.y)).setSize(Point(rect.width, rect.height))
    }

    /**
     * Sets the size for this Shell.
     *
     *  * If `size` is null, or both components are `<= 0`, the shell will be packed as tightly as possible.
     *  * If both components are `> 0`, the shell will be set to that size.
     *  * If *one* component is `<= 0`, the positive dimension will be constrained and the other dimension will be packed as tightly as possible.
     *
     * @throws IllegalArgumentException if size is non-null and both components are negative
     */
    fun setSize(size: Point?): Shells {
        if (size == null) {
            this.size.x = SWT.DEFAULT
            this.size.y = SWT.DEFAULT
        } else {
            setSize(size.x, size.y)
        }
        return this
    }

    /** @see .setSize
     */
    fun setSize(x: Int, y: Int): Shells {
        size.x = sanitizeToDefault(x)
        size.y = sanitizeToDefault(y)
        return this
    }

    /**
     * Sets the absolute location of the top-left of this shell. If the value
     * is null, the shell will open:
     *
     *  * if there is a parent shell, below and to the right of the parent
     *  * if there isn't a parent shell, at the current cursor position
     *
     */
    fun setLocation(openPosition: Point): Shells {
        return setLocation(Corner.TOP_LEFT, openPosition)
    }

    /**
     * Sets the absolute location of the the given corner of this shell. If the value
     * is null, the shell will open:
     *
     *  * if there is a parent shell, below and to the right of the parent
     *  * if there isn't a parent shell, at the current cursor position
     *
     */
    fun setLocation(corner: Corner, position: Point): Shells {
        this.location = Maps.immutableEntry(Objects.requireNonNull(corner), Objects.requireNonNull(position))
        return this
    }

    /**
     * If true, size and location will set the the "outside" of the Shell - including the trim.
     * If false, it will set the "inside" of the Shell - not including the trim.
     * Default value is true.
     */
    fun setPositionIncludesTrim(positionIncludesTrim: Boolean): Shells {
        this.positionIncludesTrim = positionIncludesTrim
        return this
    }

    /**
     * If true, the "openOn" methods will create the shell but not actually open them.
     * This is rare and a little awkward, might get changed someday: https://github.com/diffplug/durian-swt/issues/4
     */
    fun setDontOpen(dontOpen: Boolean): Shells {
        this.dontOpen = dontOpen
        return this
    }

    /**
     * Determines whether the shell will close on escape, defaults to false.
     */
    fun setCloseOnEscape(closeOnEscape: Boolean): Shells {
        this.closeOnEscape = closeOnEscape
        return this
    }

    /** Opens the shell on this parent shell.  */
    fun openOn(parent: Shell): Shell {
        Preconditions.checkNotNull(parent)
        val shell = Shell(parent, style)
        if (location == null) {
            val parentPos = shell.parent.location
            val SHELL_MARGIN = SwtMisc.systemFontHeight()
            parentPos.x += SHELL_MARGIN
            parentPos.y += SHELL_MARGIN
            location = Maps.immutableEntry(Corner.TOP_LEFT, parentPos)
        }
        setupShell(shell)
        return shell
    }


    @Deprecated("for {@link #openOnBlocking(Shell)} - same behavior, but name is consistent with the others.")
    fun openOnAndBlock(parent: Shell) {
        openOnBlocking(parent)
    }

    /** Opens the shell on this parent and blocks.  */
    fun openOnBlocking(parent: Shell) {
        Preconditions.checkArgument(!dontOpen)
        SwtMisc.loopUntilDisposed(openOn(parent))
    }

    /** Opens the shell on the currently active shell.  */
    fun openOnActive(): Shell {
        val display = SwtMisc.assertUI()
        val shell: Shell
        val parent = active()
        shell = if (parent == null) {
            Shell(display, style)
        } else {
            Shell(parent, style)
        }
        if (location == null) {
            location = Maps.immutableEntry(Corner.CENTER, display.cursorLocation)
        }
        setupShell(shell)
        return shell
    }

    /** Prevents setting any size or position.  Does not prevent changing the location and size to ensure that the shell is on-screen.  */
    fun dontSetPositionOrSize(): Shells {
        location = SENTINEL_DONT_SET_POSITION_OR_SIZE
        return this
    }

    /** Opens the shell on the currently active shell and blocks.  */
    fun openOnActiveBlocking() {
        Preconditions.checkArgument(!dontOpen)
        SwtMisc.loopUntilDisposed(openOnActive())
    }

    /** Opens the shell as a root shell.  */
    fun openOnDisplay(): Shell {
        val display = SwtMisc.assertUI()
        if (location == null) {
            location = Maps.immutableEntry(Corner.CENTER, display.cursorLocation)
        }
        val shell = Shell(display, style)
        setupShell(shell)
        return shell
    }

    /** Opens the shell as a root shell and blocks.  */
    fun openOnDisplayBlocking() {
        Preconditions.checkArgument(!dontOpen)
        SwtMisc.loopUntilDisposed(openOnDisplay())
    }

    private fun setupShell(shell: Shell) {
        // set the text, image, and alpha
        if (title != null) {
            shell.text = title
        }
        if (image != null && WS.getRunning() != WS.COCOA) {
            shell.image = image
        }
        if (alpha != SWT.DEFAULT) {
            shell.alpha = alpha
        }
        // disable close on ESCAPE
        if (!closeOnEscape) {
            shell.addListener(SWT.Traverse) { e: Event ->
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    e.doit = false
                }
            }
        }
        // find the composite we're going to draw on
        coat.putOn(shell)

        val bounds: Rectangle
        if (location === SENTINEL_DONT_SET_POSITION_OR_SIZE) {
            bounds = shell.bounds
        } else {
            if (positionIncludesTrim) {
                val computedSize: Point
                if ((size.x == SWT.DEFAULT) xor (size.y == SWT.DEFAULT)) {
                    // if we're specifying only one side or the other,
                    // then we need to adjust for the trim
                    val trimFor100x100 = shell.computeTrim(100, 100, 100, 100)
                    val dwidth = trimFor100x100.width - 100
                    val dheight = trimFor100x100.height - 100
                    val widthHint = if (size.x == SWT.DEFAULT) SWT.DEFAULT else size.x - dwidth
                    val heightHint = if (size.y == SWT.DEFAULT) SWT.DEFAULT else size.y - dheight
                    computedSize = shell.computeSize(widthHint, heightHint)
                } else {
                    if (size.x == SWT.DEFAULT) {
                        // we're packing as tight as can
                        Preconditions.checkState(size.y == SWT.DEFAULT)
                        computedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true)
                    } else {
                        // the size is specified completely
                        Preconditions.checkState(size.y != SWT.DEFAULT)
                        computedSize = size
                    }
                }
                val topLeft =
                    location!!.key.topLeftRequiredFor(Rectangle(0, 0, computedSize.x, computedSize.y), location!!.value)
                bounds = Rectangle(topLeft.x, topLeft.y, computedSize.x, computedSize.y)
            } else {
                val computedSize = shell.computeSize(size.x, size.y, true)
                val topLeft =
                    location!!.key.topLeftRequiredFor(Rectangle(0, 0, computedSize.x, computedSize.y), location!!.value)
                bounds = shell.computeTrim(topLeft.x, topLeft.y, computedSize.x, computedSize.y)
            }
        }

        // constrain the position by the Display's bounds (getClientArea() takes the Start bar into account)
        val monitorBounds =
            SwtMisc.monitorFor(Corner.CENTER.getPosition(bounds)).orElse(SwtMisc.assertUI().monitors[0]).clientArea
        val inbounds = monitorBounds.intersection(bounds)
        if (inbounds != bounds) {
            // push left if needed
            if (inbounds.x > bounds.x) {
                bounds.x = inbounds.x
            }
            // push down if needed
            if (inbounds.y > bounds.y) {
                bounds.y = inbounds.y
            }
            // push right, but not past the edge of the monitor (better to hang off to the right)
            val pushRight = bounds.x + bounds.width - (inbounds.x + inbounds.width)
            if (pushRight > 0) {
                bounds.x -= pushRight
                if (bounds.x < monitorBounds.x) {
                    bounds.x = monitorBounds.x
                }
            }
            // push up, but not past the edge of the monitor (better to hang off to the bottom)
            val pushUp = bounds.y + bounds.height - (inbounds.y + inbounds.height)
            if (pushUp > 0) {
                bounds.y -= pushUp
                if (bounds.y < monitorBounds.y) {
                    bounds.y = monitorBounds.y
                }
            }
        }

        // set the location and open it up!
        shell.bounds = bounds
        if (!dontOpen) {
            shell.open()
        }
    }

    companion object {
        /** Creates a new Shells for this Coat.  */
		@JvmStatic
		fun builder(style: Int, coat: Coat): Shells {
            return Shells(style, coat)
        }

        private fun sanitizeToDefault(`val`: Int): Int {
            return if (`val` > 0) `val` else SWT.DEFAULT
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
        @JvmStatic
        fun active(): Shell? {
            val display = SwtMisc.assertUI()
            val active = display.activeShell
            val activeLocation = if (active == null) {
                display.cursorLocation
            } else {
                if (isValidActiveShell(active)) {
                    return active
                } else {
                    active.location
                }
            }

            // we now have the location of the cursor, all we have to do is find which shell is underneath it

            // first we'll look at the direct ancestors of the active shell
            if (active != null) {
                val validParentOfActive = TreeStream.toParent(SwtMisc.treeDefShell(), active)
                    .filter { shell: Shell -> isValidActiveShell(shell) }
                    .filter { shell: Shell -> shell.bounds.contains(activeLocation) }
                    .findFirst()
                if (validParentOfActive.isPresent) {
                    return validParentOfActive.get()
                }
            }

            // then we'll look at every valid shell
            val shellsUnderActiveLocation: MutableList<Shell> = ArrayList()
            val shells = display.shells
            for (rootShell in shells) {
                // only look at valid shells
                for (shell in TreeIterable.breadthFirst<Shell>(SwtMisc.treeDefShell()
                    .filter { shell: Shell -> isValidActiveShell(shell) }, rootShell
                )) {
                    if (shell.bounds.contains(activeLocation)) {
                        shellsUnderActiveLocation.add(shell)
                    }
                }
            }
            if (shellsUnderActiveLocation.isEmpty()) {
                return null
            } else if (shellsUnderActiveLocation.size == 1) {
                return shellsUnderActiveLocation[0]
            }
            // otherwise, we prefer the deepest shell
            val byDepth =
                Comparator.comparingInt { shell: Shell -> TreeQuery.toRoot(SwtMisc.treeDefShell(), shell).size }
            return Collections.max(shellsUnderActiveLocation, byDepth)
        }

        private fun isValidActiveShell(shell: Shell): Boolean {
            return shell.isVisible && SwtMisc.flagIsSet(SWT.TITLE, shell)
        }

        private val SENTINEL_DONT_SET_POSITION_OR_SIZE: Map.Entry<Corner, Point> =
            Maps.immutableEntry<Corner, Point>(null, null)

        /** Prevents the given shell from closing without prompting.  Returns a Subscription which can cancel this blocking.  */
        @JvmStatic
        fun confirmClose(shell: Shell, title: String, question: String, runOnClose: Runnable): Job {
            val listener = Listener { e: Event ->
                e.doit = SwtMisc.blockForQuestion(title, question, shell)
                if (e.doit) {
                    runOnClose.run()
                }
            }
            shell.addListener(SWT.Close, listener)
            return Job().apply {
                invokeOnCompletion {
                    SwtExec.immediate().guardOn(shell).execute {
                        shell.removeListener(SWT.Close, listener)
                    }
                }
            }
        }
    }
}
