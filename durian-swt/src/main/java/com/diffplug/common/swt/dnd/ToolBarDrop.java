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
package com.diffplug.common.swt.dnd;


import com.diffplug.common.base.Unhandled;
import com.diffplug.common.swt.OnePerWidget;
import com.diffplug.common.swt.SwtThread;
import com.diffplug.common.swt.dnd.StructuredDrop.DropMethod;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/** Mechanism for adding drop to toolbars. */
@SwtThread
public class ToolBarDrop {
	private static final OnePerWidget<ToolBar, ToolBarDrop> pool = new OnePerWidget<ToolBar, ToolBarDrop>() {
		@Override
		protected ToolBarDrop create(ToolBar ctl) {
			return new ToolBarDrop(ctl);
		}
	};

	final ToolBar toolbar;
	final Set<Transfer> transferSet = new HashSet<>();
	final Map<IAction, DropTargetListener> actionToListener = new HashMap<>();
	final Map<ToolItem, DropTargetListener> toolItemToListener = new HashMap<>();
	final DropTarget target;
	Transfer[] transferArray;

	private ToolBarDrop(ToolBar toolbar) {
		this.toolbar = toolbar;
		Object obj = toolbar.getData(DND.DROP_TARGET_KEY);
		if (obj != null) {
			// CoolBars might add a drop listener, but we don't want their behavior.
			// Clobber the existing DropTarget.
			((DropTarget) obj).dispose();
		}
		target = new DropTarget(toolbar, DndOp.dropAll());
		target.addDropListener(delegateListener);
	}

	final DropTargetListener delegateListener = new DropTargetListener() {
		// @formatter:off
		@Override public void dragEnter(DropTargetEvent event) {			handle(event, DropMethod.dragEnter);			}
		@Override public void dragLeave(DropTargetEvent event) {			handle(event, DropMethod.dragLeave);			}
		@Override public void dragOperationChanged(DropTargetEvent event) {	handle(event, DropMethod.dragOperationChanged);	}
		@Override public void dragOver(DropTargetEvent event) {				handle(event, DropMethod.dragOver);				}
		@Override public void drop(DropTargetEvent event) {					handle(event, DropMethod.drop);					}
		@Override public void dropAccept(DropTargetEvent event) {			handle(event, DropMethod.dropAccept);			}
		// @formatter:on

		private void handle(DropTargetEvent event, DropMethod method) {
			// by default we'll set the detail to NONE, to tell the user that there's no deal.
			// if there is a listener, it can override this
			event.detail = DND.DROP_DEFAULT;

			// map the point from global coordinates to the toolbar
			Point point = new Point(event.x, event.y);
			point = toolbar.getDisplay().map(null, toolbar, point);
			// get the ToolItem that it landed on
			ToolItem item = toolbar.getItem(point);

			// find the item directly or through actionToListener
			DropTargetListener listener = toolItemToListener.get(item);
			if (listener == null && item != null && item.getData() instanceof ActionContributionItem) {
				// if it's an ActionContributionItem, get it
				ActionContributionItem contributionItem = (ActionContributionItem) item.getData();
				// find the associated action
				IAction action = contributionItem.getAction();
				// find the DropTargetListener for the item
				listener = actionToListener.get(action);
			}

			if (listener != null) {
				// if there is a listener, delegate the method call to it
				// @formatter:off
				switch (method) {
				case dragEnter:				listener.dragEnter(event);				break;
				case dragLeave:				listener.dragLeave(event);				break;
				case dragOperationChanged:	listener.dragOperationChanged(event);	break;
				case dragOver:				listener.dragOver(event);				break;
				case drop:					listener.drop(event);					break;
				case dropAccept:			listener.dropAccept(event);				break;
				default:	throw Unhandled.enumException(method);
				}
				// @formatter:on
			}
		}
	};

	private void enforceTransfers(List<Transfer> transfers) {
		if (transferSet.containsAll(transfers)) {
			return;
		}
		transferSet.addAll(transfers);
		transferArray = transferSet.toArray(new Transfer[transferSet.size()]);
		target.setTransfer(transferArray);
	}

	private void add(IAction action, Transfer[] transfers, DropTargetListener listener) {
		enforceTransfers(Arrays.asList(transfers));
		Object previous = actionToListener.put(action, listener);
		if (previous != null) {
			throw new IllegalArgumentException("Duplicate drop listener for " + action);
		}
	}

	private void add(ToolItem item, Transfer[] transfers, DropTargetListener listener) {
		enforceTransfers(Arrays.asList(transfers));
		Object previous = toolItemToListener.put(item, listener);
		if (previous != null) {
			throw new IllegalArgumentException("Duplicate drop listener for " + item);
		}
	}

	public static void addDropSupport(ToolBarManager toolbarManager, IAction action, Transfer[] transfers, DropTargetListener listener) {
		pool.forWidget(toolbarManager.getControl()).add(action, transfers, listener);
	}

	public static void addDropSupport(ToolBarManager toolbarManager, IAction action, StructuredDrop.Listener listener) {
		addDropSupport(toolbarManager, action, listener.transferArray(), listener);
	}

	public static void addDropSupport(ToolItem item, Transfer[] transfers, DropTargetListener listener) {
		pool.forWidget(item.getParent()).add(item, transfers, listener);
	}

	public static void addDropSupport(ToolItem toolItem, StructuredDrop.Listener listener) {
		addDropSupport(toolItem, listener.transferArray(), listener);
	}
}
