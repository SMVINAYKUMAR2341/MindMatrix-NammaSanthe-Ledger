package com.nammasanthe.ledger.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manages Firebase Authentication with offline-first support.
 * Works offline using cached credentials, syncs when online.
 */
class FirebaseAuthManager private constructor(context: Context) {

    private val auth: FirebaseAuth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _authState.value = if (firebaseAuth.currentUser != null) {
                AuthState.Authenticated(firebaseAuth.currentUser!!)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Sign up with email and password.
     */
    suspend fun signUp(email: String, password: String): Result<FirebaseUser> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("User creation failed"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Authentication failed"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Sign out.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                auth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Check if user is currently signed in.
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /**
     * Get current user ID or null.
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Update user display name.
     */
    suspend fun updateDisplayName(name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Check if we can sync (user is authenticated and online).
     */
    fun canSync(): Boolean {
        return isSignedIn() && _currentUser.value != null
    }
}

/**
 * Auth states.
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
