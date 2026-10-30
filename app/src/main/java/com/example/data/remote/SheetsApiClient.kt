package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SheetsApiClient {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: GoogleSheetsApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleSheetsApiService::class.java)
    }

    /**
     * Google Apps Script template mapped to:
     * Column A: ID
     * Column B: Date
     * Column C: Type
     * Column D: Category
     * Column E: Amount
     * Column F: Notes
     */
    val DEFAULT_APPS_SCRIPT_TEMPLATE = """
        // --- Mpangopesa Google Apps Script Web App ---
        // Header Schema:
        // A: ID | B: Date | C: Type | D: Category | E: Amount | F: Notes
        //
        // Quick Setup:
        // 1. Open your Google Sheet (sheets.google.com).
        // 2. Go to Extensions > Apps Script.
        // 3. Replace all code in Code.gs with this script and click Save (💾).
        // 4. Click Deploy > New deployment > Web app.
        //    - Execute as: 'Me'
        //    - Who has access: 'Anyone'
        // 5. Click Deploy, Authorize access, and copy the Web App URL into Mpangopesa!

        function setupSheet() {
          const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
          if (sheet.getLastRow() === 0) {
            sheet.appendRow(["ID", "Date", "Type", "Category", "Amount", "Notes"]);
            sheet.getRange(1, 1, 1, 6).setFontWeight("bold").setBackground("#D1FAE5");
          }
        }

        function doGet(e) {
          setupSheet();
          const action = (e && e.parameter && e.parameter.action) || 'getTransactions';
          if (action === 'ping') {
            return ContentService.createTextOutput(JSON.stringify({
              status: 'success',
              message: 'Connected to Google Sheet successfully!'
            })).setMimeType(ContentService.MimeType.JSON);
          }

          const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
          const data = sheet.getDataRange().getValues();
          if (data.length <= 1) {
            return ContentService.createTextOutput(JSON.stringify({ status: 'success', items: [] }))
              .setMimeType(ContentService.MimeType.JSON);
          }

          const items = [];
          for (let i = 1; i < data.length; i++) {
            const row = data[i];
            if (row[0]) {
              const rowNotes = String(row[5] || '');
              items.push({
                id: String(row[0]),
                date: String(row[1] || ''),
                type: String(row[2] || 'EXPENSE'),
                category: String(row[3] || 'OTHER'),
                amount: Number(row[4] || 0),
                notes: rowNotes,
                title: rowNotes || String(row[3] || 'Transaction'),
                remoteRowId: String(i + 1),
                updatedAt: Date.now()
              });
            }
          }
          return ContentService.createTextOutput(JSON.stringify({ status: 'success', items: items }))
            .setMimeType(ContentService.MimeType.JSON);
        }

        function doPost(e) {
          setupSheet();
          try {
            const payload = JSON.parse(e.postData.contents);
            const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
            const data = sheet.getDataRange().getValues();
            const rowMap = {}; // ID -> row index (1-based)
            for (let i = 1; i < data.length; i++) {
              rowMap[String(data[i][0])] = i + 1;
            }

            // Handle deletions
            if (payload.deletedIds && payload.deletedIds.length > 0) {
              const toDelete = payload.deletedIds.map(id => rowMap[id]).filter(Boolean).sort((a,b) => b-a);
              toDelete.forEach(r => sheet.deleteRow(r));
            }

            // Refresh row map after deletions
            const updatedData = sheet.getDataRange().getValues();
            const updatedRowMap = {};
            for (let i = 1; i < updatedData.length; i++) {
              updatedRowMap[String(updatedData[i][0])] = i + 1;
            }

            const syncedIds = [];
            if (payload.items && payload.items.length > 0) {
              payload.items.forEach(item => {
                const existingRow = updatedRowMap[item.id];
                // Columns: A: ID, B: Date, C: Type, D: Category, E: Amount, F: Notes
                const notesValue = item.notes || item.title || '';
                const rowValues = [
                  item.id,
                  item.date,
                  item.type,
                  item.category,
                  item.amount,
                  notesValue
                ];
                if (existingRow) {
                  sheet.getRange(existingRow, 1, 1, 6).setValues([rowValues]);
                } else {
                  sheet.appendRow(rowValues);
                }
                syncedIds.push(item.id);
              });
            }

            return ContentService.createTextOutput(JSON.stringify({
              status: 'success',
              syncedIds: syncedIds,
              message: 'Sync completed'
            })).setMimeType(ContentService.MimeType.JSON);

          } catch (err) {
            return ContentService.createTextOutput(JSON.stringify({
              status: 'error',
              message: err.toString()
            })).setMimeType(ContentService.MimeType.JSON);
          }
        }
    """.trimIndent()
}
