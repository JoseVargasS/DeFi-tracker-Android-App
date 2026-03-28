package com.defitracker.app.presentation.crypto_detail

import android.graphics.Canvas
import android.graphics.Color as GraphicsColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.github.mikephil.charting.renderer.DataRenderer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDetailScreen(
    onBack: () -> Unit,
    viewModel: CryptoDetailViewModel = hiltViewModel()
) {
    val state = viewModel.state.value

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
                            text = "${state.detail?.symbol?.replace("USDT", "")}/USDT",
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
                            Text(text = "≈$${String.format("%.2f", detail.price.toDoubleOrNull() ?: 0.0)}", color = Color.Gray, fontSize = 14.sp)
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
                        StatRow("24h vol (${detail.symbol.replace("USDT", "")})", formatVol(detail.volume))
                        StatRow("24h turnover (USDT)", formatVol(detail.quoteVolume))
                    }
                }
            }

            // Interval Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val intervals = listOf("3D", "1D", "4H", "1H", "15M", "5M", "1M")
                intervals.forEach { interval ->
                    val isSelected = state.selectedInterval.uppercase() == interval
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { viewModel.loadChartData(interval.lowercase()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = interval,
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
                if (state.candles.isNotEmpty()) {
                    // Shared chart references for sync
                    val priceChartRef = remember { mutableStateOf<CombinedChart?>(null) }
                    val volumeChartRef = remember { mutableStateOf<BarChart?>(null) }
                    val stochChartRef = remember { mutableStateOf<LineChart?>(null) }

                    Box(modifier = Modifier.weight(2.5f)) {
                        PriceChart(state, priceChartRef, volumeChartRef, stochChartRef)
                    }
                    Box(modifier = Modifier.weight(0.8f)) {
                        VolumeChart(state, volumeChartRef)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StochRSIChart(state, stochChartRef)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1ECB81))
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

private fun formatDecimal(value: String): String {
    val d = value.toDoubleOrNull() ?: return value
    return String.format("%.2f", d)
}

private fun formatVol(vol: String): String {
    val v = vol.toDoubleOrNull() ?: return vol
    return when {
        v >= 1_000_000 -> String.format("%.2fM", v / 1_000_000)
        v >= 1_000 -> String.format("%.2fK", v / 1_000)
        else -> String.format("%.2f", v)
    }
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
                            val dx = kotlin.math.abs(event.x - downX)
                            val dy = kotlin.math.abs(event.y - downY)
                            
                            // If highlight is shown and we move, update it and BLOCK chart panning
                            if ((dx > 10f || dy > 10f) && highlighted != null && highlighted.isNotEmpty()) {
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
                            val dx = kotlin.math.abs(event.x - downX)
                            val dy = kotlin.math.abs(event.y - downY)
                            
                            isDragEnabled = true // Restore for next potential gesture
                            
                            // Detect a TAP
                            if (dx < 10f && dy < 10f) {
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

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val entries = stateRef.value.candles
                    if (entries.isEmpty()) return
                    
                    val visibleStart = lowestVisibleX.toInt().coerceIn(0, entries.size - 1)
                    val visibleEnd = highestVisibleX.toInt().coerceIn(0, entries.size - 1)
                    if (visibleStart >= visibleEnd) return
                    
                    val visible = entries.subList(visibleStart, (visibleEnd + 1).coerceAtMost(entries.size))
                    val maxEntry = visible.maxByOrNull { it.high } ?: return
                    val minEntry = visible.minByOrNull { it.low } ?: return

                    val trans = getTransformer(YAxis.AxisDependency.LEFT)
                    val contentLeft = viewPortHandler.contentLeft()
                    val contentRight = viewPortHandler.contentRight()
                    val contentTop = viewPortHandler.contentTop()
                    val contentBottom = viewPortHandler.contentBottom()

                    // --- Draw Max/Min Refined ---
                    // Draw Max Label
                    run {
                        val pts = floatArrayOf(entries.indexOf(maxEntry).toFloat(), maxEntry.high.toFloat())
                        trans.pointValuesToPixel(pts)
                        val px = pts[0]
                        val py = pts[1]
                        val label = String.format("%.2f", maxEntry.high)
                        val labelWidth = labelPaint.measureText(label)
                        val lineEndX = if (px + labelWidth + 16f < contentRight) px + 20f else px - 20f
                        canvas.drawLine(px, py, lineEndX, py, linePaint)
                        val textX = if (px + labelWidth + 16f < contentRight) lineEndX + 4f else lineEndX - labelWidth - 4f
                        canvas.drawText(label, textX, py + 10f, labelPaint)
                    }

                    // Draw Min Label
                    run {
                        val pts = floatArrayOf(entries.indexOf(minEntry).toFloat(), minEntry.low.toFloat())
                        trans.pointValuesToPixel(pts)
                        val px = pts[0]
                        val py = pts[1]
                        val label = String.format("%.2f", minEntry.low)
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
                    
                    val priceText = String.format("%.2f", priceAtTouch)
                    val pctText = String.format("%+.2f%%", pctFromCurrent)
                    
                    val twPrice = tagTextPaint.measureText(priceText)
                    val twPct = tagTextPaint.measureText(pctText)
                    val maxWidth = Math.max(twPrice, twPct)
                    val thY = tagTextPaint.textSize
                    val lineHeight = thY + 8f 
                    
                    val tagRight = contentRight + maxWidth + 20f
                    val chartWidth = width.toFloat()
                    val offsetRight = if (tagRight > chartWidth) tagRight - chartWidth + 4f else 0f
                    
                    val tagGap = 6f
                    val rectY = android.graphics.RectF(
                        contentRight - offsetRight, 
                        py + tagGap, 
                        contentRight + maxWidth + 20f - offsetRight, 
                        py + tagGap + (lineHeight * 2) + 4f
                    )
                    canvas.drawRoundRect(rectY, 4f, 4f, tagBackgroundPaint)
                    canvas.drawText(priceText, rectY.centerX(), py + tagGap + thY + 2f, tagTextPaint)
                    canvas.drawText(pctText, rectY.centerX(), py + tagGap + (thY * 2) + 10f, tagTextPaint)

                    // X-Axis Tag (Date)
                    val dateText = (xAxis.valueFormatter as? ValueFormatter)?.getFormattedValue(h.x) ?: ""
                    val twX = tagTextPaint.measureText(dateText)
                    val rectX = android.graphics.RectF(px - twX/2 - 10f, contentBottom, px + twX/2 + 10f, contentBottom + thY + 16f)
                    canvas.drawRoundRect(rectX, 4f, 4f, tagBackgroundPaint)
                    canvas.drawText(dateText, rectX.centerX(), rectX.centerY() + thY/3, tagTextPaint)
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
            val candleEntries = state.candles.mapIndexed { index, it ->
                CandleEntry(index.toFloat(), it.high.toFloat(), it.low.toFloat(), it.open.toFloat(), it.close.toFloat())
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                val daySdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                val hourSdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt().coerceIn(0, state.candles.size - 1)
                    if (idx < 0 || idx >= state.candles.size) return ""
                    val isDaily = state.selectedInterval.contains("d", ignoreCase = true)
                    val sdf = if (isDaily) daySdf else hourSdf
                    return sdf.format(Date(state.candles[idx].time))
                }
            }

            val combinedData = CombinedData()
            // ... BB calculation and setting data ...
            if (state.bbUpper.isNotEmpty()) {
                val upperEntries = state.bbUpper.mapIndexed { index, it ->
                    val offsetIndex = index + (state.candles.size - state.bbUpper.size)
                    Entry(offsetIndex.toFloat(), it.second.toFloat())
                }
                val middleEntries = state.bbMiddle.mapIndexed { index, it ->
                    val offsetIndex = index + (state.candles.size - state.bbMiddle.size)
                    Entry(offsetIndex.toFloat(), it.second.toFloat())
                }
                val lowerEntries = state.bbLower.mapIndexed { index, it ->
                    val offsetIndex = index + (state.candles.size - state.bbLower.size)
                    Entry(offsetIndex.toFloat(), it.second.toFloat())
                }

                val bbColor = GraphicsColor.parseColor("#FF9800")
                val middleColor = GraphicsColor.parseColor("#E91E63")

                val areaEntries = state.bbUpper.mapIndexed { index, it ->
                    val offsetIndex = index + (state.candles.size - state.bbUpper.size)
                    val lower = state.bbLower.getOrNull(index)?.second ?: it.second
                    CandleEntry(offsetIndex.toFloat(), it.second.toFloat(), lower.toFloat(), lower.toFloat(), it.second.toFloat())
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
                    isHighlightEnabled = false // Fix: Indicator area shouldn't be selectable
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
            chart.applySyncAndInitialZoom(candleEntries)
        }
    )
}

// ─── VOLUME CHART ────────────────────────────────────────────────────────────
@Composable
fun VolumeChart(state: CryptoDetailState, chartRef: MutableState<BarChart?>) {
    val stateRef = remember { mutableStateOf(state) }
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
                isDragEnabled = false
                setScaleEnabled(false)
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
            val entries = state.candles.mapIndexed { index, it ->
                BarEntry(index.toFloat(), it.volume.toFloat())
            }
            val dataset = BarDataSet(entries, "Vol").apply {
                setDrawValues(false)
                highLightAlpha = 0 // Remove default block highlight
                val colors = state.candles.map {
                    if (it.close >= it.open) GraphicsColor.parseColor("#1ECB81") else GraphicsColor.parseColor("#F6465D")
                }
                setColors(colors)
            }
            chart.data = BarData(dataset)
            chart.applySyncAndInitialZoom(state.candles)
        }
    )
}

// ─── STOCHRSI CHART ──────────────────────────────────────────────────────────
@Composable
fun StochRSIChart(state: CryptoDetailState, chartRef: MutableState<LineChart?>) {
    val stateRef = remember { mutableStateOf(state) }
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
                        val k = currentState.stochK.find { it.first.toInt() == idx }?.second
                        val d = currentState.stochD.find { it.first.toInt() == idx }?.second
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
                        val kText = "K: ${String.format(java.util.Locale.US, "%.2f", kVal)}  "
                        canvas.drawText(kText, textX, textY, kPaint)
                        val kWidth = kPaint.measureText(kText)
                        val dText = "D: ${String.format(java.util.Locale.US, "%.2f", dVal)}"
                        canvas.drawText(dText, textX + kWidth, textY, dPaint)
                    }
                }
            }.apply {
                setupCommonChartParams()
                isDragEnabled = false
                setScaleEnabled(false)
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
            val lineData = LineData()
            if (state.stochK.isNotEmpty()) {
                val kEntries = state.stochK.map { Entry(it.first.toFloat(), it.second.toFloat()) }
                val dEntries = state.stochD.map { Entry(it.first.toFloat(), it.second.toFloat()) }
                lineData.addDataSet(createBBLineDataSet(kEntries, "K", GraphicsColor.parseColor("#FF9800"), 1f, highlight = true))
                lineData.addDataSet(createBBLineDataSet(dEntries, "D", GraphicsColor.parseColor("#E91E63"), 1f, highlight = true))
            }
            chart.data = lineData
            chart.applySyncAndInitialZoom(state.candles)
        }
    )
}

