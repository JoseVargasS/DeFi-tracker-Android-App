package com.defitracker.app.presentation.crypto_detail

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.defitracker.app.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDetailScreen(
    onBack: () -> Unit,
    viewModel: CryptoDetailViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val tradingPair = splitTradingPair(state.detail?.symbol ?: state.symbol)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tradingPair.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF000000),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF000000))
        ) {
            // Stats Header
            state.detail?.let { detail ->
                val detailPair = splitTradingPair(detail.symbol)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = "Last price", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            text = detail.price,
                            color = if (detail.isPositive) Color(0xFF1ECB81) else Color(0xFFF6465D),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = detailPair.displayName, color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (detail.isPositive) "+" else ""}${detail.priceChangePercent}%",
                                color = if (detail.isPositive) Color(0xFF1ECB81) else Color(0xFFF6465D),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        StatRow("24h high", formatDecimal(detail.highPrice))
                        StatRow("24h low", formatDecimal(detail.lowPrice))
                        StatRow("24h vol (${detailPair.baseAsset})", formatVol(detail.volume))
                        StatRow("24h turnover (${detailPair.quoteAsset})", formatVol(detail.quoteVolume))
                    }
                }
            }

            // Interval Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val intervals = listOf(
                    "1mo" to "1mo",
                    "2w" to "2w",
                    "1w" to "1w",
                    "5d" to "5d",
                    "3d" to "3d",
                    "1d" to "1d",
                    "12h" to "12h",
                    "6h" to "6h",
                    "4h" to "4h",
                    "2h" to "2h",
                    "1h" to "1h",
                    "30m" to "30m",
                    "15m" to "15m",
                    "5m" to "5m",
                    "1m" to "1m"
                )
                intervals.forEach { interval ->
                    val isSelected = state.selectedInterval == interval.second
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(36.dp)
                            .clickable { viewModel.loadChartData(interval.second) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = interval.first,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stacked Charts Area
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    state.candles.isNotEmpty() -> {
                        // Shared chart references for sync
                        val priceChartRef = remember { mutableStateOf<CombinedChart?>(null) }
                        val volumeChartRef = remember { mutableStateOf<BarChart?>(null) }
                        val stochChartRef = remember { mutableStateOf<LineChart?>(null) }

                        Box(modifier = Modifier.weight(2.5f)) {
                            PriceChart(state, priceChartRef, volumeChartRef, stochChartRef)
                        }
                        Box(modifier = Modifier.weight(0.8f)) {
                            VolumeChart(state, priceChartRef, volumeChartRef)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StochRSIChart(state, priceChartRef, stochChartRef)
                        }
                    }
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF1ECB81))
                        }
                    }
                    state.error.isNotBlank() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.error, color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No chart data available", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

private data class TradingPairParts(
    val baseAsset: String,
    val quoteAsset: String
) {
    val displayName: String = if (quoteAsset.isBlank()) baseAsset else "$baseAsset/$quoteAsset"
}

private fun splitTradingPair(symbol: String): TradingPairParts {
    if (symbol.isBlank()) return TradingPairParts("", "")

    val quoteAsset = knownQuoteAssets.firstOrNull { symbol.endsWith(it) }.orEmpty()
    return if (quoteAsset.isNotBlank() && symbol.length > quoteAsset.length) {
        TradingPairParts(
            baseAsset = symbol.removeSuffix(quoteAsset),
            quoteAsset = quoteAsset
        )
    } else {
        TradingPairParts(symbol, "")
    }
}

private val knownQuoteAssets = listOf(
    "USDT",
    "FDUSD",
    "USDC",
    "TUSD",
    "BUSD",
    "BTC",
    "ETH",
    "BNB",
    "BRL",
    "EUR",
    "TRY",
    "DAI"
)

private fun formatDecimal(value: String): String {
    val d = value.toDoubleOrNull() ?: return value
    return formatPriceForChart(d)
}

