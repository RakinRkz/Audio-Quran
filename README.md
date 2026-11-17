<p align="center">
  <img src="docs/images/icon.png" alt="Audio Quran app icon" width="160">
</p>

<h1 align="center">Audio Quran</h1>

<p align="center">
  An Android app for listening to Quran recitations stored on your phone.
</p>

<p align="center">
  <a href="https://github.com/RakinRkz/Audio-Quran/actions/workflows/android-ci.yml"><img src="https://github.com/RakinRkz/Audio-Quran/actions/workflows/android-ci.yml/badge.svg?branch=main" alt="Android CI"></a>
</p>

---

Point the app at a folder of surah audio files. It lists the surahs in order with their English and Arabic names and plays them like a music player, including in the background and from the lock screen.

## Features

- **Use your own files.** Pick any folder with the system file picker. The app remembers it and rescans it each time it starts.
- **Surah list.** Each file appears as a surah with its number, English name and Arabic name.
- **Search that tolerates typos.** Fuzzy search over the English and Arabic names shows the five closest matches.
- **Background playback.** Audio keeps playing when the app is closed, with play/pause, skip and seek in the notification and on the lock screen.
- **Playlist and repeat.** Go to the next or previous surah, and repeat one surah, the whole list, or nothing.
- **Full-screen player.** Expand the mini player into a full player view.

Supported formats are `m4a`, `mp3`, `ogg`, `wav`, `aac`, `flac` and `webm`.

## Preparing your audio files

The app reads surah details from the filename. Name each file like this:

```
001 - Al-Fatiha with bangla translation - recited by Reciter Name.mp3
002 - Al-Baqarah with bangla translation - recited by Reciter Name.mp3
```

- The leading number (1–114) is the surah number, which sets the Arabic name. The list is sorted by filename, so pad the number with zeros (`001`, `002`, … `114`) to keep surahs in order.
- The text after the number is shown as the surah name. The `with bangla translation` and `recited by …` parts are optional and are removed from the displayed name.
- Put all files directly in one folder. Subfolders are not scanned.

## Getting started

There is no published release yet, so build the app from source.

**Requirements:** JDK 17 and the Android SDK (API 34). Android Studio installs both. The app runs on Android 8.0 (API 26) and later.

```bash
git clone https://github.com/RakinRkz/Audio-Quran.git
cd Audio-Quran
./gradlew installDebug      # build and install on a connected device or emulator
```

On Windows, use `gradlew.bat` instead of `./gradlew`. To build without installing, run `./gradlew assembleDebug`. The APK is written to `app/build/outputs/apk/debug/`.

On first launch the app asks for permission to read audio and, on Android 13 and later, to show notifications. Then pick the folder that holds your recitations.

## Development

| Command | What it does |
|---|---|
| `./gradlew assembleDebug` | Build a debug APK |
| `./gradlew lint` | Run Android lint |
| `./gradlew test` | Run JVM unit tests |
| `./gradlew assembleRelease` | Build a minified release APK |

Every pull request to `main` runs the [Android CI](.github/workflows/android-ci.yml) workflow (build, lint and unit tests). Lint errors fail the build.

**Built with:** Kotlin, Jetpack Compose (Material 3), Media3 / ExoPlayer with a `MediaSessionService` for background playback, and DataStore for saved settings.

To learn the codebase:

- [docs/overview.md](docs/overview.md) covers the feature list and project structure.
- [CLAUDE.md](CLAUDE.md) has detailed architecture notes: how playback state moves between the service and the UI, how filenames are parsed, and how search ranks results.
