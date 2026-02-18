package com.defitracker.app.presentation.crypto_detail

import android.graphics.Color as GraphicsColor
import android.graphics.Paint
import android.view.LayoutInflater
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import java.text.SimpleDateFormat
import java.util.*

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
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Back",
                            modifier = Modifier.clickable { onBack() }.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${state.detail?.symbol?.replace("USDT", "")}/USDT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Star/Fav */ }) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray)
                    }
                },
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
            // OKX Style Stats Header
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
                    Box(modifier = Modifier.weight(2.5f)) {
                        PriceChart(state)
                    }
                    Box(modifier = Modifier.weight(0.8f)) {
                        VolumeChart(state)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StochRSIChart(state)
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


@Composable
fun PriceChart(state: CryptoDetailState) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            CombinedChart(context).apply {
                setupCommonChartParams()
                marker = CustomChartMarker(context, state)
                axisLeft.apply {
                    setLabelCount(6, false)
                    setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                }
            }
        },
        update = { chart ->
            val candleEntries = state.candles.mapIndexed { index, it -> 
                CandleEntry(index.toFloat(), it.high.toFloat(), it.low.toFloat(), it.open.toFloat(), it.close.toFloat()) 
            }
            
            val combinedData = CombinedData()
            
            // Bollinger Bands (OKX Professional Style)
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

                val bbColor = GraphicsColor.parseColor("#FF9800") // Reddish Yellow / Orange
                val middleColor = GraphicsColor.parseColor("#E91E63") // Red middle line

                // Area Shading (Between bands only)
                val areaEntries = state.bbUpper.mapIndexed { index, it ->
                    val offsetIndex = index + (state.candles.size - state.bbUpper.size)
                    val lower = state.bbLower.getOrNull(index)?.second ?: it.second
                    CandleEntry(offsetIndex.toFloat(), it.second.toFloat(), lower.toFloat(), lower.toFloat(), it.second.toFloat())
                }
                val areaDs = CandleDataSet(areaEntries, "BBArea").apply {
                    axisDependency = YAxis.AxisDependency.LEFT
                    setDrawValues(false)
                    shadowWidth = 0f
                    barSpace = 0f // Eliminate gaps between bars for continuous fill
                    val fillColor = GraphicsColor.argb(35, 255, 152, 0) // Light reddish yellow
                    decreasingColor = fillColor
                    increasingColor = fillColor
                    decreasingPaintStyle = Paint.Style.FILL
                    increasingPaintStyle = Paint.Style.FILL
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
            chart.drawMaxMinLabels(candleEntries)
            chart.applySyncAndInitialZoom(candleEntries)
        }
    )
}

@Composable
fun VolumeChart(state: CryptoDetailState) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            com.github.mikephil.charting.charts.BarChart(context).apply {
                setupCommonChartParams()
                axisLeft.apply {
                    setLabelCount(2, false)
                    setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                }
            }
        },
        update = { chart ->
            val entries = state.candles.mapIndexed { index, it ->
                BarEntry(index.toFloat(), it.volume.toFloat())
            }
            val dataset = BarDataSet(entries, "Vol").apply {
                setDrawValues(false)
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

@Composable
fun StochRSIChart(state: CryptoDetailState) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            com.github.mikephil.charting.charts.LineChart(context).apply {
                setupCommonChartParams()
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 100f
                    setLabelCount(3, true)
                }
            }
        },
        update = { chart ->
            val lineData = LineData()
            if (state.stochK.isNotEmpty()) {
                val kEntries = state.stochK.map { Entry(it.first.toFloat(), it.second.toFloat()) }
                val dEntries = state.stochD.map { Entry(it.first.toFloat(), it.second.toFloat()) }
                
                lineData.addDataSet(createBBLineDataSet(kEntries, "K", GraphicsColor.parseColor("#FF9800")))
                lineData.addDataSet(createBBLineDataSet(dEntries, "D", GraphicsColor.parseColor("#E91E63")))
            }
            chart.data = lineData
            chart.applySyncAndInitialZoom(state.candles)
        }
    )
}

