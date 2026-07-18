package com.daemon.expnesso.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.repository.AuthRepository
import com.daemon.expnesso.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SessionViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadSessions()
    }

    private fun loadSessions() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestoreRepository.getUserSessions(uid).collect { userSessions ->
                    _sessions.value = userSessions
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun createSession(name: String, onSuccess: (String) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sessionId = firestoreRepository.createSession(name, uid)
                _isLoading.value = false
                onSuccess(sessionId)
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun joinSession(inviteCode: String, onSuccess: (String) -> Unit) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sessionId = firestoreRepository.joinSession(inviteCode, uid)
                _isLoading.value = false
                if (sessionId != null) {
                    onSuccess(sessionId)
                } else {
                    _error.value = "Invalid invite code"
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }
}