private fun formatPriceForChart(value: Double): String {
    return if (abs(value) < 1.0) {
        String.format(Locale.US, "%.4f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun String.isCalendarInterval(): Boolean {
    return this in setOf("1d", "3d", "5d", "1w", "2w", "1mo")
}

private fun List<CandleData>.spansMultipleYears(): Boolean {
    val first = firstOrNull()?.time ?: return false
    val last = lastOrNull()?.time ?: return false
    return last - first >= 365L * 24L * 60L * 60L * 1000L
}

private fun CryptoDetailState.viewportKey(): String {
    val first = candles.firstOrNull()?.time ?: 0L
    val last = candles.lastOrNull()?.time ?: 0L
    return "$symbol-${candles.size}-$first-$last"
}

private fun formatVol(vol: String): String {
    val v = vol.toDoubleOrNull() ?: return vol
    return when {
        v >= 1_000_000 -> String.format(Locale.US, "%.2fM", v / 1_000_000)
        v >= 1_000 -> String.format(Locale.US, "%.2fK", v / 1_000)
        else -> String.format(Locale.US, "%.2f", v)
    }
}

private fun List<Pair<Long, Double>>.getValueAtCandleIndex(index: Int): Double? {
    val direct = getOrNull(index)
    if (direct != null && direct.first.toInt() == index) return direct.second
    return firstOrNull { it.first.toInt() == index }?.second
}

// ─── PRICE CHART ────────────────────────────────────────────────────────────
@Composable
fun PriceChart(
    state: CryptoDetailState,
    priceChartRef: MutableState<CombinedChart?>,
    volumeChartRef: MutableState<BarChart?>,
    stochChartRef: MutableState<LineChart?>
) {
    val stateRef = remember { mutableStateOf(state) }
    val lastRenderedDataKey = remember { mutableStateOf<String?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // Subclass CombinedChart to override onDraw for max/min labels
            object : CombinedChart(context) {
                private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.WHITE
                    textSize = 28f 
                    textAlign = Paint.Align.LEFT
                }
                private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.argb(220, 255, 255, 255)
                    strokeWidth = 1.5f
                    pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                }

                private val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.BLACK
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                private val tagBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.WHITE
                }
                private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.WHITE
                    style = Paint.Style.FILL
                }
                private val dotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                private val yTagRect = RectF()
                private val xTagRect = RectF()

                private var lastTouchYPx = -1f
                private var downX = 0f
                private var downY = 0f

                override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
                    if (event == null) return false
                    
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = abs(event.x - downX)
                            val dy = abs(event.y - downY)
                            
                            // If highlight is shown and we move, update it and BLOCK chart panning
                            if (event.pointerCount == 1 && (dx > 10f || dy > 10f) && highlighted != null && highlighted.isNotEmpty()) {
                                isDragEnabled = false // Lock chart motion
                                parent.requestDisallowInterceptTouchEvent(true)
                                
                                lastTouchYPx = event.y
                                val h = getHighlightByTouchPoint(event.x, event.y)
                                if (h != null) {
                                    highlightValue(h, true)
                                }
                                invalidate()
                                return true // Consume movement to prevent chart panning
                            }
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            val dx = abs(event.x - downX)
                            val dy = abs(event.y - downY)
                            
                            isDragEnabled = true // Restore for next potential gesture
                            
                            // Detect a TAP
                            if (dx < 10f && dy < 10f) {
                                performClick()
                                if (highlighted != null && highlighted.isNotEmpty()) {
                                    // Toggle OFF
                                    highlightValue(null)
                                    lastTouchYPx = -1f
                                    syncHighlights(this, volumeChartRef.value, stochChartRef.value)
                                } else {
                                    // Toggle ON
                                    lastTouchYPx = event.y
                                    val h = getHighlightByTouchPoint(event.x, event.y)
                                    highlightValue(h, true)
                                }
                                invalidate()
                                return true
                            }
                        }
                    }
                    return super.onTouchEvent(event)
                }

                override fun performClick(): Boolean {
                    super.performClick()
                    return true
                }

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val entries = stateRef.value.candles
                    if (entries.isEmpty()) return
                    
                    val visibleStart = lowestVisibleX.toInt().coerceIn(0, entries.size - 1)
                    val visibleEnd = highestVisibleX.toInt().coerceIn(0, entries.size - 1)
                    if (visibleStart >= visibleEnd) return
                    
                    var maxIndex = visibleStart
                    var minIndex = visibleStart
                    for (index in visibleStart..visibleEnd) {
                        val entry = entries[index]
                        if (entry.high > entries[maxIndex].high) {
                            maxIndex = index
                        }
                        if (entry.low < entries[minIndex].low) {
                            minIndex = index
                        }
                    }
                    val maxEntry = entries[maxIndex]
                    val minEntry = entries[minIndex]

                    val trans = getTransformer(YAxis.AxisDependency.LEFT)
                    val contentLeft = viewPortHandler.contentLeft()
                    val contentRight = viewPortHandler.contentRight()
                    val contentTop = viewPortHandler.contentTop()
                    val contentBottom = viewPortHandler.contentBottom()

                    // --- Draw Max/Min Refined ---
                    // Draw Max Label
                    run {
                        val pts = floatArrayOf(maxIndex.toFloat(), maxEntry.high.toFloat())
                        trans.pointValuesToPixel(pts)
                        val px = pts[0]
                        val py = pts[1]
                        val label = formatPriceForChart(maxEntry.high)
                        val labelWidth = labelPaint.measureText(label)
                        val lineEndX = if (px + labelWidth + 16f < contentRight) px + 20f else px - 20f
                        canvas.drawLine(px, py, lineEndX, py, linePaint)
                        val textX = if (px + labelWidth + 16f < contentRight) lineEndX + 4f else lineEndX - labelWidth - 4f
                        canvas.drawText(label, textX, py + 10f, labelPaint)
                    }

                    // Draw Min Label
                    run {
                        val pts = floatArrayOf(minIndex.toFloat(), minEntry.low.toFloat())
                        trans.pointValuesToPixel(pts)
                        val px = pts[0]
                        val py = pts[1]
                        val label = formatPriceForChart(minEntry.low)
                        val labelWidth = labelPaint.measureText(label)
                        val lineEndX = if (px + labelWidth + 16f < contentRight) px + 20f else px - 20f
                        canvas.drawLine(px, py, lineEndX, py, linePaint)
                        val textX = if (px + labelWidth + 16f < contentRight) lineEndX + 4f else lineEndX - labelWidth - 4f
                        canvas.drawText(label, textX, py + 10f, labelPaint)
                    }

                    // --- Draw Crosshair Lines Manually (Exact Y, Snapped X) ---
                    val h = highlighted?.getOrNull(0) ?: return

                    val xPts = floatArrayOf(h.x, h.y)
                    trans.pointValuesToPixel(xPts)
                    val px = xPts[0]
                    val snappedPy = xPts[1]
                    
                    val py = if (lastTouchYPx >= 0) lastTouchYPx.coerceIn(contentTop, contentBottom) else snappedPy
                    
                    // Vertical Line
                    canvas.drawLine(px, contentTop, px, contentBottom, linePaint)
                    // Horizontal Line
                    canvas.drawLine(contentLeft, py, contentRight, py, linePaint)
                    // Intersection Dot
                    canvas.drawCircle(px, py, 6f, dotPaint)
                    canvas.drawCircle(px, py, 6f, dotOutlinePaint)

                    // Y-Axis Tag (Price + %) - Use Exact Y
                    val priceAtTouch = trans.getValuesByTouchPoint(0f, py).y
                    val currentPrice = stateRef.value.detail?.price?.toDoubleOrNull() ?: entries.lastOrNull()?.close ?: 0.0
                    val pctFromCurrent = if (currentPrice != 0.0) (priceAtTouch - currentPrice) / currentPrice * 100 else 0.0
                    
                    val priceText = formatPriceForChart(priceAtTouch)
                    val pctText = String.format(Locale.US, "%+.2f%%", pctFromCurrent)
                    
                    val twPrice = tagTextPaint.measureText(priceText)
                    val twPct = tagTextPaint.measureText(pctText)
                    val maxWidth = maxOf(twPrice, twPct)
                    val thY = tagTextPaint.textSize
                    val lineHeight = thY + 8f 
                    
                    val tagRight = contentRight + maxWidth + 20f
                    val chartWidth = width.toFloat()
                    val offsetRight = if (tagRight > chartWidth) tagRight - chartWidth + 4f else 0f
                    
                    val tagGap = 6f
                    yTagRect.set(
                        contentRight - offsetRight, 
                        py + tagGap, 
                        contentRight + maxWidth + 20f - offsetRight, 
                        py + tagGap + (lineHeight * 2) + 4f
                    )
                    canvas.drawRoundRect(yTagRect, 4f, 4f, tagBackgroundPaint)
                    canvas.drawText(priceText, yTagRect.centerX(), py + tagGap + thY + 2f, tagTextPaint)
                    canvas.drawText(pctText, yTagRect.centerX(), py + tagGap + (thY * 2) + 10f, tagTextPaint)

                    // X-Axis Tag (Date)
                    val dateText = xAxis.valueFormatter.getFormattedValue(h.x)
                    val twX = tagTextPaint.measureText(dateText)
                    xTagRect.set(px - twX/2 - 10f, contentBottom, px + twX/2 + 10f, contentBottom + thY + 16f)
                    canvas.drawRoundRect(xTagRect, 4f, 4f, tagBackgroundPaint)
                    canvas.drawText(dateText, xTagRect.centerX(), xTagRect.centerY() + thY/3, tagTextPaint)
                }
            }.apply {
                setupCommonChartParams()
                marker = OKXChartMarker(context) { stateRef.value }
                
                axisRight.apply {
                    isEnabled = true
                    setDrawLabels(false)
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }

                axisLeft.apply {
                    setLabelCount(6, false)
                    setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatPriceForChart(value.toDouble())
                        }
                    }
                }

                onChartGestureListener = object : OnChartGestureListener {
                    override fun onChartGestureStart(me: android.view.MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                    override fun onChartGestureEnd(me: android.view.MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                    override fun onChartLongPressed(me: android.view.MotionEvent?) {}
                    override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}
                    override fun onChartSingleTapped(me: android.view.MotionEvent?) {}
                    override fun onChartFling(me1: android.view.MotionEvent?, me2: android.view.MotionEvent?, velocityX: Float, velocityY: Float) {}

                    override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {
                        syncSubCharts(this@apply, volumeChartRef.value, stochChartRef.value)
                    }
                    override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {
                        syncSubCharts(this@apply, volumeChartRef.value, stochChartRef.value)
                    }
                }

                setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        syncHighlights(this@apply, volumeChartRef.value, stochChartRef.value)
                    }
                    override fun onNothingSelected() {
                        syncHighlights(this@apply, volumeChartRef.value, stochChartRef.value)
                    }
                })

                priceChartRef.value = this
            }
        },
        update = { chart ->
            stateRef.value = state
            val viewportKey = state.viewportKey()
            val isNewDataset = lastRenderedDataKey.value != viewportKey

            chart.xAxis.applyTimeAxis(state)

            if (isNewDataset) {
                val candleEntries = ArrayList<CandleEntry>(state.candles.size)
                state.candles.forEachIndexed { index, candle ->
                    candleEntries.add(
                        CandleEntry(
                            index.toFloat(),
                            candle.high.toFloat(),
                            candle.low.toFloat(),
                            candle.open.toFloat(),
                            candle.close.toFloat()
                        )
                    )
                }
                val combinedData = CombinedData()
                if (state.bbUpper.isNotEmpty()) {
                    val upperEntries = ArrayList<Entry>(state.bbUpper.size)
                    state.bbUpper.forEachIndexed { index, value ->
                        val offsetIndex = index + (state.candles.size - state.bbUpper.size)
                        upperEntries.add(Entry(offsetIndex.toFloat(), value.second.toFloat()))
                    }
                    val middleEntries = ArrayList<Entry>(state.bbMiddle.size)
                    state.bbMiddle.forEachIndexed { index, value ->
                        val offsetIndex = index + (state.candles.size - state.bbMiddle.size)
                        middleEntries.add(Entry(offsetIndex.toFloat(), value.second.toFloat()))
                    }
                    val lowerEntries = ArrayList<Entry>(state.bbLower.size)
                    state.bbLower.forEachIndexed { index, value ->
                        val offsetIndex = index + (state.candles.size - state.bbLower.size)
                        lowerEntries.add(Entry(offsetIndex.toFloat(), value.second.toFloat()))
                    }
                    val bbColor = GraphicsColor.parseColor("#FF9800")
                    val middleColor = GraphicsColor.parseColor("#E91E63")
                    val areaEntries = ArrayList<CandleEntry>(state.bbUpper.size)
                    state.bbUpper.forEachIndexed { index, value ->
                        val offsetIndex = index + (state.candles.size - state.bbUpper.size)
                        val lower = state.bbLower.getOrNull(index)?.second ?: value.second
                        areaEntries.add(
                            CandleEntry(
                                offsetIndex.toFloat(),
                                value.second.toFloat(),
                                lower.toFloat(),
                                lower.toFloat(),
                                value.second.toFloat()
                            )
                        )
                    }
                    val areaDs = CandleDataSet(areaEntries, "BBArea").apply {
                        axisDependency = YAxis.AxisDependency.LEFT
                        setDrawValues(false)
                        shadowWidth = 0f
                        barSpace = 0f
                        val fillColor = GraphicsColor.argb(15, 255, 152, 0)
                        decreasingColor = fillColor
                        increasingColor = fillColor
                        decreasingPaintStyle = Paint.Style.FILL
                        increasingPaintStyle = Paint.Style.FILL
                        isHighlightEnabled = false
                        setDrawHorizontalHighlightIndicator(false)
                        setDrawVerticalHighlightIndicator(false)
                    }
                    val candleDataSet = createCandleDataSet(candleEntries)
                    val candleData = CandleData(candleDataSet)
                    candleData.addDataSet(areaDs)
                    combinedData.setData(candleData)
                    val lineData = LineData()
                    lineData.addDataSet(createBBLineDataSet(upperEntries, "Upper", bbColor))
                    lineData.addDataSet(createBBLineDataSet(lowerEntries, "Lower", bbColor))
                    lineData.addDataSet(createBBLineDataSet(middleEntries, "Middle", middleColor, 1.2f))
                    combinedData.setData(lineData)
                } else {
                    combinedData.setData(CandleData(createCandleDataSet(candleEntries)))
                }
                chart.data = combinedData
            } else if (state.candles.isNotEmpty()) {
                updateLastCandleInPlace(chart, state)
            }

            chart.applySyncAndInitialZoom(state.candles, resetViewport = isNewDataset, onPositioned = {
                syncSubCharts(chart, volumeChartRef.value, stochChartRef.value)
            })
            if (isNewDataset) {
                lastRenderedDataKey.value = viewportKey
            }
        }
    )
}