// Helper Extensions and Functions
private fun com.github.mikephil.charting.charts.BarLineChartBase<*>.setupCommonChartParams() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(true)
    setScaleXEnabled(true)
    setScaleYEnabled(false)
    setBackgroundColor(GraphicsColor.BLACK)
    setAutoScaleMinMaxEnabled(true)
    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        textColor = "#ADB1B8".toColorInt()
        gridColor = "#1A1D23".toColorInt()
        setDrawLabels(this is com.github.mikephil.charting.charts.LineChart) // Only show labels on bottom-most chart
    }
    axisLeft.textColor = "#ADB1B8".toColorInt()
    axisLeft.gridColor = "#1A1D23".toColorInt()
    axisLeft.setDrawAxisLine(false)
    axisRight.isEnabled = false
}

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
    highlightLineWidth = 1f
    highLightColor = "#ADB1B8".toColorInt()
}

private fun createBBLineDataSet(entries: List<Entry>, label: String, color: Int, width: Float = 1f) = LineDataSet(entries, label).apply {
    this.color = color
    setDrawCircles(false)
    lineWidth = width
    setDrawValues(false)
    mode = LineDataSet.Mode.CUBIC_BEZIER
}

private fun com.github.mikephil.charting.charts.BarLineChartBase<*>.applySyncAndInitialZoom(data: List<*>) {
    if (data.isNotEmpty() && viewPortHandler.scaleX <= 1f) {
        setVisibleXRangeMaximum(100f)
        moveViewToX((data.size - 1).toFloat())
    }
    invalidate()
}

private fun CombinedChart.drawMaxMinLabels(candleEntries: List<CandleEntry>) {
    axisLeft.removeAllLimitLines()
    if (candleEntries.isNotEmpty()) {
        val visibleStart = lowestVisibleX.toInt().coerceIn(0, candleEntries.size - 1)
        val visibleEnd = highestVisibleX.toInt().coerceIn(0, candleEntries.size - 1)
        val visibleCandles = candleEntries.subList(visibleStart, (visibleEnd + 1).coerceAtMost(candleEntries.size))
        
        if (visibleCandles.isNotEmpty()) {
            val maxEntry = visibleCandles.maxByOrNull { it.high }
            val minEntry = visibleCandles.minByOrNull { it.low }

            maxEntry?.let {
                val limitLine = com.github.mikephil.charting.components.LimitLine(it.high, "— ${String.format("%.2f", it.high)}")
                limitLine.textColor = GraphicsColor.WHITE
                limitLine.textSize = 10f
                limitLine.labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                limitLine.lineColor = GraphicsColor.argb(100, 255, 255, 255)
                axisLeft.addLimitLine(limitLine)
            }
            minEntry?.let {
                val limitLine = com.github.mikephil.charting.components.LimitLine(it.low, "— ${String.format("%.2f", it.low)}")
                limitLine.textColor = GraphicsColor.WHITE
                limitLine.textSize = 10f
                limitLine.labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                limitLine.lineColor = GraphicsColor.argb(100, 255, 255, 255)
                axisLeft.addLimitLine(limitLine)
            }
        }
    }
}

class CustomChartMarker(context: android.content.Context, private val state: CryptoDetailState) : MarkerView(context, R.layout.chart_marker) {
    private val tvPrice: TextView = findViewById(R.id.tvPrice)
    private val tvTime: TextView = findViewById(R.id.tvTime)
    private val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val index = it.x.toInt()
            if (index >= 0 && index < state.candles.size) {
                val candle = state.candles[index]
                tvPrice.text = "$${candle.close}"
                tvTime.text = sdf.format(Date(candle.time))
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): com.github.mikephil.charting.utils.MPPointF {
        return com.github.mikephil.charting.utils.MPPointF(-(width / 2).toFloat(), -height.toFloat() - 10f)
    }
}
