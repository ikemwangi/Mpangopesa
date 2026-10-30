package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Category
import com.example.model.SyncStatus
import com.example.model.Transaction
import com.example.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String,
    val notes: String = "",
    val syncStatus: String = "SYNCED",
    val remoteRowId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            title = title,
            amount = amount,
            type = TransactionType.fromString(type),
            category = Category.fromString(category),
            date = date,
            notes = notes,
            syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.SYNCED),
            remoteRowId = remoteRowId,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: Transaction): TransactionEntity {
            return TransactionEntity(
                id = domain.id,
                title = domain.title,
                amount = domain.amount,
                type = domain.type.name,
                category = domain.category.name,
                date = domain.date,
                notes = domain.notes,
                syncStatus = domain.syncStatus.name,
                remoteRowId = domain.remoteRowId,
                updatedAt = domain.updatedAt
            )
        }
    }
}
