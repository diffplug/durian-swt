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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.common.collect.Immutables;
import com.diffplug.common.rx.Rx;
import com.diffplug.common.rx.RxBox;
import com.diffplug.common.swt.InteractiveTest;
import com.diffplug.common.swt.Layouts;
import com.diffplug.common.swt.SwtMisc;
import com.diffplug.common.swt.jface.ColumnViewerFormat;
import com.diffplug.common.swt.jface.ViewerMisc;
import com.diffplug.common.tree.TreeDef;
import com.diffplug.common.tree.TreeNode;
import com.diffplug.common.tree.TreeStream;

@Category(InteractiveTest.class)
public class ViewerMiscTest {
	TreeNode<String> testData = TreeNode.createTestData(
			"root",
			" Heroes",
			"  Luke Skywalker",
			"  Leia Organa",
			" Villains",
			"  Anakin Skywalker",
			"  Sheev Palpatine",
			" Disasters",
			"  Jarjar Binks",
			"  Padme Amidala");

	/** Returns true iff the given TreeNode is a name. */
	private static boolean isName(TreeNode<String> node) {
		return node.getContent().contains(" ");
	}

	/** Returns a function for finding the given place in the given name in the TreeNode. */
	private static Function<TreeNode<String>, String> getPlace(int place) {
		return node -> {
			String obj = node.getContent();
			String[] pieces = obj.split(" ");
			return place < pieces.length ? pieces[place] : "";
		};
	}

	/** Creates a TableViewer and TreeViewer. */
	private class TableAndTree {
		final TableViewer table;
		final TreeViewer tree;

		TableAndTree(Composite cmp, int style) {
			Layouts.setFill(cmp);

			ColumnViewerFormat<TreeNode<String>> format = ColumnViewerFormat.builder();
			format.setStyle(style | SWT.FULL_SELECTION);
			format.addColumn().setText("First").setLabelProviderText(getPlace(0));
			format.addColumn().setText("Last").setLabelProviderText(getPlace(1));

			// create a table
			table = format.buildTable(new Composite(cmp, SWT.BORDER));
			table.setContentProvider(new ArrayContentProvider());
			List<TreeNode<String>> listInput = TreeStream.depthFirst(TreeNode.treeDef(), testData)
					.filter(node -> node.getContent().contains(" "))
					.collect(Collectors.toList());
			table.setInput(listInput);

			// and a tree
			tree = format.buildTree(new Composite(cmp, SWT.BORDER));
			ViewerMisc.setTreeContentProvider(tree, TreeNode.treeDef());
			tree.setInput(testData);
		}
	}

	@Test
	public void testSingleSelection() {
		String message = StringPrinter.buildStringFromLines(
				"- The table and the tree should keep their selection in sync.",
				"- The table and the tree should not allow multi-selection.",
				"- The categories in the tree should not be selectable.");
		InteractiveTest.testCoat(message, cmp -> {
			TableAndTree tableAndTree = new TableAndTree(cmp, SWT.SINGLE);

			// get the selection of the tree
			RxBox<Optional<TreeNode<String>>> treeSelection = ViewerMisc.<TreeNode<String>> singleSelection(tableAndTree.tree)
					// only names can be selected - not categories
					.enforce(opt -> opt.map(val -> isName(val) ? val : null));

			// sync the tree and the table
			RxBox<Optional<TreeNode<String>>> tableSelection = ViewerMisc.singleSelection(tableAndTree.table);
			Rx.subscribe(treeSelection, tableSelection::set);
			Rx.subscribe(tableSelection, treeSelection::set);
		});
	}

	@Test
	public void testMultiSelectionList() {
		String message = StringPrinter.buildStringFromLines(
				"- The table and the tree should keep their selection in sync.",
				"- The table and the tree should allow multi-selection.",
				"- The categories in the tree should not be selectable.");
		InteractiveTest.testCoat(message, cmp -> {
			TableAndTree tableAndTree = new TableAndTree(cmp, SWT.MULTI);

			// ensure the tree only supports selecting names
			RxBox<ImmutableList<TreeNode<String>>> treeSelection = ViewerMisc.<TreeNode<String>> multiSelectionList(tableAndTree.tree)
					// remove any nodes that aren't a name
					.enforce(Immutables.mutatorList(mutable -> mutable.removeIf(node -> !isName(node))));

			// sync the tree and the table
			RxBox<ImmutableList<TreeNode<String>>> tableSelection = ViewerMisc.multiSelectionList(tableAndTree.table);
			Rx.subscribe(treeSelection, tableSelection::set);
			Rx.subscribe(tableSelection, treeSelection::set);
		});
	}

