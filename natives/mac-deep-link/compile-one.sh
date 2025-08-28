#!/bin/bash

# DeepLinkBridge Compilation Script
#
# Usage: ./compile-one.sh <architecture>
#   architecture: x86_64 (Intel) or arm64 (Apple Silicon)
#
# This script compiles native Objective-C code for handling deep links on macOS.
#
# Prerequisites:
# - Xcode Command Line Tools
# - Java Development Kit (JDK) installed
#
# Output:
# Produces DeepLinkBridge.dylib, a macOS dynamic library that can be loaded by the JVM
# to handle URL open events from the operating system.
#
# The library provides JNI functions that:
# 1. JNI_OnLoad - Stores a reference to the JVM when the library is loaded
# 2. Java_com_diffplug_deeplink_MacDeepLink_installOpenURLHandler - Installs a handler for URL open events that calls back to Java
#
# The library should be loaded early in the application lifecycle before the UI event loop starts.

set -e  # Exit on error

# Check for required architecture argument
if [ $# -ne 1 ]; then
    echo "Error: Architecture argument required"
    echo "Usage: $0 <x86_64|arm64>"
    echo "  x86_64 - Compile for Intel Macs"
    echo "  arm64  - Compile for Apple Silicon Macs"
    exit 1
fi

ARCH=$1

# Validate architecture argument
if [ "$ARCH" != "x86_64" ] && [ "$ARCH" != "arm64" ]; then
    echo "Error: Invalid architecture '$ARCH'"
    echo "Valid architectures: x86_64, arm64"
    exit 1
fi

echo "Compiling for architecture: $ARCH"

echo "Checking for Java installation..."
if ! /usr/libexec/java_home >/dev/null 2>&1; then
    echo "Error: Java not found. Please install a JDK."
    echo "Run: /usr/libexec/java_home to verify Java installation"
    exit 1
fi

echo "Java found at: $(/usr/libexec/java_home)"

# Verify the JNI headers exist
echo "Verifying JNI headers..."
if [ ! -f "$(/usr/libexec/java_home)/include/jni.h" ]; then
    echo "Error: JNI headers not found at $(/usr/libexec/java_home)/include/jni.h"
    echo "Ensure you have a JDK installed (not just JRE)"
    exit 1
fi
echo "JNI headers found."

# Compile the dynamic library
echo "Compiling DeepLinkBridge.dylib for $ARCH..."
clang -arch $ARCH -dynamiclib \
  -I"$(/usr/libexec/java_home)/include" \
  -I"$(/usr/libexec/java_home)/include/darwin" \
  -framework Cocoa \
  -o DeepLinkBridge.dylib \
  DeepLinkBridge.m

echo "Success! DeepLinkBridge.dylib has been created for $ARCH architecture."
echo ""
echo "Verifying architecture:"
file DeepLinkBridge.dylib