// ─── VOLUME CHART ────────────────────────────────────────────────────────────
@Composable
fun VolumeChart(
    state: CryptoDetailState,
    priceChartRef: MutableState<CombinedChart?>,
    chartRef: MutableState<BarChart?>
) {
    val stateRef = remember { mutableStateOf(state) }
    val lastRenderedDataKey = remember { mutableStateOf<String?>(null) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            object : BarChart(context) {
                private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.argb(220, 255, 255, 255)
                    strokeWidth = 1.5f
                    pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                }
                private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.WHITE
                    textSize = context.resources.displayMetrics.density * 10f
                    textAlign = Paint.Align.LEFT
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                
                private fun formatVolDouble(v: Double): String = when {
                    v >= 1_000_000 -> String.format(Locale.US, "%.2fM", v / 1_000_000)
                    v >= 1_000 -> String.format(Locale.US, "%.2fK", v / 1_000)
                    else -> String.format(Locale.US, "%.2f", v)
                }

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val h = highlighted?.getOrNull(0)
                    val currentState = stateRef.value
                    if (currentState.candles.isEmpty()) return
                    
                    val density = context.resources.displayMetrics.density
                    val textX = viewPortHandler.contentLeft() + (density * 5f)
                    val textY = viewPortHandler.contentTop() + (density * 14f)
                    
                    if (h != null) {
                        val xPts = floatArrayOf(h.x, 0f)
                        getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(xPts)
                        val px = xPts[0]
                        canvas.drawLine(px, viewPortHandler.contentTop(), px, viewPortHandler.contentBottom(), linePaint)
                        
                        val idx = h.x.toInt()
                        if (idx in currentState.candles.indices) {
                            val vol = currentState.candles[idx].volume
                            canvas.drawText("Vol: ${formatVolDouble(vol)}", textX, textY, textPaint)
                        }
                    } else {
                        val last = currentState.candles.lastOrNull()
                        if (last != null) {
                            canvas.drawText("Vol: ${formatVolDouble(last.volume)}", textX, textY, textPaint)
                        }
                    }
                }
            }.apply {
                setupCommonChartParams()
                setTouchEnabled(false)
                isDragEnabled = false
                setScaleEnabled(true)
                isScaleYEnabled = false
                setPinchZoom(false)
                axisLeft.apply {
                    setLabelCount(2, false)
                    setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                    spaceTop = 35f
                }
                chartRef.value = this
            }
        },
        update = { chart ->
            stateRef.value = state
            val viewportKey = state.viewportKey()
            val isNewDataset = lastRenderedDataKey.value != viewportKey

            if (isNewDataset) {
                val entries = ArrayList<BarEntry>(state.candles.size)
                val colors = ArrayList<Int>(state.candles.size)
                state.candles.forEachIndexed { index, candle ->
                    entries.add(BarEntry(index.toFloat(), candle.volume.toFloat()))
                    colors.add(
                        if (candle.close >= candle.open) {
                            GraphicsColor.parseColor("#1ECB81")
                        } else {
                            GraphicsColor.parseColor("#F6465D")
                        }
                    )
                }
                val dataset = BarDataSet(entries, "Vol").apply {
                    setDrawValues(false)
                    highLightAlpha = 0
                    setColors(colors)
                }
                chart.data = BarData(dataset)
            } else if (state.candles.isNotEmpty()) {
                updateLastVolumeInPlace(chart, state)
            }

            chart.xAxis.applyTimeAxis(state)
            chart.xAxis.setDrawLabels(false)
            chart.applySyncAndInitialZoom(state.candles, resetViewport = isNewDataset, skipPositioning = isNewDataset)
            priceChartRef.value?.let { chart.syncViewportFrom(it) }
            if (isNewDataset) {
                lastRenderedDataKey.value = viewportKey
            }
        }
    )
}

