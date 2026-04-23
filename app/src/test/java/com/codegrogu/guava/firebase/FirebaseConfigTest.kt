package com.codegrogu.guava.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock

class FirebaseConfigTest {

    @Test
    fun testAuthInitialization() {
        mockStatic(FirebaseAuth::class.java).use { staticMock ->
            val mockAuth = mock<FirebaseAuth>()
            staticMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)
            
            assertNotNull(FirebaseConfig.auth)
        }
    }

    @Test
    fun testFirestoreInitialization() {
        mockStatic(FirebaseFirestore::class.java).use { staticMock ->
            val mockFirestore = mock<FirebaseFirestore>()
            staticMock.`when`<FirebaseFirestore> { FirebaseFirestore.getInstance() }.thenReturn(mockFirestore)
            
            assertNotNull(FirebaseConfig.firestore)
        }
    }

    @Test
    fun testStorageInitialization() {
        mockStatic(FirebaseStorage::class.java).use { staticMock ->
            val mockStorage = mock<FirebaseStorage>()
            staticMock.`when`<FirebaseStorage> { FirebaseStorage.getInstance() }.thenReturn(mockStorage)
            
            assertNotNull(FirebaseConfig.storage)
        }
    }
}
