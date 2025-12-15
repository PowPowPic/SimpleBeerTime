package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors
import com.powder.simplebeertime.ui.viewmodel.BeerViewModel
import com.powder.simplebeertime.util.currentLogicalDate
import com.powder.simplebeertime.util.toLogicalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun GraphScreen(
    viewModel: BeerViewModel,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allRecords.collectAsState()
    val logicalToday = remember { currentLogicalDate(cutoffHour = 3) }

    // 上段：年選択状態
    var selectedYear by remember { mutableIntStateOf(logicalToday.year) }

    // 上段：月別合計（Double）
    val monthlyTotals = remember(allRecords, selectedYear) {
        computeMonthlyTotals(selectedYear, allRecords, logicalToday)
    }

    // 下段：直近10週の週平均
    val weekAvgList = remember(allRecords, logicalToday) {
        computeLast10WeekAverages(logicalToday, allRecords)
    }

    // 下段：週ラベル
    val weekLabels = remember(logicalToday) {
        computeLast10WeekLabels(logicalToday)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 35.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        // 🔼 上段：月別棒グラフ
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.GraphBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.graph_monthly_title),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))

                YearNavigationHeader(
                    year = selectedYear,
                    onPreviousYear = { selectedYear-- },
                    onNextYear = { if (selectedYear < logicalToday.year) selectedYear++ },
                    canGoNext = selectedYear < logicalToday.year
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MonthlyBarChart(
                        values = monthlyTotals,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 🔽 下段：週別折れ線グラフ
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.GraphBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.graph_weekly_title),
                    color = SimpleColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    WeeklyLineChart(
                        values = weekAvgList,
                        labels = weekLabels,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ========================================
// ロジック関数
// ========================================

/** 週の開始日（月曜）を求める */
private fun startOfWeekMonday(date: LocalDate): LocalDate {
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/** 直近10週の週開始日（月曜）リスト（古い→新しい順） */
private fun last10WeeksStartMondays(logicalToday: LocalDate): List<LocalDate> {
    val thisWeekStart = startOfWeekMonday(logicalToday)
    return (9 downTo 0).map { weeksAgo ->
        thisWeekStart.minusWeeks(weeksAgo.toLong())
    }
}

/** 直近10週の週平均（SUM(amount)/7.0） */
private fun computeLast10WeekAverages(
    logicalToday: LocalDate,
    records: List<com.powder.simplebeertime.data.entity.BeerRecord>
): List<Double> {
    val weekStarts = last10WeeksStartMondays(logicalToday)
    val totalsByWeekStart = weekStarts.associateWith { 0.0 }.toMutableMap()

    for (r in records) {
        val logicalDate = r.timestamp.toLogicalDate(cutoffHour = 3)
        val weekStart = startOfWeekMonday(logicalDate)
        if (totalsByWeekStart.containsKey(weekStart)) {
            totalsByWeekStart[weekStart] = (totalsByWeekStart[weekStart] ?: 0.0) + r.amount
        }
    }

    return weekStarts.map { ws ->
        val total = totalsByWeekStart[ws] ?: 0.0
        total / 7.0
    }
}

/** 直近10週のラベル（M/d形式） */
private fun computeLast10WeekLabels(
    logicalToday: LocalDate,
    locale: Locale = Locale.getDefault()
): List<String> {
    val weekStarts = last10WeeksStartMondays(logicalToday)
    val fmt = DateTimeFormatter.ofPattern("M/d", locale)
    return weekStarts.map { it.format(fmt) }
}

/** 月別合計（1〜12月） */
private fun computeMonthlyTotals(
    year: Int,
    records: List<com.powder.simplebeertime.data.entity.BeerRecord>,
    logicalToday: LocalDate
): List<Double> {
    val result = MutableList(12) { 0.0 }
    records.forEach { record ->
        val date = record.timestamp.toLogicalDate(cutoffHour = 3)
        if (date.year == year) {
            val month = date.monthValue
            result[month - 1] = result[month - 1] + record.amount
        }
    }
    return result
}

// ========================================
// UI コンポーネント
// ========================================

@Composable
private fun YearNavigationHeader(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    canGoNext: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousYear) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.graph_cd_previous_year),
                tint = SimpleColors.TextPrimary
            )
        }
        Text(
            text = year.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = SimpleColors.TextPrimary
        )
        IconButton(
            onClick = onNextYear,
            enabled = canGoNext
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.graph_cd_next_year),
                tint = if (canGoNext) SimpleColors.TextPrimary else SimpleColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MonthlyBarChart(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val safe = if (values.size >= 12) values.take(12) else values + List(12 - values.size) { 0.0 }
    val rawMax = safe.maxOrNull() ?: 0.0

    val yMax = when {
        rawMax <= 30 -> 30.0
        rawMax <= 50 -> 50.0
        rawMax <= 100 -> 100.0
        rawMax <= 150 -> 150.0
        rawMax <= 200 -> 200.0
        else -> ((rawMax + 49) / 50).toInt() * 50.0
    }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val paddingLeft = 44f
        val paddingBottom = 34f
        val paddingTop = 32f
        val paddingRight = 10f

        val w = size.width
        val h = size.height
        val chartW = (w - paddingLeft - paddingRight).coerceAtLeast(1f)
        val chartH = (h - paddingTop - paddingBottom).coerceAtLeast(1f)

        fun yFor(value: Double): Float {
            val ratio = (value.coerceIn(0.0, yMax) / yMax).toFloat()
            return paddingTop + (chartH - chartH * ratio)
        }

        // 軸線
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + chartH),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(paddingLeft, paddingTop + chartH),
            end = Offset(paddingLeft + chartW, paddingTop + chartH),
            strokeWidth = 2f
        )

        // 目盛り線
        val y0 = yFor(0.0)
        val yMid = yFor(yMax / 2)
        val yTop = yFor(yMax)

        listOf(y0, yMid, yTop).forEach { yy ->
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(paddingLeft, yy),
                end = Offset(paddingLeft + chartW, yy),
                strokeWidth = 1f
            )
        }

        // Y軸ラベル
        val yPaint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.RIGHT
            alpha = 160
            isAntiAlias = true
        }

        listOf(
            0.0 to y0,
            (yMax / 2) to yMid,
            yMax to yTop
        ).forEach { (v, yy) ->
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), "%.0f", v),
                paddingLeft - 8f,
                yy + 8f,
                yPaint
            )
        }

        // 棒グラフ
        val barCount = 12
        val gap = 6f
        val barW = ((chartW - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)

        val valuePaint = android.graphics.Paint().apply {
            textSize = 26f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 255
            isAntiAlias = true
            isFakeBoldText = true
        }

        safe.forEachIndexed { i, v ->
            val ratio = (v.coerceAtLeast(0.0) / yMax).toFloat()
            val barH = chartH * ratio
            val left = paddingLeft + i * (barW + gap)
            val top = paddingTop + (chartH - barH)

            drawRect(
                color = SimpleColors.PureBlue,
                topLeft = Offset(left, top),
                size = Size(barW, barH)
            )

            // 数値ラベル（%.1f）
            if (v > 0) {
                val labelX = left + barW / 2f
                var labelY = top - 8f
                if (labelY < paddingTop + 20f) labelY = top + 28f

                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.getDefault(), "%.1f", v),
                    labelX,
                    labelY,
                    valuePaint
                )
            }
        }

        // X軸ラベル（月）
        val xPaint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 160
            isAntiAlias = true
        }

        for (m in 1..12) {
            val x = paddingLeft + (m - 1) * (barW + gap) + barW / 2f
            val y = paddingTop + chartH + 28f
            drawContext.canvas.nativeCanvas.drawText(m.toString(), x, y, xPaint)
        }
    }
}

