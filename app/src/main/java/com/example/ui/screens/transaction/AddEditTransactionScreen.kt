package com.example.ui.screens.transaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import kotlinx.coroutines.CancellationException
import com.example.ui.components.LoadingContent
import com.example.ui.components.ErrorContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.domain.CurrencyFormatter
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class ActiveInputSection {
    AMOUNT,
    CATEGORY,
    ACCOUNT,
    TO_ACCOUNT,
    DATE,
    NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: MainViewModel,
    transactionId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0
    var recordRetry by remember { mutableIntStateOf(0) }
    var recordError by remember { mutableStateOf<String?>(null) }
    val existingRows by produceState<List<TransactionWithDetails>?>(null, transactionId, recordRetry) {
        recordError = null
        if (transactionId == null || transactionId == 0L) value = emptyList()
        else try { viewModel.transaction(transactionId).collect { value = it } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { recordError = "Could not load this transaction" }
    }
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()

    if (existingRows == null) {
        if (recordError != null) ErrorContent(recordError!!) { recordRetry++ } else LoadingContent()
        return
    }
    val loadedQueries by viewModel.loadedQueries.collectAsStateWithLifecycle()
    if ("accounts" !in loadedQueries || "categories" !in loadedQueries) { LoadingContent(); return }
    val existingTx = existingRows?.singleOrNull()
    if (transactionId != null && transactionId != 0L && existingTx == null) {
        EmptyStateView("Transaction not found", "This transaction may have been deleted.")
        return
    }

    var transactionType by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.type ?: TransactionType.EXPENSE)
    }

    var amountMinorUnits by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.amount ?: 0L)
    }
    var expressionText by rememberSaveable(transactionId) {
        mutableStateOf(if (amountMinorUnits > 0) CurrencyFormatter.toDecimalString(amountMinorUnits) else "0")
    }

    // Default to AMOUNT input on launch for fast, minimal data entry
    var activeSection by rememberSaveable(transactionId) {
        mutableStateOf(if (existingTx == null) ActiveInputSection.AMOUNT else ActiveInputSection.NONE)
    }

    var selectedCalendar by rememberSaveable(transactionId, stateSaver = Saver<Calendar, Long>(save = { it.timeInMillis }, restore = { Calendar.getInstance().apply { timeInMillis = it } })) {
        val cal = Calendar.getInstance()
        if (existingTx != null) {
            cal.timeInMillis = existingTx.transaction.dateMillis
        }
        mutableStateOf(cal)
    }

    var selectedAccountId by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.accountId ?: accounts.firstOrNull()?.id ?: 1L)
    }

    var selectedToAccountId by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.toAccountId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L)
    }

    var transferFeeMinorUnits by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.transferFee ?: 0L)
    }

    val typeFilteredCategories = remember(categories, transactionType) {
        categories.filter { it.type == transactionType && it.parentId == null }
    }

    var selectedCategoryId by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.categoryId ?: typeFilteredCategories.firstOrNull()?.id ?: 1L)
    }

    var selectedSubcategoryId by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.subcategoryId)
    }

    LaunchedEffect(transactionType, typeFilteredCategories) {
        if (transactionType != TransactionType.TRANSFER && typeFilteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = typeFilteredCategories.firstOrNull()?.id ?: 0L
            selectedSubcategoryId = null
        }
    }

    val subcategories = remember(categories, selectedCategoryId) {
        categories.filter { it.parentId == selectedCategoryId }
    }

    var payeeText by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.payee ?: "")
    }

    var noteText by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.note ?: "")
    }

    var tagsText by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.tags ?: "")
    }

    var paymentMethod by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.paymentMethod ?: PaymentMethod.CASH)
    }

    var receiptUri by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.receiptUri)
    }

    var isExcludedFromStats by rememberSaveable(transactionId) {
        mutableStateOf(existingTx?.transaction?.isExcludedFromStats ?: false)
    }

    var saveAsBookmark by rememberSaveable(transactionId) { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    val dateFormat = remember { SimpleDateFormat("M/d/yy (EEE)  h:mm a", Locale.US) }

    val currentSelectedAccount = accounts.find { it.id == selectedAccountId }
    val currentSelectedToAccount = accounts.find { it.id == selectedToAccountId }
    val currentSelectedCategory = categories.find { it.id == selectedCategoryId }
    val currentSelectedSubcategory = categories.find { it.id == selectedSubcategoryId }

    val themeColor = when (transactionType) {
        TransactionType.EXPENSE -> FinancialColors.expense
        TransactionType.INCOME -> FinancialColors.income
        TransactionType.TRANSFER -> FinancialColors.transfer
    }

    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = selectedCalendar.clone() as Calendar
                newCal.set(Calendar.YEAR, year)
                newCal.set(Calendar.MONTH, month)
                newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedCalendar = newCal
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newCal = selectedCalendar.clone() as Calendar
                newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCal.set(Calendar.MINUTE, minute)
                selectedCalendar = newCal
            },
            selectedCalendar.get(Calendar.HOUR_OF_DAY),
            selectedCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    var saveError by rememberSaveable(transactionId) { mutableStateOf<String?>(null) }
    val isSaving by viewModel.transactionSaving.collectAsStateWithLifecycle()
    var isSaveSuccess by remember { mutableStateOf(false) }

    fun exitScreen() {
        focusManager.clearFocus()
        activeSection = ActiveInputSection.NONE
        onNavigateBack()
    }

    // Automatically hide numpad whenever software keyboard pops up (e.g. typing in note, payee, tags)
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && activeSection == ActiveInputSection.AMOUNT) {
            activeSection = ActiveInputSection.NONE
        }
    }

    BackHandler {
        if (activeSection != ActiveInputSection.NONE) {
            activeSection = ActiveInputSection.NONE
        } else {
            exitScreen()
        }
    }

    fun performSave(closeOnFinish: Boolean = true) {
        if (isSaving || isSaveSuccess) return
        if (amountMinorUnits <= 0L) {
            amountMinorUnits = CurrencyFormatter.parseToMinorUnits(expressionText)
        }
        if (amountMinorUnits <= 0L) { saveError = "Enter an amount greater than zero"; return }

        focusManager.clearFocus()
        saveError = null
        viewModel.saveTransaction(
            id = existingTx?.transaction?.id ?: 0L,
            type = transactionType,
            dateMillis = selectedCalendar.timeInMillis,
            amount = amountMinorUnits,
            accountId = selectedAccountId,
            toAccountId = if (transactionType == TransactionType.TRANSFER) selectedToAccountId else null,
            transferFee = if (transactionType == TransactionType.TRANSFER) transferFeeMinorUnits else 0L,
            categoryId = if (transactionType == TransactionType.TRANSFER) 0L else selectedCategoryId,
            subcategoryId = if (transactionType == TransactionType.TRANSFER) null else selectedSubcategoryId,
            payee = payeeText,
            note = noteText,
            tags = tagsText,
            receiptUri = receiptUri,
            paymentMethod = paymentMethod,
            isExcludedFromStats = isExcludedFromStats,
            saveAsBookmark = saveAsBookmark,
            onError = { message -> saveError = message },
            onComplete = {
                if (closeOnFinish) {
                    isSaveSuccess = true
                    exitScreen()
                } else {
                    // Reset fields for rapid consecutive addition
                    amountMinorUnits = 0L
                    expressionText = "0"
                    noteText = ""
                    payeeText = ""
                    tagsText = ""
                    receiptUri = null
                    activeSection = ActiveInputSection.AMOUNT
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { if (saveError != null) Snackbar { Text(saveError!!) } },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingTx != null) "Edit ${transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}"
                               else transactionType.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { exitScreen() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { saveAsBookmark = !saveAsBookmark }) {
                        Icon(
                            imageVector = if (saveAsBookmark) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark favorite",
                            tint = if (saveAsBookmark) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (existingTx != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteTransaction(existingTx.transaction)
                                exitScreen()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FinancialColors.expense)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable upper form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Segmented Tabs: [ Income ] [ Expense ] [ Transfer ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val types = listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER)
                    types.forEach { type ->
                        val isSelected = transactionType == type
                        val activeColor = when (type) {
                            TransactionType.INCOME -> FinancialColors.income
                            TransactionType.EXPENSE -> FinancialColors.expense
                            TransactionType.TRANSFER -> FinancialColors.transfer
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) activeColor else Color.Transparent)
                                .clickable {
                                    transactionType = type
                                    val validCats = categories.filter { it.type == type && it.parentId == null }
                                    if (validCats.isNotEmpty() && validCats.none { it.id == selectedCategoryId }) {
                                        selectedCategoryId = validCats.first().id
                                        selectedSubcategoryId = null
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (type) {
                                    TransactionType.INCOME -> "Income"
                                    TransactionType.EXPENSE -> "Expense"
                                    TransactionType.TRANSFER -> "Transfer"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Table-Style Form Container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // 1. DATE ROW
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    openDatePicker()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(80.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(selectedCalendar.time),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { openTimePicker() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Edit Time",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 2. ACCOUNT ROW
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (activeSection == ActiveInputSection.ACCOUNT)
                                        themeColor.copy(alpha = 0.08f) else Color.Transparent
                                )
                                .clickable {
                                    focusManager.clearFocus()
                                    activeSection = if (activeSection == ActiveInputSection.ACCOUNT) ActiveInputSection.NONE else ActiveInputSection.ACCOUNT
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (transactionType == TransactionType.TRANSFER) "From" else "Account",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(80.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (currentSelectedAccount?.type) {
                                        AccountType.CASH -> Icons.Default.AccountBalanceWallet
                                        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                        else -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentSelectedAccount?.name ?: "Select Account",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (currentSelectedAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2b. TO ACCOUNT (FOR TRANSFER)
                        if (transactionType == TransactionType.TRANSFER) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (activeSection == ActiveInputSection.TO_ACCOUNT)
                                            themeColor.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                    .clickable {
                                        focusManager.clearFocus()
                                        activeSection = if (activeSection == ActiveInputSection.TO_ACCOUNT) ActiveInputSection.NONE else ActiveInputSection.TO_ACCOUNT
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "To",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(80.dp)
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = FinancialColors.transfer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentSelectedToAccount?.name ?: "Select Destination Account",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 3. CATEGORY ROW (FOR EXPENSE / INCOME)
                        if (transactionType != TransactionType.TRANSFER) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (activeSection == ActiveInputSection.CATEGORY)
                                            themeColor.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                    .clickable {
                                        focusManager.clearFocus()
                                        activeSection = if (activeSection == ActiveInputSection.CATEGORY) ActiveInputSection.NONE else ActiveInputSection.CATEGORY
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Category",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(80.dp)
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentSelectedCategory != null) {
                                        CategoryIconBadge(
                                            iconName = currentSelectedCategory.iconName,
                                            colorHex = currentSelectedCategory.colorHex,
                                            size = 24.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = buildString {
                                                append(currentSelectedCategory.name)
                                                if (currentSelectedSubcategory != null) {
                                                    append(" · ${currentSelectedSubcategory.name}")
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = "Select Category",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 4. AMOUNT ROW
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (activeSection == ActiveInputSection.AMOUNT)
                                        themeColor.copy(alpha = 0.08f) else Color.Transparent
                                )
                                .clickable {
                                    focusManager.clearFocus()
                                    activeSection = if (activeSection == ActiveInputSection.AMOUNT) ActiveInputSection.NONE else ActiveInputSection.AMOUNT
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(80.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayFormatted = if (amountMinorUnits > 0) {
                                    CurrencyFormatter.formatAmount(amountMinorUnits, currencyCode)
                                } else {
                                    "${CurrencyFormatter.getCurrencySymbol(currencyCode)} $expressionText"
                                }

                                Text(
                                    text = displayFormatted,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp
                                    ),
                                    color = themeColor
                                )
                            }

                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    activeSection = if (activeSection == ActiveInputSection.AMOUNT) ActiveInputSection.NONE else ActiveInputSection.AMOUNT
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Calculator",
                                    tint = if (activeSection == ActiveInputSection.AMOUNT) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 5. NOTE ROW (Direct keyboard entry)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Note",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(80.dp)
                            )

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                placeholder = { Text("Enter note...", style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            activeSection = ActiveInputSection.NONE
                                        }
                                    },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 6. DESCRIPTION / PAYEE & RECEIPT ROW
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (transactionType == TransactionType.INCOME) "Payer" else "Merchant",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(80.dp)
                            )

                            OutlinedTextField(
                                value = payeeText,
                                onValueChange = { payeeText = it },
                                placeholder = { Text("e.g. Swiggy, Amazon...", style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            activeSection = ActiveInputSection.NONE
                                        }
                                    },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                )
                            )

                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (receiptUri != null) Icons.Default.Receipt else Icons.Default.CameraAlt,
                                    contentDescription = "Attach Receipt",
                                    tint = if (receiptUri != null) FinancialColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Show Receipt Attached Badge if any
                if (receiptUri != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FinancialColors.income, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Receipt Photo Attached", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { receiptUri = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove receipt", tint = FinancialColors.expense, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Expandable More Options (Tags, Exclude from stats, Payment Method)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMoreOptions = !showMoreOptions }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showMoreOptions) "Less Details" else "+ More Details (Tags, Stats)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (showMoreOptions) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                label = { Text("Tags") },
                                placeholder = { Text("e.g. Vacation, Office, Tax") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            activeSection = ActiveInputSection.NONE
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                            )

                            if (transactionType == TransactionType.TRANSFER) {
                                OutlinedTextField(
                                    value = if (transferFeeMinorUnits > 0) CurrencyFormatter.toDecimalString(transferFeeMinorUnits) else "",
                                    onValueChange = { transferFeeMinorUnits = CurrencyFormatter.parseToMinorUnits(it) },
                                    label = { Text("Transfer Fee") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                activeSection = ActiveInputSection.NONE
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Exclude from statistics", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = isExcludedFromStats,
                                    onCheckedChange = { isExcludedFromStats = it }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SAVE AND CONTINUE BUTTONS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { performSave(closeOnFinish = true) },
                        enabled = !isSaving && !isSaveSuccess,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        AnimatedContent(
                            targetState = when {
                                isSaveSuccess -> "Saved"
                                isSaving -> "Saving..."
                                else -> "Save"
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(120))
                            },
                            label = "SaveButtonState"
                        ) { label ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSaveSuccess) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (existingTx == null) {
                        OutlinedButton(
                            onClick = { performSave(closeOnFinish = false) },
                            enabled = !isSaving && !isSaveSuccess,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Continue",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // BOTTOM INTERACTIVE PANEL (Animated content based on active section with smooth slide & expand)
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    if (initialState == ActiveInputSection.NONE && targetState != ActiveInputSection.NONE) {
                        (slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(220))) togetherWith (
                            fadeOut(animationSpec = tween(140))
                        )
                    } else if (initialState != ActiveInputSection.NONE && targetState == ActiveInputSection.NONE) {
                        fadeIn(animationSpec = tween(140)) togetherWith (
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(200, easing = FastOutLinearInEasing)
                            ) + fadeOut(animationSpec = tween(180))
                        )
                    } else {
                        (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.98f)) togetherWith (
                            fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.98f)
                        )
                    }
                },
                label = "BottomInteractivePanel"
            ) { section ->
                when (section) {
                    ActiveInputSection.AMOUNT -> {
                        CalculatorKeypad(
                            currentExpression = expressionText,
                            onExpressionChanged = { expr ->
                                expressionText = expr
                                amountMinorUnits = CurrencyFormatter.parseToMinorUnits(expr)
                            },
                            onConfirmed = { confirmedMinorUnits ->
                                amountMinorUnits = confirmedMinorUnits
                                expressionText = CurrencyFormatter.toDecimalString(confirmedMinorUnits)
                                // Smoothly advance to category (or account if transfer)
                                activeSection = if (transactionType == TransactionType.TRANSFER) ActiveInputSection.ACCOUNT else ActiveInputSection.CATEGORY
                            },
                            onClose = {
                                activeSection = ActiveInputSection.NONE
                            },
                            currencyCode = currencyCode
                        )
                    }

                    ActiveInputSection.CATEGORY -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Select Category",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = { activeSection = ActiveInputSection.NONE },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Subcategories if present for the selected category
                                if (subcategories.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(subcategories) { sub ->
                                            val isSubSelected = selectedSubcategoryId == sub.id
                                            FilterChip(
                                                selected = isSubSelected,
                                                onClick = {
                                                    selectedSubcategoryId = if (isSubSelected) null else sub.id
                                                },
                                                label = { Text(sub.name, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                // Categories Grid (3 Columns)
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    items(typeFilteredCategories) { cat ->
                                        val isSelected = selectedCategoryId == cat.id
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    selectedCategoryId = cat.id
                                                    selectedSubcategoryId = null
                                                    // Auto close or switch to none after choosing category
                                                    activeSection = ActiveInputSection.NONE
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                                     else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CategoryIconBadge(
                                                    iconName = cat.iconName,
                                                    colorHex = cat.colorHex,
                                                    size = 28.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cat.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ActiveInputSection.ACCOUNT -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (transactionType == TransactionType.TRANSFER) "Select Source Account" else "Select Account",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = { activeSection = ActiveInputSection.NONE },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.heightIn(max = 220.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    items(accounts) { acc ->
                                        val isSelected = selectedAccountId == acc.id
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    selectedAccountId = acc.id
                                                    activeSection = ActiveInputSection.NONE
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                                     else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = when (acc.type) {
                                                        AccountType.CASH -> Icons.Default.AccountBalanceWallet
                                                        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                                        else -> Icons.Default.AccountBalance
                                                    },
                                                    contentDescription = null,
                                                    tint = themeColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ActiveInputSection.TO_ACCOUNT -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Select Destination Account",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = { activeSection = ActiveInputSection.NONE },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.heightIn(max = 220.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    items(accounts) { acc ->
                                        val isSelected = selectedToAccountId == acc.id
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    selectedToAccountId = acc.id
                                                    activeSection = ActiveInputSection.NONE
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                                     else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = when (acc.type) {
                                                        AccountType.CASH -> Icons.Default.AccountBalanceWallet
                                                        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                                        else -> Icons.Default.AccountBalance
                                                    },
                                                    contentDescription = null,
                                                    tint = FinancialColors.transfer,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ActiveInputSection.DATE,
                    ActiveInputSection.NONE -> {
                        // Empty spacer when panel is dismissed
                        Box(modifier = Modifier.fillMaxWidth().height(0.dp))
                    }
                }
            }
        }
    }
}
