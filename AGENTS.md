# 家庭安全管理系統開發規範

## 01. 技術與架構
- Kotlin、XML Layout、ViewBinding；不使用 Jetpack Compose。
- 主要介面採 Single MainActivity + Fragments；MainActivity 管理 FragmentContainerView 與 BottomNavigationView。保留既有 Authentication 資源，不藉命名作業改動架構。
- 未來資料流：Fragment → ViewModel → Repository → Retrofit / ApiService → REST API → Raspberry Pi Server → MariaDB。

## 02. 命名
- View ID：`元件類型_頁面_用途`，例如 `textView_home_title`、`horizontalScrollView_alert_filters`、`cardView_alert_item`。
- 使用者的「全部 snake_case」與其範例有大小寫差異；本次提案依具體範例使用元件類型 lowerCamelCase，頁面與用途使用英文 snake_case，待使用者確認，不擅自將既有合規 ID 全部小寫化。
- 頁面名稱：main、login、forgot、verify、reset、home、environment、alert、task、management；不另創 h、a、env 等縮寫。
- 元件類型以實際 XML 為準。MaterialButton 使用 button，MaterialCardView 使用 cardView，TextInputEditText 提案使用 editText；自訂元件、共用 ID、Menu / Navigation ID 個別審查。
- Kotlin Class 使用 PascalCase；Function / Variable 使用 camelCase；Constant 使用 UPPER_SNAKE_CASE。不為 View ID 作業重新命名無關 Kotlin 符號。

## 03. 字串與 UI
- 新增使用者可見文字放入 res/values/strings.xml 與 res/values-zh-rTW/strings.xml，兩種語系 Key 一致，不新增硬編碼介面文字。
- 優先沿用既有 Drawable、Color、Style、Class、View ID，不建立重複資源。
- 色票：Background #F2FAFF、Primary Blue #1687E8、Dark Blue #0B3D75、Light Blue #E8F5FF、Secondary Text #607D9D、Safe Green #19B394、Warning Red #F0525C、Card #FFFFFF；Task Alert 保留現有紫色系。
- 命名作業不得改動外觀、文字、Mock Data、資料模型、感測器判斷、安全倒數、地點切換或圖表資料。
- 保留 Alert Header WindowInsets、Dialog Lifecycle、分類／篩選／RecyclerView、Filter 自動水平捲動、Overdue 完整顯示與 BottomNavigation。
- GAS_LEAK_RISK：瓦斯 ON + 火焰 NO，立即警報。UNATTENDED_COOKING：瓦斯 ON + 火焰 YES + 無人，倒數結束仍符合才正式警報。Mock Resolve 不等於 Server 已驗證安全。

## 04. 修改與驗證
- 先讀取現有程式，只修改當次 STEP 範圍，保留清楚的大項註解與原有功能；不新增未要求的功能或開始下一 STEP。
- XML ID 修改須同步檢查 ViewBinding、R.id、findViewById、Constraint / RelativeLayout / Behavior / Accessibility / Transition 參照、Menu / Navigation 與測試。不得編輯產生的 Binding 程式、建立重複 ID 或移除邏輯規避編譯錯誤。
- 每次修改後執行 Build；每個重新命名 Batch 完成後執行 `.\gradlew.bat :app:assembleDebug --console=plain`。失敗先停止並修正，不繼續下一 Batch。
- 核准 Batch 全部完成後執行 `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain`；Emulator 可用時執行 `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain`。
- 回報實際修改檔案、原因、指令與輸出、測試數量，以繁體中文集中摘要。未執行標示 NOT TESTED；靜態檢查不能宣稱 Emulator 實測，舊報告不能冒充本次結果。

## 05. STEP 4-6 安全閘門
- 首次僅檢查備份、建立規範、盤點 ID、輸出完整提案；等待使用者確認盤點結果與備份方式才可開始 Batch 1。
- 目前未建立 Git Repository，尚無可確認的 STEP 4-5 Stable Checkpoint。不得自行初始化 Git 或開始大量重新命名。
- 不將 local.properties、密碼、Token、API Key、Keystore 等機密加入版本控制。檢查忽略規則及待納入內容後才建立還原點；不覆蓋或丟棄使用者變更，不擅自 Force Push 或改寫歷史。
- 核准後順序：Home → Environment → Alert → Task → Management → Main / Authentication。每批完成 Build 才進下一批。
