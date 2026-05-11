package com.codegrogu.guava.service

import com.codegrogu.guava.firebase.FirebaseConfig
import com.codegrogu.guava.model.User
import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.viewmodel.AppViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
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
            
            val (user, role) = getUserData(firebaseUser)
            if (role == UserRole.UNKNOWN) {
                auth.signOut()
                appViewModel.clearState()
                throw Exception("User role not found. Contact an administrator to finish account setup.")
            }

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
        return getUserData(userId, null)
    }

    private suspend fun getUserData(firebaseUser: FirebaseUser): Pair<User?, UserRole> {
        return getUserData(firebaseUser.uid, firebaseUser.email)
    }

    private suspend fun getUserData(userId: String, email: String?): Pair<User?, UserRole> {
        val usersCollection = firestore.collection("users")
        val uidDocument = usersCollection.document(userId).get().await()

        val uidResult = uidDocument.toUserAndRole(userId, email.orEmpty())
        if (uidResult.second != UserRole.UNKNOWN) {
            return uidResult
        }

        if (!email.isNullOrBlank()) {
            val emailDocument = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val emailResult = emailDocument?.toUserAndRole(userId, email)
            if (emailResult != null && emailResult.second != UserRole.UNKNOWN) {
                return emailResult
            }
        }

        return Pair(User(userId, "User", email.orEmpty()), UserRole.UNKNOWN)
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
                    try {
                        val (storedUser, role) = getUserData(firebaseUser)
                        if (role == UserRole.UNKNOWN) {
                            auth.signOut()
                            appViewModel.clearState()
                            appViewModel.showSnackbar("User role not found. Contact an administrator to finish account setup.")
                            return@launch
                        }

                        val user = storedUser ?: User(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email ?: ""
                        )

                        appViewModel.updateUser(user, role)
                    } catch (e: Exception) {
                        appViewModel.clearState()
                        appViewModel.showSnackbar(e.message ?: "Unable to load user profile.")
                    }
                }
            } else {
                appViewModel.clearState()
            }
        }
    }

    private fun DocumentSnapshot.toUserAndRole(fallbackUserId: String, fallbackEmail: String): Pair<User?, UserRole> {
        if (!exists()) {
            return Pair(null, UserRole.UNKNOWN)
        }

        val roleString = getString("role")
        val role = when (roleString?.uppercase()) {
            "MECHANIC" -> UserRole.MECHANIC
            "MANAGER" -> UserRole.MANAGER
            else -> {
                UserRole.UNKNOWN
            }
        }

        val user = User(
            id = fallbackUserId,
            name = getString("name") ?: "User",
            email = getString("email") ?: fallbackEmail
        )

        return Pair(user, role)
    }
}
