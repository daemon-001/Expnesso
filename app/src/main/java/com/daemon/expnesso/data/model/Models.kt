package com.daemon.expnesso.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val defaultSessionId: String? = null
)

data class Session(
    val id: String = "",
    val name: String = "",
    val adminUid: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)

data class Transaction(
    val id: String = "",
    val sessionId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val addedByUid: String = "",
    val paidByUid: String = "",
    val splits: Map<String, Double> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now()
)