// ─── STOCHRSI CHART ──────────────────────────────────────────────────────────
@Composable
fun StochRSIChart(
    state: CryptoDetailState,
    priceChartRef: MutableState<CombinedChart?>,
    chartRef: MutableState<LineChart?>
) {
    val stateRef = remember { mutableStateOf(state) }
    val lastRenderedDataKey = remember { mutableStateOf<String?>(null) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            object : LineChart(context) {
                private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.argb(220, 255, 255, 255)
                    strokeWidth = 1.5f
                    pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                }
                private val kPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.parseColor("#FF9800")
                    textSize = context.resources.displayMetrics.density * 10f
                    textAlign = Paint.Align.LEFT
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                private val dPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = GraphicsColor.parseColor("#E91E63")
                    textSize = context.resources.displayMetrics.density * 10f
                    textAlign = Paint.Align.LEFT
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val h = highlighted?.getOrNull(0)
                    val currentState = stateRef.value
                    
                    val density = context.resources.displayMetrics.density
                    val textX = viewPortHandler.contentLeft() + (density * 5f)
                    val textY = viewPortHandler.contentTop() + (density * 14f)
                    
                    var kVal = 0.0
                    var dVal = 0.0
                    var hasVal = false
                    
                    if (h != null) {
                        val xPts = floatArrayOf(h.x, 0f)
                        getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(xPts)
                        val px = xPts[0]
                        canvas.drawLine(px, viewPortHandler.contentTop(), px, viewPortHandler.contentBottom(), linePaint)
                        
                        val idx = h.x.toInt()
                        val k = currentState.stochK.getValueAtCandleIndex(idx)
                        val d = currentState.stochD.getValueAtCandleIndex(idx)
                        if (k != null && d != null) {
                            kVal = k
                            dVal = d
                            hasVal = true
                        }
                    } else {
                        val lastK = currentState.stochK.lastOrNull()
                        val lastD = currentState.stochD.lastOrNull()
                        if (lastK != null && lastD != null) {
                            kVal = lastK.second
                            dVal = lastD.second
                            hasVal = true
                        }
                    }
                    
                    if (hasVal) {
                        val kText = "K: ${String.format(Locale.US, "%.2f", kVal)}  "
                        canvas.drawText(kText, textX, textY, kPaint)
                        val kWidth = kPaint.measureText(kText)
                        val dText = "D: ${String.format(Locale.US, "%.2f", dVal)}"
                        canvas.drawText(dText, textX + kWidth, textY, dPaint)
                    }
                }
            }.apply {
                setupCommonChartParams()
                setTouchEnabled(false)
                isDragEnabled = false
                setScaleEnabled(true)
                isScaleYEnabled = false
                setPinchZoom(false)
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 135f
                    setLabelCount(3, true)
                    setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                }
                chartRef.value = this
            }
        },
        update = { chart ->
            stateRef.value = state
            val viewportKey = state.viewportKey()
            val isNewDataset = lastRenderedDataKey.value != viewportKey

            if (isNewDataset) {
                val lineData = LineData()
                if (state.stochK.isNotEmpty()) {
                    val kEntries = ArrayList<Entry>(state.stochK.size)
                    state.stochK.forEach { value ->
                        kEntries.add(Entry(value.first.toFloat(), value.second.toFloat()))
                    }
                    val dEntries = ArrayList<Entry>(state.stochD.size)
                    state.stochD.forEach { value ->
                        dEntries.add(Entry(value.first.toFloat(), value.second.toFloat()))
                    }
                    lineData.addDataSet(createBBLineDataSet(kEntries, "K", GraphicsColor.parseColor("#FF9800"), 1f, highlight = true))
                    lineData.addDataSet(createBBLineDataSet(dEntries, "D", GraphicsColor.parseColor("#E91E63"), 1f, highlight = true))
                }
                chart.data = lineData
            } else if (state.stochK.isNotEmpty()) {
                updateLastStochInPlace(chart, state)
            }

            chart.xAxis.applyTimeAxis(state)
            chart.xAxis.setDrawLabels(false)
            chart.applySyncAndInitialZoom(state.candles, resetViewport = isNewDataset, skipPositioning = isNewDataset)
            priceChartRef.value?.let { chart.syncViewportFrom(it) }
            if (isNewDataset) {
                lastRenderedDataKey.value = viewportKey
            }
        }
    )
}

