package com.example.familysafety

/** Mock records only. No sensor, database, or server connection yet. */
object MockEnvironmentAlertDataSource {
    fun create(): MutableList<EnvironmentAlert> {
        val now = System.currentTimeMillis()
        return mutableListOf(
            EnvironmentAlert(1L, EnvironmentAlertType.GAS_LEAK_RISK,
                EnvironmentAlertStatus.PENDING, "Kitchen", now - 30L * 60_000L),
            EnvironmentAlert(2L, EnvironmentAlertType.UNATTENDED_COOKING,
                EnvironmentAlertStatus.PENDING, "Kitchen", now - 90L * 60_000L),
            EnvironmentAlert(3L, EnvironmentAlertType.UNATTENDED_COOKING,
                EnvironmentAlertStatus.RESOLVED, "Kitchen", now - 24L * 60L * 60_000L)
        )
    }
}
