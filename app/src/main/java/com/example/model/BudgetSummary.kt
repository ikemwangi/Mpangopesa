package com.example.model

data class CategorySpend(
    val category: Category,
    val totalAmount: Double,
    val percentageOfTotal: Float
)

data class BudgetSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val savingsRate: Float = 0f,
    val categoryBreakdown: List<CategorySpend> = emptyList()
)
