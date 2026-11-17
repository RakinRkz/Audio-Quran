---
name: android-release
description: Build, version and ship the Audio Quran Android app. Bumps versionCode/versionName, sets up or checks release signing (keystore.properties), runs the Gradle release build (signed AAB and APK), checks the R8/ProGuard output and signatures, archives the artifacts plus mapping.txt, commits and tags the release, and publishes it as a GitHub Release with the APK attached or prepares the Play Store upload. Use this whenever the user wants to cut, prepare, build, publish or ship a release, make a signed or release APK/AAB, bump or set the app version, put the app on GitHub Releases or the Play Store, sign the app or set up a keystore, or asks why the release build is broken, unsigned, or crashing after minification, even if they only say "make a build I can install" or "new version".
---

# Android release

This skill turns the current commit of this repo into a versioned, signed release: a Play Store bundle (`.aab`) and an installable `.apk`, with the R8 mapping file kept beside them and a git tag pointing at the exact source. From there, it can be published as a GitHub Release, uploaded to the Play Store, or both.

Facts about this project that the steps below rely on:

- Single module `:app`, `applicationId` `com.audioquran`. Version lives as literals in `app/build.gradle.kts` (`versionCode = N`, `versionName = "X.Y"`).
- The `release` build type has `isMinifyEnabled = true` and `isShrinkResources = true`, with keep rules in `app/proguard-rules.pro`.
- `.gitignore` already ignores `*.jks`, `*.keystore`, `*.apk`, `*.aab` and `local.properties`. `local.properties` has `sdk.dir`, which is where `build-tools/` (apksigner) and `platform-tools/` (adb) live.
- `JAVA_HOME` points at a JDK 17. `java` may not be on PATH in Git Bash, so call JDK tools as `"$JAVA_HOME/bin/keytool"` and `"$JAVA_HOME/bin/jarsigner"`.
- Use `./gradlew` in Git Bash and `.\gradlew.bat` in PowerShell. A cold release build with R8 takes about 3 minutes, so give the command a 10-minute timeout or run it in the background.
- CI (`.github/workflows/android-ci.yml`) only builds debug, lints and runs unit tests. Release builds happen locally, because the signing key exists only on the user's machine; don't put the keystore or its passwords into CI.
- `minSdk` is 26, so the APK is signed with the v2 scheme only (no v1/v3). That is correct, not a problem to fix.
- GitHub: `origin` is `RakinRkz/Audio-Quran`, and `gh` is installed.

## The flow

Work through these in order. A release that fails halfway (tag pointing at the wrong commit, artifacts lost by the next `clean`) is more annoying to clean up than one that never started, so each step says what to check before moving on.

The run may stop partway: at step 3 when there's no key yet, or at step 8 waiting for a yes. When it does, end with what's done, what's left (including the publishing options from step 8), and the one thing you need from the user. When the user comes back, pick up where it stopped instead of starting over.

### 1. Preflight

- `git status` and the current branch. Release from committed code. Uncommitted changes from an earlier stopped run of this skill (version bump, signing config, `.gitignore`) belong to the release and are fine. If there are other uncommitted changes, stop and ask, because a tag should describe exactly what's in the build. Releases normally go out from `main`. If you're on another branch, say so in the summary rather than switching branches yourself.
- Read the current `versionCode` / `versionName`, the latest tag (`git describe --tags --abbrev=0`, may be none), and `git diff HEAD -- app/build.gradle.kts`. If the working tree already bumped the version past HEAD, that's a resumed release: keep that version and skip the bump in step 2.
- **Gradle wrapper:** `gradle/wrapper/gradle-wrapper.jar` is committed. If `./gradlew` ever fails with `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`, the jar has gone missing. Regenerate it from the cached Gradle 8.2, offline, because without `--offline` it can hang on the network for minutes. Commit that fix separately from the release:
  ```bash
  G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.2-bin/*/gradle-8.2 | head -1)
  "$G/bin/gradle" wrapper --gradle-version 8.2 --distribution-type bin --offline
  ```

### 2. Set the version

Do this before signing, so the version the user asked for is in place even if the run stops at step 3 for a key.

```bash
python .claude/skills/android-release/scripts/bump_version.py patch      # or minor / major
python .claude/skills/android-release/scripts/bump_version.py --set 2.0.0
```

