package com.powder.simplebeertime.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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

    // ロケール対応の日付フォーマッター
    val dateFormatter = remember { createLocaleDateFormat() }

    // ページデータ生成
    val allPages: List<WeekPageData> = remember(pageCount, currentMonday, recordsByWeek, weekFields, logicalToday, dateFormatter) {
        (0 until pageCount).map { pageIndex: Int ->
            val weeksBack = pageIndex * WEEKS_PER_PAGE
            val endMonday = currentMonday.minusWeeks(weeksBack.toLong())

            val mondays: List<LocalDate> = (WEEKS_PER_PAGE - 1 downTo 0).map { back: Int ->
                endMonday.minusWeeks(back.toLong())
            }

            val values: List<Double> = mondays.map { monday: LocalDate ->
                val key = WeekKey(
                    weekBasedYear = monday.get(weekFields.weekBasedYear()),
                    weekOfWeekBasedYear = monday.get(weekFields.weekOfWeekBasedYear())
                )
                val weekRecords = recordsByWeek[key].orEmpty()
                val weekTotal = weekRecords.sumOf { it.amount }

                val daysToAverage = if (monday == currentMonday) {
                    val hasTodayRecord = weekRecords.any { record ->
                        record.timestamp.toLogicalDate(cutoffHour = 3) == logicalToday
                    }
                    if (hasTodayRecord) {
                        (logicalToday.toEpochDay() - monday.toEpochDay() + 1).toInt().coerceIn(1, 7)
                    } else {
                        (logicalToday.toEpochDay() - monday.toEpochDay()).toInt().coerceIn(1, 7)
                    }
                } else {
                    7
                }

                weekTotal / daysToAverage.toDouble()
            }

            val labels: List<String> = mondays.map { monday: LocalDate ->
                monday.format(dateFormatter)
            }

            WeekPageData(
                values = values,
                labels = labels
            )
        }.reversed()
    }

    val allValues = remember(allPages) { allPages.flatMap { it.values } }
    val allLabels = remember(allPages) { allPages.flatMap { it.labels } }

    // ★ 横スクロール（10週分の間隔を維持しつつ連続スクロール）
    val horizontalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // ★ 画面幅から10週分に相当する1ポイントあたりのdp幅を計算
    // 画面幅（パディング除く）を10等分した幅をポイント間隔とする
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    // カード内パディング(12dp*2) + 画面パディング(16dp*2) + Canvas内パディング分を考慮
    val availableWidthDp = screenWidthDp - 56.dp  // 大まかに利用可能幅
    val pointIntervalDp = (availableWidthDp / 9)  // 10ポイント→9区間
        .coerceAtLeast(28.dp)  // 最小間隔を保証

    // Canvas全体の幅（ポイント数に応じた幅）
    // paddingLeft(44f) + paddingRight(36f) をdp換算で加算
    val density = LocalDensity.current
    val canvasPaddingDp = with(density) { (44f + 36f).toDp() }
    val canvasWidthDp = if (allValues.size <= 1) {
        availableWidthDp
    } else {
        pointIntervalDp * (allValues.size - 1) + canvasPaddingDp
    }

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

    // ★ ルール①：スクロール可能
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
                .heightIn(min = 250.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.GraphBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
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

                // ★ 棒グラフタップでカレンダーへ遷移
                MonthlyBarChart(
                    values = monthlyTotals,
                    onMonthClick = { monthIndex ->
                        viewModel.requestCalendarNavigation(selectedYear, monthIndex + 1)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2つのカード間の余白
        Spacer(modifier = Modifier.height(12.dp))

        // ── 下段：週別折れ線グラフ（横スクロール＋10週間隔）──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = SimpleColors.GraphBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
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

                    // ★ 「最新」ボタン → 右端へスクロール
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SimpleColors.ButtonPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.graph_now_button),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ★ RTL対応: 下段グラフのみLTRを強制
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        WeeklyLineChart(
                            values = allValues,
                            labels = allLabels,
                            modifier = Modifier
                                .height(170.dp)
                                .width(canvasWidthDp)
                        )
                    }
                }
            }
        }

        // 画面下部の余白
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * 年ナビゲーション
 */
@Composable
private fun YearNavigationHeader(
    year: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    canGoNext: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPreviousYear) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.graph_cd_previous_year),
                tint = SimpleColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = year.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SimpleColors.TextPrimary
        )

        Spacer(modifier = Modifier.width(12.dp))

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

/**
 * 選択年の月別合計（1〜12月）を計算
 */
private fun computeMonthlyTotals(
    year: Int,
    allRecords: List<com.powder.simplebeertime.data.entity.BeerRecord>,
    logicalToday: LocalDate
): List<Double> {
    val totals = MutableList(12) { 0.0 }

    if (year > logicalToday.year) {
        return totals
    }

    val maxMonth = if (year == logicalToday.year) logicalToday.monthValue else 12

    allRecords
        .asSequence()
        .map { record ->
            val date = record.timestamp.toLogicalDate(cutoffHour = 3)
            date to record
        }
        .filter { (date, _) -> date.year == year && date.monthValue <= maxMonth }
        .forEach { (date, record) ->
            val idx = date.monthValue - 1
            totals[idx] += record.amount
        }

    return totals
}

