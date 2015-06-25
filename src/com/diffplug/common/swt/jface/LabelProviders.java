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
package com.diffplug.common.swt.jface;

import java.util.function.Function;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.diffplug.common.base.Functions;

/** Utilities for creating JFace {@link CellLabelProvider} (which is also appropriate for plain-jane {@link ILabelProvider}). */
public class LabelProviders {
	/** Returns a fluent builder for creating a {@link ColumnLabelProvider}. */
	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

	/** Creates a {@link ColumnLabelProvider} for text. */
	public static <T> ColumnLabelProvider createWithText(Function<? super T, ? extends String> text) {
		Builder<T> builder = builder();
		builder.withText(text);
		return builder.build();
	}

	/** Creates a {@link ColumnLabelProvider} for images. */
	public static <T> ColumnLabelProvider createWithImage(Function<? super T, ? extends Image> image) {
		Builder<T> builder = builder();
		builder.withImage(image);
		return builder.build();
	}

	/** Creates a {@link ColumnLabelProvider} for text and images. */
	public static <T> ColumnLabelProvider createWithTextAndImage(Function<? super T, ? extends String> text, Function<? super T, ? extends Image> image) {
		Builder<T> builder = builder();
		builder.withText(text);
		builder.withImage(image);
		return builder.build();
	}

	/** An override of {@link ColumnLabelProvider}. */
	@SuppressWarnings("unchecked")
	private static class Imp<T> extends ColumnLabelProvider {
		private final Function<? super T, ? extends String> text;
		private final Function<? super T, ? extends Image> image;
		private final Function<? super T, ? extends Color> background;
		private final Function<? super T, ? extends Color> foreground;
		private final Function<? super T, ? extends Font> font;

		private Imp(Function<? super T, ? extends String> text, Function<? super T, ? extends Image> image, Function<? super T, ? extends Color> background, Function<? super T, ? extends Color> foreground, Function<? super T, ? extends Font> font) {
			this.text = text;
			this.image = image;
			this.background = background;
			this.foreground = foreground;
			this.font = font;
		}

		@Override
		public String getText(Object element) {
			return text.apply((T) element);
		}

		@Override
		public Image getImage(Object element) {
			return image.apply((T) element);
		}

		@Override
		public Color getBackground(Object element) {
			return background.apply((T) element);
		}

		@Override
		public Color getForeground(Object element) {
			return foreground.apply((T) element);
		}

		@Override
		public Font getFont(Object element) {
			return font.apply((T) element);
		}
	}

	/** A fluent API for creating a {@link ColumnLabelProvider}. */
	public static class Builder<T> extends ColumnLabelProvider {
		private Function<? super T, ? extends String> text = Functions.constant(null);
		private Function<? super T, ? extends Image> image = Functions.constant(null);
		private Function<? super T, ? extends Color> background = Functions.constant(null);
		private Function<? super T, ? extends Color> foreground = Functions.constant(null);
		private Function<? super T, ? extends Font> font = Functions.constant(null);

		private Builder() {}

		/** Sets the function used to determine the text. */
		public Builder<T> withText(Function<? super T, ? extends String> text) {
			this.text = text;
			return this;
		}

		/** Sets the function used to determine the image. */
		public Builder<T> withImage(Function<? super T, ? extends Image> image) {
			this.image = image;
			return this;
		}

		/** Sets the function used to determine the background color. */
		public Builder<T> withBackground(Function<? super T, ? extends Color> background) {
			this.background = background;
			return this;
		}

		/** Sets the function used to determine the foreground color. */
		public Builder<T> withForeground(Function<? super T, ? extends Color> foreground) {
			this.foreground = foreground;
			return this;
		}

		/** Sets the function used to determine the font. */
		public Builder<T> withFont(Function<? super T, ? extends Font> font) {
			this.font = font;
			return this;
		}

		/** Returns a new ColumnLabelProvider. */
		public ColumnLabelProvider build() {
			return new Imp<T>(text, image, background, foreground, font);
		}
	}
}
