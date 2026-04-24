# Firebase Setup and Integration Guide

This document serves as a comprehensive guide for setting up and using Firebase within the Guava project, based on the implementation completed in April 2026.

## 1. Project Configuration

### Version Catalog (`libs.versions.toml`)
We use a centralized version catalog to manage Firebase dependencies. This ensures consistency across the project.

- **Firebase BoM (Bill of Materials)**: `34.12.0`. The BoM allows you to manage all Firebase library versions by specifying only the BoM version.
- **Google Services Plugin**: `4.4.4`. This plugin processes the `google-services.json` file.
- **Testing Libraries**: Includes `mockito-inline` and `mockito-kotlin` for unit testing Firebase singletons.

```toml
[versions]
firebaseBom = "34.12.0"
googleServices = "4.4.4"
mockito = "5.2.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }
firebase-storage = { group = "com.google.firebase", name = "firebase-storage" }
mockito-inline = { group = "org.mockito", name = "mockito-inline", version.ref = "mockito" }
```

### Build Scripts
The Google Services plugin is applied at both the project and app levels.

- **Root `build.gradle.kts`**: Declare the plugin with `apply false`.
- **App `build.gradle.kts`**: Apply the plugin and include the dependencies using the BoM.

```kotlin
// App build.gradle.kts
dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
}
```

## 2. Singleton Configuration (`FirebaseConfig`)

We use a Kotlin `object` for Firebase services to ensure a single point of access and consistent configuration.

### Key Features:
- **Lazy Initialization**: Instances are only created when first accessed via `by lazy`.
- **Explicit Firestore Persistence**: Configured using `PersistentCacheSettings` to ensure reliable offline behavior.
- **Initialization Order**: Access `FirebaseConfig.firestore` early in the app lifecycle to ensure settings are applied before any implicit calls to `FirebaseFirestore.getInstance()`.

```kotlin
object FirebaseConfig {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            val cacheSettings = PersistentCacheSettings.newBuilder().build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            firestoreSettings = settings
        }
    }
}
```

## 3. Configuration & Verification

### Mandatory File: `google-services.json`
**Location**: `app/google-services.json`  
This file contains the project identifiers. The app will not build or connect without it.

### Testing and Verification
The setup has been verified through a two-tiered testing suite:
1. **Unit Tests** ([FirebaseConfigTest.kt](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/app/src/test/java/com/codegrogu/guava/firebase/FirebaseConfigTest.kt)): Verifies lazy initialization logic and service retrieval using static mocking.
2. **Instrumental Tests** ([FirebaseInitializationTest.kt](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/app/src/androidTest/java/com/codegrogu/guava/firebase/FirebaseInitializationTest.kt)): Verifies the real Firebase initialization on a device/emulator.

For detailed testing instructions, see the [Testing Guide](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/documentation/TESTCASES.md).

## 4. Troubleshooting

- **"File google-services.json is missing"**: Place the file in the root of the `app/` directory.
- **"Internal Error ... CONFIGURATION_NOT_FOUND"**: This is caused by missing SHA fingerprints in the Firebase Console. Add the SHA-1 and SHA-256 fingerprints generated via `./gradlew signingReport` to your Project Settings.
- **"Could not find method mapPath()"**: This is an environment-specific Gradle scripting error. Workaround: Run tests via CLI using `./gradlew` with an explicit `JAVA_HOME`.
- **Persistence Error**: Ensure `firestoreSettings` are applied before any other Firestore operations.
- **Unresolved Firebase classes**: Perform a **Gradle Sync** to refresh the project dependencies.

## 5. Security & Fingerprints

For local development, the following fingerprints must be registered in the Firebase Console for Authentication and reCAPTCHA to function correctly:

**SHA-1**: `30:3F:01:D8:15:4B:13:75:6A:BF:31:FD:65:E4:AE:14:FD:66:AD:1B`  
**SHA-256**: `89:B9:61:F8:F8:05:D0:26:2F:88:5D:26:40:A8:50:9B:D5:B6:04:4E:EF:1F:06:6D:86:06:28:52:FC:E4:CC:11`

## 5. Future Architecture
While a static `object` is used for simplicity, migrating to **Hilt (Dependency Injection)** is recommended as the project scales to improve testability and modularity.