// ─── SYNC HELPERS ─────────────────────────────────────────────────────────────
private fun syncSubCharts(priceChart: CombinedChart, volumeChart: BarChart?, stochChart: LineChart?) {
    val lowestX = priceChart.lowestVisibleX
    val scaleX = priceChart.viewPortHandler.scaleX

    listOf(volumeChart, stochChart).forEach { sub ->
        sub ?: return@forEach
        val vph = sub.viewPortHandler
        if (vph.scaleX != scaleX) {
            vph.setZoom(scaleX, vph.scaleY, 0f, 0f)
        }
        sub.moveViewToX(lowestX)
        syncHighlights(priceChart, volumeChart, stochChart)
    }
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
private fun BarLineChartBase<*>.setupCommonChartParams() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(true)
    setScaleXEnabled(true)
    setScaleYEnabled(false)
    setBackgroundColor(GraphicsColor.BLACK)
    setHighlightPerDragEnabled(true)
    setAutoScaleMinMaxEnabled(true)
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

// ─── INITIAL ZOOM / SCROLL ───────────────────────────────────────────────────
private fun BarLineChartBase<*>.applySyncAndInitialZoom(data: List<*>) {
    if (data.isNotEmpty() && viewPortHandler.scaleX <= 1f) {
        setVisibleXRangeMaximum(100f)
        moveViewToX((data.size - 1).toFloat())
    }
    invalidate()
}

// ─── FIX 2 & 3: OKX MARKER ───────────────────────────────────────────────────
/**
 * Full OKX-style marker: shows OHLCV card next to the candle.
 * Fix 3: accepts a lambda so it always reads the latest [CryptoDetailState].
 */
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

    private val daySdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val hourSdf = SimpleDateFormat("MM/dd, HH:mm", Locale.getDefault())

    private var lastHighlight: Highlight? = null
    override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: Highlight?) {
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
                    android.graphics.Color.parseColor("#1ECB81")
                else
                    android.graphics.Color.parseColor("#F6465D")

                val isDaily = state.selectedInterval.contains("d", ignoreCase = true)
                val sdf = if (isDaily) daySdf else hourSdf
                tvTime.text = sdf.format(Date(candle.time))
                tvOpen.text = String.format("%.2f", candle.open)
                tvHigh.text = String.format("%.2f", candle.high)
                tvLow.text = String.format("%.2f", candle.low)
                tvClose.text = String.format("%.2f", candle.close)
                tvChange.text = (if (isUp) "+" else "") + String.format("%.2f", change)
                tvChange.setTextColor(changeColor)
                tvChangePct.text = (if (isUp) "+" else "") + String.format("%.2f", changePct) + "%"
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
        v >= 1_000_000 -> String.format("%.2fM", v / 1_000_000)
        v >= 1_000 -> String.format("%.2fK", v / 1_000)
        else -> String.format("%.2f", v)
    }
}