// ─── SYNC HELPERS ─────────────────────────────────────────────────────────────
private fun syncSubCharts(priceChart: CombinedChart, volumeChart: BarChart?, stochChart: LineChart?) {
    listOf(volumeChart, stochChart).forEach { sub ->
        sub ?: return@forEach
        sub.syncViewportFrom(priceChart)
    }
    syncHighlights(priceChart, volumeChart, stochChart)
}

private fun BarLineChartBase<*>.syncViewportFrom(source: BarLineChartBase<*>) {
    xAxis.axisMinimum = source.xAxis.axisMinimum
    xAxis.axisMaximum = source.xAxis.axisMaximum

    val sourceValues = FloatArray(9)
    source.viewPortHandler.matrixTouch.getValues(sourceValues)

    val targetMatrix = Matrix(viewPortHandler.matrixTouch)
    val targetValues = FloatArray(9)
    targetMatrix.getValues(targetValues)
    targetValues[Matrix.MSCALE_X] = sourceValues[Matrix.MSCALE_X]
    targetValues[Matrix.MTRANS_X] = sourceValues[Matrix.MTRANS_X]
    targetValues[Matrix.MSKEW_X] = 0f
    targetValues[Matrix.MSKEW_Y] = 0f
    targetValues[Matrix.MSCALE_Y] = 1f
    targetValues[Matrix.MTRANS_Y] = 0f
    targetMatrix.setValues(targetValues)

    viewPortHandler.refresh(targetMatrix, this, true)
}

