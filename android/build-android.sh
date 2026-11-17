#!/bin/bash
# Builds CF Evolution for Android: dist/cf-evolution-android.apk (2026-11-17).
#
# No Gradle: javac, then the Android SDK's own d8, aapt2, zipalign and apksigner.
# Needs the core first (../build.sh makes ../dist/cf-evolution-core.jar) and an
# Android SDK with platform 37 and build-tools 37.0.0: set ANDROID_HOME, or keep
# the SDK in an android-sdk folder beside the cf-evolution folder.
#
# Signed with a debug key kept OUTSIDE the repository: CFE_KEYSTORE, or
# android-signing/debug.keystore beside the cf-evolution folder (made there on
# first build if missing). Keep it: an update installs over the app only when
# it is signed with the same key.
#
# Also compiles the screen models alone into build/screens, against the core
# only, so they can be run and checked on the desktop (harnesses ScreenGate).
set -e
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$(cd ../.. && pwd)/android-sdk}"
PLATFORM="$SDK/platforms/android-37.0/android.jar"
TOOLS="$SDK/build-tools/37.0.0"
CORE=../dist/cf-evolution-core.jar
MIN_SDK=26          # Android 8.0: the core uses java.nio.file
TARGET_SDK=35
# Rises with every build (minutes since 2020), so a phone always takes a new APK as an update
VERSION_CODE=$(( ( $(date -u +%s) - 1577836800 ) / 60 ))
VERSION_NAME="$(sed -n 's/.*versionText.setText("Version \(.*\)");.*/\1/p' ../src/cfevolution/gui/AboutGUI.java)"

for f in "$PLATFORM" "$TOOLS/d8" "$TOOLS/aapt2" "$CORE"; do
    if [ ! -e "$f" ]; then
        echo "Build failed: $f is missing (see the top of this script)"
        exit 1
    fi
done

rm -rf build
mkdir -p build/classes build/screens build/dex ../dist

# The screen models alone, against the core only: proof they need no Android
javac --release 8 -nowarn -cp "$CORE" -d build/screens $(find src/cfevolution/screens -name '*.java') 2>&1 | grep -v "^warning: \[options\]\|^[0-9] warnings\?$" || true

# Everything, against Android's own class library (Java 8 language)
javac -source 8 -target 8 -nowarn -Xlint:-options -bootclasspath "$PLATFORM" -cp "$CORE" -d build/classes \
      $(find src -name '*.java')
(cd build/classes && jar cf ../app-classes.jar .)

# Android bytecode for the app and the core together
"$TOOLS/d8" --release --min-api $MIN_SDK --lib "$PLATFORM" --output build/dex build/app-classes.jar "$CORE"

# Package: the manifest (no resources: the screens are made in code), then the code
"$TOOLS/aapt2" link -o build/unsigned.apk --manifest AndroidManifest.xml -I "$PLATFORM" \
      --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK \
      --version-code $VERSION_CODE --version-name "$VERSION_NAME"
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk

# Signed with the debug key from outside the repository (see the top of this file)
KEYSTORE="${CFE_KEYSTORE:-$(cd ../.. && pwd)/android-signing/debug.keystore}"
if [ ! -f "$KEYSTORE" ]; then
    echo "No signing key at $KEYSTORE: making a new debug key there."
    echo "(An app signed with a different key must be uninstalled before this one will install.)"
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -alias androiddebugkey -keypass android \
            -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" > /dev/null 2>&1
fi
"$TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
      --out ../dist/cf-evolution-android.apk build/aligned.apk
echo "Build complete: dist/cf-evolution-android.apk (version $VERSION_NAME, build $VERSION_CODE, Android 8.0 and later)"
