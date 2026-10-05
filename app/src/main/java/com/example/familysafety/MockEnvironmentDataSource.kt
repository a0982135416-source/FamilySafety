package com.example.familysafety

import android.os.SystemClock

/** Deadline is authoritative; screens only request reconciliation and render snapshots. */
object MockEnvironmentDataSource {
    // Current readings and selection are shared by Environment and Home, not history data.
    data class EnvironmentMockData(val temperature: Double, val humidity: Int)
    private val locationMockData = mapOf(
        "KITCHEN" to EnvironmentMockData(26.8, 58),
        "LIVING_ROOM" to EnvironmentMockData(25.4, 61),
        "BEDROOM" to EnvironmentMockData(24.9, 55),
    )
    var selectedLocation = "KITCHEN"
        private set

    fun selectLocation(location: String): Boolean {
        if (location !in locationMockData) return false
        selectedLocation = location
        return true
    }

    fun getCurrentEnvironmentData(): EnvironmentMockData = locationMockData.getValue(selectedLocation)

    var sensorState = EnvironmentSensorState()
        private set
    private var configuredMinutes = 10
    private var phase = EnvironmentCountdownPhase.IDLE
    private var deadline: Long? = null
    private var gasRiskLatched = false
    private var unattendedCycleConsumed = false
    private var clock: () -> Long = { SystemClock.elapsedRealtime() }
    private var durationOverrideForTests: Long? = null

    // Mock stove controls enforce physical transitions; raw sensor input keeps safety rules independent.
    fun toggleMockGas() {
        val on = !sensorState.gasOn
        updateSensors(sensorState.copy(gasOn = on,
            flameDetected = on && sensorState.flameDetected))
    }

    fun toggleMockFlame(): Boolean {
        if (!sensorState.gasOn) return false
        updateSensors(sensorState.copy(flameDetected = !sensorState.flameDetected))
        return true
    }

    fun toggleMockPerson() = updateSensors(sensorState.copy(personDetected = !sensorState.personDetected))

    fun updateSensors(state: EnvironmentSensorState) {
        sensorState = state
        refresh()
    }

    fun configureMinutes(minutes: Int): Boolean {
        refresh()
        if (minutes !in 1..180) return false
        configuredMinutes = minutes
        // Changing duration never leaves active safety monitoring waiting for a manual Start.
        resetCountdown()
        if (!EnvironmentSafetyEvaluator.gasLeakRisk(sensorState) &&
            EnvironmentSafetyEvaluator.shouldStartCountdown(sensorState)) start(clock())
        return true
    }

    private fun start(now: Long) {
        phase = EnvironmentCountdownPhase.RUNNING
        deadline = now + (durationOverrideForTests ?: configuredMinutes * 60_000L)
    }

    private fun resetCountdown() {
        phase = EnvironmentCountdownPhase.IDLE
        deadline = null
    }

    fun refresh() {
        val now = clock()
        val risk = EnvironmentSafetyEvaluator.gasLeakRisk(sensorState)
        if (risk && !gasRiskLatched) {
            MockEnvironmentAlertDataSource.addAlert(EnvironmentAlertType.GAS_LEAK_RISK)
        }
        gasRiskLatched = risk

        // Gas leak takes priority over the pre-warning timer, including at the expiry boundary.
        if (risk && phase == EnvironmentCountdownPhase.RUNNING) {
            resetCountdown()
            unattendedCycleConsumed = false
        }

        if (sensorState.personDetected) {
            unattendedCycleConsumed = false
            if (EnvironmentSafetyEvaluator.shouldCancelCountdown(sensorState,
                    phase == EnvironmentCountdownPhase.RUNNING) ||
                phase == EnvironmentCountdownPhase.FINISHED) resetCountdown()
        }

        if (phase == EnvironmentCountdownPhase.RUNNING && now >= deadline!!) {
            phase = EnvironmentCountdownPhase.FINISHED
            deadline = null
            if (!unattendedCycleConsumed && EnvironmentSafetyEvaluator.expiryEligible(sensorState)) {
                MockEnvironmentAlertDataSource.addAlert(EnvironmentAlertType.UNATTENDED_COOKING)
            }
            unattendedCycleConsumed = true
        }

        if (phase == EnvironmentCountdownPhase.IDLE && !unattendedCycleConsumed &&
            EnvironmentSafetyEvaluator.shouldStartCountdown(sensorState)) start(now)

        MockEnvironmentAlertDataSource.autoResolve(sensorState)
    }

    fun getActiveWarning(): EnvironmentAlert? = MockEnvironmentAlertDataSource.getAlerts()
        .filter { it.status == EnvironmentAlertStatus.PENDING &&
            !EnvironmentSafetyEvaluator.shouldResolve(it.type, sensorState) }
        .sortedWith(compareBy<EnvironmentAlert> { if (it.type == EnvironmentAlertType.GAS_LEAK_RISK) 0 else 1 }
            .thenByDescending { it.id }).firstOrNull()

    fun getCountdown(): EnvironmentCountdownState {
        refresh()
        val remaining = when (phase) {
            EnvironmentCountdownPhase.IDLE -> configuredMinutes * 60_000L
            EnvironmentCountdownPhase.RUNNING -> (deadline!! - clock()).coerceAtLeast(0L)
            EnvironmentCountdownPhase.FINISHED -> 0L
        }
        return EnvironmentCountdownState(configuredMinutes, phase, deadline, remaining)
    }

    /** Reuse the safety engine's reconciled state, with active danger taking priority. */
    fun getSafetyStatus(): EnvironmentSafetyStatus {
        val countdown = getCountdown()
        return when {
            getActiveWarning() != null -> EnvironmentSafetyStatus.DANGER
            countdown.phase == EnvironmentCountdownPhase.RUNNING -> EnvironmentSafetyStatus.WARNING
            else -> EnvironmentSafetyStatus.SAFE
        }
    }

    /** Test-only control; production settings remain integer minutes, 1..180. */
    internal fun resetForTests(testClock: () -> Long = { SystemClock.elapsedRealtime() }) {
        selectedLocation = "KITCHEN"
        sensorState = EnvironmentSensorState()
        configuredMinutes = 10
        phase = EnvironmentCountdownPhase.IDLE
        deadline = null
        gasRiskLatched = false
        unattendedCycleConsumed = false
        clock = testClock
        durationOverrideForTests = null
    }

    /** Instrumented real-clock coverage only; never called by production UI. */
    internal fun setDurationOverrideForTests(durationMillis: Long) {
        require(durationMillis > 0)
        check(phase != EnvironmentCountdownPhase.RUNNING)
        durationOverrideForTests = durationMillis
    }

    internal fun setDeadlineForTests(deadlineElapsed: Long) {
        check(phase == EnvironmentCountdownPhase.RUNNING)
        deadline = deadlineElapsed
    }
}
