# News Crux

Daily current affairs for UPSC CSE Prelims, on Android. It works fully offline-first; AI is optional.

## Releases
Bump `android:versionName` in `AndroidManifest.xml` (and add `release-notes/vX.Y.Z.md`), then push to `main`. GitHub Actions builds, signs and publishes the APK as release `vX.Y.Z` (a pushed tag `vX.Y.Z` works too).
The app checks the latest release when it opens and every 6 hours, then offers to install it.

Signing needs two repository secrets: `NC_KEYSTORE_B64` (the keystore, base64) and `NC_KEYSTORE_PASS`.
Never commit the keystore.

## Build locally
```
ANDROID_JAR=… BUILD_TOOLS=… KS=newscrux.p12 KS_PASS=… KEY_PASS=… VERSION=1.6.1 ./build.sh
./tests/run.sh
```
No Gradle: aapt2 → javac → d8 → zipalign → apksigner.

## Layout
- `src/app/newsbreak/core` — feeds, scoring, editions, PYQ matching, sentinel, AI prompts.
- `src/app/newsbreak` — Android glue: sync jobs, notifications, updater.
- `assets/www` — the reading UI (HTML/JS) and data: `kb.json`, `pyq.js` (CSE), `pyq_more.js` (CDS/CAPF), `sources.json`.
- `release-notes/vX.Y.Z.md` — used as the release text.
