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
<!-- SMCPKG_SUPPORT>>>Cursor080
- `versionName`: `1.2.0` / `versionCode`: `4`
SMCPKG_SUPPORT<<<Cursor080 -->
<!-- SMCPKG_SUPPORT>>>Cursor086
- `versionName`: `1.2.1` / `versionCode`: `5`
SMCPKG_SUPPORT<<<Cursor086 -->
<!-- SMCPKG_SUPPORT>>>Cursor093
- `versionName`: `1.3.0` / `versionCode`: `6`
SMCPKG_SUPPORT<<<Cursor093 -->
<!-- SMCPKG_SUPPORT>>>Cursor100
- `versionName`: `1.4.0` / `versionCode`: `7`
SMCPKG_SUPPORT<<<Cursor100 -->
<!-- SMCPKG_SUPPORT>>>Cursor102
- `versionName`: `1.5.0` / `versionCode`: `8`
SMCPKG_SUPPORT<<<Cursor102 -->
<!-- SMCPKG_SUPPORT>>>Cursor106
- `versionName`: `1.5.1` / `versionCode`: `9`
SMCPKG_SUPPORT<<<Cursor106 -->
- `versionName`: `1.6.0` / `versionCode`: `10`
- `versionName`: `1.6.1` / `versionCode`: `11`
- `versionName`: `1.0.0` / `versionCode`: `12`
- 最低 SDK：26，目標 SDK：35

## 功能

- 啟動時以 Google UMP 更新同意狀態；只有 `canRequestAds()` 為真才初始化 Mobile Ads 並載入廣告。設定頁在需要時顯示「隱私選項」。debug 不強制 DebugGeography，release 不會帶入
- 頂部為錨定自適應橫幅（容器實際寬度）。ID 依 build type 分開，debug 不會帶正式廣告單元
  - debug（Google 官方測試 ID）：App `ca-app-pub-3940256099942544~3347511713`，Banner `ca-app-pub-3940256099942544/6300978111`，Native `ca-app-pub-3940256099942544/2247696110`
  - release：App `ca-app-pub-6138247296137119~5720425997`，Banner `ca-app-pub-6138247296137119/7955882105`，Native `ca-app-pub-6138247296137119/2601192653`
- 原生廣告約每 8 列插入在地、世界的電台列表（含該列表搜尋結果）與 Podcast 列表。載入失敗或未同意時不占位。沒有插頁或獎勵廣告
- 開屏廣告：冷啟動在同意表單結束且 Mobile Ads 初始化之後才嘗試，4 秒內載入完成才顯示，否則略過。從背景回到前景（ProcessLifecycleOwner）若已有未過期廣告且通過檢查才顯示。兩次顯示至少間隔 4 小時，廣告本身 4 小時過期。電台或 Podcast 播放／緩衝中不顯示，也不蓋在同意表單或其他全螢幕廣告上
  - debug（Google 官方測試開屏）：`ca-app-pub-3940256099942544/9257395921`
  - release：`ca-app-pub-6138247296137119/7661947645`
- 搜尋列即時過濾目前列表；鬧鐘圖示顯示 Toast「鬧鐘功能開發中」。Podcast 分頁在鬧鐘旁有國家／地區圖示，可搜尋 Apple 排行榜支援的地區（含「自動（裝置地區）」），選擇會存進 DataStore 並重載列表
- 電台列表：favicon（Coil，失敗時 fallback）、點卡片播放、愛心收藏
- Mini Player：目前電台名稱 + 播放/暫停
- 右上角齒輪進入設定，顯示 versionName / versionCode
- 音效面板按鈕、四段等化器（Bass / Low / Mid / High）、左右平衡、語音增強（自訂 PCM DSP，DataStore 持久化）
- 3D 空間音效：關閉 / 自然 3D / 環繞 / 環軌 四選一（參數化雙耳模型，取代虛擬環繞）。空間音效開啟時，乾聲與雙耳路徑對齊同一基底延遲；關閉時完全略過空間處理（沒有 64-sample 基底延遲，Mono 維持單聲道）。切換關閉時以約 30 ms 交叉淡化，Mono 串流另以音量淡出／淡入後重新設定播放器。自然 3D 的 Mono 在正中，Stereo 寬度隨相關性連續變化
- 介面字串跟隨系統語言：預設英文、繁體中文（台灣／香港）、簡體中文、日文
- 背景播放：AndroidX Media3 ExoPlayer + `MediaSessionService` 前景服務，支援通知列 / 鎖屏控制。Podcast 單集也走這條播放路徑
- 底部分頁：在地 / 世界 / Podcast / 分類 / 我的最愛
- 我的最愛以 DataStore Preferences（Gson JSON）持久化

## Radio-Browser API

Base URL 使用官方鏡像 `https://de1.api.radio-browser.info/`，並送出 `User-Agent: Go2RadioLive/1.6.1`。

重新整理會帶 `Cache-Control: no-cache` 與 `cb` 查詢參數，按鈕在載入時停用並顯示進度，完成後出現 Snackbar，失敗時列表仍在也會顯示錯誤。

鬧鐘圖示開啟睡眠計時與定時開播。睡眠計時在播放服務裡倒數，結束前約 30 秒淡出。定時開播使用 `AlarmManager.setAlarmClock` 與 `SCHEDULE_EXACT_ALARM`（沒有 `USE_EXACT_ALARM`）。未允許精確鬧鐘時改為不精確排程並說明。開機與時區變更會重新排程。權限：`RECEIVE_BOOT_COMPLETED`、`SCHEDULE_EXACT_ALARM`。

| 分頁 | 端點 | 說明 |
| --- | --- | --- |
| 在地（預設） | `GET /json/stations/bycountrycodeexact/{cc}?limit=500&hidebroken=true&order=clickcount` | 裝置地區：網路國碼、SIM 國碼，其次系統語系國家。無法判斷，或該區沒有電台時改查 US。偵測為 TW 時仍查 TW。2026-10-19 健康的 TW 列表有 193 個；`Radio ICRT` 點擊排序第 102、`ICRT` 第 159（皆 lastcheckok=1）。limit 100 會把兩者裁掉，所以改為 500 |
| 世界 | `GET /json/countries` → `GET /json/stations/bycountrycodeexact/{code}?limit=500` | 國家分類（名稱依系統語言），點選後列出該國最多 500 個點擊最高的電台 |
| Podcast | Apple Marketing Tools 排行榜（每次最多 100）＋ iTunes 類型 RSS，去重後約 200 個；單集來自 iTunes Lookup 的 `feedUrl` RSS | 預設用與在地相同的裝置地區。使用者選定的 storefront 存在 DataStore。進入分頁先顯示該區上次成功的列表（超過約 12 小時或手動重新整理才在背景更新）。總榜與類型榜同時抓取，總榜先出現，類型再併入。單一類型失敗不會清空列表；全部失敗且沒有快取才顯示全頁錯誤，有快取則保留列表並提示重試。只有該區沒有排行榜（空結果，或不是 Apple storefront）才改顯示美國排行榜並附一行說明。請求失敗不會偷偷換成美國榜。單集以既有 ExoPlayer 播放 |
| 分類 | 排行榜（votes / clickcount / clicktrend）與固定分類的 tag 查詢 | 上方選擇國家（預設與在地相同，或全球）。分類以卡片顯示電台數。點進列表可依熱門、投票、名稱排序，並篩選語言與 128 kbps 以上。該國結果少於 10 個時，後面加上其他國家。上次成功的內容會先顯示，超過約 12 小時或手動重新整理才在背景更新 |
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
