package com.daemon.expnesso.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daemon.expnesso.ui.theme.TextPrimary
import com.daemon.expnesso.ui.theme.TextSecondary
import com.daemon.expnesso.utils.FormatUtils

@Composable
fun CategoryDonutChart(
    categories: List<CategoryExpense>,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val totalPercentage = categories.sumOf { it.percentage }
    val proportions = if (totalPercentage > 0) {
        categories.map { it.percentage.toFloat() / totalPercentage.toFloat() }
    } else {
        emptyList()
    }

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(categories) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 32.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            
            if (categories.isEmpty() || totalPercentage == 0.0) {
                drawArc(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                return@Canvas
            }

            var startAngle = -90f
            
            categories.forEachIndexed { index, category ->
                val proportion = proportions[index]
                val sweepAngle = proportion * 360f * animatedProgress.value
                
                // Parse color from string
                val colorInt = android.graphics.Color.parseColor(category.colorString)
                val color = Color(colorInt)
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Total",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${FormatUtils.formatAmount(totalExpense)}",
                fontSize = 20.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