private fun syncHighlights(priceChart: CombinedChart, volumeChart: BarChart?, stochChart: LineChart?) {
    val h = priceChart.highlighted?.getOrNull(0)
    listOf(volumeChart, stochChart).forEach { sub ->
        if (h != null) {
            sub?.highlightValue(h.x, 0, false)
        } else {
            sub?.highlightValues(null)
        }
    }
}


// ─── COMMON CHART SETUP ──────────────────────────────────────────────────────
private fun XAxis.applyTimeAxis(state: CryptoDetailState) {
    valueFormatter = object : ValueFormatter() {
        private val daySdf = SimpleDateFormat("MM/dd", Locale.US)
        private val yearSdf = SimpleDateFormat("yyyy/MM", Locale.US)
        private val hourSdf = SimpleDateFormat("MM/dd HH:mm", Locale.US)

        override fun getFormattedValue(value: Float): String {
            if (state.candles.isEmpty()) return ""
            val idx = value.toInt().coerceIn(0, state.candles.size - 1)
            val sdf = when {
                state.candles.spansMultipleYears() -> yearSdf
                state.selectedInterval.isCalendarInterval() -> daySdf
                else -> hourSdf
            }
            return sdf.format(Date(state.candles[idx].time))
        }
    }

    val spansYears = state.candles.spansMultipleYears()
    val isCalendar = state.selectedInterval.isCalendarInterval()
    labelRotationAngle = when {
        spansYears -> -30f
        !isCalendar -> -18f
        else -> 0f
    }
    textSize = if (spansYears) 9f else 10f
    yOffset = if (labelRotationAngle == 0f) 4f else 8f
    setLabelCount(
        when {
            spansYears -> 3
            !isCalendar -> 3
            else -> 4
        },
        false
    )
}

