# UI Theme and Error Handling Guide

This document outlines the professional design standards and centralized error handling system implemented in the Guava project.

## 1. Global UI Theme ("Garage" Palette)

The application uses a custom Material 3 theme tailored for a garage/automotive context.

### Color Palette
- **Primary**: `GuavaGreen` (#2E7D32) - Represents reliability and growth.
- **Secondary**: `GearGray` (#455A64) - Represents machinery and precision.
- **Background**: `OilCharcoal` (#263238) - Used in dark mode for a professional, high-contrast look.
- **Semantic Colors**:
    - **Error**: `ErrorRed` (#D32F2F)
    - **Success**: `SuccessGreen` (#43A047)

### Implementation
- **Tokens**: [Color.kt](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/app/src/main/java/com/codegrogu/guava/ui/theme/Color.kt)
- **Theme Logic**: [Theme.kt](file:///C:/Users/Jaden/Documents/Programming/University/MAP/Guava/app/src/main/java/com/codegrogu/guava/ui/theme/Theme.kt)

---

## 2. Centralized Error Handling

We use a **one-shot event flow** to handle global notifications (Snackbars) reliably, ensuring they are not re-triggered by configuration changes.

### Architecture: `UiEvent`
Transient UI events are handled via a `Channel` in `AppViewModel`.

1. **Emit**: Call `appViewModel.showSnackbar(message, isError)`.
2. **Collect**: `AppNavigation` collects these events in a `LaunchedEffect`.
3. **Display**: A custom `SnackbarHost` styles the notification based on whether it is an error or a success message.

```kotlin
// Triggering from a Composable/Service
if (result.isFailure) {
    appViewModel.showSnackbar("Invalid password", isError = true)
}
```

### Technical Details:
- **`SnackbarVisuals` Integration**: The `UiEvent.ShowSnackbar` class implements the Material 3 `SnackbarVisuals` interface directly, allowing it to be passed into `hostState.showSnackbar()`.
- **Consistency**: All auth-related errors are now displayed through this global system instead of local text fields.

---

## 3. Modern UI Practices

- **Surface Usage**: All screens are wrapped in a `Surface` to ensure correct background and content color transitions.
- **Material 3 Components**:
    - **Segmented Buttons**: Used for role selection in `SignUpScreen`.
    - **Outlined Text Fields**: Optimized for accessibility with `KeyboardOptions`.
    - **Elevated Surfaces**: Used for dashboard elements to provide depth.
- **Loading States**: Centralized `isLoading` state in `AppViewModel` triggers professional circular progress indicators inside buttons.
