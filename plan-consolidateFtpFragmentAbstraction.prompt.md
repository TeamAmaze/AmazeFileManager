## Plan: Delete BaseFtpServerFragment, Move App to Single FTP UI Owner

Consolidate `BaseFtpServerFragment` logic into `FtpServerFragment`, move all FTP resources from `ftpserver` module to `app` module, rewrite tests for concrete fragment behavior, and remove abstraction layer. All flavor-specific UI logic will stay inside `FtpServerFragment`; pluggability remains in `server-core` service layer.

### Rationale

- `BaseFtpServerFragment` was created as UI abstraction for reusability, but FTP server is module-specific and not truly reusable.
- Flavor/provider pluggability is now handled in `server-core` via `ServerRegistry`/service contracts, not fragment inheritance.
- Keeping both base and app fragment creates duplicate UI logic, split resource ownership, and maintenance drift risk.
- One concrete `FtpServerFragment` with internal gating hooks (`onBeforeStartServer`) is simpler and clearer.
- Different server implementations can still have their own UI logic baked into `FtpServerFragment` conditional blocks based on `server-core` provider state.

### Implementation Steps

#### 1. Consolidate core logic into `FtpServerFragment`

Copy the following from `BaseFtpServerFragment.kt` into `FtpServerFragment.kt`:
- `onCreate()`, `onCreateView()`, `onDestroyView()` lifecycle wiring
- `onResume()` / `onPause()` receiver registration/cleanup
- `onCreateOptionsMenu()` / `onOptionsItemSelected()` menu handling
- Event bus collection and `onFtpServerEvent()` handler
- All `update*()` methods: `updateSpans()`, `updateStatus()`, `updateViews()`, `updatePathText()`, `updatePasswordText()`, `updateUsernameText()`, `updatePortText()`
- Span initialization and URL/status rendering
- `wifiReceiver` broadcast receiver and registration logic
- Protected `onBeforeStartServer(proceed: () -> Unit)` hook (keep as local method, not abstract)
- Protected `showPortDialog()` / `showTimeoutDialog()` stubs (move to concrete `FtpServerFragment`)
- Preference setter helpers: `setReadonlyPreference()`, `setSecurePreference()`, `setSafFilesystemPreference()`

**Remove from `FtpServerFragment.kt`** (now duplicate):
- Candidate duplicates: any overlap in span/status/preference handling already in base

**Keep abstract method implementations** in `FtpServerFragment` (now concrete):
- `getAccentColor()` → use `mainActivity.accent`
- `isConnectedToLocalNetwork()` → use `isConnectedToLocalNetwork(requireContext())`
- `isConnectedToWifi()` → use `isConnectedToWifi(requireContext())`
- `getLocalAddress()` → use `getLocalInetAddress(requireContext())`
- `startFtpService(startedByTile)` → keep `doStartServer()` logic
- `stopFtpService()` → keep broadcast stop logic
- `promptUserToEnableWireless()` → keep snackbar prompt
- `dismissSnackbar()` → keep snackbar?.dismiss()
- `getEncryptedPassword()` → use preference lookup
- `decryptPassword()` → use `PasswordUtil.decryptPassword()`
- `onPathChangeRequested()` → keep SAF/folder dialog
- `onLoginChangeRequested()` → keep login dialog

#### 2. Move FTP resources from `ftpserver` to app module

**Source files to copy/consolidate:**
- `ftpserver/src/main/res/menu/ftp_server_menu.xml` → `app/src/main/res/menu/ftp_server_menu.xml`
  - App version already exists; ftpserver version uses `ftpmod_*` string refs (different naming)
  - **Decision: Keep app version (uses `ftp_*` keys), merge any missing items from ftpserver version if needed**
  
- `ftpserver/src/main/res/layout/fragment_ftp.xml` → `app/src/main/res/layout/fragment_ftp.xml`
  - App version already exists; verify identical or pick best version
  - **Recommendation: Use one version (likely app already has correct bindings)**

- `ftpserver/src/main/res/values/strings.xml` (FTP-related keys)
  - Copy or alias `ftpmod_*` string definitions to `app/src/main/res/values/strings.xml` if not already present
  - **Keys to preserve: `ftpmod_url_label`, `ftpmod_status_*`, `ftpmod_port_*`, `ftpmod_path`, `ftpmod_login`, etc.**
  - Rename usage in code from `com.amaze.filemanager.ftpserver.R.ftpmod_*` to local `R.ftp*` or keep cross-module refs for backward compat if preferences use them

**After copy:**
- Delete `ftpserver/src/main/res/menu/ftp_server_menu.xml`
- Delete `ftpserver/src/main/res/layout/fragment_ftp.xml` (if not shared with other fragments)
- Keep `ftpserver/src/main/res/values/strings.xml` but remove FTP UI string resources (keep service-level strings if any)

#### 3. Update resource references in `FtpServerFragment`

Search-replace patterns:
- `com.amaze.filemanager.ftpserver.R.string.ftpmod_*` → `R.string.ftpmod_*` or `getString(R.string.ftpmod_*)`
- `com.amaze.filemanager.ftpserver.R.menu.ftp_server_menu` → `R.menu.ftp_server_menu`
- `com.amaze.filemanager.ftpserver.R.layout.fragment_ftp` → keep as `Fragment(R.layout.fragment_ftp)`
- Ensure all string keys used in `FtpServerFragment` are defined in `app/src/main/res/values/strings.xml`

**Option:** If keeping `ftpmod_*` keys in ftpserver strings for backward compat, reference via `com.amaze.filemanager.ftpserver.R.string.*` but note that as tech debt for later unification.

