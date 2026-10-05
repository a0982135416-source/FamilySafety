package com.example.familysafety

/** Kitchen safety sensors; shared only for the current App process. */
data class EnvironmentSensorState(
    val gasOn: Boolean = false,
    val flameDetected: Boolean = false,
    val personDetected: Boolean = false,
)

enum class EnvironmentCountdownPhase { IDLE, RUNNING, FINISHED }

/** Presentation of existing active alerts/countdown; contains no sensor safety rules. */
enum class EnvironmentSafetyStatus { SAFE, WARNING, DANGER }

data class EnvironmentCountdownState(
    val configuredMinutes: Int,
    val phase: EnvironmentCountdownPhase,
    val deadlineElapsed: Long?,
    val remainingMillis: Long,
)
