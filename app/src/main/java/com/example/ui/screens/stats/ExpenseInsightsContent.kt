@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.domain.CurrencyFormatter
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FullscreenableChart
import com.example.ui.components.ChartFullscreenButton
import com.example.ui.components.chartPlotHeight
import com.example.ui.theme.FinancialColors
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.math.roundToInt

fun LazyListScope.expenseInsightItems(data: ExpenseInsights, visible: Set<InsightWidget>,
                                     onThreshold: (Long) -> Unit, onTransaction: (Long) -> Unit) {
    item("insights_context") {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(data.monthLabel, style = MaterialTheme.typography.headlineSmall)
            Text("Recorded spending · ${data.currency} · ${data.comparedDays} elapsed days", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Excluded records and transfer principal are omitted; transfer fees count as spending. Charts use up to two calendar years of records.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (data.omittedCurrencyRecords > 0) Text("${data.omittedCurrencyRecords} records in other currencies or without an account are omitted. Change your primary currency in Settings to explore another currency.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(InsightWidget.entries.filter { it.isNew && it in visible }, key = { it.name }, contentType = { "insight" }) { widget ->
        FullscreenableChart("insight_${data.monthStartMillis}_${data.currency}_${widget.name}", "${widget.title} · ${data.monthLabel}", showButton = false) {
        when (widget) {
            InsightWidget.HEATMAP -> SpendingHeatmap(data, onTransaction)
            InsightWidget.CATEGORY_TRENDS -> CategoryTrends(data)
            InsightWidget.SPENDING_CHANGE -> SpendingChange(data)
            InsightWidget.RECURRING_TREND -> RecurringTrend(data)
            InsightWidget.SMALL_PURCHASES -> SmallPurchases(data, onThreshold)
            InsightWidget.FREQUENCY -> PurchaseFrequency(data)
            InsightWidget.MERCHANT_HABITS -> MerchantHabits(data)
            InsightWidget.SPIKES -> SpikeExplanations(data, onTransaction)
            InsightWidget.SEASONALITY -> Seasonality(data)
            InsightWidget.SAVINGS_WHAT_IF -> SavingsWhatIf(data)
            InsightWidget.WEEKLY_DIGEST -> WeeklyDigest(data, onTransaction)
            else -> Unit
        }
        }
    }
}

@Composable
private fun InsightCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ChartFullscreenButton()
            }
            content()
        }
    }
}

private fun money(amount: Long, currency: String) = CurrencyFormatter.formatAmount(amount, currency)
private fun signedMoney(amount: Long, currency: String) = (if (amount > 0) "+" else "") + money(amount, currency)
private fun percentage(part: Long, total: Long) = if (total <= 0L) "0%" else String.format(Locale.getDefault(), "%.1f%%", part.toDouble() * 100 / total)

