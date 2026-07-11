@echo off
if not exist build mkdir build
if not exist dist mkdir dist
dir /s /b src\*.java > .sources.txt
javac -source 8 -target 8 -d build @.sources.txt
del .sources.txt
jar cfm dist\cf-evolution.jar META-INF\MANIFEST.MF -C build .
echo Build complete: dist\cf-evolution.jar
