# DurianSwt releases

### Version 1.3.0 - TBD ([javadoc](http://diffplug.github.io/durian-swt/javadoc/snapshot/))

* Changed OSGi metadata Bundle-SymbolicName to `com.diffplug.durian.swt`.
* OSGi metadata is now auto-generated using bnd.
* Added `ColumnViewerFormat.ColumnBuilder::setFinalSetup()` which allows us to get the `ViewerColumn` object and setup something such as a `CellEditor` on it.
* `ColumnViewerFormat` no longer requires a `LabelProvider`, since we might set that stuff up in `setFinalSetup()`.
* Fixed a bug in `Shells` which caused windows to always open on the primary monitor.

### Version 1.2.0 - September 14th 2015 ([javadoc](http://diffplug.github.io/durian-swt/javadoc/1.2.0/), [jcenter](https://bintray.com/diffplug/opensource/durian-swt/1.2.0/view))

* Major improvement to the `CoatMux` API.
* Added `ColorPool.getSystemColor(int systemColor)` so that ColorPool can be your one-stop-shop for getting colors.
* Added some methods to `SwtMisc`:
	* `Shell rootShell(Control control)`
	* `void forEachDeep(Composite root, Consumer<Control> ctlSetter)`
	* `void setForegroundDeep(Composite root, Color foreground)`
	* `void setBackgroundDeep(Composite root, Color background)`
	* `Rectangle globalBounds(Control control)`
	* `Rectangle toDisplay(Control control, Rectangle rect)`
	* `Optional<Monitor> monitorFor(Point p)`
* Fixed `SwtMisc.setEnabledDeep()` - it now skips `Composite`s which haven't been subclassed.
* Fixed `SwtRx.toggle()` now works with radio buttons.
* Fixed `Shells` so that the dialogs it creates don't automatically close on escape, and so that they are better about opening on-screen.

### Version 1.1.1 - July 27th 2015 ([javadoc](http://diffplug.github.io/durian-swt/javadoc/1.1.1/), [jcenter](https://bintray.com/diffplug/opensource/durian-swt/1.1.1/view))

* Gah! MANIFEST.MF still had -SNAPSHOT version.  Fixed now.  Would be really nice if we could get MANIFEST.MF generation working.

### Version 1.1 - July 26th 2015 ([javadoc](http://diffplug.github.io/durian-swt/javadoc/1.1/), [jcenter](https://bintray.com/diffplug/opensource/durian-swt/1.1/view))

* Fixed a linux-specific bug in `Shells.openOnActive()`.
* Fixed an NPE in `Shells.openOnActive()`.
* Made `InteractiveTest.testShellWithoutHandle()` a little more forgiving.

### Version 1.0 - May 13th 2015 ([javadoc](http://diffplug.github.io/durian-swt/javadoc/1.0/), [jcenter](https://bintray.com/diffplug/opensource/durian-swt/1.0/view))

* First stable release.
