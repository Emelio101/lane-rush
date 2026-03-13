package com.lanerush.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.lanerush.domain.model.User
import com.lanerush.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override fun getCurrentUser(): User? {
        return auth.currentUser?.toUser()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        result.user?.toUser() ?: throw Exception("Sign-in failed")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.toUser() ?: throw Exception("Login failed")
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign up failed")
        
        val profileUpdates = userProfileChangeRequest {
            this.displayName = displayName
        }
        user.updateProfile(profileUpdates).await()
        
        user.toUser().copy(displayName = displayName)
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun authStateFlow(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser?.toUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun com.google.firebase.auth.FirebaseUser.toUser(): User {
        return User(
            uid = uid,
            displayName = displayName ?: "",
            photoUrl = photoUrl?.toString() ?: ""
        )
    }
}
