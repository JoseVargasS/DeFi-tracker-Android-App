package com.defitracker.app.presentation.crypto_list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.defitracker.app.domain.model.CryptoPair

@Composable
fun CryptoPairItem(
    pair: CryptoPair,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF181A20))
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = "https://assets.coincap.io/assets/icons/${pair.baseAsset.lowercase()}@2x.png",
                    contentDescription = pair.baseAsset,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pair.baseAsset,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " /${pair.quoteAsset}",
                        fontSize = 12.sp,
                        color = Color(0xFF777777),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                Text(
                    text = pair.baseAsset,
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA),
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pair.price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                val changeColor = if (pair.isPositive) Color(0xFF1ECB81) else Color(0xFFE74C4C)
                val bgColor = if (pair.isPositive) Color(0xFF1A2E1E) else Color(0xFF2E1A1A)
                
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "${if (pair.isPositive) "+" else ""}${pair.priceChangePercent}%",
                        color = changeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
