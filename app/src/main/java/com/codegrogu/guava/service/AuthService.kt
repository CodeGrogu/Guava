package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.viewmodel.AppViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthService(
    private val auth: FirebaseAuth = FirebaseConfig.auth,
    private val firestore: FirebaseFirestore = FirebaseConfig.firestore,
    private val appViewModel: AppViewModel
) {

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User not found after login")
            
            val user = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "User",
                email = firebaseUser.email ?: email
            )
            
            val role = getUserRole(user.id)
            appViewModel.updateUser(user, role)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logoutUser() {
        auth.signOut()
        appViewModel.clearState()
    }

    suspend fun getUserRole(userId: String): UserRole {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val roleString = document.getString("role")
            when (roleString?.uppercase()) {
                "MECHANIC" -> UserRole.MECHANIC
                "MANAGER" -> UserRole.MANAGER
                else -> UserRole.UNKNOWN
            }
        } catch (e: Exception) {
            UserRole.UNKNOWN
        }
    }

    fun listenToAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // We have a user, but we might not have their role yet
                // In a real app, you might want to launch a coroutine here to fetch the role
                // For simplicity in this setup, we'll just check if the state is already set
                if (appViewModel.uiState.value.currentUser == null) {
                    // This listener is synchronous, but role fetching is async.
                    // We'll rely on the manual login to set the role, 
                    // and handle session restoration in the UI/ViewModel if needed.
                }
            } else {
                appViewModel.clearState()
            }
        }
    }
}
