package com.example

import com.example.data.local.TransactionEntity
import com.example.model.Category
import com.example.model.SyncStatus
import com.example.model.Transaction
import com.example.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetUnitTest {

    @Test
    fun transactionEntity_toDomain_conversionIsCorrect() {
        val entity = TransactionEntity(
            id = "tx_123",
            title = "Groceries",
            amount = 85.50,
            type = "EXPENSE",
            category = "FOOD",
            date = "2026-10-30",
            notes = "Organic apples and milk",
            syncStatus = "PENDING_INSERT",
            remoteRowId = null,
            updatedAt = 1000L
        )

        val domain = entity.toDomain()
        assertEquals("tx_123", domain.id)
        assertEquals("Groceries", domain.title)
        assertEquals(85.50, domain.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals(Category.FOOD, domain.category)
        assertEquals(SyncStatus.PENDING_INSERT, domain.syncStatus)
    }

    @Test
    fun transactionDomain_fromDomain_conversionIsCorrect() {
        val domain = Transaction(
            id = "tx_456",
            title = "Salary Deposit",
            amount = 3200.0,
            type = TransactionType.INCOME,
            category = Category.SALARY,
            date = "2026-10-01",
            notes = "Direct deposit",
            syncStatus = SyncStatus.SYNCED,
            remoteRowId = "5",
            updatedAt = 2000L
        )

        val entity = TransactionEntity.fromDomain(domain)
        assertEquals("tx_456", entity.id)
        assertEquals("Salary Deposit", entity.title)
        assertEquals("INCOME", entity.type)
        assertEquals("SALARY", entity.category)
        assertEquals("SYNCED", entity.syncStatus)
        assertEquals("5", entity.remoteRowId)
    }

    @Test
    fun exact6ColumnSheetSchema_mapsCorrectly() {
        val dto = com.example.data.remote.RemoteTransactionDto(
            id = "uuid_1",
            date = "2026-10-30",
            type = "Expense",
            category = "Food",
            amount = 14.50,
            notes = "Lunch with team"
        )

        assertEquals("uuid_1", dto.id)
        assertEquals("2026-10-30", dto.date)
        assertEquals("Expense", dto.type)
        assertEquals("Food", dto.category)
        assertEquals(14.50, dto.amount, 0.001)
        assertEquals("Lunch with team", dto.notes)
    }

    @Test
    fun monthlyGoalEntity_storesCorrectTargetAmount() {
        val goal = com.example.data.local.MonthlyGoalEntity(
            monthKey = "2026-10",
            targetAmount = 85000.0
        )
        assertEquals("2026-10", goal.monthKey)
        assertEquals(85000.0, goal.targetAmount, 0.001)
    }
}
