/**
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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Unhandled;

/** Utilities for using ImageDescriptors correctly. */
public class ImageDescriptors {
	/**
	 * ImageDescriptor allows an image to be shared in a pool using reference counting. In order to not screw-up the reference
	 * counting, you need to be pretty careful with how you use them.
	 * 
	 * This creates a Box.Nullable<ImageDescriptor> which sets and gets images in a way that will keep the reference counting happy.
	 * 
	 * NO ONE MUST SET THE IMAGE EXCEPT THIS SETTER.
	 *
	 * @param lifecycle   Any outstanding images will be destroyed with the lifecycle of this Widget.
	 * @param imageGetter Function which returns the image being gotten.
	 * @param imageSetter Function which sets the image being set.
	 * @return
	 */
	public static Box.Nullable<ImageDescriptor> createSetter(Widget lifecycle, Supplier<Image> imageGetter, Consumer<Image> imageSetter) {
		return new Box.Nullable<ImageDescriptor>() {
			private ImageDescriptor lastDesc;
			private Image lastImg;

			{
				// when the control is disposed, we'll clear the image
				lifecycle.addListener(SWT.Dispose, e -> {
					if (lastDesc != null) {
						lastDesc.destroyResource(lastImg);
					}
				});
			}

			@Override
			public ImageDescriptor get() {
				return lastDesc;
			}

			@Override
			public void set(ImageDescriptor newDesc) {
				// make sure nobody else messed with the image
				if (imageGetter.get() != lastImg) {
					// if someone else did mess with it, we can probably survive, so best to just
					// log the failure and continue with setting the image
					Errors.log().handle(new IllegalStateException("Setter must have exclusive control over the image field."));
				}

				// set the new image
				Image newImg;
				if (newDesc != null) {
					newImg = (Image) newDesc.createResource(lifecycle.getDisplay());
				} else {
					newImg = null;
				}
				imageSetter.accept(newImg);

				// if an image was already set, destroy it
				if (lastDesc != null) {
					lastDesc.destroyResource(lastImg);
				}

				// save the fields for the next go-round
				lastDesc = newDesc;
				lastImg = newImg;
			}
		};
	}

	/** Global cache of widget -> image descriptor setters. */
	private static final OnePerWidget<Widget, Box.Nullable<ImageDescriptor>> map = OnePerWidget.from((Widget widget) -> {
		if (widget instanceof Item) {
			Item cast = (Item) widget;
			return createSetter(cast, cast::getImage, cast::setImage);
		} else if (widget instanceof Button) {
			Button cast = (Button) widget;
			return createSetter(cast, cast::getImage, cast::setImage);
		} else if (widget instanceof Label) {
			Label cast = (Label) widget;
			return createSetter(cast, cast::getImage, cast::setImage);
		} else {
			throw Unhandled.classException(widget);
		}
	});

	/** Sets the given Item to have the image described by the given descriptor, maintaining proper reference counting. */
	public static void set(Item widget, ImageDescriptor image) {
		map.get(widget).set(image);
	}

	/** Sets the given Button to have the image described by the given descriptor, maintaining proper reference counting. */
	public static void set(Button widget, ImageDescriptor image) {
		map.get(widget).set(image);
	}

	/** Sets the given Label to have the image described by the given descriptor, maintaining proper reference counting. */
	public static void set(Label widget, ImageDescriptor image) {
		map.get(widget).set(image);
	}
}
