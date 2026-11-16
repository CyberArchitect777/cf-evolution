#!/bin/bash
# Start from an empty build folder, so classes from deleted or renamed source never reach the jar.
rm -rf build
mkdir -p build/core build/desktop dist
# The core (data, generator, edit) is everything an interface edits through: it is compiled
# first and on its own, so it can never come to depend on the Swing interface, and is also
# packaged alone as dist/cf-evolution-core.jar for other interfaces (such as Android) to use.
CORE="src/cfevolution/data src/cfevolution/generator src/cfevolution/edit"
find $CORE -name "*.java" > .core-sources.txt
find src -name "*.java" | grep -v -e "^src/cfevolution/data/" -e "^src/cfevolution/generator/" -e "^src/cfevolution/edit/" > .desktop-sources.txt
# --release 8 compiles against the real Java 8 library, so a call to anything newer fails
# here rather than on a user's Java 8 install. A Java 8 compiler lacks the flag.
if javac --release 8 -version > /dev/null 2>&1; then
    RELEASE="--release 8"
else
    RELEASE="-source 8 -target 8"
fi
javac $RELEASE -d build/core @.core-sources.txt
RESULT=$?
if [ $RESULT -eq 0 ]; then
    javac $RELEASE -cp build/core -d build/desktop @.desktop-sources.txt
    RESULT=$?
fi
rm .core-sources.txt .desktop-sources.txt
# Stop here if compiling failed, rather than packaging the previous build's classes.
if [ $RESULT -ne 0 ]; then
    echo "Build failed: dist/cf-evolution.jar was not updated"
    exit 1
fi
jar cfm dist/cf-evolution.jar META-INF/MANIFEST.MF -C build/core . -C build/desktop . META-INF/services
jar cf dist/cf-evolution-core.jar -C build/core .
echo "Build complete: dist/cf-evolution.jar and dist/cf-evolution-core.jar"
