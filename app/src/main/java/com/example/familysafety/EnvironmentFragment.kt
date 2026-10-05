package com.example.familysafety

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.familysafety.databinding.FragmentEnvironmentBinding
import com.example.familysafety.environment.data.MockHistoryDataSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * ============================================================================
 * 環境監控頁面
 * Environment Monitoring Fragment
 * ============================================================================
 *
 * 此 Fragment 負責顯示家庭環境監控資訊。
 * This Fragment displays home environment monitoring information.
 *
 * 目前已完成：
 * Currently implemented:
 *
 * 1. 安全倒數時間設定 / Safety countdown setting
 * 2. 環境地點選擇 / Environment location selection
 * 3. 地點溫濕度模擬資料 / Location temperature and humidity mock data
 */
class EnvironmentFragment : Fragment() {

    // =========================================================================
    // 01. ViewBinding
    // ViewBinding
    // =========================================================================

    private var _binding: FragmentEnvironmentBinding? = null
    private val binding get() = _binding!!


    // =========================================================================
    // 02. 安全倒數設定
    // Safety Countdown Setting
    // =========================================================================

    // STEP 8-1: process-memory safety state; this ticker only renders the current View.
    private val selectedCountdownMinutes get() = MockEnvironmentDataSource.getCountdown().configuredMinutes
    private val dialogs = mutableSetOf<AlertDialog>()
    private val refreshSafety = object : Runnable {
        override fun run() {
            val current = _binding ?: return
            renderSafety()
            current.root.postDelayed(this, 250L)
        }
    }

    // =========================================================================
    // 03. 環境監控地點
    // Environment Monitoring Location
    // =========================================================================

    // 地點與即時讀值共用 process-memory source；歷史趨勢資料維持原樣。
    // Selection/current readings are shared with Home; history remains independent.
    private val selectedLocation: String
        get() = MockEnvironmentDataSource.selectedLocation


    // =========================================================================
    // 04. 共用環境模擬資料
    // Shared Environment Mock Data
    // =========================================================================

    // EnvironmentMockData / locationMockData 已移至 MockEnvironmentDataSource。
    // Existing location values now have one source of truth.


    // =========================================================================
    // 06. 建立 Fragment View
    // Create Fragment View
    // =========================================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentEnvironmentBinding.inflate(
            inflater,
            container,
            false,
        )

        return binding.root
    }


    // =========================================================================
    // 07. View 建立完成
    // View Created
    // =========================================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)


        // 01. 初始化安全倒數
        // Initialize safety countdown
        setupSafetyCountdown()
        setupSensorControls()

        // 02. 初始化地點選擇
        // Initialize location selector
        setupLocationSelector()

        // 03. 初始化歷史趨勢圖
        // Initialize historical trend chart
        setupHistoricalTrend()
    }


// =========================================================================
// 01. 初始化安全倒數
// Initialize Safety Countdown
// =========================================================================

    private fun setupSafetyCountdown() {

        // 初始化倒數顯示
        // Initialize countdown display
        updateCountdownDisplay()

        // 預設狀態：待命
        // Default status: Standby
        renderSafety()

        // 設定倒數分鐘數
        // Open duration setting dialog
        binding.buttonEnvironmentCountdownSetting.setOnClickListener {
            showCountdownDialog()
        }
    }

    // =========================================================================
