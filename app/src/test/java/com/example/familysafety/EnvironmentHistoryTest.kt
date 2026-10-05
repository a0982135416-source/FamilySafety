package com.example.familysafety

import com.example.familysafety.environment.data.MockHistoryDataSource
import org.junit.Assert.*
import org.junit.Test

class EnvironmentHistoryTest {
    private val locations = listOf("KITCHEN", "LIVING_ROOM", "BEDROOM")

    @Test fun allPeriodsHaveDistinctTemperatureAndHumidityForEachLocation() {
        MockHistoryDataSource.Period.values().forEach { period ->
            val histories = locations.map { MockHistoryDataSource.getHistory(period, it) }
            assertEquals(3, histories.map { data -> data.map { it.temperature } }.distinct().size)
            assertEquals(3, histories.map { data -> data.map { it.humidity } }.distinct().size)
            histories.forEach { assertEquals(7, it.size) }
        }
    }

    @Test fun repeatedQueriesAndLocationRoundTripRetainFixedHistory() {
        MockHistoryDataSource.Period.values().forEach { period ->
            locations.forEach { location ->
                val original = MockHistoryDataSource.getHistory(period, location)
                repeat(3) {
                    locations.forEach { MockHistoryDataSource.getHistory(period, it) }
                    assertEquals(original, MockHistoryDataSource.getHistory(period, location))
                }
            }
        }
    }

    @Test fun originalPeriodLabelsAndTimeSemanticsArePreserved() {
        val labels = mapOf(
            MockHistoryDataSource.Period.DAY to listOf("00", "04", "08", "12", "16", "20", "24"),
            MockHistoryDataSource.Period.WEEK to listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            MockHistoryDataSource.Period.MONTH to listOf("1", "5", "10", "15", "20", "25", "30")
        )
        labels.forEach { (period, expected) ->
            locations.forEach { assertEquals(expected, MockHistoryDataSource.getHistory(period, it).map { p -> p.label }) }
        }
    }

    @Test fun locationMappingMatchesReasonableFixedReadings() {
        val temperatures = listOf(26.0f..27.3f, 25.0f..26.1f, 24.2f..25.2f)
        val humidities = listOf(56f..60f, 59f..63f, 53f..57f)
        locations.forEachIndexed { index, location ->
            MockHistoryDataSource.Period.values().forEach { period ->
                MockHistoryDataSource.getHistory(period, location).forEach {
                    assertTrue(it.temperature in temperatures[index])
                    assertTrue(it.humidity in humidities[index])
                }
            }
        }
        assertEquals(listOf(26.2f, 26.0f, 26.5f, 27.2f, 27.0f, 26.8f, 26.4f),
            MockHistoryDataSource.getHistory(MockHistoryDataSource.Period.DAY, "KITCHEN").map { it.temperature })
    }
}
