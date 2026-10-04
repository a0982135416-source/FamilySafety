package com.example.familysafety

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.core.view.doOnLayout
import com.google.android.material.textfield.TextInputLayout

/** Auth 共用的 Insets、必填驗證與返回 Login；不改動主頁導覽。 */
internal fun AppCompatActivity.setupAuthInsets(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val top = root.paddingTop
    val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        // Edge-to-edge：IME 已包含底部導覽區，不與 navigation bar 重複相加。
        view.updatePadding(top = top + bars.top, bottom = bottom + maxOf(bars.bottom, ime.bottom))
        if (insets.isVisible(WindowInsetsCompat.Type.ime())) view.doOnLayout {
            it.findFocus()?.let { focused ->
                focused.requestRectangleOnScreen(Rect(0, 0, focused.width, focused.height), false)
            }
        }
        insets
    }
    ViewCompat.requestApplyInsets(root)
}

internal fun AppCompatActivity.requiredAuthFields(vararg fields: EditText): Boolean {
    var valid = true
    fields.forEach { field ->
        field.setAuthError(if (field.text.isNullOrBlank()) getString(R.string.management_required).also { valid = false } else null)
    }
    return valid
}

/** 密碼錯誤由 TextInputLayout 顯示，避免 EditText 原生錯誤圖示與眼睛重疊。 */
internal fun EditText.setAuthError(message: String?) {
    val layout = generateSequence(parent as? View) { it.parent as? View }
        .filterIsInstance<TextInputLayout>().firstOrNull()
    if (layout != null && layout.endIconMode == TextInputLayout.END_ICON_PASSWORD_TOGGLE) {
        error = null
        layout.error = message
    } else error = message
}

internal object AuthNavigation {
    const val EXTRA_EMAIL = "auth.email"
    const val EXTRA_RESET_SUCCESS = "auth.reset_success"

    fun returnToLogin(activity: AppCompatActivity, success: Boolean = false) {
        activity.startActivity(Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_RESET_SUCCESS, success)
        })
        activity.finish()
    }
}