The script adds 1 to `versionCode` and bumps `versionName` (reading `1.0` as `1.0.0`). It refuses to go backwards, keeps the file's line endings, and prints the old and new values plus the tag name as JSON. Use `--dry-run` to preview. The default is a patch bump; use what the user says ("minor", "1.0.1") when they say it, and don't ask if they didn't. Play rejects an upload whose `versionCode` isn't higher than every earlier one, which is why the code always goes up.

If the user only asked to bump the version, stop here. Show the diff and old → new in a few lines, and say in one sentence that they can ask for the release build when they're ready. Don't build, don't commit, and don't list the remaining release steps: they asked for a small edit, and a checklist of everything else reads as pressure to keep going.

### 3. Signing

Check whether `app/build.gradle.kts` has a `signingConfigs { create("release") ... }` that the release build type uses, and whether `keystore.properties` exists in the repo root.

- **Not wired yet:** add the config from `references/signing.md`. It reads `keystore.properties` and leaves the release unsigned when the file is missing, so the build never breaks on a machine without the key. Add `keystore.properties` to `.gitignore` in the same change.
- **No `keystore.properties`:** follow the "No keystore yet" part of `references/signing.md`. The release key is the app's identity for good: if it's lost, installed copies can't be updated, so the user creates it themselves and keeps a backup. Don't run `keytool -genkeypair` for the real key, and don't offer to. Never put real passwords in a command line, a file you write, or your messages. Write `keystore.properties` with placeholder values, give the user the `keytool` command to run in their own terminal, and stop.
- **Everything present:** don't print `keystore.properties`; just use it.

If the user only wants a quick test build, say an unsigned APK can't be installed and offer a debug build (`assembleDebug`) instead.

### 4. Build

```bash
./gradlew clean bundleRelease assembleRelease assembleDebug --console=plain
```

`clean` matters: a stale `app-release-unsigned.apk` from an earlier build sitting next to the new one causes confusion. `assembleDebug` is only there so step 5 can compare sizes; drop it if time matters. Outputs:

