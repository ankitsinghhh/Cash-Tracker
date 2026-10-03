package com.example.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Category
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CategoryIconResolver
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val mainCategories = remember(allCategories, selectedType) {
        allCategories.filter { it.type == selectedType && it.parentId == null }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var parentCategoryForSubcategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = { Text("Category Manager", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        text = { Text("Expense Categories", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        text = { Text("Income Categories", fontWeight = FontWeight.SemiBold) }
                    )
                }
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
            items(mainCategories, key = { it.id }) { cat ->
                val subcategories = allCategories.filter { it.parentId == cat.id }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(iconName = cat.iconName, colorHex = cat.colorHex, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(cat.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("${subcategories.size} subcategories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row {
                                IconButton(onClick = { parentCategoryForSubcategory = cat }) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Subcategory")
                                }
                                IconButton(onClick = { categoryToEdit = cat }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                                }
                            }
                        }

                        // Subcategories Chips
                        if (subcategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subcategories.forEach { sub ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sub.name, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.deleteCategory(sub) },
                                                tint = ExpenseRed
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

    // Add or Edit Category Dialog
    if (showAddCategoryDialog || categoryToEdit != null) {
        val existing = categoryToEdit
        AddEditCategoryDialog(
            existingCategory = existing,
            defaultType = selectedType,
            onSave = { cat ->
                viewModel.saveCategory(cat)
                showAddCategoryDialog = false
                categoryToEdit = null
            },
            onDismiss = {
                showAddCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    // Add Subcategory Dialog
    if (parentCategoryForSubcategory != null) {
        val parent = parentCategoryForSubcategory!!
        var subName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { parentCategoryForSubcategory = null },
            title = { Text("New Subcategory in '${parent.name}'", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    label = { Text("Subcategory Name") },
                    placeholder = { Text("e.g. Coffee, Vegetables") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subName.isNotBlank()) {
                            viewModel.saveCategory(
                                Category(
                                    name = subName,
                                    type = parent.type,
                                    iconName = parent.iconName,
                                    colorHex = parent.colorHex,
                                    parentId = parent.id
                                )
                            )
                            parentCategoryForSubcategory = null
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { parentCategoryForSubcategory = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddEditCategoryDialog(
    existingCategory: Category?,
    defaultType: TransactionType,
    onSave: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(existingCategory) { mutableStateOf(existingCategory?.name ?: "") }
    var selectedIcon by remember(existingCategory) { mutableStateOf(existingCategory?.iconName ?: "restaurant") }
    var selectedColorHex by remember(existingCategory) { mutableStateOf(existingCategory?.colorHex ?: "#0D9488") }

    val iconOptions = listOf(
        "restaurant", "shopping_cart", "directions_car", "local_gas_station",
        "shopping_bag", "receipt_long", "home", "movie", "medical_services",
        "subscriptions", "flight", "spa", "school", "card_giftcard",
        "trending_up", "payments", "work", "show_chart", "savings", "apartment"
    )

    val colorOptions = listOf(
        "#0D9488", "#10B981", "#059669", "#3B82F6", "#6366F1",
        "#8B5CF6", "#EC4899", "#F43F5E", "#EF4444", "#F59E0B",
        "#14B8A6", "#64748B"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingCategory != null) "Edit Category" else "New Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconOptions.take(6).forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconResolver.getIcon(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.take(6).forEach { hex ->
                        val color = CategoryIconResolver.parseColor(hex)
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            existingCategory?.copy(
                                name = name,
                                iconName = selectedIcon,
                                colorHex = selectedColorHex
                            ) ?: Category(
                                name = name,
                                type = defaultType,
                                iconName = selectedIcon,
                                colorHex = selectedColorHex
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
