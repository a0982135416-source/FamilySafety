package com.example.familysafety

import java.util.Locale

/** STEP 7：僅供展示的 process-memory credentials，成員資料仍以 MockMemberDataSource 為準。 */
object MockAuthDataSource {
    const val INITIAL_PASSWORD = "Mock123"
    const val RESET_CODE = "123456"
    private val passwords = mutableMapOf(1 to INITIAL_PASSWORD, 2 to INITIAL_PASSWORD, 3 to INITIAL_PASSWORD)
    private val verifiedEmails = mutableSetOf<String>()
    private var currentMemberId: Int? = null

    fun getCurrentMember(): Member? = MockMemberDataSource.getMembers().find { it.id == currentMemberId }
    fun canManage(): Boolean = getCurrentMember()?.role == MemberRole.ADMIN
    fun logout() { currentMemberId = null; verifiedEmails.clear() }

    private fun normalized(email: String) = email.trim().lowercase(Locale.ROOT)
    private fun memberForEmail(email: String) = MockMemberDataSource.getMembers()
        .find { normalized(it.email) == normalized(email) }

    fun login(account: String, password: String): Member? {
        if (account.isBlank() || password.isBlank()) return null
        val member = MockMemberDataSource.getMembers().find { it.account == account.trim() } ?: return null
        return member.takeIf { passwords[it.id] == password }?.also { currentMemberId = it.id }
    }

    fun emailExists(email: String) = email.isNotBlank() && memberForEmail(email) != null

    fun beginPasswordReset(email: String): Boolean {
        verifiedEmails.remove(normalized(email))
        return emailExists(email)
    }

    fun verifyResetCode(email: String, code: String): Boolean {
        val key = normalized(email)
        verifiedEmails.remove(key)
        return (emailExists(email) && code == RESET_CODE).also { if (it) verifiedEmails.add(key) }
    }

    fun isResetVerified(email: String) = emailExists(email) && normalized(email) in verifiedEmails

    /** 驗證授權只在成功重設時消耗，規則錯誤不會改動密碼。 */
    fun resetPassword(email: String, newPassword: String): Boolean {
        val member = memberForEmail(email) ?: return false
        if (!isResetVerified(email) || validatePassword(newPassword, newPassword) != null) return false
        passwords[member.id] = newPassword
        verifiedEmails.remove(normalized(email))
        return true
    }

    internal fun resetForTests() {
        passwords.clear()
        passwords.putAll(mapOf(1 to INITIAL_PASSWORD, 2 to INITIAL_PASSWORD, 3 to INITIAL_PASSWORD))
        verifiedEmails.clear()
        currentMemberId = null
    }
}

enum class PasswordValidation { REQUIRED, RULE, MISMATCH }

/** STEP 7-FIX：6–12 字元，至少一個英文字母與一個數字，確認密碼一致。 */
fun validatePassword(password: String, confirmation: String): PasswordValidation? = when {
    password.isBlank() || confirmation.isBlank() -> PasswordValidation.REQUIRED
    password.length !in 6..12 || password.none { it in 'A'..'Z' || it in 'a'..'z' } ||
        password.none { it in '0'..'9' } -> PasswordValidation.RULE
    password != confirmation -> PasswordValidation.MISMATCH
    else -> null
}
