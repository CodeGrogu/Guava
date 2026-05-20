package com.codegrogu.guava.service

import com.codegrogu.guava.model.UserRole
import com.codegrogu.guava.viewmodel.AppViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AuthServiceTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var appViewModel: AppViewModel
    private lateinit var authService: AuthService

    @Before
    fun setup() {
        auth = mock(FirebaseAuth::class.java)
        firestore = mock(FirebaseFirestore::class.java)
        appViewModel = mock(AppViewModel::class.java)
        authService = AuthService(auth, firestore, appViewModel)
    }

    @Test
    fun `logoutUser calls auth signOut and clears appViewModel`() {
        authService.logoutUser()
        
        verify(auth).signOut()
        verify(appViewModel).clearState()
    }

    @Test
    fun `getUserData returns correct user and role from firestore`() = runTest {
        val userId = "testId"
        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(DocumentReference::class.java)
        val mockTask = mock(Task::class.java) as Task<DocumentSnapshot>
        val mockSnapshot = mock(DocumentSnapshot::class.java)

        whenever(firestore.collection("users")).thenReturn(mockCollection)
        whenever(mockCollection.document(userId)).thenReturn(mockDocument)
        whenever(mockDocument.get()).thenReturn(mockTask)
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.isSuccessful).thenReturn(true)
        whenever(mockTask.result).thenReturn(mockSnapshot)
        whenever(mockSnapshot.exists()).thenReturn(true)
        whenever(mockSnapshot.id).thenReturn(userId)
        whenever(mockSnapshot.getString("name")).thenReturn("Test Name")
        whenever(mockSnapshot.getString("email")).thenReturn("test@test.com")
        whenever(mockSnapshot.getString("role")).thenReturn("MECHANIC")

        val (user, role) = authService.getUserData(userId)
        
        assertEquals("Test Name", user?.name)
        assertEquals("test@test.com", user?.email)
        assertEquals(UserRole.MECHANIC, role)
    }

    @Test
    fun `loginUser success updates appViewModel with full data`() = runTest {
        val email = "test@example.com"
        val password = "password"
        val userId = "uid123"
        
        val mockAuthTask = mock(Task::class.java) as Task<AuthResult>
        val mockAuthResult = mock(AuthResult::class.java)
        val mockFirebaseUser = mock(FirebaseUser::class.java)
        
        whenever(auth.signInWithEmailAndPassword(email, password)).thenReturn(mockAuthTask)
        whenever(mockAuthTask.isComplete).thenReturn(true)
        whenever(mockAuthTask.isSuccessful).thenReturn(true)
        whenever(mockAuthTask.result).thenReturn(mockAuthResult)
        whenever(mockAuthResult.user).thenReturn(mockFirebaseUser)
        whenever(mockFirebaseUser.uid).thenReturn(userId)
        
        // Mock full data fetching
        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(DocumentReference::class.java)
        val mockDocTask = mock(Task::class.java) as Task<DocumentSnapshot>
        val mockSnapshot = mock(DocumentSnapshot::class.java)
        
        whenever(firestore.collection("users")).thenReturn(mockCollection)
        whenever(mockCollection.document(userId)).thenReturn(mockDocument)
        whenever(mockDocument.get()).thenReturn(mockDocTask)
        whenever(mockDocTask.isComplete).thenReturn(true)
        whenever(mockDocTask.isSuccessful).thenReturn(true)
        whenever(mockDocTask.result).thenReturn(mockSnapshot)
        whenever(mockSnapshot.exists()).thenReturn(true)
        whenever(mockSnapshot.id).thenReturn(userId)
        whenever(mockSnapshot.getString("name")).thenReturn("Manager User")
        whenever(mockSnapshot.getString("email")).thenReturn(email)
        whenever(mockSnapshot.getString("role")).thenReturn("MANAGER")

        val result = authService.loginUser(email, password)
        
        assertTrue(result.isSuccess)
        verify(appViewModel).updateUser(any(), eq(UserRole.MANAGER))
    }

    @Test
    fun `loginUser falls back to email lookup when uid document is missing`() = runTest {
        val email = "manager@valentine.com"
        val password = "password"
        val userId = "authUid123"
        val firestoreDocumentId = "legacyDocId"

        val mockAuthTask = mock(Task::class.java) as Task<AuthResult>
        val mockAuthResult = mock(AuthResult::class.java)
        val mockFirebaseUser = mock(FirebaseUser::class.java)

        whenever(auth.signInWithEmailAndPassword(email, password)).thenReturn(mockAuthTask)
        whenever(mockAuthTask.isComplete).thenReturn(true)
        whenever(mockAuthTask.isSuccessful).thenReturn(true)
        whenever(mockAuthTask.result).thenReturn(mockAuthResult)
        whenever(mockAuthResult.user).thenReturn(mockFirebaseUser)
        whenever(mockFirebaseUser.uid).thenReturn(userId)
        whenever(mockFirebaseUser.email).thenReturn(email)

        val mockCollection = mock(CollectionReference::class.java)
        val mockUidDocument = mock(DocumentReference::class.java)
        val mockUidTask = mock(Task::class.java) as Task<DocumentSnapshot>
        val mockMissingSnapshot = mock(DocumentSnapshot::class.java)
        val mockEmailQuery = mock(Query::class.java)
        val mockLimitedQuery = mock(Query::class.java)
        val mockQueryTask = mock(Task::class.java) as Task<QuerySnapshot>
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)
        val mockEmailSnapshot = mock(DocumentSnapshot::class.java)

        whenever(firestore.collection("users")).thenReturn(mockCollection)
        whenever(mockCollection.document(userId)).thenReturn(mockUidDocument)
        whenever(mockUidDocument.get()).thenReturn(mockUidTask)
        whenever(mockUidTask.isComplete).thenReturn(true)
        whenever(mockUidTask.isSuccessful).thenReturn(true)
        whenever(mockUidTask.result).thenReturn(mockMissingSnapshot)
        whenever(mockMissingSnapshot.exists()).thenReturn(false)
        whenever(mockMissingSnapshot.id).thenReturn(userId)

        whenever(mockCollection.whereEqualTo("email", email)).thenReturn(mockEmailQuery)
        whenever(mockEmailQuery.limit(1)).thenReturn(mockLimitedQuery)
        whenever(mockLimitedQuery.get()).thenReturn(mockQueryTask)
        whenever(mockQueryTask.isComplete).thenReturn(true)
        whenever(mockQueryTask.isSuccessful).thenReturn(true)
        whenever(mockQueryTask.result).thenReturn(mockQuerySnapshot)
        whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockEmailSnapshot))
        whenever(mockEmailSnapshot.exists()).thenReturn(true)
        whenever(mockEmailSnapshot.id).thenReturn(firestoreDocumentId)
        whenever(mockEmailSnapshot.getString("name")).thenReturn("Test Manager")
        whenever(mockEmailSnapshot.getString("email")).thenReturn(email)
        whenever(mockEmailSnapshot.getString("role")).thenReturn("MANAGER")

        val result = authService.loginUser(email, password)

        assertTrue(result.isSuccess)
        verify(appViewModel).updateUser(any(), eq(UserRole.MANAGER))
    }

    @Test
    fun `registerUser success stores full profile and updates appViewModel`() = runTest {
        val email = "new@example.com"
        val password = "password"
        val name = "New User"
        val userId = "newUid"
        val role = UserRole.MECHANIC
        
        val mockAuthTask = mock(Task::class.java) as Task<AuthResult>
        val mockAuthResult = mock(AuthResult::class.java)
        val mockFirebaseUser = mock(FirebaseUser::class.java)
        
        whenever(auth.createUserWithEmailAndPassword(email, password)).thenReturn(mockAuthTask)
        whenever(mockAuthTask.isComplete).thenReturn(true)
        whenever(mockAuthTask.isSuccessful).thenReturn(true)
        whenever(mockAuthTask.result).thenReturn(mockAuthResult)
        whenever(mockAuthResult.user).thenReturn(mockFirebaseUser)
        whenever(mockFirebaseUser.uid).thenReturn(userId)
        
        val mockCollection = mock(CollectionReference::class.java)
        val mockDocument = mock(DocumentReference::class.java)
        val mockSetTask = mock(Task::class.java) as Task<Void>
        
        whenever(firestore.collection("users")).thenReturn(mockCollection)
        whenever(mockCollection.document(userId)).thenReturn(mockDocument)
        whenever(mockDocument.set(any())).thenReturn(mockSetTask)
        whenever(mockSetTask.isComplete).thenReturn(true)
        whenever(mockSetTask.isSuccessful).thenReturn(true)

        val result = authService.registerUser(email, password, name, role)
        
        assertTrue(result.isSuccess)
        // Verify that set() was called with a map containing all fields
        verify(mockDocument).set(mapOf(
            "name" to name,
            "email" to email,
            "role" to role.name
        ))
        verify(appViewModel).updateUser(any(), eq(role))
    }
}
