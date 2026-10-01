# STEP 4-6 第一階段驗證紀錄

執行日期：2026-10-01（Asia/Taipei）。本次只有規範與盤點文件，尚未開始任何 View ID 重新命名 Batch。

## 實際 Build

在專案根目錄執行：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug --console=plain
```

首次執行因 sandbox 無法存取使用者 Gradle Wrapper 快取鎖而中止，Exit code 1：

```text
java.nio.file.AccessDeniedException:
C:\Users\Lydia\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1-bin.zip.lck
```

這是執行環境存取權限問題，尚未進入 Android 編譯，不是 View ID 錯誤。取得所需執行權限後以相同指令重試，Exit code 0，實際輸出摘要：

```text
> Task :app:dataBindingGenBaseClassesDebug UP-TO-DATE
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:compileDebugJavaWithJavac UP-TO-DATE
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:processDebugManifestForPackage
> Task :app:processDebugResources
> Task :app:dexBuilderDebug
> Task :app:packageDebug
> Task :app:assembleDebug
> Task :app:createDebugApkListingFileRedirect

BUILD SUCCESSFUL in 7s
37 actionable tasks: 7 executed, 30 up-to-date
Configuration cache entry stored.
```

`:app:assembleDebug` 成功。這是文件階段對目前原始程式的增量 Build，不是 Clean Build，也不是 ID 重新命名後的驗收。

## Unit / Emulator

| 項目 | 本次結果 | 實際執行數 | Passed / Failed / Errors / Skipped |
|---|---|---:|---|
| :app:testDebugUnitTest | NOT TESTED | 0 | N/A / N/A / N/A / N/A |
| TaskAlertTest | NOT TESTED | 0 | N/A / N/A / N/A / N/A |
| :app:connectedDebugAndroidTest | NOT TESTED | 0 | N/A / N/A / N/A / N/A |

依本次首次執行僅完成第 1–4 項的要求，未執行重新命名批次及完整測試。沒有把既有測試 XML 報告當成此次執行結果，也沒有宣稱此次進行 Emulator 操作驗證。

## 靜態盤點

- 13 個 Layout、1 個 Menu：235 個專案 ID 宣告，229 個不同名稱；另列 3 個 Framework ID 引用。
- 34 個合規、184 個建議修改、17 個特殊待確認。17 個包含 11 個跨 Layout 共用名稱的宣告、1 個自訂圖表 ID、5 個選單 ID。
- 全部 230 個 Layout View ID 對應的現有 Binding 欄位均已與 generated Java 交叉核對；未編輯 generated Java。
- 提案 ID 無重複；本次實際修改 ID 為 0。後續新 Binding 欄位仍須在各 Batch 編譯確認。
- 沒有建立 Git Repository 或可確認的還原點。須待使用者確認盤點與備份方式才開始 Batch 1。
