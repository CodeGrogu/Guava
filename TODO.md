# Valentine's Garage Android Application - Implementation To-Do List

This document outlines the detailed implementation plan, structured chronologically by priority. The project uses pure Kotlin with Jetpack Compose, Firebase (Auth, Firestore for concurrency), and Android Navigation Component. Tasks are distributed evenly among exactly 4 team members.

---

## Phase 1: Project Setup & Authentication (Priority 1)
**Assignee: Member 1**

### 1. Initialize Firebase Configuration
- **File Name**: `FirebaseConfig.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/firebase/`
- **Description**: Setup Firebase connection for Auth, Firestore (real-time DB for concurrency), and Storage.
- **Depends On**: `build.gradle` (Firebase SDK dependencies)
- **Where it will be called**: All service classes, MainActivity
- **Tasks**:
    - [x] Create the Firebase project in the Firebase Console (Add an Android App).
    - [x] Add Firebase dependencies to `build.gradle.kts` (Firebase Auth, Firestore, Storage).
    - [x] **Download `google-services.json` from Firebase Console and add to `android/app/` directory. Update `build.gradle` files.**
    - [x] Create `FirebaseConfig.kt` object to initialize Firebase Auth, Firestore, and Storage.
    - [x] Explicitly enable Firestore offline persistence/caching to support offline task updates in garage dead zones.
    - [x] Expose `auth`, `firestore`, and `storage` references as singleton instances.

### 2. Global State Management Setup
- **File Name**: `AppViewModel.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/viewmodel/`
- **Description**: Setup ViewModel-based state management to handle auth status and user roles without complex parameter passing.
- **Depends On**: `build.gradle.kts` (Jetpack Lifecycle/ViewModel dependencies)
- **Where it will be called**: MainActivity and all Fragments/Composables
- **Tasks**:
    - [x] Add Jetpack Lifecycle and ViewModel dependencies to `build.gradle.kts`.
    - [x] Create `AppViewModel` extending `ViewModel` to hold `currentUser` (ID, Name, Email) and `userRole` (Mechanic vs. Manager) as `StateFlow<>` or `LiveData<>`.
    - [x] Create functions to update the global state upon successful login/logout.
    - [x] Ensure the ViewModel is shared across all screens via the MainActivity.

### 3. Authentication Service Logic
- **File Name**: `AuthService.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/service/`
- **Description**: Functions for login/logout and session management using Firebase Auth.
- **Depends On**: `FirebaseConfig.kt`, AppViewModel
- **Where it will be called**: LoginScreen Composable, MainActivity
- **Tasks**:
    - [ ] Create `loginUser(email: String, password: String)` suspend function using Firebase Auth.
    - [ ] Create `logoutUser()` function.
    - [ ] Create `getUserRole(userId: String)` suspend function to fetch the user's role from Firestore.
    - [ ] Set up an Auth state listener in `AuthService` to persist user login sessions and update the AppViewModel.

### 4. Login Screen & Main App Navigator
- **File Name**: `LoginScreen.kt` & `MainActivity.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/screens/` and `app/src/main/java/com/codegrogu/guava/`
- **Description**: Jetpack Compose UI for login and entry point/navigation setup.
- **Depends On**: `AuthService.kt`, AppViewModel
- **Where it will be called**: AppNavigation Composable
- **Tasks**:
    - [ ] Add Jetpack Compose and Navigation dependencies to `build.gradle.kts`.
    - [ ] Create `LoginScreen` Composable with Email/Password inputs, Login button, loading state, and error handling.
    - [ ] Set up Navigation Graph with root navigation controller.
    - [ ] Implement conditional navigation logic: AuthGraph (Login), MechanicGraph, or ManagerGraph based on AppViewModel `userRole` and auth status.
    - [ ] Create `AppNavigation` Composable to manage all navigation flows.

---

## Phase 2: Vehicle Check-In Module (Priority 2)
**Assignee: Member 2**

### 5. Camera & Image Optimization Component
- **File Name**: `CameraCapture.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/components/`
- **Description**: Reusable Composable to access native Android camera, capture, and compress vehicle physical condition images.
- **Depends On**: Android Manifest permissions, necessary dependencies
- **Where it will be called**: `CheckInScreen` Composable
- **Tasks**:
    - [ ] Add camera and image compression libraries to `build.gradle.kts` (e.g., `androidx.camera:camera-camera2`, image compression library).
    - [ ] **Update `AndroidManifest.xml` with `<uses-permission android:name="android.permission.CAMERA" />` and storage permissions.**
    - [ ] Implement runtime permission requests for camera and storage permissions.
    - [ ] Build Jetpack Compose camera preview UI (Viewfinder, Capture button, Retake button).
    - [ ] Implement logic to compress raw high-resolution images before saving to device cache (conserves Firebase free-tier bandwidth).

### 6. Vehicle Check-In Service
- **File Name**: `VehicleService.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/service/`
- **Description**: Firestore queries to create a new vehicle check-in, log initial kilometers, and upload compressed photos.
- **Depends On**: `FirebaseConfig.kt`
- **Where it will be called**: `CheckInScreen` Composable, `ReportService.kt`
- **Tasks**:
    - [ ] Create `uploadConditionImage(compressedImageUri: Uri)` suspend function to push local images to Firebase Storage and return the download URL.
    - [ ] Create `createCheckInRecord(vehicleDetails: VehicleData, km: Int, imageUrl: String)` suspend function.
    - [ ] Include server timestamp creation in `createCheckInRecord` to accurately log exact check-in time.
    - [ ] Ensure the check-in record automatically initializes an empty "Repair Tasks" sub-collection.

