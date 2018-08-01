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
package com.diffplug.common.swt.dnd;

import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.graphics.Image;

/** Custom implementation of a local transfer class. The "data" object will be this Transfer itself. */
public abstract class CustomDragImageListener implements DragSourceListener {
	private Image dragImage;
	private final DragSourceListener delegate;

	protected CustomDragImageListener(DragSourceListener delegate) {
		this.delegate = delegate;
	}

	/**
	 * Creates the image to put under the cursor during drag. It will be disposed automatically.
	 * 
	 * {@link CustomDrawer} might useful here.
	 */
	protected abstract Image createImage(DragSourceEvent e);

	/** Disposes the image which was formerly under the cursor during drag. */
	private void disposeImage() {
		if (dragImage != null) {
			dragImage.dispose();
			dragImage = null;
		}
	}

	@Override
	public void dragStart(DragSourceEvent e) {
		disposeImage();

		// start the drag
		delegate.dragStart(e);

		if (e.doit) {
			dragImage = createImage(e);
		}
	}

	@Override
	public void dragSetData(DragSourceEvent e) {
		delegate.dragSetData(e);
	}

	@Override
	public void dragFinished(DragSourceEvent e) {
		disposeImage();
		delegate.dragFinished(e);
	}
}
