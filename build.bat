@echo off
if not exist build mkdir build
if not exist dist mkdir dist
dir /s /b src\*.java > .sources.txt
rem --release 8 compiles against the real Java 8 library, so a call to anything newer fails
rem here rather than on a user's Java 8 install. A Java 8 compiler lacks the flag.
javac --release 8 -version >nul 2>&1
if errorlevel 1 (set RELEASE=-source 8 -target 8) else (set RELEASE=--release 8)
javac %RELEASE% -d build @.sources.txt
del .sources.txt
jar cfm dist\cf-evolution.jar META-INF\MANIFEST.MF -C build .
echo Build complete: dist\cf-evolution.jar