private fun BarLineChartBase<*>.setupCommonChartParams() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(false)
    isScaleXEnabled = true
    isScaleYEnabled = true
    setBackgroundColor(GraphicsColor.BLACK)
    isHighlightPerDragEnabled = true
    isAutoScaleMinMaxEnabled = true
    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        textColor = "#ADB1B8".toColorInt()
        gridColor = "#1A1D23".toColorInt()
        setDrawLabels(true)
        setAvoidFirstLastClipping(true)
        setLabelCount(5, false) // Fix 1: Limit label count to avoid crowding
        granularity = 1f
    }
    axisLeft.textColor = "#ADB1B8".toColorInt()
    axisLeft.gridColor = "#1A1D23".toColorInt()
    axisLeft.setDrawAxisLine(false)
    axisRight.isEnabled = false
}

// ─── DATASET HELPERS ─────────────────────────────────────────────────────────
private fun createCandleDataSet(entries: List<CandleEntry>) = CandleDataSet(entries, "Klines").apply {
    shadowColor = "#F4F4F4".toColorInt()
    shadowWidth = 1f
    decreasingColor = "#F6465D".toColorInt()
    increasingColor = "#1ECB81".toColorInt()
    decreasingPaintStyle = Paint.Style.FILL
    increasingPaintStyle = Paint.Style.FILL
    setDrawValues(false)
    shadowColorSameAsCandle = true
    barSpace = 0.1f
    highLightColor = "#ADB1B8".toColorInt()
    highlightLineWidth = 1f
    setDrawHorizontalHighlightIndicator(false)
    setDrawVerticalHighlightIndicator(false)
    enableDashedHighlightLine(10f, 5f, 0f)
}

private fun createBBLineDataSet(entries: List<Entry>, label: String, color: Int, width: Float = 1f, highlight: Boolean = false) = LineDataSet(entries, label).apply {
    this.color = color
    setDrawCircles(false)
    lineWidth = width
    setDrawValues(false)
    isHighlightEnabled = highlight // Fix: Disable highlight for BB lines, but allow for indicators
    mode = LineDataSet.Mode.CUBIC_BEZIER
    highLightColor = "#ADB1B8".toColorInt()
    highlightLineWidth = 1f
    enableDashedHighlightLine(10f, 5f, 0f)
    if (highlight) {
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)
    }
}

// ─── IN-PLACE UPDATE HELPERS ─────────────────────────────────────────────────
private fun updateLastCandleInPlace(chart: CombinedChart, state: CryptoDetailState) {
    val cd = chart.data ?: return
    val last = state.candles.lastOrNull() ?: return
    val lastIdx = state.candles.size - 1
    cd.candleData?.dataSets?.forEach { ds ->
        if (ds.entryCount > lastIdx) {
            val e = ds.getEntryForIndex(lastIdx)
            e.open = last.open.toFloat()
            e.close = last.close.toFloat()
            e.high = last.high.toFloat()
            e.low = last.low.toFloat()
        }
    }
    val lineData = cd.lineData
    if (lineData != null && state.bbUpper.isNotEmpty()) {
        val bbSize = state.bbUpper.size
        val bbLastIdx = bbSize - 1
        val lastUpper = state.bbUpper.lastOrNull()?.second
        val lastMiddle = state.bbMiddle.lastOrNull()?.second
        val lastLower = state.bbLower.lastOrNull()?.second
        for (i in 0 until lineData.dataSetCount) {
            val ds = lineData.getDataSetByIndex(i)
            if (ds.entryCount > bbLastIdx && bbLastIdx >= 0) {
                val e = ds.getEntryForIndex(bbLastIdx)
                when (ds.label) {
                    "Upper" -> lastUpper?.let { e.y = it.toFloat() }
                    "Middle" -> lastMiddle?.let { e.y = it.toFloat() }
                    "Lower" -> lastLower?.let { e.y = it.toFloat() }
                }
            }
        }
    }
    chart.invalidate()
}

private fun updateLastVolumeInPlace(chart: BarChart, state: CryptoDetailState) {
    val bd = chart.data ?: return
    val last = state.candles.lastOrNull() ?: return
    val lastIdx = state.candles.size - 1
    bd.dataSets.forEach { ds ->
        if (ds.entryCount > lastIdx) {
            val e = ds.getEntryForIndex(lastIdx)
            e.y = last.volume.toFloat()
        }
    }
    chart.invalidate()
}

private fun updateLastStochInPlace(chart: LineChart, state: CryptoDetailState) {
    val ld = chart.data ?: return
    val lastK = state.stochK.lastOrNull()
    val lastD = state.stochD.lastOrNull()
    if (lastK != null && ld.dataSets.isNotEmpty()) {
        val ds = ld.getDataSetByIndex(0)
        if (ds.entryCount > 0) {
            val e = ds.getEntryForIndex(ds.entryCount - 1)
            e.y = lastK.second.toFloat()
        }
    }
    if (lastD != null && ld.dataSets.size > 1) {
        val ds = ld.getDataSetByIndex(1)
        if (ds.entryCount > 0) {
            val e = ds.getEntryForIndex(ds.entryCount - 1)
            e.y = lastD.second.toFloat()
        }
    }
    chart.invalidate()
}

