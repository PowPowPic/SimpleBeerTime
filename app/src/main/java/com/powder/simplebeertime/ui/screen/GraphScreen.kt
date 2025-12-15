package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.PathEffect
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
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private const val MAX_INTERVAL_MILLIS = 24L * 60 * 60 * 1000L
private const val WEEKS_PER_PAGE = 15

private data class WeekKey(
    val weekBasedYear: Int,
    val weekOfWeekBasedYear: Int
)

private data class WeekPageData(
    val endWeekKey: WeekKey,
    val weekKeys: List<WeekKey>,
    val mondayDates: List<LocalDate>,
    val values: List<Float>,
    val labels: List<String>,
    val meaningfulMask: List<Boolean>,
    val labelMask: List<Boolean>
)

@Composable
fun GraphScreen(
    viewModel: BeerViewModel,
    modifier: Modifier = Modifier
) {
    val allRecords by viewModel.allRecords.collectAsState()
    val logicalToday = remember { currentLogicalDate(cutoffHour = 3) }

    var selectedYear by remember { mutableIntStateOf(logicalToday.year) }

    val monthlyTotals = remember(allRecords, selectedYear) {
        viewModel.getMonthlyTotalsForYear(selectedYear)
    }

    val monthlyValues = remember(monthlyTotals) {
        monthlyTotals.sortedBy { it.month }.map { it.totalBeers.toInt() }
    }

    val weekFields = remember { WeekFields.ISO }
    val currentMonday = remember(logicalToday) { logicalToday.with(DayOfWeek.MONDAY) }
    val recordsByWeek: Map<WeekKey, List<Long>> = remember(allRecords) {
        allRecords
            .map { it.timestamp }
            .groupBy { ts ->
                val d = ts.toLogicalDate(cutoffHour = 3)
                WeekKey(
                    weekBasedYear = d.get(weekFields.weekBasedYear()),
                    weekOfWeekBasedYear = d.get(weekFields.weekOfWeekBasedYear())
                )
            }
            .mapValues { (_, list) -> list.sorted() }
    }

    var pageCount by remember { mutableIntStateOf(1) }

    val weekLabelFormat = stringResource(R.string.graph_week_label_format)

    val allPagesData: List<WeekPageData> = remember(pageCount, currentMonday, recordsByWeek, weekFields, weekLabelFormat) {
        (0 until pageCount).map { pageIndex ->
            val weeksBack = pageIndex * WEEKS_PER_PAGE
            val endMonday = currentMonday.minusWeeks(weeksBack.toLong())
            val mondayDates = (WEEKS_PER_PAGE - 1 downTo 0).map { back ->
                endMonday.minusWeeks(back.toLong())
            }
            val weekKeys = mondayDates.map { monday ->
                WeekKey(
                    weekBasedYear = monday.get(weekFields.weekBasedYear()),
                    weekOfWeekBasedYear = monday.get(weekFields.weekOfWeekBasedYear())
                )
            }
            val endWeekKey = weekKeys.last()
            val values = mondayDates.map { monday ->
                val key = WeekKey(
                    weekBasedYear = monday.get(weekFields.weekBasedYear()),
                    weekOfWeekBasedYear = monday.get(weekFields.weekOfWeekBasedYear())
                )
                val tsList = recordsByWeek[key].orEmpty()
                calculateWeeklyHours(tsList)
            }
            val labels = mondayDates.map { monday ->
                val month = monday.monthValue
                val weekOfMonth = monday.get(weekFields.weekOfMonth())
                String.format(Locale.getDefault(), weekLabelFormat, month, weekOfMonth)
            }
            val meaningfulMask = mondayDates.map { monday ->
                val key = WeekKey(
                    weekBasedYear = monday.get(weekFields.weekBasedYear()),
                    weekOfWeekBasedYear = monday.get(weekFields.weekOfWeekBasedYear())
                )
                (recordsByWeek[key]?.size ?: 0) >= 2
            }
            val labelMask = calculateLabelMask(meaningfulMask, pageIndex == 0)

            WeekPageData(
                endWeekKey = endWeekKey,
                weekKeys = weekKeys,
                mondayDates = mondayDates,
                values = values,
                labels = labels,
                meaningfulMask = meaningfulMask,
                labelMask = labelMask
            )
        }.reversed()
    }

    val allValues = remember(allPagesData) { allPagesData.flatMap { it.values } }
    val allLabels = remember(allPagesData) { allPagesData.flatMap { it.labels } }
    val allMeaningfulMask = remember(allPagesData) { allPagesData.flatMap { it.meaningfulMask } }
    val allLabelMask = remember(allMeaningfulMask) { calculateGlobalLabelMask(allMeaningfulMask) }
    val meaningfulWeeks = remember(allMeaningfulMask) { allMeaningfulMask.count { it } }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(allValues.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    LaunchedEffect(scrollState.value) {
        if (scrollState.value < 100 && pageCount < 100) {
            pageCount++
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 35.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
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
                    onNextYear = { selectedYear++ }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MonthlyBarChart(
                        values = monthlyValues,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.GraphBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.graph_weekly_title),
                        color = SimpleColors.TextPrimary
                    )
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SimpleColors.ButtonPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.graph_now_button),
                            fontSize = 12.sp,
                            color = SimpleColors.TextPrimary
                        )
                    }
                }

                if (meaningfulWeeks == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.graph_weekly_no_data),
                        fontSize = 12.sp,
                        color = SimpleColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    val chartWidthPerWeek = 40.dp
                    val totalWidth = chartWidthPerWeek * allValues.size

                    WeeklyLineChart(
                        pointsHours = allValues,
                        labels = allLabels,
                        meaningfulMask = allMeaningfulMask,
                        labelMask = allLabelMask,
                        modifier = Modifier
                            .width(totalWidth.coerceAtLeast(300.dp))
                            .fillMaxHeight()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun calculateWeeklyHours(tsList: List<Long>): Float {
    val millis = when {
        tsList.size >= 2 -> {
            val diffs = tsList.zipWithNext { a, b -> (b - a).coerceAtLeast(0L) }
                .filter { it > 0L }
            val avg = if (diffs.isNotEmpty()) {
                diffs.average().toLong()
            } else {
                MAX_INTERVAL_MILLIS
            }
            avg.coerceAtMost(MAX_INTERVAL_MILLIS)
        }
        else -> MAX_INTERVAL_MILLIS
    }
    return millis.coerceIn(0L, MAX_INTERVAL_MILLIS).toFloat() / 3_600_000f
}

private fun calculateLabelMask(meaningfulMask: List<Boolean>, isLatestPage: Boolean): List<Boolean> {
    val result = MutableList(meaningfulMask.size) { false }
    val meaningfulIndices = meaningfulMask.mapIndexedNotNull { index, isMeaningful ->
        if (isMeaningful) index else null
    }

    if (meaningfulIndices.isEmpty()) return result

    if (meaningfulIndices.size <= 3) {
        meaningfulIndices.forEach { result[it] = true }
        return result
    }

    val sortedIndices = meaningfulIndices.sortedDescending()
    sortedIndices.forEachIndexed { i, idx ->
        if (isLatestPage && i == 0) {
            result[idx] = true
        } else if (i % 2 == 0) {
            result[idx] = true
        }
    }

    return result
}

private fun calculateGlobalLabelMask(meaningfulMask: List<Boolean>): List<Boolean> {
    val result = MutableList(meaningfulMask.size) { false }
    val meaningfulIndices = meaningfulMask.mapIndexedNotNull { index, isMeaningful ->
        if (isMeaningful) index else null
    }

    if (meaningfulIndices.isEmpty()) return result

    if (meaningfulIndices.size <= 3) {
        meaningfulIndices.forEach { result[it] = true }
        return result
    }

    val sortedIndices = meaningfulIndices.sortedDescending()
    sortedIndices.forEachIndexed { i, idx ->
        if (i == 0) {
            result[idx] = true
        } else if (i % 2 == 0) {
            result[idx] = true
        }
    }

    return result
}

@Composable
private fun YearNavigationHeader(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
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
        IconButton(onClick = onNextYear) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.graph_cd_next_year),
                tint = SimpleColors.TextPrimary
            )
        }
    }
}

