package com.example.familysafety

/** Mock records only. No sensor, database, or server connection yet. */
object MockEnvironmentAlertDataSource {
    private var sharedAlerts = create().toList()
    private var nextId = 4L

    fun getAlerts(): List<EnvironmentAlert> = sharedAlerts.toList()

    fun addAlert(type: EnvironmentAlertType, location: String = "Kitchen",
                 timestamp: Long = System.currentTimeMillis()): EnvironmentAlert {
        val alert = EnvironmentAlert(nextId++, type, EnvironmentAlertStatus.PENDING, location, timestamp)
        sharedAlerts = sharedAlerts + alert
        return alert
    }

    // Environment status is sensor-authoritative; history is never removed or manually resolved.
    internal fun autoResolve(sensor: EnvironmentSensorState) {
        sharedAlerts = sharedAlerts.map { alert ->
            if (alert.status == EnvironmentAlertStatus.PENDING &&
                EnvironmentSafetyEvaluator.shouldResolve(alert.type, sensor)) {
                alert.copy(status = EnvironmentAlertStatus.RESOLVED)
            } else alert
        }
    }

    internal fun resetForTests() { sharedAlerts = create().toList(); nextId = 4L }

    // Independent fixture factory retained for existing isolated unit tests.
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
