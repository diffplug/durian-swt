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
package com.diffplug.common.swt.dnd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

import com.diffplug.common.swt.OnePerWidget;

public class DropTargetRouter {
	private DropTarget target;

	/** All of the Transfers currently supported by this DropTargetRouter. */
	private Set<Transfer> transferSet = new HashSet<>();

	/** Private constructor to force people to use the map. */
	private DropTargetRouter(Control ctl) {
		target = new DropTarget(ctl, DndOp.dropAll());
		target.addDropListener(new DelegatingListener());
		target.setDropTargetEffect(null);
	}

	private void addTransfers(Transfer[] transfers) {
		transferSet.addAll(Arrays.asList(transfers));
		Transfer[] allTransfers = transferSet.toArray(new Transfer[transferSet.size()]);
		target.setTransfer(allTransfers);
	}

	private Disposable subscription = null;

	/** Sets the DropTargetListener which will get called for this DropTarget. */
	public Disposable subscribe(DropTargetListener listener, DropTargetEvent e) {
		if (subscription != null) {
			subscription.unsubscribe(e);
		}
		subscription = new Disposable(listener, e);
		return subscription;
	}

	public class Disposable {
		private DropTargetListener listener;
		private boolean unsubscribed = false;

		/** Call dragEnter on subscription. */
		private Disposable(DropTargetListener listener, DropTargetEvent e) {
			this.listener = listener;
			listener.dragEnter(e);
		}

		/** Call dragLeave on unsubscribe. Don't complete the unsubscription until drop or dropAccept is called. */
		public void unsubscribe(DropTargetEvent e) {
			if (unsubscribed) {
				if (subscription == this) {
					subscription = null;
				}
			} else {
				unsubscribed = true;
				listener.dragLeave(e);
			}
		}

		public void dragOperationChanged(DropTargetEvent e) {
			if (unsubscribed) {
				subscription = null;
			} else {
				listener.dragOperationChanged(e);
			}
		}

		public void dragOver(DropTargetEvent e) {
			if (unsubscribed) {
				subscription = null;
			} else {
				listener.dragOver(e);
			}
		}

		public void drop(DropTargetEvent e) {
			listener.drop(e);
		}

		public void dropAccept(DropTargetEvent e) {
			listener.dropAccept(e);
		}
	}

	/** Adds a listener which bypasses the routing mechanism. */
	public void addBypassListener(DropTargetListener listener) {
		target.addDropListener(listener);
	}

	/** Adds a listener which bypasses the routing mechanism. */
	public void removeBypassListener(DropTargetListener listener) {
		target.removeDropListener(listener);
	}

	/** Listener which delegates its calls to the currentListener. */
	private class DelegatingListener implements DropTargetListener {
		// enter / leave is handled by the Disposable object
		@Override
		public void dragEnter(DropTargetEvent e) {}

		@Override
		public void dragLeave(DropTargetEvent e) {}

		@Override
		public void dragOperationChanged(DropTargetEvent e) {
			if (subscription != null) {
				subscription.dragOperationChanged(e);
			}
		}

		@Override
		public void dragOver(DropTargetEvent e) {
			if (subscription != null) {
				subscription.dragOver(e);
			}
		}

		@Override
		public void drop(DropTargetEvent e) {
			if (subscription != null) {
				subscription.drop(e);
			}
		}

		@Override
		public void dropAccept(DropTargetEvent e) {
			if (subscription != null) {
				subscription.dropAccept(e);
			}
		}
	}

	/** Returns the MultipleDragSource for the given Control. */
	public static DropTargetRouter forControl(Control ctl, Transfer[] transfers) {
		DropTargetRouter router = onePerControl.forWidget(ctl);
		router.addTransfers(transfers);
		return router;
	}

	private static final OnePerWidget<Control, DropTargetRouter> onePerControl = OnePerWidget.from(DropTargetRouter::new);
}
