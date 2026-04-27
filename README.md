# Gelbooru Client

A native Android Booru client built with **Kotlin** and **Jetpack Compose**, featuring a unique **Tactile Minimalism** design language.

## Features

- **Floating Command Center**: Bottom-centered navigation FAB with 20% content overlap and radial menu
- **Floating Toolbar**: Top-left search bar with adaptive positioning and boundary checks
- **WebView HTML Scraper**: Headless scraping engine for Gelbooru (no API dependency)
- **Original Image Resolution**: Follows post detail pages to extract high-res originals
- **Content Bypass**: Cookie injection for NSFW and high-res content warnings
- **Image Download**: Foreground service with progress notifications
- **Scoped Storage**: Full Android 10+ MediaStore support
- **Local Caching**: Two-tier disk + memory image caching

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (Material3) |
| Network | OkHttp 4.12 + Jsoup 1.17 |
| Image Loading | Coil 2.5 |
| Storage | DataStore Preferences + MediaStore |
| Architecture | MVVM with Repository pattern |

## Project Structure

```
app/src/main/java/com/gelbooru/client/
├── GelbooruApp.kt              # Application class
├── data/
│   ├── model/                  # Data classes (GelbooruPost, DownloadTask, etc.)
│   └── repository/             # Repository layer (GelbooruRepository, PreferencesRepository)
├── network/                    # OkHttp downloader + image cache
├── scraping/                   # WebView scraper + HTML parser
├── service/                    # Download foreground service
└── ui/
    ├── theme/                  # Tactile theme (colors, typography)
    ├── components/             # Floating UI components
    ├── screens/                # Gallery, Settings screens
    └── MainActivity.kt         # Entry point
```

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

## CI/CD

APKs are automatically built and published to GitHub Releases via GitHub Actions on every push to `main`.
