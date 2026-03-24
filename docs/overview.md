# Audio Quran - Overview

A native Android app for playing Quran audio files with a modern Compose UI.

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + Clean Architecture |
| Audio Playback | Media3 (ExoPlayer) |
| Background Service | MediaSessionService |
| Preferences | DataStore |
| Min SDK | API 26 (Android 8.0) |

## Project Structure

```
app/src/main/java/com/audioquran/
├── data/
│   ├── model/          # Surah, PlaybackState, RepeatMode
│   └── repository/    # AudioFileRepository, PreferencesRepository, FuzzySearch
├── player/
│   ├── AudioPlayerController.kt   # Playback logic
│   └── AudioPlaybackService.kt    # Background service
└── ui/
    ├── MainViewModel.kt
    ├── screens/HomeScreen.kt
    ├── components/    # PlayerScreen, MiniPlayer, SurahListItem
    └── theme/         # Colors, Typography, Theme
```

## Key Features

1. **Folder Selection** - Uses SAF (Storage Access Framework) to let users pick any folder containing Quran audio files
2. **Auto-Discovery** - Scans selected folder for audio files (m4a, mp3, ogg, wav, aac, flac, webm)
3. **Fuzzy Search** - Find Surahs by name using cosine similarity with n-gram matching (handles typos)
4. **Background Playback** - Continues playing when app is in background via MediaSessionService
5. **Media Controls** - Notification controls with play/pause, skip, and seek
6. **Playlist** - Full playlist with next/previous navigation
7. **Repeat Modes** - Off / Repeat All / Repeat One
8. **Persistent Storage** - Remembers selected folder across app restarts

## Architecture

- **ViewModel** acts as single source of truth, managing UI state
- **Repository** pattern for data access (audio files, preferences, search)
- **Controller** wraps Media3 for playback control
- **StateFlow** for reactive state management between components
