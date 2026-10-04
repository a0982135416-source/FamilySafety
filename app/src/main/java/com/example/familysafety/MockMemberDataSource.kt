package com.example.familysafety

/** 同一 App process 共用；不寫入檔案或資料庫。 */
object MockMemberDataSource {
    private fun initial() = listOf(
        Member(1, "Alex", "alex", "alex@example.test", MemberRole.ADMIN),
        Member(2, "Jamie", "jamie", "jamie@example.test", MemberRole.MEMBER),
        Member(3, "Taylor", "taylor", "taylor@example.test", MemberRole.MEMBER),
    )
    private var members = initial()
    fun getMembers(): List<Member> = members.toList()

    fun addMember(name: String, account: String, email: String, role: MemberRole): Member? {
        if (listOf(name, account, email).any { it.isBlank() }) return null
        val member = Member((members.maxOfOrNull { it.id } ?: 0) + 1,
            name.trim(), account.trim(), email.trim(), role)
        members = members + member
        return member
    }

    internal fun resetForTests() { members = initial() }
}
