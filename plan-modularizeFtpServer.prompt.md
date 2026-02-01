## Plan: Modularize FTP Server Functionality for Pluggable Server Implementations

The goal is to extract FTP server functionality from the `app` module into a dedicated module, enabling future pluggable implementations (SSH server, WebDAV server, etc.). Currently, FTP-related code is tightly coupled with the app module, including UI (`FtpServerFragment`), services (`FtpService`, `FtpReceiver`, `FtpTileService`), notifications (`FtpNotification`), filesystem factories, and resources.

### Implementation Progress

#### ✅ Completed Steps

1. **Define a server abstraction layer in a new core module** — Created `server-core` module with:
   - `FileServer.kt` - Base interface for server implementations
   - `ServerPreferences.kt` - Interface for server preferences management
   - `ServerNotification.kt` - Interface for notification handling
   - `ServerEvent.kt` - Sealed class for server state events
   - `ServerRegistry.kt` - Registry for discovering and managing server providers

2. **Restructure the existing `ftpserver` module** — Updated module to depend on `server-core` instead of `app`. Created:
   - `service/FtpServerEngine.kt` - Core FTP server lifecycle management
   - `service/FtpServerService.kt` - Abstract Android Service for FTP
   - `service/FtpReceiver.kt` - Abstract BroadcastReceiver for FTP start/stop
   - `service/FtpEventBus.kt` - Event bus using Kotlin Flow
   - `service/FtpPreferences.kt` - FTP-specific preferences
   - `service/FtpCipherSuites.kt` - SSL cipher configuration
   - `service/FtpCommandFactoryFactory.kt` - Custom command factory

3. **Move FTP filesystem factories to the ftpserver module** — Created in `ftpserver/filesystem/`:
   - `AndroidFileSystemFactory.kt`
   - `AndroidFtpFileSystemView.kt`
   - `AndroidFtpFile.kt`
   - `RootFileSystemFactory.kt`
   - `RootFileSystemView.kt`
   - `RootFtpFile.kt`
   - `commands/AVBL.kt`, `FEAT.kt`, `PWD.kt`

4. **Created FtpServerProvider** — Implementation of `ServerProvider` interface in `FtpServerProvider.kt`

5. **Extract FTP UI components to the ftpserver module** — ✅ Completed:
   - `ui/BaseFtpServerFragment.kt` - Abstract base fragment for FTP UI
   - `ui/FtpServerNotification.kt` - Notification handler implementation
   - `res/layout/fragment_ftp.xml` - FTP fragment layout
   - `res/menu/ftp_server_menu.xml` - Menu resources
   - `res/values/strings.xml` - FTP strings (with `ftpmod_` prefix to avoid collisions)
   - `res/values/colors.xml` - Color resources
   - `res/drawable/` - Icon drawables

6. **Update AndroidManifest and app module integration** — ✅ Partially completed:
   - Added abstract FtpServerService and FtpReceiver to ftpserver manifest
   - App module still uses its own FtpService, FtpReceiver implementations (coexisting during transition)

#### 🔄 Remaining Steps

7. **Migration of app module FTP code** — Still need to:
   - Create concrete implementations in app that extend `FtpServerService` and `FtpReceiver`
   - Update `FtpServerFragment` in app to extend `BaseFtpServerFragment`
   - Gradually deprecate duplicate code in app module
   - Update `MainActivity.java` and `Drawer.java` to use `ServerRegistry` for fragment instantiation

8. **Move FTP tests to the ftpserver module** — Need to relocate:
   - `FtpServiceEspressoTest.kt`
   - `FtpReceiverTest.kt`
   - Integration tests

### Module Structure Created

```
server-core/
├── build.gradle
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/amaze/filemanager/server/
│       ├── FileServer.kt
│       ├── ServerEvent.kt
│       ├── ServerNotification.kt
│       ├── ServerPreferences.kt
│       └── ServerRegistry.kt

ftpserver/
├── build.gradle
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/amaze/filemanager/ftpserver/
│   │   ├── FtpServerProvider.kt
│   │   ├── commands/
│   │   │   ├── AVBL.kt
│   │   │   ├── FEAT.kt
│   │   │   └── PWD.kt
│   │   ├── filesystem/
│   │   │   ├── AndroidFileSystemFactory.kt
│   │   │   ├── AndroidFtpFile.kt
│   │   │   ├── AndroidFtpFileSystemView.kt
│   │   │   ├── RootFileSystemFactory.kt
│   │   │   ├── RootFileSystemView.kt
│   │   │   └── RootFtpFile.kt
│   │   ├── service/
│   │   │   ├── FtpCipherSuites.kt
│   │   │   ├── FtpCommandFactoryFactory.kt
│   │   │   ├── FtpEventBus.kt
│   │   │   ├── FtpPreferences.kt
│   │   │   ├── FtpReceiver.kt
│   │   │   ├── FtpServerEngine.kt
│   │   │   └── FtpServerService.kt
│   │   └── ui/
│   │       ├── BaseFtpServerFragment.kt
│   │       └── FtpServerNotification.kt
│   └── res/
│       ├── drawable/
│       │   ├── ic_clear_all.xml
│       │   ├── ic_eye_grey600_24dp.xml
│       │   ├── ic_ftp_dark.xml
│       │   └── ic_ftp_light.xml
│       ├── layout/
│       │   └── fragment_ftp.xml
│       ├── menu/
│       │   └── ftp_server_menu.xml
│       └── values/
│           ├── colors.xml
│           └── strings.xml
```

### Further Considerations

1. **Dependency inversion** — ✅ Done: `ftpserver` now depends on `server-core`, and `app` depends on both.

2. **Feature module vs library module** — Implemented as library module (simpler approach).

3. **Preference storage** — ✅ Done: `ServerPreferences` interface created in server-core, implemented in `FtpServerProvider`.

4. **String resources** — Used `ftpmod_` prefix for all ftpserver module strings to avoid collision with app module's existing strings during transition period.

