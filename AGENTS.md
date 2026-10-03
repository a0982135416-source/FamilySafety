# 家庭安全管理系統開發規範

## 01. 技術與架構
- Kotlin、XML Layout、ViewBinding；不使用 Jetpack Compose。
- 主要介面採 Single MainActivity + Fragments；MainActivity 管理 FragmentContainerView 與 BottomNavigationView。保留既有 Authentication 資源，不藉命名作業改動架構。
- 未來資料流：Fragment → ViewModel → Repository → Retrofit / ApiService → REST API → Raspberry Pi Server → MariaDB。

## 02. 命名
- View ID：`元件類型_頁面_用途`，例如 `textView_home_title`、`horizontalScrollView_alert_filters`、`cardView_alert_item`。
- 依既有已核准 STEP 4-6 提案與 Inventory，元件類型使用 lowerCamelCase，頁面與用途使用英文 snake_case；不擅自將既有合規 ID 全部小寫化。
- STEP 4-6 View ID 命名以既有已核准 Inventory 與本文件的 `元件類型_頁面_用途` 規則為準。PROJECT_SPEC.md 中的 btnLogin、rvTodayTasks 等舊式 ID 視為架構／功能示例，不得據此將已完成的 ID 改回舊格式。
- 頁面名稱：main、login、forgot、verify、reset、home、environment、alert、task、management；不另創 h、a、env 等縮寫。
- 元件類型以實際 XML 為準。MaterialButton 使用 button，MaterialCardView 使用 cardView，TextInputEditText 依既有已核准提案使用 editText；Batch 5～6 仍依既有 Inventory 執行，自訂元件、共用 ID、Menu / Navigation ID 等特殊 ID 必須個別審查。
- Main / BottomNavigation / Menu IDs（fragmentContainer、bottomNavigation、navHome、navEnvironment、navAlert、navTask、navManagement）屬於 Batch 6 / Special Review，不得因一般 View ID 規則自動重新命名；實際處置必須依既有 Inventory。
- Kotlin Class 使用 PascalCase；Function / Variable 使用 camelCase；Constant 使用 UPPER_SNAKE_CASE。不為 View ID 作業重新命名無關 Kotlin 符號。

## 03. 字串與 UI
- 新增使用者可見文字放入 res/values/strings.xml 與 res/values-zh-rTW/strings.xml，兩種語系 Key 一致，不新增硬編碼介面文字。
- 優先沿用既有 Drawable、Color、Style、Class、View ID，不建立重複資源。
- 色票：Background #F2FAFF、Primary Blue #1687E8、Dark Blue #0B3D75、Light Blue #E8F5FF、Secondary Text #607D9D、Safe Green #19B394、Warning Red #F0525C、Card #FFFFFF；Task Alert 保留現有紫色系。
- 命名作業不得改動外觀、文字、Mock Data、資料模型、感測器判斷、安全倒數、地點切換或圖表資料。
- 保留 Alert Header WindowInsets、Dialog Lifecycle、分類／篩選／RecyclerView、Filter 自動水平捲動、Overdue 完整顯示與 BottomNavigation。
- GAS_LEAK_RISK：Gas ON + Flame NOT detected，立即警示，不依賴安全倒數。
- 無人烹煮安全倒數：Flame detected + Person NOT detected，自動啟動安全倒數；Gas ON 不是必要條件。
- 倒數期間 Person detected，取消／重置安全倒數。
- Countdown expires 且 Person NOT detected，產生正式 UNATTENDED_COOKING 警報；到期判斷不得額外加入 Gas ON 或 Flame detected 作為必要條件。
- 安全倒數預設 10 分鐘，使用者可自由輸入整數分鐘數，例如 18 分鐘；不使用固定 5 / 10 / 15 / 30 分鐘選項。
- Mock Resolve 不等於 Server 已驗證安全。

## 04. 修改與驗證
- 先讀取現有程式，只修改當次 STEP 範圍，保留清楚的大項註解與原有功能；不新增未要求的功能或開始下一 STEP。
- XML ID 修改須同步檢查 ViewBinding、R.id、findViewById、Constraint / RelativeLayout / Behavior / Accessibility / Transition 參照、Menu / Navigation 與測試。不得編輯產生的 Binding 程式、建立重複 ID 或移除邏輯規避編譯錯誤。
- Android Source / Resource 修改必須執行相應 Build / Tests；每個重新命名 Batch 完成後執行 `.\gradlew.bat :app:assembleDebug --console=plain`。失敗先停止並修正，不繼續下一 Batch。
- 純 Markdown / 規格文件修改不需要為文件本身執行 Android Build，但必須檢查 Git diff、`git diff --check` 與文件一致性；Build / Tests 標示 NOT REQUIRED。
- 核准 Batch 全部完成後執行 `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain`；Emulator 可用時執行 `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain`。
- 回報實際修改檔案、原因、指令與輸出、測試數量，以繁體中文集中摘要。未執行標示 NOT TESTED；靜態檢查不能宣稱 Emulator 實測，舊報告不能冒充本次結果。

## 05. STEP 4-6 安全閘門
- 既有已核准 STEP 4-6 提案與 Inventory 是 Batch 1～4 的執行依據；已完成首次作業閘門，不重新要求 Batch 1 前的首次確認、不重新開始 Batch 1、不重做全專案 Inventory。
- 已完成 Batch 不得因後續工作重新 Rename、重新執行 Inventory 或修改 Home / Environment / Alert / Task，除非使用者明確授權修正 regression。
- 專案已是 Git Repository，正式 branch 為 main。STEP 4-6 Batch 4 完成 checkpoint 為 e20e053（STEP 4-6 Batch 4 Task）；checkpoint 不等於目前 HEAD。
- 本次規格同步修正開始時 HEAD 為 779237e（Add FamilySafety project specification）；此為歷史基準，未來不得將 HEAD hash 永久視為固定值。每次開始工作應唯讀確認 git status、branch 與 HEAD。
- 禁止重新初始化 Git、擅自 reset、擅自 checkout 舊 checkpoint 覆蓋目前 HEAD、擅自 rebase 或 amend；不得開始未授權的重新命名。
- 不將 local.properties、密碼、Token、API Key、Keystore 等機密加入版本控制。檢查忽略規則及待納入內容後才建立還原點；不覆蓋或丟棄使用者變更，不擅自 Force Push 或改寫歷史。
- 原始順序：Home → Environment → Alert → Task → Management → Main / Authentication。
- Batch 1 Home：COMPLETED，commit c8e4eff。
- Batch 2 Environment：COMPLETED，commit aff1452。
- Batch 3 Alert：COMPLETED，commit edeb9ad。
- Batch 4 Task：COMPLETED，commit e20e053。
- Batch 5 Management：NEXT，尚未執行。
- Batch 6 Main + Auth：PENDING，尚未執行。
- 每批須取得對應授權並完成 Build，才可進下一批；本次 AGENTS.md 文件修正不代表已授權開始 Batch 5。
