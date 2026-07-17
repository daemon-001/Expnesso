package com.daemon.expnesso.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val sessionId: String
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent
    
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session
    
    val currentUserId: String? = authRepository.currentUser?.uid

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            firestoreRepository.getSession(sessionId).collect { s ->
                _session.value = s
            }
        }
        viewModelScope.launch {
            firestoreRepository.getSessionTransactions(sessionId).collect { txList ->
                _transactions.value = txList
                _totalSpent.value = txList.sumOf { it.amount }
            }
        }
    }

    fun addTransaction(amount: Double, description: String, onSuccess: () -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val tx = Transaction(
                sessionId = sessionId,
                amount = amount,
                description = description,
                addedByUid = uid
            )
            firestoreRepository.addTransaction(tx)
            onSuccess()
        }
    }

    fun deleteTransaction(transactionId: String, adminUid: String, addedByUid: String) {
        val uid = authRepository.currentUser?.uid ?: return
        // Only admin or the person who added it can delete
        if (uid == adminUid || uid == addedByUid) {
            viewModelScope.launch {
                firestoreRepository.deleteTransaction(transactionId)
            }
        }
    }
}
