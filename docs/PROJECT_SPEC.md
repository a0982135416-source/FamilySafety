# FamilySafety 家庭安全管理系統 --- PROJECT_SPEC

> **文件用途**：本文件是 `FamilySafety` 專案的核心規格來源（Project
> Specification / Shared Context）。\
> ChatGPT、Codex、Android Studio 開發與 GitHub
> 三機同步時，皆應以本文件中「已確認」的規格為基準。\
> 若後續需求正式變更，應同步更新本文件，避免
> Notebook／學校桌機／家裡桌機出現不同版本的專案認知。

------------------------------------------------------------------------

## 0. 專案基本資訊

-   **專案名稱**：家庭安全管理系統（FamilySafety）
-   **Android 專案**：`FamilySafety`
-   **GitHub Repository**：`Iishvarii/FamilySafety`
-   **主要開發環境**：Android Studio
-   **Android 語言**：Kotlin
-   **UI 技術**：XML Layout + ViewBinding
-   **目前開發者程度**：Android / Kotlin 初學階段
-   **核心原則**：優先使用清楚、可維護、容易理解的實作，不為了「看起來專業」而加入目前不必要的複雜框架。

------------------------------------------------------------------------

# 1. 系統目標

本專題以家庭環境安全、任務管理與異常警示為核心，整合 IoT
感測器、Raspberry Pi Server、MariaDB 與 Android App。

整體資料流程：

``` text
IoT Sensors
    ↓
ESP8266 / Raspberry Pi
    ↓
Server REST API
    ↓
MariaDB
    ↓
Android App
```

警報採事件驅動：

``` text
感測資料
    ↓
Server 判斷安全條件
    ↓
建立 Alert
    ↓
Push Notification
    ↓
Android 接收通知
    ↓
Alert Details
```

Android App 不應依靠 App 長時間常駐背景持續偵測危險；主要警報條件應由
Server 判斷。

------------------------------------------------------------------------

# 2. 硬體與感測器

目前專題使用／規劃的硬體：

  元件             用途
  ---------------- -------------------------
  Raspberry Pi     Server / 系統整合
  ESP8266          感測資料傳輸
  可變電阻         **模擬瓦斯開關 ON/OFF**
  火焰感測器       判斷是否有火焰
  PIR 人體感測器   判斷是否偵測到人員
  AM2302 / DHT22   溫度、濕度

### 重要限制

目前的可變電阻只是模擬：

``` text
Gas Switch = ON / OFF
```

它**不是瓦斯濃度感測器**。

因此 UI、Alert、API 與文件應使用：

-   瓦斯開關開啟
-   瓦斯外洩風險
-   異常燃氣使用風險

避免錯誤描述成：

-   已偵測到瓦斯濃度
-   已確認瓦斯外洩

除非未來真的加入瓦斯濃度感測器。

------------------------------------------------------------------------

# 3. 安全判斷邏輯

## 3.1 瓦斯開啟但沒有火焰

``` text
Gas = ON
Flame = FALSE
        ↓
瓦斯外洩風險
        ↓
立即警示
```

此情境不需要等待安全倒數。

------------------------------------------------------------------------

## 3.2 有火焰但沒有偵測到人員

安全倒數的核心觸發條件是：

``` text
Flame = TRUE
Person = FALSE
        ↓
無人看管火源風險
        ↓
自動啟動安全倒數
```

安全倒數：

-   預設：**10 分鐘**
-   使用者可自由輸入分鐘數
-   例如：18 分鐘
-   不限制固定 5 / 10 / 15 / 30 分鐘
-   不需要額外顯示「開始時間」欄位
-   亦允許使用者手動啟動 Timer

倒數結束：

``` text
Timer = 0
AND
Person = FALSE
        ↓
建立警報
```

如果人員在倒數期間回來：

``` text
Person = TRUE
        ↓
取消 / 重置倒數
```

> 注意：不要把安全倒數錯誤設計成「Gas ON + Person
> absent」才啟動。核心條件是 **Flame detected + Person not detected**。

