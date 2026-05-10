# Authentication and Role-Based Access Control (RBAC)

This document describes the authentication logic and how user roles are managed within the Guava project.

## 1. Authentication Service (`AuthService`)

The `AuthService` handles all interactions with Firebase Authentication and synchronizes the session with the global `AppViewModel`.

### Key Functions:
- **`loginUser(email, password)`**: Performs an asynchronous login. If successful, it automatically fetches the user's role from Firestore and updates the global `AppState`.
- **`registerUser(email, password, name, role)`**: Creates a new Firebase Auth account and stores the associated user role in Firestore.
- **`logoutUser()`**: Signs the user out of Firebase and resets the global state.
- **`listenToAuthState()`**: A listener that monitors the user's session.

## 2. Test Users

The following test users are available for evaluation with the password **`123QWE!@#qwe`**:

| Role | Email | Name |
| :--- | :--- | :--- |
| **Manager** | `manager@valentine.com` | Valentine Manager |
| **Mechanic** | `testmechanic@valetine.com` | Test Mechanic |

> [!NOTE]
> These accounts are available in Firebase for role-based evaluation.

## 3. Role-Based Access Control (RBAC) & Firestore Schema

User data is stored in Firestore to manage roles and display full profile information after authentication.

### Firestore Schema:
- **Collection**: `users`
- **Document ID**: `{firebase_uid}`
- **Fields**:
    - `name`: String (User's full name)
    - `email`: String (User's email address)
    - `role`: String (one of `"MECHANIC"`, `"MANAGER"`)

### Troubleshooting reCAPTCHA Errors

If you encounter an **"Internal Error: CONFIGURATION_NOT_FOUND"** or persistent reCAPTCHA prompts during login/sign-up, follow these steps to disable enforcement:

1.  **Google Cloud Console**: Go to the [Identity Platform Settings](https://console.cloud.google.com/customer-identity/settings/security).
2.  **Security Tab**: Locate the **reCAPTCHA bot protection** section.
3.  **Disable Enforcement**: Change the Enforcement Mode to **OFF**.
4.  **Authorized Domains**: Ensure your local testing environment (usually `localhost` or specific internal IPs for emulators) is not being aggressively blocked.

> [!IMPORTANT]
> Disabling reCAPTCHA is recommended for development to avoid complex attestation errors on emulators.

## 3. Usage in UI

In Compose, you can access the current user and their role by collecting the state from `AppViewModel`:

```kotlin
val uiState by appViewModel.uiState.collectAsState()

if (uiState.currentUser != null) {
    when (uiState.userRole) {
        UserRole.MANAGER -> ManagerDashboard()
        UserRole.MECHANIC -> MechanicDashboard()
        else -> LoadingOrError()
    }
} else {
    LoginScreen(onLogin = { email, pass -> authService.loginUser(email, pass) })
}
```

## 4. Security

- **Persistence**: Auth state is persisted by Firebase by default.
- **Firestore Rules**: Ensure that Firestore rules allow users to read their own document in the `users` collection:
  ```
  match /users/{userId} {
    allow read: if request.auth != null && request.auth.uid == userId;
  }
  ```
