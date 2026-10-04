package com.example.familysafety

/** STEP 6-2：僅供 Mock 管理，不代表 Server 已驗證權限。 */
data class Member(val id: Int, val name: String, val account: String,
                  val email: String, val role: MemberRole)

enum class MemberRole { ADMIN, MEMBER }
