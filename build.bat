@echo off
rem Start from an empty build folder, so classes from deleted or renamed source never reach the jar.
if exist build rmdir /s /q build
mkdir build\core
mkdir build\desktop
if not exist dist mkdir dist
rem The core (data, generator, edit) is everything an interface edits through: it is compiled
rem first and on its own, so it can never come to depend on the Swing interface, and is also
rem packaged alone as dist\cf-evolution-core.jar for other interfaces (such as Android) to use.
dir /s /b src\cfevolution\data\*.java src\cfevolution\generator\*.java src\cfevolution\edit\*.java > .core-sources.txt
dir /s /b src\*.java | findstr /v /i /c:"\src\cfevolution\data\" /c:"\src\cfevolution\generator\" /c:"\src\cfevolution\edit\" > .desktop-sources.txt
rem --release 8 compiles against the real Java 8 library, so a call to anything newer fails
rem here rather than on a user's Java 8 install. A Java 8 compiler lacks the flag.
javac --release 8 -version >nul 2>&1
if errorlevel 1 (set RELEASE=-source 8 -target 8) else (set RELEASE=--release 8)
javac %RELEASE% -d build\core @.core-sources.txt
set RESULT=%ERRORLEVEL%
if "%RESULT%"=="0" (
    javac %RELEASE% -cp build\core -d build\desktop @.desktop-sources.txt
)
if "%RESULT%"=="0" set RESULT=%ERRORLEVEL%
del .core-sources.txt
del .desktop-sources.txt
rem Stop here if compiling failed, rather than packaging the previous build's classes.
if not "%RESULT%"=="0" (
    echo Build failed: dist\cf-evolution.jar was not updated
    exit /b 1
)
jar cfm dist\cf-evolution.jar META-INF\MANIFEST.MF -C build\core . -C build\desktop . META-INF\services
jar cf dist\cf-evolution-core.jar -C build\core .
echo Build complete: dist\cf-evolution.jar and dist\cf-evolution-core.jar
