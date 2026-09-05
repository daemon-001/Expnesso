package com.daemon.expnesso.ui.analytics

import com.daemon.expnesso.data.model.Transaction
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ExpenseAnalyticsEngine {

    private val colorPalette = listOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#F9D56E", "#FF8C42",
        "#845EC2", "#D65DB1", "#FF9671", "#FFC75F", "#F9F871"
    )

    fun calculateAnalytics(
        allTransactions: List<Transaction>,
        currentUserId: String,
        period: AnalysisPeriod
    ): ExpenseAnalysis {
        val today = LocalDate.now()
        
        // Define current and previous period boundaries
        val (currentStart, currentEnd) = when (period) {
            AnalysisPeriod.SEVEN_DAYS -> today.minusDays(6) to today
            AnalysisPeriod.ONE_MONTH -> today.minusMonths(1) to today
            AnalysisPeriod.ONE_YEAR -> today.minusYears(1) to today
            AnalysisPeriod.ALL -> LocalDate.MIN to today
        }

        val (prevStart, prevEnd) = when (period) {
            AnalysisPeriod.SEVEN_DAYS -> today.minusDays(13) to today.minusDays(7)
            AnalysisPeriod.ONE_MONTH -> today.minusMonths(2) to today.minusMonths(1).minusDays(1)
            AnalysisPeriod.ONE_YEAR -> today.minusYears(2) to today.minusYears(1).minusDays(1)
            AnalysisPeriod.ALL -> LocalDate.MIN to LocalDate.MIN // No previous period for ALL
        }

        val currentPeriodExpenses = mutableListOf<Pair<Transaction, Double>>()
        val previousPeriodExpenses = mutableListOf<Pair<Transaction, Double>>()

        allTransactions.forEach { tx ->
            val date = tx.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val paidBy = tx.paidByUid.ifEmpty { tx.addedByUid }
            var myExpense = 0.0
            
            if (tx.splits.isNotEmpty()) {
                myExpense = tx.splits[currentUserId] ?: 0.0
            } else if (paidBy == currentUserId) {
                myExpense = tx.amount
            }

            if (myExpense > 0) {
                if (date in currentStart..currentEnd || (period == AnalysisPeriod.ALL)) {
                    currentPeriodExpenses.add(tx to myExpense)
                } else if (date in prevStart..prevEnd && period != AnalysisPeriod.ALL) {
                    previousPeriodExpenses.add(tx to myExpense)
                }
            }
        }

        val totalCurrent = currentPeriodExpenses.sumOf { it.second }
        val totalPrevious = previousPeriodExpenses.sumOf { it.second }
        
        val percentageChange = if (totalPrevious > 0) {
            ((totalCurrent - totalPrevious) / totalPrevious) * 100.0
        } else if (totalCurrent > 0) {
            100.0 // 100% increase if previous was 0 and current is > 0
        } else {
            0.0
        }

        val transactionCount = currentPeriodExpenses.size
        
        // Days count for average calculation
        val daysInPeriod = when (period) {
            AnalysisPeriod.SEVEN_DAYS -> 7
            AnalysisPeriod.ONE_MONTH -> ChronoUnit.DAYS.between(currentStart, currentEnd).toInt() + 1
            AnalysisPeriod.ONE_YEAR -> ChronoUnit.DAYS.between(currentStart, currentEnd).toInt() + 1
            AnalysisPeriod.ALL -> {
                val firstDate = currentPeriodExpenses.minByOrNull { 
                    it.first.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }?.first?.timestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: today
                maxOf(1, ChronoUnit.DAYS.between(firstDate, today).toInt() + 1)
            }
        }

        val avgPerDay = if (daysInPeriod > 0) totalCurrent / daysInPeriod else 0.0
        val avgPerTx = if (transactionCount > 0) totalCurrent / transactionCount else 0.0

        val highestExpense = currentPeriodExpenses.maxByOrNull { it.second }?.first

        // Category breakdown
        val categoryMap = mutableMapOf<String, Pair<Double, Int>>() // Name -> Pair(Amount, Count)
        currentPeriodExpenses.forEach { (tx, amount) ->
            val cat = tx.description.take(15) // Infer category from description
            val currentStats = categoryMap[cat] ?: Pair(0.0, 0)
            categoryMap[cat] = Pair(currentStats.first + amount, currentStats.second + 1)
        }

        val sortedCategories = categoryMap.entries.sortedByDescending { it.value.first }
        
        // Group into top 5 and "Other" if there are many
        val topCategories = mutableListOf<CategoryExpense>()
        var otherAmount = 0.0
        var otherCount = 0
        
        sortedCategories.forEachIndexed { index, entry ->
            if (index < 5 || sortedCategories.size <= 6) {
                topCategories.add(
                    CategoryExpense(
                        categoryName = entry.key,
                        amount = entry.value.first,
                        percentage = if (totalCurrent > 0) (entry.value.first / totalCurrent) * 100 else 0.0,
                        transactionCount = entry.value.second,
                        colorString = colorPalette[index % colorPalette.size]
                    )
                )
            } else {
                otherAmount += entry.value.first
                otherCount += entry.value.second
            }
        }
        
        if (otherAmount > 0) {
            topCategories.add(
                CategoryExpense(
                    categoryName = "Other",
                    amount = otherAmount,
                    percentage = if (totalCurrent > 0) (otherAmount / totalCurrent) * 100 else 0.0,
                    transactionCount = otherCount,
                    colorString = "#B0BEC5" // Greyish color for other
                )
            )
        }

        // Trend graph data
        val trendDataMap = mutableMapOf<LocalDate, Double>()
        currentPeriodExpenses.forEach { (tx, amount) ->
            val date = tx.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            trendDataMap[date] = (trendDataMap[date] ?: 0.0) + amount
        }

        val trendPoints = mutableListOf<ExpenseTrendPoint>()
        if (period == AnalysisPeriod.SEVEN_DAYS || period == AnalysisPeriod.ONE_MONTH) {
            // Fill missing days with 0
            var d = currentStart
            while (!d.isAfter(currentEnd)) {
                trendPoints.add(
                    ExpenseTrendPoint(
                        date = d,
                        label = "${d.dayOfMonth} ${d.month.name.take(3)}",
                        amount = trendDataMap[d] ?: 0.0
                    )
                )
                d = d.plusDays(1)
            }
        } else {
            // Group by month
            val monthlyMap = mutableMapOf<String, Double>()
            val monthlyDateMap = mutableMapOf<String, LocalDate>()
            
            trendDataMap.forEach { (date, amount) ->
                val monthKey = "${date.year}-${String.format("%02d", date.monthValue)}"
                monthlyMap[monthKey] = (monthlyMap[monthKey] ?: 0.0) + amount
                if (!monthlyDateMap.containsKey(monthKey)) {
                    monthlyDateMap[monthKey] = date.withDayOfMonth(1)
                }
            }
            
            monthlyMap.keys.sorted().forEach { key ->
                val date = monthlyDateMap[key]!!
                trendPoints.add(
                    ExpenseTrendPoint(
                        date = date,
                        label = "${date.month.name.take(3)} '${date.year.toString().takeLast(2)}",
                        amount = monthlyMap[key]!!
                    )
                )
            }
        }

        return ExpenseAnalysis(
            period = period,
            totalExpense = totalCurrent,
            previousPeriodExpense = totalPrevious,
            percentageChange = percentageChange,
            transactionCount = transactionCount,
            averagePerDay = avgPerDay,
            averageTransaction = avgPerTx,
            highestExpense = highestExpense,
            categoryBreakdown = topCategories.sortedByDescending { it.amount },
            trendData = trendPoints,
            spendingComparison = Pair(totalCurrent, totalPrevious)
        )
    }
}
