package com.daemon.expnesso.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daemon.expnesso.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseAnalysisViewModel : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(AnalysisPeriod.ONE_MONTH)
    val selectedPeriod: StateFlow<AnalysisPeriod> = _selectedPeriod

    private val _uiState = MutableStateFlow<ExpenseAnalysisUiState>(ExpenseAnalysisUiState.Loading)
    val uiState: StateFlow<ExpenseAnalysisUiState> = _uiState

    // This will be called from the UI layer to feed the transaction stream
    fun bindTransactions(transactionsFlow: StateFlow<List<Transaction>>, currentUserId: String) {
        viewModelScope.launch {
            // Combine the transactions stream and selected period
            combine(transactionsFlow, _selectedPeriod) { txList, period ->
                Pair(txList, period)
            }.collect { (txList, period) ->
                _uiState.value = ExpenseAnalysisUiState.Loading
                
                if (txList.isEmpty()) {
                    _uiState.value = ExpenseAnalysisUiState.Empty
                    return@collect
                }

                // Perform heavy analytics calculation off the main thread
                val analysis = withContext(Dispatchers.Default) {
                    ExpenseAnalyticsEngine.calculateAnalytics(txList, currentUserId, period)
                }
                
                if (analysis.totalExpense <= 0 && analysis.previousPeriodExpense <= 0) {
                     _uiState.value = ExpenseAnalysisUiState.Empty
                } else {
                     _uiState.value = ExpenseAnalysisUiState.Success(analysis)
                }
            }
        }
    }

    fun setPeriod(period: AnalysisPeriod) {
        _selectedPeriod.value = period
    }
}