@Composable
private fun MonthlyBarChart(
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    val safe = if (values.size >= 12) values.take(12) else values + List(12 - values.size) { 0 }
    val rawMax = safe.maxOrNull() ?: 0

    val yMax = when {
        rawMax <= 300 -> 300
        rawMax <= 500 -> 500
        rawMax <= 800 -> 800
        rawMax <= 1000 -> 1000
        else -> ((rawMax + 199) / 200) * 200
    }.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val paddingLeft = 38f
        val paddingBottom = 34f
        val paddingTop = 24f
        val paddingRight = 10f

        val w = size.width
        val h = size.height
        val chartW = (w - paddingLeft - paddingRight).coerceAtLeast(1f)
        val chartH = (h - paddingTop - paddingBottom).coerceAtLeast(1f)

        fun yFor(value: Int): Float {
            val ratio = (value.coerceIn(0, yMax).toFloat() / yMax.toFloat())
            return paddingTop + (chartH - chartH * ratio)
        }

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

        val y0 = yFor(0)
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

        val yPaint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.RIGHT
            alpha = 160
            isAntiAlias = true
        }

        listOf(
            0 to y0,
            (yMax / 2) to yMid,
            yMax to yTop
        ).forEach { (v, yy) ->
            drawContext.canvas.nativeCanvas.drawText(
                v.toString(),
                paddingLeft - 8f,
                yy + 8f,
                yPaint
            )
        }

        val barCount = 12
        val gap = 6f
        val barW = ((chartW - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)

        val valuePaint = android.graphics.Paint().apply {
            textSize = 30f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 255
            isAntiAlias = true
            isFakeBoldText = true
        }

        safe.forEachIndexed { i, v ->
            val ratio = (v.coerceAtLeast(0).toFloat() / yMax.toFloat())
            val barH = chartH * ratio
            val left = paddingLeft + i * (barW + gap)
            val top = paddingTop + (chartH - barH)

            drawRect(
                color = SimpleColors.PureBlue,
                topLeft = Offset(left, top),
                size = Size(barW, barH)
            )

            if (v > 0) {
                val labelX = left + barW / 2f
                var labelY = top - 8f

                if (labelY < paddingTop + 20f) {
                    labelY = top + 32f
                }

                drawContext.canvas.nativeCanvas.drawText(
                    v.toString(),
                    labelX,
                    labelY,
                    valuePaint
                )
            }
        }

        val xPaint = android.graphics.Paint().apply {
            textSize = 26f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 160
            isAntiAlias = true
        }

        for (m in 1..12) {
            val x = paddingLeft + (m - 1) * (barW + gap) + barW / 2f
            val y = paddingTop + chartH + 30f
            drawContext.canvas.nativeCanvas.drawText(m.toString(), x, y, xPaint)
        }
    }
}

