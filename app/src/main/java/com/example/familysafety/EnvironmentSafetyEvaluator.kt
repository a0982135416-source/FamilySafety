package com.example.familysafety

/** Pure rules: gas/flame are deliberately irrelevant to expiry eligibility. */
object EnvironmentSafetyEvaluator {
    fun gasLeakRisk(sensor: EnvironmentSensorState) = sensor.gasOn && !sensor.flameDetected
    fun shouldStartCountdown(sensor: EnvironmentSensorState) = sensor.flameDetected && !sensor.personDetected
    fun shouldCancelCountdown(sensor: EnvironmentSensorState, running: Boolean) = running && sensor.personDetected
    fun expiryEligible(sensor: EnvironmentSensorState) = !sensor.personDetected
    fun shouldResolveGasLeak(sensor: EnvironmentSensorState) = !gasLeakRisk(sensor)
    fun shouldResolveUnattended(sensor: EnvironmentSensorState) =
        sensor.personDetected || (!sensor.gasOn && !sensor.flameDetected)

    fun shouldResolve(type: EnvironmentAlertType, sensor: EnvironmentSensorState) = when (type) {
        EnvironmentAlertType.GAS_LEAK_RISK -> shouldResolveGasLeak(sensor)
        EnvironmentAlertType.UNATTENDED_COOKING -> shouldResolveUnattended(sensor)
    }
}
