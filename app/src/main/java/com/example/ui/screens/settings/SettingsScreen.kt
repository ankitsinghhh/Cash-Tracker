package com.example.ui.screens.settings
import com.example.ui.theme.FinancialColors

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.BackupManager
import com.example.domain.CurrencyFormatter
import com.example.ui.MainViewModel
import com.example.ui.components.ChartPreferencesDialog
import com.example.data.model.InsightWidget
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class StatusDialogData(
    val title: String,
    val message: String,
    val isSuccess: Boolean = true,
    val details: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToCategoryManager: () -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPCManager: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val financialGoal by viewModel.financialGoal.collectAsStateWithLifecycle()
    val monthlyBudgetGoal by viewModel.monthlyBudgetGoal.collectAsStateWithLifecycle()
    val showAccountsTab by viewModel.showAccountsTab.collectAsStateWithLifecycle()
    val currencyCode by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val isIndianFormat by viewModel.isIndianFormat.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val storedPin by viewModel.storedPin.collectAsStateWithLifecycle()
    val currentPalette by viewModel.themePalette.collectAsStateWithLifecycle()
    val isPcManagerRunning by viewModel.isPcServerRunning.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val visibleCharts by viewModel.visibleCharts.collectAsStateWithLifecycle()
    val calendarHeatmap by viewModel.calendarHeatmap.collectAsStateWithLifecycle()
    var showChartsDialog by remember { mutableStateOf(false) }

    var showThemePaletteDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showResetContentsDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var showRestoreJsonDialog by remember { mutableStateOf(false) }
    var importCsvText by remember { mutableStateOf("") }
    var restoreJsonText by remember { mutableStateOf("") }
    var statusDialogData by remember { mutableStateOf<StatusDialogData?>(null) }

    // Direct Native File Launchers
    val importCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        }
                    }
                    if (!content.isNullOrBlank()) {
                        viewModel.importUniversalCsv(content) { result ->
                            if (result.success) {
                                statusDialogData = StatusDialogData(
                                    title = "Import Successful!",
                                    message = "Successfully imported ${result.totalImported} transactions into your records.",
                                    isSuccess = true,
                                    details = buildList {
                                        if (result.totalIncome > 0) add("Total Income: ${CurrencyFormatter.formatAmount(result.totalIncome, currencyCode, isIndianFormat)}")
                                        if (result.totalExpense > 0) add("Total Expense: ${CurrencyFormatter.formatAmount(result.totalExpense, currencyCode, isIndianFormat)}")
                                        if (result.accountsCreated.isNotEmpty()) add("New Accounts: ${result.accountsCreated.joinToString(", ")}")
                                        if (result.categoriesCreated.isNotEmpty()) add("New Categories: ${result.categoriesCreated.joinToString(", ")}")
                                    }
                                )
                            } else {
                                statusDialogData = StatusDialogData(
                                    title = "Import Failed",
                                    message = result.message.ifBlank { "Could not parse transactions from the chosen file." },
                                    isSuccess = false
                                )
                            }
                        }
                    } else {
                        statusDialogData = StatusDialogData(
                            title = "Empty File",
                            message = "The selected file is empty or could not be read.",
                            isSuccess = false
                        )
                    }
                } catch (e: Exception) {
                    statusDialogData = StatusDialogData(
                        title = "Import Error",
                        message = "Error reading file: ${e.localizedMessage ?: "Unknown error"}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    val restoreJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        }
                    }
                    if (!content.isNullOrBlank()) {
                        viewModel.restoreDatabaseJson(content) { success ->
                            statusDialogData = if (success) {
                                StatusDialogData(
                                    title = "Backup Restored!",
                                    message = "Your entire database including accounts, categories, transactions, budgets, and memos has been restored successfully.",
                                    isSuccess = true
                                )
                            } else {
                                StatusDialogData(
                                    title = "Restore Failed",
                                    message = "Failed to parse or restore data from the JSON backup file.",
                                    isSuccess = false
                                )
                            }
                        }
                    } else {
                        statusDialogData = StatusDialogData(
                            title = "Empty Backup File",
                            message = "The selected backup file is empty.",
                            isSuccess = false
                        )
                    }
                } catch (e: Exception) {
                    statusDialogData = StatusDialogData(
                        title = "Restore Error",
                        message = "Error reading file: ${e.localizedMessage ?: "Unknown error"}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun shareFile(fileName: String, content: String, mimeType: String) {
        try {
            val exportFile = File(context.cacheDir, fileName)
            exportFile.writeText(content)
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share $fileName"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text share
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_TITLE, fileName)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, fileName))
        }
    }

    fun shareText(title: String, content: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings & Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Personalization & Experience Group
            item {
                Text(
                    text = "Personalization & Accounts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToOnboarding() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Rerun Setup & Personalization",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Reconfigure banks, accounts, budgets, & financial goals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // User Name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditNameDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Profile / Account Owner", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        userName.ifBlank { "Personal Ledger (Tap to set)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider()

                        // Monthly Budget Goal -> Opens Budget Management
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToBudget() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Target Monthly Budget", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (monthlyBudgetGoal > 0L) {
                                            "${CurrencyFormatter.formatAmount(monthlyBudgetGoal, currencyCode, showDecimals = false)} / month • Tap to manage"
                                        } else {
                                            "Flexible / Not Set • Tap to set & manage"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (monthlyBudgetGoal > 0L) FinancialColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider()

                        // Accounts & Net Worth Tab Visibility Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Accounts & Net Worth Tab", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (showAccountsTab) "Enabled in bottom navigation bar" else "Disabled (hidden from bottom bar)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = showAccountsTab,
                                onCheckedChange = { viewModel.setShowAccountsTab(it) }
                            )
                        }

                        HorizontalDivider()

                        // Financial Strategy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToOnboarding() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Financial Strategy Focus", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        when (financialGoal) {
                                            "SAVE_MONEY" -> "Cut Overspending & Save"
                                            "MANAGE_CARDS" -> "Manage Accounts & Cards"
                                            "BUILD_WEALTH" -> "Track Net Worth & Assets"
                                            else -> "Track Daily Expenses"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Appearance & Themes Group
            item {
                Text(
                    text = "Appearance & Themes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Theme Mode Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = when (currentThemeMode) {
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK -> Icons.Default.DarkMode
                                    AppThemeMode.AMOLED -> Icons.Default.Contrast
                                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Theme Mode",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = currentThemeMode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Spacious Theme Mode Selector Grid (2x2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT).forEach { mode ->
                                    val isSelected = currentThemeMode == mode
                                    Surface(
                                        onClick = { viewModel.setThemeMode(mode) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                                                        else -> Icons.Default.DarkMode
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when (mode) {
                                                        AppThemeMode.SYSTEM -> "System"
                                                        AppThemeMode.LIGHT -> "Light"
                                                        AppThemeMode.DARK -> "Dark"
                                                        AppThemeMode.AMOLED -> "AMOLED"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(AppThemeMode.DARK, AppThemeMode.AMOLED).forEach { mode ->
                                    val isSelected = currentThemeMode == mode
                                    Surface(
                                        onClick = { viewModel.setThemeMode(mode) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        AppThemeMode.DARK -> Icons.Default.DarkMode
                                                        AppThemeMode.AMOLED -> Icons.Default.Contrast
                                                        else -> Icons.Default.BrightnessAuto
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when (mode) {
                                                        AppThemeMode.SYSTEM -> "System"
                                                        AppThemeMode.LIGHT -> "Light"
                                                        AppThemeMode.DARK -> "Dark"
                                                        AppThemeMode.AMOLED -> "AMOLED"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Color Palettes Title & Dialog Opener
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemePaletteDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Color Palette Accent",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${currentPalette.displayName} — ${currentPalette.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Horizontal Quick Palette Swatches
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AppThemePalette.values()) { palette ->
                                val isSelected = currentPalette == palette
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setThemePalette(palette) }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    // Dual Color Swatch Circle
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(palette.previewPrimary),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(palette.previewSecondary)
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = palette.displayName.replace(" ", "\n"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Charts & insights", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        Row(Modifier.fillMaxWidth().clickable { showChartsDialog = true }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Visible charts", style = MaterialTheme.typography.bodyLarge)
                                Text("${visibleCharts.size} of ${InsightWidget.entries.size} enabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, "Choose visible charts")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Calendar heatmap overlay", style = MaterialTheme.typography.bodyLarge)
                                Text("Shade Home calendar days by spending. Separate from the Stats heatmap.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = calendarHeatmap, onCheckedChange = viewModel::setCalendarHeatmap)
                        }
                    }
                }
            }

            // General Settings Group
            item {
                Text(
                    text = "General Preferences",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // Currency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCurrencyDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Primary Currency", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        "${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()} ($currencyCode)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider()

                        // Indian Number System Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Indian Numbering Format", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (isIndianFormat) "e.g. ₹ 1,23,456.00 (Lakhs/Crores)" else "e.g. ₹ 123,456.00 (Standard)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isIndianFormat,
                                onCheckedChange = { viewModel.setIndianNumberFormat(it) }
                            )
                        }

                        HorizontalDivider()

                        // Category Manager Shortcut
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCategoryManager() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Manage Categories & Subcategories", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Security Group
            item {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("App PIN Lock", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (isPinEnabled) "PIN protection is active" else "Disabled — anyone can open the app",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPinEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isPinEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        showPinSetupDialog = true
                                    } else {
                                        viewModel.setAppPin(null)
                                        Toast.makeText(context, "PIN Lock Disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (isPinEnabled) {
                            HorizontalDivider()

                            // Change PIN button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPinSetupDialog = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Change Security PIN", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }

                            HorizontalDivider()

                            // Test Lock App Now button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.lockApp() }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LockPerson, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Lock App Now", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                        Text("Immediately display PIN lock screen to test", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }

            // PC Manager Section
            item {
                Text(
                    text = "PC & Web Access",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPCManager() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Devices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "PC Manager",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    "Access Cash Tracker on your PC via web browser",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPcManagerRunning) {
                                Surface(
                                    color = FinancialColors.income.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, FinancialColors.income.copy(alpha = 0.5f)),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "RUNNING",
                                        color = FinancialColors.income,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Data Management & Backups
            item {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // Export Excel / CSV File
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val txs = viewModel.allTransactionsWithDetails.value
                                        val csv = withContext(Dispatchers.IO) {
                                            BackupManager.exportRealbyteCsv(
                                                viewModel.repository.database,
                                                txs,
                                                currencyCode
                                            )
                                        }
                                        val fileName = "Cash_Tracker_Export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
                                        shareFile(fileName, csv, "text/csv")
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Export Data (Excel / CSV File)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text("Generates .csv spreadsheet compatible with Excel, Google Sheets & Cash Tracker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.Share, contentDescription = null)
                        }

                        HorizontalDivider()

                        // Import Excel / CSV File
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    importCsvText = ""
                                    showImportCsvDialog = true
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = FinancialColors.income)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Import Excel / CSV File", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text("Select .csv / .xlsx file or paste data from Cash Tracker export", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider()

                        // Full JSON Backup File
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val json = withContext(Dispatchers.IO) {
                                            BackupManager.createJsonBackup(viewModel.repository.database)
                                        }
                                        val fileName = "Cash_Tracker_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                                        shareFile(fileName, json, "application/json")
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Backup Database (JSON File)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text("Complete offline file backup of accounts, categories, transactions & budgets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.Share, contentDescription = null)
                        }

                        HorizontalDivider()

                        // Restore JSON Backup File
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    restoreJsonText = ""
                                    showRestoreJsonDialog = true
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Restore Database from File", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text("Pick a .json backup file to restore your full ledger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Reset Section
            item {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = FinancialColors.expense
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // Reset contents only
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetContentsDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = com.example.ui.theme.WarningAmber)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Reset contents only (Others remain)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                    Text("Erase all transactions and memos; keep accounts & categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider()

                        // A complete reset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetConfirmDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = FinancialColors.expense)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("A complete reset", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = FinancialColors.expense))
                                    Text("Erase everything and restore clean factory configuration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // About App
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAbout() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "About App",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "About App",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Currency Selector Dialog
    if (showCurrencyDialog) {
        val currencies = listOf(
            "INR" to "Indian Rupee (₹)",
            "USD" to "US Dollar ($)",
            "EUR" to "Euro (€)",
            "GBP" to "British Pound (£)",
            "JPY" to "Japanese Yen (¥)",
            "CAD" to "Canadian Dollar (CA$)",
            "AUD" to "Australian Dollar (AU$)",
            "AED" to "UAE Dirham (AED)",
            "SGD" to "Singapore Dollar (S$)"
        )

        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Primary Currency", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(currencies) { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPrimaryCurrency(code)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            if (currencyCode == code) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var isConfirmStep by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    if (!isConfirmStep) "Set 4-Digit PIN" else "Confirm Your PIN",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (!isConfirmStep) "Choose a 4-digit numeric code to protect your records" else "Re-enter the 4-digit code to confirm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = if (!isConfirmStep) pinInput else confirmPinInput,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                if (!isConfirmStep) pinInput = input else confirmPinInput = input
                                errorMsg = null
                            }
                        },
                        label = { Text(if (!isConfirmStep) "4-Digit PIN" else "Confirm 4-Digit PIN") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        isError = errorMsg != null,
                        supportingText = errorMsg?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isConfirmStep) {
                            if (pinInput.length == 4) {
                                isConfirmStep = true
                                errorMsg = null
                            } else {
                                errorMsg = "Please enter all 4 digits"
                            }
                        } else {
                            if (confirmPinInput == pinInput) {
                                viewModel.setAppPin(pinInput)
                                showPinSetupDialog = false
                                Toast.makeText(context, "PIN Protection Enabled Successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                errorMsg = "PINs do not match. Please try again."
                                confirmPinInput = ""
                            }
                        }
                    },
                    enabled = if (!isConfirmStep) pinInput.length == 4 else confirmPinInput.length == 4
                ) {
                    Text(if (!isConfirmStep) "Next" else "Save & Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isConfirmStep) {
                            isConfirmStep = false
                            confirmPinInput = ""
                            errorMsg = null
                        } else {
                            showPinSetupDialog = false
                        }
                    }
                ) {
                    Text(if (isConfirmStep) "Back" else "Cancel")
                }
            }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Profile / Owner Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Name / Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(nameInput.trim())
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Monthly Budget Goal Dialog
    if (showEditBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf(if (monthlyBudgetGoal > 0L) (monthlyBudgetGoal / 100).toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = { Text("Target Monthly Spending Limit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Set your overall target spending limit for each month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Monthly Budget (${CurrencyFormatter.getCurrencySymbol(currencyCode).trim()})") },
                        placeholder = { Text("e.g. 30000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = CurrencyFormatter.parseToMinorUnits(budgetInput)
                        viewModel.setMonthlyBudgetGoal(amount)
                        showEditBudgetDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Contents Only Dialog
    if (showResetContentsDialog) {
        AlertDialog(
            onDismissRequest = { showResetContentsDialog = false },
            title = { Text("Reset Contents Only?") },
            text = { Text("This will delete all transactions and memos. Your accounts, categories, and settings will remain unchanged.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetTransactionsOnly {
                            statusDialogData = StatusDialogData(
                                title = "Contents Cleared",
                                message = "All transactions and daily memos have been erased. Your accounts, categories, and settings remain untouched.",
                                isSuccess = true
                            )
                        }
                        showResetContentsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinancialColors.expense)
                ) {
                    Text("Reset Contents")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetContentsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Data Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("Warning: All transactions, custom accounts, and records will be deleted permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData {
                            statusDialogData = StatusDialogData(
                                title = "Complete Reset Successful",
                                message = "All transaction history and custom records have been deleted. Default bank accounts and standard categories have been restored.",
                                isSuccess = true
                            )
                        }
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinancialColors.expense)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Import Universal / Realbyte CSV Dialog
    if (showImportCsvDialog) {
        AlertDialog(
            onDismissRequest = { showImportCsvDialog = false },
            title = { Text("Import Transactions (Excel / CSV)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Primary Native File Pick Button
                    Button(
                        onClick = {
                            showImportCsvDialog = false
                            importCsvFileLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select File from Device (.csv / .xlsx / .txt)")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            " OR PASTE TEXT ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        placeholder = { Text("Date,Account,Category,Amount...\n2026/09/01,HDFC Bank,Food,250.00,Exp.,Lunch") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 180.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                importCsvText = "Date,Account,Category,Subcategory,Note,Amount,Income/Expense,Description,Currency,Transfer Account,Fee\n" +
                                        "\"2026/09/01 12:30\",\"HDFC Bank\",\"Food & Dining\",\"Lunch\",\"Team lunch\",450.00,Exp.,\"Cafe Coffee Day\",INR,\"\",\n" +
                                        "\"2026/09/01 09:15\",\"HDFC Bank\",\"Salary\",\"\",\"Monthly salary credit\",85000.00,Inc.,\"Employer Pvt Ltd\",INR,\"\",\n" +
                                        "\"2026/09/02 18:00\",\"Cash Wallet\",\"Groceries\",\"\",\"Fresh produce\",320.00,Exp.,\"Supermarket\",INR,\"\",\n" +
                                        "\"2026/09/03 14:00\",\"HDFC Bank\",\"\",\"\",\"ATM cash withdrawal\",2000.00,Transfer,\"\",INR,\"Cash Wallet\",0.00"
                            }
                        ) {
                            Text("Insert Sample Realbyte CSV", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importCsvText.isNotBlank()) {
                            viewModel.importUniversalCsv(importCsvText) { result ->
                                if (result.success) {
                                    statusDialogData = StatusDialogData(
                                        title = "Import Successful!",
                                        message = "Successfully imported ${result.totalImported} transactions into your records.",
                                        isSuccess = true,
                                        details = buildList {
                                            if (result.totalIncome > 0) add("Total Income: ${CurrencyFormatter.formatAmount(result.totalIncome, currencyCode, isIndianFormat)}")
                                            if (result.totalExpense > 0) add("Total Expense: ${CurrencyFormatter.formatAmount(result.totalExpense, currencyCode, isIndianFormat)}")
                                            if (result.accountsCreated.isNotEmpty()) add("New Accounts: ${result.accountsCreated.joinToString(", ")}")
                                            if (result.categoriesCreated.isNotEmpty()) add("New Categories: ${result.categoriesCreated.joinToString(", ")}")
                                        }
                                    )
                                } else {
                                    statusDialogData = StatusDialogData(
                                        title = "Import Failed",
                                        message = result.message.ifBlank { "No valid transactions found in pasted text." },
                                        isSuccess = false
                                    )
                                }
                            }
                            showImportCsvDialog = false
                        }
                    },
                    enabled = importCsvText.isNotBlank()
                ) {
                    Text("Import Pasted Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportCsvDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Restore JSON Dialog
    if (showRestoreJsonDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreJsonDialog = false },
            title = { Text("Restore from JSON Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Primary Native File Pick Button
                    Button(
                        onClick = {
                            showRestoreJsonDialog = false
                            restoreJsonFileLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup File (.json)")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            " OR PASTE JSON ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("{\n  \"appName\": \"Cash Tracker\",\n  ...\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 180.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.restoreDatabaseJson(restoreJsonText) { success ->
                                statusDialogData = if (success) {
                                    StatusDialogData(
                                        title = "Backup Restored!",
                                        message = "Your database has been restored successfully from pasted JSON.",
                                        isSuccess = true
                                    )
                                } else {
                                    StatusDialogData(
                                        title = "Restore Failed",
                                        message = "Failed to parse JSON backup text.",
                                        isSuccess = false
                                    )
                                }
                            }
                            showRestoreJsonDialog = false
                        }
                    },
                    enabled = restoreJsonText.isNotBlank()
                ) {
                    Text("Restore Pasted JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreJsonDialog = false }) { Text("Cancel") }
            }
        )
    }

    // General Operation Result Dialog
    statusDialogData?.let { data ->
        AlertDialog(
            onDismissRequest = { statusDialogData = null },
            icon = {
                Icon(
                    imageVector = if (data.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (data.isSuccess) FinancialColors.income else FinancialColors.expense,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (data.details.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                data.details.forEach { detail ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { statusDialogData = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Theme Palette Dialog
    if (showChartsDialog) ChartPreferencesDialog(visibleCharts, viewModel::setChartVisible, viewModel::setAllChartsVisible) { showChartsDialog = false }
    if (showThemePaletteDialog) {
        AlertDialog(
            onDismissRequest = { showThemePaletteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Select Theme Palette")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AppThemePalette.values()) { palette ->
                        val isSelected = currentPalette == palette
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemePalette(palette)
                                    showThemePaletteDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Dual Swatch
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(palette.previewPrimary),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(palette.previewSecondary)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = palette.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = palette.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setThemePalette(palette)
                                        showThemePaletteDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePaletteDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

