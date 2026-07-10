# Releasing Countri

## Signing

Every build (debug and release) is signed with the same key so updates
install over each other — no reinstalls, ever. The key lives in
`keystore/countri-release.jks` with credentials in `keystore.properties`
(both git-ignored; both live inside this Google-Drive-synced folder, so
they're backed up). **Do not lose the keystore** — Android will refuse
updates signed with a different key.

## Cutting a release locally

```
git tag v0.2
.\gradlew.bat assembleRelease
# APK: app\build\outputs\apk\release\app-release.apk
adb install -r app\build\outputs\apk\release\app-release.apk
```

Bump `versionCode` (integer, +1 every release) and `versionName` in
`app/build.gradle.kts` first.

## GitHub Actions

`.github/workflows/release.yml` builds and attaches a signed APK whenever
a `v*` tag is pushed. Before the first run, add two repository secrets:

- `KEYSTORE_BASE64` — `base64 -w0 keystore/countri-release.jks`
  (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\countri-release.jks"))`)
- `KEYSTORE_PASSWORD` — the `storePassword` from `keystore.properties`
