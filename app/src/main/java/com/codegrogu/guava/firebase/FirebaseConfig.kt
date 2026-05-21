package com.codegrogu.guava.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseConfig {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .build() // Default size is 100MB

            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()

            firestoreSettings = settings
        }
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
}
