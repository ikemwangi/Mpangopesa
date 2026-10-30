package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteTransactionDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "type") val type: String,
    @Json(name = "category") val category: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "title") val title: String? = null,
    @Json(name = "remoteRowId") val remoteRowId: String? = null,
    @Json(name = "updatedAt") val updatedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SyncBatchRequest(
    @Json(name = "action") val action: String = "syncBatch",
    @Json(name = "items") val items: List<RemoteTransactionDto> = emptyList(),
    @Json(name = "deletedIds") val deletedIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String? = null,
    @Json(name = "items") val items: List<RemoteTransactionDto>? = null,
    @Json(name = "syncedIds") val syncedIds: List<String>? = null
)
