package com.example.familysafety

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** One reminder schedule per visible Environment screen; acknowledgements never change alert records. */
internal class EnvironmentWarningReminder {
    var activeId: Long? = null
        private set
    private var nextDueAt = 0L

    fun shouldWarn(id: Long?, now: Long): Boolean {
        if (activeId != id) { activeId = id; nextDueAt = 0L }
        return id != null && now >= nextDueAt
    }

    fun acknowledge(now: Long, intervalMillis: Long) { nextDueAt = now + intervalMillis }
    fun reset() { activeId = null; nextDueAt = 0L }
}

/** View-owned Dialog and one-shot platform sound. The Fragment's existing ticker drives the schedule. */
internal class EnvironmentWarningController(
    private val context: Context,
    private val canShow: () -> Boolean,
    private val onViewAlert: () -> Unit,
) {
    companion object {
        const val REMINDER_INTERVAL_MILLIS = 10_000L
        internal var reminderIntervalForTests: Long? = null
    }

    private val reminder = EnvironmentWarningReminder()
    private var visible = false
    private var dialog: AlertDialog? = null
    private var tone: ToneGenerator? = null

    fun resume() { visible = true; reminder.reset() }

    fun update(alert: EnvironmentAlert?) {
        if (!visible) return
        if (reminder.activeId != alert?.id) dismissWarning()
        val due = reminder.shouldWarn(alert?.id, SystemClock.elapsedRealtime())
        if (alert == null || !due || dialog?.isShowing == true || !canShow()) return
        val gas = alert.type == EnvironmentAlertType.GAS_LEAK_RISK
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (gas) R.string.alert_type_gas_leak else R.string.alert_type_unattended)
            .setMessage(if (gas) R.string.environment_warning_gas_message else R.string.environment_warning_unattended_message)
            .setCancelable(false)
            .setPositiveButton(R.string.environment_warning_got_it) { _, _ -> acknowledge() }
            .setNegativeButton(R.string.environment_warning_view_alert) { _, _ ->
                acknowledge()
                if (visible && canShow()) onViewAlert()
            }
            .create().also { warning ->
                warning.setOnDismissListener {
                    if (dialog === warning) {
                        dialog = null
                        tone?.stopTone()
                    }
                }
                warning.show()
            }
        // A finite platform alarm tone, never a looping player or a background notification.
        if (tone == null) tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 80) }.getOrNull()
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
    }

    private fun acknowledge() {
        tone?.stopTone()
        reminder.acknowledge(SystemClock.elapsedRealtime(),
            reminderIntervalForTests ?: REMINDER_INTERVAL_MILLIS)
    }

    private fun dismissWarning() {
        tone?.stopTone()
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
    }

    fun stop() {
        visible = false
        reminder.reset()
        dismissWarning()
        tone?.release()
        tone = null
    }

    fun destroy() = stop()
}
