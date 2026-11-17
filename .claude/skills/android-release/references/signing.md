# Release signing for Audio Quran

Read this when `app/build.gradle.kts` has no release `signingConfig`, or when `keystore.properties` is missing.

## How it fits together

- The keystore (`.jks`) holds the private key. Keep it **outside the repo**, for example `C:/Users/<you>/keystores/audioquran-upload.jks`, and back it up somewhere other than this machine.
- `keystore.properties` in the repo root says where the keystore is and holds its passwords. It is ignored by git.
- `app/build.gradle.kts` reads `keystore.properties` if it exists. Without it, the release build still succeeds but is unsigned. That way a fresh clone or CI machine without the key can still build.

On the Play Store with Play App Signing (the default for new apps), this key is the **upload key**. Google re-signs the app with its own app-signing key. A lost upload key can be reset through Play Console support. A lost key for an app distributed outside Play (GitHub, sideloading) can't be reset: users would have to uninstall to get updates.

## Wiring it into `app/build.gradle.kts`

This exact setup has been built and verified with AGP 8.2 in this project. The import goes at the very top of the file, above `plugins { }`. Writing `java.util.Properties()` inline instead fails with `Unresolved reference: util`, because inside a Gradle Kotlin script `java` refers to the project's `java` extension, not the package.

```kotlin
import java.util.Properties
```

After the `plugins { }` block, before `android { }`:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}
```

Inside `android { }`, before `buildTypes`:

```kotlin
signingConfigs {
    if (keystorePropertiesFile.exists()) {
        create("release") {
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
}
```

In `buildTypes { release { ... } }`, keep the existing minify and ProGuard lines and add:

```kotlin
if (keystorePropertiesFile.exists()) {
    signingConfig = signingConfigs.getByName("release")
}
```

Add to `.gitignore` (next to the existing keystore entries):

```
keystore.properties
```

## `keystore.properties`

```properties
storeFile=C:/Users/<you>/keystores/audioquran-upload.jks
storePassword=CHANGE_ME
keyAlias=upload
keyPassword=CHANGE_ME
```

Use **forward slashes** in `storeFile`. In a `.properties` file a backslash is an escape character. A path like `C:\Users\me\tools\key.jks` is read as `C:Usersme<TAB>oolskey.jks` (`\t` becomes a tab), and the build fails in `:app:validateSigningRelease` with "Keystore file ... not found". Doubled backslashes work, but writing them through Git Bash (`printf`, heredocs) can collapse them back to single ones, so write this file with the Write tool and use forward slashes. A relative path is resolved against the repo root.

When you write this file for the user, use the placeholders above. Don't fill in real passwords, even if the user offers them in chat: chat logs aren't a place for them. Tell the user to open the file and fill in the values themselves.

## No keystore yet

Creating the real release key is the user's job, because they need to choose and remember the passwords and keep the backup. Give them this to run in **their own terminal** (it prompts for passwords and a name interactively, which doesn't work through Claude's shell):

```powershell
mkdir $HOME\keystores -Force
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore "$HOME\keystores\audioquran-upload.jks" -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Then they fill in `keystore.properties` and tell you to continue. Remind them to back up the `.jks` file and its passwords, for example in a password manager.

A throwaway keystore for testing (not for releases) can be made non-interactively, outside the repo:

```bash
"$JAVA_HOME/bin/keytool" -genkeypair -keystore /path/outside/repo/test.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass testpass -keypass testpass -dname "CN=Test"
```

## Checking the result

After `./gradlew clean bundleRelease assembleRelease`, the APK is named `app-release.apk` instead of `app-release-unsigned.apk`. `scripts/verify_release.py` confirms both signatures and prints the certificate fingerprint.
