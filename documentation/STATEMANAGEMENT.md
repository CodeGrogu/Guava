# Global State Management Guide

This document explains how global application state is managed in the Guava project using Jetpack Compose and ViewModels.

## 1. Architecture Overview

We use **Unidirectional Data Flow (UDF)** with a centralized `AppViewModel` to manage state that needs to be accessed across multiple screens, such as:
- User Authentication Status
- User Roles (Mechanic vs. Manager)
- Global Loading States

### State Holder: `AppState`
The entire global state is represented by a single immutable data class:

```kotlin
data class AppState(
    val currentUser: User? = null,
    val userRole: UserRole = UserRole.UNKNOWN,
    val isLoading: Boolean = false
)
```

## 2. Implementation Details

### `AppViewModel`
The `AppViewModel` exposes the state as a `StateFlow`, which ensures that the UI always has access to the latest state and handles configuration changes automatically.

- **Exposed State**: `uiState: StateFlow<AppState>`
- **Mutations**: Controlled via explicit functions like `updateUser` and `clearState`.

### Sharing the ViewModel
The `AppViewModel` is initialized in `MainActivity` using the `viewModels()` delegate. This ensures it lives as long as the Activity.

```kotlin
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    // ...
}
```

In Compose, the state is collected using `collectAsState()` (or `collectAsStateWithLifecycle()` for better lifecycle awareness):

```kotlin
val uiState by appViewModel.uiState.collectAsState()
```

## 3. Best Practices

1.  **Avoid Prop Drilling**: Instead of passing the user object through every Composable, pass the `AppViewModel` or use a `CompositionLocal` if deep nesting is required.
2.  **Immutable State**: Never modify `AppState` directly. Always use the `.copy()` method within the ViewModel's `update` block.
3.  **Loading States**: Use `setLoading(true/false)` to trigger global progress indicators during async operations.
4.  **Role-Based UI**: Use `uiState.userRole` to conditionally show or hide features (e.g., Manager-only reports).

## 4. Testing

Unit tests for `AppViewModel` ensure that state transitions occur correctly and that the initial state is valid. See `AppViewModelTest.kt` for implementation details.
