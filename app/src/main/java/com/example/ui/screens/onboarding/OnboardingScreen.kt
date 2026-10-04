package com.example.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.domain.CurrencyFormatter
import com.example.ui.MainViewModel
import com.example.ui.theme.*

data class AccountSetupItem(
    val id: String,
    val name: String,
    val type: AccountType,
    val institution: String,
    val icon: ImageVector,
    var isSelected: Boolean = false,
    var initialBalanceText: String = "0",
    var creditLimitText: String = "",
    val isGenericBank: Boolean = false,
    val description: String = "",
    val isCustom: Boolean = false
)

data class FinancialGoalOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    isEditModeFromSettings: Boolean = false,
    onFinishOnboarding: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 5

    // Step 1: Persona & Goals
    var userName by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("TRACK_EXPENSES") }

    // Step 2: Currency & Formatting
    var selectedCurrency by remember { mutableStateOf("INR") }
    var isIndianFormat by remember { mutableStateOf(true) }

    // Step 3: Accounts Setup
    val accountsList = remember {
        mutableStateListOf(
            // Generic Option for users who don't want to specify institution
            AccountSetupItem(
                id = "generic_bank",
                name = "Primary Bank Account",
                type = AccountType.BANK,
                institution = "Bank Account (General)",
                icon = Icons.Default.AccountBalance,
                isSelected = true,
                initialBalanceText = "25000",
                isGenericBank = true,
                description = "Anonymous / General checking or savings account without naming a specific bank"
            ),
            AccountSetupItem(
                id = "cash",
                name = "Cash Wallet",
                type = AccountType.CASH,
                institution = "Physical Cash",
                icon = Icons.Default.Payments,
                isSelected = true,
                initialBalanceText = "3000",
                description = "Daily cash in hand & pocket money"
            ),
            AccountSetupItem(
                id = "digital_wallet",
                name = "Digital Wallet / UPI",
                type = AccountType.CASH,
                institution = "GPay / Paytm / PhonePe / Apple Pay",
                icon = Icons.Default.QrCodeScanner,
                isSelected = true,
                initialBalanceText = "1500",
                description = "Quick mobile payments and instant digital wallets"
            ),
            AccountSetupItem(
                id = "sbi",
                name = "State Bank of India (SBI)",
                type = AccountType.BANK,
                institution = "SBI",
                icon = Icons.Default.AccountBalance,
                isSelected = false,
                initialBalanceText = "15000",
                description = "Salary / Primary savings account"
            ),
            AccountSetupItem(
                id = "hdfc",
                name = "HDFC Bank",
                type = AccountType.BANK,
                institution = "HDFC Bank",
                icon = Icons.Default.AccountBalance,
                isSelected = false,
                initialBalanceText = "50000",
                description = "Main banking & operations"
            ),
            AccountSetupItem(
                id = "icici",
                name = "ICICI Bank",
                type = AccountType.BANK,
                institution = "ICICI Bank",
                icon = Icons.Default.AccountBalance,
                isSelected = false,
                initialBalanceText = "20000",
                description = "Savings & transfers"
            ),
            AccountSetupItem(
                id = "credit_card",
                name = "Credit Card",
                type = AccountType.CREDIT_CARD,
                institution = "Credit Card Issuer",
                icon = Icons.Default.CreditCard,
                isSelected = false,
                initialBalanceText = "0",
                creditLimitText = "100000",
                description = "Track credit bills, billing cycle & credit utilization"
            ),
            AccountSetupItem(
                id = "investments",
                name = "Mutual Funds / Stocks",
                type = AccountType.INVESTMENT,
                institution = "Investment Broker",
                icon = Icons.Default.TrendingUp,
                isSelected = false,
                initialBalanceText = "50000",
                description = "Track long-term portfolio & assets"
            )
        )
    }

    var showAddCustomAccountDialog by remember { mutableStateOf(false) }

    // Step 4: Monthly Budget
    var wantMonthlyBudget by remember { mutableStateOf(true) }
    var monthlyBudgetText by remember { mutableStateOf("30000") }
    var autoDistributeCategories by remember { mutableStateOf(true) }

    // Step 5: Security / PIN
    var wantPinSecurity by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }

    val goals = listOf(
        FinancialGoalOption(
            id = "TRACK_EXPENSES",
            title = "Track Daily Expenses",
            description = "Know where every penny goes with lightning-fast entry and clear category breakdown.",
            icon = Icons.Default.ReceiptLong,
            badgeColor = FinancialColors.accent
        ),
        FinancialGoalOption(
            id = "SAVE_MONEY",
            title = "Cut Overspending & Save",
            description = "Set strict category limits, monitor alerts, and hit monthly savings targets.",
            icon = Icons.Default.Savings,
            badgeColor = FinancialColors.income
        ),
        FinancialGoalOption(
            id = "MANAGE_CARDS",
            title = "Manage Accounts & Cards",
            description = "Keep multiple bank accounts, cash registers, and credit card limits in sync.",
            icon = Icons.Default.CreditCard,
            badgeColor = FinancialColors.transfer
        ),
        FinancialGoalOption(
            id = "BUILD_WEALTH",
            title = "Track Net Worth & Assets",
            description = "Holistic view of assets, liquid cash, investments, and liabilities in one place.",
            icon = Icons.Default.AccountBalanceWallet,
            badgeColor = GeoTertiary
        )
    )

    val currencies = listOf(
        Triple("INR", "₹ (Indian Rupee)", true),
        Triple("USD", "$ (US Dollar)", false),
        Triple("EUR", "€ (Euro)", false),
        Triple("GBP", "£ (British Pound)", false),
        Triple("CAD", "$ (Canadian Dollar)", false),
        Triple("AUD", "$ (Australian Dollar)", false),
        Triple("AED", "د.إ (UAE Dirham)", false),
        Triple("SGD", "S$ (Singapore Dollar)", false),
        Triple("JPY", "¥ (Japanese Yen)", false)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditModeFromSettings) "Personalization & Setup" else "Welcome to Cash Tracker",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }
                    } else if (isEditModeFromSettings) {
                        IconButton(onClick = onFinishOnboarding) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                actions = {
                    if (!isEditModeFromSettings) {
                        TextButton(
                            onClick = {
                                viewModel.skipOnboarding(onFinishOnboarding)
                            }
                        ) {
                            Text("Skip", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        text = "Step ${currentStep + 1} of $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                // Final step: Save everything and complete onboarding!
                                val selectedAccs = accountsList.filter { it.isSelected }
                                val finalAccounts = if (selectedAccs.isEmpty()) {
                                    // Fallback at least 1 account
                                    listOf(accountsList.first().copy(isSelected = true))
                                } else {
                                    selectedAccs
                                }

                                val budgetAmount = if (wantMonthlyBudget) {
                                    CurrencyFormatter.parseToMinorUnits(monthlyBudgetText)
                                } else {
                                    null
                                }

                                val finalPin = if (wantPinSecurity && pinText.length == 4) pinText else null

                                viewModel.completeOnboarding(
                                    name = userName.trim().ifEmpty { "My Wallet" },
                                    currency = selectedCurrency,
                                    isIndianFormat = isIndianFormat,
                                    accounts = finalAccounts,
                                    monthlyBudget = budgetAmount,
                                    autoDistributeCategories = autoDistributeCategories,
                                    pin = finalPin,
                                    goal = selectedGoal,
                                    onComplete = onFinishOnboarding
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_next_button")
                    ) {
                        Text(
                            text = if (currentStep == totalSteps - 1) {
                                if (isEditModeFromSettings) "Save Changes" else "Launch Cockpit"
                            } else {
                                "Continue"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep == totalSteps - 1) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1f) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                0 -> StepWelcomeAndGoal(
                    userName = userName,
                    onNameChange = { userName = it },
                    selectedGoal = selectedGoal,
                    onGoalSelect = { selectedGoal = it },
                    goals = goals
                )
                1 -> StepCurrencyAndFormat(
                    selectedCurrency = selectedCurrency,
                    onCurrencySelect = { curr, indian ->
                        selectedCurrency = curr
                        isIndianFormat = indian
                    },
                    isIndianFormat = isIndianFormat,
                    onFormatToggle = { isIndianFormat = it },
                    currencies = currencies
                )
                2 -> StepAccountsSelection(
                    accounts = accountsList,
                    currencyCode = selectedCurrency,
                    onToggleAccount = { item, isSelected ->
                        val index = accountsList.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            accountsList[index] = accountsList[index].copy(isSelected = isSelected)
                        }
                    },
                    onUpdateBalance = { item, balance ->
                        val index = accountsList.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            accountsList[index] = accountsList[index].copy(initialBalanceText = balance)
                        }
                    },
                    onAddCustomClick = { showAddCustomAccountDialog = true }
                )
                3 -> StepBudgetPreference(
                    wantMonthlyBudget = wantMonthlyBudget,
                    onWantBudgetChange = { wantMonthlyBudget = it },
                    monthlyBudgetText = monthlyBudgetText,
                    onBudgetTextChange = { monthlyBudgetText = it },
                    autoDistribute = autoDistributeCategories,
                    onAutoDistributeChange = { autoDistributeCategories = it },
                    currencyCode = selectedCurrency
                )
                4 -> StepSecurityAndReview(
                    userName = userName,
                    selectedGoal = selectedGoal,
                    goals = goals,
                    selectedCurrency = selectedCurrency,
                    selectedAccountsCount = accountsList.count { it.isSelected },
                    wantMonthlyBudget = wantMonthlyBudget,
                    monthlyBudgetText = monthlyBudgetText,
                    wantPin = wantPinSecurity,
                    onWantPinChange = { wantPinSecurity = it },
                    pin = pinText,
                    onPinChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinText = it }
                )
            }
        }
    }

    if (showAddCustomAccountDialog) {
        AddCustomAccountDialog(
            currencyCode = selectedCurrency,
            onAdd = { customItem ->
                accountsList.add(customItem)
                showAddCustomAccountDialog = false
            },
            onDismiss = { showAddCustomAccountDialog = false }
        )
    }
}

