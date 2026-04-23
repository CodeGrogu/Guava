# Authentication and Role-Based Access Control (RBAC)

This document describes the authentication logic and how user roles are managed within the Guava project.

## 1. Authentication Service (`AuthService`)

The `AuthService` handles all interactions with Firebase Authentication and synchronizes the session with the global `AppViewModel`.

### Key Functions:
- **`loginUser(email, password)`**: Performs an asynchronous login. If successful, it automatically fetches the user's role from Firestore and updates the global `AppState`.
- **`logoutUser()`**: Signs the user out of Firebase and resets the global state.
- **`listenToAuthState()`**: A listener that monitors the user's session. It ensures that the `AppViewModel` is cleared if the user's token expires or they are signed out from another device.

## 2. Role-Based Access Control (RBAC)

User roles are stored in Firestore under the `users` collection. Each document is identified by the user's Firebase UID.

### Firestore Schema:
- **Collection**: `users`
- **Document ID**: `{firebase_uid}`
- **Fields**:
    - `role`: String (one of `"MECHANIC"`, `"MANAGER"`)

### Role Resolution Logic:
1. User logs in via `AuthService`.
2. `AuthService` fetches the document from Firestore.
3. The role string is converted to the `UserRole` enum.
4. The `AppViewModel` is updated with both the `User` object and the resolved `UserRole`.

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
