#!/bin/bash
mkdir -p build dist
find src -name "*.java" > .sources.txt
javac -source 8 -target 8 -d build @.sources.txt
rm .sources.txt
jar cfm dist/cf-evolution.jar META-INF/MANIFEST.MF -C build .
echo "Build complete: dist/cf-evolution.jar"
