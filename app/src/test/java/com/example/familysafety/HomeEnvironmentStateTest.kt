package com.example.familysafety

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HomeEnvironmentStateTest {
    private val source = MockEnvironmentDataSource
    private var now = 1_000L

    @Before fun reset() {
        now = 1_000L
        source.resetForTests { now }
        MockEnvironmentAlertDataSource.resetForTests()
        source.refresh()
    }

    @Test fun defaultKitchenAndAllSelectedLocationsUseExistingReadings() {
        assertEquals("KITCHEN", source.selectedLocation)
        listOf(Triple("KITCHEN", 26.8, 58), Triple("LIVING_ROOM", 25.4, 61),
            Triple("BEDROOM", 24.9, 55)).forEach { (location, temperature, humidity) ->
            assertTrue(source.selectLocation(location))
            assertEquals(location, source.selectedLocation)
            assertEquals(temperature, source.getCurrentEnvironmentData().temperature, 0.0)
            assertEquals(humidity, source.getCurrentEnvironmentData().humidity)
        }
    }

    @Test fun invalidLocationPreservesSelectionAndReading() {
        source.selectLocation("BEDROOM")
        val reading = source.getCurrentEnvironmentData()
        assertFalse(source.selectLocation("UNKNOWN"))
        assertEquals("BEDROOM", source.selectedLocation)
        assertSame(reading, source.getCurrentEnvironmentData())
    }

    @Test fun selectionDoesNotChangeSensorsCountdownOrAlerts() {
        source.updateSensors(EnvironmentSensorState(true, true, false))
        val sensor = source.sensorState
        val countdown = source.getCountdown()
        val alerts = MockEnvironmentAlertDataSource.getAlerts()
        source.selectLocation("LIVING_ROOM")
        assertSame(sensor, source.sensorState)
        assertEquals(countdown, source.getCountdown())
        assertEquals(alerts, MockEnvironmentAlertDataSource.getAlerts())
    }

    @Test fun resetRestoresDefaultLocation() {
        source.selectLocation("BEDROOM")
        source.resetForTests { now }
        assertEquals("KITCHEN", source.selectedLocation)
    }

    @Test fun safeOffAndAttendedCookingIgnoreResolvedHistory() {
        assertTrue(MockEnvironmentAlertDataSource.getAlerts().all { it.status == EnvironmentAlertStatus.RESOLVED })
        assertEquals(EnvironmentSafetyStatus.SAFE, source.getSafetyStatus())
        source.updateSensors(EnvironmentSensorState(true, true, true))
        assertEquals(EnvironmentSafetyStatus.SAFE, source.getSafetyStatus())
    }

    @Test fun gracePeriodIsWarningThenExpiryIsDangerOnlyOnce() {
        source.updateSensors(EnvironmentSensorState(true, true, false))
        assertEquals(EnvironmentSafetyStatus.WARNING, source.getSafetyStatus())
        assertNull(source.getActiveWarning())
        now += 600_001L
        assertEquals(EnvironmentSafetyStatus.DANGER, source.getSafetyStatus())
        assertEquals(EnvironmentAlertType.UNATTENDED_COOKING, source.getActiveWarning()!!.type)
        repeat(5) { assertEquals(EnvironmentSafetyStatus.DANGER, source.getSafetyStatus()) }
        assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
    }

    @Test fun activeDangerOutranksRestartedCountdownThenPersonReturnResolves() {
        source.updateSensors(EnvironmentSensorState(true, true, false))
        now += 600_001L
        source.refresh()
        source.configureMinutes(15)
        assertEquals(EnvironmentCountdownPhase.RUNNING, source.getCountdown().phase)
        assertEquals(EnvironmentSafetyStatus.DANGER, source.getSafetyStatus())
        source.toggleMockPerson()
        assertEquals(EnvironmentSafetyStatus.SAFE, source.getSafetyStatus())
        assertEquals(EnvironmentAlertStatus.RESOLVED, MockEnvironmentAlertDataSource.getAlerts().last().status)
    }

    @Test fun gasLeakResolvesToWarningThenSafeWithoutDiscardingHistory() {
        source.toggleMockGas()
        assertEquals(EnvironmentSafetyStatus.DANGER, source.getSafetyStatus())
        source.toggleMockFlame()
        assertEquals(EnvironmentSafetyStatus.WARNING, source.getSafetyStatus())
        assertEquals(EnvironmentAlertStatus.RESOLVED, MockEnvironmentAlertDataSource.getAlerts().last().status)
        source.toggleMockGas()
        source.toggleMockPerson()
        assertEquals(EnvironmentSafetyStatus.SAFE, source.getSafetyStatus())
        assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
    }
}