// 02. 自由設定倒數分鐘數
// Set Custom Countdown Duration
// =========================================================================

    private fun showCountdownDialog() {

        // 建立數字輸入欄位
        // Create numeric input field
        val input = EditText(requireContext()).apply {

            // 只能輸入正整數
            // Allow positive integer input only
            inputType = InputType.TYPE_CLASS_NUMBER

            // 顯示目前設定值
            // Display the currently selected duration
            setText(selectedCountdownMinutes.toString())

            // 游標移至文字最後
            // Move cursor to the end
            setSelection(text.length)

            // 文字置中
            // Center the input text
            gravity = Gravity.CENTER

            // 輸入提示
            // Input hint
            setHint(R.string.countdown_duration_hint)

            // 限制輸入長度
            // Limit input length
            filters = arrayOf(
                android.text.InputFilter.LengthFilter(3)
            )
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.countdown_duration_title)
            .setView(input)
            .setNegativeButton(R.string.countdown_dialog_cancel, null)
            .setPositiveButton(R.string.countdown_dialog_confirm, null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE
            ).setOnClickListener {

                if (_binding == null || !isAdded) return@setOnClickListener
                // 取得使用者輸入的分鐘數
                // Read the entered duration
                val minutes = input.text
                    .toString()
                    .toIntOrNull()

                // 驗證分鐘數：1～180
                // Validate duration: 1 to 180 minutes
                if (minutes == null || (minutes !in 1..180)) {

                    input.error = getString(
                        R.string.countdown_input_invalid
                    )

                    return@setOnClickListener
                }

                // 儲存使用者設定
                // Save selected duration
                if (!MockEnvironmentDataSource.configureMinutes(minutes)) return@setOnClickListener

                // 更新畫面
                // Refresh countdown display
                updateCountdownDisplay()

                dialog.dismiss()
            }
        }

        showTrackedDialog(dialog)
    }


    // =========================================================================
    // 10. 更新安全倒數時間顯示
    // Update Safety Countdown Display
    // =========================================================================

    // =========================================================================
    // 03. 更新倒數設定顯示
    // Update Countdown Duration Display
    // =========================================================================

    private fun updateCountdownDisplay() = renderSafety()

    // =========================================================================
    // 11. 設定環境地點選擇功能
    // Setup Environment Location Selector
    // =========================================================================

    private fun setupLocationSelector() {

        // 顯示目前選擇的地點
        // Display the currently selected location
        updateLocationDisplay()

        // 顯示目前地點的溫度與濕度
        // Display temperature and humidity for the current location
        updateEnvironmentData()

        // 點擊地點按鈕時開啟選擇 Dialog
        // Open the location dialog when the location button is clicked
        binding.buttonEnvironmentLocation.setOnClickListener {
            showLocationDialog()
        }
    }


    // =========================================================================
    // 12. 顯示環境地點選擇 Dialog
    // Show Environment Location Selection Dialog
    // =========================================================================

    private fun showLocationDialog() {

        // Dialog 中顯示的地點名稱
        // Location names displayed in the dialog
        val locationLabels = arrayOf(
            getString(R.string.location_kitchen),
            getString(R.string.location_living_room),
            getString(R.string.location_bedroom)
        )

        // 根據目前地點決定預設勾選位置
        // Determine the currently selected location index
        val selectedIndex = when (selectedLocation) {

            "KITCHEN" -> 0

            "LIVING_ROOM" -> 1

            "BEDROOM" -> 2

            else -> 0
        }

        // 建立地點選擇 Dialog
        // Create location selection dialog
        val locationDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.location_dialog_title)

            // 單選地點
            // Single-choice location selection
            .setSingleChoiceItems(
                locationLabels,
                selectedIndex
            ) { dialog, which ->
                if (_binding == null || !isAdded) return@setSingleChoiceItems

                // 儲存選擇的地點
                // Save the selected location
                MockEnvironmentDataSource.selectLocation(when (which) {

                    0 -> "KITCHEN"

                    1 -> "LIVING_ROOM"

                    2 -> "BEDROOM"

                    else -> "KITCHEN"
                })

                // 更新地點文字與 Icon
                // Update location text and icon
                updateLocationDisplay()

                // 更新該地點的溫度與濕度
                // Update temperature and humidity
                updateEnvironmentData()

                // 選擇完成後關閉 Dialog
                // Close the dialog after selection
                dialog.dismiss()
            }

            // 取消按鈕
            // Cancel button
            .setNegativeButton(
                R.string.location_dialog_cancel
            ) { dialog, _ ->

                dialog.dismiss()
            }

            .create()
        showTrackedDialog(locationDialog)
    }


    // =========================================================================
    // 13. 更新環境地點顯示
    // Update Environment Location Display
    // =========================================================================

    private fun updateLocationDisplay() {

        when (selectedLocation) {

            "KITCHEN" -> {

                binding.buttonEnvironmentLocation.setText(
                    R.string.location_kitchen_selector
                )

                binding.buttonEnvironmentLocation.setIconResource(
                    R.drawable.ic_location_kitchen
                )
            }

            "LIVING_ROOM" -> {

                binding.buttonEnvironmentLocation.setText(
                    R.string.location_living_room_selector
                )

                binding.buttonEnvironmentLocation.setIconResource(
                    R.drawable.ic_location_livingroom
                )
            }

            "BEDROOM" -> {

                binding.buttonEnvironmentLocation.setText(
                    R.string.location_bedroom_selector
                )

                binding.buttonEnvironmentLocation.setIconResource(
                    R.drawable.ic_location_bedroom
                )
            }
        }
    }


    // =========================================================================
    // 14. 更新目前地點的環境資料
    // Update Environment Data for the Selected Location
    // =========================================================================

    private fun updateEnvironmentData() {

        // 根據 selectedLocation 取得對應的 Mock Data
        // Get mock data for the currently selected location
        val data = MockEnvironmentDataSource.getCurrentEnvironmentData()

        // 更新溫度
        // Update temperature
        binding.textViewEnvironmentTemperatureValue.text =
            getString(R.string.environment_temperature_value_format, data.temperature)

        // 更新濕度
        // Update humidity
        binding.textViewEnvironmentHumidityValue.text =
            getString(R.string.environment_humidity_value_format, data.humidity)
    }


    // =========================================================================
    // 15. 銷毀 Fragment View
    // Destroy Fragment View
    // =========================================================================

    // =========================================================================
