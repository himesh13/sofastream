# SofaStream Codebase Reference

SofaStream is an Android application (Kotlin) that combines a **Jellyfin** media server client with a **Jellyseerr** media-request front-end, delivering a Netflix-inspired streaming UI.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Module & Directory Structure](#2-module--directory-structure)
3. [Build Configuration](#3-build-configuration)
4. [Key Dependencies](#4-key-dependencies)
5. [Application Entry Points](#5-application-entry-points)
6. [Data Layer](#6-data-layer)
   - 6.1 [Domain Models](#61-domain-models)
   - 6.2 [User Preferences (DataStore)](#62-user-preferences-datastore)
   - 6.3 [JellyfinRepository](#63-jellyfinrepository)
   - 6.4 [JellyseerrRepository](#64-jellyseerrrepository)
7. [API Layer](#7-api-layer)
   - 7.1 [ApiClient (Retrofit singleton)](#71-apiclient-retrofit-singleton)
   - 7.2 [SessionCookieJar](#72-sessioncookiejar)
   - 7.3 [JellyfinApi — endpoints & data classes](#73-jellyfinapi--endpoints--data-classes)
   - 7.4 [JellyseerrApi — endpoints & data classes](#74-jellyseerrapi--endpoints--data-classes)
8. [UI Layer](#8-ui-layer)
   - 8.1 [Onboarding — ServerSetupActivity](#81-onboarding--serversetupactivity)
   - 8.2 [MainActivity & Navigation](#82-mainactivity--navigation)
   - 8.3 [Home — HomeFragment / HomeViewModel](#83-home--homefragment--homeviewmodel)
   - 8.4 [Detail — DetailFragment / DetailViewModel](#84-detail--detailfragment--detailviewmodel)
   - 8.5 [Player — PlayerActivity](#85-player--playeractivity)
   - 8.6 [Search — SearchFragment / SearchViewModel](#86-search--searchfragment--searchviewmodel)
   - 8.7 [Settings — SettingsFragment](#87-settings--settingsfragment)
   - 8.8 [Downloads — DownloadsFragment](#88-downloads--downloadsfragment)
   - 8.9 [Common Adapters](#89-common-adapters)
9. [End-to-End Data Flows](#9-end-to-end-data-flows)
   - 9.1 [First-launch / Onboarding](#91-first-launch--onboarding)
   - 9.2 [Home screen loading](#92-home-screen-loading)
   - 9.3 [Media detail & playback](#93-media-detail--playback)
   - 9.4 [Search](#94-search)
   - 9.5 [Playback session reporting](#95-playback-session-reporting)
10. [Error Handling Patterns](#10-error-handling-patterns)
11. [Coding Conventions](#11-coding-conventions)

---

## 1. Project Overview

| Property | Value |
|---|---|
| Package | `com.sofastream.app` |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 35 |
| Version | 1.0 (versionCode 1) |
| Language | Kotlin (JVM target 17) |
| Architecture | Single-module MVVM (ViewModel + LiveData + Repository) |

---

## 2. Module & Directory Structure

```
sofastream/
├── app/
│   └── src/main/java/com/sofastream/app/
│       ├── SofaStreamApp.kt           # Application class — holds UserPreferences singleton
│       ├── MainActivity.kt            # Single Activity host for Navigation Component
│       ├── api/
│       │   ├── ApiClient.kt           # Retrofit singleton builder (Jellyfin + Jellyseerr)
│       │   ├── JellyfinApi.kt         # Retrofit interface + all Jellyfin data classes
│       │   ├── JellyseerrApi.kt       # Retrofit interface + all Jellyseerr data classes
│       │   └── SessionCookieJar.kt    # In-memory cookie jar for Jellyseerr session auth
│       ├── data/
│       │   ├── model/
│       │   │   └── MediaItem.kt       # Domain models: MediaItem, Season, PlaybackInfo, CastMember, MediaType
│       │   ├── preferences/
│       │   │   └── UserPreferences.kt # DataStore wrapper — stores server URLs, token, userId
│       │   └── repository/
│       │       ├── JellyfinRepository.kt   # All Jellyfin data access (items, playback, reporting)
│       │       └── JellyseerrRepository.kt # All Jellyseerr data access (requests, discovery)
│       └── ui/
│           ├── common/
│           │   ├── MediaRowAdapter.kt  # Horizontal list adapter (poster + progress bar)
│           │   └── MediaGridAdapter.kt # 3-column grid adapter (search results)
│           ├── home/
│           │   ├── HomeFragment.kt
│           │   └── HomeViewModel.kt
│           ├── detail/
│           │   ├── DetailFragment.kt
│           │   └── DetailViewModel.kt
│           ├── player/
│           │   └── PlayerActivity.kt  # Full-screen ExoPlayer activity
│           ├── search/
│           │   ├── SearchFragment.kt
│           │   └── SearchViewModel.kt
│           ├── settings/
│           │   └── SettingsFragment.kt
│           ├── downloads/
│           │   └── DownloadsFragment.kt  # Placeholder (not yet implemented)
│           └── onboarding/
│               └── ServerSetupActivity.kt
├── build.gradle      # App-level Gradle (dependencies, Android config)
├── settings.gradle   # Root project settings (repositories, module include)
└── gradle.properties # JVM args, AndroidX flags, Kotlin code style
```

---

## 3. Build Configuration

**`build.gradle` (app-level) highlights:**

```groovy
android {
    compileSdk 35
    defaultConfig {
        applicationId "com.sofastream.app"
        minSdk 26
        targetSdk 35
        versionCode 1
        versionName "1.0"
    }
    buildFeatures { viewBinding true }   // View Binding enabled, no DataBinding
    compileOptions { sourceCompatibility/targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = '17' }
}
```

**Applied plugins:** `com.android.application`, `kotlin.android`, `kotlin.plugin.parcelize`, `androidx.navigation.safeargs.kotlin`

---

## 4. Key Dependencies

| Category | Library | Version |
|---|---|---|
| Kotlin Coroutines | `kotlinx-coroutines-android` | 1.7.3 |
| Networking | `retrofit2` + `converter-gson` | 2.9.0 |
| Networking | `okhttp3` + `logging-interceptor` | 4.12.0 |
| JSON | `gson` | 2.10.1 |
| Image loading | `glide` | 4.16.0 |
| Video playback | `media3-exoplayer`, `-hls`, `-dash`, `-ui`, `-common`, `-session` | 1.3.0 |
| Persistence | `datastore-preferences` | 1.0.0 |
| Navigation | `navigation-fragment-ktx` + `-ui-ktx` | 2.7.7 |
| Lifecycle | `lifecycle-viewmodel-ktx`, `-livedata-ktx`, `-runtime-ktx` | 2.7.0 |
| UI | `material`, `constraintlayout`, `recyclerview`, `viewpager2`, `swiperefreshlayout` | various |
| Dynamic color | `palette-ktx` | 1.0.0 |

---

## 5. Application Entry Points

### `SofaStreamApp` (Application class)
- Initialises a single `UserPreferences` instance accessible app-wide via `SofaStreamApp.instance.userPreferences`.
- No dependency injection framework is used; repositories are instantiated per ViewModel using the preferences singleton.

### `ServerSetupActivity` (launcher activity)
- Checks `prefs.isSetupCompleteSync()` on start.
- If setup is complete → immediately launches `MainActivity` and finishes.
- Otherwise, shows the onboarding UI.

---

## 6. Data Layer

### 6.1 Domain Models

**File:** `data/model/MediaItem.kt`

All models implement `Parcelable` via the `@Parcelize` plugin, enabling them to be passed in Navigation `SafeArgs` and `Intent` extras.

| Class | Purpose |
|---|---|
| `MediaItem` | Universal media entity (Movie, Series, Episode). Contains id, title, overview, type, year, rating, runtime, image URLs, genres, studios, series/season/episode info, playback state, cast. |
| `MediaType` | Enum: `MOVIE`, `SERIES`, `EPISODE`, `COLLECTION` |
| `CastMember` | name, role, type (Actor / Director / etc.) |
| `Season` | id, name, seasonNumber, episodeCount, overview, posterUrl |
| `PlaybackInfo` | Passed to `PlayerActivity`; contains streamUrl, mediaSourceId, playSessionId, startPositionTicks, title/series/season/episode metadata, backdropUrl |

`MediaItem.getRuntime()` converts `runtimeTicks` (100-nanosecond units) → human-readable `"Xh Ym"` string.

### 6.2 User Preferences (DataStore)

**File:** `data/preferences/UserPreferences.kt`

Backed by Jetpack DataStore (`preferencesDataStore` named `"sofastream_prefs"`).

| Key | Type | Purpose |
|---|---|---|
| `jellyfin_url` | String | Jellyfin server base URL |
| `jellyfin_token` | String | Jellyfin access token (from auth response) |
| `jellyfin_user_id` | String | Jellyfin user UUID |
| `jellyfin_user_name` | String | Display name |
| `jellyseerr_url` | String | Jellyseerr server base URL |
| `is_setup_complete` | Boolean | Whether onboarding has been completed |

Exposes `Flow<T>` properties for reactive access and `*Sync()` blocking helpers (use `runBlocking`/`first`) for ViewModel init where coroutine context is not yet available.

### 6.3 JellyfinRepository

**File:** `data/repository/JellyfinRepository.kt`

Constructor receives `api: JellyfinApi`, `baseUrl`, `token`, `userId`. A new instance is created per ViewModel call (no singleton).

All public functions return `Result<T>` — success or failure with an `Exception`. HTTP error codes are wrapped as `Exception("Error: ${response.code()}")`.

| Function | Returns | Description |
|---|---|---|
| `getLatestMedia()` | `Result<List<MediaItem>>` | Recently added items |
| `getMovies(startIndex, limit)` | `Result<Pair<List<MediaItem>, Int>>` | Paginated movie list + total count |
| `getSeries(startIndex, limit)` | `Result<Pair<List<MediaItem>, Int>>` | Paginated series list + total count |
| `getItemDetails(itemId)` | `Result<MediaItem>` | Full item metadata |
| `getSeasons(seriesId)` | `Result<List<Season>>` | Seasons for a series |
| `getEpisodes(seriesId, seasonId)` | `Result<List<MediaItem>>` | Episodes in a season |
| `searchItems(query)` | `Result<List<MediaItem>>` | Search movies + series |
| `getPlaybackInfo(itemId)` | `Result<PlaybackInfo>` | Resolve stream URL + resume position |
| `reportPlaybackStart(...)` | Unit (fire & forget) | POST to Sessions/Playing |
| `reportPlaybackStop(...)` | Unit (fire & forget) | POST to Sessions/Playing/Stopped |
| `reportPlaybackProgress(...)` | Unit (fire & forget) | POST to Sessions/Playing/Progress |

**`getPlaybackInfo` stream URL resolution logic:**
1. POST `Items/{itemId}/PlaybackInfo` with `PlaybackInfoRequest` body.
2. Take `body.MediaSources.first()`.
3. If `SupportsDirectStream` → `Videos/{itemId}/stream?Static=true&mediaSourceId=...&api_key=...`
4. Else if `SupportsTranscoding && TranscodingUrl != null` → `{baseUrl}{TranscodingUrl}`
5. Else fallback → `Videos/{itemId}/stream?mediaSourceId=...&api_key=...`
6. Fetches item details to populate resume position and display metadata.

**Image URL helpers (private):**
- `getImageUrl(itemId, imageType, tag)` → `{baseUrl}/Items/{itemId}/Images/{imageType}?tag={tag}`
- `getBackdropUrl(itemId, tag)` → `{baseUrl}/Items/{itemId}/Images/Backdrop?tag={tag}`

### 6.4 JellyseerrRepository

**File:** `data/repository/JellyseerrRepository.kt`

Constructor receives `api: JellyseerrApi`. Session auth is handled via cookie jar in `ApiClient`.

| Function | Returns | Description |
|---|---|---|
| `login(email, password)` | `Result<JellyseerrUser>` | Authenticate, session cookie saved automatically |
| `search(query)` | `Result<List<JellyseerrSearchResult>>` | Cross-media search |
| `getTrending()` | `Result<List<JellyseerrSearchResult>>` | Trending items |
| `discoverMovies()` | `Result<List<JellyseerrSearchResult>>` | Movie discovery |
| `discoverTv()` | `Result<List<JellyseerrSearchResult>>` | TV discovery |
| `requestMedia(mediaType, mediaId, seasons?)` | `Result<JellyseerrRequest>` | Create download/availability request |
| `getRequests()` | `Result<List<JellyseerrRequest>>` | Existing requests |
| `getMovieDetails(movieId)` | `Result<JellyseerrMediaDetails>` | TMDB movie details + request status |
| `getTvDetails(tvId)` | `Result<JellyseerrMediaDetails>` | TMDB TV details + request status |

---

## 7. API Layer

### 7.1 ApiClient (Retrofit singleton)

**File:** `api/ApiClient.kt`

`object ApiClient` — lazy-initialises one Retrofit instance per server. Rebuilds if the base URL changes.

```
getJellyfinApi(baseUrl)  → JellyfinApi  (no cookie jar)
getJellyseerrApi(baseUrl) → JellyseerrApi (with SessionCookieJar)
resetClients()            → clears both Retrofit instances + cookie jar (called on logout/settings change)
```

OkHttp config: 30 s connect/read/write timeouts, `BASIC` logging interceptor.  
`ensureTrailingSlash(url)` guarantees the base URL ends with `/` as required by Retrofit.

### 7.2 SessionCookieJar

**File:** `api/SessionCookieJar.kt`

Simple in-memory `CookieJar` used for Jellyseerr. Merges cookies from responses (replaces on name match) and returns matching cookies per request URL. Cleared on `resetClients()`.

### 7.3 JellyfinApi — endpoints & data classes

**File:** `api/JellyfinApi.kt`

Authentication header: `X-Emby-Token` (passed on every authenticated request).

#### Endpoints

| Method | Path | Function |
|---|---|---|
| POST | `Users/AuthenticateByName` | `authenticateByName` — uses `X-Emby-Authorization` header |
| GET | `Users/{userId}/Views` | `getUserViews` |
| GET | `Users/{userId}/Items/Latest` | `getLatestMedia` |
| GET | `Users/{userId}/Items` | `getItems` (movies, series, search) |
| GET | `Users/{userId}/Items/{itemId}` | `getItemDetails` |
| GET | `Shows/{seriesId}/Seasons` | `getSeasons` |
| GET | `Shows/{seriesId}/Episodes` | `getEpisodes` |
| POST | `Sessions/Playing` | `reportPlaybackStart` |
| POST | `Sessions/Playing/Stopped` | `reportPlaybackStop` |
| POST | `Sessions/Playing/Progress` | `reportPlaybackProgress` |
| **POST** | `Items/{itemId}/PlaybackInfo` | `getPlaybackInfo` — Jellyfin requires POST (not GET) with a device-profile body |

#### Data Classes (Jellyfin API)

| Class | Key Fields |
|---|---|
| `JellyfinAuthRequest` | `Username`, `Pw` |
| `JellyfinAuthResponse` | `User: JellyfinUser`, `AccessToken`, `ServerId` |
| `JellyfinUser` | `Id`, `Name` |
| `JellyfinItemsResponse` | `Items: List<JellyfinItem>`, `TotalRecordCount`, `StartIndex` |
| `JellyfinItem` | `Id`, `Name`, `Type`, `Overview`, `ProductionYear`, `CommunityRating`, `OfficialRating`, `RunTimeTicks`, `ImageTags`, `BackdropImageTags`, `Genres`, `Studios`, `MediaSources`, series/season/episode fields, `UserData`, `Taglines`, `People` |
| `JellyfinMediaSource` | `Id`, `Name`, `Protocol`, `TranscodingUrl`, `DirectStreamUrl`, `SupportsDirectStream`, `SupportsTranscoding`, `MediaStreams` |
| `JellyfinMediaStream` | `Type`, `Codec`, `Language`, `DisplayTitle`, `Index`, `IsDefault`, `IsForced` |
| `JellyfinUserData` | `PlaybackPositionTicks`, `PlayCount`, `IsFavorite`, `Played`, `PlayedPercentage` |
| `JellyfinPlaybackInfo` | `MediaSources: List<JellyfinMediaSource>`, `PlaySessionId` |
| `PlaybackInfoRequest` | `UserId`, `MaxStreamingBitrate` (140 Mbps default — intentionally high so Jellyfin prefers direct play over transcoding; lower if bandwidth is constrained), `DeviceProfile: PlaybackDeviceProfile` |
| `PlaybackDeviceProfile` | `DirectPlayProfiles` (Video + Audio), `TranscodingProfiles` (H.264/AAC/TS) |
| `PlaybackStartInfo` | `ItemId`, `PlaySessionId`, `MediaSourceId`, `PositionTicks` |
| `PlaybackStopInfo` | same as above |
| `PlaybackProgressInfo` | same + `IsPaused`, `IsMuted` |

> **Note:** All Jellyfin data class property names use **PascalCase** to match the Jellyfin JSON API directly (no `@SerializedName` annotations needed).

### 7.4 JellyseerrApi — endpoints & data classes

**File:** `api/JellyseerrApi.kt`

Session-cookie authenticated (no explicit token header).

#### Endpoints

| Method | Path | Function |
|---|---|---|
| POST | `api/v1/auth/local` | `login` |
| GET | `api/v1/auth/me` | `getMe` |
| GET | `api/v1/movie/{movieId}` | `getMovieDetails` |
| GET | `api/v1/tv/{tvId}` | `getTvDetails` |
| POST | `api/v1/request` | `createRequest` |
| GET | `api/v1/request` | `getRequests` |
| GET | `api/v1/search` | `search` |
| GET | `api/v1/discover/movies` | `discoverMovies` |
| GET | `api/v1/discover/tv` | `discoverTv` |
| GET | `api/v1/discover/trending` | `getTrending` |

> **Note:** Jellyseerr data class properties use **camelCase** matching Jellyseerr's JSON keys.

---

## 8. UI Layer

### 8.1 Onboarding — ServerSetupActivity

**File:** `ui/onboarding/ServerSetupActivity.kt`

Standalone `AppCompatActivity` (launcher). Not part of the Navigation graph.

- Collects Jellyfin URL, username, password, optional Jellyseerr URL.
- **Connect**: calls `JellyfinApi.authenticateByName` with `X-Emby-Authorization` header (`MediaBrowser Client="SofaStream", Device="Android", DeviceId="sofastream-android", Version="1.0"`). On success, saves credentials via `UserPreferences` and navigates to `MainActivity`.
- **Skip**: saves URL with empty token / `"default"` userId for unauthenticated access.
- Validates that the URL starts with `http://` or `https://`.
- Handles `ConnectException`, `SocketTimeoutException`, `UnknownHostException` with specific user-facing messages.

### 8.2 MainActivity & Navigation

**File:** `MainActivity.kt`

- Single-Activity host. Inflates `ActivityMainBinding` which contains a `NavHostFragment` (`R.id.nav_host_fragment`) and a `BottomNavigationView`.
- Navigation graph drives Home → Detail, Search → Detail, and standalone Downloads/Settings tabs.
- Uses `setupWithNavController` to sync bottom nav with the nav controller.

### 8.3 Home — HomeFragment / HomeViewModel

**HomeFragment** (`ui/home/HomeFragment.kt`)
- Three horizontal `RecyclerView`s using `MediaRowAdapter`: Continue Watching, Recent Movies, Recent Series.
- Featured banner (backdrop image + title/meta + Play/Info buttons).
- `SwipeRefreshLayout` triggers `viewModel.loadHomeContent()`.
- Navigates to `DetailFragment` via `HomeFragmentDirections.actionHomeToDetail(item)` (SafeArgs).

**HomeViewModel** (`ui/home/HomeViewModel.kt`)
- `loadHomeContent()`: calls `getLatestMedia()` (splits by type into movies/series) and `getMovies(0, 10)` filtered to in-progress items for Continue Watching.
- Also exposes `getMovies(startIndex, callback)` and `getSeries(startIndex, callback)` for paginated loading.
- LiveData: `continueWatching`, `recentMovies`, `recentSeries`, `featuredItem`, `isLoading`, `error`.

### 8.4 Detail — DetailFragment / DetailViewModel

**DetailFragment** (`ui/detail/DetailFragment.kt`)
- Receives a `MediaItem` via SafeArgs (`args.mediaItem`).
- Displays backdrop, poster, title, overview, year/rating/runtime meta, tagline, genre chips, rating bar, cast.
- For Series: shows season chips (`ChipGroup`) and an episodes `RecyclerView` (`MediaRowAdapter`).
- **Play** button → `viewModel.getPlaybackInfo(item.id)` → on success, `launchPlayerWithInfo(PlaybackInfo)`.
- **Request** button → `viewModel.requestMedia(mediaType, 0)` (uses Jellyseerr).
- Episode tap → `viewModel.getPlaybackInfo(episode.id)`.

**DetailViewModel** (`ui/detail/DetailViewModel.kt`)
- `loadDetails(itemId)`: fetches item; if series, also loads seasons + first season's episodes.
- `selectSeason(season)`: loads episodes for the chosen season.
- `getPlaybackInfo(itemId)`: calls repository, posts to `_playbackInfo` LiveData.
- `requestMedia(mediaType, tmdbId)`: calls Jellyseerr; posts success/failure to `_requestSuccess`.
- Nulling helpers: `clearPlaybackInfo()`, `clearRequestStatus()` (called after observers consume one-shot events).
- LiveData: `mediaItem`, `seasons`, `episodes`, `selectedSeason`, `playbackInfo`, `isLoading`, `error`, `requestSuccess`.

### 8.5 Player — PlayerActivity

**File:** `ui/player/PlayerActivity.kt`

Standalone `AppCompatActivity`, launched from `DetailFragment` with a `PlaybackInfo` parcelable extra (`EXTRA_PLAYBACK_INFO`).

**Constants:**
- `CONTROLS_HIDE_DELAY_MS = 4000` — auto-hide overlay after 4 s
- `SKIP_DURATION_MS = 10_000` — forward/back skip step
- `PROGRESS_REPORT_INTERVAL_MS = 10_000` — session report interval

**Lifecycle:**
| Lifecycle event | Behaviour |
|---|---|
| `onCreate` | Setup fullscreen, extract `PlaybackInfo`, `setupPlayer()`, `setupControls()`, `setupMetadata()` |
| `onResume` | `player?.play()` |
| `onPause` | `player?.pause()` (unless PiP) |
| `onStop` | `reportPlaybackStop()`, cancel coroutine jobs (unless PiP) |
| `onDestroy` | Cancel jobs, release ExoPlayer, remove handler callbacks |
| `onUserLeaveHint` | Enter PiP (Android O+) if feature available |

**Player setup (`setupPlayer`):**
1. Creates `ExoPlayer` and attaches to `PlayerView` (custom controls, `useController = false`).
2. Seeks to `startPositionTicks / 10_000` (ms) if non-zero.
3. `playWhenReady = true`.
4. State listener: `STATE_BUFFERING` shows spinner; `STATE_READY` starts progress reporting + time bar updates + calls `reportPlaybackStart()`; `STATE_ENDED` calls `reportPlaybackStop()`.
5. Error listener: shows `tvError` with message.

**Controls (`setupControls`):** Play/Pause toggle, ±10 s skip with animated skip indicators, back button, PiP button (shown only if feature present), auto-hide after 4 s when playing.

**Metadata (`setupMetadata`):** Shows series name (title bar) + "S{n}E{n} • {episode title}" subtitle for episodes; movie title only for movies.

**Time bar:** `DefaultTimeBar` (Media3) updates every 500 ms via a coroutine job. Scrub-stop seeks the player.

**Progress reporting:** Coroutine fires every 10 s calling `JellyfinRepository.reportPlaybackProgress`. Errors are silently swallowed.

**PiP:** 16:9 aspect ratio on Android O+. In PiP mode, controls and progress bar are hidden.

**Fullscreen:** Android R+ uses `WindowInsetsController`; older uses deprecated `systemUiVisibility` flags.

### 8.6 Search — SearchFragment / SearchViewModel

**SearchFragment** (`ui/search/SearchFragment.kt`)
- `EditText` with `TextWatcher` triggers `viewModel.search(query)`.
- Results in a 3-column `GridLayoutManager` using `MediaGridAdapter`.
- Navigates to detail via `SearchFragmentDirections.actionSearchToDetail(item)`.

**SearchViewModel** (`ui/search/SearchViewModel.kt`)
- Debounces queries by 300 ms (cancels previous `searchJob` before launching new one).
- Calls `JellyfinRepository.searchItems(query)` — searches Movies + Series.
- LiveData: `results`, `isLoading`, `error`.

### 8.7 Settings — SettingsFragment

**File:** `ui/settings/SettingsFragment.kt`

- Shows current Jellyfin URL, Jellyseerr URL, and connected username (collected from `UserPreferences` flows).
- **Save**: validates URL, saves updated URLs, calls `ApiClient.resetClients()` to force re-creation with new base URLs.
- **Logout**: `prefs.clearAll()`, `ApiClient.resetClients()`, navigates to `ServerSetupActivity` with `FLAG_ACTIVITY_NEW_TASK or CLEAR_TASK`.

### 8.8 Downloads — DownloadsFragment

**File:** `ui/downloads/DownloadsFragment.kt`

Placeholder screen — only inflates the layout. No functionality implemented.

### 8.9 Common Adapters

Both adapters extend `ListAdapter<MediaItem, *>` with `DiffUtil` (equality by `id`).

**`MediaRowAdapter`** — horizontal card list
- Binds `posterUrl ?: thumbUrl ?: backdropUrl` via Glide.
- Shows a `ProgressBar` overlay when `playedPercentage > 0` (continue watching indicator).

**`MediaGridAdapter`** — 3-column grid
- Same image priority as above.
- No progress bar overlay.

---

## 9. End-to-End Data Flows

### 9.1 First-launch / Onboarding

```
ServerSetupActivity.onCreate
  → prefs.isSetupCompleteSync() == false
  → User fills URL / username / password
  → btnConnect click
  → ApiClient.getJellyfinApi(url).authenticateByName(header, body)
  → response.isSuccessful
  → prefs.saveJellyfinCredentials(url, token, userId, userName)
  → prefs.setSetupComplete(true)
  → startActivity(MainActivity) + finish()
```

### 9.2 Home screen loading

```
HomeFragment.onViewCreated
  → viewModel.loadHomeContent()
    → JellyfinRepository.getLatestMedia()
        → GET Users/{userId}/Items/Latest
        → split items by MediaType → _recentMovies, _recentSeries, _featuredItem
    → JellyfinRepository.getMovies(0, 10)
        → GET Users/{userId}/Items?IncludeItemTypes=Movie&...
        → filter playedPercentage > 0 && < 100 → _continueWatching
  → LiveData observers update RecyclerViews + featured banner
```

### 9.3 Media detail & playback

```
HomeFragment: navigateToDetail(item)
  → SafeArgs action → DetailFragment (receives MediaItem)

DetailFragment.onViewCreated
  → viewModel.loadDetails(item.id)
    → JellyfinRepository.getItemDetails(itemId)
        → GET Users/{userId}/Items/{itemId}?Fields=...
    → if Series: loadSeasons → loadEpisodes(firstSeason)
  → LiveData observers bind UI

User taps Play (or episode row)
  → viewModel.getPlaybackInfo(itemId)
    → JellyfinRepository.getPlaybackInfo(itemId)
        → POST Items/{itemId}/PlaybackInfo  { UserId, MaxStreamingBitrate, DeviceProfile }
        → resolve streamUrl (direct / transcode / fallback)
        → GET Users/{userId}/Items/{itemId}  (resume position + metadata)
        → return PlaybackInfo
  → _playbackInfo LiveData → DetailFragment observer
  → Intent(PlayerActivity, EXTRA_PLAYBACK_INFO=playbackInfo)
  → startActivity(intent)

PlayerActivity.onCreate
  → ExoPlayer.setMediaItem(streamUrl) → prepare() → seekTo(startPositionTicks/10000) → play
```

### 9.4 Search

```
SearchFragment: TextWatcher → viewModel.search(query)
  → cancel previous job, delay 300 ms
  → JellyfinRepository.searchItems(query)
      → GET Users/{userId}/Items?searchTerm=...&IncludeItemTypes=Movie,Series
  → _results LiveData → MediaGridAdapter.submitList(items)
```

### 9.5 Playback session reporting

```
ExoPlayer STATE_READY
  → reportPlaybackStart()
      → POST Sessions/Playing { ItemId, MediaSourceId, PlaySessionId }
  → startProgressReporting()  [coroutine, every 10 s]
      → POST Sessions/Playing/Progress { ItemId, ..., PositionTicks, IsPaused }
  → startTimeBarUpdates()     [coroutine, every 500 ms → UI only]

ExoPlayer STATE_ENDED / onStop / onDestroy
  → reportPlaybackStop()
      → POST Sessions/Playing/Stopped { ItemId, ..., PositionTicks }
  → progressReportJob.cancel()
```

---

## 10. Error Handling Patterns

All repository functions use the `Result<T>` pattern:

```kotlin
return try {
    val response = api.someEndpoint(...)
    if (response.isSuccessful) {
        Result.success(response.body()!!)
    } else {
        Result.failure(Exception("Error: ${response.code()}"))
    }
} catch (e: Exception) {
    Result.failure(e)
}
```

ViewModels observe failures and post to `_error: MutableLiveData<String?>`:

```kotlin
result.onSuccess { /* update state */ }
result.onFailure { _error.value = it.message }
```

Fragments observe `error` and show a `Toast`. Home screen also shows an inline `tvError` text view.

Playback session reporting (`reportPlaybackStart/Stop/Progress`) silently swallows all exceptions — player operation is never interrupted by reporting failures.

---

## 11. Coding Conventions

- **No DI framework** — repositories are instantiated directly in ViewModels using `prefs.*Sync()` helpers.
- **ViewBinding** is used in every Fragment/Activity; `_binding` nullable field pattern with `get() = _binding!!` and nulled in `onDestroyView`.
- **Jellyfin data classes use PascalCase** property names to match the Jellyfin REST API JSON keys without `@SerializedName`.
- **Jellyseerr data classes use camelCase** property names.
- **Coroutines**: all network calls are `suspend` functions called from `viewModelScope.launch` or `lifecycleScope.launch`.
- **One-shot LiveData events** (playbackInfo, requestSuccess) are cleared via `clear*()` methods after the observer consumes them.
- **Result<T>** is used universally for repository return values instead of exceptions or nullable returns.
- **SafeArgs** is used for all Fragment-to-Fragment navigation arguments.
- `PlayerActivity` is a separate Activity (not a Fragment) to enable full lifecycle control over `ExoPlayer` and PiP mode.
- Image loading priority in adapters: `posterUrl ?: thumbUrl ?: backdropUrl`.
- Runtime ticks in Jellyfin are in **100-nanosecond units** (divide by `10_000` → ms; divide by `600_000_000` → minutes).
