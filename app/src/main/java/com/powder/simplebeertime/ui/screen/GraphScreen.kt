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
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private const val WEEKS_PER_PAGE = 10

private data class WeekKey(
    val weekBasedYear: Int,
    val weekOfWeekBasedYear: Int
)

private data class WeekPageData(
    val values: List<Double>,
    val labels: List<String>
)

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

    // 下段：週データ
    val weekFields = remember { WeekFields.ISO }
    val currentMonday = remember(logicalToday) { logicalToday.with(DayOfWeek.MONDAY) }

    // レコードを週ごとにグループ化
    val recordsByWeek: Map<WeekKey, List<com.powder.simplebeertime.data.entity.BeerRecord>> = remember(allRecords, weekFields) {
        allRecords.groupBy { record ->
            val d = record.timestamp.toLogicalDate(cutoffHour = 3)
            WeekKey(
                weekBasedYear = d.get(weekFields.weekBasedYear()),
                weekOfWeekBasedYear = d.get(weekFields.weekOfWeekBasedYear())
            )
        }
    }

    var pageCount by remember { mutableIntStateOf(1) }

    // ページデータ生成
    val allPages: List<WeekPageData> = remember(pageCount, currentMonday, recordsByWeek, weekFields) {
        (0 until pageCount).map { pageIndex: Int ->
            val weeksBack = pageIndex * WEEKS_PER_PAGE
            val endMonday = currentMonday.minusWeeks(weeksBack.toLong())

            val mondays: List<LocalDate> = (WEEKS_PER_PAGE - 1 downTo 0).map { back: Int ->
                endMonday.minusWeeks(back.toLong())
            }

            // 週ごとの平均（SUM(amount) / 7.0）
            val values: List<Double> = mondays.map { monday: LocalDate ->
                val key = WeekKey(
                    weekBasedYear = monday.get(weekFields.weekBasedYear()),
                    weekOfWeekBasedYear = monday.get(weekFields.weekOfWeekBasedYear())
                )
                val weekRecords = recordsByWeek[key].orEmpty()
                val weekTotal = weekRecords.sumOf { it.amount }
                weekTotal / 7.0
            }

            // ラベル（M/d形式）
            val fmt = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
            val labels: List<String> = mondays.map { monday: LocalDate ->
                monday.format(fmt)
            }

            WeekPageData(
                values = values,
                labels = labels
            )
        }.reversed()
    }

    val allValues = remember(allPages) { allPages.flatMap { it.values } }
    val allLabels = remember(allPages) { allPages.flatMap { it.labels } }

    // 横スクロール（週グラフ用）
    val horizontalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 初期表示時に右端（最新週）へスクロール
    LaunchedEffect(allValues.size) {
        horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
    }

    // 左端に近づいたら過去週を追加
    LaunchedEffect(horizontalScrollState.value) {
        if (horizontalScrollState.value < 100 && pageCount < 100) {
            pageCount++
        }
    }

    // 小さい画面用の保険スクロール
    val verticalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 広告スペース
        Spacer(modifier = Modifier.height(35.dp))

        // ── 上段：月別棒グラフ ──
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

                Spacer(modifier = Modifier.height(4.dp))

                YearNavigationHeader(
                    year = selectedYear,
                    onPreviousYear = { selectedYear-- },
                    onNextYear = { if (selectedYear < logicalToday.year) selectedYear++ },
                    canGoNext = selectedYear < logicalToday.year
                )

                Spacer(modifier = Modifier.height(6.dp))

                MonthlyBarChart(
                    values = monthlyTotals,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2つのカード間の余白
        Spacer(modifier = Modifier.height(12.dp))

        // ── 下段：週別折れ線グラフ ──
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

                    // Nowボタン（右端へ戻る）
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                horizontalScrollState.animateScrollTo(horizontalScrollState.maxValue)
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SimpleColors.ButtonPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.graph_now_button),
                            fontSize = 12.sp,
                            color = SimpleColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    val chartWidthPerWeek = 50.dp
                    val totalWidth = chartWidthPerWeek * allValues.size

                    WeeklyLineChart(
                        values = allValues,
                        labels = allLabels,
                        modifier = Modifier
                            .width(totalWidth.coerceAtLeast(300.dp))
                            .fillMaxHeight()
                    )
                }
            }
        }

        // bottom側の余白
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ========================================
// ロジック関数
// ========================================

/** 月別合計を計算（1〜12月） */
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

/** 週平均の「数値ラベル」だけ色分けするルール */
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
        val paddingTop = 28f
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
                radius = 5f,
                center = Offset(x, y)
            )

            // 数値ラベル（%.2f）
            val labelText = String.format(Locale.getDefault(), "%.2f", v)
            var labelY = y - 14f
            if (labelY < paddingTop + 20f) labelY = y + 28f

            pointLabelPaint.color = weeklyAvgLabelColor(v)

            // 白縁取り
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                x,
                labelY,
                pointLabelOutlinePaint
            )
            // 色文字
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                x,
                labelY,
                pointLabelPaint
            )
        }

        // X軸ラベル（常に全て表示）
        val xPaint = android.graphics.Paint().apply {
            textSize = 20f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 180
            isAntiAlias = true
        }

        labels.forEachIndexed { i, label ->
            val x = paddingLeft + stepX * i
            val yy = paddingTop + chartH + 30f
            drawContext.canvas.nativeCanvas.drawText(label, x, yy, xPaint)
        }
    }
}