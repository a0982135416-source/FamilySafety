package com.example.familysafety

/** STEP 4-2: UI-only environmental alert record. */
data class EnvironmentAlert(
    val id: Long,
    val type: EnvironmentAlertType,
    val status: EnvironmentAlertStatus,
    val location: String,
    val createdAt: Long
)

enum class EnvironmentAlertType {
    GAS_LEAK_RISK,
    UNATTENDED_COOKING
}

enum class EnvironmentAlertStatus {
    PENDING,
    RESOLVED
}
