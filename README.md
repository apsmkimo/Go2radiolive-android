<!-- SMCPKG_SUPPORT>>>Cursor001
# Go2radiolive-android
An android network radio app
SMCPKG_SUPPORT<<<Cursor001 -->

# Go2Radio Live

Kotlin + Jetpack Compose + Material 3 網路收音機 App，採用 **MVVM + Clean Architecture**。

- `applicationId`: `com.apsmkimo.go2radiolive`
<!-- SMCPKG_SUPPORT>>>Cursor016
- `versionName`: `1.0.0` / `versionCode`: `1`
SMCPKG_SUPPORT<<<Cursor016 -->
<!-- SMCPKG_SUPPORT>>>Cursor039
- `versionName`: `1.1.0` / `versionCode`: `2`
SMCPKG_SUPPORT<<<Cursor039 -->
<!-- SMCPKG_SUPPORT>>>Cursor057
- `versionName`: `1.1.1` / `versionCode`: `3`
SMCPKG_SUPPORT<<<Cursor057 -->
- `versionName`: `1.2.0` / `versionCode`: `4`
- 最低 SDK：26，目標 SDK：35

## 功能

- 頂部 AdMob 橫幅（Google **測試** ID）
  - App：`ca-app-pub-3940256099942544~3347511713`
  - Banner：`ca-app-pub-3940256099942544/6300978111`
- 搜尋列即時過濾目前列表；鬧鐘圖示顯示 Toast「鬧鐘功能開發中」
- 電台列表：favicon（Coil，失敗時 fallback）、點卡片播放、愛心收藏
- Mini Player：目前電台名稱 + 播放/暫停
- 右上角齒輪進入設定，顯示 versionName / versionCode
- 音效面板按鈕、四段等化器（Bass / Low / Mid / High）、左右平衡、語音增強（自訂 PCM DSP，DataStore 持久化）
- 3D 空間音效：關閉 / 自然 3D / 環繞 / 環軌 四選一（參數化雙耳模型，取代虛擬環繞）。乾聲與雙耳路徑對齊同一基底延遲；自然 3D 的 Mono 在正中，Stereo 寬度隨相關性連續變化
- 介面字串跟隨系統語言：預設英文、繁體中文（台灣／香港）、簡體中文、日文
- 背景播放：AndroidX Media3 ExoPlayer + `MediaSessionService` 前景服務，支援通知列 / 鎖屏控制。Podcast 單集也走這條播放路徑
- 底部分頁：在地 / 世界 / Podcast / 分類 / 我的最愛
- 我的最愛以 DataStore Preferences（Gson JSON）持久化

## Radio-Browser API

Base URL 使用官方鏡像 `https://de1.api.radio-browser.info/`，並送出 `User-Agent: Go2RadioLive/1.2.0`。

| 分頁 | 端點 | 說明 |
| --- | --- | --- |
| 在地（預設） | `GET /json/stations/bycountrycodeexact/TW?limit=100` | 台灣電台 |
| 世界 | `GET /json/countries` → `GET /json/stations/bycountrycodeexact/{code}?limit=100` | 國家分類（名稱依系統語言），點選後列出該國電台 |
| Podcast | Apple Marketing Tools 排行榜（每次最多 100）＋ iTunes 類型 RSS，去重後約 200 個；單集來自 iTunes Lookup 的 `feedUrl` RSS | 地區用裝置區域，沒有則用 US。單集以既有 ExoPlayer 播放 |
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
