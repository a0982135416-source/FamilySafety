package com.example.familysafety.environment.model

/**
 * 歷史圖表單筆資料
 * A single historical chart data point
 *
 * label       X 軸標籤 / X-axis label
 * temperature 溫度（°C）/ Temperature
 * humidity    濕度（%）/ Humidity
 */
data class HistoryPoint(
    val label: String,
    val temperature: Float,
    val humidity: Float
)
