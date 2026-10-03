package com.example.ui.screens.memos

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyMemo
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIconResolver
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.ExpenseRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMemosScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val memos by viewModel.memos.collectAsStateWithLifecycle()
    var showAddMemoDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Daily Calendar Memos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMemoDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Memo")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (memos.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Calendar Memos",
                        message = "Attach daily notes, payday memos, trip logs, or milestones to calendar dates.",
                        actionButton = {
                            Button(onClick = { showAddMemoDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Memo")
                            }
                        }
                    )
                }
            } else {
                items(memos, key = { it.dateString }) { memo ->
                    val color = CategoryIconResolver.parseColor(memo.colorHex)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp, 40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = memo.dateString,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = memo.memoText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deleteMemo(memo) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMemoDialog) {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var dateStr by remember { mutableStateOf(f.format(Date())) }
        var memoText by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#10B981") }

        AlertDialog(
            onDismissRequest = { showAddMemoDialog = false },
            title = { Text("New Daily Memo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it },
                        label = { Text("Memo Text") },
                        placeholder = { Text("e.g. Salary credited, Roadtrip day 1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memoText.isNotBlank() && dateStr.isNotBlank()) {
                            viewModel.saveMemo(dateStr, memoText, selectedColorHex)
                            showAddMemoDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoDialog = false }) { Text("Cancel") }
            }
        )
    }
}
