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
package com.diffplug.common.swt.widgets;


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.Lists;
import com.diffplug.common.collect.MoreCollectors;
import com.diffplug.common.swt.Coat;
import com.diffplug.common.swt.ControlWrapper;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.LayoutsGridData;
import com.diffplug.common.swt.SwtMisc;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/** A quick way to make right-justified buttons. */
public class ButtonPanel extends ControlWrapper.AroundControl<Composite> {
	public static class Builder {
		private Optional<Coat> leftCoat = Optional.empty();
		private List<String> texts = Lists.newArrayList();
		private List<Integer> sizes = Lists.newArrayList();
		private List<Runnable> actions = Lists.newArrayList();

		/** Adds an OK button. */
		public Builder leftSide(Coat coat) {
			Preconditions.checkArgument(!leftCoat.isPresent());
			leftCoat = Optional.of(coat);
			return this;
		}

		/** Adds an OK button. */
		public Builder ok(Runnable ok) {
			return add("OK", ok);
		}

		/** Adds an OK and Cancel button. */
		public Builder okCancel(Runnable ok, Runnable cancel) {
			add("OK", ok);
			add("Cancel", cancel);
			return this;
		}

		/** Adds an OK and Cancel button. */
		public Builder okCancel(Consumer<Boolean> okClicked) {
			return okCancel(() -> okClicked.accept(true), () -> okClicked.accept(false));
		}

		/** Adds a button with the given text and action, as well as the default width. */
		public Builder add(String text, Runnable action) {
			return add(text, SwtMisc.defaultButtonWidth(), action);
		}

		/** Adds a button with the given text, width, and action. */
		public Builder add(String text, int size, Runnable action) {
			texts.add(text);
			sizes.add(size);
			actions.add(action);
			return this;
		}

		/** Creates a button panel on the given Composite. */
		public ButtonPanel build(Composite parent) {
			return new ButtonPanel(parent, this);
		}

		/** Creates a button panel on the given Composite, and returns a LayoutsGridData for manipulating it. */
		public LayoutsGridData buildOnGrid(Composite parent) {
			return Layouts.setGridData(build(parent));
		}
	}

	/** Creates a ButtonPanel builder. */
	public static Builder builder() {
		return new Builder();
	}

	final ImmutableList<Button> buttons;

	protected ButtonPanel(Composite parent, Builder builder) {
		super(new Composite(parent, SWT.NONE));
		Layouts.setGrid(wrapped).numColumns(builder.texts.size() + 1).margin(0);
		if (builder.leftCoat.isPresent()) {
			Composite leftSide = new Composite(wrapped, SWT.NONE);
			Layouts.setGridData(leftSide).grabHorizontal();
			builder.leftCoat.get().putOn(leftSide);
		} else {
			Layouts.newGridPlaceholder(wrapped).grabHorizontal();
		}
		ImmutableList.Builder<Button> buttonsBuilder = ImmutableList.builder();
		for (int i = 0; i < builder.texts.size(); ++i) {
			Button button = new Button(wrapped, SWT.PUSH);
			buttonsBuilder.add(button);
			button.setText(builder.texts.get(i));
			Layouts.setGridData(button).widthHint(builder.sizes.get(i));
			Runnable action = builder.actions.get(i);
			button.addListener(SWT.Selection, e -> action.run());
		}
		buttons = buttonsBuilder.build();
	}

	public ImmutableList<Button> getButtons() {
		return buttons;
	}

	public Button getButton(String text) {
		return MoreCollectors.singleOrEmptyShortCircuiting(buttons.stream().filter(b -> text.equals(b.getText()))).get();
	}

	public void setTexts(String... texts) {
		Preconditions.checkArgument(buttons.size() == texts.length);
		for (int i = 0; i < texts.length; ++i) {
			buttons.get(i).setText(texts[i]);
		}
	}
}
