# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This is a Gradle-based Java/Kotlin project focused on SWT (Standard Widget Toolkit) utilities. Standard gradle tasks.

### macOS-specific Requirements
- On macOS, SWT tests require the `-XstartOnFirstThread` JVM argument (automatically configured in build.gradle).

## Project Architecture
### Multi-Module Structure
DurianSwt is organized as a multi-module Gradle project with platform-specific implementations:

- **durian-swt.os**: Platform detection utilities (OS, Arch, SwtPlatform) - no SWT dependencies
- **durian-swt**: Main module with core SWT utilities and builders
- **platform-specific modules**: (durian-swt.cocoa.macosx.aarch64, durian-swt.cocoa.macosx.x86_64, durian-swt.gtk.linux.x86_64, durian-swt.win32.win32.x86_64)

## Native Components

### macOS Deep Link Support (`natives/mac-deep-link/`)

Contains Objective-C code for handling deep links via a custom `diffplug://` protocol on macOS without breaking SWT:

- **DeepLinkBridge.m**: JNI bridge that intercepts URL open events from macOS
- **compile-one.sh**: Compiles the native library for a specific architecture (x86_64 or arm64)
- **clean-and-build.sh**: Builds for both architectures and deploys to appropriate module resources

To rebuild native libraries:
```bash
cd natives/mac-deep-link
./clean-and-build.sh  # Builds for both architectures
# Or compile for specific architecture:
./compile-one.sh arm64    # Apple Silicon
./compile-one.sh x86_64   # Intel
```

The resulting `DeepLinkBridge.dylib` files are placed in the platform-specific modules under `src/main/resources/durian-swt-natives/`.
