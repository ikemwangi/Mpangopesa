package com.example.model

enum class TransactionType {
    EXPENSE,
    INCOME;

    companion object {
        fun fromString(type: String?): TransactionType {
            return if (type?.trim()?.uppercase() == "INCOME") INCOME else EXPENSE
        }
    }
}

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE
}

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val date: String, // Format: YYYY-MM-DD
    val notes: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val remoteRowId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
