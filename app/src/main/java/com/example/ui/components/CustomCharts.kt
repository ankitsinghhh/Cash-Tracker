package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.*
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlin.math.*

@Composable
fun DonutPieChart(
    items: List<CategorySpending>,
    totalAmount: Long,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier,
    onCategorySelected: (CategorySpending) -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(1f) }

    LaunchedEffect(items.map { it.category.id }) { animatedProgress.animateTo(1f, animationSpec = tween(200)) }

    val palette = remember(items) {
        items.map { CategoryIconResolver.parseColor(it.category.colorHex) }
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = offset - center
                            val distance = touchVec.getDistance()
                            val minDim = min(size.width, size.height).toFloat()
                            val minRadius = (minDim / 2f) * 0.5f
                            val maxRadius = minDim / 2f

                            if (distance in minRadius..maxRadius && items.isNotEmpty()) {
                                var touchAngle = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                                if (touchAngle < 0) touchAngle += 360f
                                // Align with chart starting angle (-90 deg = top)
                                var chartAngle = (touchAngle + 90f) % 360f

                                var currentAngle = 0f
                                for (i in items.indices) {
                                    val sweep = (items[i].percentage / 100f) * 360f
                                    if (chartAngle in currentAngle..(currentAngle + sweep)) {
                                        selectedIndex = if (selectedIndex == i) null else i
                                        selectedIndex?.let { onCategorySelected(items[it]) }
                                        break
                                    }
                                    currentAngle += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = 36.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                var startAngle = -90f

                for (i in items.indices) {
                    val item = items[i]
                    val sweepAngle = (item.percentage / 100f) * 360f * animatedProgress.value
                    val isSelected = selectedIndex == i
                    val color = palette.getOrElse(i) { Color.Gray }

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - if (items.size > 1) 2f else 0f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 8.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val selectedItem = selectedIndex?.let { items.getOrNull(it) }
                if (selectedItem != null) {
                    Text(
                        text = selectedItem.category.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = CurrencyFormatter.formatAmount(selectedItem.totalAmount, currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.getOrElse(selectedIndex ?: 0) { MaterialTheme.colorScheme.primary }
                    )
                    Text(
                        text = "${String.format("%.1f", selectedItem.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatAmount(totalAmount, currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend / Top categories list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(8).forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val color = palette.getOrElse(index) { Color.Gray }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                        .clickable {
                            selectedIndex = if (selectedIndex == index) null else index
                            onCategorySelected(item)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.category.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.transactionCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CurrencyFormatter.formatAmount(item.totalAmount, currencyCode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${String.format("%.1f", item.percentage)}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = color,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    monthlyData: List<MonthlyAggregation>,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    val displayList = remember(monthlyData) { monthlyData.take(6).reversed() }
    val maxVal = remember(displayList) {
        val maxInc = displayList.maxOfOrNull { it.totalIncome } ?: 1L
        val maxExp = displayList.maxOfOrNull { it.totalExpense } ?: 1L
        max(1L, max(maxInc, maxExp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash Flow Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IncomeGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ExpenseRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bars Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                for (item in displayList) {
                    val incHeightFraction = (item.totalIncome.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)
                    val expHeightFraction = (item.totalExpense.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Income Bar
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(incHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(IncomeGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Expense Bar
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(expHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(ExpenseRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.monthString.substring(5), // "08"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

enum class TrendChartMode {
    DAILY_BARS,
    CUMULATIVE_PACE
}

@Composable
fun DailySpendingTrendChart(
    data: DailySpendingTrendData,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier
) {
    if (data.days.isEmpty()) return

    var chartMode by rememberSaveable { mutableStateOf(TrendChartMode.DAILY_BARS) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(1f) }

    LaunchedEffect(chartMode) { animatedProgress.animateTo(1f, animationSpec = tween(200)) }

    val maxDailyExpense = remember(data.days) {
        val maxVal = data.days.maxOfOrNull { it.totalExpense } ?: 0L
        if (maxVal > 0) maxVal else 1L
    }

    val maxCumulativeExpense = remember(data.days, data.idealPaceCumulative) {
        val maxActual = data.days.lastOrNull()?.cumulativeExpense ?: 0L
        val maxIdeal = data.idealPaceCumulative.lastOrNull() ?: 0L
        max(1L, max(maxActual, maxIdeal))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val spikeColor = Color(0xFFFF7043)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Velocity & Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (chartMode == TrendChartMode.DAILY_BARS) "Daily burn & 7-day trend" else "Actual vs ideal run-rate pace",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariantColor
                    )
                }

                // Mode Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceVariantColor.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (chartMode == TrendChartMode.DAILY_BARS) primaryColor else Color.Transparent)
                            .clickable {
                                chartMode = TrendChartMode.DAILY_BARS
                                selectedPointIndex = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Daily",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (chartMode == TrendChartMode.DAILY_BARS) Color.White else onSurfaceVariantColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (chartMode == TrendChartMode.CUMULATIVE_PACE) primaryColor else Color.Transparent)
                            .clickable {
                                chartMode = TrendChartMode.CUMULATIVE_PACE
                                selectedPointIndex = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Pace",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (chartMode == TrendChartMode.CUMULATIVE_PACE) Color.White else onSurfaceVariantColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip if a day is tapped
            val selectedPoint = selectedPointIndex?.let { data.days.getOrNull(it) }
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Day ${selectedPoint.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (selectedPoint.isSpike) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = spikeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Spike",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = spikeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatAmount(selectedPoint.totalExpense, currencyCode),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedPoint.isSpike) spikeColor else primaryColor
                            )
                            Text(
                                text = "7D Avg: ${CurrencyFormatter.formatAmount(selectedPoint.movingAverageExpense, currencyCode)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariantColor
                            )
                        }
                    }
                }
            }

            // Legend Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chartMode == TrendChartMode.DAILY_BARS) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(primaryColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Daily Spend", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(spikeColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Spike", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(12.dp).height(2.dp).background(tertiaryColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("7D Trend", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(primaryColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actual Spend", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(12.dp).height(2.dp).background(onSurfaceVariantColor.copy(alpha = 0.7f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Steady Pace", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                    }
                }
            }

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(data.days, chartMode) {
                            detectTapGestures { offset ->
                                val count = data.days.size
                                if (count > 0) {
                                    val colWidth = size.width / count
                                    val tappedIndex = (offset.x / colWidth).toInt().coerceIn(0, count - 1)
                                    selectedPointIndex = if (selectedPointIndex == tappedIndex) null else tappedIndex
                                }
                            }
                        }
                ) {
                    val count = data.days.size
                    if (count <= 0) return@Canvas

                    val progress = animatedProgress.value
                    val w = size.width
                    val h = size.height - 24.dp.toPx()
                    val slotWidth = w / count

                    // Draw 2 horizontal guidelines
                    val halfH = h * 0.5f
                    drawLine(
                        color = surfaceVariantColor.copy(alpha = 0.5f),
                        start = Offset(0f, halfH),
                        end = Offset(w, halfH),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = surfaceVariantColor.copy(alpha = 0.5f),
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1.dp.toPx()
                    )

                    if (chartMode == TrendChartMode.DAILY_BARS) {
                        val barWidth = max(2.dp.toPx(), slotWidth * 0.65f)
                        val maPath = Path()
                        var maPathStarted = false

                        for (i in 0 until count) {
                            val point = data.days[i]
                            val barHeight = ((point.totalExpense.toFloat() / maxDailyExpense.toFloat()) * h * progress).coerceAtLeast(0f)
                            val x = i * slotWidth + (slotWidth - barWidth) / 2f
                            val y = h - barHeight

                            val isSelected = selectedPointIndex == i
                            val barColor = when {
                                point.isSpike -> spikeColor
                                isSelected -> primaryColor
                                else -> primaryColor.copy(alpha = 0.65f)
                            }

                            if (barHeight > 0f) {
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }

                            // Moving average spline point
                            val maHeight = ((point.movingAverageExpense.toFloat() / maxDailyExpense.toFloat()) * h * progress).coerceAtLeast(0f)
                            val maX = i * slotWidth + slotWidth / 2f
                            val maY = h - maHeight

                            if (!maPathStarted) {
                                maPath.moveTo(maX, maY)
                                maPathStarted = true
                            } else {
                                maPath.lineTo(maX, maY)
                            }
                        }

                        // Draw moving average line overlay
                        if (maPathStarted) {
                            drawPath(
                                path = maPath,
                                color = tertiaryColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    } else {
                        // CUMULATIVE_PACE mode
                        // 1. Draw dashed ideal pace reference line
                        val idealPath = Path()
                        idealPath.moveTo(slotWidth / 2f, h)
                        for (i in 0 until count) {
                            val idealVal = data.idealPaceCumulative.getOrElse(i) { 0L }
                            val idealH = ((idealVal.toFloat() / maxCumulativeExpense.toFloat()) * h).coerceAtLeast(0f)
                            val ix = i * slotWidth + slotWidth / 2f
                            val iy = h - idealH
                            idealPath.lineTo(ix, iy)
                        }
                        drawPath(
                            path = idealPath,
                            color = onSurfaceVariantColor.copy(alpha = 0.6f),
                            style = Stroke(
                                width = 1.8.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )

                        // 2. Draw actual cumulative area curve
                        val actualPath = Path()
                        val fillPath = Path()
                        val firstX = slotWidth / 2f
                        fillPath.moveTo(firstX, h)

                        for (i in 0 until count) {
                            val actualVal = data.days[i].cumulativeExpense
                            val actualH = ((actualVal.toFloat() / maxCumulativeExpense.toFloat()) * h * progress).coerceAtLeast(0f)
                            val ax = i * slotWidth + slotWidth / 2f
                            val ay = h - actualH

                            if (i == 0) {
                                actualPath.moveTo(ax, ay)
                            } else {
                                actualPath.lineTo(ax, ay)
                            }
                            fillPath.lineTo(ax, ay)
                        }

                        val lastX = (count - 1) * slotWidth + slotWidth / 2f
                        fillPath.lineTo(lastX, h)
                        fillPath.close()

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f)),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Line curve
                        drawPath(
                            path = actualPath,
                            color = primaryColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // X-Axis day labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = data.days.size
                    val labelDays = listOf(1, 5, 10, 15, 20, 25, count).filter { it in 1..count }.distinct()
                    for (d in labelDays) {
                        Text(
                            text = "$d",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = onSurfaceVariantColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Metric Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Daily Avg", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CurrencyFormatter.formatAmount(data.averageDailyExpense, currencyCode, showDecimals = false),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Peak Day", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Day ${data.peakDay}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = spikeColor
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Active Spend", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${data.activeDaysCount}/${data.days.size}d",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankedCategoryBarChart(
    items: List<CategorySpending>,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier,
    maximumAmount: Long? = null,
    showHeader: Boolean = true,
    onCategorySelected: (CategorySpending) -> Unit = {}
) {
    if (items.isEmpty()) return

    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }
    val maxAmount = remember(items, maximumAmount) { (maximumAmount ?: items.maxOfOrNull { it.totalAmount } ?: 1L).coerceAtLeast(1L) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Spend Ranking",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${items.size} Categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    val color = CategoryIconResolver.parseColor(item.category.colorHex)
                    val isExpanded = expandedCategoryId == item.category.id
                    val barFraction = (item.totalAmount.toFloat() / maxAmount.toFloat()).coerceIn(0.04f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                expandedCategoryId = if (isExpanded) null else item.category.id
                                onCategorySelected(item)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                CategoryIconBadge(
                                    iconName = item.category.iconName,
                                    colorHex = item.category.colorHex,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.transactionCount} transactions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.formatAmount(item.totalAmount, currencyCode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format("%.1f", item.percentage)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded && item.subcategoryBreakdown.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 42.dp, top = 8.dp, end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item.subcategoryBreakdown.forEach { sub ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${sub.subcategory.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatAmount(sub.totalAmount, currencyCode)} (${String.format("%.1f", sub.percentage)}%)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthOverMonthCategoryBarChart(
    comparisons: List<CategoryComparisonItem>,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier,
    maximumAmount: Long? = null,
    showHeader: Boolean = true
) {
    if (comparisons.isEmpty()) return

    val maxAmount = remember(comparisons, maximumAmount) {
        val maxVal = maximumAmount ?: comparisons.maxOfOrNull { max(it.currentMonthAmount, it.previousMonthAmount) } ?: 0L
        if (maxVal > 0) maxVal else 1L
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Shift (vs Last Month)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("This", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Last", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                comparisons.forEach { item ->
                    val curFraction = (item.currentMonthAmount.toFloat() / maxAmount.toFloat()).coerceIn(0.02f, 1f)
                    val prevFraction = (item.previousMonthAmount.toFloat() / maxAmount.toFloat()).coerceIn(0.02f, 1f)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(
                                    iconName = item.category.iconName,
                                    colorHex = item.category.colorHex,
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                            }

                            // Delta Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    item.previousMonthAmount == 0L -> MaterialTheme.colorScheme.surfaceVariant
                                    item.isIncrease -> ExpenseRed.copy(alpha = 0.14f)
                                    else -> IncomeGreen.copy(alpha = 0.14f)
                                }
                            ) {
                                val deltaText = when {
                                    item.previousMonthAmount == 0L -> "New"
                                    item.isIncrease -> "+${String.format("%.1f", item.deltaPercent)}%"
                                    else -> "${String.format("%.1f", item.deltaPercent)}%"
                                }
                                val textColor = when {
                                    item.previousMonthAmount == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                                    item.isIncrease -> ExpenseRed
                                    else -> IncomeGreen
                                }
                                Text(
                                    text = deltaText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Current Month Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(curFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = CurrencyFormatter.formatAmount(item.currentMonthAmount, currencyCode, showDecimals = false),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(64.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Previous Month Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(prevFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = CurrencyFormatter.formatAmount(item.previousMonthAmount, currencyCode, showDecimals = false),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayOfWeekHabitsChart(
    habits: List<DayOfWeekSpending>,
    currencyCode: String = "INR",
    modifier: Modifier = Modifier
) {
    if (habits.isEmpty()) return

    val maxDayExpense = remember(habits) {
        val maxVal = habits.maxOfOrNull { it.totalExpense } ?: 0L
        if (maxVal > 0) maxVal else 1L
    }

    val peakDay = remember(habits) { habits.find { it.isPeakDay } }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Day-of-Week Spending Habits",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Weekly spending pattern & peak days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (peakDay != null && peakDay.totalExpense > 0L) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Peak: ${peakDay.dayName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vertical 7-Day Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                habits.forEach { day ->
                    val heightFraction = (day.totalExpense.toFloat() / maxDayExpense.toFloat()).coerceIn(0.04f, 1f)
                    val barColor = if (day.isPeakDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${String.format("%.0f", day.percentage)}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (day.isPeakDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(90.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = day.dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (day.isPeakDay) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (day.isPeakDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (peakDay != null && peakDay.totalExpense > 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You spend the most on ${peakDay.dayName}s (${CurrencyFormatter.formatAmount(peakDay.totalExpense, currencyCode, showDecimals = false)}, accounting for ${String.format("%.1f", peakDay.percentage)}% of your expenses).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

