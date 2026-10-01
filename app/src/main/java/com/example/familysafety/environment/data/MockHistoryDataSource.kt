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

    fun getHistory(period: Period): List<HistoryPoint> {

        return when (period) {
            Period.DAY -> getDayData()
            Period.WEEK -> getWeekData()
            Period.MONTH -> getMonthData()
        }
    }


    // =========================================================================
    // 03. 每日模擬資料
    // Daily Mock Data
    // =========================================================================

    private fun getDayData(): List<HistoryPoint> {

        return listOf(
            HistoryPoint("00", 24.1f, 62f),
            HistoryPoint("04", 23.8f, 65f),
            HistoryPoint("08", 25.2f, 61f),
            HistoryPoint("12", 28.1f, 55f),
            HistoryPoint("16", 27.3f, 57f),
            HistoryPoint("20", 25.4f, 60f),
            HistoryPoint("24", 24.6f, 63f)
        )
    }


    // =========================================================================
    // 04. 每週模擬資料
    // Weekly Mock Data
    // =========================================================================

    private fun getWeekData(): List<HistoryPoint> {

        return listOf(
            HistoryPoint("Mon", 25.2f, 60f),
            HistoryPoint("Tue", 26.1f, 58f),
            HistoryPoint("Wed", 27.4f, 56f),
            HistoryPoint("Thu", 26.8f, 59f),
            HistoryPoint("Fri", 28.2f, 54f),
            HistoryPoint("Sat", 27.1f, 57f),
            HistoryPoint("Sun", 25.8f, 62f)
        )
    }


    // =========================================================================
    // 05. 每月模擬資料
    // Monthly Mock Data
    // =========================================================================

    private fun getMonthData(): List<HistoryPoint> {

        return listOf(
            HistoryPoint("1", 24.5f, 63f),
            HistoryPoint("5", 25.2f, 61f),
            HistoryPoint("10", 26.4f, 59f),
            HistoryPoint("15", 28.1f, 55f),
            HistoryPoint("20", 27.3f, 57f),
            HistoryPoint("25", 26.2f, 60f),
            HistoryPoint("30", 25.1f, 62f)
        )
    }
}