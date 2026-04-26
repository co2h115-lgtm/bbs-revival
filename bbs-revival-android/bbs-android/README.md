# BBS Revival — Android App

A native Android client for BBS Revival, built with Kotlin + Jetpack Compose.
All 6 feature phases in one app:
- Phase 1: Auth (encrypted token storage)
- Phase 2: Message boards, threads, posts, private mail, search
- Phase 3: Real-time chat via Socket.IO
- Phase 4: File areas + ANSI gallery
- Phase 5: Door games with live terminal
- Phase 6: Native notifications, dark terminal theme

---

## Prerequisites

- Android Studio Ladybug (2024.2) or newer
- Android SDK 35
- Java 17 (bundled with Android Studio)
- Your BBS Revival server running (see main project)

---

## Quick Start

### 1. Open in Android Studio

```
File → Open → select the bbs-android/ folder
```

Android Studio will sync Gradle automatically on first open (~2–5 min).

### 2. Set your server URL

Edit `gradle.properties`:

```properties
# Android emulator talking to your dev machine:
apiBaseUrl=http://10.0.2.2:3001

# Physical device on same WiFi — use your machine's LAN IP:
apiBaseUrl=http://192.168.1.X:3001

# Production:
apiBaseUrl=https://your-domain.com
```

### 3. Run on emulator

- Click **Run ▶** in Android Studio
- Choose an emulator (Pixel 6 API 35 recommended)
- App launches automatically

### 4. Run on physical device

- Enable **Developer Options** on your Android phone
- Enable **USB Debugging**
- Connect via USB
- Select your device in Android Studio's device picker
- Click **Run ▶**

---

## Building a Release APK

### Debug APK (for testing, no signing needed)

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (for distribution)

1. Generate a signing keystore (one time):
```bash
keytool -genkey -v -keystore bbs-release.jks \
  -alias bbs -keyalg RSA -keysize 2048 -validity 10000
```

2. Add to `gradle.properties` (keep this file out of git!):
```properties
RELEASE_STORE_FILE=../bbs-release.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=bbs
RELEASE_KEY_PASSWORD=your_password
```

3. Add signing config to `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile     = file(properties["RELEASE_STORE_FILE"].toString())
        storePassword = properties["RELEASE_STORE_PASSWORD"].toString()
        keyAlias      = properties["RELEASE_KEY_ALIAS"].toString()
        keyPassword   = properties["RELEASE_KEY_PASSWORD"].toString()
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        ...
    }
}
```

4. Build:
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Project Structure

```
app/src/main/
├── java/com/bbsrevival/
│   ├── BbsApplication.kt           # Hilt app + notification channels
│   ├── MainActivity.kt             # Entry point + bottom nav scaffold
│   ├── data/
│   │   ├── AppModule.kt            # Hilt DI providers
│   │   ├── BootReceiver.kt         # Recreate notif channels on boot
│   │   ├── NotificationHelper.kt   # Native PM / mention / broadcast notifs
│   │   └── api/
│   │       ├── ApiClient.kt        # Ktor HTTP client, all endpoints
│   │       ├── SocketManager.kt    # Socket.IO wrapper, flows
│   │       └── TokenStore.kt       # Encrypted token storage
│   └── ui/
│       ├── navigation/
│       │   └── Navigation.kt       # NavHost + all routes
│       ├── screens/
│       │   └── Screens.kt          # All screens + ViewModels
│       └── theme/
│           └── Theme.kt            # BBS dark palette, typography
├── res/
│   ├── drawable/                   # Launcher + splash icons
│   ├── mipmap-*/                   # Adaptive icons all densities
│   ├── values/                     # strings, colors, themes
│   └── xml/
│       └── network_security_config.xml
└── AndroidManifest.xml
```

---

## Screens

| Screen | Route | Features |
|--------|-------|---------|
| Login | `/login` | Email/password, error handling |
| Register | `/register` | Handle + email + password |
| Boards | `/boards` | All groups + boards, tap to open |
| Thread List | `/thread_list/{id}` | Paginated, new thread form, pull-to-refresh |
| Thread | `/thread/{id}` | Posts, reply, delete, pagination |
| Chat | `/chat` | Room list → live chat, typing indicator |
| Files | `/files` | Area list → file browser, download |
| Doors | `/doors` | Game lobby + live terminal |
| Door Game | `/door_game/...` | Leaderboard strip + ANSI terminal |
| Gallery | `/gallery` | Browse + like + full detail view |
| Messages | `/messages` | Inbox, sent, compose, read, delete |
| Search | `/search` | Full-text search threads + posts |
| Profile | `/profile` | View/edit, stats, logout |

---

## Architecture

- **UI**: Jetpack Compose + Material3
- **Navigation**: Navigation Compose
- **DI**: Hilt
- **HTTP**: Ktor client with kotlinx.serialization
- **WebSocket**: Socket.IO Android client
- **Token storage**: EncryptedSharedPreferences (AES-256)
- **State**: ViewModel + MutableState (no separate state reducer needed)

---

## Notifications

The app sends native Android notifications for:
- New private messages
- Chat @mentions (when your handle appears in a message)
- Sysop system broadcasts

Requires `POST_NOTIFICATIONS` permission (Android 13+, requested on first launch).

---

## Connecting to Production

Change `apiBaseUrl` in `gradle.properties` to your production HTTPS URL:

```properties
apiBaseUrl=https://bbsrevival.com
```

The network security config already enforces HTTPS-only for non-local domains.
