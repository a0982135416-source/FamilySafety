package com.example.familysafety.environment

/**
 * 環境感測器日誌資料模型 (Sensor Log Data Class)
 * 對接 Raspberry Pi Server / MariaDB 的環境感測歷史資料
 */
data class SensorLog(
    val id: Int? = null,
    val location: String? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val gasLevel: Double? = null,
    val flameDetected: Boolean? = null,
    val personDetected: Boolean? = null,
    val timestamp: String? = null
)
