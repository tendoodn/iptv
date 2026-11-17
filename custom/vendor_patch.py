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
s, n = re.subn(r"applicationId providers\.gradleProperty\('launcherApplicationId'\)\.getOrElse\('com\.gathin\.tvhome'\)", "applicationId 'vn.tendoo.iptv'", s)
assert n == 1, "Cannot find CleanYourTV applicationId"
s, n = re.subn(r"versionCode\s+6\b", "versionCode 2", s)
assert n == 1, "Cannot find original versionCode"
s, n = re.subn(r"versionName\s+'2\.5'", "versionName '1.0.0.test2'", s)
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
m = m.replace('        android:allowBackup="false"', '        android:allowBackup="false"\n        android:usesCleartextTraffic="false"')
m = m.replace('    </application>', '''
        <activity android:name=".TendooChannelsActivity"
            android:exported="false" android:screenOrientation="landscape" />
        <activity android:name=".TendooPlayerActivity"
            android:exported="false" android:screenOrientation="landscape" />
    </application>''')
manifest.write_text(m)

dest = root / "launcher/app/src/main/java/com/gathin/tvhome"
dest.mkdir(parents=True, exist_ok=True)
sources = list((root / "custom/java").glob("*.java"))
assert len(sources) >= 4, "Custom launcher Java sources are missing"
for java in sources:
    (dest / java.name).write_bytes(java.read_bytes())
print("Patched CleanYourTV resources with", len(sources), "IPTV Java sources")
