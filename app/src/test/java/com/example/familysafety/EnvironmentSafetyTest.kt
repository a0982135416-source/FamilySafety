package com.example.familysafety

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EnvironmentSafetyTest {
    private var now = 1_000L
    private val source = MockEnvironmentDataSource
    private val alerts = MockEnvironmentAlertDataSource
    private fun added(type: EnvironmentAlertType) = alerts.getAlerts().drop(3).count { it.type == type }
    private fun sensors(gas: Boolean = false, flame: Boolean = false, person: Boolean = false) =
        source.updateSensors(EnvironmentSensorState(gas, flame, person))
    private fun expire() { now += 600_001L; source.refresh() }
    @Before fun reset() {
        now = 1_000L
        alerts.resetForTests()
        source.resetForTests { now }
    }

    @Test fun safeInitialSensorsProduceNoAlertOrCountdown() {
        sensors()
        assertEquals(0, added(EnvironmentAlertType.GAS_LEAK_RISK))
        assertEquals(EnvironmentCountdownPhase.IDLE, source.getCountdown().phase)
    }
    @Test fun gasOnWithoutFlameProducesImmediateAlertIndependentOfPerson() {
        sensors(gas = true, person = true)
        val event = alerts.getAlerts().last()
        assertEquals(EnvironmentAlertType.GAS_LEAK_RISK, event.type)
        assertEquals(EnvironmentAlertStatus.PENDING, event.status)
        assertEquals("Kitchen", event.location)
        assertTrue(event.createdAt > 0)
        assertEquals(EnvironmentCountdownPhase.IDLE, source.getCountdown().phase)
    }
    @Test fun gasOnWithFlameProducesNoGasLeak() {
        sensors(gas = true, flame = true, person = true)
        assertEquals(0, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }
    @Test fun repeatedGasRiskEvaluationProducesOnlyOneEvent() {
        repeat(5) { sensors(gas = true); source.refresh() }
        assertEquals(1, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }
    @Test fun gasOffThenRiskAllowsNewEvent() {
        sensors(gas = true); sensors(); sensors(gas = true)
        assertEquals(2, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }
    @Test fun flameRestoresSafetyAndResetsGasLatch() {
        sensors(gas = true, person = true)
        sensors(gas = true, flame = true, person = true)
        sensors(gas = true, person = true)
        assertEquals(2, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }
    @Test fun flameAndAbsentPersonStartAutoCountdown() {
        sensors(flame = true)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
    }
    @Test fun flameWithPersonDoesNotStartCountdown() {
        sensors(flame = true, person = true)
        assertEquals(EnvironmentCountdownPhase.IDLE, source.getCountdown().phase)
    }
    @Test fun autoCountdownDoesNotRequireGasOn() {
        sensors(gas = false, flame = true)
        assertFalse(source.sensorState.gasOn)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
    }
    @Test fun repeatedUpdatesDoNotExtendRunningDeadline() {
        sensors(flame = true)
        val deadline = source.getCountdown().deadlineElapsed
        now += 1000L
        sensors(flame = true); source.refresh()
        assertEquals(deadline, source.getCountdown().deadlineElapsed)
        assertEquals(599_000L, source.getCountdown().remainingMillis)
    }
    @Test fun personReturnsCancelsAndResetsWithoutAlert() {
        sensors(flame = true); now += 5000L; sensors(flame = true, person = true)
        assertEquals(EnvironmentCountdownPhase.IDLE, source.getCountdown().phase)
        assertEquals(600_000L, source.getCountdown().remainingMillis)
        assertNull(source.getCountdown().deadlineElapsed)
        expire(); assertEquals(0, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }
    @Test fun flameDisappearingPreservesRunningDeadline() {
        sensors(flame = true)
        val before = source.getCountdown().deadlineElapsed
        sensors(flame = false)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
        assertEquals(before, source.getCountdown().deadlineElapsed)
    }
    @Test fun expiryWithAbsentPersonCreatesUnattendedCooking() {
        sensors(flame = true); expire()
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
        assertEquals(EnvironmentCountdownPhase.FINISHED, source.getCountdown().phase)
        assertEquals(0L, source.getCountdown().remainingMillis)
    }
    @Test fun expiryDoesNotRequireGasOn() {
        sensors(flame = true); expire()
        assertFalse(source.sensorState.gasOn)
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }
    @Test fun expiryDoesNotRequireFlameDetected() {
        sensors(flame = true); sensors(); expire()
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }
    @Test fun manualCountdownWithPersonPresentNeverCreatesUnattendedAlert() {
        sensors(person = true)
        assertTrue(source.startManualCountdown())
        expire()
        assertEquals(0, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }
    @Test fun repeatedExpiryEvaluationAndReentryDoNotDuplicateOrRestartCycle() {
        sensors(flame = true); expire()
        repeat(5) { source.refresh(); source.getCountdown(); sensors(flame = true) }
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
        assertEquals(EnvironmentCountdownPhase.FINISHED, source.getCountdown().phase)
    }
    @Test fun personReturnAllowsNewUnattendedCycle() {
        sensors(flame = true); expire()
        sensors(flame = true, person = true); sensors(flame = true)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
        expire(); assertEquals(2, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }
    @Test fun autoResolveIsSharedAndHistorySurvives() {
        sensors(gas = true)
        val id = alerts.getAlerts().last().id
        assertEquals(EnvironmentAlertStatus.PENDING, alerts.getAlerts().last().status)
        sensors()
        source.refresh()
        assertEquals(EnvironmentAlertStatus.RESOLVED, alerts.getAlerts().single { it.id == id }.status)
        assertEquals(1, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }

    @Test fun flameLostWithGasOnEscalatesBeforeExpiry() {
        sensors(gas = true, flame = true)
        val before = source.getCountdown().deadlineElapsed!!
        sensors(gas = true)
        assertEquals(EnvironmentCountdownPhase.IDLE, source.getCountdown().phase)
        assertNull(source.getCountdown().deadlineElapsed)
        assertEquals(EnvironmentAlertType.GAS_LEAK_RISK, source.getActiveWarning()!!.type)
        now = before + 1
        source.refresh()
        assertEquals(0, added(EnvironmentAlertType.UNATTENDED_COOKING))
        assertEquals(1, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }

    @Test fun gasEscalationWinsAtDeadlineBoundaryAndNewCookingCanStart() {
        sensors(gas = true, flame = true)
        now = source.getCountdown().deadlineElapsed!!
        sensors(gas = true)
        assertEquals(0, added(EnvironmentAlertType.UNATTENDED_COOKING))
        sensors(gas = true, flame = true)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
        assertEquals(EnvironmentAlertStatus.RESOLVED, alerts.getAlerts().last().status)
    }

    @Test fun unattendedAutoResolveRequiresPersonOrBothGasOffAndNoFlame() {
        sensors(gas = true, flame = true); expire()
        val id = alerts.getAlerts().last().id
        sensors(gas = true, flame = false)
        assertEquals(EnvironmentAlertStatus.PENDING, alerts.getAlerts().single { it.id == id }.status)
        sensors(gas = false, flame = true)
        assertEquals(EnvironmentAlertStatus.PENDING, alerts.getAlerts().single { it.id == id }.status)
        sensors()
        assertEquals(EnvironmentAlertStatus.RESOLVED, alerts.getAlerts().single { it.id == id }.status)
    }

    @Test fun personResolvesUnattendedButNeverGasLeak() {
        sensors(gas = true, flame = true); expire()
        val id = alerts.getAlerts().last().id
        sensors(gas = true, person = true)
        assertEquals(EnvironmentAlertStatus.RESOLVED, alerts.getAlerts().single { it.id == id }.status)
        assertEquals(EnvironmentAlertType.GAS_LEAK_RISK, source.getActiveWarning()!!.type)
    }

    @Test fun remindersAcknowledgeWithoutNewRecordsAndSafeThenDangerAllowsNewId() {
        sensors(gas = true)
        val event = source.getActiveWarning()!!
        val reminder = EnvironmentWarningReminder()
        assertTrue(reminder.shouldWarn(event.id, now))
        reminder.acknowledge(now, 10_000L)
        repeat(3) { source.refresh(); assertFalse(reminder.shouldWarn(event.id, now + 9_999L)) }
        assertTrue(reminder.shouldWarn(event.id, now + 10_000L))
        assertEquals(1, added(EnvironmentAlertType.GAS_LEAK_RISK))
        assertEquals(EnvironmentAlertStatus.PENDING, alerts.getAlerts().last().status)
        sensors(); assertNull(source.getActiveWarning())
        assertFalse(reminder.shouldWarn(null, now + 20_000L))
        sensors(gas = true)
        assertNotEquals(event.id, source.getActiveWarning()!!.id)
        assertEquals(2, added(EnvironmentAlertType.GAS_LEAK_RISK))
    }

    @Test fun warningResumeAndEscalationAreImmediatelyEligible() {
        val reminder = EnvironmentWarningReminder()
        assertTrue(reminder.shouldWarn(4L, now))
        reminder.acknowledge(now, 10_000L)
        assertTrue(reminder.shouldWarn(5L, now))
        reminder.acknowledge(now, 10_000L)
        reminder.reset()
        assertTrue(reminder.shouldWarn(5L, now))
        assertEquals(10_000L, EnvironmentWarningController.REMINDER_INTERVAL_MILLIS)
    }
    @Test fun alertIdsAreUniqueAcrossFixtureAndEvents() {
        sensors(gas = true); sensors(flame = true); expire(); sensors(gas = true)
        val all = alerts.getAlerts()
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.drop(3).all { it.id > 3L })
    }
    @Test fun arbitraryEighteenMinutesAndManualCancelReset() {
        assertTrue(source.configureMinutes(18))
        assertEquals(1_080_000L, source.getCountdown().remainingMillis)
        assertTrue(source.startManualCountdown())
        assertFalse(source.configureMinutes(10))
        source.cancelCountdown()
        assertEquals(EnvironmentCountdownPhase.CANCELLED, source.getCountdown().phase)
        assertEquals(1_080_000L, source.getCountdown().remainingMillis)
    }
    @Test fun defaultIsTenMinutes() {
        assertEquals(10, source.getCountdown().configuredMinutes)
        assertEquals(600_000L, source.getCountdown().remainingMillis)
    }
    @Test fun validAndInvalidMinuteBoundaries() {
        listOf(-1, 0, 181, Int.MAX_VALUE).forEach { assertFalse(source.configureMinutes(it)) }
        assertEquals(10, source.getCountdown().configuredMinutes)
        assertTrue(source.configureMinutes(1)); assertTrue(source.configureMinutes(180))
    }
    @Test fun expiryIsReconciledAfterNoScreenRefresh() {
        sensors(flame = true)
        now += 600_001L
        assertEquals(0, added(EnvironmentAlertType.UNATTENDED_COOKING))
        source.getCountdown()
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }
    @Test fun manualCancelDoesNotImmediatelyAutoRestart() {
        sensors(flame = true); source.cancelCountdown(); source.refresh()
        assertEquals(EnvironmentCountdownPhase.CANCELLED, source.getCountdown().phase)
        assertEquals(600_000L, source.getCountdown().remainingMillis)
    }

    @Test fun mockStoveTransitionsKeepGasAndFlameConsistent() {
        assertFalse(source.toggleMockFlame())
        assertEquals(EnvironmentSensorState(), source.sensorState)
        source.toggleMockGas()
        assertEquals(1, added(EnvironmentAlertType.GAS_LEAK_RISK))
        assertTrue(source.toggleMockFlame())
        assertTrue(source.sensorState.flameDetected)
        val deadline = source.getCountdown().deadlineElapsed
        source.toggleMockGas()
        assertEquals(EnvironmentSensorState(), source.sensorState)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
        assertEquals(deadline, source.getCountdown().deadlineElapsed)
        expire()
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
    }

    @Test fun cancelledCycleStaysCancelledUntilPersonReturns() {
        sensors(flame = true); source.cancelCountdown()
        sensors(); sensors(flame = true)
        assertEquals(EnvironmentCountdownPhase.CANCELLED, source.getCountdown().phase)
        sensors(flame = true, person = true)
        assertEquals(EnvironmentCountdownPhase.IDLE, source.getCountdown().phase)
        sensors(flame = true)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
    }

    @Test fun cancelConfigureManualFinishAndNextAutoCycleKeepEighteenMinutes() {
        assertTrue(source.configureMinutes(18))
        sensors(flame = true)
        source.cancelCountdown()
        assertTrue(source.configureMinutes(18))
        assertEquals(EnvironmentCountdownPhase.CANCELLED, source.getCountdown().phase)
        assertEquals(18, source.getCountdown().configuredMinutes)
        assertTrue(source.startManualCountdown())
        assertEquals(1_080_000L, source.getCountdown().remainingMillis)
        now += 1_080_001L
        source.refresh()
        assertEquals(EnvironmentCountdownPhase.FINISHED, source.getCountdown().phase)
        assertEquals(18, source.getCountdown().configuredMinutes)
        repeat(3) { source.refresh() }
        assertEquals(1, added(EnvironmentAlertType.UNATTENDED_COOKING))
        sensors(flame = true, person = true); sensors(flame = true)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
        assertEquals(1_080_000L, source.getCountdown().remainingMillis)
    }

    @Test fun mockPersonControlIsIndependentOfGasAndFlame() {
        source.toggleMockPerson()
        assertEquals(EnvironmentSensorState(personDetected = true), source.sensorState)
        source.toggleMockPerson()
        assertEquals(EnvironmentSensorState(), source.sensorState)
    }
}
