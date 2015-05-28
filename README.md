# <img align="left" src="durian-swt.png"> DurianSwt: Reactive utilities and fluent builders for SWT

[![Maven artifact](https://img.shields.io/badge/mavenCentral-com.diffplug.durian%3Adurian--swt-blue.svg)](https://bintray.com/diffplug/opensource/durian-swt/view)
[![Latest release](http://img.shields.io/badge/last release-None yet-blue.svg)](https://github.com/diffplug/durian-swt/releases/latest)
[![Changelog](http://img.shields.io/badge/master-1.0--SNAPSHOT-lightgrey.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/durian-swt.svg?branch=master)](https://travis-ci.org/diffplug/durian-swt)
[![License](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

# NOT YET SUITABLE FOR USE - we're releasing a formerly internal library, bear with us as we clean it up for public release

### Infrastructure

* [`ControlWrapper`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/ControlWrapper.html) - create custom widgets which properly encapsulate their base control.
* [`Coat`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/Coat.html) - a functional interface for populating an empty Composite.
* [`SwtExec`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/SwtExec.html) - an `ExecutorService` which executes on the SWT thread.
* [`SwtExec.Guarded`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/SwtExec.Guarded.html) - an `Executor` which is tied to the lifetime of an SWT widget. Say goodbye to `SWTException: Widget is disposed` forever! It can also subscribe to any kind of observable (Guava's ListenableFuture or RxJava's Observable), see [DurianRx](https://github.com/diffplug/durian-rx) for more info. 
```java
SwtExec.async().guardOn(textBox).subscribe(serverResponse, txt -> {
	textBox.setText(txt);
});
```
### Fluent builders

* [`Layouts`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/Layouts.html) - all the layouts you'll need in SWT

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

* [`Shells`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/Shells.html) - dialogs without boilerplate

```java
Shells.create(SWT.DIALOG_TRIM, this::textOkCanel)
	.setTitle("Confirm operation")
	.setSize(SwtMisc.defaultDialogWidth(), 0) // set the width, pack height to fit contents
	.openOnDisplayBlocking();
```

### Resource management

* [`OnePerWidget`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/OnePerWidget.html) - a cache tied to the lifetime of an SWT Widget.
* [`ColorPool`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/ColorPool.html) - a pool of colors tied to the lifetime of a widget. `ColorPool.forWidget(widget).getColor(rgbValue)`
* [`ImageDescriptors`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/ImageDescriptors.html) - use ImageDescriptors with proper resource sharing. `ImageDescriptors.set(btn, imageDescriptor)`

### Miscellaneous stuff

* [`SwtMisc`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/SwtMisc.html) - useful static methods.
	+ `blockForError`, `blockForSuccess`, `blockForQuestion`, etc. - opens a dialog and blocks for the user's response, can be called from any thread.
	+ `loopUntil`, `loopUntilDisposed`, `loopUntilGet` - spins the SWT display loop until some condition is satisfied.
	+ `systemFontHeight/Width`, `scaleByFont`, `scaleByFontHeight` - resolution-independent sizes.
	+ `treeDefControl`, `treeDefComposite` - a [`TreeDef`](http://diffplug.github.io/durian/javadoc/snapshot/com/diffplug/common/base/TreeDef.html) for traversing UI elements.
	+ `setEnabledDeep` - sets the enabled status of every child, grandchild, etc. of the given composite.

* [`OS`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/os/OS.html), [`Arch`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/os/Arch.html), and [`SwtPlatform`](http://diffplug.github.io/durian-swt/javadoc/snapshot/com/diffplug/common/swt/os/SwtPlatform.html) - detect things about the running system, and manipulate the SWT jars for build tools.
	+ These do not require SWT or JFace, so you can add DurianSwt to your gradle or maven dependencies without needing to also figure out the SWT messiness.
	+ You can also just copy-paste these straight into your own code - they have no external dependencies.
```java
String installerExtension = OS.getNative().winMacLinux("exe","dmg","sh");
String helperBinary = "driver_" + Arch.getNative().x86x64("", "_64") + ".dll";
String swtJarName = "org.eclipse.swt." + SwtPlatform.getRunning();
```

## Requirements

Durian requires:
* Java 8
* [Durian](https://github.com/diffplug/durian) and [DurianRx](https://github.com/diffplug/durian-rx)
* [Guava](https://github.com/google/guava) and [RxJava](https://github.com/ReactiveX/RxJava)
* SWT and JFace from Eclipse 4.4+
	+ SWT and JFace are not included in the Maven POM, but everything else is.

## Acknowledgements

* Thanks to Moritz Post for his [fluent layout idea](http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/).
* Formatted by [spotless](https://github.com/diffplug/spotless), [as such](https://github.com/diffplug/durian-rx/blob/v1.0/build.gradle?ts=4#L70-L90).
* Bugs found by [findbugs](http://findbugs.sourceforge.net/), [as such](https://github.com/diffplug/durian-rx/blob/v1.0/build.gradle?ts=4#L92-L116).
* Scripts in the `.ci` folder are inspired by [Ben Limmer's work](http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Maintained by [DiffPlug](http://www.diffplug.com/).
