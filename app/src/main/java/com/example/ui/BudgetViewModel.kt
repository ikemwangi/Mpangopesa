package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BudgetRepository
import com.example.model.BudgetSummary
import com.example.model.Category
import com.example.model.CategorySpend
import com.example.model.SheetsConfig
import com.example.model.SyncState
import com.example.model.Transaction
import com.example.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransactionFilter {
    ALL,
    EXPENSES,
    INCOME
}

class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val syncState: StateFlow<SyncState> = repository.syncState
    val sheetsConfig: StateFlow<SheetsConfig> = repository.sheetsConfig
    val pendingCount: StateFlow<Int> = repository.pendingCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currentMonthKey: String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())

    val monthlySpendingGoal: StateFlow<Double> = repository.getMonthlyGoal(currentMonthKey).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 70000.0
    )

    private val _rawTransactions = repository.activeTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered transactions for the list
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        _rawTransactions,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        list.filter { item ->
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.EXPENSES -> item.type == TransactionType.EXPENSE
                TransactionFilter.INCOME -> item.type == TransactionType.INCOME
            }
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.notes.contains(query, ignoreCase = true) ||
                item.category.displayName.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic budget summary calculated from all active transactions
    val budgetSummary: StateFlow<BudgetSummary> = _rawTransactions.combine(_selectedFilter) { list, _ ->
        val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expense
        val savingsRate = if (income > 0) ((income - expense) / income * 100).toFloat().coerceAtLeast(0f) else 0f

        val expenseItems = list.filter { it.type == TransactionType.EXPENSE }
        val totalExpenseAmount = expenseItems.sumOf { it.amount }

        val categoryBreakdown = expenseItems
            .groupBy { it.category }
            .map { (cat, items) ->
                val sum = items.sumOf { it.amount }
                val pct = if (totalExpenseAmount > 0) (sum / totalExpenseAmount).toFloat() else 0f
                CategorySpend(cat, sum, pct)
            }
            .sortedByDescending { it.totalAmount }

        BudgetSummary(
            totalIncome = income,
            totalExpense = expense,
            netBalance = balance,
            savingsRate = savingsRate,
            categoryBreakdown = categoryBreakdown
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetSummary()
    )

    // UI Dialog States
    val isAddDialogOpen = MutableStateFlow(false)
    val editingTransaction = MutableStateFlow<Transaction?>(null)
    val isConfigDialogOpen = MutableStateFlow(false)
    val isScriptViewerOpen = MutableStateFlow(false)
    val isEditIncomeDialogOpen = MutableStateFlow(false)
    val isEditExpenseDialogOpen = MutableStateFlow(false)
    val isSetGoalDialogOpen = MutableStateFlow(false)
    val testConnectionMessage = MutableStateFlow<String?>(null)
    val isTestingConnection = MutableStateFlow(false)

    fun setMonthlySpendingGoal(amount: Double) {
        viewModelScope.launch {
            repository.setMonthlyGoal(currentMonthKey, amount)
            isSetGoalDialogOpen.value = false
        }
    }

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun updateMonthlyIncome(newAmount: Double) {
        viewModelScope.launch {
            val incomeItems = _rawTransactions.value.filter { it.type == TransactionType.INCOME }
            if (incomeItems.isNotEmpty()) {
                val primary = incomeItems.maxByOrNull { it.amount } ?: incomeItems.first()
                val otherIncome = incomeItems.filter { it.id != primary.id }.sumOf { it.amount }
                val adjusted = (newAmount - otherIncome).coerceAtLeast(0.0)
                repository.updateTransaction(primary.copy(amount = adjusted))
            } else {
                repository.addTransaction(
                    title = "Monthly Income",
                    amount = newAmount,
                    type = TransactionType.INCOME,
                    category = Category.SALARY,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                    notes = "Set from dashboard"
                )
            }
            isEditIncomeDialogOpen.value = false
        }
    }

    fun updateMonthlyExpense(newAmount: Double) {
        viewModelScope.launch {
            val expenseItems = _rawTransactions.value.filter { it.type == TransactionType.EXPENSE }
            if (expenseItems.isNotEmpty()) {
                val primary = expenseItems.maxByOrNull { it.amount } ?: expenseItems.first()
                val otherExpenses = expenseItems.filter { it.id != primary.id }.sumOf { it.amount }
                val adjusted = (newAmount - otherExpenses).coerceAtLeast(0.0)
                repository.updateTransaction(primary.copy(amount = adjusted))
            } else {
                repository.addTransaction(
                    title = "Monthly Expenses",
                    amount = newAmount,
                    type = TransactionType.EXPENSE,
                    category = Category.HOUSING,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                    notes = "Set from dashboard"
                )
            }
            isEditExpenseDialogOpen.value = false
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddDialog() {
        editingTransaction.value = null
        isAddDialogOpen.value = true
    }

    fun openEditDialog(transaction: Transaction) {
        editingTransaction.value = transaction
        isAddDialogOpen.value = true
    }

    fun closeAddEditDialog() {
        isAddDialogOpen.value = false
        editingTransaction.value = null
    }

    fun saveTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        date: String,
        notes: String
    ) {
        viewModelScope.launch {
            val toEdit = editingTransaction.value
            if (toEdit != null) {
                repository.updateTransaction(
                    toEdit.copy(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        date = date,
                        notes = notes
                    )
                )
            } else {
                repository.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    notes = notes
                )
            }
            closeAddEditDialog()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            closeAddEditDialog()
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncWithSheets(force = true)
        }
    }

    fun updateConfig(url: String, autoSync: Boolean) {
        repository.updateConfig(url, autoSync)
        isConfigDialogOpen.value = false
    }

    fun testConnection(url: String) {
        viewModelScope.launch {
            isTestingConnection.value = true
            testConnectionMessage.value = null
            val result = repository.testConnection(url)
            result.onSuccess {
                testConnectionMessage.value = "Success: $it"
            }.onFailure {
                testConnectionMessage.value = "Error: ${it.localizedMessage}"
            }
            isTestingConnection.value = false
        }
    }

    class Factory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
                return BudgetViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