#### 4. Rewrite `FtpServerFragmentBatteryOptimizationTest.kt`

**Replace abstraction test pattern:**

Current structure:
```kotlin
private class DefaultFragment : BaseFtpServerFragment() { 
  // overrides all abstract methods 
}
private class GatingFragment : BaseFtpServerFragment() { 
  override fun onBeforeStartServer(proceed) { /* intercept */ }
}
```

New structure:
```kotlin
// Use a mock/spy of FtpServerFragment or test startServer() flow directly
// Test battery optimization check inside startServer() method
// Validate that checkBatteryOptimizationIfNecessary is called before doStartServer()
// Validate gating behavior (dialog shown vs skipped) based on exemption state
```

**Steps:**
1. Remove `DefaultFragment` and `GatingFragment` subclasses
2. Create a test fixture that can invoke `FtpServerFragment.startServer()` (make it testable or extract `checkBatteryOptimizationIfNecessary` into testable helper)
3. Rewrite tests to validate the concrete battery + SAF + start sequence
4. Keep test assertions for preference persistence and PowerManager mocking

#### 5. Delete abstraction files

**Files to delete:**
- `/home/airwave/git/AmazeFileManager/ftpserver/src/main/java/com/amaze/filemanager/ftpserver/ui/BaseFtpServerFragment.kt`
- `/home/airwave/git/AmazeFileManager/ftpserver/src/main/res/layout/fragment_ftp.xml` (if not shared)
- `/home/airwave/git/AmazeFileManager/ftpserver/src/main/res/menu/ftp_server_menu.xml` (after merge)
- Any stub/adapter files in `ftpserver/src/main/java/.../ui/` directory if they only served as bridge

**Update `ftpserver/build.gradle`:**
- Remove any UI-specific dependencies if no longer needed (AndroidX fragments, databinding, etc.)
- May still need service/preference/engine dependencies
- Verify module can compile without Android UI libraries if appropriate

#### 6. Validate imports and compile

**Checklist:**
- [ ] Search codebase for remaining `BaseFtpServerFragment` references → should find only deletions
- [ ] Search for `com.amaze.filemanager.ftpserver.ui` imports → should only be `FtpServerEngine`, `FtpPreferences`, `FtpEventBus`, not UI classes
- [ ] Verify `FragmentFtpBinding` imports point to `com.amaze.filemanager.databinding.FragmentFtpBinding` (app module)
- [ ] Run `./gradlew :app:compileDebugKotlin` to catch syntax/import errors
- [ ] Run `./gradlew :ftpserver:build` to verify module still builds
- [ ] Run full `./gradlew build` to catch downstream issues
- [ ] Rerun `FtpServerFragmentBatteryOptimizationTest.kt` to validate test rewrites

### Resource & Key Naming Decisions

**Option A: Preserve `ftpmod_*` keys indefinitely**
- Pro: No preference migration, backward compat with any stored values
- Con: Code carries cross-module resource references forever
- Recommendation: Keep for now, document as tech debt

**Option B: Rename to `ftp_*` keys and migrate preferences**
- Pro: Cleaner, single-module ownership
- Con: Requires migration on app upgrade
- Recommendation: Consider in future cleanup if app has migration framework

**Decision for this PR: Go with Option A** — keep `ftpmod_*` keys in `app/src/main/res/values/strings.xml` after copy, reference locally without cross-module indirection.

### Testing Strategy

1. **Unit tests**: Rewrite `FtpServerFragmentBatteryOptimizationTest.kt` as concrete behavior tests
2. **Instrumented tests**: If any exist, ensure they still reference `FtpServerFragment` directly (not base class)
3. **Manual smoke test**: Launch app, navigate to FTP fragment, verify UI renders, menus work, dialogs trigger
4. **Preference migration**: Check that existing stored preferences still load (no key changes)

### Rollout Risk & Mitigation

**Risk**: Duplicate logic in base + app ends up with behavioral divergence if only one path changes
- **Mitigation**: Remove base class entirely; all logic now in one place

**Risk**: Preference keys or string resources break if migration incomplete
- **Mitigation**: Keep exact key names, add aliases if needed; validate in UI tests

**Risk**: Test subclasses become unmaintainable
- **Mitigation**: Rewrite tests to be concrete, easier to mock/stub fragments

### Success Criteria

- [ ] `BaseFtpServerFragment.kt` deleted
- [ ] `FtpServerFragment` compiles without abstraction dependencies
- [ ] All FTP UI strings/menu/layout in app module, none in ftpserver module (except service layer)
- [ ] `FtpServerFragmentBatteryOptimizationTest.kt` passes with new concrete tests
- [ ] Full app build succeeds (`./gradlew build`)
- [ ] Manual FTP UI smoke test: fragment loads, menu items visible, dialogs trigger, start/stop works
- [ ] Git diff shows only one FtpServerFragment (consolidation), no duplicates or orphans

### Commit Strategy (Single PR)

One logical commit covering:
1. Consolidate logic into `FtpServerFragment`
2. Move resources to app module
3. Delete `BaseFtpServerFragment` and ftpserver UI resources
4. Rewrite tests
5. Update imports/references

Or split into 2–3 commits if you want to preserve history per component:
- Commit 1: Copy logic from base to app, make app self-contained (behavior equivalent)
- Commit 2: Delete base class and ftpserver UI resources
- Commit 3: Move resources and rewrite tests

Recommend single commit for this scope to keep PR atomic and easy to review.