------------------------------------------------------------------------

# 4. Android 整體架構

Android 採 Single Activity Architecture：

``` text
MainActivity
├── HomeFragment
├── EnvironmentFragment
├── AlertFragment
├── TaskFragment
└── ManagementFragment
```

主要資料架構：

``` text
Fragment
    ↓
ViewModel
    ↓
Repository
    ↓
Retrofit / ApiService
    ↓
REST API
    ↓
Raspberry Pi Server
    ↓
MariaDB
```

登入相關流程為降低初學階段複雜度，可以使用獨立 Activity：

``` text
LoginActivity
ForgotPasswordActivity
VerifyCodeActivity
ResetPasswordActivity
```

------------------------------------------------------------------------

# 5. activity_main.xml

`activity_main.xml` 只作為主要 Fragment 容器與 Bottom Navigation。

主要元件：

``` text
ConstraintLayout
├── FragmentContainerView
└── BottomNavigationView
```

已使用／預定 ID：

``` text
fragmentContainer
bottomNavigation
```

Bottom Navigation：

  ID                 功能
  ------------------ ----------
  `navHome`          首頁
  `navEnvironment`   環境監控
  `navAlert`         警報
  `navTask`          任務
  `navManagement`    管理

**Fragment Layout 不可再次放入 BottomNavigationView。**

------------------------------------------------------------------------

# 6. Android UI 開發原則

主要使用：

``` text
ConstraintLayout
NestedScrollView
MaterialCardView
TextView
ImageView
ImageButton
TextInputLayout
EditText
MaterialButton
RecyclerView
BottomNavigationView
ProgressBar
MaterialAlertDialog
```

目前不使用 Jetpack Compose。

Server / API 尚未完成時，可以先：

``` text
Mock Data
    ↓
完成 Layout
    ↓
完成畫面互動
    ↓
ViewModel / Repository
    ↓
Retrofit / API
```

------------------------------------------------------------------------

# 7. ID 與 Resource 命名

## 7.1 原則

ID 必須：

-   唯一
-   簡單
-   可辨識用途
-   同功能保持一致
-   程式需要操作或資料會變動的元件優先設定 ID
-   純裝飾／固定背景不應為了方便而大量加入無意義 ID

範例：

``` text
btnLogin
btnMenu
btnProfile

tvGreeting
tvTemperature
tvHumidity
tvAlertCount

ivHomeStatus
ivAlertStatus

cardSafety
cardEnvironment

rvTodayTasks

progressTask
```

當既有 XML 已有 ID：

> **優先沿用既有 ID。**

除非有明確理由，不應任意重新命名，以免 Kotlin / ViewBinding 一起失效。

------------------------------------------------------------------------

# 8. 字串與語系

畫面文字應放入：

``` text
res/values/strings.xml
res/values-zh-rTW/strings.xml
```

Class、Variable、ID 使用英文。

UI 不應同時重複顯示中文主標與英文副標，例如：

``` text
警報中心
Alert Center
```

如果兩者只是相同意思，原則上只顯示目前語系的一個標題。

目標：

-   減少重複資訊
-   增加有效畫面空間
-   支援 Localization

------------------------------------------------------------------------

# 9. 全專案色彩

  用途       HEX
  ---------- -----------
  App 背景   `#F2FAFF`
  主藍色     `#1687E8`
  深藍標題   `#0B3D75`
  淺藍色     `#E8F5FF`
  灰藍色     `#8CA5BD`
  次要文字   `#607D9D`
  安全綠     `#19B394`
  警示紅     `#F0525C`
  Card       `#FFFFFF`

優先集中定義於：

``` text
res/values/colors.xml
```

避免長期在各 XML 大量 Hardcode 顏色。

------------------------------------------------------------------------

# 10. Home

主要 Layout：

``` text
fragment_home.xml
```

畫面：

``` text
Header
├── Menu
├── 您好！使用者
└── Profile

家庭目前狀態

2 × 2 Cards
├── 環境安全
├── 環境監測
├── 今日任務
└── 今日任務列表

警報摘要
```

