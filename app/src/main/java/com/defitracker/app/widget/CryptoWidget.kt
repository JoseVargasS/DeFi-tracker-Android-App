package com.defitracker.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.defitracker.app.R
import com.defitracker.app.domain.model.CryptoPair
import com.defitracker.app.domain.repository.CryptoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class CryptoWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CryptoWidgetEntryPoint {
        fun repository(): CryptoRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext ?: context
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            CryptoWidgetEntryPoint::class.java
        )
        val repository = entryPoint.repository()
        
        val trackedPairs = repository.getTrackedPairs().first().take(4)
        val updatedPairs = trackedPairs.map { pair ->
            try {
                val detail = repository.getPairDetail(pair.symbol, pair.source)
                pair.copy(price = detail.price, priceChangePercent = detail.priceChangePercent, isPositive = detail.isPositive)
            } catch (e: Exception) { pair }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(updatedPairs)
            }
        }
    }

    @Composable
    private fun WidgetContent(pairs: List<CryptoPair>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(R.color.dark_gray))
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DeFi Tracker Live",
                    style = TextStyle(
                        color = ColorProvider(R.color.white),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "↻",
                    modifier = GlanceModifier
                        .padding(4.dp)
                        .clickable(actionRunCallback<RefreshActionCallback>()),
                    style = TextStyle(
                        color = ColorProvider(R.color.binance_green),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(12.dp))
            if (pairs.isEmpty()) {
                Text(text = "No pairs added", style = TextStyle(color = ColorProvider(R.color.white)))
            } else {
                pairs.forEach { pair ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pair.baseAsset,
                            style = TextStyle(color = ColorProvider(R.color.white), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${pair.price}",
                                style = TextStyle(color = ColorProvider(R.color.white), fontSize = 14.sp)
                            )
                            val colorRes = if (pair.isPositive) R.color.binance_green else R.color.binance_red
                            Text(
                                text = "${if(pair.isPositive) "+" else ""}${pair.priceChangePercent}%",
                                style = TextStyle(color = ColorProvider(colorRes), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        CryptoWidget().update(context, glanceId)
    }
}

class CryptoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryptoWidget()
}
