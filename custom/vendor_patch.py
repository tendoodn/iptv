#!/usr/bin/env python3
"""Apply small, deterministic IPTV changes to the user's CleanYourTV launcher copy.

The original CleanYourTV code/resources remain in launcher/ (MIT license retained).
Only the HOME entry point changes; the native theme/layout is reused by TendooHomeActivity.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
build = root / "launcher/app/build.gradle"
manifest = root / "launcher/app/src/main/AndroidManifest.xml"

s = build.read_text()
s = s.replace("compileSdk 34", "compileSdk 35")
base_gradle = root / "launcher/build.gradle"
base = base_gradle.read_text()
assert "8.5.2" in base, "Unexpected upstream Android Gradle Plugin version"
base_gradle.write_text(base.replace("8.5.2", "8.7.3"))
s, n = re.subn(r"applicationId providers\.gradleProperty\('launcherApplicationId'\)\.getOrElse\('com\.gathin\.tvhome'\)", "applicationId 'vn.tendoo.iptv'", s)
assert n == 1, "Cannot find CleanYourTV applicationId"
s, n = re.subn(r"versionCode\s+6\b", "versionCode 5", s)
assert n == 1, "Cannot find original versionCode"
s, n = re.subn(r"versionName\s+'2\.5'", "versionName '1.0.0.test5'", s)
assert n == 1, "Cannot find versionName"
s = s.replace(
    "    testImplementation 'junit:junit:4.13.2'",
    """    implementation 'androidx.media3:media3-exoplayer:1.5.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.5.1'
    implementation 'androidx.media3:media3-ui:1.5.1'
    testImplementation 'junit:junit:4.13.2'"""
)
assert "androidx.media3:media3-exoplayer-hls" in s
s += """
// Bundle the owner's editable playlist without any network access at build time.
android.sourceSets.main.assets.srcDirs += [file("$rootDir/../playlist")]
"""
build.write_text(s)
m = manifest.read_text()
m = m.replace('android:name=".MainActivity"\n            android:exported="true"',
              'android:name=".TendooHomeActivity"\n            android:exported="true"')
assert 'android:name=".TendooHomeActivity"' in m
m = m.replace('    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />',
    '    <uses-permission android:name="android.permission.INTERNET" />\n'
    '    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />')
m = m.replace('        android:label="@string/app_name"', '        android:label="Tendoo IPTV Home"')
m = m.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@drawable/tendoo_icon"')
m = m.replace('android:banner="@drawable/banner"', 'android:banner="@drawable/tendoo_banner"')
# Both the Android phone app drawer and Android TV app row must see the entry point.
# HOME intent alone does not make a user-visible app icon.
home_start = m.index('android:name=".TendooHomeActivity"')
home_end = m.index('</activity>', home_start)
activity = m[home_start:home_end]
assert 'android.intent.category.LEANBACK_LAUNCHER' in activity
activity = activity.replace(
    'android:name=".TendooHomeActivity"',
    'android:name=".TendooHomeActivity"\n'
    '            android:label="Tendoo IPTV Home"\n'
    '            android:icon="@drawable/tendoo_icon"\n'
    '            android:banner="@drawable/tendoo_banner"')
activity = activity.replace(
    '                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />\n'
    '            </intent-filter>',
    '                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />\n'
    '            </intent-filter>\n'
    '            <intent-filter>\n'
    '                <action android:name="android.intent.action.MAIN" />\n'
    '                <category android:name="android.intent.category.LAUNCHER" />\n'
    '            </intent-filter>')
assert 'android.intent.category.LAUNCHER' in activity
m = m[:home_start] + activity + m[home_end:]
m = m.replace('        android:allowBackup="false"', '        android:allowBackup="false"\n        android:usesCleartextTraffic="false"')
m = m.replace('    </application>', '''
        <activity android:name=".TendooChannelsActivity"
            android:exported="false" android:screenOrientation="landscape" />
        <activity android:name=".TendooPlayerActivity"
            android:exported="false" android:screenOrientation="landscape" />
    </application>''')
# Android 11+ package visibility must permit discovery of installed TV apps.
# Without these queries the user might see blank cards / an empty Add App picker.
if "<queries>" not in m:
    m = m.replace("    <application", """    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>
    <application""", 1)
manifest.write_text(m)

dest = root / "launcher/app/src/main/java/com/gathin/tvhome"
dest.mkdir(parents=True, exist_ok=True)
sources = list((root / "custom/java").glob("*.java"))
assert len(sources) >= 4, "Custom launcher Java sources are missing"
for java in sources:
    (dest / java.name).write_bytes(java.read_bytes())
drawable_dst = root / "launcher/app/src/main/res/drawable"
icons = list((root / "custom/drawable").glob("*.xml"))
assert len(icons) == 2, "Expected a launcher icon and a TV banner"
for icon in icons:
    (drawable_dst / icon.name).write_bytes(icon.read_bytes())
# Package the actual broadcaster logos into the offline APK.
asset_dst = root / "launcher/app/src/main/assets"
asset_dst.mkdir(parents=True, exist_ok=True)
logo_bundle = root / "custom/logo_pack.json"
if logo_bundle.exists():
    (asset_dst / "logo_pack.json").write_bytes(logo_bundle.read_bytes())
else:
    (asset_dst / "logo_pack.json").write_text("{}")
print("Patched CleanYourTV resources with", len(sources),
      "IPTV Java sources and", len(icons), "launcher icon resources")
