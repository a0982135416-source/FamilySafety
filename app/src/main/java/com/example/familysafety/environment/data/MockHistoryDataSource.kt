package com.example.familysafety.environment.data

import com.example.familysafety.environment.model.HistoryPoint

/**
 * ============================================================================
 * 歷史趨勢模擬資料來源
 * Historical Trend Mock Data Source
 * ============================================================================
 *
 * 僅供 Android 開發與畫面測試。
 * For Android development and UI testing only.
 *
 * 不會讀取或寫入 MariaDB。
 * Does not read from or write to MariaDB.
 */

object MockHistoryDataSource {

    // =========================================================================
    // 01. 日期範圍
    // History Period
    // =========================================================================

    enum class Period {
        DAY,
        WEEK,
        MONTH
    }


    // =========================================================================
    // 02. 取得歷史資料
    // Get Historical Data
    // =========================================================================

    fun getHistory(period: Period, location: String): List<HistoryPoint> {

        val labels = when (period) {
            Period.DAY -> getDayData()
            Period.WEEK -> getWeekData()
            Period.MONTH -> getMonthData()
        }
        val temperatures = when (location) {
            "KITCHEN" -> when (period) {
                Period.DAY -> listOf(26.2f, 26.0f, 26.5f, 27.2f, 27.0f, 26.8f, 26.4f)
                Period.WEEK -> listOf(26.4f, 26.8f, 27.1f, 26.6f, 27.3f, 26.9f, 26.5f)
                Period.MONTH -> listOf(26.1f, 26.5f, 26.9f, 27.2f, 27.0f, 26.7f, 26.3f)
            }
            "LIVING_ROOM" -> when (period) {
                Period.DAY -> listOf(25.0f, 25.2f, 25.6f, 26.0f, 25.8f, 25.4f, 25.1f)
                Period.WEEK -> listOf(25.3f, 25.7f, 25.5f, 26.1f, 25.9f, 25.4f, 25.2f)
                Period.MONTH -> listOf(25.1f, 25.4f, 25.8f, 26.0f, 25.6f, 25.3f, 25.5f)
            }
            "BEDROOM" -> when (period) {
                Period.DAY -> listOf(24.4f, 24.2f, 24.6f, 25.1f, 25.0f, 24.9f, 24.5f)
                Period.WEEK -> listOf(24.6f, 24.9f, 24.7f, 25.2f, 25.0f, 24.8f, 24.3f)
                Period.MONTH -> listOf(24.3f, 24.7f, 25.0f, 25.2f, 24.8f, 24.5f, 24.9f)
            }
            else -> error("Unknown environment location: $location")
        }
        val humidities = when (location) {
            "KITCHEN" -> when (period) {
                Period.DAY -> listOf(60f, 59f, 58f, 56f, 57f, 58f, 59f)
                Period.WEEK -> listOf(59f, 58f, 56f, 60f, 57f, 58f, 59f)
                Period.MONTH -> listOf(60f, 59f, 57f, 56f, 58f, 59f, 58f)
            }
            "LIVING_ROOM" -> when (period) {
                Period.DAY -> listOf(63f, 62f, 61f, 59f, 60f, 61f, 62f)
                Period.WEEK -> listOf(62f, 60f, 63f, 59f, 61f, 62f, 60f)
                Period.MONTH -> listOf(63f, 61f, 60f, 59f, 62f, 61f, 60f)
            }
            else -> when (period) {
                Period.DAY -> listOf(57f, 56f, 55f, 53f, 54f, 55f, 56f)
                Period.WEEK -> listOf(56f, 55f, 57f, 53f, 54f, 56f, 55f)
                Period.MONTH -> listOf(57f, 55f, 54f, 53f, 56f, 55f, 54f)
            }
        }
        // Fixed period labels and point count retain the existing time semantics.
        return labels.mapIndexed { index, label -> HistoryPoint(label, temperatures[index], humidities[index]) }
    }


    // =========================================================================
    // 03. 每日模擬資料
    // Daily Mock Data
    // =========================================================================

    private fun getDayData(): List<String> {

        return listOf(
            "00", "04", "08", "12", "16", "20", "24"
        )
    }


    // =========================================================================
    // 04. 每週模擬資料
    // Weekly Mock Data
    // =========================================================================

    private fun getWeekData(): List<String> {

        return listOf(
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
        )
    }


    // =========================================================================
    // 05. 每月模擬資料
    // Monthly Mock Data
    // =========================================================================

    private fun getMonthData(): List<String> {

        return listOf(
            "1", "5", "10", "15", "20", "25", "30"
        )
    }
}
