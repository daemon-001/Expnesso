package com.daemon.expnesso.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daemon.expnesso.ui.analytics.*
import com.daemon.expnesso.ui.theme.*
import com.daemon.expnesso.utils.FormatUtils
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel,
    analyticsViewModel: ExpenseAnalysisViewModel = viewModel()
) {
    val uiState by analyticsViewModel.uiState.collectAsState()
    val selectedPeriod by analyticsViewModel.selectedPeriod.collectAsState()
    
    // Bind transactions once
    LaunchedEffect(Unit) {
        analyticsViewModel.bindTransactions(dashboardViewModel.allUserTransactions, dashboardViewModel.currentUserId)
    }

    Scaffold(
        containerColor = PremiumBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Expense Analysis", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
                        Text("Understand where your money goes", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate("expense_history") }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("History", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PremiumBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Time Range Selector (Sticky at top)
            TimeFilterSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { analyticsViewModel.setPeriod(it) }
            )
            
            when (val state = uiState) {
                is ExpenseAnalysisUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                }
                is ExpenseAnalysisUiState.Empty -> {
                    EmptyAnalyticsState()
                }
                is ExpenseAnalysisUiState.Success -> {
                    AnalyticsContent(analysis = state.analysis)
                }
            }
        }
    }
}

@Composable
fun TimeFilterSelector(
    selectedPeriod: AnalysisPeriod,
    onPeriodSelected: (AnalysisPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(PremiumSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AnalysisPeriod.entries.forEach { period ->
            val isSelected = selectedPeriod == period
            val bgColor by animateColorAsState(if (isSelected) PrimaryAccent else Color.Transparent)
            val textColor by animateColorAsState(if (isSelected) Color.Black else TextSecondary)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPeriodSelected(period) }
                    .background(bgColor)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.displayName,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun AnalyticsContent(analysis: ExpenseAnalysis) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { CategoryDistributionSection(analysis) }
        item { TotalSummaryCard(analysis) }
        item { ExpenseTrendGraph(analysis) }
        item { SpendingComparisonSection(analysis) }
        item { AverageSpendingGrid(analysis) }
        item { HighestExpenseCard(analysis) }
        item { SmartInsightsSection(analysis) }
    }
}

@Composable
fun TotalSummaryCard(analysis: ExpenseAnalysis) {
    val isIncrease = analysis.percentageChange > 0
    val isDecrease = analysis.percentageChange < 0
    
    val changeColor = when {
        isDecrease -> SuccessGreen
        isIncrease -> ErrorRed
        else -> TextSecondary
    }
    
    val arrow = when {
        isIncrease -> "↑"
        isDecrease -> "↓"
        else -> "•"
    }
    
    val formattedChange = String.format("%.1f", Math.abs(analysis.percentageChange))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PremiumSurface)
            .padding(24.dp)
    ) {
        Column {
            Text("TOTAL EXPENSE", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹${FormatUtils.formatAmount(analysis.totalExpense)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$arrow $formattedChange%",
                    color = changeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = " compared to previous period",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ExpenseTrendGraph(analysis: ExpenseAnalysis) {
    val producer = remember(analysis.trendData) {
        val entries = analysis.trendData.mapIndexed { i, pt -> FloatEntry(i.toFloat(), pt.amount.toFloat()) }
        ChartEntryModelProducer(entries.ifEmpty { listOf(FloatEntry(0f,0f)) })
    }

    Column {
        Text("Expense Trend", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Your spending over time", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PremiumSurface)
                .padding(16.dp)
        ) {
            val chart = lineChart()
            Chart(
                chart = chart,
                chartModelProducer = producer,
                startAxis = rememberStartAxis(valueFormatter = { value, _ -> "₹${value.toInt()}" }),
                bottomAxis = rememberBottomAxis(valueFormatter = { value, _ ->
                    analysis.trendData.getOrNull(value.toInt())?.label ?: ""
                }),
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
    }
}

@Composable
fun CategoryDistributionSection(analysis: ExpenseAnalysis) {
    Column {
        Text("Where your money goes", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PremiumSurface)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CategoryDonutChart(
                    categories = analysis.categoryBreakdown,
                    totalExpense = analysis.totalExpense
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                // Breakdown List
                analysis.categoryBreakdown.forEach { cat ->
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(android.graphics.Color.parseColor(cat.colorString))))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat.categoryName, color = TextPrimary, fontSize = 14.sp)
                            }
                            Text("₹${FormatUtils.formatAmount(cat.amount)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${String.format("%.1f", cat.percentage)}% of total", color = TextSecondary, fontSize = 12.sp)
                            Text("${cat.transactionCount} transactions", color = TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (cat.percentage / 100).toFloat(),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(android.graphics.Color.parseColor(cat.colorString)),
                            trackColor = PremiumSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingComparisonSection(analysis: ExpenseAnalysis) {
    if (analysis.period == AnalysisPeriod.ALL) return
    
    val current = analysis.spendingComparison.first
    val prev = analysis.spendingComparison.second
    val max = maxOf(current, prev, 1.0)
    
    Column {
        Text("Spending Comparison", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PremiumSurface)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Current
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current", color = TextPrimary, fontSize = 14.sp)
                    Text("₹${FormatUtils.formatAmount(current)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (current / max).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = PrimaryAccent,
                    trackColor = PremiumSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                // Previous
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Previous", color = TextSecondary, fontSize = 14.sp)
                    Text("₹${FormatUtils.formatAmount(prev)}", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (prev / max).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = Color.Gray,
                    trackColor = PremiumSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AverageSpendingGrid(analysis: ExpenseAnalysis) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(PremiumSurface).padding(16.dp)
        ) {
            Column {
                Text("Average / day", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${FormatUtils.formatAmount(analysis.averagePerDay)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(PremiumSurface).padding(16.dp)
        ) {
            Column {
                Text("Avg / transaction", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${FormatUtils.formatAmount(analysis.averageTransaction)}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun HighestExpenseCard(analysis: ExpenseAnalysis) {
    val tx = analysis.highestExpense ?: return
    
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val localDate = tx.timestamp.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    
    Column {
        Text("Highest Expense", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PremiumSurface)
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(tx.description, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(localDate.format(formatter), color = TextSecondary, fontSize = 13.sp)
                }
                Text("₹${FormatUtils.formatAmount(tx.amount)}", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun SmartInsightsSection(analysis: ExpenseAnalysis) {
    Column {
        Text("Smart Insights", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryAccent.copy(alpha = 0.1f))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (analysis.categoryBreakdown.isNotEmpty()) {
                    InsightRow("💡", "${analysis.categoryBreakdown.first().categoryName} is your highest spending category this period.")
                }
                if (analysis.percentageChange != 0.0) {
                    val word = if (analysis.percentageChange > 0) "increased" else "decreased"
                    InsightRow("📈", "Your spending $word by ${String.format("%.1f", Math.abs(analysis.percentageChange))}% compared with the previous period.")
                }
                InsightRow("💳", "Your average transaction is ₹${FormatUtils.formatAmount(analysis.averageTransaction)}.")
                analysis.highestExpense?.let {
                    InsightRow("🔥", "Your highest expense was ₹${FormatUtils.formatAmount(it.amount)} for ${it.description}.")
                }
                InsightRow("📊", "You made ${analysis.transactionCount} transactions during this period.")
            }
        }
    }
}

@Composable
fun InsightRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun EmptyAnalyticsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Info, contentDescription = "Empty", tint = TextSecondary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No expenses yet", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "There are no expenses recorded for this period. Add an expense to start seeing your spending analysis.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
