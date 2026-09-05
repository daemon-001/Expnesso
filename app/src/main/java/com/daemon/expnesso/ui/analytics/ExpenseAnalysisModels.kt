package com.daemon.expnesso.ui.analytics

import com.daemon.expnesso.data.model.Transaction
import java.time.LocalDate

enum class AnalysisPeriod(val displayName: String) {
    SEVEN_DAYS("7D"),
    ONE_MONTH("1M"),
    ONE_YEAR("1Y"),
    ALL("All")
}

data class CategoryExpense(
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int,
    val colorString: String // E.g., hex color for chart
)

data class ExpenseTrendPoint(
    val date: LocalDate,
    val label: String,
    val amount: Double
)

data class ExpenseAnalysis(
    val period: AnalysisPeriod,
    val totalExpense: Double,
    val previousPeriodExpense: Double,
    val percentageChange: Double, // Can be positive, negative, or 0.0
    val transactionCount: Int,
    val averagePerDay: Double,
    val averageTransaction: Double,
    val highestExpense: Transaction?,
    val categoryBreakdown: List<CategoryExpense>,
    val trendData: List<ExpenseTrendPoint>,
    val spendingComparison: Pair<Double, Double> // current vs previous
)

// UI State for the screen
sealed class ExpenseAnalysisUiState {
    object Loading : ExpenseAnalysisUiState()
    object Empty : ExpenseAnalysisUiState()
    data class Success(val analysis: ExpenseAnalysis) : ExpenseAnalysisUiState()
}
