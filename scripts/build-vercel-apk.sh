#!/usr/bin/env bash
# Tendoo IPTV Home - native Android APK produced INSIDE a Vercel Sandbox VM.
# Not a Vercel web-function build and not GitHub Actions.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${HOME:?}"
if ! command -v java >/dev/null 2>&1; then
  sudo dnf -y install java-17-amazon-corretto-devel
fi
if ! command -v unzip >/dev/null 2>&1; then
  sudo dnf -y install unzip
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  curl -fLsS --retry 3     'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip'     -o /tmp/tendoo-android-cli.zip
  unzip -q /tmp/tendoo-android-cli.zip -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
SDKM="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -f "$ANDROID_HOME/platforms/android-35/android.jar" ]] ||
   [[ ! -x "$ANDROID_HOME/build-tools/35.0.0/aapt" ]]; then
  yes | "$SDKM" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
  "$SDKM" --sdk_root="$ANDROID_HOME" --install     "platforms;android-35" "build-tools;35.0.0" "platform-tools"
fi

# Vender upstream UI exactly: native XML, original clock, resource theme and
# dock decoration. Custom changes ONLY via tracked patch + custom/java sources.
source_tree="/tmp/tendoo-cleanyourtv-ui"
rm -rf "$source_tree"
git clone --quiet --depth 1 --branch main   'https://github.com/tendoodn/cleanyourtv.git' "$source_tree"
rm -rf launcher
cp -a "$source_tree/launcher" launcher
cp "$source_tree/LICENSE" CLEAN_YOUR_TV_LICENSE.txt
python3 custom/vendor_patch.py

cd launcher
chmod +x gradlew
./gradlew --no-daemon :app:assembleRelease --stacktrace
cd ..
mkdir -p dist
cp launcher/app/build/outputs/apk/release/app-release.apk   dist/Tendoo-IPTV-Home-1.0.0-test5.apk

APK="dist/Tendoo-IPTV-Home-1.0.0-test5.apk"
AAPT="$ANDROID_HOME/build-tools/35.0.0/aapt"
"$AAPT" dump badging "$APK" | tee dist/badging.txt
"$AAPT" dump xmltree "$APK" AndroidManifest.xml >dist/manifest.txt
grep -q "launchable-activity:" dist/badging.txt
grep -q "leanback-launchable-activity:" dist/badging.txt
grep -q "android.intent.category.HOME" dist/manifest.txt
grep -q "android.intent.category.LAUNCHER" dist/manifest.txt
grep -q "android.intent.category.LEANBACK_LAUNCHER" dist/manifest.txt
grep -E -q "^leanback-launchable-activity:.*banner='res/" dist/badging.txt

sha256sum "$APK" | tee dist/SHA256SUMS.txt
ls -lh "$APK"
echo "VERCEL_SANDBOX_TEST5_BUILD_COMPLETE"
