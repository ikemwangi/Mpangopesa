package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface GoogleSheetsApiService {

    @GET
    suspend fun fetchTransactions(
        @Url url: String,
        @Query("action") action: String = "getTransactions"
    ): Response<SyncResponse>

    @POST
    suspend fun pushBatch(
        @Url url: String,
        @Body request: SyncBatchRequest
    ): Response<SyncResponse>

    @GET
    suspend fun testConnection(
        @Url url: String,
        @Query("action") action: String = "ping"
    ): Response<SyncResponse>
}
