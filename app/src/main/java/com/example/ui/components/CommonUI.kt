package com.example.ui.components

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalDensity
import com.example.data.model.*
import com.example.domain.CurrencyFormatter
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val monthHeaderFormatter = ThreadLocal.withInitial { SimpleDateFormat("MMM yyyy", Locale.US) }

@Composable
fun Modifier.horizontalSwipeListener(
    thresholdDp: Dp = 44.dp,
    cooldownMillis: Long = 280L,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier {
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)
    var lastTriggerTime by remember { mutableLongStateOf(0L) }

    return this.pointerInput(thresholdDp, cooldownMillis) {
        val thresholdPx = thresholdDp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalDragX = 0f
            var totalDragY = 0f
            var isHorizontalSwipe: Boolean? = null

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                // A child scrollable has claimed this gesture. Never steal it
                // or turn a cancelled drag into month navigation.
                if (change.isConsumed || event.changes.count { it.pressed } > 1) {
                    isHorizontalSwipe = false
                    break
                }
                if (!change.pressed) break

                val dragX = change.position.x - change.previousPosition.x
                val dragY = change.position.y - change.previousPosition.y
                totalDragX += dragX
                totalDragY += dragY

                val absX = kotlin.math.abs(totalDragX)
                val absY = kotlin.math.abs(totalDragY)
                val touchSlop = viewConfiguration.touchSlop

                if (isHorizontalSwipe == null) {
                    if (absY > touchSlop && absY > absX) {
                        // Vertical scroll initiated: yield immediately so LazyColumn scrolls freely
                        isHorizontalSwipe = false
                        break
                    } else if (absX > touchSlop && absX > absY * 1.25f) {
                        // Horizontal swipe confirmed
                        isHorizontalSwipe = true
                    }
                }

                if (isHorizontalSwipe == true) {
                    change.consume()
                }
            }

            if (isHorizontalSwipe == true) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTriggerTime >= cooldownMillis) {
                    if (totalDragX > thresholdPx) {
                        lastTriggerTime = now
                        currentOnSwipeRight()
                    } else if (totalDragX < -thresholdPx) {
                        lastTriggerTime = now
                        currentOnSwipeLeft()
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelectorHeader(
    currentMonthDate: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayString = monthHeaderFormatter.get()?.format(currentMonthDate.time) ?: ""

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.horizontalSwipeListener(
            thresholdDp = 36.dp,
            onSwipeLeft = onNextMonth,
            onSwipeRight = onPreviousMonth
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMonthClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = displayString,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PeriodTotalsBar(
    totalIncome: Long, totalExpense: Long, balance: Long,
    currencyCode: String = "INR", modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Monthly spending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(CurrencyFormatter.formatAmount(totalExpense, currencyCode),
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatAmount(totalIncome, currencyCode), color = FinancialColors.income,
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Net cash flow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatAmount(balance, currencyCode),
                        color = if (balance < 0) FinancialColors.expense else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
                }
            }
        }
    }
}
private val rowTimeFormat = ThreadLocal.withInitial {
    SimpleDateFormat("hh:mm a", Locale.US)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionItemRow(
    item: TransactionWithDetails,
    currencyCode: String = "INR",
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tx = item.transaction
    val timeString = remember(tx.dateMillis) {
        rowTimeFormat.get()?.format(Date(tx.dateMillis)) ?: ""
    }
    val formattedAmount = remember(tx.amount, currencyCode) {
        CurrencyFormatter.formatAmount(tx.amount, currencyCode)
    }

    val isTransfer = tx.type == TransactionType.TRANSFER
    val isIncome = tx.type == TransactionType.INCOME
    val isExpense = tx.type == TransactionType.EXPENSE

    val amountColor = when {
        isIncome -> FinancialColors.income
        isExpense -> FinancialColors.expense
        else -> FinancialColors.transfer
    }

    val amountPrefix = when {
        isIncome -> "+"
        isExpense -> "-"
        else -> "⇆"
    }

    // Merchant first; preserve notes in the metadata when both are present.
    val mainHighlightText = when {
        tx.payee.isNotBlank() -> tx.payee
        tx.note.isNotBlank() -> tx.note
        isTransfer -> "Transfer"
        item.subcategory != null -> item.subcategory.name
        else -> item.category?.name ?: "Expense"
    }

    val categoryDisplayName = if (isTransfer) {
        "⇆ Transfer"
    } else {
        item.category?.name ?: "General"
    }

    val accountSubtitle = if (isTransfer) {
        "${item.account?.name ?: "Account"} → ${item.toAccount?.name ?: "Account"}"
    } else {
        val acc = item.account?.name ?: "Account"
        if (tx.payee.isNotBlank() && tx.note.isNotBlank()) {
            "$acc · ${tx.note}"
        } else {
            acc
        }
    }

    val isThemeDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val useLargeTextLayout = LocalDensity.current.fontScale >= 1.3f
    val selectedBgColor = if (FinancialColors.refined) MaterialTheme.colorScheme.primaryContainer else if (isThemeDark) {
        Color(0xFF3E2226) // Deep warm burgundy highlight in dark mode (as user confirmed: "for dark its fine whatever is there currently")
    } else {
        Color(0xFFFFEBEE) // Clean soft rose/coral highlight in light mode
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) selectedBgColor else Color.Transparent,
        border = if (isSelected) {
            if (FinancialColors.refined) BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary) else if (isThemeDark) {
                BorderStroke(0.5.dp, Color(0xFF5C2A2D))
            } else {
                BorderStroke(0.5.dp, Color(0xFFFFCDD2))
            }
        } else null,
        tonalElevation = 0.dp
    ) {
        if (useLargeTextLayout) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(mainHighlightText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("$amountPrefix $formattedAmount", color = amountColor, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
                Text(categoryDisplayName, style = MaterialTheme.typography.bodySmall)
                Text(accountSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tx.receiptUri != null) Text("Receipt attached", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(
                    iconName = if (isTransfer) "currency_exchange" else item.category?.iconName ?: "category",
                    colorHex = item.category?.colorHex ?: "#64748B", size = 36.dp, iconSize = 19.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(mainHighlightText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (tx.receiptUri != null) Icon(Icons.Default.Receipt, "Receipt attached", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(categoryDisplayName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(accountSubtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.widthIn(max = 150.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$amountPrefix $formattedAmount", color = amountColor,
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"), maxLines = 2)
                    Text(timeString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable
fun EmptyStateView(
    title: String,
    message: String,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
    },
    actionButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { icon() }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(16.dp))
            actionButton()
        }
    }
}

@Composable
fun MonthPickerDialog(
    currentCalendar: Calendar,
    onMonthPicked: (Int, Int) -> Unit, // year, monthIndex (0-11)
    onDismiss: () -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = remember { todayCal.get(Calendar.YEAR) }
    val todayMonth = remember { todayCal.get(Calendar.MONTH) }

    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    val currentSelectedMonth = currentCalendar.get(Calendar.MONTH)
    val currentSelectedYear = currentCalendar.get(Calendar.YEAR)

    val monthLabels = listOf(
        "JAN", "FEB", "MAR", "APR",
        "MAY", "JUN", "JUL", "AUG",
        "SEP", "OCT", "NOV", "DEC"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            onMonthPicked(todayYear, todayMonth)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "THIS MONTH",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year navigation: < 2026 >
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYear-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Year",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "$selectedYear",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { selectedYear++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Year",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 rows x 4 columns grid of months
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0..3) {
                            val monthIdx = row * 4 + col
                            val isCurrentlySelected = selectedYear == currentSelectedYear && monthIdx == currentSelectedMonth
                            val isActualCurrentMonth = selectedYear == todayYear && monthIdx == todayMonth

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrentlySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        onMonthPicked(selectedYear, monthIdx)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthLabels[monthIdx],
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrentlySelected || isActualCurrentMonth) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = when {
                                        isCurrentlySelected -> FinancialColors.expense
                                        isActualCurrentMonth -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