Bottom Navigation 已存在 `activity_main.xml`，不可放入
`fragment_home.xml`。

## 10.1 家庭狀態

``` text
SAFE
WARNING
DANGER
```

顏色：

``` text
SAFE       → 綠
WARNING    → 橘
DANGER     → 紅
```

Icon、文字、Card 等應可由 Kotlin 根據狀態動態更新，不可把 SAFE
顏色永久畫死在圖片中。

## 10.2 今日任務

使用 RecyclerView：

``` text
rvTodayTasks
```

目前已有／曾使用：

``` text
TodayTaskAdapter.kt
item_today_task.xml
```

今日任務區應支援列表捲動，避免內容超出 Card。

------------------------------------------------------------------------

# 11. Environment Monitoring

採單頁上下滑動。

``` text
即時影像
    ↓
瓦斯開關
火焰
人體
    ↓
安全倒數
    ↓
溫度 / 濕度
    ↓
歷史曲線
```

地點：

``` text
廚房
客廳
臥室
```

## 11.1 即時狀態

應呈現：

-   Gas Switch
-   Flame
-   Person
-   Temperature
-   Humidity

## 11.2 Safety Countdown

-   自動觸發：Flame detected + Person not detected
-   亦可手動 Start Timer
-   預設 10 分鐘
-   使用者自由輸入分鐘
-   人員回歸時 Reset
-   倒數結束仍無人時建立警報

------------------------------------------------------------------------

# 12. Alert Center

主要分成：

``` text
環境監控警報
任務逾期
```

## 12.1 環境 Alert 狀態

建議流程：

``` text
PENDING
    ↓
使用者確認處理
    ↓
ACKNOWLEDGED
    ↓
感測狀態恢復安全
    ↓
RESOLVED
```

中文：

``` text
待處理
已確認
已解除
```

重要：

> **「使用者已確認」不代表「危險已解除」。**

確認與解除必須是不同狀態概念。

## 12.2 UI Filter

環境警報：

``` text
全部
待處理
已解除
```

若 UI / API 需要呈現 `ACKNOWLEDGED`，可再依實際需求加入，但不得把它和
`RESOLVED` 混為一談。

任務逾期：

``` text
全部
待處理
進行中
逾期
```

Filter、Badge、Count、Button、Card Background 優先使用 Android XML /
Material Components，不製作成 PNG。

------------------------------------------------------------------------

# 13. Task Center

流程：

``` text
Calendar
日 / 週 / 月
    ↓
Filter
全部 / 待處理 / 進行中 / 逾期
    ↓
RecyclerView
    ↓
Task Details
    ↓
Progress / Report
```

## 13.1 儲存狀態

Database / API 真正儲存：

``` text
PENDING
IN_PROGRESS
COMPLETED
```

`OVERDUE` 原則上為衍生狀態：

``` text
now > due_date
AND
status != COMPLETED
```

因此不必作為基本 Task Status 儲存。

## 13.2 權限

一般使用者：

-   查看任務
-   查看詳情
-   更新進度
-   回報

一般使用者**不提供新增任務**。

新增／指派：

``` text
Management
    ↓
Task Management
```

------------------------------------------------------------------------

# 14. Management

只提供 Admin。

``` text
Management
├── Member Management
└── Task Management
```

## 14.1 Member Management

新增成員使用：

``` text
MaterialAlertDialog
```

欄位：

``` text
姓名
帳號
Email
角色
```

管理員不直接替新成員設定正式密碼。

建議流程：

``` text
Admin 建立成員
    ↓
Server 寄送啟用 / 驗證資訊
    ↓
新成員自行建立密碼
```

## 14.2 權限安全

Android 隱藏 Management：

``` text
只屬於 UI / UX 控制
```

真正的 Admin 權限必須由 Server 驗證。

不可只依靠：

``` text
if (isAdmin) {
    showManagement()
}
```

來保護 Server API。

------------------------------------------------------------------------

# 15. Forgot Password

固定流程：