| What | Path |
|---|---|
| Bundle (Play Store) | `app/build/outputs/bundle/release/app-release.aab` |
| APK (install directly) | `app/build/outputs/apk/release/app-release.apk` (named `app-release-unsigned.apk` if signing isn't set up) |
| R8 mapping | `app/build/outputs/mapping/release/mapping.txt` |

If the build fails, read the actual error before changing anything. R8 "Missing class" errors usually mean a library needs a `-dontwarn` or keep rule; add the narrowest rule that fixes it, and say what you added and why. Don't turn off `isMinifyEnabled` to make an error go away. That silently ships a bigger, unobfuscated app.

### 5. Verify

Run the bundled checker. It finds the SDK through `local.properties`, verifies both signatures, compares sizes with the debug APK, and confirms the mapping file is there:

```bash
python .claude/skills/android-release/scripts/verify_release.py
```

Then review the results with judgment instead of just passing them on:

- **Signing:** the APK must pass `apksigner verify` with the v2 scheme, and the AAB must be "jar verified". `jarsigner` exits 0 even for an unsigned bundle, so trust the script's text check, not the exit code. Its warnings about a self-signed certificate, an invalid certificate chain or a missing timestamp are normal for an upload key; don't report them as problems. Report the certificate's **full** SHA-256 fingerprint: it's what the user compares against Play Console's upload key. If it's the debug certificate (`CN=Android Debug`), the signing config points at the wrong keystore; stop and say so.
- **Size:** a release APK much smaller than debug is the expected effect of R8; here it's about 3 MB against about 17 MB. If they're about the same size, minify probably isn't running.
- **Keep rules:** look at `app/proguard-rules.pro`. Broad rules like `-keep class androidx.media3.** { *; }` keep an entire library, which cancels most of R8's work on it. Media3 ships its own consumer rules, so this one is probably unnecessary. Point such rules out and suggest narrowing them, but don't change them in the same release. That's a behaviour change that needs its own testing on a device, especially playback and the background service.
- **Device smoke test:** run `adb devices` (in `<sdk.dir>/platform-tools`). If a device is connected, ask before installing. A release-signed APK can't be installed over a debug build of the same app, so installing means uninstalling first, which wipes the saved audio-folder choice. If the user agrees: `adb install -r` the APK, launch with `adb shell am start -n com.audioquran/.MainActivity`, wait a few seconds, then check `adb logcat -d -s AndroidRuntime:E` for a `FATAL EXCEPTION`. A crash here that doesn't happen in debug is almost always R8 removing or renaming something; the stack trace names the class, and `mapping.txt` deobfuscates it. With no device connected, say the smoke test was skipped.

### 6. Archive

Copy the artifacts out of `build/` (which `clean` wipes) into `dist/vX.Y.Z/`:

- `audioquran-vX.Y.Z.aab`, `audioquran-vX.Y.Z.apk`, `mapping-vX.Y.Z.txt`

Add `dist/` to `.gitignore` if it isn't there. Keep the mapping file because it's the only way to read crash stack traces from the APK, the copy people install from GitHub or by sideloading. The `.aab` carries its own copy (`BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`), so Play deobfuscates crashes automatically and nothing needs uploading separately.

### 7. Commit and tag

```bash
git add app/build.gradle.kts            # plus .gitignore / signing config if this release changed them
git commit -m "Release vX.Y.Z (versionCode N)"
git tag -a vX.Y.Z -m "Release vX.Y.Z (versionCode N)"
```

Run these as separate commands, and never chain a push onto them. Never commit `keystore.properties`, the keystore, or anything in `dist/`.

### 8. Publish (ask first)

Everything up to here is local and easy to undo. Publishing isn't, so offer the options and wait for a yes before pushing, creating a release, or uploading anything:

- **GitHub Release**: push the commit and tag, then create a release on `RakinRkz/Audio-Quran` with the signed APK attached, for people who install it directly. It's created as a draft by default, so the user can check it before publishing. Follow `references/github-release.md`. Don't attach the AAB or `mapping.txt`.
- **Play Store**: the user uploads `dist/vX.Y.Z/audioquran-vX.Y.Z.aab` in Play Console themselves; there's no upload tool here. The bundle already includes the R8 mapping.
- **Just push**: push the commit and tag, nothing else.
- **Nothing yet**: leave it local, for example to test on a phone first.

Always list both GitHub and Play, even when the user mentioned only one ("for the Play Store", "an apk for my phone"). The user wants to see GitHub Releases as an option every time, and choosing both is normal. List them as well when the run stopped early (no key yet), under what comes after the build.

If the user's request already gave the go-ahead for a destination ("put it on GitHub, go ahead and push"), that's the yes for that destination. Do it, and mention the other options only in the summary.

**First Play upload:** if the user says this is the app's first time on the Play Store, read `references/play-first-upload.md` and include its points in the summary as a short checklist, one line each. If it only *might* be the first (the release includes an `.aab` but there are no earlier tags), don't paste the checklist. Add one line instead: "First Play upload? There are a few things to check first (target SDK, package name, Play Console forms); ask and I'll list them."

## Final summary

End with a short summary in this shape, so the user can see at a glance what happened and what's left:

```
Release v1.0.1 (versionCode 2)    (was 1.0, versionCode 1)

Artifacts (dist/v1.0.1/)
  audioquran-v1.0.1.aab   5.2 MB  signed, jar verified
  audioquran-v1.0.1.apk   3.1 MB  signed (v2), 18% of debug size
  mapping-v1.0.1.txt
Signing cert SHA-256: AB:CD:...:EF  (full value)
Smoke test: launched on Pixel 7 emulator, no crash  |  skipped, no device
Git: committed and tagged v1.0.1 locally on main, not pushed

Notes
  - proguard-rules.pro keeps all of androidx.media3; consider narrowing (separate change)

Publish? Pick any:
  1. GitHub Release: push, then a draft release with the APK attached
  2. Play Store: you upload the .aab in Play Console
  3. Just push the commit and tag
  4. Nothing yet
```

Leave out lines that don't apply, and say plainly if a step was skipped or failed. Keep it about this long. The user wants the outcome and the next decision, not a recap of every check. If the run stopped early (no key yet), use the same shape. Say which steps are done, list what you need from the user, and still show the publish options (GitHub and Play) so the user knows what comes after.
