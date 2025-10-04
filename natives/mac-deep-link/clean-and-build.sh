#!/bin/bash

for arch in aarch64 x86_64; do
    rm -rf ../../durian-swt.cocoa.macosx.${arch}/src/main/resources/durian-swt-natives
    mkdir -p ../../durian-swt.cocoa.macosx.${arch}/src/main/resources/durian-swt-natives
    if [ "$arch" = "aarch64" ]; then
        ./compile-one.sh arm64
    else
        ./compile-one.sh ${arch}
    fi
    mv DeepLinkBridge.dylib ../../durian-swt.cocoa.macosx.${arch}/src/main/resources/durian-swt-natives
done