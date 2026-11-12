#!/bin/bash
mkdir -p build dist
find src -name "*.java" > .sources.txt
# --release 8 compiles against the real Java 8 library, so a call to anything newer fails
# here rather than on a user's Java 8 install. A Java 8 compiler lacks the flag.
if javac --release 8 -version > /dev/null 2>&1; then
    RELEASE="--release 8"
else
    RELEASE="-source 8 -target 8"
fi
javac $RELEASE -d build @.sources.txt
RESULT=$?
rm .sources.txt
# Stop here if compiling failed, rather than packaging the previous build's classes.
if [ $RESULT -ne 0 ]; then
    echo "Build failed: dist/cf-evolution.jar was not updated"
    exit 1
fi
jar cfm dist/cf-evolution.jar META-INF/MANIFEST.MF -C build .
echo "Build complete: dist/cf-evolution.jar"