// 01. 初始化歷史趨勢圖
// Initialize Historical Trend Chart
// =========================================================================

    private fun setupHistoricalTrend() {

        // 預設顯示每日歷史資料
        // Display daily historical data by default
        updateHistoricalChart(
            MockHistoryDataSource.Period.DAY
        )

        // 設定 Day 為預設選取項目
        // Set Day as the default selected period
        binding.materialButtonToggleGroupEnvironmentHistoryPeriod.check(
            R.id.button_environment_history_day
        )

        // 監聽 Day / Week / Month 按鈕
        // Listen for Day / Week / Month selection
        binding.materialButtonToggleGroupEnvironmentHistoryPeriod.addOnButtonCheckedListener {
                _, checkedId, isChecked ->

            // 只處理選取事件，忽略取消選取事件
            // Handle checked events only
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            when (checkedId) {

                // Day：每日資料
                // Daily data
                R.id.button_environment_history_day -> {

                    updateHistoricalChart(
                        MockHistoryDataSource.Period.DAY
                    )
                }

                // Week：每週資料
                // Weekly data
                R.id.button_environment_history_week -> {

                    updateHistoricalChart(
                        MockHistoryDataSource.Period.WEEK
                    )
                }

                // Month：每月資料
                // Monthly data
                R.id.button_environment_history_month -> {

                    updateHistoricalChart(
                        MockHistoryDataSource.Period.MONTH
                    )
                }
            }
        }
    }

    // =========================================================================
// 02. 更新歷史趨勢圖
// Update Historical Trend Chart
// =========================================================================

    private fun updateHistoricalChart(
        period: MockHistoryDataSource.Period
    ) {

        // 從獨立 Mock DataSource 取得歷史資料
        // Get historical data from the independent Mock DataSource
        val historyData = MockHistoryDataSource.getHistory(period)

        // 將資料傳送給 HistoryChartView
        // Pass historical data to HistoryChartView
        binding.historyChartViewEnvironmentHistory.setHistoryData(historyData)
    }

    // =========================================================================
