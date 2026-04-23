package com.codegrogu.guava.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseInitializationTest {

    @Test
    fun testFirebaseAppInitialized() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(appContext)
        // This will check if Firebase was initialized (requires google-services.json)
        assertNotNull(FirebaseApp.getInstance())
    }

    @Test
    fun testFirestoreSettingsApplied() {
        val firestore = FirebaseConfig.firestore
        assertNotNull(firestore.firestoreSettings)
        // Note: Specific settings check might be limited by the SDK visibility, 
        // but ensuring it's not null after our custom initialization is a good start.
    }
}
