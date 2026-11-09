#!/usr/bin/env bash
# Builds and signs the News Crux APK without Gradle.
#
#   VERSION=1.6.0 ./build.sh            -> build/NewsCrux-1.6.0.apk
#
# Needs: ANDROID_JAR (android.jar of API 33/34), aapt2, d8, zipalign and apksigner
# (taken from BUILD_TOOLS when set, e.g. $ANDROID_HOME/build-tools/34.0.0),
# and the signing key: KS (keystore file), KS_PASS, KEY_ALIAS, KEY_PASS.
set -euo pipefail
cd "$(dirname "$0")"

VERSION="${VERSION:-$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' AndroidManifest.xml | head -1)}"
IFS=. read -r MA MI PA _ <<<"${VERSION}.0.0"
VCODE="${VCODE:-$(( MA * 10000 + MI * 100 + PA ))}"

: "${ANDROID_JAR:?set ANDROID_JAR to android.jar}"
BT="${BUILD_TOOLS:-}"
AAPT2="${AAPT2:-${BT:+$BT/}aapt2}"
D8="${D8:-${BT:+$BT/}d8}"
ZIPALIGN="${ZIPALIGN:-${BT:+$BT/}zipalign}"
APKSIGNER="${APKSIGNER:-${BT:+$BT/}apksigner}"

OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== News Crux $VERSION (code $VCODE)"
$AAPT2 compile --dir res -o "$OUT/res.zip"
$AAPT2 link -o "$OUT/base.apk" -I "$ANDROID_JAR" --manifest AndroidManifest.xml \
  --java "$OUT/gen" --custom-package app.newsbreak -A assets "$OUT/res.zip" \
  --min-sdk-version 24 --target-sdk-version 34 \
  --version-code "$VCODE" --version-name "$VERSION" --replace-version

javac -nowarn --release 8 -encoding UTF-8 -cp "$ANDROID_JAR" -d "$OUT/classes" \
  $(find src "$OUT/gen" -name '*.java')
$D8 --release --min-api 24 --lib "$ANDROID_JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -j ../unsigned.apk classes.dex)
$ZIPALIGN "$OUT/unsigned.apk" "$OUT/aligned.apk" 2>/dev/null || $ZIPALIGN -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

: "${KS:?set KS to the keystore file}"
$APKSIGNER sign --ks "$KS" --ks-pass env:KS_PASS --ks-key-alias "${KEY_ALIAS:-newscrux}" --key-pass env:KEY_PASS \
  --min-sdk-version 24 --out "$OUT/NewsCrux-$VERSION.apk" "$OUT/aligned.apk"
$APKSIGNER verify --min-sdk-version 24 "$OUT/NewsCrux-$VERSION.apk"
echo "== $OUT/NewsCrux-$VERSION.apk"
