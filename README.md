# <img align="left" src="durian-swt.png"> DurianSwt: Reactive utilities and fluent builders for SWT

<!---freshmark shields
output = [
    link(shield('Maven central', 'mavencentral', 'com.diffplug.durian:durian-swt', 'blue'), 'https://search.maven.org/artifact/com.diffplug.durian/durian-swt'),
    link(shield('Apache 2.0', 'license', 'apache-2.0', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
    '',
    link(shield('Changelog', 'changelog', versionLast, 'brightgreen'), 'CHANGES.md'),
    link(shield('Javadoc', 'javadoc', 'yes', 'brightgreen'), 'https://javadoc.io/static/com.diffplug.durian/durian-swt/{{versionLast}}/overview-summary.html'),
    link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/diffplug/durian'),
    link(image('Travis CI', 'https://travis-ci.org/diffplug/durian-swt.svg?branch=master'), 'https://travis-ci.org/diffplug/durian-swt'),
    ].join('\n');
-->
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.durian%3Adurian--swt-blue.svg)](https://search.maven.org/artifact/com.diffplug.durian/durian-swt)
[![Apache 2.0](https://img.shields.io/badge/license-apache--2.0-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-3.3.1-brightgreen.svg)](CHANGES.md)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-brightgreen.svg)](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/overview-summary.html)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/durian)
[![Travis CI](https://travis-ci.org/diffplug/durian-swt.svg?branch=master)](https://travis-ci.org/diffplug/durian-swt)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://javadoc.io/static/com.diffplug.durian/durian-swt/', '/', versionLast);
-->
### Infrastructure

* [`ControlWrapper`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/ControlWrapper.html) - create custom widgets which properly encapsulate their base control.
* [`Coat`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/Coat.html) - a functional interface for populating an empty Composite.
* [`CoatMux`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/CoatMux.html) - a mechanism for layering and swapping Coats.
* [`SwtExec`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/SwtExec.html) - an `ExecutorService` which executes on the SWT thread.
* [`SwtExec.Guarded`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/SwtExec.Guarded.html) - an `ExecutorService` which is tied to the lifetime of an SWT widget. Say goodbye to `SWTException: Widget is disposed` forever! It can also subscribe to any kind of observable (Guava's ListenableFuture or RxJava's Observable), see [DurianRx](https://github.com/diffplug/durian-rx) for more info.

```java
SwtExec.async().guardOn(textBox).subscribe(serverResponse, txt -> {
  textBox.setText(txt);
});
```

### Fluent builders

* [`Layouts`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/Layouts.html) - all the layouts you'll need in SWT

```java
void textOkCanel(Composite cmp) {
  Layouts.setGrid(cmp).numColumns(3);

  // instructions fill the full width
  Text text = new Text(cmp, SWT.WRAP);
  Layouts.setGridData(text).horizontalSpan(3).grabAll();

  // right-justified ok / cancel buttons
  Layouts.newGridPlaceholder(cmp).grabHorizontal();
  Button btnOk = new Button(cmp, SWT.PUSH);
  Layouts.setGridData(btn).widthHint(SwtMisc.defaultButtonWidth());
  Button btnCancel = new Button(cmp, SWT.PUSH);
  Layouts.setGridData(btn).widthHint(SwtMisc.defaultButtonWidth());
}
```

* [`Shells`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/Shells.html) - dialogs without boilerplate

```java
Shells.builder(SWT.DIALOG_TRIM, this::textOkCanel)
  .setTitle("Confirm operation")
  .setSize(SwtMisc.defaultDialogWidth(), 0) // set the width, pack height to fit contents
  .openOnDisplayBlocking();
```

* [`Actions`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/jface/Actions.html) - builder and one-liner:
`Actions.create("Redo", this::redo);`

* [`LabelProviders`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/jface/LabelProviders.html) - builder and one-liner:
`LabelProviders.createWithText(Person::getName)`

* [`ColumnFormat`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/ColumnFormat.html) and [`ColumnViewerFormat`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/jface/ColumnViewerFormat.html) - tables and trees without boilerplate

```java
ColumnViewerFormat<Person> format = ColumnViewerFormat.builder();
format.setStyle(SWT.SINGLE | SWT.FULL_SELECTION);
format.addColumn().setText("First").setLabelProviderText(Person::getFirstName);
format.addColumn().setText("Last").setLabelProviderText(Person::getLastName);
format.addColumn().setText("Age").setLabelProviderText(p -> Integer.toString(p.getAge())).setLayoutPixel(3 * SwtMisc.systemFontWidth());
TableViewer table = format.buildTable(parent);
TreeViewer tree = format.buildTree(parent);
```

### Resource management

* [`OnePerWidget`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/OnePerWidget.html) - a cache tied to the lifetime of an SWT Widget.
* [`ColorPool`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/ColorPool.html) - a pool of colors tied to the lifetime of a widget. `ColorPool.forWidget(widget).getColor(rgbValue)`
* [`ImageDescriptors`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/jface/ImageDescriptors.html) - use ImageDescriptors with proper resource sharing. `ImageDescriptors.set(btn, imageDescriptor)`

### Interactive testing

Ideally, all UI code would have fully automated UI testing, but
such tests are time-consuming to write, so they often just don't
get written at all. [`InteractiveTest`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/InteractiveTest.html)
bridges the gap by making it easy to write user-in-the-loop guided tests. Furthermore,
these tests can even be run in a [headless enviroment on a CI server](https://github.com/diffplug/durian-swt/blob/master/build.gradle#L66-L93), where the test UI
will be opened, then automatically closed after a timeout.  This ensures that the tests
are all in working order and ready for a human tester to do final validation.

![InteractiveTest](interactive-test.png)

From [`ViewerMiscTest.java`](https://github.com/diffplug/durian-swt/blob/master/test/com/diffplug/common/swt/jface/ViewerMiscTest.java):

```java
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
  RxOptional<TreeNode<String>> tableSelection = ViewerMisc.singleSelection(tableAndTree.table);
  Rx.subscribe(treeSelection, tableSelection::set);
  Rx.subscribe(tableSelection, treeSelection::set);
});
```

### Miscellaneous stuff

* [`SwtMisc`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/SwtMisc.html) - useful static methods.
  + `blockForError`, `blockForSuccess`, `blockForQuestion`, etc. - opens a dialog and blocks for the user's response, can be called from any thread.
  + `loopUntil`, `loopUntilDisposed`, `loopUntilGet` - spins the SWT display loop until some condition is satisfied.
  + `systemFontHeight/Width`, `scaleByFont`, `scaleByFontHeight` - resolution-independent sizes.
  + `treeDefControl`, `treeDefComposite` - a [`TreeDef`](http://diffplug.github.io/durian/javadoc/snapshot/com/diffplug/common/base/TreeDef.html) for traversing UI elements.
  + `setEnabledDeep` - sets the enabled status of every child, grandchild, etc. of the given composite.
* [`SwtRx`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/SwtRx.html) - methods for converting SWT events and models to RxJava Observables.
* [`SwtDebug`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/SwtDebug.html) - utilities for debugging SWT events.
* [`OS`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/os/OS.html), [`Arch`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/os/Arch.html), and [`SwtPlatform`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/os/SwtPlatform.html) - detect things about the running system, and manipulate the SWT jars for build tools.
  + These do not require SWT or JFace, so you can add DurianSwt to your gradle or maven dependencies without needing to also figure out the SWT messiness.
  + You can also just copy-paste these straight into your own code - they have no external dependencies.
```java
String installerExtension = OS.getNative().winMacLinux("exe","dmg","sh");
String helperBinary = "driver_" + Arch.getNative().x86x64("", "_64") + ".dll";
String swtJarName = "org.eclipse.swt." + SwtPlatform.getRunning();
```
* [`ViewerMisc`](https://javadoc.io/static/com.diffplug.durian/durian-swt/3.3.1/com/diffplug/common/swt/jface/ViewerMisc.html) - useful static methods for JFace viewers.
  + `singleSelection`, `multiSelection` - returns an RxBox for listening to and setting the selection of a viewer.
  + `setTreeContentProvider`, `setLazyTreeContentProvider` - uses a TreeDef to provide the content of a TreeViewer.

<!---freshmark /javadoc -->

## Requirements

Durian requires:
* Java 8
* [Durian](https://github.com/diffplug/durian) and [DurianRx](https://github.com/diffplug/durian-rx)
* [Guava](https://github.com/google/guava) and [RxJava](https://github.com/reactivex/rxjava)
* SWT and JFace from Eclipse 4.4+
  + SWT and JFace are not included in the Maven POM, but everything else is.

## Acknowledgements

* Thanks to [David Karnok](https://akarnokd.blogspot.com/) for [contributing an SwtScheduler that honors the Scheduler/Worker contracts](https://github.com/diffplug/durian-swt/pull/1).
* Thanks to Moritz Post for his [fluent layout idea](http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/).
* Formatted by [spotless](https://github.com/diffplug/spotless), [as such](https://github.com/diffplug/durian-rx/blob/v1.0/build.gradle?ts=4#L70-L90).
* Bugs found by [findbugs](http://findbugs.sourceforge.net/), [as such](https://github.com/diffplug/durian-rx/blob/v1.0/build.gradle?ts=4#L92-L116).
* OSGi metadata generated by JRuyi's [osgibnd-gradle-plugin] (https://github.com/jruyi/osgibnd-gradle-plugin), which leverages Peter Kriens' [bnd](http://www.aqute.biz/Bnd/Bnd).
* Scripts in the `.ci` folder are inspired by [Ben Limmer's work](http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Maintained by [DiffPlug](http://www.diffplug.com/).
