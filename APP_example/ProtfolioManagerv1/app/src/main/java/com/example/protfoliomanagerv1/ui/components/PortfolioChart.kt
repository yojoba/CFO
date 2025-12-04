package com.example.protfoliomanagerv1.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.protfoliomanagerv1.ui.theme.*

@Composable
fun PortfolioLineChart(
    data: List<Double>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CryptoDarkSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            SimpleLineChart(data = data, color = ChartLine)
        }
    }
}

@Composable
fun MiniSparkline(
    data: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val lineColor = if (isPositive) NeonGreen else ElectricRed
    
    Box(
        modifier = modifier
            .width(60.dp)
            .height(30.dp)
    ) {
        SimpleLineChart(data = data, color = lineColor)
    }
}

@Composable
fun SimpleLineChart(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (data.size < 2) return@Canvas
        
        val maxValue = data.maxOrNull() ?: 1.0
        val minValue = data.minOrNull() ?: 0.0
        val range = maxValue - minValue
        if (range == 0.0) return@Canvas
        
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minValue) / range * height).toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )
    }
}

@Composable
fun ExchangeDistributionChart(
    exchangeTotals: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    // For now, we'll create a simple visual representation
    // A full bar chart would require more complex Vico setup
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CryptoDarkSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Exchange Distribution",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val total = exchangeTotals.values.sum()
            exchangeTotals.forEach { (exchange, value) ->
                val percentage = if (total > 0) (value / total * 100) else 0.0
                ExchangeBar(
                    exchange = exchange,
                    percentage = percentage.toFloat(),
                    value = value
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExchangeBar(
    exchange: String,
    percentage: Float,
    value: Double
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = exchange.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "$${String.format("%,.2f", value)} (${String.format("%.1f", percentage)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = GradientBlue,
            trackColor = CryptoDarkSurfaceVariant,
        )
    }
}