// 04. 感測器操作與安全倒數畫面
// Sensor Controls and Safety Countdown Display
// =========================================================================

    // Sensor cards remain the existing controls, with explicit localized interaction hints.
    private fun setupSensorControls() {
        binding.cardViewEnvironmentGasStatus.setOnClickListener {
            MockEnvironmentDataSource.toggleMockGas()
            renderSafety()
        }
        binding.cardViewEnvironmentFlameStatus.setOnClickListener {
            if (!MockEnvironmentDataSource.toggleMockFlame()) {
                Toast.makeText(requireContext(), R.string.environment_turn_on_gas_first, Toast.LENGTH_SHORT).show()
            }
            renderSafety()
        }
        binding.cardViewEnvironmentPersonStatus.setOnClickListener {
            MockEnvironmentDataSource.toggleMockPerson()
            renderSafety()
        }
        renderSafety()
    }

    private fun renderSafety() {
        val current = _binding ?: return
        val countdown = MockEnvironmentDataSource.getCountdown()
        val sensor = MockEnvironmentDataSource.sensorState
        fun stateText(view: TextView, key: Int, color: Int) {
            view.setText(key)
            view.setBackgroundResource(R.drawable.bg_sensor_neutral)
            view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color))
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
        stateText(current.textViewEnvironmentGasValue,
            if (sensor.gasOn) R.string.environment_status_on else R.string.environment_status_off,
            if (EnvironmentSafetyEvaluator.gasLeakRisk(sensor)) R.color.warning_red else R.color.safe_green)
        stateText(current.textViewEnvironmentFlameValue,
            if (sensor.flameDetected) R.string.environment_status_detected else R.string.environment_status_not_detected,
            if (sensor.flameDetected) R.color.warning_red else R.color.secondary_text)
        stateText(current.textViewEnvironmentPersonValue,
            if (sensor.personDetected) R.string.environment_status_detected else R.string.environment_status_not_detected,
            if (sensor.personDetected) R.color.safe_green else R.color.secondary_text)
        listOf(current.cardViewEnvironmentGasStatus to R.string.environment_gas_status,
            current.cardViewEnvironmentFlameStatus to R.string.environment_flame_detection,
            current.cardViewEnvironmentPersonStatus to R.string.environment_person_detection).forEach { (card, key) ->
            card.contentDescription = getString(key)
        }
        current.textViewEnvironmentCountdownDefault.text = getString(R.string.countdown_duration_display, countdown.configuredMinutes)
        updateRemainingTime(countdown.remainingMillis)
        current.textViewEnvironmentCountdownStatus.setText(when (countdown.phase) {
            EnvironmentCountdownPhase.IDLE -> R.string.countdown_status_standby
            EnvironmentCountdownPhase.RUNNING -> R.string.countdown_status_running
            EnvironmentCountdownPhase.FINISHED -> R.string.countdown_status_finished
        })
        (activity as? MainActivity)?.refreshEnvironmentWarning()
    }

    private fun showTrackedDialog(dialog: AlertDialog) {
        if (_binding == null || !isAdded) return
        dialogs.add(dialog)
        dialog.setOnDismissListener { dialogs.remove(dialog) }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.root.removeCallbacks(refreshSafety)
        binding.root.post(refreshSafety)
    }

    override fun onPause() {
        _binding?.root?.removeCallbacks(refreshSafety)
        super.onPause()
    }

    private fun updateRemainingTime(remainingMillis: Long) {

        // 向上取整秒數，避免開始後立即顯示少一秒
        // Round up seconds to avoid immediately losing one second
        val totalSeconds = (
                remainingMillis.coerceAtLeast(0L) + 999L
                ) / 1000L

        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L

        // 顯示 MM:SS，例如 18:00、17:59
        // Display MM:SS format
        binding.textViewEnvironmentCountdownRemaining.text = getString(
            R.string.countdown_remaining_display,
            minutes,
            seconds
        )
    }

    // =========================================================================
    // 07. 清理 Fragment View
    // Clean Up Fragment View
    // =========================================================================

    override fun onDestroyView() {

        // Detach View callbacks only; shared countdown deadline remains authoritative.
        _binding?.root?.removeCallbacks(refreshSafety)
        dialogs.toList().forEach { it.setOnDismissListener(null); it.dismiss() }
        dialogs.clear()

        super.onDestroyView()
        _binding = null
    }
}
