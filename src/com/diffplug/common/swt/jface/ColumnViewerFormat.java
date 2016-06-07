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
package com.diffplug.common.swt.jface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import com.diffplug.common.base.Consumers;
import com.diffplug.common.swt.ColumnFormat;

/** Fluent API for creating {@link TableViewer}s and {@link TreeViewer}s with a certain format. */
public class ColumnViewerFormat<T> {
	/** Creates a {@code TableFormat} with the given style bits. */
	public static <T> ColumnViewerFormat<T> builder() {
		return new ColumnViewerFormat<T>();
	}

	private int style;
	private boolean linesVisible = true;
	private boolean headerVisible = true;
	private boolean useHashLookup = true;
	private final List<ColumnBuilder<T>> columnBuilders = new ArrayList<>();

	private ColumnViewerFormat() {}

	/** Sets the SWT style flags. */
	public ColumnViewerFormat<T> setStyle(int style) {
		this.style = style;
		return this;
	}

	/** Sets the lines to be visible, defaults to true. */
	public ColumnViewerFormat<T> setLinesVisible(boolean linesVisible) {
		this.linesVisible = linesVisible;
		return this;
	}

	/** Sets the lines to be visible, defaults to true. */
	public ColumnViewerFormat<T> setHeaderVisible(boolean headerVisible) {
		this.headerVisible = headerVisible;
		return this;
	}

	/**
	 * Sets the whether the viewer is accelerated by a hashtable, defaults to true.
	 * 
	 * @see StructuredViewer#setUseHashlookup(boolean)
	 * */
	public ColumnViewerFormat<T> setUseHashLookup(boolean useHashLookup) {
		this.useHashLookup = useHashLookup;
		return this;
	}

	/** Adds a column to the table. */
	public ColumnBuilder<T> addColumn() {
		ColumnBuilder<T> builder = new ColumnBuilder<T>();
		columnBuilders.add(builder);
		return builder;
	}

	/** Builds a {@link TableViewer} on the given parent. */
	public TableViewer buildTable(Composite parent) {
		Table control = Portal.buildTable(parent, style, linesVisible, headerVisible, columnBuilders);
		return buildViewer(new TableViewer(control), Arrays.asList(control.getColumns()), TableViewerColumn::new);
	}

	/** Builds a {@link TreeViewer} on the given parent. */
	public TreeViewer buildTree(Composite parent) {
		Tree control = Portal.buildTree(parent, style, linesVisible, headerVisible, columnBuilders);
		return buildViewer(new TreeViewer(control), Arrays.asList(control.getColumns()), TreeViewerColumn::new);
	}

	/** Returns the columns array. */
	public List<ColumnBuilder<T>> getColumns() {
		return Collections.unmodifiableList(columnBuilders);
	}

	/** Returns as a regular non-viewer format. */
	public ColumnFormat asColumnFormat() {
		return new Portal(this);
	}

	/** Provides a portal to the protected static methods in ColumnFormat. */
	private static class Portal extends ColumnFormat {
		private Portal(ColumnViewerFormat<?> source) {
			Portal.this.style = source.style;
			Portal.this.linesVisible = source.linesVisible;
			Portal.this.headerVisible = source.headerVisible;
			Portal.this.columnBuilders.addAll(source.columnBuilders);
		}

		protected static Table buildTable(Composite parent, int style, boolean linesVisible, boolean headerVisible, List<? extends ColumnBuilder> columnBuilders) {
			return ColumnFormat.buildTable(parent, style, linesVisible, headerVisible, columnBuilders);
		}

		protected static Tree buildTree(Composite parent, int style, boolean linesVisible, boolean headerVisible, List<? extends ColumnBuilder> columnBuilders) {
			return ColumnFormat.buildTree(parent, style, linesVisible, headerVisible, columnBuilders);
		}
	}

	/** Builds a viewer. */
	private <ViewerType extends StructuredViewer, ColumnType extends Item> ViewerType buildViewer(ViewerType viewer, List<ColumnType> columns, BiFunction<ViewerType, ColumnType, ViewerColumn> columnViewerCreator) {
		viewer.setUseHashlookup(useHashLookup);
		for (int i = 0; i < columnBuilders.size(); ++i) {
			ViewerColumn viewerColumn = columnViewerCreator.apply(viewer, columns.get(i));
			ColumnBuilder<T> builder = columnBuilders.get(i);
			if (builder.provider != null) {
				viewerColumn.setLabelProvider(builder.provider);
			}
			builder.finalSetup.accept(viewerColumn);
		}
		return viewer;
	}

	/** Builder for a single TableColumn. */
	public static class ColumnBuilder<T> extends ColumnFormat.ColumnBuilder {
		private ColumnBuilder() {}

		@Nullable
		private ColumnLabelProvider provider;
		private Consumer<? super ViewerColumn> finalSetup = Consumers.doNothing();

		/** Uses the given as the label provider. */
		public ColumnBuilder<T> setLabelProvider(ColumnLabelProvider provider) {
			this.provider = provider;
			return this;
		}

		/** Uses the given function as the textual label provider. */
		public ColumnBuilder<T> setLabelProviderText(Function<? super T, String> text) {
			return setLabelProvider(LabelProviders.createWithText(text));
		}

		/** Uses the given function as the image label provider. */
		public ColumnBuilder<T> setLabelProviderImage(Function<? super T, Image> image) {
			return setLabelProvider(LabelProviders.createWithImage(image));
		}

		/** Uses the given function as the textual and image label provider. */
		public ColumnBuilder<T> setLabelProviderTextAndImage(Function<? super T, String> text, Function<? super T, Image> image) {
			return setLabelProvider(LabelProviders.createWithTextAndImage(text, image));
		}

		/** Calls the given consumer after the ColumnViewer has been constructed. */
		public ColumnBuilder<T> setFinalSetup(Consumer<? super ViewerColumn> finalSetup) {
			this.finalSetup = finalSetup;
			return this;
		}

		////////////////////////////////
		// Override the regular stuff //
		////////////////////////////////
		@Override
		public ColumnBuilder<T> setText(String text) {
			super.setText(text);
			return this;
		}

		@Override
		public ColumnBuilder<T> setImage(Image image) {
			super.setImage(image);
			return this;
		}

		@Override
		public ColumnBuilder<T> setStyle(int style) {
			super.setStyle(style);
			return this;
		}

		/////////////
		// GETTERS //
		/////////////
		@Nullable
		public ColumnLabelProvider getLabelProvider() {
			return provider;
		}
	}
}
