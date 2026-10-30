package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Category(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val isExpenseCategory: Boolean = true
) {
    FOOD("Food & Dining", Icons.Filled.Fastfood, Color(0xFFEF6C00)),
    HOUSING("Rent & Housing", Icons.Filled.Home, Color(0xFF1976D2)),
    TRANSPORT("Transportation", Icons.Filled.DirectionsCar, Color(0xFF0288D1)),
    UTILITIES("Bills & Utilities", Icons.Filled.Receipt, Color(0xFF7B1FA2)),
    ENTERTAINMENT("Entertainment", Icons.Filled.Movie, Color(0xFFE91E63)),
    SHOPPING("Shopping", Icons.Filled.ShoppingCart, Color(0xFFC2185B)),
    HEALTH("Healthcare", Icons.Filled.LocalHospital, Color(0xFFD32F2F)),
    SALARY("Salary & Wages", Icons.Filled.Payments, Color(0xFF2E7D32), isExpenseCategory = false),
    INVESTMENT("Investments", Icons.Filled.TrendingUp, Color(0xFF00796B), isExpenseCategory = false),
    BONUS("Bonus / Gift", Icons.Filled.CardGiftcard, Color(0xFF388E3C), isExpenseCategory = false),
    OTHER("Other", Icons.Filled.MoreHoriz, Color(0xFF616161));

    companion object {
        fun fromString(name: String?): Category {
            if (name == null) return OTHER
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}
