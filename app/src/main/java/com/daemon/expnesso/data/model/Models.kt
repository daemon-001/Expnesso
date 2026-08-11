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
    val createdAt: Timestamp = Timestamp.now(),
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    @set:com.google.firebase.firestore.PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null
)

data class Transaction(
    val id: String = "",
    val sessionId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val addedByUid: String = "",
    val paidByUid: String = "",
    val splits: Map<String, Double> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now(),
    @get:com.google.firebase.firestore.Exclude 
    @set:com.google.firebase.firestore.Exclude 
    var isSynced: Boolean = true
)

data class ActivityLog(
    val id: String = "",
    val sessionId: String = "",
    val uid: String = "",
    val userName: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
