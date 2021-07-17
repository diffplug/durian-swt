# DurianSwt releases

## [Unreleased]
### Added
- Support for Apple Silicon in the `OS`, `Arch`, and `SwtPlatform` classes. ([#13](https://github.com/diffplug/durian-swt/pull/13))

## [3.3.1] - 2020-01-13
### Fixed
- Improved javadoc publishing (though SWT is still broken)

## [3.3.0] - 2019-11-02
- Added `FlatBtn.setBackground` and `FlatBtn.setSelection`.
- Added `Shells.dontSetPositionOrSize` and `Shells.setCloseOnEscape`.
- Shells now reposition themselves if they were going to open offscreen.
- Shells no longer set their image on Mac, so that they don't hijack the dock icon.
- Added `ControlWrapper.transparent` for efficiently passing `Control` to `ControlWrapper`.
- Added `ControlWrapper.setParent` which is a pure delegation to `Control.setParent`.
- Improved `StructuredDrop.handler`.
- Added `SwtMisc.globalBounds` for `ControlWrapper`.
- `SwtDebug.dumpEvent` is now null-safe.

## [3.2.1] - 2019-08-07
- Added a non-coat version of `RadioGroup`.
- Fixed `Shells.openOnActive()` to take advantage of `Shells.active()`.

## [3.2.0] - 2019-04-29
- `Fonts.systemLarge()` was hardcoded to 12 points.  This worked well on standard DPI windows, but was actually smaller than the normal font on retina mac.  Now adds 33% to the normal size, which exactly matches the previous behavior on Windows, but works better on other platforms.
- Added `SwtMisc.getSystemCursor` and `getSystemIcon`.
- Added `Shells.dontOpen` which makes it possible to create a shell that doesn't open.
- Added `Shells.active` which finds the currently active shell.

## [3.1.1] - 2019-04-24
- Fixed a bug in `StructuredDrop` which caused `dragLeave` events to get swallowed.

## [3.1.0] - 2019-01-02
- `Shells` now detects the monitor using the center of the proposed bounds, rather than the top-left.
- Added `StructuredDrag.copyToClipboard()` and `StructuredDrop.pasteFromClipboard()` [(#2)](https://github.com/diffplug/durian-swt/pull/2).
- Reduced noise in `InteractiveTest` failures.
- Added `ImageDescriptors.getFromPool()`.
- Added `Actions.run()` which updates the `IAction.isChecked`.
- Made the spacebar visible in `Actions.getAcceleratorString()`.
- Fix `StructuredDrop` for files when executing multiple drags from the desktop.

## [3.0.0] - 2018-08-01
* Extracted the `com.diffplug.common.swt.os` package into its own jar with no dependencies, published to `com.diffplug.durian:durian-swt.os`.
* Added `com.diffplug.durian:durian-swt.{SWT_PLATFORM_CODE}` for each of `win x86`, `win x64`, `linux x86`, `linux x64`, `mac x64`.
  + Only used for `SmoothTable`, if you don't use `SmoothTable` then you don't need the platform-specific jar.
+ Added useful DnD stuff to `com.diffplug.common.swt.dnd.`
+ Added useful widgets to `com.diffplug.common.swt.widgets.`

## [3.0.0.BETA2] - 2017-03-08
* Added `TypedDataField` for storing strongly-typed data into widgets.
* Added `SwtExec.guardOn(DisposableEar ear)`.
* `InteractiveTest.setResult()` can now be called from any thread, and the user can pass any exception.
* Implemented `scheduleAtFixedRate` and `scheduleWithFixedDelay` for `SwtExec` and `SwtExec.Guarded`.
* Improved `CoatMux`'s reliability.
* Deprecated `SwtMisc.requestLayout`.

## [3.0.0.BETA] - 2017-02-07
* `SwtExec.Guarded` is now a `GuardedExecutor`.
* Added `SwtRx.disposableEar()`.
* Added `SwtPlatform.toOS()`.
* Improvements to `SwtRx.textImmediate` and `SwtRx.textConfirmed`:
  * now prevents double-calls to `setText(String str)`.
  * selections are now preserved in a more intuitive way when the text changes.
* `@SwtThread` can now annotate exceptions to the rules with `@SwtThread(SwtThread.Kind.THREADSAFE)`.
* Added `Corner.getPosition(Control | ToolItem)`.
* Added `Shells.setAlpha(int alpha)`.

## [3.0.0.ALPHA] - 2016-11-11
* Bumped RxJava to 2.0, and dealt with some initial fallout from that change.

## [2.0.0] - 2016-07-13
* Introduced the `SwtThread` annotation to mark that a method is only safe to use from an SWT thread.
* Added `SwtExec.swtOnly()` as a high-performance, non-thread-safe version of `SwtExec.immediate()` (thanks to David Karnok).
* Renamed `Shells.openOnAndBlock()` to `Shells.openOnBlocking()`.
* `SwtScheduler` now honors the `Scheduler/Worker` contracts (thanks to David Karnok).
* `ColumnFormat` and `ColumnViewerFormat` now expose all their data through getters.
* Fallout from 2.0 bump of durian-rx.

## [1.7.0] - 2016-04-06
* Upgraded Eclipse/SWT dependencies from Luna SR2 to Mars.2 (4.4.2 to 4.5.2).
* Changed `SwtDebug` event names to their 4.5-based names.
* `InteractiveTest` can now pass/fail itself automatically, for self-testing gui tests.
* Added `SwtRx.combo()` methods for reactive control of combo boxes.
* Added `SwtMisc.requestLayout()`, which is backported from Eclipse Neon.  It will be deprecated when Neon comes out.

## [1.6.0] - 2016-02-09
* Ditched Guava for DurianGuava.

## [1.5.1] - 2015-12-30
* Fixed a bug in `OS.calculateNative` on linux x86 systems that report their `os.arch` as `i386`.

## [1.5.0] - 2015-12-11
* Added `Actions.setCallback`, which allows behavior based on the actual IAction which ends up being created.
* Added `SwtMisc.copyEvent()`.
* Added `SwtMisc.withGcRun` and `SwtMisc.withGcCompute`, which allows quick access to a GC for e.g. computing the size of some text.

## [1.4.0] - 2015-11-18
* `SwtExec.Guarded::subscribe` now supports `CompletionStage` and `CompletableFuture`, in support of improvements made to DurianRx 1.2.0.
* Added `SwtMisc.setFlag()`.
* Added `JFaceRx`, which exposes JFace properties as RxJava observables.

## [1.3.0] - 2015-10-19
* Changed OSGi metadata Bundle-SymbolicName to `com.diffplug.durian.swt`.
* OSGi metadata is now auto-generated using bnd.
* Added `ColumnViewerFormat.ColumnBuilder::setFinalSetup()` which allows us to get the `ViewerColumn` object and setup something such as a `CellEditor` on it.
* `ColumnViewerFormat` no longer requires a `LabelProvider`, since we might set that stuff up in `setFinalSetup()`.
* Fixed a bug in `Shells` which caused windows to always open on the primary monitor.
* `Actions` is now based on `runWithEvent()` rather than `run()`.  This allows implementors to get the `Event` which is causing the action to fire, which can allow more detailed actions to be taken.

## [1.2.0] - 2015-09-14
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

## [1.1.1] - 2015-07-27
* Gah! MANIFEST.MF still had -SNAPSHOT version.  Fixed now.  Would be really nice if we could get MANIFEST.MF generation working.

## [1.1] - 2015-07-26
* Fixed a linux-specific bug in `Shells.openOnActive()`.
* Fixed an NPE in `Shells.openOnActive()`.
* Made `InteractiveTest.testShellWithoutHandle()` a little more forgiving.

## [1.0] - 2015-05-13
* First stable release.
