# Releasing

For maintainers and forks.

## Making a release

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, then either push a tag (`git tag v1.1.0 && git push --tags`) or draft a release on GitHub with a new `v…` tag. The workflow builds the APK and attaches it to the release.

## Signing

Without a release key, builds are signed with the debug key committed in `app/debug.keystore`. That's fine for trying things out, but anyone can sign an APK with that key. For releases people will rely on, the workflow signs with a private key kept in the repository's secrets:

```sh
keytool -genkeypair -v -keystore release.jks -alias minlauncher -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks   # the value for RELEASE_KEYSTORE_BASE64
```

Add these under **Settings › Secrets and variables › Actions**: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Keep `release.jks` and its passwords somewhere safe: every future update must be signed with the same key. An APK signed with a different key can't install over the old one, so export your settings, uninstall, install, then import.
