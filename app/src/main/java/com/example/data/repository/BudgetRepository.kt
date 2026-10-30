package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.remote.GoogleSheetsApiService
import com.example.data.remote.RemoteTransactionDto
import com.example.data.remote.SyncBatchRequest
import com.example.model.Category
import com.example.model.SheetsConfig
import com.example.model.SyncState
import com.example.model.SyncStatus
import com.example.model.Transaction
import com.example.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

import com.example.data.local.GoalDao
import com.example.data.local.MonthlyGoalEntity

class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val apiService: GoogleSheetsApiService,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("budget_sheet_prefs", Context.MODE_PRIVATE)

    private val _sheetsConfig = MutableStateFlow(
        SheetsConfig(
            webAppUrl = prefs.getString("web_app_url", "") ?: "",
            isConnected = prefs.getBoolean("is_connected", false),
            autoSync = prefs.getBoolean("auto_sync", true)
        )
    )
    val sheetsConfig: StateFlow<SheetsConfig> = _sheetsConfig.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(
        SyncState.Idle(lastSyncTimestamp = prefs.getLong("last_sync_time", 0L).takeIf { it > 0 })
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Active transactions flow from Room DB, converted to domain models
    val activeTransactions: Flow<List<Transaction>> =
        transactionDao.getActiveTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    val pendingCount: Flow<Int> = transactionDao.getPendingCount()

    fun getMonthlyGoal(monthKey: String): Flow<Double> {
        return goalDao.getGoalForMonth(monthKey).map { it?.targetAmount ?: 75000.0 }
    }

    suspend fun setMonthlyGoal(monthKey: String, amount: Double) = withContext(Dispatchers.IO) {
        goalDao.setGoal(MonthlyGoalEntity(monthKey = monthKey, targetAmount = amount))
    }

    fun updateConfig(url: String, autoSync: Boolean) {
        val trimmed = url.trim()
        val isConfigured = trimmed.isNotEmpty()
        prefs.edit()
            .putString("web_app_url", trimmed)
            .putBoolean("is_connected", isConfigured)
            .putBoolean("auto_sync", autoSync)
            .apply()
        _sheetsConfig.value = SheetsConfig(webAppUrl = trimmed, isConnected = isConfigured, autoSync = autoSync)
    }

    suspend fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        date: String,
        notes: String
    ): Transaction = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val domain = Transaction(
            id = newId,
            title = title,
            amount = amount,
            type = type,
            category = category,
            date = date,
            notes = notes,
            syncStatus = SyncStatus.PENDING_INSERT,
            remoteRowId = null,
            updatedAt = now
        )

        // 1. Immediately insert into local Room DB cache
        transactionDao.insert(TransactionEntity.fromDomain(domain))

        // 2. Trigger auto-sync in background if configured
        if (_sheetsConfig.value.isConnected && _sheetsConfig.value.autoSync) {
            triggerSyncInternal()
        }

        domain
    }

    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val existing = transactionDao.getTransactionById(transaction.id)
        val now = System.currentTimeMillis()

        // If it was already pending insert, keep it as pending insert; otherwise mark pending update
        val newStatus = if (existing?.syncStatus == SyncStatus.PENDING_INSERT.name) {
            SyncStatus.PENDING_INSERT
        } else {
            SyncStatus.PENDING_UPDATE
        }

        val updated = transaction.copy(
            syncStatus = newStatus,
            updatedAt = now
        )

        transactionDao.update(TransactionEntity.fromDomain(updated))

        if (_sheetsConfig.value.isConnected && _sheetsConfig.value.autoSync) {
            triggerSyncInternal()
        }
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        val existing = transactionDao.getTransactionById(id) ?: return@withContext

        if (existing.syncStatus == SyncStatus.PENDING_INSERT.name) {
            // Never synced to Google Sheets, safe to delete locally right away
            transactionDao.hardDelete(id)
        } else {
            // Exists in Google Sheets; soft delete by marking PENDING_DELETE so sync can purge it remotely
            transactionDao.markForDelete(id, System.currentTimeMillis())
        }

        if (_sheetsConfig.value.isConnected && _sheetsConfig.value.autoSync) {
            triggerSyncInternal()
        }
    }

    suspend fun testConnection(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.testConnection(url)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()?.message ?: "Connected successfully!")
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string() ?: "Connection failed"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithSheets(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        triggerSyncInternal()
    }

    private suspend fun triggerSyncInternal(): Result<Unit> {
        val config = _sheetsConfig.value
        val pendingList = transactionDao.getPendingTransactions()

        if (!config.isConnected || config.webAppUrl.isBlank()) {
            // Offline / Unconfigured fallback: Simulate sync queue if user clicks sync without URL,
            // or inform user they need a Google Sheets Web App URL
            _syncState.value = SyncState.Error(
                errorMessage = "Google Sheets Web App URL not configured. Go to Settings to link your spreadsheet.",
                pendingCount = pendingList.size
            )
            return Result.failure(IllegalStateException("No Google Sheets URL configured"))
        }

        _syncState.value = SyncState.Syncing("Reconciling with Google Sheets...")

        return try {
            val pendingEntities = transactionDao.getPendingTransactions()
            val toPush = pendingEntities.filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
            val toDelete = pendingEntities.filter { it.syncStatus == SyncStatus.PENDING_DELETE.name }

            var pushedCount = 0

            // Step 1: Push pending local changes to remote Google Sheet if any exist
            if (toPush.isNotEmpty() || toDelete.isNotEmpty()) {
                val dtos = toPush.map {
                    val combinedNotes = if (it.notes.isNotBlank() && it.notes != it.title) "${it.title} - ${it.notes}" else it.title.ifBlank { it.notes }
                    RemoteTransactionDto(
                        id = it.id,
                        date = it.date,
                        type = it.type,
                        category = it.category,
                        amount = it.amount,
                        notes = combinedNotes,
                        title = it.title,
                        remoteRowId = it.remoteRowId,
                        updatedAt = it.updatedAt
                    )
                }

                val pushRequest = SyncBatchRequest(
                    action = "syncBatch",
                    items = dtos,
                    deletedIds = toDelete.map { it.id }
                )

                val pushResponse = apiService.pushBatch(config.webAppUrl, pushRequest)
                if (!pushResponse.isSuccessful) {
                    throw IllegalStateException("Push failed: HTTP ${pushResponse.code()}")
                }

                // Mark pushed items as SYNCED locally
                toPush.forEach {
                    transactionDao.updateSyncStatus(
                        id = it.id,
                        status = SyncStatus.SYNCED.name,
                        remoteRowId = it.remoteRowId,
                        updatedAt = it.updatedAt
                    )
                }

                // Remove deleted items from local Room DB
                if (toDelete.isNotEmpty()) {
                    transactionDao.hardDeleteAll(toDelete.map { it.id })
                }

                pushedCount = toPush.size + toDelete.size
            }

            // Step 2: Pull latest rows from Google Sheets
            val pullResponse = apiService.fetchTransactions(config.webAppUrl)
            var pulledCount = 0

            if (pullResponse.isSuccessful && pullResponse.body()?.status == "success") {
                val remoteItems = pullResponse.body()?.items ?: emptyList()
                val localPending = transactionDao.getPendingTransactions().associateBy { it.id }

                val entitiesToUpsert = mutableListOf<TransactionEntity>()
                for (remote in remoteItems) {
                    val local = localPending[remote.id]
                    val resolvedTitle = remote.title?.ifBlank { null } ?: remote.notes.ifBlank { remote.category }

                    if (local == null) {
                        // Clean remote row: insert or update as SYNCED
                        entitiesToUpsert.add(
                            TransactionEntity(
                                id = remote.id,
                                title = resolvedTitle,
                                amount = remote.amount,
                                type = remote.type,
                                category = remote.category,
                                date = remote.date,
                                notes = remote.notes,
                                syncStatus = SyncStatus.SYNCED.name,
                                remoteRowId = remote.remoteRowId,
                                updatedAt = remote.updatedAt
                            )
                        )
                        pulledCount++
                    } else {
                        // Conflict resolution: If local has unsaved changes, local wins if local timestamp >= remote
                        if (remote.updatedAt > local.updatedAt && local.syncStatus != SyncStatus.PENDING_DELETE.name) {
                            entitiesToUpsert.add(
                                TransactionEntity(
                                    id = remote.id,
                                    title = resolvedTitle,
                                    amount = remote.amount,
                                    type = remote.type,
                                    category = remote.category,
                                    date = remote.date,
                                    notes = remote.notes,
                                    syncStatus = SyncStatus.SYNCED.name,
                                    remoteRowId = remote.remoteRowId,
                                    updatedAt = remote.updatedAt
                                )
                            )
                            pulledCount++
                        }
                    }
                }

                if (entitiesToUpsert.isNotEmpty()) {
                    transactionDao.insertAll(entitiesToUpsert)
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()

            _syncState.value = SyncState.Success(
                lastSyncTimestamp = now,
                pushedCount = pushedCount,
                pulledCount = pulledCount
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val pending = transactionDao.getPendingTransactions().size
            _syncState.value = SyncState.Error(
                errorMessage = e.localizedMessage ?: "Sync error",
                pendingCount = pending
            )
            Result.failure(e)
        }
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val pending = transactionDao.getPendingTransactions()
        // Check if DB is completely empty
        val testExisting = transactionDao.getTransactionById("sample_1")
        if (testExisting == null) {
            val now = System.currentTimeMillis()
            val sampleItems = listOf(
                TransactionEntity(
                    id = "sample_salary",
                    title = "Monthly Salary",
                    amount = 145000.00,
                    type = TransactionType.INCOME.name,
                    category = Category.SALARY.name,
                    date = "2026-10-01",
                    notes = "Direct salary transfer",
                    syncStatus = SyncStatus.SYNCED.name,
                    remoteRowId = "2",
                    updatedAt = now - 86400000L * 28
                ),
                TransactionEntity(
                    id = "sample_rent",
                    title = "Apartment Rent",
                    amount = 35000.00,
                    type = TransactionType.EXPENSE.name,
                    category = Category.HOUSING.name,
                    date = "2026-10-02",
                    notes = "Monthly lease payment",
                    syncStatus = SyncStatus.SYNCED.name,
                    remoteRowId = "3",
                    updatedAt = now - 86400000L * 27
                ),
                TransactionEntity(
                    id = "sample_groceries",
                    title = "Carrefour Groceries",
                    amount = 12450.00,
                    type = TransactionType.EXPENSE.name,
                    category = Category.FOOD.name,
                    date = "2026-10-15",
                    notes = "Household grocery restock",
                    syncStatus = SyncStatus.SYNCED.name,
                    remoteRowId = "4",
                    updatedAt = now - 86400000L * 14
                ),
                TransactionEntity(
                    id = "sample_electric",
                    title = "KPLC Power & Water",
                    amount = 4800.00,
                    type = TransactionType.EXPENSE.name,
                    category = Category.UTILITIES.name,
                    date = "2026-10-18",
                    notes = "Tokens & water bill",
                    syncStatus = SyncStatus.SYNCED.name,
                    remoteRowId = "5",
                    updatedAt = now - 86400000L * 11
                ),
                TransactionEntity(
                    id = "sample_dining",
                    title = "Java House Dinner",
                    amount = 3200.00,
                    type = TransactionType.EXPENSE.name,
                    category = Category.FOOD.name,
                    date = "2026-10-24",
                    notes = "Family dinner",
                    syncStatus = SyncStatus.PENDING_INSERT.name,
                    remoteRowId = null,
                    updatedAt = now - 86400000L * 5
                ),
                TransactionEntity(
                    id = "sample_gas",
                    title = "Total Energies Fuel",
                    amount = 5000.00,
                    type = TransactionType.EXPENSE.name,
                    category = Category.TRANSPORT.name,
                    date = "2026-10-28",
                    notes = "Car full tank",
                    syncStatus = SyncStatus.PENDING_INSERT.name,
                    remoteRowId = null,
                    updatedAt = now - 86400000L * 1
                )
            )
            transactionDao.insertAll(sampleItems)
            goalDao.setGoal(MonthlyGoalEntity(monthKey = "2026-10", targetAmount = 70000.0))
        }
    }
}
