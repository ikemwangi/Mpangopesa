package com.example.model

sealed interface SyncState {
    data class Idle(
        val lastSyncTimestamp: Long? = null,
        val pendingCount: Int = 0
    ) : SyncState

    data class Syncing(
        val progressMessage: String = "Syncing with Google Sheets..."
    ) : SyncState

    data class Success(
        val lastSyncTimestamp: Long = System.currentTimeMillis(),
        val pushedCount: Int = 0,
        val pulledCount: Int = 0
    ) : SyncState

    data class Error(
        val errorMessage: String,
        val lastSyncTimestamp: Long? = null,
        val pendingCount: Int = 0
    ) : SyncState
}

data class SheetsConfig(
    val webAppUrl: String = "",
    val isConnected: Boolean = false,
    val autoSync: Boolean = true
)
