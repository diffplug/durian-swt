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
package com.diffplug.common.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.AbstractColumnLayout;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

/** Fluent API for creating {@link Table}s and {@link Tree}s with the specified columns and layout. */
public class ColumnFormat {
	/** Creates a {@code TableFormat} with the given style bits. */
	public static ColumnFormat builder() {
		return new ColumnFormat();
	}

	private int style;
	private boolean linesVisible = true;
	private boolean headerVisible = true;
	private final List<ColumnBuilder> columnBuilders = new ArrayList<>();

	protected ColumnFormat() {}

	/** Returns the style of this format.  Allows utilities that require a certain flag (e.g. SWT.VIRTUAL) to do error-checking. */
	public int getStyle() {
		return style;
	}

	/** Sets the SWT style flags. */
	public ColumnFormat setStyle(int style) {
		this.style = style;
		return this;
	}

	/** Sets the lines to be visible, defaults to true. */
	public ColumnFormat setLinesVisible(boolean linesVisible) {
		this.linesVisible = linesVisible;
		return this;
	}

	/** Sets the header to be visible, defaults to true. */
	public ColumnFormat setHeaderVisible(boolean headerVisible) {
		this.headerVisible = headerVisible;
		return this;
	}

	/** Adds a column to the table. */
	public ColumnBuilder addColumn() {
		ColumnBuilder builder = new ColumnBuilder();
		columnBuilders.add(builder);
		return builder;
	}

	/** Returns the columns array. */
	public List<ColumnBuilder> getColumns() {
		return columnBuilders;
	}

	/** Builds a {@link Table} with the specified columns and layout. */
	public Table buildTable(Composite parent) {
		return buildTable(parent, style, linesVisible, headerVisible, columnBuilders);
	}

	/** Builds a {@link Tree} with the specified columns and layout. */
	public Tree buildTree(Composite parent) {
		return buildTree(parent, style, linesVisible, headerVisible, columnBuilders);
	}

	/** Builds a table with the given columns. */
	protected static Table buildTable(Composite parent, int style, boolean linesVisible, boolean headerVisible, List<? extends ColumnBuilder> columnBuilders) {
		SwtMisc.assertClean(parent);
		// create the control
		Table control = new Table(parent, style);
		control.setLinesVisible(linesVisible);
		control.setHeaderVisible(headerVisible);

		// create the columns and layout
		Function<ColumnBuilder, TableColumn> buildFunc = builder -> builder.build(control);
		List<TableColumn> columns = columnBuilders.stream().map(buildFunc).collect(Collectors.toList());
		buildLayout(control, new TableColumnLayout(), columns, columnBuilders);

		// return the control
		return control;
	}

	/** Builds a table with the given columns. */
	protected static Tree buildTree(Composite parent, int style, boolean linesVisible, boolean headerVisible, List<? extends ColumnBuilder> columnBuilders) {
		SwtMisc.assertClean(parent);
		// create the control
		Tree control = new Tree(parent, style);
		control.setLinesVisible(linesVisible);
		control.setHeaderVisible(headerVisible);

		// create the columns and layout
		Function<ColumnBuilder, TreeColumn> buildFunc = builder -> builder.build(control);
		List<TreeColumn> columns = columnBuilders.stream().map(buildFunc).collect(Collectors.toList());
		buildLayout(control, new TreeColumnLayout(), columns, columnBuilders);

		// return the control
		return control;
	}

	/** Builds the layout. */
	private static void buildLayout(Scrollable control, AbstractColumnLayout layout, List<? extends Widget> columns, List<? extends ColumnBuilder> columnBuilders) {
		// create the layout
		for (int i = 0; i < columns.size(); ++i) {
			layout.setColumnData(columns.get(i), columnBuilders.get(i).dataBuilder.data);
		}
		control.getParent().setLayout(layout);

		// update the layout on every resize
		SwtMisc.asyncLayoutOnResize(control.getParent());
		// sometimes complicated trees can take a long time to get settled, so we'll do some last-ditch checks
		Runnable checkLayout = () -> {
			control.getParent().layout(true, true);
		};
		SwtExec.Guarded guarded = SwtExec.async().guardOn(control);
		guarded.timerExec(500, checkLayout);
		guarded.timerExec(1000, checkLayout);
		guarded.timerExec(2000, checkLayout);
	}

	/** Builder for a single TableColumn. */
	public static class ColumnBuilder {
		private String text;
		private Image image;
		private int style = SWT.LEFT;
		ColumnDataBuilder<?> dataBuilder = new ColumnWeightDataBuilder(1);

		protected ColumnBuilder() {}

		TableColumn build(Table parent) {
			return buildHelper(new TableColumn(parent, style));
		}

		TreeColumn build(Tree parent) {
			return buildHelper(new TreeColumn(parent, style));
		}

		private <T extends Item> T buildHelper(T item) {
			Optional.ofNullable(text).ifPresent(item::setText);
			Optional.ofNullable(image).ifPresent(item::setImage);
			return item;
		}

		public ColumnBuilder setText(String text) {
			this.text = text;
			return this;
		}

		public ColumnBuilder setImage(Image image) {
			this.image = image;
			return this;
		}

		public ColumnBuilder setStyle(int style) {
			this.style = style;
			return this;
		}

		public ColumnWeightDataBuilder setLayoutWeight(int weight) {
			ColumnWeightDataBuilder dataBuilder = new ColumnWeightDataBuilder(weight);
			this.dataBuilder = dataBuilder;
			return dataBuilder;
		}

		public ColumnPixelDataBuilder setLayoutPixel(int pixels) {
			ColumnPixelDataBuilder dataBuilder = new ColumnPixelDataBuilder(pixels);
			this.dataBuilder = dataBuilder;
			return dataBuilder;
		}
	}

	/** Base class for {@link ColumnWeightDataBuilder} and {@link ColumnPixelDataBuilder}. */
	private static abstract class ColumnDataBuilder<T extends ColumnLayoutData> {
		protected final T data;

		protected ColumnDataBuilder(T data) {
			this.data = data;
		}
	}

	/** A fluent API for manipulating a {@link ColumnWeightData}. */
	public static class ColumnWeightDataBuilder extends ColumnDataBuilder<ColumnWeightData> {
		private ColumnWeightDataBuilder(int weight) {
			super(new ColumnWeightData(weight));
		}

		/** Determines whether the column is resizable. */
		public ColumnWeightDataBuilder resizable(boolean resizable) {
			data.resizable = resizable;
			return this;
		}

		/**
		 * Sets the minimum width in pixels.
		 * <p>
		 * Default value is 0.
		 */
		public ColumnWeightDataBuilder minimumWidth(int minimumWidth) {
			data.minimumWidth = minimumWidth;
			return this;
		}
	}

	/** A fluent API for manipulating a {@link ColumnPixelData}. */
	public static class ColumnPixelDataBuilder extends ColumnDataBuilder<ColumnPixelData> {
		private ColumnPixelDataBuilder(int pixels) {
			super(new ColumnPixelData(pixels));
			data.addTrim = true;
		}

		/** Determines whether the column is resizable. */
		public ColumnPixelDataBuilder resizable(boolean resizable) {
			data.resizable = resizable;
			return this;
		}

		/**
		 * Whether to allocate extra width to the column to account for trim taken by the column itself.
		 * <p>
		 * Default value is true.
		 */
		public ColumnPixelDataBuilder addTrim(boolean addTrim) {
			data.addTrim = addTrim;
			return this;
		}
	}
}