``` text
Login
    ↓
Forgot Password
    ↓
輸入 Email
    ↓
Server 產生 6 位 Reset Code
    ↓
Email
    ↓
Verify Code
    ↓
Reset Password
    ↓
Login
```

原則：

-   Reset Code 由 Server 產生、保存與驗證
-   Email 由 Server 發送
-   Android 不直接使用 SMTP 發信
-   Reset Code 應設定有效期限
-   驗證成功後才能允許 Reset Password

------------------------------------------------------------------------

# 16. Push Notification

事件驅動：

``` text
Sensor
    ↓
Raspberry Pi / Server
    ↓
Server 判斷 Alert Condition
    ↓
建立 Alert Record
    ↓
Push Notification
    ↓
Android
    ↓
使用者點擊
    ↓
Alert Details
```

Android 不應為了安全偵測而要求 App 永久保持前景或 Always Running。

------------------------------------------------------------------------

# 17. Server / Database / API

目前系統：

``` text
Android App
    ↓
REST API
    ↓
Raspberry Pi Server
    ↓
MariaDB
```

MariaDB 已確認為實際資料庫；早期 SQLite 討論不再作為目前正式架構。

## 17.1 Task 已知欄位

``` text
id
title
description
assignee_id
type
frequency
due_date
status
```

可能值：

``` text
type:
- one-time
- recurring

frequency:
- daily
- weekly
- monthly

status:
- PENDING
- IN_PROGRESS
- COMPLETED
```

Database Schema 尚未完全定案的部分，不應由 Codex 自行假設。

------------------------------------------------------------------------

# 18. Android Drawable / Icon 規格

Android UI 圖片素材原則：

``` text
預設尺寸：100 × 100 px
格式：PNG
背景：真正 Alpha Transparent
用途：Android Studio res/drawable
主體：置中
四周：適當透明邊距
風格：簡潔、現代、圓潤、藍白 Android UI
```

除非另外指定：

-   不加文字
-   不加外框
-   不加陰影
-   不加背景

## 18.1 真正透明

「去背」必須是：

``` text
PNG Alpha Channel = Transparent
```

禁止：

``` text
把灰白棋盤格直接畫進 PNG
```

Android Studio 顯示的棋盤格只是編輯器透明背景提示，不應成為圖片像素。

## 18.2 XML 可做的背景不要畫進 PNG

例如：

``` text
紅色圓形背景
    ↓
Android XML / Shape Drawable

白色 Flame Icon
    ↓
Transparent PNG
```

Card、圓形、Badge、Filter Background 等可由 XML 完成者，不應做成 PNG。

## 18.3 動態狀態

如果同一 Icon 需要 SAFE / WARNING / DANGER：

優先考慮：

-   Drawable Tint
-   XML Shape
-   Kotlin 動態切換

避免為每個狀態製作大量幾乎相同的 Bitmap。

------------------------------------------------------------------------

# 19. Android 開發回答／Codex 修改原則

處理本專案程式碼時：

1.  先理解現有程式碼與 Resource。
2.  優先修改現有檔案，不任意建立重複架構。
3.  不得假設不存在的 Drawable、ID、Activity、Fragment、String 或 API。
4.  若需要新增 Resource，必須明確列出新增內容。
5.  優先沿用既有 ID。
6.  完整程式碼應有大項註解。
7.  不以 `...` 省略必要程式。
8.  一次集中處理一個 Layout / Kotlin 邏輯區域。
9.  發現架構、安全邏輯或資料模型問題時應指出，而不是盲目延續錯誤。
10. 初學階段以可理解、可 Debug 為優先。

XML 建議區塊：

``` xml
<!-- 01. Header -->
<!-- 02. Status -->
<!-- 03. Environment -->
<!-- 04. Task -->
<!-- 05. Alert -->
```

------------------------------------------------------------------------

# 20. Git / 三台電腦同步規則

開發設備：

``` text
Notebook
學校桌機
家裡桌機
```

三台電腦不得互相以 Copy 整個專案資料夾作為日常同步方式。

唯一正式程式碼來源：

