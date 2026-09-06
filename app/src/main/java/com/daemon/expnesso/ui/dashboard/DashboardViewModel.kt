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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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
    
    private val _isSessionsLoaded = MutableStateFlow(false)
    val isSessionsLoaded: StateFlow<Boolean> = _isSessionsLoaded

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _activityLogs = MutableStateFlow<List<com.daemon.expnesso.data.model.ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<com.daemon.expnesso.data.model.ActivityLog>> = _activityLogs

    private val _sessionMembers = MutableStateFlow<Map<String, User>>(emptyMap())
    val sessionMembers: StateFlow<Map<String, User>> = _sessionMembers

    // Net balance for each user. Positive = they are owed. Negative = they owe.
    private val _netBalances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val netBalances: StateFlow<Map<String, Double>> = _netBalances

    private val _myCredits = MutableStateFlow<List<Debt>>(emptyList())
    val myCredits: StateFlow<List<Debt>> = _myCredits

    private val _myDebts = MutableStateFlow<List<Debt>>(emptyList())
    val myDebts: StateFlow<List<Debt>> = _myDebts

    private val _allDebts = MutableStateFlow<List<Debt>>(emptyList())
    val allDebts: StateFlow<List<Debt>> = _allDebts

    private val _allKnownUsers = MutableStateFlow<Map<String, User>>(emptyMap())
    val allKnownUsers: StateFlow<Map<String, User>> = _allKnownUsers

    private val _allUserTransactionsMap = MutableStateFlow<Map<String, List<Transaction>>>(emptyMap())
    val allUserTransactions: StateFlow<List<Transaction>> = _allUserTransactionsMap
        .map { it.values.flatten().sortedByDescending { tx -> tx.timestamp } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent

    // Map of sessionId -> Pair(willGet, willPay)
    private val _sessionBalances = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val sessionBalances: StateFlow<Map<String, Pair<Double, Double>>> = _sessionBalances

    // Map of sessionId -> Double (user expense)
    private val _sessionUserExpenses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val sessionUserExpenses: StateFlow<Map<String, Double>> = _sessionUserExpenses

    // Map of sessionId -> List of all Debts in that session
    private val _sessionDebts = MutableStateFlow<Map<String, List<Debt>>>(emptyMap())
    val sessionDebts: StateFlow<Map<String, List<Debt>>> = _sessionDebts

    // Map of sessionId -> Map of userId to total amount spent (paid)
    private val _sessionMemberSpending = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val sessionMemberSpending: StateFlow<Map<String, Map<String, Double>>> = _sessionMemberSpending

    private var sessionJob: Job? = null
    private var transactionsJob: Job? = null
    private var activityLogJob: Job? = null
    private val sessionBalanceJobs = mutableMapOf<String, Job>()

    init {
        loadAllSessions()
        switchSession(initialSessionId)
    }

    fun loadAllSessions() {
        viewModelScope.launch {
            val user = try { firestoreRepository.getUser(currentUserId) } catch (e: Exception) { null }
            val defaultSessionId = user?.defaultSessionId
            
            try {
                firestoreRepository.getUserSessions(currentUserId).collect { sessions ->
                    _allSessions.value = sessions
                    calculateAllSessionBalances(sessions)
                    
                    val allUids = sessions.flatMap { it.memberUids }.distinct()
                    if (allUids.isNotEmpty()) {
                        try {
                            val users = firestoreRepository.getUsers(allUids)
                            _allKnownUsers.value = users.associateBy { it.uid }
                        } catch (e: Exception) {}
                    }
                    
                    if (_currentSessionId.value.isBlank() && sessions.isNotEmpty()) {
                        if (defaultSessionId != null && sessions.any { it.id == defaultSessionId }) {
                            switchSession(defaultSessionId)
                        } else {
                            switchSession(sessions.first().id)
                        }
                    }
                    _isSessionsLoaded.value = true
                }
            } catch (e: Exception) {
                // Ignore flow cancellation or permission denied on logout
            }
        }
    }

    fun switchSession(sessionId: String) {
        if (sessionId.isBlank()) return
        _currentSessionId.value = sessionId
        sessionJob?.cancel()
        transactionsJob?.cancel()
        activityLogJob?.cancel()

        sessionJob = viewModelScope.launch {
            try {
                firestoreRepository.getSession(sessionId).collect { s ->
                    _session.value = s
                    if (s != null) {
                        val members = try { firestoreRepository.getUsers(s.memberUids) } catch (e: Exception) { emptyList() }
                        _sessionMembers.value = members.associateBy { it.uid }
                        calculateBalances()
                    }
                }
            } catch (e: Exception) {
                // Ignore flow cancellation or permission denied on logout
            }
        }
        
        transactionsJob = viewModelScope.launch {
            try {
                firestoreRepository.getSessionTransactions(sessionId).collect { txList ->
                    _transactions.value = txList
                    // Calculate total spent (just the sum of all transactions for simplicity, or sum of current user's splits)
                    // For a personal tracker, total spent is sum of amount.
                    // For a group tracker, we might want to show total group spending, or just user's spending. We'll show total group spending.
                    _totalSpent.value = txList.sumOf { it.amount }
                    calculateBalances()
                }
            } catch (e: Exception) {
                // Ignore flow cancellation or permission denied on logout
            }
        }
        
        activityLogJob = viewModelScope.launch {
            try {
                firestoreRepository.getSessionActivityLogs(sessionId).collect { logs ->
                    _activityLogs.value = logs
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    fun setAsDefaultSession(sessionId: String) {
        viewModelScope.launch {
            firestoreRepository.setDefaultSession(currentUserId, sessionId)
            val currentMembers = _sessionMembers.value.toMutableMap()
            val me = currentMembers[currentUserId]
            if (me != null) {
                currentMembers[currentUserId] = me.copy(defaultSessionId = sessionId)
                _sessionMembers.value = currentMembers
            }
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
        
        _allDebts.value = allDebts
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
                _sessionUserExpenses.value = _sessionUserExpenses.value - id
                _sessionDebts.value = _sessionDebts.value - id
                _sessionMemberSpending.value = _sessionMemberSpending.value - id
                _allUserTransactionsMap.value = _allUserTransactionsMap.value - id
            }
        }
        
        // Start jobs for new sessions
        sessions.forEach { session ->
            if (session.id !in sessionBalanceJobs) {
                sessionBalanceJobs[session.id] = viewModelScope.launch {
                    try {
                        firestoreRepository.getSessionTransactions(session.id).collect { txList ->
                            val balances = mutableMapOf<String, Double>()
                            val spending = mutableMapOf<String, Double>()
                            session.memberUids.forEach { 
                                balances[it] = 0.0 
                                spending[it] = 0.0
                            }
                            
                            var sessionUserExpense = 0.0

                            txList.forEach { tx ->
                                val paidBy = tx.paidByUid.ifEmpty { tx.addedByUid }
                                balances[paidBy] = (balances[paidBy] ?: 0.0) + tx.amount
                                spending[paidBy] = (spending[paidBy] ?: 0.0) + tx.amount

                                if (tx.splits.isNotEmpty()) {
                                    tx.splits.forEach { (uid, amountOwed) ->
                                        balances[uid] = (balances[uid] ?: 0.0) - amountOwed
                                    }
                                    sessionUserExpense += tx.splits[currentUserId] ?: 0.0
                                } else {
                                    balances[paidBy] = (balances[paidBy] ?: 0.0) - tx.amount
                                    if (paidBy == currentUserId) {
                                        sessionUserExpense += tx.amount
                                    }
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
                            
                            val userTxs = txList.filter { tx -> 
                                (tx.paidByUid.ifEmpty { tx.addedByUid }) == currentUserId || tx.splits.containsKey(currentUserId) 
                            }
                            
                            _sessionBalances.value = _sessionBalances.value + (session.id to Pair(willGet, willPay))
                            _sessionUserExpenses.value = _sessionUserExpenses.value + (session.id to sessionUserExpense)
                            _sessionDebts.value = _sessionDebts.value + (session.id to allDebts)
                            _sessionMemberSpending.value = _sessionMemberSpending.value + (session.id to spending)
                            _allUserTransactionsMap.value = _allUserTransactionsMap.value + (session.id to userTxs)
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    fun addTransaction(amount: Double, description: String, paidByUid: String, splits: Map<String, Double>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentSession = _currentSessionId.value
            val me = _sessionMembers.value[currentUserId]
            val tx = Transaction(
                sessionId = currentSession,
                amount = amount,
                description = description,
                addedByUid = currentUserId,
                paidByUid = paidByUid,
                splits = splits
            )
            firestoreRepository.addTransaction(tx)
            
            // Log Activity
            val log = com.daemon.expnesso.data.model.ActivityLog(
                sessionId = currentSession,
                uid = currentUserId,
                userName = me?.name ?: "Unknown",
                action = "Added expense",
                details = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(amount)} for $description"
            )
            firestoreRepository.addActivityLog(log)
            
            onSuccess()
        }
    }

    fun deleteTransaction(transactionId: String, adminUid: String, addedByUid: String) {
        if (currentUserId == adminUid || currentUserId == addedByUid) {
            viewModelScope.launch {
                val tx = _transactions.value.find { it.id == transactionId }
                firestoreRepository.deleteTransaction(transactionId)
                if (tx != null) {
                    val me = _sessionMembers.value[currentUserId]
                    val log = com.daemon.expnesso.data.model.ActivityLog(
                        sessionId = _currentSessionId.value,
                        uid = currentUserId,
                        userName = me?.name ?: "Unknown",
                        action = "Deleted expense",
                        details = "₹${com.daemon.expnesso.utils.FormatUtils.formatAmount(tx.amount)} for ${tx.description}"
                    )
                    firestoreRepository.addActivityLog(log)
                }
            }
        }
    }



    fun createSession(name: String, onSuccess: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newSessionId = firestoreRepository.createSession(name, currentUserId)
            val user = try { firestoreRepository.getUser(currentUserId) } catch (e: Exception) { null }
            if (user?.defaultSessionId.isNullOrEmpty()) {
                setAsDefaultSession(newSessionId)
            }
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
                    val user = try { firestoreRepository.getUser(currentUserId) } catch (e: Exception) { null }
                    if (user?.defaultSessionId.isNullOrEmpty()) {
                        setAsDefaultSession(joinedSessionId)
                    }
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

    fun addManualMember(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.isBlank()) {
            onError("Name cannot be empty")
            return
        }
        val currentSession = _currentSessionId.value
        if (currentSession.isBlank()) return
        
        viewModelScope.launch {
            try {
                firestoreRepository.addManualUser(name, currentSession)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add manual member")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun deleteSession(sessionId: String, adminUid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (currentUserId != adminUid) {
            onError("You are not the admin of this book.")
            return
        }
        viewModelScope.launch {
            try {
                firestoreRepository.softDeleteSession(sessionId)
                
                val remaining = allSessions.value.filter { it.id != sessionId }
                val newDefaultId = remaining.firstOrNull()?.id ?: ""
                
                // If the deleted session was the current one, switch to the first available session or clear
                if (currentSessionId.value == sessionId) {
                    if (newDefaultId.isNotEmpty()) {
                        switchSession(newDefaultId)
                    } else {
                        _session.value = null
                        _transactions.value = emptyList()
                        _sessionMembers.value = emptyMap()
                        _netBalances.value = emptyMap()
                        _myCredits.value = emptyList()
                        _myDebts.value = emptyList()
                    }
                }
                
                // Update default session if it was the deleted one
                val user = firestoreRepository.getUser(currentUserId)
                if (user?.defaultSessionId == sessionId) {
                    firestoreRepository.setDefaultSession(currentUserId, newDefaultId)
                    val me = _sessionMembers.value[currentUserId]
                    if (me != null && me.defaultSessionId == sessionId) {
                        _sessionMembers.value = _sessionMembers.value.toMutableMap().apply {
                            put(currentUserId, me.copy(defaultSessionId = newDefaultId))
                        }
                    }
                }
                
                _allSessions.value = remaining
                
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete book")
            }
        }
    }

    fun renameSession(sessionId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (newName.isBlank()) {
            onError("Name cannot be empty")
            return
        }
        viewModelScope.launch {
            try {
                firestoreRepository.renameSession(sessionId, newName)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to rename book")
            }
        }
    }
}
