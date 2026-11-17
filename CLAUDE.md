# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`, package `com.audioquran`) that plays Quran audio files from a user-picked folder. Kotlin + Jetpack Compose (Material3), Media3/ExoPlayer, DataStore. minSdk 26, target/compileSdk 34, AGP 8.2.0, Kotlin 1.9.20, Compose compiler 1.5.5, JVM target 1.8. `docs/overview.md` has the feature list.

## Commands

Use `./gradlew` (Git Bash) or `gradlew.bat` (PowerShell/cmd).

```bash
./gradlew assembleDebug            # build debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug             # build and install on a connected device/emulator
./gradlew assembleRelease          # R8 minify + resource shrinking (see app/proguard-rules.pro)
./gradlew lint                     # Android lint
./gradlew test                     # JVM unit tests
./gradlew connectedAndroidTest     # instrumented tests (needs a device)
./gradlew test --tests "com.audioquran.SomeTest.someMethod"   # single unit test
```

There are no tests yet: `app/src/test` and `app/src/androidTest` don't exist. JUnit4, Espresso and Compose ui-test are already declared as dependencies.

## Architecture

There is no DI framework and no navigation graph, even though `navigation-compose` is a dependency. Everything is wired by hand:

- `MainActivity` hosts a single `HomeScreen(viewModel)`.
- `MainViewModel` (an `AndroidViewModel`) constructs `AudioFileRepository`, `PreferencesRepository` and `AudioPlayerController` itself, and exposes UI state as `StateFlow`s.
- `HomeScreen` switches between its sub-screens (permission, folder picker, loading, surah list) based on ViewModel state. It shows the full-screen `PlayerScreen` in place of the list through a local `showFullPlayer` flag, not through navigation.

### Playback: two processes of state

- `player/AudioPlaybackService` is a `MediaSessionService` that owns the `ExoPlayer` and `MediaSession`. Media3 provides the notification and lock-screen controls. The service stops itself in `onTaskRemoved` when nothing is playing.
- `player/AudioPlayerController` lives in the ViewModel and talks to the service only through a `MediaController` that it builds asynchronously in `initialize()`. Until that future resolves, `mediaController` is null and every command is a silent no-op.
- The controller keeps its own `playlist` / `currentIndex` mirror and maps `Player` callbacks into a single `PlaybackState` flow (`data/model/PlaybackState.kt`). `currentSurah` is looked up by `currentMediaItemIndex` in that mirrored playlist, so the list passed to `setMediaItems` and the controller's `playlist` must stay in the same order.
- `PlaybackState.currentPosition` is refreshed only on player events (play/pause, transitions, seeks). Nothing polls the position, so progress UI does not tick on its own.
- `RepeatMode` (OFF/ONE/ALL) is the app's own enum, mapped to and from `Player.REPEAT_MODE_*`.
- `Surah.toMediaItem()` hard-codes the album title "Quran with Bangla Translation" and the artwork URI `android.resource://com.audioquran/drawable/ic_quran`.

### Audio files and metadata

- Folder access uses SAF: the user picks a tree URI and the app takes a persistable read permission (both in `HomeScreen`'s picker callback and in `AudioFileRepository.scanFolder`). The URI is saved in DataStore (`PreferencesRepository`, key `audio_folder_uri`). `MainViewModel` collects that flow on startup and rescans automatically.
- `scanFolder` lists only the folder's direct children (no recursion) with supported extensions and sorts them by filename.
- Surah metadata comes from filenames, using the format `"001 - Name with bangla translation - recited by ..."`. `Surah.parseSurahNumber` reads the leading number (falling back to the file's sort index), `parseSurahName` strips the translation/reciter/extension suffixes, and the Arabic name comes from the static `Surah.SURAH_NAMES` map (1–114). Note that `parseSurahName`'s extension regex covers only m4a/mp3/ogg/wav, while the scanner also accepts aac/flac/webm.
- `Surah.duration` is always 0 after scanning; the real duration only reaches the UI through `PlaybackState.duration` from the player.
- `AudioFileRepository.scanDefaultDownloadsFolder` is a legacy fallback for API ≤ 28 and is not called anywhere.

### Search

`data/repository/FuzzySearch` is a global `object` that ranks surahs by cosine similarity of character bigrams over the English and Arabic names and returns the **top 5 only**. It caches n-grams keyed on list equality. The ViewModel calls `cacheSurahNgrams` after a scan and `clearCache` on folder change. Query updates are debounced by 150 ms in `MainViewModel.updateSearchQuery`.

### UI

`ui/theme` defines the green palette (`Green700` etc.) used directly by the screens. The `formatDuration` helper is duplicated as a private function in `MiniPlayer`, `PlayerScreen` and `SurahListItem`.