	@Test
	public void testMultiSelectionSet() {
		String message = StringPrinter.buildStringFromLines(
				"- The table and the tree should keep their selection in sync.",
				"- The table and the tree should allow multi-selection.",
				"- The categories in the tree should not be selectable.");
		InteractiveTest.testCoat(message, cmp -> {
			TableAndTree tableAndTree = new TableAndTree(cmp, SWT.MULTI);

			// ensure the tree only supports selecting names
			RxBox<ImmutableSet<TreeNode<String>>> treeSelection = ViewerMisc.<TreeNode<String>> multiSelectionSet(tableAndTree.tree)
					// remove any nodes that aren't a name
					.enforce(Immutables.mutatorSet(mutable -> mutable.removeIf(node -> !isName(node))));

			// sync the tree and the table
			RxBox<ImmutableSet<TreeNode<String>>> tableSelection = ViewerMisc.multiSelectionSet(tableAndTree.table);
			Rx.subscribe(treeSelection, tableSelection::set);
			Rx.subscribe(tableSelection, treeSelection::set);
		});
	}

	@Test
	public void testLazyContentProviderFile() {
		InteractiveTest.testCoat("You should be able to browse the filesystem.", cmp -> {
			// define the format of the tree
			ColumnViewerFormat<File> format = ColumnViewerFormat.builder();
			format.setStyle(SWT.VIRTUAL);
			format.addColumn().setText("Name")
					.setLabelProviderText(File::getName);
			format.addColumn().setText("Last Modified")
					.setLabelProviderText(file -> new Date(file.lastModified()).toString())
					.setLayoutPixel(SwtMisc.systemFontWidth() * new Date().toString().length());

			// create the tree viewer
			TreeViewer viewer = format.buildTree(cmp);
			// define the structure of the tree's contents
			ViewerMisc.setLazyTreeContentProvider(viewer, TreeDef.forFile(Errors.suppress()));

			// set the tree's root
			File[] roots = File.listRoots();
			if (roots.length == 1) {
				viewer.setInput(roots[0]);
			} else {
				Optional<File> cDrive = Arrays.asList(roots).stream().filter(file -> file.getAbsolutePath().contains("C")).findFirst();
				viewer.setInput(cDrive.orElse(roots[0]));
			}
		});
	}

	@Test
	public void testLazyContentProviderPath() {
		InteractiveTest.testCoat("You should be able to browse the filesystem.", cmp -> {
			// define the format of the tree
			ColumnViewerFormat<Path> format = ColumnViewerFormat.builder();
			format.setStyle(SWT.VIRTUAL);
			format.addColumn().setText("Name")
					.setLabelProviderText(path -> path.getFileName().toString());
			format.addColumn().setText("Last Modified")
					.setLabelProviderText(path -> Errors.suppress().getWithDefault(() -> Files.getLastModifiedTime(path).toString(), ""))
					.setLayoutPixel(SwtMisc.systemFontWidth() * new Date().toString().length());

			// create the tree viewer
			TreeViewer viewer = format.buildTree(cmp);
			// define the structure of the tree's contents
			ViewerMisc.setLazyTreeContentProvider(viewer, TreeDef.forPath(Errors.suppress()));

			// set the tree's root
			File[] roots = File.listRoots();
			if (roots.length == 1) {
				viewer.setInput(roots[0].toPath());
			} else {
				Optional<File> cDrive = Arrays.asList(roots).stream().filter(file -> file.getAbsolutePath().contains("C")).findFirst();
				viewer.setInput(cDrive.orElse(roots[0]).toPath());
			}
		});
	}
}
