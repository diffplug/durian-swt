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
package com.diffplug.common.swt.jface;

import java.util.Optional;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;

import com.diffplug.common.base.Converter;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.SwtExec;
import com.diffplug.common.swt.SwtMisc;
import com.diffplug.common.swt.SwtThread;
import com.diffplug.common.tree.TreeDef;

/** Utilities for manipulating and creating JFace viewers. */
public class ViewerMisc {
	/** Returns a thread-safe `RxBox<Optional>` for manipulating the selection of a {@link StructuredViewer} created with {@link SWT#SINGLE}. */
	public static @SwtThread <T> RxBox<Optional<T>> singleSelection(StructuredViewer viewer) {
		RxBox<Optional<T>> box = RxBox.of(Optional.empty());
		singleSelection(viewer, box);
		return box;
	}

	/** Returns a thread-safe `RxBox<Optional>` for manipulating the selection of a {@link StructuredViewer} created with {@link SWT#SINGLE}. */
	public static <T> void singleSelection(StructuredViewer viewer, RxBox<Optional<T>> box) {
		Preconditions.checkArgument(SwtMisc.flagIsSet(SWT.SINGLE, viewer.getControl()), "Control style does not have SWT.SINGLE set.");
		// set the box when the selection changes
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			@SuppressWarnings("unchecked")
			T selected = (T) selection.getFirstElement();
			box.set(Optional.ofNullable(selected));
		});
		// set the selection when the box changes
		SwtExec.immediate().guardOn(viewer.getControl()).subscribe(box.asObservable(), optional -> {
			if (optional.isPresent()) {
				viewer.setSelection(new StructuredSelection(optional.get()));
			} else {
				viewer.setSelection(StructuredSelection.EMPTY);
			}
		});
	}

	/** Returns a thread-safe `RxBox<ImmutableSet>` for manipulating the selection of a {@link StructuredViewer} created with {@link SWT#MULTI}. */
	public static @SwtThread <T> RxBox<ImmutableSet<T>> multiSelectionSet(StructuredViewer viewer) {
		RxBox<ImmutableSet<T>> box = RxBox.of(ImmutableSet.of());
		multiSelectionSet(viewer, box);
		return box;
	}

	/** Manipulates the selection of the given viewer with the given `RxBox<ImmutableSet>`. */
	public static <T> void multiSelectionSet(StructuredViewer viewer, RxBox<ImmutableSet<T>> box) {
		Converter<ImmutableSet<T>, ImmutableList<T>> converter = Converter.from(
				ImmutableSet::asList, ImmutableSet::copyOf, "setToList");
		multiSelectionList(viewer, box.map(converter));
	}

	/** Returns a thread-safe `RxBox<ImmutableList>` for manipulating the selection of a {@link StructuredViewer} created with {@link SWT#MULTI}. */
	public static @SwtThread <T> RxBox<ImmutableList<T>> multiSelectionList(StructuredViewer viewer) {
		RxBox<ImmutableList<T>> box = RxBox.of(ImmutableList.of());
		multiSelectionList(viewer, box);
		return box;
	}

	@SuppressWarnings("unchecked")
	public static <T> void multiSelectionList(StructuredViewer viewer, RxBox<ImmutableList<T>> box) {
		Preconditions.checkArgument(SwtMisc.flagIsSet(SWT.MULTI, viewer.getControl()), "Control style does not have SWT.MULTI set.");
		// set the box when the selection changes
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			box.set(ImmutableList.copyOf(selection.toList()));
		});
		// set the selection when the box changes
		SwtExec.immediate().guardOn(viewer.getControl()).subscribe(box, list -> {
			viewer.setSelection(new StructuredSelection(list));
		});
	}

	/** Sets an {@link ITreeContentProvider} implemented by the given {@link TreeDef.Parented}. */
	@SuppressWarnings("unchecked")
	public static <T> void setTreeContentProvider(TreeViewer viewer, TreeDef.Parented<T> treeDef) {
		viewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return treeDef.childrenOf((T) parentElement).toArray();
			}

			@Override
			public Object getParent(Object element) {
				return treeDef.parentOf((T) element);
			}

			@Override
			public boolean hasChildren(Object element) {
				return !treeDef.childrenOf((T) element).isEmpty();
			}

			@Override
			public void dispose() {}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});
	}

	/** Sets an {@link ILazyTreeContentProvider} implemented by the given {@link TreeDef.Parented}. */
	@SuppressWarnings("unchecked")
	public static <T> void setLazyTreeContentProvider(TreeViewer viewer, TreeDef.Parented<T> treeDef) {
		Preconditions.checkArgument(SwtMisc.flagIsSet(SWT.VIRTUAL, viewer.getControl()), "The tree must have SWT.VIRTUAL set.");
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new ILazyTreeContentProvider() {
			@Override
			public void dispose() {}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public void updateElement(Object parent, int index) {
				T child = treeDef.childrenOf((T) parent).get(index);
				viewer.replace(parent, index, child);
				updateChildCount(child, 0);
			}

			@Override
			public void updateChildCount(Object element, int currentChildCount) {
				viewer.setChildCount(element, treeDef.childrenOf((T) element).size());
			}

			@Override
			public Object getParent(Object element) {
				return treeDef.parentOf((T) element);
			}
		});
	}
}
