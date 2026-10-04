package com.example.familysafety

import android.os.SystemClock

/** Deadline is authoritative; screens only request reconciliation and render snapshots. */
object MockEnvironmentDataSource {
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
        if (minutes !in 1..180 || phase == EnvironmentCountdownPhase.RUNNING) return false
        configuredMinutes = minutes
        if (phase != EnvironmentCountdownPhase.CANCELLED) phase = EnvironmentCountdownPhase.IDLE
        return true
    }

    fun startManualCountdown(): Boolean {
        refresh()
        if (phase == EnvironmentCountdownPhase.RUNNING) return false
        start(clock())
        return true
    }

    fun cancelCountdown() {
        // Keep cancellation stable while the same unattended sensor condition persists.
        unattendedCycleConsumed = true
        resetCountdown()
        phase = EnvironmentCountdownPhase.CANCELLED
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
                phase == EnvironmentCountdownPhase.FINISHED ||
                phase == EnvironmentCountdownPhase.CANCELLED) resetCountdown()
        }

        if (phase == EnvironmentCountdownPhase.RUNNING && now >= deadline!!) {
            phase = EnvironmentCountdownPhase.FINISHED
            deadline = null
            unattendedCycleConsumed = true
            if (EnvironmentSafetyEvaluator.expiryEligible(sensorState)) {
                MockEnvironmentAlertDataSource.addAlert(EnvironmentAlertType.UNATTENDED_COOKING)
            }
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
            EnvironmentCountdownPhase.IDLE, EnvironmentCountdownPhase.CANCELLED -> configuredMinutes * 60_000L
            EnvironmentCountdownPhase.RUNNING -> (deadline!! - clock()).coerceAtLeast(0L)
            EnvironmentCountdownPhase.FINISHED -> 0L
        }
        return EnvironmentCountdownState(configuredMinutes, phase, deadline, remaining)
    }

    /** Test-only control; production settings remain integer minutes, 1..180. */
    internal fun resetForTests(testClock: () -> Long = { SystemClock.elapsedRealtime() }) {
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