@Composable
private fun WeeklyLineChart(
    pointsHours: List<Float>,
    labels: List<String>,
    meaningfulMask: List<Boolean>,
    labelMask: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val maxH = 24f
    val safePoints = pointsHours

    val label0 = stringResource(R.string.graph_axis_time_0)
    val label12 = stringResource(R.string.graph_axis_time_12)
    val label24 = stringResource(R.string.graph_axis_time_24)

    Canvas(modifier = modifier) {
        val paddingLeft = 44f
        val paddingBottom = 48f
        val paddingTop = 28f
        val paddingRight = 36f

        val w = size.width
        val h = size.height
        val chartW = (w - paddingLeft - paddingRight).coerceAtLeast(1f)
        val chartH = (h - paddingTop - paddingBottom).coerceAtLeast(1f)

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

        fun yFor(hh: Float): Float {
            val ratio = hh.coerceIn(0f, maxH) / maxH
            return paddingTop + (chartH - chartH * ratio)
        }

        val y0 = yFor(0f)
        val y12 = yFor(12f)
        val y24 = yFor(24f)

        listOf(y0, y12, y24).forEach { yy ->
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(paddingLeft, yy),
                end = Offset(paddingLeft + chartW, yy),
                strokeWidth = 1f
            )
        }

        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(paddingLeft, y24),
            end = Offset(paddingLeft + chartW, y24),
            strokeWidth = 1.5f,
            pathEffect = dash
        )

        val yPaint = android.graphics.Paint().apply {
            textSize = 26f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.RIGHT
            alpha = 160
            isAntiAlias = true
        }

        listOf(
            label0 to y0,
            label12 to y12,
            label24 to y24
        ).forEach { (text, yy) ->
            drawContext.canvas.nativeCanvas.drawText(
                text,
                paddingLeft - 8f,
                yy + 8f,
                yPaint
            )
        }

        if (safePoints.isEmpty()) return@Canvas

        val stepX = if (safePoints.size <= 1) chartW else chartW / (safePoints.size - 1).toFloat()

        val path = Path()
        safePoints.forEachIndexed { i, hh ->
            val x = paddingLeft + stepX * i
            val y = yFor(hh)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = SimpleColors.ButtonPrimary,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        val pointLabelPaint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val pointLabelOutlinePaint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
        }

        safePoints.forEachIndexed { i, hh ->
            val x = paddingLeft + stepX * i
            val y = yFor(hh)

            val isMeaningful = meaningfulMask.getOrElse(i) { false }
            val pointColor = if (hh >= 24f) {
                Color(0xFF2E7D32)
            } else {
                SimpleColors.ButtonPrimary
            }

            drawCircle(
                color = pointColor,
                radius = 5f,
                center = Offset(x, y)
            )

            val showLabel = labelMask.getOrElse(i) { false }
            if (isMeaningful && showLabel && hh < 24f) {
                val totalMinutes = (hh * 60).toInt()
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                val labelText = "$hours:${minutes.toString().padStart(2, '0')}"

                var labelY = y - 14f

                if (labelY < paddingTop + 20f) {
                    labelY = y + 32f
                }

                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    x,
                    labelY,
                    pointLabelOutlinePaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    x,
                    labelY,
                    pointLabelPaint
                )
            }
        }

        val labelStep = when {
            labels.size > 60 -> 8
            labels.size > 45 -> 6
            labels.size > 30 -> 4
            labels.size > 20 -> 3
            labels.size > 12 -> 2
            else -> 1
        }

        val xPaint = android.graphics.Paint().apply {
            textSize = 22f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 160
            isAntiAlias = true
        }

        labels.forEachIndexed { i, label ->
            if (i % labelStep != 0) return@forEachIndexed
            val x = paddingLeft + stepX * i
            val y = paddingTop + chartH + 30f
            drawContext.canvas.nativeCanvas.drawText(label, x, y, xPaint)
        }
    }
}