/** ✅ 週平均の「数値ラベル」だけ色分けするルール */
private fun weeklyAvgLabelColor(value: Double): Int {
    return when {
        value < 2.0 -> SimpleColors.PureBlue.toArgb()
        value >= 2.5 -> SimpleColors.PureRed.toArgb()
        else -> SimpleColors.TextPrimary.toArgb()
    }
}

@Composable
private fun WeeklyLineChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxY = (values.maxOrNull() ?: 0.0).coerceAtLeast(3.0)
    val yMax = when {
        maxY <= 2.0 -> 2.0
        maxY <= 3.0 -> 3.0
        maxY <= 4.0 -> 4.0
        maxY <= 5.0 -> 5.0
        else -> ((maxY + 0.9) / 1.0).toInt() * 1.0
    }

    Canvas(modifier = modifier) {
        val paddingLeft = 44f
        val paddingBottom = 48f
        val paddingTop = 36f
        val paddingRight = 36f

        val w = size.width
        val h = size.height
        val chartW = (w - paddingLeft - paddingRight).coerceAtLeast(1f)
        val chartH = (h - paddingTop - paddingBottom).coerceAtLeast(1f)

        fun yFor(value: Double): Float {
            val ratio = (value.coerceIn(0.0, yMax) / yMax).toFloat()
            return paddingTop + (chartH - chartH * ratio)
        }

        // 軸線
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + chartH),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(paddingLeft, paddingTop + chartH),
            end = Offset(paddingLeft + chartW, paddingTop + chartH),
            strokeWidth = 2f
        )

        // 目盛り線（0, yMax/2, yMax）
        val y0 = yFor(0.0)
        val yMid = yFor(yMax / 2)
        val yTop = yFor(yMax)

        listOf(y0, yMid, yTop).forEach { yy ->
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(paddingLeft, yy),
                end = Offset(paddingLeft + chartW, yy),
                strokeWidth = 1f
            )
        }

        // Y軸ラベル
        val yPaint = android.graphics.Paint().apply {
            textSize = 26f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.RIGHT
            alpha = 160
            isAntiAlias = true
        }

        listOf(
            0.0 to y0,
            (yMax / 2) to yMid,
            yMax to yTop
        ).forEach { (v, yy) ->
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), "%.1f", v),
                paddingLeft - 8f,
                yy + 8f,
                yPaint
            )
        }

        if (values.isEmpty()) return@Canvas

        val stepX = if (values.size <= 1) chartW else chartW / (values.size - 1).toFloat()

        // 折れ線
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = paddingLeft + stepX * i
            val y = yFor(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = SimpleColors.ButtonPrimary,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // ポイント＆数値ラベル
        val pointLabelPaint = android.graphics.Paint().apply {
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val pointLabelOutlinePaint = android.graphics.Paint().apply {
            textSize = 26f
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 5f
        }

        values.forEachIndexed { i, v ->
            val x = paddingLeft + stepX * i
            val y = yFor(v)

            // ポイント
            drawCircle(
                color = SimpleColors.ButtonPrimary,
                radius = 6f,
                center = Offset(x, y)
            )

            // 数値ラベル（%.2f）- 必ず表示（色はルールで変更）
            val labelText = String.format(Locale.getDefault(), "%.2f", v)
            var labelY = y - 14f
            if (labelY < paddingTop + 20f) labelY = y + 28f

            // ✅ ここで「数値ラベル色」を決める
            pointLabelPaint.color = weeklyAvgLabelColor(v)

            // 白縁取り
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                x,
                labelY,
                pointLabelOutlinePaint
            )
            // 色文字（青/赤/黒）
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                x,
                labelY,
                pointLabelPaint
            )
        }

        // X軸ラベル（週開始日）
        val xPaint = android.graphics.Paint().apply {
            textSize = 22f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 160
            isAntiAlias = true
        }

        labels.forEachIndexed { i, label ->
            val x = paddingLeft + stepX * i
            val y = paddingTop + chartH + 30f
            drawContext.canvas.nativeCanvas.drawText(label, x, y, xPaint)
        }
    }
}
