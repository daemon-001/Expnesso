package com.daemon.expnesso.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daemon.expnesso.data.model.Debt
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.model.User
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    initialSessionId: String
) : ViewModel() {

    val currentUserId: String = authRepository.currentUser?.uid ?: ""

    private val _currentSessionId = MutableStateFlow(initialSessionId)
    val currentSessionId: StateFlow<String> = _currentSessionId

    private val _allSessions = MutableStateFlow<List<Session>>(emptyList())
    val allSessions: StateFlow<List<Session>> = _allSessions

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _sessionMembers = MutableStateFlow<Map<String, User>>(emptyMap())
    val sessionMembers: StateFlow<Map<String, User>> = _sessionMembers

    // Net balance for each user. Positive = they are owed. Negative = they owe.
    private val _netBalances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val netBalances: StateFlow<Map<String, Double>> = _netBalances

    private val _myCredits = MutableStateFlow<List<Debt>>(emptyList())
    val myCredits: StateFlow<List<Debt>> = _myCredits

    private val _myDebts = MutableStateFlow<List<Debt>>(emptyList())
    val myDebts: StateFlow<List<Debt>> = _myDebts

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent

    // Map of sessionId -> Pair(willGet, willPay)
    private val _sessionBalances = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val sessionBalances: StateFlow<Map<String, Pair<Double, Double>>> = _sessionBalances

    private var sessionJob: Job? = null
    private var transactionsJob: Job? = null
    private val sessionBalanceJobs = mutableMapOf<String, Job>()

    init {
        loadAllSessions()
        switchSession(initialSessionId)
    }

    private fun loadAllSessions() {
        viewModelScope.launch {
            firestoreRepository.getUserSessions(currentUserId).collect { sessions ->
                _allSessions.value = sessions
                calculateAllSessionBalances(sessions)
            }
        }
    }

    fun switchSession(sessionId: String) {
        if (sessionId.isBlank()) return
        _currentSessionId.value = sessionId
        sessionJob?.cancel()
        transactionsJob?.cancel()

        sessionJob = viewModelScope.launch {
            firestoreRepository.getSession(sessionId).collect { s ->
                _session.value = s
                if (s != null) {
                    val members = firestoreRepository.getUsers(s.memberUids)
                    _sessionMembers.value = members.associateBy { it.uid }
                    calculateBalances()
                }
            }
        }
        
        transactionsJob = viewModelScope.launch {
            firestoreRepository.getSessionTransactions(sessionId).collect { txList ->
                _transactions.value = txList
                // Calculate total spent (just the sum of all transactions for simplicity, or sum of current user's splits)
                // For a personal tracker, total spent is sum of amount.
                // For a group tracker, we might want to show total group spending, or just user's spending. We'll show total group spending.
                _totalSpent.value = txList.sumOf { it.amount }
                calculateBalances()
            }
        }
    }
    
    fun setAsDefaultSession(sessionId: String) {
        viewModelScope.launch {
            firestoreRepository.setDefaultSession(currentUserId, sessionId)
        }
    }

    private fun calculateBalances() {
        val txs = _transactions.value
        val members = _session.value?.memberUids ?: emptyList()
        val balances = mutableMapOf<String, Double>()
        members.forEach { balances[it] = 0.0 }

        txs.forEach { tx ->
            val paidBy = tx.paidByUid.ifEmpty { tx.addedByUid }
            balances[paidBy] = (balances[paidBy] ?: 0.0) + tx.amount

            if (tx.splits.isNotEmpty()) {
                tx.splits.forEach { (uid, amountOwed) ->
                    balances[uid] = (balances[uid] ?: 0.0) - amountOwed
                }
            } else {
                // If no splits defined, assume the person who added it owes it all (Personal mode)
                balances[paidBy] = (balances[paidBy] ?: 0.0) - tx.amount
            }
        }
        _netBalances.value = balances

        val debtors = balances.filterValues { it < -0.01 }.mapValues { -it.value }.toMutableMap()
        val creditors = balances.filterValues { it > 0.01 }.toMutableMap()
        
        val allDebts = mutableListOf<Debt>()
        
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtor = debtors.keys.first()
            val creditor = creditors.keys.first()
            
            val debtAmount = debtors[debtor]!!
            val creditAmount = creditors[creditor]!!
            
            val settledAmount = minOf(debtAmount, creditAmount)
            allDebts.add(Debt(debtor, creditor, settledAmount))
            
            debtors[debtor] = debtAmount - settledAmount
            creditors[creditor] = creditAmount - settledAmount
            
            if (debtors[debtor]!! < 0.01) debtors.remove(debtor)
            if (creditors[creditor]!! < 0.01) creditors.remove(creditor)
        }
        
        _myCredits.value = allDebts.filter { it.toUid == currentUserId }
        _myDebts.value = allDebts.filter { it.fromUid == currentUserId }
    }

    private fun calculateAllSessionBalances(sessions: List<Session>) {
        val currentSessionIds = sessions.map { it.id }
        
        // Cancel jobs for removed sessions
        sessionBalanceJobs.keys.toList().forEach { id ->
            if (id !in currentSessionIds) {
                sessionBalanceJobs[id]?.cancel()
                sessionBalanceJobs.remove(id)
                _sessionBalances.value = _sessionBalances.value - id
            }
        }
        
        // Start jobs for new sessions
        sessions.forEach { session ->
            if (session.id !in sessionBalanceJobs) {
                sessionBalanceJobs[session.id] = viewModelScope.launch {
                    firestoreRepository.getSessionTransactions(session.id).collect { txList ->
                        val balances = mutableMapOf<String, Double>()
                        session.memberUids.forEach { balances[it] = 0.0 }
                        
                        txList.forEach { tx ->
                            val paidBy = tx.paidByUid.ifEmpty { tx.addedByUid }
                            balances[paidBy] = (balances[paidBy] ?: 0.0) + tx.amount

                            if (tx.splits.isNotEmpty()) {
                                tx.splits.forEach { (uid, amountOwed) ->
                                    balances[uid] = (balances[uid] ?: 0.0) - amountOwed
                                }
                            } else {
                                balances[paidBy] = (balances[paidBy] ?: 0.0) - tx.amount
                            }
                        }
                        
                        val debtors = balances.filterValues { it < -0.01 }.mapValues { -it.value }.toMutableMap()
                        val creditors = balances.filterValues { it > 0.01 }.toMutableMap()
                        
                        val allDebts = mutableListOf<Debt>()
                        
                        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
                            val debtor = debtors.keys.first()
                            val creditor = creditors.keys.first()
                            
                            val debtAmount = debtors[debtor]!!
                            val creditAmount = creditors[creditor]!!
                            
                            val settledAmount = minOf(debtAmount, creditAmount)
                            allDebts.add(Debt(debtor, creditor, settledAmount))
                            
                            debtors[debtor] = debtAmount - settledAmount
                            creditors[creditor] = creditAmount - settledAmount
                            
                            if (debtors[debtor]!! < 0.01) debtors.remove(debtor)
                            if (creditors[creditor]!! < 0.01) creditors.remove(creditor)
                        }
                        
                        val willGet = allDebts.filter { it.toUid == currentUserId }.sumOf { it.amount }
                        val willPay = allDebts.filter { it.fromUid == currentUserId }.sumOf { it.amount }
                        
                        _sessionBalances.value = _sessionBalances.value + (session.id to Pair(willGet, willPay))
                    }
                }
            }
        }
    }

    fun addTransaction(amount: Double, description: String, paidByUid: String, splits: Map<String, Double>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val tx = Transaction(
                sessionId = _currentSessionId.value,
                amount = amount,
                description = description,
                addedByUid = currentUserId,
                paidByUid = paidByUid,
                splits = splits
            )
            firestoreRepository.addTransaction(tx)
            onSuccess()
        }
    }

    fun deleteTransaction(transactionId: String, adminUid: String, addedByUid: String) {
        if (currentUserId == adminUid || currentUserId == addedByUid) {
            viewModelScope.launch {
                firestoreRepository.deleteTransaction(transactionId)
            }
        }
    }

    fun createSession(name: String, onSuccess: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newSessionId = firestoreRepository.createSession(name, currentUserId)
            switchSession(newSessionId)
            onSuccess(newSessionId)
        }
    }

    fun joinSession(inviteCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (inviteCode.isBlank()) return
        viewModelScope.launch {
            try {
                val joinedSessionId = firestoreRepository.joinSession(inviteCode, currentUserId)
                if (joinedSessionId != null) {
                    loadAllSessions()
                    switchSession(joinedSessionId)
                    onSuccess()
                } else {
                    onError("Invalid invite code")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to join")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun deleteSession(sessionId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                firestoreRepository.softDeleteSession(sessionId)
                
                // If the deleted session was the current one, switch to the first available session or clear
                if (currentSessionId.value == sessionId) {
                    val remaining = allSessions.value.filter { it.id != sessionId }
                    if (remaining.isNotEmpty()) {
                        switchSession(remaining.first().id)
                    } else {
                        _session.value = null
                        _transactions.value = emptyList()
                        _sessionMembers.value = emptyMap()
                        _netBalances.value = emptyMap()
                        _myCredits.value = emptyList()
                        _myDebts.value = emptyList()
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete book")
            }
        }
    }
}
