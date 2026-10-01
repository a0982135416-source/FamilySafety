package com.example.familysafety.environment.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

import com.example.familysafety.R
import com.example.familysafety.environment.model.HistoryPoint

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/**
 * ============================================================================
 * 雙 Y 軸歷史曲線圖
 * Dual Y-Axis Historical Line Chart
 * ============================================================================
 *
 * 左 Y 軸：溫度 °C
 * Left Y-axis: Temperature
 *
 * 右 Y 軸：濕度 %
 * Right Y-axis: Humidity
 *
 * X 軸：時間標籤
 * X-axis: Time labels
 */
class HistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    // =========================================================================
    // 01. 圖表資料
    // Chart Data
    // =========================================================================

    private var historyData: List<HistoryPoint> = emptyList()


    // =========================================================================
    // 02. 圖表顏色
    // Chart Colors
    // =========================================================================

    private val temperatureColor = Color.parseColor("#F0525C")

    private val humidityColor = Color.parseColor("#1687E8")

    private val axisColor = Color.parseColor("#607D9D")

    private val gridColor = Color.parseColor("#E2EAF1")


    // =========================================================================
    // 03. Paint 設定
    // Paint Configuration
    // =========================================================================

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = axisColor
        strokeWidth = dp(1f)
        textSize = sp(10f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = dp(1f)
    }

    private val temperaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = temperatureColor
        strokeWidth = dp(2.5f)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val humidityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = humidityColor
        strokeWidth = dp(2.5f)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }


    // =========================================================================
    // 04. 更新圖表資料
    // Update Chart Data
    // =========================================================================

    fun setHistoryData(data: List<HistoryPoint>) {

        // 儲存新的歷史資料
        // Store the new historical data
        historyData = data.toList()

        // 通知 Android 重新繪製圖表
        // Request chart redraw
        invalidate()
    }


    // =========================================================================
    // 05. dp / sp 單位轉換
    // Dimension Conversion
    // =========================================================================

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return value * resources.displayMetrics.scaledDensity
    }


    // =========================================================================
    // 06. 繪製圖表
    // Draw Chart
    // =========================================================================

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        // 沒有資料時不繪製
        // Do not draw when data is empty
        if (historyData.isEmpty()) {
            return
        }


        // =====================================================================
        // 6-1. 計算圖表範圍
        // Calculate Chart Bounds
        // =====================================================================

        val left = paddingLeft + dp(38f)

        val right = width - paddingRight - dp(38f)

        val top = paddingTop + dp(38f)

        val bottom = height - paddingBottom - dp(28f)

        val chartWidth = right - left

        val chartHeight = bottom - top

        if (chartWidth <= 0f || chartHeight <= 0f) {
            return
        }


        // =====================================================================
        // 6-2. 計算左右 Y 軸的最大值與最小值
        // Calculate Min / Max Values for Both Y Axes
        // =====================================================================

        val minTemperature = (
                floor(
                    historyData.minOf { it.temperature }.toDouble() / 5.0
                ) * 5.0 - 5.0
                ).toFloat()

        val maxTemperature = (
                ceil(
                    historyData.maxOf { it.temperature }.toDouble() / 5.0
                ) * 5.0 + 5.0
                ).toFloat()

        val minHumidity = (
                floor(
                    historyData.minOf { it.humidity }.toDouble() / 10.0
                ) * 10.0 - 10.0
                ).toFloat().coerceAtLeast(0f)

        val maxHumidity = (
                ceil(
                    historyData.maxOf { it.humidity }.toDouble() / 10.0
                ) * 10.0 + 10.0
                ).toFloat().coerceAtMost(100f)


        // =====================================================================
        // 6-3. 繪製圖例
        // Draw Legend
        // =====================================================================

        val legendY = paddingTop + dp(15f)

        // 溫度圖例
        // Temperature legend
        pointPaint.color = temperatureColor

        canvas.drawCircle(
            left,
            legendY,
            dp(4f),
            pointPaint
        )

        axisPaint.color = temperatureColor
        axisPaint.textAlign = Paint.Align.LEFT

        canvas.drawText(
            context.getString(R.string.environment_history_temperature) +
                    " (°C)",
            left + dp(8f),
            legendY + dp(4f),
            axisPaint
        )


        // 濕度圖例
        // Humidity legend
        val humidityLegendX = left + chartWidth * 0.52f

        pointPaint.color = humidityColor

        canvas.drawCircle(
            humidityLegendX,
            legendY,
            dp(4f),
            pointPaint
        )

        axisPaint.color = humidityColor
        axisPaint.textAlign = Paint.Align.LEFT

        canvas.drawText(
            context.getString(R.string.environment_history_humidity) +
                    " (%)",
            humidityLegendX + dp(8f),
            legendY + dp(4f),
            axisPaint
        )


        // =====================================================================
        // 6-4. 繪製 Y 軸刻度及水平格線
        // Draw Y-Axis Labels and Horizontal Grid Lines
        // =====================================================================

        val gridCount = 4

        for (i in 0..gridCount) {

            val ratio = i.toFloat() / gridCount

            val y = bottom - chartHeight * ratio


            // 水平格線
            // Horizontal grid line
            canvas.drawLine(
                left,
                y,
                right,
                y,
                gridPaint
            )


            // 左 Y 軸：溫度
            // Left Y-axis: Temperature
            val temperatureValue =
                minTemperature +
                        (maxTemperature - minTemperature) * ratio

            axisPaint.color = temperatureColor
            axisPaint.textAlign = Paint.Align.RIGHT

            canvas.drawText(
                String.format(
                    Locale.US,
                    "%.0f",
                    temperatureValue
                ),
                left - dp(6f),
                y + dp(4f),
                axisPaint
            )


            // 右 Y 軸：濕度
            // Right Y-axis: Humidity
            val humidityValue =
                minHumidity +
                        (maxHumidity - minHumidity) * ratio

            axisPaint.color = humidityColor
            axisPaint.textAlign = Paint.Align.LEFT

            canvas.drawText(
                String.format(
                    Locale.US,
                    "%.0f",
                    humidityValue
                ),
                right + dp(6f),
                y + dp(4f),
                axisPaint
            )
        }


        // =====================================================================
        // 6-5. 繪製 X 軸與左右 Y 軸
        // Draw X Axis and Both Y Axes
        // =====================================================================

        axisPaint.color = axisColor
        axisPaint.strokeWidth = dp(1f)

        canvas.drawLine(left, top, left, bottom, axisPaint)

        canvas.drawLine(right, top, right, bottom, axisPaint)

        canvas.drawLine(left, bottom, right, bottom, axisPaint)


        // =====================================================================
        // 6-6. 建立溫度與濕度曲線
        // Build Temperature and Humidity Paths
        // =====================================================================

        val temperaturePath = Path()

        val humidityPath = Path()

        val pointCount = historyData.size

        historyData.forEachIndexed { index, point ->

            // 計算 X 軸位置
            // Calculate X coordinate
            val xRatio = if (pointCount > 1) {
                index.toFloat() / (pointCount - 1)
            } else {
                0.5f
            }

            val x = left + chartWidth * xRatio


            // 計算溫度 Y 座標
            // Calculate temperature Y coordinate
            val temperatureRatio =
                (point.temperature - minTemperature) /
                        (maxTemperature - minTemperature)

            val temperatureY =
                bottom - chartHeight * temperatureRatio


            // 計算濕度 Y 座標
            // Calculate humidity Y coordinate
            val humidityRatio =
                (point.humidity - minHumidity) /
                        (maxHumidity - minHumidity)

            val humidityY =
                bottom - chartHeight * humidityRatio


            // 建立兩條折線
            // Build both line paths
            if (index == 0) {

                temperaturePath.moveTo(x, temperatureY)

                humidityPath.moveTo(x, humidityY)

            } else {

                temperaturePath.lineTo(x, temperatureY)

                humidityPath.lineTo(x, humidityY)
            }


            // 繪製 X 軸時間標籤
            // Draw X-axis time labels
            axisPaint.color = axisColor
            axisPaint.textAlign = Paint.Align.CENTER

            canvas.drawText(
                point.label,
                x,
                bottom + dp(17f),
                axisPaint
            )
        }


        // =====================================================================
        // 6-7. 繪製溫度與濕度曲線
        // Draw Temperature and Humidity Lines
        // =====================================================================

        canvas.drawPath(
            temperaturePath,
            temperaturePaint
        )

        canvas.drawPath(
            humidityPath,
            humidityPaint
        )


        // =====================================================================
        // 6-8. 繪製曲線上的資料點
        // Draw Data Points
        // =====================================================================

        historyData.forEachIndexed { index, point ->

            val xRatio = if (pointCount > 1) {
                index.toFloat() / (pointCount - 1)
            } else {
                0.5f
            }

            val x = left + chartWidth * xRatio


            // 溫度資料點座標
            // Temperature point coordinate
            val temperatureY =
                bottom - chartHeight *
                        (point.temperature - minTemperature) /
                        (maxTemperature - minTemperature)


            // 濕度資料點座標
            // Humidity point coordinate
            val humidityY =
                bottom - chartHeight *
                        (point.humidity - minHumidity) /
                        (maxHumidity - minHumidity)


            // 紅色溫度資料點
            // Red temperature point
            pointPaint.color = temperatureColor

            canvas.drawCircle(
                x,
                temperatureY,
                dp(3f),
                pointPaint
            )


            // 藍色濕度資料點
            // Blue humidity point
            pointPaint.color = humidityColor

            canvas.drawCircle(
                x,
                humidityY,
                dp(3f),
                pointPaint
            )
        }
    }
}