### 7. Vehicle Check-In Screen UI
- **File Name**: `CheckInScreen.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/screens/`
- **Description**: Jetpack Compose form to input truck details, kilometers, and access the camera for condition images.
- **Depends On**: `CameraCapture.kt`, `VehicleService.kt`
- **Where it will be called**: Mechanic Navigation Graph
- **Tasks**:
    - [ ] Build Compose text fields for Truck License Plate/ID and Initial Kilometers.
    - [ ] Integrate `CameraCapture` Composable into the form layout.
    - [ ] Build a "Submit Check-In" button styled with Material Design 3.
    - [ ] Implement form validation (prevent submission without km, ID, and at least one condition photo).
    - [ ] Show a Toast message and navigate back to dashboard upon successful DB upload.

---

## Phase 3: Collaborative Repairs Module (Priority 3)
**Assignee: Member 3**

### 8. Repair Service (Real-time Sync)
- **File Name**: `RepairService.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/service/`
- **Description**: Firestore listeners for real-time syncing of repair checklists using Flow.
- **Depends On**: `FirebaseConfig.kt`
- **Where it will be called**: `RepairWorkflowScreen` Composable, `TaskListItem` Composable
- **Tasks**:
    - [ ] Create `subscribeToVehicleTasks(vehicleId: String): Flow<List<RepairTask>>` using Firestore `snapshotFlow` to listen for real-time changes.
    - [ ] Create `toggleTaskStatus(vehicleId: String, taskId: String, isComplete: Boolean, mechanicId: String, mechanicName: String)` suspend function.
    - [ ] Create `updateTaskNote(vehicleId: String, taskId: String, noteText: String, mechanicId: String, mechanicName: String)` suspend function.
    - [ ] Ensure all updates append the mechanic's ID/Name to enforce accountability (NFR-1).

### 9. Task List Item Component
- **File Name**: `TaskListItem.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/components/`
- **Description**: Jetpack Compose UI for a single repair task with Material Design checkbox, displaying mechanic attributions and notes.
- **Depends On**: AppViewModel
- **Where it will be called**: `RepairWorkflowScreen` Composable
- **Tasks**:
    - [ ] Design the task row layout using Compose (Task title, native Material Checkbox).
    - [ ] Add sub-text displaying "Completed by: [Name]" conditionally when checked.
    - [ ] Add a collapsible/expandable section for "Mechanic Notes".
    - [ ] Build an input field and "Save Note" button within the expanded section.
    - [ ] Display existing notes with the name of the mechanic who wrote them.
    - [ ] Consume AppViewModel for current mechanic details directly to execute task toggles/notes.

### 10. Collaborative Repair Screen UI
- **File Name**: `RepairWorkflowScreen.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/screens/`
- **Description**: Real-time Compose list of all tasks for a specific vehicle. Multiple mechanics concurrently update this screen.
- **Depends On**: `TaskListItem.kt`, `RepairService.kt`
- **Where it will be called**: Mechanic Navigation Graph
- **Tasks**:
    - [ ] Use `LaunchedEffect` to trigger `subscribeToVehicleTasks` on screen composition.
    - [ ] Render a LazyColumn using the `TaskListItem` Composable for each task.
    - [ ] Add visual cues (e.g., a green highlight or Material icon checkmark) when another mechanic finishes a task while the screen is displayed (NFR-2).

---

## Phase 4: Reporting and Oversight Module (Priority 4)
**Assignee: Member 4**

### 11. Reporting Service
- **File Name**: `ReportService.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/service/`
- **Description**: Firestore aggregation queries to pull work history and fetch check-in logs.
- **Depends On**: `FirebaseConfig.kt`, `VehicleService.kt`
- **Where it will be called**: `ManagerDashboard` Composable
- **Tasks**:
    - [ ] Create `getEmployeePerformanceReport(mechanicId: String, dateRange: DateRange)` suspend function to query all tasks completed/noted by a specific user.
    - [ ] Create `getVehicleIntakeReport(dateRange: DateRange)` suspend function to pull initial condition images and kilometers logged at check-in.
    - [ ] Format and sanitize the data returns so they are easily consumable by the UI components.

### 12. Report Card UI Component
- **File Name**: `ReportCard.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/components/`
- **Description**: Reusable Jetpack Compose Card UI for displaying statistical summaries.
- **Depends On**: None
- **Where it will be called**: `ManagerDashboard` Composable
- **Tasks**:
    - [ ] Design Employee variant: Show mechanic name, total tasks completed, and list of specific vehicle IDs they worked on (use Material Design elevation/shadows).
    - [ ] Design Vehicle variant: Show Truck ID, Check-in Date, Initial KM, and an image thumbnail of the vehicle condition.
    - [ ] Implement a modal/fullscreen feature to click and expand the vehicle condition thumbnail to full screen.

### 13. Manager Dashboard Screen UI
- **File Name**: `ManagerDashboard.kt`
- **Directory**: `app/src/main/java/com/codegrogu/guava/ui/screens/`
- **Description**: Exclusive screen for the Manager role (Valentine). Displays oversight reports using Jetpack Compose.
- **Depends On**: `ReportCard.kt`, `ReportService.kt`, AppViewModel
- **Where it will be called**: Manager Navigation Graph
- **Tasks**:
    - [ ] Create a Compose TabRow to toggle between "Employee Reports" and "Vehicle Intake Logs".
    - [ ] Build date filter UI (e.g., "Today", "This Week", "This Month") to pass arguments to the reporting service.
    - [ ] Map the fetched data to `ReportCard` Composables.
    - [ ] Add an "Export" or "Print" button (optional, for extra polish).
    - [ ] Add a Manager Logout button using the AppViewModel authentication state logic.