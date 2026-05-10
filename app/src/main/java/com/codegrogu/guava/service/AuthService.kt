package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.viewmodel.AppViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

open class AuthService(
    private val auth: FirebaseAuth = FirebaseConfig.auth,
    private val firestore: FirebaseFirestore = FirebaseConfig.firestore,
    private val appViewModel: AppViewModel
) {

    open suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User not found after login")
            
            val (user, role) = getUserData(firebaseUser.uid)
            val finalUser = user ?: User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "User",
                email = firebaseUser.email ?: email
            )
            
            appViewModel.updateUser(finalUser, role)
            
            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun registerUser(email: String, password: String, name: String, role: UserRole): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            
            val user = User(
                id = firebaseUser.uid,
                name = name,
                email = email
            )
            
            // Store full user profile in Firestore
            firestore.collection("users").document(user.id).set(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "role" to role.name
                )
            ).await()
            
            appViewModel.updateUser(user, role)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open fun logoutUser() {
        auth.signOut()
        appViewModel.clearState()
    }

    open suspend fun getUserData(userId: String): Pair<User?, UserRole> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val name = document.getString("name") ?: "User"
            val email = document.getString("email") ?: ""
            val roleString = document.getString("role")
            
            val user = User(userId, name, email)
            val role = when (roleString?.uppercase()) {
                "MECHANIC" -> UserRole.MECHANIC
                "MANAGER" -> UserRole.MANAGER
                else -> UserRole.UNKNOWN
            }
            Pair(user, role)
        } catch (e: Exception) {
            Pair(null, UserRole.UNKNOWN)
        }
    }

    open fun listenToAuthState(scope: CoroutineScope) {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val state = appViewModel.uiState.value
                if (state.isLoading) return@addAuthStateListener
                if (state.currentUser?.id == firebaseUser.uid && state.userRole != UserRole.UNKNOWN) {
                    return@addAuthStateListener
                }

                appViewModel.setLoading(true)
                scope.launch {
                    val (storedUser, role) = getUserData(firebaseUser.uid)
                    val user = storedUser ?: User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: ""
                    )

                    appViewModel.updateUser(user, role)
                    if (role == UserRole.UNKNOWN) {
                        appViewModel.showSnackbar("User role not found")
                    }
                }
            } else {
                appViewModel.clearState()
            }
        }
    }
}
