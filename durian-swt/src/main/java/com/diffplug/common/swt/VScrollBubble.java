/*
 * Copyright (C) 2020-2021 DiffPlug, LLC - All Rights Reserved
 * Unauthorized copying of this file via any medium is strictly prohibited.
 * Proprietary and confidential.
 * Please send any inquiries to Ned Twigg <ned.twigg@diffplug.com>
 */
package com.diffplug.common.swt;

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.diffplug.common.swt.SwtMisc;
import com.diffplug.common.tree.TreeStream;

/**
 * Bubbles vertical scroll events up to a parent container, so that you don't get stuck.
 * Doesn't work great on mac due to rubber-banding.
 */
public class VScrollBubble {
	private static Display appliedTo;

	/** Returns true iff the given scrollable is maxed out for scrolling in the direction of the given event. */
	private static boolean isMaxed(Event e, Scrollable scrollable) {
		ScrollBar scrollBar = scrollable.getVerticalBar();
		if (scrollBar == null) {
			return true;
		} else {
			boolean isMaxed;
			//			System.out.println("[" + scrollBar.getMinimum() + " " + scrollBar.getMaximum() + "] " + scrollBar.getSelection() + " thumb=" + scrollBar.getThumb());
			if (e.count > 0) {
				// scrolling up
				isMaxed = scrollBar.getSelection() <= 0;
			} else {
				// scrolling down
				isMaxed = scrollBar.getSelection() >= scrollBar.getMaximum() - scrollBar.getThumb();
				if (!isMaxed) {
					// special case for trees & tables which have all their items visible, where scrolling down gives
					// scrollBar.getMinimum()/getMaximum=[0 100] scrollBar.getSelection()=0 scrollBar.getThumb()=10
					if (scrollable instanceof Tree) {
						Tree tree = (Tree) scrollable;
						int itemCount = tree.getItemCount();
						if (itemCount == 0) {
							return true;
						}
						TreeItem lastItem = tree.getItem(itemCount - 1);
						while (lastItem.getItemCount() > 0) {
							lastItem = lastItem.getItem(lastItem.getItemCount() - 1);
						}
						Rectangle itemBounds = lastItem.getBounds();
						Rectangle clientArea = tree.getClientArea();
						return itemBounds.y + itemBounds.height < clientArea.height;
					}
				}
			}
			return isMaxed;
		}
	}

	/** Applies the necessary display filter, don't worry about calling it more than once. */
	public static void applyTo(Display display) {
		// prevent double-apply
		if (display == appliedTo) {
			return;
		}
		appliedTo = display;
		display.addFilter(SWT.MouseVerticalWheel, e -> {
			if (!(e.widget instanceof Scrollable)) {
				return;
			}
			// find the parent two levels up
			Composite parentOfScrollable = ((Scrollable) e.widget).getParent();
			if (parentOfScrollable == null) {
				return;
			}
			parentOfScrollable = parentOfScrollable.getParent();
			if (parentOfScrollable == null) {
				return;
			}

			boolean isMaxed = isMaxed(e, (Scrollable) e.widget);
			if (!isMaxed) {
				return;
			}

			Optional<ScrolledComposite> firstNotMaxed = TreeStream.toParent(SwtMisc.treeDefComposite(), parentOfScrollable)
					.filter(c -> c instanceof ScrolledComposite && SwtMisc.flagIsSet(SWT.V_SCROLL, c))
					.map(ScrolledComposite.class::cast)
					.filter(sc -> !isMaxed(e, sc))
					.findFirst();
			if (!firstNotMaxed.isPresent()) {
				// there isn't a parent which we can bubble the scroll to
				return;
			}
			ScrolledComposite toScroll = firstNotMaxed.get();
			Point origin = toScroll.getOrigin();
			origin.y -= e.count * toScroll.getVerticalBar().getIncrement();
			toScroll.setOrigin(origin);

			// copy the event
			Event copy = SwtMisc.copyEvent(e);
			// cancel the old one
			e.doit = false;
			// and send the copy to the parent
			copy.widget = toScroll;
			Point d = ((Scrollable) e.widget).toDisplay(e.x, e.y);
			Point mapped = toScroll.toControl(d);
			copy.x = mapped.x;
			copy.y = mapped.y;
			copy.widget.notifyListeners(SWT.MouseVerticalWheel, copy);
		});
	}
}
