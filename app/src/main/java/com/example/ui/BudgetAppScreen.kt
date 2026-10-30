package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SyncState
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.AppsScriptViewerDialog
import com.example.ui.components.BudgetProgressArc
import com.example.ui.components.CategoryBreakdownView
import com.example.ui.components.EditDashboardAmountDialog
import com.example.ui.components.OverviewCard
import com.example.ui.components.SetSpendingGoalDialog
import com.example.ui.components.SheetsConfigDialog
import com.example.ui.components.TransactionItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.TerracottaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetAppScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val sheetsConfig by viewModel.sheetsConfig.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val budgetSummary by viewModel.budgetSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val isAddDialogOpen by viewModel.isAddDialogOpen.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val isConfigDialogOpen by viewModel.isConfigDialogOpen.collectAsStateWithLifecycle()
    val isScriptViewerOpen by viewModel.isScriptViewerOpen.collectAsStateWithLifecycle()
    val isEditIncomeDialogOpen by viewModel.isEditIncomeDialogOpen.collectAsStateWithLifecycle()
    val isEditExpenseDialogOpen by viewModel.isEditExpenseDialogOpen.collectAsStateWithLifecycle()
    val isSetGoalDialogOpen by viewModel.isSetGoalDialogOpen.collectAsStateWithLifecycle()
    val monthlySpendingGoal by viewModel.monthlySpendingGoal.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testMessage by viewModel.testConnectionMessage.collectAsStateWithLifecycle()

    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .testTag("add_transaction_fab")
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add transaction",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Top Header: Month indicator & Profile / Cloud Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentMonthYear,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Mpangopesa",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Minimalist Sync Status Pill
                        MinimalSyncBadge(
                            syncState = syncState,
                            pendingCount = pendingCount,
                            isConnected = sheetsConfig.isConnected,
                            onClick = {
                                if (!sheetsConfig.isConnected) {
                                    viewModel.isConfigDialogOpen.value = true
                                } else {
                                    viewModel.triggerSync()
                                }
                            }
                        )

                        // Modern Fintech Profile Avatar / Settings Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.isConfigDialogOpen.value = true }
                                .testTag("settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "IK",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. Large Clean Total Balance Card (with Income & Expense indicators)
            item {
                OverviewCard(
                    summary = budgetSummary,
                    onEditIncome = { viewModel.isEditIncomeDialogOpen.value = true },
                    onEditExpense = { viewModel.isEditExpenseDialogOpen.value = true }
                )
            }

            // 3. Sleek Semi-Circular Progress Arc (Budget spent vs. Room Goal)
            item {
                BudgetProgressArc(
                    totalSpent = budgetSummary.totalExpense,
                    monthlyBudget = monthlySpendingGoal,
                    onEditGoal = { viewModel.isSetGoalDialogOpen.value = true }
                )
            }

            // 4. Category Spending Distribution
            if (budgetSummary.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownView(categories = budgetSummary.categoryBreakdown)
                }
            }

            // 5. Recent Transactions Header & Filter Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${transactions.size} total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search transactions, merchants, notes...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field")
                    )

                    // Minimal Filter Chips (All, Expenses, Income)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedFilter == TransactionFilter.ALL,
                            onClick = { viewModel.setFilter(TransactionFilter.ALL) },
                            label = { Text("All") },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_all")
                        )

                        FilterChip(
                            selected = selectedFilter == TransactionFilter.EXPENSES,
                            onClick = { viewModel.setFilter(TransactionFilter.EXPENSES) },
                            label = { Text("Expenses") },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_expenses")
                        )

                        FilterChip(
                            selected = selectedFilter == TransactionFilter.INCOME,
                            onClick = { viewModel.setFilter(TransactionFilter.INCOME) },
                            label = { Text("Income") },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_income")
                        )
                    }
                }
            }

            // 6. Recent Transactions List
            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching records" else "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button below to log your first transaction in KSh.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = transactions,
                    key = { it.id }
                ) { item ->
                    TransactionItem(
                        transaction = item,
                        onEdit = { viewModel.openEditDialog(item) }
                    )
                }
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Animated Add/Edit Transaction Screen with smooth slide-up and slide-down transition
    AnimatedVisibility(
        visible = isAddDialogOpen,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        AddTransactionScreen(
            transaction = editingTransaction,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { title, amount, type, category, date, notes ->
                viewModel.saveTransaction(title, amount, type, category, date, notes)
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
            }
        )
    }

    // Edit Monthly Income from Dashboard
    if (isEditIncomeDialogOpen) {
        EditDashboardAmountDialog(
            isIncome = true,
            currentAmount = budgetSummary.totalIncome,
            onDismiss = { viewModel.isEditIncomeDialogOpen.value = false },
            onSaveAmount = { newAmount -> viewModel.updateMonthlyIncome(newAmount) }
        )
    }

    // Edit Monthly Expense from Dashboard
    if (isEditExpenseDialogOpen) {
        EditDashboardAmountDialog(
            isIncome = false,
            currentAmount = budgetSummary.totalExpense,
            onDismiss = { viewModel.isEditExpenseDialogOpen.value = false },
            onSaveAmount = { newAmount -> viewModel.updateMonthlyExpense(newAmount) }
        )
    }

    // Set Monthly Spending Goal in Room DB
    if (isSetGoalDialogOpen) {
        SetSpendingGoalDialog(
            currentGoal = monthlySpendingGoal,
            onDismiss = { viewModel.isSetGoalDialogOpen.value = false },
            onSaveGoal = { amount -> viewModel.setMonthlySpendingGoal(amount) }
        )
    }

    if (isConfigDialogOpen) {
        SheetsConfigDialog(
            config = sheetsConfig,
            isTesting = isTesting,
            testMessage = testMessage,
            onDismiss = { viewModel.isConfigDialogOpen.value = false },
            onSaveConfig = { url, autoSync ->
                viewModel.updateConfig(url, autoSync)
            },
            onTestConnection = { url ->
                viewModel.testConnection(url)
            },
            onOpenScriptViewer = {
                viewModel.isScriptViewerOpen.value = true
            }
        )
    }

    if (isScriptViewerOpen) {
        AppsScriptViewerDialog(
            onDismiss = { viewModel.isScriptViewerOpen.value = false }
        )
    }
}

@Composable
private fun MinimalSyncBadge(
    syncState: SyncState,
    pendingCount: Int,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    // Hide offline badge on dashboard completely
    if (!isConnected) return

    val (icon, tint, label) = when {
        syncState is SyncState.Syncing -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.primary, "Syncing")
        pendingCount > 0 -> Triple(Icons.Default.CloudUpload, TerracottaPrimary, "$pendingCount")
        else -> Triple(Icons.Default.CloudDone, EmeraldPrimary, "Synced")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (syncState is SyncState.Syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
