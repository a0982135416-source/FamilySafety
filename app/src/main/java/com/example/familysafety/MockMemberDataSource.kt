package com.example.familysafety

/** 同一 App process 共用；不寫入檔案或資料庫。 */
object MockMemberDataSource {
    private fun initial() = listOf(
        Member(1, "Alex", "alex", "alex@example.test", MemberRole.ADMIN),
        Member(2, "Jamie", "jamie", "jamie@example.test", MemberRole.MEMBER),
        Member(3, "Taylor", "taylor", "taylor@example.test", MemberRole.MEMBER),
    )
    private var members = initial()
    private var nextId = 4
    fun getMembers(): List<Member> = members.toList()

    fun addMember(name: String, account: String, email: String, role: MemberRole): Member? {
        if (!MockAuthDataSource.canManage() || listOf(name, account, email).any { it.isBlank() }) return null
        val member = Member(nextId++,
            name.trim(), account.trim(), email.trim(), role)
        members = members + member
        return member
    }

    // Stable ID preserves the credential/session association when account or email changes.
    fun updateMember(id: Int, name: String, account: String, email: String, role: MemberRole): Member? {
        if (!MockAuthDataSource.canManage() || listOf(name, account, email).any { it.isBlank() }) return null
        val old = members.find { it.id == id } ?: return null
        val updated = old.copy(name = name.trim(), account = account.trim(), email = email.trim(), role = role)
        members = members.map { if (it.id == id) updated else it }
        return updated
    }

    fun deleteMember(id: Int, getString: (Int) -> String): Boolean {
        if (!MockAuthDataSource.canManage() || MockAuthDataSource.getCurrentMember()?.id == id ||
            MockTaskDataSource.referencesMember(id, getString) || members.none { it.id == id }) return false
        members = members.filterNot { it.id == id }
        return true
    }

    internal fun resetForTests() { members = initial(); nextId = 4 }
}