@Composable
private fun Figure(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ContributionRows(contributions: List<ExpenseContribution>, currency: String, onTransaction: (Long) -> Unit) {
    contributions.forEach { entry ->
        Row(Modifier.fillMaxWidth().clickable { onTransaction(entry.id) }.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(money(entry.amount, currency), style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
        }
    }
}

@Composable
fun SpendingHeatmap(data: ExpenseInsights, onTransaction: (Long) -> Unit) {
    var selectedDay by rememberSaveable(data.monthStartMillis) { mutableIntStateOf(1) }
    val maximum = data.days.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    val primary = MaterialTheme.colorScheme.primary
    InsightCard("Spending heatmap", "Tap a day. Darker cells show higher recorded spending.") {
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        val rows = (data.sundayOffset + data.daysInMonth + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { col ->
                    val index = row * 7 + col - data.sundayOffset
                    val day = data.days.getOrNull(index)
                    if (day == null) Spacer(Modifier.weight(1f).height(48.dp)) else {
                        val isFuture = day.day > data.comparedDays
                        val color = if (day.amount == 0L) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            else primary.copy(alpha = 0.09f + 0.25f * day.amount.toFloat() / maximum)
                        Surface(onClick = { selectedDay = day.day }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp), color = color,
                            border = if (selectedDay == day.day) BorderStroke(1.5.dp, primary) else null) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics {
                                contentDescription = "Day ${day.day}, ${if (isFuture) "not elapsed" else money(day.amount, data.currency)}"
                            }, contentAlignment = Alignment.Center) {
                                Text("${day.day}", style = MaterialTheme.typography.bodySmall,
                                    color = if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.bodySmall)
            listOf(0.09f, 0.18f, 0.27f, 0.34f).forEach { Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(primary.copy(alpha = it))) }
            Text("More", style = MaterialTheme.typography.bodySmall)
        }
        data.days.getOrNull(selectedDay - 1)?.let { day ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Text("Day ${day.day} · ${money(day.amount, data.currency)} · ${day.count} spending records", style = MaterialTheme.typography.titleSmall)
            if (day.day > data.comparedDays) Text("This day has not elapsed. Future entries are omitted.", style = MaterialTheme.typography.bodySmall)
            else if (day.count == 0) Text("No recorded spending on this day.", style = MaterialTheme.typography.bodySmall)
            ContributionRows(day.contributors, data.currency, onTransaction)
            if (day.count > day.contributors.size) Text("Largest five records shown.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class BarDatum(val label: String, val values: List<Long>)

@Composable
private fun chartColors(): List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary, FinancialColors.transfer, FinancialColors.income, FinancialColors.expense)

@Composable
private fun Legend(labels: List<String>, colors: List<Color>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(colors[index % colors.size]))
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A small, static canvas; only the selected value changes on interaction. */
@Composable
private fun StackedBars(rows: List<BarDatum>, series: List<String>, currency: String, colors: List<Color> = chartColors(),
                        hint: String = "Tap a bar to inspect a month.") {
    if (rows.isEmpty() || rows.all { it.values.sum() == 0L }) {
        Text("No recorded spending for this chart yet.", style = MaterialTheme.typography.bodyMedium); return
    }
    var selected by rememberSaveable(rows.map { it.label }) { mutableIntStateOf(rows.lastIndex) }
    val index = selected.coerceIn(rows.indices)
    val max = rows.maxOf { it.values.sum() }.coerceAtLeast(1L)
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Text(money(max, currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Canvas(Modifier.fillMaxWidth().height(chartPlotHeight(150.dp)).semantics {
        contentDescription = "Stacked bar chart. ${rows[index].label}: ${money(rows[index].values.sum(), currency)}"
        onClick("Next month") { selected = (index + 1) % rows.size; true }
    }.pointerInput(rows) { detectTapGestures { selected = (it.x / size.width * rows.size).toInt().coerceIn(rows.indices) } }) {
        val slot = size.width / rows.size
        repeat(3) { drawLine(grid, Offset(0f, size.height * it / 2), Offset(size.width, size.height * it / 2), strokeWidth = 1.dp.toPx()) }
        rows.forEachIndexed { i, row ->
            var bottom = size.height
            row.values.forEachIndexed { seriesIndex, value ->
                val height = size.height * (value.toDouble() / max).toFloat()
                drawRect(colors[seriesIndex % colors.size].copy(alpha = if (index == i) 1f else 0.72f),
                    Offset(i * slot + slot * 0.18f, bottom - height), Size(slot * 0.64f, height))
                bottom -= height
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        rows.forEach { Text(it.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2) }
    }
    Legend(series, colors)
    Text("${rows[index].label} · ${money(rows[index].values.sum(), currency)}", style = MaterialTheme.typography.titleSmall)
    rows[index].values.forEachIndexed { i, value -> Text("${series[i]}: ${money(value, currency)}", style = MaterialTheme.typography.bodySmall) }
    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CategoryTrends(data: ExpenseInsights) {
    val months = data.months.takeLast(6)
    val categories = remember(data) {
        val totals = HashMap<Long, Long>()
        months.forEach { month -> month.categoryAmounts.forEach { (id, amount) -> totals[id] = (totals[id] ?: 0L) + amount } }
        data.categories.sortedByDescending { totals[it.id] ?: 0L }.filter { (totals[it.id] ?: 0L) > 0L }.take(5)
    }
    val rows = remember(data) { months.map { month ->
        val chosen = categories.map { month.categoryAmounts[it.id] ?: 0L }
        BarDatum(month.label, chosen + (month.expense - chosen.sum()))
    } }
    InsightCard("Category trends", "Six months · top five categories plus all other spending. Current month is partial.") {
        StackedBars(rows, categories.map { it.name } + "Other", data.currency)
    }
}

@Composable
private fun SpendingChange(data: ExpenseInsights) {
    val current = data.total
    val previous = data.previousTotal
    val delta = current - previous
    val colors = chartColors()
    val top = data.drivers.take(5)
    val remaining = data.drivers.drop(5).sumOf { it.delta }
    val changes = top.map { it.category.name to it.delta } + if (data.drivers.size > 5) listOf("Other" to remaining) else emptyList()
    val levels = remember(data) {
        var running = previous
        listOf(Triple("Previous", 0L, previous)) + changes.map { (label, amount) ->
            val from = running; running += amount; Triple(label, from, running)
        } + Triple("Selected", 0L, current)
    }
    val max = levels.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1L)
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val income = FinancialColors.income
    val expense = FinancialColors.expense
    InsightCard("Spending change explanation", "${data.comparedDays} elapsed days compared with the previous month; completed months compare full months.") {
        Figure(if (delta >= 0) "Increase in recorded spending" else "Decrease in recorded spending", signedMoney(delta, data.currency))
        Text("Previous: ${money(previous, data.currency)} · Selected: ${money(current, data.currency)}", style = MaterialTheme.typography.bodySmall)
        if (previous == 0L) Text("No previous-period spending is recorded; a percentage change would be misleading.", style = MaterialTheme.typography.bodySmall)
        if (data.drivers.isEmpty()) Text("Add expenses to see which categories changed.", style = MaterialTheme.typography.bodyMedium) else {
            Canvas(Modifier.fillMaxWidth().height(chartPlotHeight(160.dp)).semantics { contentDescription = "Spending change waterfall, ${signedMoney(delta, data.currency)}" }) {
                val slot = size.width / levels.size
                repeat(3) { drawLine(grid, Offset(0f, size.height * it / 2), Offset(size.width, size.height * it / 2), 1.dp.toPx()) }
                levels.forEachIndexed { index, (_, from, to) ->
                    val bottom = size.height * (1 - minOf(from, to).toDouble() / max).toFloat()
                    val topY = size.height * (1 - maxOf(from, to).toDouble() / max).toFloat()
                    val color = if (index == 0 || index == levels.lastIndex) colors.first() else if (to >= from) expense else income
                    drawRect(color, Offset(slot * index + slot * 0.18f, topY), Size(slot * 0.64f, (bottom - topY).coerceAtLeast(1f)))
                    if (index < levels.lastIndex) {
                        val y = size.height * (1 - to.toDouble() / max).toFloat()
                        drawLine(grid, Offset(slot * index + slot * 0.82f, y), Offset(slot * (index + 1) + slot * 0.18f, y), 1.dp.toPx())
                    }
                }
            }
            Legend(listOf("Previous / selected", "Increase", "Decrease"), listOf(colors.first(), expense, income))
            changes.forEach { (name, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(signedMoney(amount, data.currency), style = MaterialTheme.typography.titleSmall, color = if (amount > 0) expense else income)
                }
            }
            data.drivers.firstOrNull { it.delta != 0L }?.let {
                Text("${it.category.name} is the largest recorded driver, ${signedMoney(it.delta, data.currency)} versus the comparable period.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RecurringTrend(data: ExpenseInsights) {
    val rows = remember(data) { data.months.takeLast(6).map { BarDatum(it.label, listOf(it.recurring, it.installments)) } }
    InsightCard("Recurring cost trend", "Actual posted expenses linked to recurring rules or installment plans. Future bills are not included.") {
        StackedBars(rows, listOf("Recurring expenses", "Installments"), data.currency)
        Text("An installment linked to a recurring rule is counted once, under installments. Manually entered bills need a schedule link to appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SmallPurchases(data: ExpenseInsights, onThreshold: (Long) -> Unit) {
    val impact = data.smallPurchases
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var entered by rememberSaveable(impact.threshold) { mutableStateOf(BigDecimal.valueOf(impact.threshold, 2).toPlainString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    InsightCard("Small purchase impact", "Purchases at or below your chosen amount. Transfer fees are excluded from purchase counts.") {
        OutlinedButton(onClick = { showEditor = true }) { Text("Threshold: ${money(impact.threshold, data.currency)}") }
        Figure("${impact.count} of ${impact.allCount} purchases", money(impact.total, data.currency))
        Text("${percentage(impact.total, data.total)} of all recorded spending this month.", style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { if (data.total <= 0L) 0f else (impact.total.toDouble() / data.total).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Text("Small purchases can be necessities too. Use this to understand frequency, then decide what matters to you.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showEditor) AlertDialog(onDismissRequest = { showEditor = false }, title = { Text("Small purchase threshold") }, text = {
        OutlinedTextField(entered, { entered = it; error = null }, label = { Text("Amount in ${data.currency}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, isError = error != null,
            supportingText = { Text(error ?: "Enter a positive amount with up to two decimal places.") })
    }, confirmButton = { TextButton(onClick = {
        val parsed = runCatching { BigDecimal(entered.trim()).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact() }.getOrNull()
        if (parsed == null || parsed !in 1L..100_000_000_000L) error = "Enter a valid positive amount." else { onThreshold(parsed); showEditor = false }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } })
}

private data class BubbleDatum(val label: String, val count: Int, val average: Long, val total: Long)

@Composable
private fun HabitPlot(points: List<BubbleDatum>, currency: String) {
    if (points.isEmpty() || points.all { it.count == 0 }) { Text("Add purchases to see this pattern.", style = MaterialTheme.typography.bodyMedium); return }
    var selection by rememberSaveable(points.map { it.label }) { mutableIntStateOf(0) }
    val selected = selection.coerceIn(points.indices)
    val maxCount = points.maxOf { it.count }.coerceAtLeast(1)
    val maxAverage = points.maxOf { it.average }.coerceAtLeast(1L)
    val maxTotal = points.maxOf { it.total }.coerceAtLeast(1L)
    val colors = chartColors()
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    fun x(point: BubbleDatum, width: Float) = 24f + (width - 48f) * point.count / maxCount
    fun y(point: BubbleDatum, height: Float) = height - 24f - (height - 48f) * (point.average.toDouble() / maxAverage).toFloat()
    Text("Average purchase · up to ${money(maxAverage, currency)}", style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(chartPlotHeight(180.dp)).semantics {
        contentDescription = "Purchase pattern chart. ${points[selected].label}, ${points[selected].count} purchases, average ${money(points[selected].average, currency)}"
        onClick("Next point") { selection = (selected + 1) % points.size; true }
    }.pointerInput(points) { detectTapGestures { tap ->
        selection = points.indices.minByOrNull { i -> val dx = tap.x - x(points[i], size.width.toFloat()); val dy = tap.y - y(points[i], size.height.toFloat()); dx * dx + dy * dy } ?: 0
    } }) {
        repeat(3) { drawLine(grid, Offset(24f, y(BubbleDatum("", 0, maxAverage * it / 2, 0), size.height)), Offset(size.width - 24f, y(BubbleDatum("", 0, maxAverage * it / 2, 0), size.height)), 1.dp.toPx()) }
        points.forEachIndexed { i, point ->
            val radius = 5.dp.toPx() + 9.dp.toPx() * sqrt(point.total.toDouble() / maxTotal).toFloat()
            drawCircle(colors[i % colors.size].copy(alpha = if (i == selected) 1f else 0.55f), radius, Offset(x(point, size.width), y(point, size.height)))
        }
    }
    Text("Purchase count → 0 to $maxCount · bubble size represents total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val point = points[selected]
    Text(point.label, style = MaterialTheme.typography.titleMedium)
    Text("${point.count} purchases · Average ${money(point.average, currency)} · Total ${money(point.total, currency)}", style = MaterialTheme.typography.bodyMedium)
    Text("Tap a bubble or choose a label.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        points.forEachIndexed { i, point -> FilterChip(selected = selected == i, onClick = { selection = i }, label = { Text(point.label) }) }
    }
}

@Composable
private fun PurchaseFrequency(data: ExpenseInsights) {
    val points = remember(data) { data.months.takeLast(6).map { BubbleDatum(it.label, it.count, it.average, it.purchaseTotal) } }
    InsightCard("Frequency vs purchase size", "Six months · distinguish more frequent purchases from larger average purchases. Current month is partial.") {
        HabitPlot(points, data.currency)
    }
}

@Composable
private fun MerchantHabits(data: ExpenseInsights) {
    val points = remember(data) { data.merchants.take(10).map { BubbleDatum(it.name, it.count, it.average, it.total) } }
    InsightCard("Merchant habits", "This month · ten merchants with the highest totals; merchant names are matched without case differences.") {
        if (points.isEmpty()) Text("Add payee names to your expenses to explore merchant habits.", style = MaterialTheme.typography.bodyMedium)
        else HabitPlot(points, data.currency)
    }
}

@Composable
private fun SpikeExplanations(data: ExpenseInsights, onTransaction: (Long) -> Unit) {
    InsightCard("Spending spikes", "Days above 1.75× your mean active spending day. At least three active days are needed.") {
        if (data.spikes.isEmpty()) Text("No unusually expensive days found for this month, or there are too few active days to compare.", style = MaterialTheme.typography.bodyMedium)
        data.spikes.forEach { day ->
            Figure("Day ${day.day} · ${day.count} spending records", money(day.amount, data.currency))
            ContributionRows(day.contributors, data.currency, onTransaction)
            Text("These largest records account for ${percentage(day.contributors.sumOf { it.amount }, day.amount)} of the day's spending.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
        Text("A spike describes the records; it does not imply waste or an incorrect transaction.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Seasonality(data: ExpenseInsights) {
    val years = data.months.filter { it.hasRecords }.map { it.year }.distinct().sorted()
    val colors = chartColors()
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    var selectedMonth by rememberSaveable(data.monthStartMillis) { mutableIntStateOf(0) }
    val maximum = data.months.maxOfOrNull { it.expense }?.coerceAtLeast(1L) ?: 1L
    InsightCard("Annual seasonality", "Compare recorded monthly spending across calendar years. Gaps mean no records; partial months are marked.") {
        if (years.size < 2) {
            Text("Records from at least two calendar years are needed for a seasonal comparison.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(money(maximum, data.currency), style = MaterialTheme.typography.bodySmall)
            Canvas(Modifier.fillMaxWidth().height(chartPlotHeight(160.dp)).semantics {
                contentDescription = "Annual seasonality chart, ${years.joinToString()}"
                onClick("Next month") { selectedMonth = (selectedMonth + 1) % 12; true }
            }.pointerInput(data) { detectTapGestures { selectedMonth = (it.x / size.width * 12).toInt().coerceIn(0, 11) } }) {
                repeat(3) { drawLine(grid, Offset(0f, size.height * it / 2), Offset(size.width, size.height * it / 2), 1.dp.toPx()) }
                years.forEachIndexed { index, year ->
                    val months = data.months.filter { it.year == year }.associateBy { it.month }
                    var previous: Offset? = null
                    repeat(12) { m ->
                        val month = months[m]
                        if (month == null || !month.hasRecords) previous = null else {
                            val point = Offset(size.width * (m + 0.5f) / 12, size.height * (1 - month.expense.toDouble() / maximum).toFloat())
                            previous?.let { drawLine(colors[index], it, point, 2.dp.toPx(), StrokeCap.Round) }
                            drawCircle(colors[index], if (m == selectedMonth) 5.dp.toPx() else 3.dp.toPx(), point)
                            previous = point
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Jan", "Apr", "Jul", "Oct", "Dec").forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Legend(years.map { "$it" }, colors)
            val name = SimpleDateFormat("MMMM", Locale.getDefault()).format(java.util.Calendar.getInstance().apply { set(java.util.Calendar.MONTH, selectedMonth); set(java.util.Calendar.DAY_OF_MONTH, 1) }.time)
            Text(name, style = MaterialTheme.typography.titleMedium)
            years.forEach { year ->
                val month = data.months.find { it.year == year && it.month == selectedMonth }
                val partial = month?.key == data.months.last().key && data.comparedDays < data.daysInMonth
                Text("$year: ${if (month == null || !month.hasRecords) "No records" else money(month.expense, data.currency)}${if (partial) " (partial month)" else ""}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Tap the chart to inspect a month. Compare completed months for the fairest reading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SavingsWhatIf(data: ExpenseInsights) {
    val categories = data.drivers.filter { it.current > 0 }.sortedByDescending { it.current }
    var selectedId by rememberSaveable(data.monthStartMillis) { mutableStateOf<Long?>(null) }
    var reduction by rememberSaveable { mutableFloatStateOf(10f) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    val selected = categories.find { it.category.id == selectedId } ?: categories.firstOrNull()
    InsightCard("Savings what-if", "A scenario based on recorded spending. Choose a category and reduction; this does not change your budget or transactions.") {
        if (selected == null) Text("Add expenses to explore a reduction scenario.", style = MaterialTheme.typography.bodyMedium) else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.take(6).forEach { driver -> FilterChip(selected = driver.category.id == selected.category.id,
                    onClick = { selectedId = driver.category.id }, label = { Text(driver.category.name) }) }
            }
            if (categories.size > 6) OutlinedButton(onClick = { showCategories = true }) { Text("Choose another category") }
            val percent = reduction.roundToInt()
            Text("Reduce ${selected.category.name} by $percent%", style = MaterialTheme.typography.bodyMedium)
            Slider(reduction, { reduction = it }, valueRange = 0f..100f, steps = 19,
                modifier = Modifier.semantics { contentDescription = "Category spending reduction percentage" })
            val saving = selected.current / 100 * percent + selected.current % 100 * percent / 100
            Figure("Illustrative saving for this recorded period", money(saving, data.currency), FinancialColors.income)
            Text("Category spending: ${money(selected.current, data.currency)} → ${money(selected.current - saving, data.currency)}", style = MaterialTheme.typography.bodyMedium)
            Text("Total spending with this change: ${money(data.total - saving, data.currency)}", style = MaterialTheme.typography.bodyMedium)
            StackedBars(listOf(BarDatum("Recorded", listOf(selected.current)), BarDatum("Scenario", listOf(selected.current - saving))),
                listOf("Category spending"), data.currency, listOf(MaterialTheme.colorScheme.primary), "Tap a bar to inspect recorded or scenario spending.")
        }
    }
    if (showCategories) AlertDialog(onDismissRequest = { showCategories = false }, title = { Text("Choose a category") }, text = {
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(categories, key = { it.category.id }) { driver ->
                ListItem(headlineContent = { Text(driver.category.name) }, supportingContent = { Text(money(driver.current, data.currency)) },
                    modifier = Modifier.clickable { selectedId = driver.category.id; showCategories = false })
            }
        }
    }, confirmButton = { TextButton(onClick = { showCategories = false }) { Text("Close") } })
}

@Composable
private fun WeeklyDigest(data: ExpenseInsights, onTransaction: (Long) -> Unit) {
    val digest = data.digest
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    InsightCard("Weekly digest", "${dateFormat.format(Date(digest.startMillis))} – ${dateFormat.format(Date(digest.endMillis))} · latest seven elapsed days for this month") {
        if (data.comparedDays == 0) Text("This month has not started yet; there is no elapsed-week recap.", style = MaterialTheme.typography.bodyMedium) else {
            Figure("${digest.count} spending records · ${digest.daysWithSpending} active days", money(digest.expense, data.currency))
            val change = digest.expense - digest.previousExpense
            Text("${signedMoney(change, data.currency)} versus the previous seven days (${money(digest.previousExpense, data.currency)}).", style = MaterialTheme.typography.bodyMedium)
            digest.topCategory?.let {
                Text("Largest category: ${it.category.name} · ${money(it.current, data.currency)} (${percentage(it.current, digest.expense)} of this week).", style = MaterialTheme.typography.bodyMedium)
                Text("That category changed by ${signedMoney(it.delta, data.currency)} versus the preceding week.", style = MaterialTheme.typography.bodySmall)
            }
            digest.largest?.let {
                Text("Largest recorded expense", style = MaterialTheme.typography.titleSmall)
                ContributionRows(listOf(it), data.currency, onTransaction)
            }
            if (digest.count == 0) Text("No spending recorded during these seven days.", style = MaterialTheme.typography.bodyMedium)
            if (data.monthBudgetAmount > 0L) Text("Month so far: ${money(data.total, data.currency)} of your ${money(data.monthBudgetAmount, data.currency)} budget (${percentage(data.total, data.monthBudgetAmount)} used).", style = MaterialTheme.typography.bodyMedium)
            if (data.upcomingBills.isNotEmpty()) {
                Text("Scheduled bills in the next seven days", style = MaterialTheme.typography.titleSmall)
                data.upcomingBills.take(5).forEach { bill ->
                    Text("${bill.name} · ${dateFormat.format(Date(bill.dueMillis))} · ${money(bill.amount, data.currency)}", style = MaterialTheme.typography.bodyMedium)
                }
                if (data.upcomingBills.size > 5) Text("${data.upcomingBills.size - 5} more in Recurring & Installments.", style = MaterialTheme.typography.bodySmall)
                Text("These are saved schedule amounts, not a spending forecast.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("This recap reflects your entered records; missing entries can affect the comparison.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