/**
 * ★ 棒グラフの数値を白文字（黒縁取り付き）に変更
 * ★ 各月の棒グラフをタップでカレンダーに遷移
 */
@Composable
private fun MonthlyBarChart(
    values: List<Double>,
    onMonthClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safe = if (values.size == 12) values else List(12) { values.getOrElse(it) { 0.0 } }
    val maxY = (safe.maxOrNull() ?: 0.0).coerceAtLeast(4.0)
    val yMax = when {
        maxY <= 4.0 -> 4.0
        maxY <= 6.0 -> 6.0
        maxY <= 10.0 -> 10.0
        maxY <= 20.0 -> 20.0
        else -> ((maxY + 4.0) / 5.0).toInt() * 5.0
    }

    // 棒の領域を保持してタップ判定に使用
    val barRects = remember { mutableListOf<BarRect>() }

    Canvas(
        modifier = modifier
            .pointerInput(safe) {
                detectTapGestures { offset ->
                    barRects.forEachIndexed { index, rect ->
                        if (offset.x >= rect.left && offset.x <= rect.right &&
                            offset.y >= 0f && offset.y <= size.height
                        ) {
                            onMonthClick(index)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        val paddingLeft = 52f
        val paddingBottom = 45f
        val paddingTop = 35f
        val paddingRight = 20f

        val w = size.width
        val h = size.height
        val chartW = (w - paddingLeft - paddingRight).coerceAtLeast(1f)
        val chartH = (h - paddingTop - paddingBottom).coerceAtLeast(1f)

        fun yFor(value: Double): Float {
            val ratio = (value.coerceIn(0.0, yMax) / yMax).toFloat()
            return paddingTop + (chartH - chartH * ratio)
        }

        // 軸
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

        // 棒グラフ
        val barCount = 12
        val gap = 6f
        val barW = ((chartW - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)

        // ★ 数値ラベル（黒文字・縁取りなし）
        val valuePaint = android.graphics.Paint().apply {
            textSize = 26f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        // 棒の領域を記録
        barRects.clear()

        safe.forEachIndexed { i, v ->
            val ratio = (v.coerceAtLeast(0.0) / yMax).toFloat()
            val barH = chartH * ratio
            val left = paddingLeft + i * (barW + gap)
            val top = paddingTop + (chartH - barH)

            barRects.add(BarRect(left, top, left + barW, paddingTop + chartH))

            // ★ 棒グラフ（セージグリーン）
            drawRect(
                color = Color(0xFF9DB8A0),
                topLeft = Offset(left, top),
                size = Size(barW, barH)
            )

            // ★ 数値ラベル（黒文字のみ・縁取りなし）
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

/** 棒の領域を保持するデータクラス */
private data class BarRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

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
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val pointLabelOutlinePaint = android.graphics.Paint().apply {
            textSize = 22f
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

        // X軸ラベル（斜め表示で重なり防止）
        val xPaint = android.graphics.Paint().apply {
            textSize = 18f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            alpha = 180
            isAntiAlias = true
        }

        labels.forEachIndexed { i, label ->
            val x = paddingLeft + stepX * i
            val yy = paddingTop + chartH + 30f

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(-35f, x, yy)
            drawContext.canvas.nativeCanvas.drawText(label, x, yy, xPaint)
            drawContext.canvas.nativeCanvas.restore()
        }
    }
}

// ========== ロケール対応日付フォーマット ==========

/**
 * ロケールに応じた日付フォーマットを作成（グラフX軸用）
 * 日本・英語圏: M/d（例: 1/26）
 * 韓国: M.d（例: 1.26）
 * ヨーロッパ圏・アラビア語・東南アジア: d/M（例: 26/1）
 * 中国語（繁体字）: M/d（例: 1/26）
 */
private fun createLocaleDateFormat(): DateTimeFormatter {
    val locale = Locale.getDefault()

    val pattern = when (locale.language) {
        // ヨーロッパ圏: 日/月 (DD/MM)
        "de",  // ドイツ語
        "fr",  // フランス語
        "es",  // スペイン語
        "it",  // イタリア語
        "pt",  // ポルトガル語
        "tr",  // トルコ語
        "ar"   // アラビア語
            -> "d/M"

        // アジア圏（タイ、ベトナム、インドネシア）: 日/月
        "th",  // タイ語
        "vi",  // ベトナム語
        "id", "in"  // インドネシア語
            -> "d/M"

        // 韓国語: 月.日
        "ko" -> "M.d"

        // 中国語（繁体字）: 月/日
        "zh" -> "M/d"

        // 日本語、英語、その他: 月/日 (MM/DD)
        else -> "M/d"
    }

    return DateTimeFormatter.ofPattern(pattern, locale)
}
