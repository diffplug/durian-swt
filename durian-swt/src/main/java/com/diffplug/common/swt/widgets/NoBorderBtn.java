/*
 * Copyright (C) 2020-2025 DiffPlug
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
package com.diffplug.common.swt.widgets;

import com.diffplug.common.rx.Rx;
import com.diffplug.common.swt.ControlWrapper;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableSharedFlow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Regular buttons have different margins on each platform,
 * which makes it difficult to make small buttons, such as
 * expand / collapse.
 * <p>
 * This is a button with no border at all.
 */
public class NoBorderBtn extends ControlWrapper.AroundControl<Canvas> {
	private Image img = null;
	private Rectangle imgBounds;
	private boolean enabled = true;
	private MutableSharedFlow<NoBorderBtn> selection = Rx.INSTANCE.createEmitFlow();

	public NoBorderBtn(Composite parent) {
		super(new Canvas(parent, SWT.NONE));
		wrapped.addListener(SWT.Paint, e -> {
			Point size = wrapped.getSize();

			// fill the background based on the "enabled" status
			Color backgroundColor = wrapped.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
			e.gc.setBackground(backgroundColor);
			e.gc.fillRectangle(0, 0, size.x, size.y);

			// draw the image
			if (img != null) {
				int deltaX = size.x - imgBounds.width;
				int deltaY = size.y - imgBounds.height;
				e.gc.drawImage(img, deltaX / 2, deltaY / 2);
			}
		});

		// send a selection event on each click (if we aren't disabled)
		wrapped.addListener(SWT.MouseDown, e -> {
			if (enabled) {
				Rx.emit(selection, this);
			}
		});
	}

	/** Returns an Observable which responds to clicks. */
	public Flow<NoBorderBtn> clicked() {
		return selection;
	}

	/** Sets the image on this TightButton. */
	public void setImage(Image img) {
		this.img = img;
		this.imgBounds = img != null ? img.getBounds() : null;
		wrapped.redraw();
	}

	/** Sets the enabled state of the TightButton. */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		wrapped.redraw();
	}
}
