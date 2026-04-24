# Application Navigation Guide

This document explains the navigation architecture of the Guava project, featuring type-safe routes and role-based redirection.

## 1. Navigation Setup

We use **Navigation Compose** with type-safe destinations. All routes are defined as `@Serializable` objects or classes in `Destinations.kt`.

### Destinations:
- `Destination.Login`: The entry point for unauthenticated users.
- `Destination.MechanicDashboard`: The main screen for Mechanics.
- `Destination.ManagerDashboard`: The main screen for Managers.

## 2. AppNavigation Composable

The `AppNavigation` Composable acts as the root of the UI. It manages the `NavHost` and reacts to changes in the global `AppState`.

### Conditional Routing Logic:
The `AppNavigation` uses a `LaunchedEffect` to observe the `currentUser` and `userRole` from `AppViewModel`.

1. **Unauthenticated**: If `currentUser` is null, the user is navigated to `Destination.Login`.
2. **Authenticated**: Once logged in, the `userRole` determines the destination:
    - `UserRole.MECHANIC` -> `Destination.MechanicDashboard`
    - `UserRole.MANAGER` -> `Destination.ManagerDashboard`
    - `UserRole.UNKNOWN` -> Stay on Login (handles edge cases where role is still fetching).

## 3. Usage

### Navigating to a screen:
```kotlin
navController.navigate(Destination.MechanicDashboard)
```

### Clearing the backstack:
When logging in or out, we clear the backstack to prevent the user from navigating back to the previous auth state:
```kotlin
navController.navigate(Destination.MechanicDashboard) {
    popUpTo(Destination.Login) { inclusive = true }
}
```

## 4. Testing

Navigation logic is tested using instrumental tests in `NavigationTest.kt`. These tests verify that providing a specific user state results in the correct screen being displayed.
