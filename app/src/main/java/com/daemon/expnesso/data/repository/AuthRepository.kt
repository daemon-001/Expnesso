package com.daemon.expnesso.data.repository

import android.content.Context
import com.daemon.expnesso.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    val firebaseAuth = FirebaseAuth.getInstance()
    
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    fun getGoogleSignInClient(): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        val clientId = context.getString(R.string.default_web_client_id)
        return GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build()
        )
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        getGoogleSignInClient().signOut().await()
    }
}