// ─── INITIAL ZOOM / SCROLL ───────────────────────────────────────────────────
private fun BarLineChartBase<*>.applySyncAndInitialZoom(
    data: List<*>,
    resetViewport: Boolean = false,
    skipPositioning: Boolean = false,
    onPositioned: (() -> Unit)? = null
) {
    if (data.isEmpty()) return
    if (resetViewport) {
        notifyDataSetChanged()
        viewPortHandler.setMinimumScaleX(1f)
        viewPortHandler.setMaximumScaleX(1_000_000f)
        viewPortHandler.setMinimumScaleY(1f)
        viewPortHandler.setMaximumScaleY(1_000_000f)
        if (!skipPositioning) {
            post {
                val scaleX = data.size.toFloat() / INITIAL_VISIBLE_CANDLES
                val lastX = (data.size - 1).toFloat().coerceAtLeast(0f)
                val pts = floatArrayOf(lastX, 0f)
                getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(pts)
                val matrix = Matrix()
                matrix.postScale(scaleX, 1f, pts[0], 0f)
                viewPortHandler.refresh(matrix, this, true)
                onPositioned?.invoke()
            }
        } else {
            invalidate()
        }
    }
}

private const val INITIAL_VISIBLE_CANDLES = 100f

// ─── FIX 2 & 3: OKX MARKER ───────────────────────────────────────────────────
/**
 * Full OKX-style marker: shows OHLCV card next to the candle.
 * Fix 3: accepts a lambda so it always reads the latest [CryptoDetailState].
 */
@SuppressLint("ViewConstructor", "SetTextI18n")
class OKXChartMarker(
    context: android.content.Context,
    private val stateProvider: () -> CryptoDetailState
) : MarkerView(context, R.layout.chart_marker) {

    private val tvTime: TextView = findViewById(R.id.tvTime)
    private val tvOpen: TextView = findViewById(R.id.tvOpen)
    private val tvHigh: TextView = findViewById(R.id.tvHigh)
    private val tvLow: TextView = findViewById(R.id.tvLow)
    private val tvClose: TextView = findViewById(R.id.tvClose)
    private val tvChange: TextView = findViewById(R.id.tvChange)
    private val tvChangePct: TextView = findViewById(R.id.tvChangePct)
    private val tvVolume: TextView = findViewById(R.id.tvVolume)

    private val daySdf = SimpleDateFormat("MM/dd", Locale.US)
    private val yearSdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    private val hourSdf = SimpleDateFormat("MM/dd, HH:mm", Locale.US)

    private var lastHighlight: Highlight? = null
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        lastHighlight = highlight
        e?.let {
            val state = stateProvider()
            val index = it.x.toInt()
            if (index >= 0 && index < state.candles.size) {
                val candle = state.candles[index]
                val change = candle.close - candle.open
                val changePct = if (candle.open != 0.0) change / candle.open * 100 else 0.0
                val isUp = change >= 0
                val changeColor = if (isUp)
                    GraphicsColor.parseColor("#1ECB81")
                else
                    GraphicsColor.parseColor("#F6465D")

                val isDaily = state.selectedInterval.isCalendarInterval()
                val sdf = when {
                    state.candles.spansMultipleYears() -> yearSdf
                    isDaily -> daySdf
                    else -> hourSdf
                }
                tvTime.text = sdf.format(Date(candle.time))
                tvOpen.text = formatPriceForChart(candle.open)
                tvHigh.text = formatPriceForChart(candle.high)
                tvLow.text = formatPriceForChart(candle.low)
                tvClose.text = formatPriceForChart(candle.close)
                tvChange.text = (if (isUp) "+" else "") + formatPriceForChart(change)
                tvChange.setTextColor(changeColor)
                tvChangePct.text = (if (isUp) "+" else "") + String.format(Locale.US, "%.2f", changePct) + "%"
                tvChangePct.setTextColor(changeColor)
                tvVolume.text = formatVolDouble(candle.volume)
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): com.github.mikephil.charting.utils.MPPointF {
        val chart = chartView as? BarLineChartBase<*>
        val x = lastHighlight?.x ?: 0f
        val midX = if (chart != null) (chart.lowestVisibleX + chart.highestVisibleX) / 2f else 0f
        
        // If selection is in right half of screen, show marker on left, else on right
        val xOffset = if (x > midX) {
            -width.toFloat() - 40f
        } else {
            40f
        }
        
        return com.github.mikephil.charting.utils.MPPointF(xOffset, -height.toFloat() / 2f)
    }

    private fun formatVolDouble(v: Double): String = when {
        v >= 1_000_000 -> String.format(Locale.US, "%.2fM", v / 1_000_000)
        v >= 1_000 -> String.format(Locale.US, "%.2fK", v / 1_000)
        else -> String.format(Locale.US, "%.2f", v)
    }
}