``` text
GitHub
└── Iishvarii/FamilySafety
```

工作原則：

``` text
Pull
    ↓
Work
    ↓
Test
    ↓
Commit
    ↓
Push
```

開始工作：

``` bash
git pull
```

確認：

``` bash
git status
```

完成並測試後：

``` bash
git add .
git commit -m "Describe the change"
git push
```

換另一台電腦後，先：

``` bash
git pull
```

## 20.1 初學階段防衝突規則

避免：

``` text
電腦 A 修改但未 Push
        ↓
電腦 B 又修改同一檔案
        ↓
兩邊產生分叉 / Conflict
```

每次離開某台電腦前，若修改已完成且可正常執行，應 Commit + Push。

------------------------------------------------------------------------

# 21. ChatGPT / Codex / GitHub 的責任分工

``` text
ChatGPT Project
├── 討論
├── 教學
├── UI / UX 決策
├── 架構分析
└── 問題排查

PROJECT_SPEC.md
├── 已確認專案規格
├── 跨電腦 Shared Context
└── Codex 開始工作前的核心參考

GitHub
├── Kotlin
├── XML
├── Drawable
├── Gradle
├── docs
└── 正式版本控制

Codex
├── 讀取 Repository
├── 讀取 PROJECT_SPEC.md
├── 修改實際程式碼
├── 協助 Terminal / Git
└── 不作為唯一規格保存位置

Android Studio
├── Coding
├── Build
├── Run
└── Debug
```

Codex 開始大型修改前，應先參考：

``` text
docs/PROJECT_SPEC.md
```

以及實際 Repository 現況。

------------------------------------------------------------------------

# 22. 規格優先順序

若資訊互相衝突，優先順序：

``` text
1. 使用者最新明確確認的需求
2. PROJECT_SPEC.md 最新版本
3. Repository 目前實際程式碼
4. 舊 Chat / 舊 Mockup / 舊文件
```

但若最新需求會造成：

-   安全漏洞
-   資料模型矛盾
-   Android 架構明顯錯誤
-   UI 無法實作
-   API / DB 不一致

應先指出問題與影響，再修改正式規格。

------------------------------------------------------------------------

# 23. 尚未完全定案（Do Not Assume）

下列內容尚未完全定案時，Codex / ChatGPT 不應自行猜測：

-   完整 MariaDB Schema
-   所有 REST API Endpoint
-   API Request / Response JSON Schema
-   Push Notification Provider 與完整實作
-   Authentication Token 機制
-   Role / Permission 完整資料模型
-   即時影像串流協定
-   歷史溫濕度圖表 Library
-   Server Alert State Machine 的完整 Database Schema

需要實作上述項目時，先確認現有 Repository / Server 規格。

------------------------------------------------------------------------

# 24. 專案規格更新紀錄

## 2026-10-03

建立第一版 `PROJECT_SPEC.md`，整合：

-   Android App Architecture
-   Home
-   Environment
-   Safety Countdown
-   Alert Center
-   Task Center
-   Management
-   Forgot Password
-   Push Notification
-   MariaDB / REST API 基本架構
-   Android Drawable / Transparent PNG 規格
-   UI Color / Localization 原則
-   GitHub 三台電腦同步規則
-   ChatGPT / Codex / GitHub / Android Studio 分工

------------------------------------------------------------------------

## 最重要的專案原則摘要

``` text
家庭安全管理系統

IoT
 ↓
Server
 ↓
MariaDB
 ↓
REST API
 ↓
Android

Android：
XML + Kotlin + ViewBinding

Architecture：
Fragment
→ ViewModel
→ Repository
→ Retrofit / ApiService

Safety：
Gas ON + No Flame
→ Immediate Risk Alert

Flame + No Person
→ Safety Countdown
→ Timeout + Still No Person
→ Alert

三台電腦：
Notebook
學校桌機
家裡桌機
     ↓
全部透過 GitHub 同步

開發：
Pull → Work → Test → Commit → Push

專案共同規格：
docs/PROJECT_SPEC.md
```