@Composable
private fun StepWelcomeAndGoal(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedGoal: String,
    onGoalSelect: (String) -> Unit,
    goals: List<FinancialGoalOption>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Personalize Your Money Journey",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tailor every feature to your lifestyle & financial habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "1. What should we call you?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                label = { Text("Your Name / Nickname") },
                placeholder = { Text("e.g. Alex, Ankit, Sarah") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_name_input")
            )
        }

        item {
            Text(
                text = "2. What is your primary focus?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Select what matters most right now. We'll tailor your dashboard accordingly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(goals) { goal ->
            val isSelected = selectedGoal == goal.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoalSelect(goal.id) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = goal.badgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(goal.icon, contentDescription = null, tint = goal.badgeColor)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onGoalSelect(goal.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCurrencyAndFormat(
    selectedCurrency: String,
    onCurrencySelect: (String, Boolean) -> Unit,
    isIndianFormat: Boolean,
    onFormatToggle: (Boolean) -> Unit,
    currencies: List<Triple<String, String, Boolean>>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Primary Currency & Numbering",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Select your base currency for tracking ledgers, net worth, and budgets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                text = "Choose Currency",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(currencies) { (code, label, defaultIndian) ->
            val isSelected = selectedCurrency == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCurrencySelect(code, defaultIndian) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(54.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onCurrencySelect(code, defaultIndian) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Indian Number Format (Lakhs & Crores)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isIndianFormat) "e.g. ₹ 1,50,000 (Lakhs format)" else "e.g. ₹ 150,000 (Standard thousands)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isIndianFormat,
                        onCheckedChange = onFormatToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun StepAccountsSelection(
    accounts: List<AccountSetupItem>,
    currencyCode: String,
    onToggleAccount: (AccountSetupItem, Boolean) -> Unit,
    onUpdateBalance: (AccountSetupItem, String) -> Unit,
    onAddCustomClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Select Your Accounts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Choose the accounts you actively use. You can select specific banks, or choose a generic 'Bank Account' if you prefer not to specify your bank.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recommended & Popular Accounts",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(
                    onClick = onAddCustomClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        items(accounts) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (item.isSelected) 1.5.dp else 1.dp,
                        color = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (item.isGenericBank) FinancialColors.accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        tint = if (item.isGenericBank) FinancialColors.accent else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (item.isGenericBank) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = FinancialColors.accent.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                "Generic / Private",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FinancialColors.accent,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.description.ifEmpty { item.institution },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = { onToggleAccount(item, it) }
                        )
                    }

                    if (item.isSelected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Starting Balance (${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()}):",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = item.initialBalanceText,
                                onValueChange = { onUpdateBalance(item, it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(52.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBudgetPreference(
    wantMonthlyBudget: Boolean,
    onWantBudgetChange: (Boolean) -> Unit,
    monthlyBudgetText: String,
    onBudgetTextChange: (String) -> Unit,
    autoDistribute: Boolean,
    onAutoDistributeChange: (Boolean) -> Unit,
    currencyCode: String
) {
    val symbol = CurrencyFormatter.getCurrencySymbol(currencyCode).trim()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Monthly Spending Target",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Setting a monthly budget keeps your expenses in check and alerts you before overspending.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWantBudgetChange(true) }
                    .border(
                        width = if (wantMonthlyBudget) 2.dp else 1.dp,
                        color = if (wantMonthlyBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (wantMonthlyBudget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FinancialColors.income.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = FinancialColors.income)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set a Monthly Spending Budget",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Recommended. We'll show real-time spend progress and alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = wantMonthlyBudget,
                        onClick = { onWantBudgetChange(true) }
                    )
                }
            }
        }

        if (wantMonthlyBudget) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Target Total Monthly Expense Limit",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = monthlyBudgetText,
                            onValueChange = onBudgetTextChange,
                            label = { Text("Monthly Budget Amount") },
                            prefix = { Text("$symbol ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_budget_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-distribute to Top Categories",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "Allocates realistic budgets for Food (30%), Groceries (20%), Bills (20%), Shopping (15%), & Entertainment (15%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoDistribute,
                                onCheckedChange = onAutoDistributeChange
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWantBudgetChange(false) }
                    .border(
                        width = if (!wantMonthlyBudget) 2.dp else 1.dp,
                        color = if (!wantMonthlyBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!wantMonthlyBudget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.TrendingFlat, contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep it Flexible (No Budget for now)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "You can always set up category budgets anytime from the Budget tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = !wantMonthlyBudget,
                        onClick = { onWantBudgetChange(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepSecurityAndReview(
    userName: String,
    selectedGoal: String,
    goals: List<FinancialGoalOption>,
    selectedCurrency: String,
    selectedAccountsCount: Int,
    wantMonthlyBudget: Boolean,
    monthlyBudgetText: String,
    wantPin: Boolean,
    onWantPinChange: (Boolean) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit
) {
    val matchedGoal = goals.firstOrNull { it.id == selectedGoal } ?: goals.first()
    val symbol = CurrencyFormatter.getCurrencySymbol(selectedCurrency).trim()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Your Customized Money Cockpit",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Everything is tailored and ready to launch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Account Owner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userName.ifBlank { "Personal Ledger" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = matchedGoal.badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = matchedGoal.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = matchedGoal.badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Active Accounts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$selectedAccountsCount Accounts Selected", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Primary Currency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$selectedCurrency ($symbol)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Monthly Budget Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (wantMonthlyBudget && monthlyBudgetText.isNotBlank()) "$symbol $monthlyBudgetText / month" else "Flexible (No limit)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (wantMonthlyBudget) FinancialColors.income else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Optional Security PIN
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("4-Digit PIN Security", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Protect your finances upon opening", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = wantPin,
                            onCheckedChange = onWantPinChange
                        )
                    }

                    if (wantPin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pin,
                            onValueChange = onPinChange,
                            label = { Text("Enter 4-digit PIN") },
                            placeholder = { Text("e.g. 1234") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomAccountDialog(
    currencyCode: String,
    onAdd: (AccountSetupItem) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var balanceText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Salary Account, Emergency Stash") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Bank / Institution Name") },
                    placeholder = { Text("e.g. Citi, Revolut, Cash") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(AccountType.BANK, AccountType.CASH, AccountType.CREDIT_CARD, AccountType.SAVINGS).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.replace("_", " ")) }
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Starting Balance ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            AccountSetupItem(
                                id = "custom_${System.currentTimeMillis()}",
                                name = name.trim(),
                                type = selectedType,
                                institution = institution.ifBlank { name.trim() },
                                icon = when (selectedType) {
                                    AccountType.BANK -> Icons.Default.AccountBalance
                                    AccountType.CASH -> Icons.Default.Payments
                                    AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                    AccountType.INVESTMENT -> Icons.Default.TrendingUp
                                    AccountType.SAVINGS -> Icons.Default.Savings
                                    else -> Icons.Default.AccountBalanceWallet
                                },
                                isSelected = true,
                                initialBalanceText = balanceText.ifBlank { "0" },
                                isGenericBank = false,
                                description = "Custom account",
                                isCustom = true
                            )
                        )
                    }
                }
            ) {
                Text("Add Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
