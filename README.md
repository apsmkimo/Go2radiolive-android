<!-- SMCPKG_SUPPORT>>>Cursor001
# Go2radiolive-android
An android network radio app
SMCPKG_SUPPORT<<<Cursor001 -->

# Go2Radio Live

Kotlin + Jetpack Compose + Material 3 網路收音機 App，採用 **MVVM + Clean Architecture**。

- `applicationId`: `com.apsmkimo.go2radiolive`
- `versionName`: `1.0.0` / `versionCode`: `1`
- 最低 SDK：26，目標 SDK：35

## 功能

- 頂部 AdMob 橫幅（Google **測試** ID）
  - App：`ca-app-pub-3940256099942544~3347511713`
  - Banner：`ca-app-pub-3940256099942544/6300978111`
- 搜尋列即時過濾目前列表；鬧鐘圖示顯示 Toast「鬧鐘功能開發中」
- 電台列表：favicon（Coil，失敗時 fallback）、點卡片播放、愛心收藏
- Mini Player：目前電台名稱 + 播放/暫停
- 背景播放：AndroidX Media3 ExoPlayer + `MediaSessionService` 前景服務，支援通知列 / 鎖屏控制
- 底部分頁：在地 / 世界 / 保留 / 分類 / 我的最愛
- 我的最愛以 DataStore Preferences（Gson JSON）持久化

## Radio-Browser API

Base URL 使用官方鏡像 `https://de1.api.radio-browser.info/`，並送出 `User-Agent: Go2RadioLive/1.0.0`。

| 分頁 | 端點 | 說明 |
| --- | --- | --- |
| 在地（預設） | `GET /json/stations/bycountrycodeexact/TW?limit=100` | 台灣電台 |
| 世界 | `GET /json/stations/search?limit=100&hidebroken=true&order=clickcount&reverse=true` | 全球點擊數最高的 100 個電台（不限國家） |
| 保留 | `GET /json/stations/search?limit=100&hidebroken=true&order=votes&reverse=true` | 投票數最高的精選電台（placeholder） |
| 分類 | `GET /json/tags` → `GET /json/stations/bytagexact/{tag}?limit=100` | 熱門 tag 列表，點選後載入該分類電台 |
| 播放解析 | `GET /json/url/{stationuuid}` | 增加 click 並取得可播放 URL，失敗則退回 `url_resolved` |

搜尋列只過濾**目前已載入**的列表，不會再打 API。

## 架構

```
presentation  →  domain  ←  data
   UI/VM         模型/用例      Retrofit + DataStore
```

- **domain**：`RadioStation`、repository 介面、use case
- **data**：Radio-Browser Retrofit API、DataStore 收藏、repository 實作
- **presentation**：Compose UI、`HomeViewModel`、Media3 `RadioPlaybackService`
- **DI**：手動 `AppContainer` composition root（未使用 Hilt）

主要路徑：

- `app/src/main/java/com/apsmkimo/go2radiolive/Go2RadioApp.kt`
- `app/src/main/java/com/apsmkimo/go2radiolive/di/AppContainer.kt`
- `app/src/main/java/com/apsmkimo/go2radiolive/data/remote/RadioBrowserApi.kt`
- `app/src/main/java/com/apsmkimo/go2radiolive/presentation/player/RadioPlaybackService.kt`
- `app/src/main/java/com/apsmkimo/go2radiolive/presentation/ui/home/HomeScreen.kt`

## 建置

需要 JDK 17+ 與 Android SDK（compileSdk 35）。

```bash
./gradlew assembleDebug
```

產物：`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions：`.github/workflows/assemble-debug.yml` 會在 push / PR 執行 `assembleDebug`。

本機請設定 `ANDROID_HOME`，或建立 `local.properties`：

```
sdk.dir=/path/to/Android/sdk
```
