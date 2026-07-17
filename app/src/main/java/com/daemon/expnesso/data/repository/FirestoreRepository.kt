package com.daemon.expnesso.data.repository

import com.daemon.expnesso.data.model.Session
import com.daemon.expnesso.data.model.Transaction
import com.daemon.expnesso.data.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveUser(firebaseUser: FirebaseUser) {
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        )
        db.collection("users").document(user.uid).set(user).await()
    }

    suspend fun createSession(name: String, adminUid: String): String {
        val sessionId = UUID.randomUUID().toString()
        val inviteCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
        val session = Session(
            id = sessionId,
            name = name,
            adminUid = adminUid,
            inviteCode = inviteCode,
            memberUids = listOf(adminUid)
        )
        db.collection("sessions").document(sessionId).set(session).await()
        return sessionId
    }

    suspend fun joinSession(inviteCode: String, uid: String): String? {
        val snapshot = db.collection("sessions")
            .whereEqualTo("inviteCode", inviteCode)
            .get()
            .await()

        if (snapshot.documents.isNotEmpty()) {
            val doc = snapshot.documents.first()
            val session = doc.toObject(Session::class.java)
            if (session != null) {
                val updatedMembers = session.memberUids.toMutableSet().apply { add(uid) }.toList()
                db.collection("sessions").document(session.id)
                    .update("memberUids", updatedMembers)
                    .await()
                return session.id
            }
        }
        return null
    }

    fun getUserSessions(uid: String): Flow<List<Session>> = callbackFlow {
        val listener = db.collection("sessions")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { it.toObject(Session::class.java) }
                    trySend(sessions)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTransaction(transaction: Transaction) {
        val id = UUID.randomUUID().toString()
        val newTransaction = transaction.copy(id = id)
        db.collection("transactions").document(id).set(newTransaction).await()
    }

    suspend fun deleteTransaction(transactionId: String) {
        db.collection("transactions").document(transactionId).delete().await()
    }

    fun getSessionTransactions(sessionId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection("transactions")
            .whereEqualTo("sessionId", sessionId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                    trySend(transactions)
                }
            }
        awaitClose { listener.remove() }
    }
    fun getSession(sessionId: String): Flow<Session?> = callbackFlow {
        val listener = db.collection("sessions").document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(Session::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }
}
