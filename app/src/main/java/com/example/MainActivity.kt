package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.local.BudgetDatabase
import com.example.data.remote.SheetsApiClient
import com.example.data.repository.BudgetRepository
import com.example.ui.BudgetAppScreen
import com.example.ui.BudgetViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels {
        val database = BudgetDatabase.getDatabase(applicationContext)
        val repository = BudgetRepository(
            transactionDao = database.transactionDao(),
            goalDao = database.goalDao(),
            apiService = SheetsApiClient.apiService,
            context = applicationContext
        )
        BudgetViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BudgetAppScreen(viewModel = viewModel)
            }
        }
    }
}
