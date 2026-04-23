# Firebase Testing Guide

This document explains how to set up, use, and maintain the test cases for the Firebase integration in the Guava project.

## 1. Test Architecture

We use a two-tiered testing approach to ensure the Firebase setup is robust.

### Unit Tests (`app/src/test`)
- **File**: `FirebaseConfigTest.kt`
- **Purpose**: Verifies the `FirebaseConfig` singleton logic, lazy initialization, and basic SDK interaction.
- **Tools**: 
    - **Mockito-Inline**: Required to mock static methods like `FirebaseAuth.getInstance()`.
    - **Mockito-Kotlin**: Provides idiomatic Kotlin wrappers for Mockito.

### Instrumental Tests (`app/src/androidTest`)
- **File**: `FirebaseInitializationTest.kt`
- **Purpose**: Verifies that the Firebase SDK is correctly initialized within the Android system context and that Firestore settings (like persistence) are applied.
- **Requirement**: A running emulator or device and a valid `google-services.json`.

---

## 2. How to Run Tests

### Within Android Studio (Recommended)
1. Navigate to the test file in the project explorer.
2. Right-click the file or a specific test method.
3. Select **Run 'filename'**.

### From Command Line (CLI)
If you encounter environment errors (like the `mapPath()` issue) when using IDE-integrated tools, use the Gradle wrapper directly.

**Command Template (PowerShell):**
```powershell
$env:JAVA_HOME="<Path_To_Android_Studio_JBR>"; ./gradlew :app:testDebugUnitTest --tests com.codegrogu.guava.firebase.FirebaseConfigTest
```
*Note: Replace `<Path_To_Android_Studio_JBR>` with your actual JBR path, e.g., `C:\Users\Jaden\AppData\Local\Programs\Android Studio\jbr`.*

---

## 3. Key Learnings & Troubleshooting

### Mocking Singletons
Firebase services are accessed via static `getInstance()` methods. To test these without initializing the entire Firebase app (which is heavy/impossible in unit tests):
- Use `mockStatic(ClassName::class.java).use { ... }`.
- This creates a temporary mock that is automatically closed after the block.

### Handling the `mapPath()` Error
During development, we encountered a `Could not find method mapPath()` error when running tests through certain IDE automation tools. 
- **Learning**: This is an environment/scripting issue with the build tool and **not** a bug in the code.
- **Workaround**: Run tests directly via `./gradlew` in the terminal to bypass problematic initialization scripts.

### 2026 Standards
- **Mocking Final Classes**: Modern Mockito (v5+) handles final classes (which many Firebase classes are) by default using the `mockito-inline` mechanism, which is now the standard for Kotlin Android development.
- **Static Access**: Always verify that your singleton handles lazy initialization correctly to avoid unnecessary resource usage before a user is even logged in.

## 4. Maintenance
When adding new Firebase services (e.g., Cloud Functions or Remote Config):
1. Add the service to `FirebaseConfig.kt`.
2. Add a corresponding test case in `FirebaseConfigTest.kt` using the `mockStatic` pattern.
3. Verify the initialization in `FirebaseInitializationTest.kt`.
