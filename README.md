# Personal Video Library (XHamster Library)

A high-performance, personal-use Android application built with Jetpack Compose and Material 3 Dark Glassmorphism to import, scrape, locally cache, and play videos from personal `.txt` configuration lists (Playlists, Single Videos, and Profiles).

## Features

- **Strict Real Data Architecture**: Zero mock, demo, or placeholder data. Clean honest empty and error states.
- **TXT Configuration Import**: Accepts `.txt` files containing categorized links (`#playlist`, `#Single video`, `#profile link`).
- **Persistent Local Database**: Built on Room Database with SQLite for offline-first metadata and source relationship persistence.
- **Deduplication Engine**: Uses remote video IDs and canonical URL normalization to avoid redundant downloads.
- **Coil Thumbnail Caching**: Smooth local image caching for fast load times and offline browsing.
- **AndroidX Media3 Player**: In-app streaming video playback with custom dark glass controls, orientation support, and playback state handling.
- **WorkManager Background Sync**: Scheduled periodic and on-demand synchronization for playlist and profile sources.
- **Dark Glassmorphism UI**: Polished modern styling with subtle translucency, rounded cards, refined typography, and responsive edge-to-edge layout.

## TXT File Format

```text
#playlist
https://xhamster.com/playlists/...

#Single video
https://xhamster.com/videos/...

#profile link
https://xhamster.com/users/...
```

## Tech Stack

- **UI**: Jetpack Compose, Material 3, Dark Glassmorphism
- **Persistence**: Room Database with Flow & Coroutines
- **Networking & Scraper**: OkHttp 4, Jsoup HTML parser
- **Playback**: AndroidX Media3 ExoPlayer
- **Image Loading**: Coil 2.7
- **Background Jobs**: WorkManager KTX
- **Architecture**: MVVM + Clean Architecture / Repository pattern

## CI/CD Workflows

- `ci.yml`: Runs on PRs and pushes to `main` and `develop` (Unit tests + Debug build).
- `debug-apk.yml`: Builds debug APK and uploads artifact.
- `release.yml`: Runs on Git tags (`v*`), builds and signs release APK, calculates SHA-256 checksums, and publishes a GitHub Release.

## License

MIT License. See [LICENSE](LICENSE) for details.
