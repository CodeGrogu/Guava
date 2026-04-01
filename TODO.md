# Valentine's Garage Android Application - Implementation To-Do List

This document outlines the detailed implementation plan, structured chronologically by priority. The project assumes a React Native (Android specific) and Firebase (Auth, Firestore for concurrency) technology stack. Tasks are distributed evenly among exactly 4 team members.

---

## Phase 1: Project Setup & Authentication (Priority 1)
**Assignee: Member 1**

### 1. Initialize Firebase Configuration
- **File Name**: `firebaseConfig.js`
- **Directory**: `/src/config/`
- **Description**: Setup Firebase connection for Auth, Firestore (real-time DB for concurrency), and Storage.
- **Depends On**: `package.json` (Firebase SDK installation)
- **Where it will be called**: All service files, `App.js`
- **Tasks**:
    - [ ] Initialize the React Native project (e.g., `npx react-native init ValentinesGarage`).
    - [ ] Create the Firebase project in the Firebase Console (Add an Android App).
    - [ ] Install required Firebase SDKs (`@react-native-firebase/app`, `auth`, `firestore`, `storage`).
    - [ ] **Setup Android native configuration (Add `google-services.json` to the `android/app` directory and update `build.gradle` files).**
    - [ ] Explicitly enable Firestore offline persistence/caching to support offline task updates in garage dead zones.
    - [ ] Export `auth`, `db` (Firestore), and `storage` instances from `firebaseConfig.js`.

### 2. Global State Management Setup
- **File Name**: `store.js` (if Zustand) or `AuthContext.js` (if React Context)
- **Directory**: `/src/store/` or `/src/context/`
- **Description**: Setup global state management to handle auth status and user roles without deep prop drilling.
- **Depends On**: `package.json` (Zustand installation, if applicable)
- **Where it will be called**: `App.js`, and globally across screens
- **Tasks**:
    - [ ] Install state manager (e.g., `npm install zustand`) or initialize React Context.
    - [ ] Create a global store to hold `currentUser` (ID, Name, Email) and `userRole` (Mechanic vs. Manager).
    - [ ] Create actions/reducers to update the global state upon successful login/logout.

### 3. Authentication Service Logic
- **File Name**: `authService.js`
- **Directory**: `/src/services/`
- **Description**: Functions for login/logout and session management.
- **Depends On**: `firebaseConfig.js`, Global State store
- **Where it will be called**: `LoginScreen.js`, `App.js`
- **Tasks**:
    - [ ] Create `loginUser(email, password)` function using Firebase Auth.
    - [ ] Create `logoutUser()` function.
    - [ ] Create a `getUserRole(userId)` function to fetch the user's role from Firestore.
    - [ ] Set up an auth state listener to persist user login sessions and hydrate the global state.

### 4. Login Screen & Main App Navigator
- **File Name**: `LoginScreen.js` & `App.js`
- **Directory**: `/src/screens/` and `/` (Project Root)
- **Description**: Android UI for login and entry point/routing setup.
- **Depends On**: `authService.js`, Global State
- **Where it will be called**: `index.js`
- **Tasks**:
    - [ ] Install React Navigation dependencies (`@react-navigation/native`, `@react-navigation/stack`, plus Android dependencies like `react-native-screens` and `react-native-safe-area-context`).
    - [ ] Build Login UI (Email/Password inputs, Login button, loading spinner, error handling).
    - [ ] Set up the Navigation Stacks: `AuthStack`, `MechanicStack`, `ManagerStack`.
    - [ ] Implement routing logic to automatically switch stacks based on the global state `userRole` and auth status.

---

## Phase 2: Vehicle Check-In Module (Priority 2)
**Assignee: Member 2**

### 5. Camera & Image Optimization Component
- **File Name**: `CameraCapture.js`
- **Directory**: `/src/components/`
- **Description**: Reusable component to access native Android camera, capture, and compress vehicle physical condition.
- **Depends On**: Android Manifest permissions.
- **Where it will be called**: `CheckInScreen.js`
- **Tasks**:
    - [ ] Install camera library (e.g., `react-native-vision-camera` or `expo-camera`).
    - [ ] Install an image compression library (e.g., `react-native-compressor` or `react-native-image-crop-picker`).
    - [ ] **Update `android/app/src/main/AndroidManifest.xml` with `<uses-permission android:name="android.permission.CAMERA" />` and related storage permissions.**
    - [ ] Write logic to request Android runtime camera and storage permissions from the user.
    - [ ] Build camera UI (Viewfinder, Capture button, Retake button).
    - [ ] Implement logic to compress raw high-resolution images before temporarily saving them to local device cache (saves Firebase free-tier bandwidth).

### 6. Vehicle Check-In Service
- **File Name**: `vehicleService.js`
- **Directory**: `/src/services/`
- **Description**: DB queries to create a new vehicle check-in, log initial kilometres, and upload compressed photos.
- **Depends On**: `firebaseConfig.js`
- **Where it will be called**: `CheckInScreen.js`, `reportService.js`
- **Tasks**:
    - [ ] Create `uploadConditionImage(compressedImageUri)` function to push local images to Firebase Storage and return the download URL.
    - [ ] Create `createCheckInRecord(truckDetails, km, imageUrl)` function.
    - [ ] Include server timestamp creation in `createCheckInRecord` to accurately log exact check-in time.
    - [ ] Ensure the check-in record automatically initializes an empty "Repair Tasks" sub-collection.

### 7. Vehicle Check-In Screen UI
- **File Name**: `CheckInScreen.js`
- **Directory**: `/src/screens/`
- **Description**: Form to input truck details, kilometers, and access the camera for condition images.
- **Depends On**: `CameraCapture.js`, `vehicleService.js`
- **Where it will be called**: Mechanic Stack Navigation
- **Tasks**:
    - [ ] Build text inputs for Truck License Plate/ID and Initial Kilometres.
    - [ ] Integrate `<CameraCapture />` component into the form layout.
    - [ ] Build a "Submit Check-In" button specifically styled following Android Material Design guidelines.
    - [ ] Implement form validation (prevent submission without km, ID, and at least one condition photo).
    - [ ] Show an Android Toast message (`ToastAndroid`) and navigate back to the dashboard upon successful DB upload.

---

## Phase 3: Collaborative Repairs Module (Priority 3)
**Assignee: Member 3**

### 8. Repair Service (Real-time Sync)
- **File Name**: `repairService.js`
- **Directory**: `/src/services/`
- **Description**: Firestore listeners for real-time syncing of repair checklists.
- **Depends On**: `firebaseConfig.js`
- **Where it will be called**: `RepairWorkflowScreen.js`, `TaskListItem.js`
- **Tasks**:
    - [ ] Create `subscribeToVehicleTasks(vehicleId, callback)` using Firestore `onSnapshot` to listen for real-time changes.
    - [ ] Create `toggleTaskStatus(vehicleId, taskId, isComplete, mechanicId, mechanicName)` function.
    - [ ] Create `updateTaskNote(vehicleId, taskId, noteText, mechanicId, mechanicName)` function.
    - [ ] Ensure all updates append the mechanic's ID/Name to enforce accountability (NFR-1).

### 9. Task List Item Component
- **File Name**: `TaskListItem.js`
- **Directory**: `/src/components/`
- **Description**: UI for a single repair task with an Android-style checkbox, displaying mechanic attributions and notes.
- **Depends On**: Global State (Context/Zustand)
- **Where it will be called**: `RepairWorkflowScreen.js`
- **Tasks**:
    - [ ] Design the task row layout (Task title, native Android Checkbox component via `@react-native-community/checkbox` or similar).
    - [ ] Add a sub-text field displaying "Completed by: [Name]" conditionally if checked.
    - [ ] Add a collapsible/expandable section for "Mechanic Notes".
    - [ ] Build an input field and "Save Note" button within the expanded section.
    - [ ] Display existing notes with the name of the mechanic who wrote them.
    - [ ] Consume global state for the current mechanic's details directly in this component to execute task toggles/notes, avoiding deep prop drilling.

### 10. Collaborative Repair Screen UI
- **File Name**: `RepairWorkflowScreen.js`
- **Directory**: `/src/screens/`
- **Description**: Real-time list of all tasks for a specific vehicle. Multiple mechanics concurrently update this screen.
- **Depends On**: `TaskListItem.js`, `repairService.js`
- **Where it will be called**: Mechanic Stack Navigation
- **Tasks**:
    - [ ] Implement `useEffect` to trigger `subscribeToVehicleTasks` on screen mount.
    - [ ] Render a FlatList/ScrollView using the `<TaskListItem />` component for each task.
    - [ ] Add visual cues (e.g., a green highlight or Material icon checkmark) when another mechanic finishes a task while the screen is open (NFR-2).

---

## Phase 4: Reporting and Oversight Module (Priority 4)
**Assignee: Member 4**

### 11. Reporting Service
- **File Name**: `reportService.js`
- **Directory**: `/src/services/`
- **Description**: Data aggregation queries to pull work history and fetch check-in logs.
- **Depends On**: `firebaseConfig.js`, `vehicleService.js`
- **Where it will be called**: `ManagerDashboard.js`
- **Tasks**:
    - [ ] Create `getEmployeePerformanceReport(mechanicId, dateRange)` to query all tasks completed/noted by a specific user.
    - [ ] Create `getVehicleIntakeReport(dateRange)` to pull initial condition images and kilometres logged at check-in.
    - [ ] Format and sanitize the data returns so they are easily consumable by the UI components.

### 12. Report Card UI Component
- **File Name**: `ReportCard.js`
- **Directory**: `/src/components/`
- **Description**: Reusable Android Card UI component for displaying statistical summaries.
- **Depends On**: None
- **Where it will be called**: `ManagerDashboard.js`
- **Tasks**:
    - [ ] Design Employee variant: Show mechanic name, total tasks completed, and a list of specific vehicle IDs they worked on (use Android elevation for card shadows).
    - [ ] Design Vehicle variant: Show Truck ID, Check-in Date, Initial KM, and an image thumbnail of the vehicle condition.
    - [ ] Implement a modal/lightbox feature to click and expand the vehicle condition thumbnail to full screen.

### 13. Manager Dashboard Screen UI
- **File Name**: `ManagerDashboard.js`
- **Directory**: `/src/screens/`
- **Description**: Exclusive screen for the Manager role (Valentine). Displays oversight reports.
- **Depends On**: `ReportCard.js`, `reportService.js`, Global State
- **Where it will be called**: Manager Stack Navigation
- **Tasks**:
    - [ ] Create an Android Top Tabs layout (using `@react-navigation/material-top-tabs`) to toggle between "Employee Reports" and "Vehicle Intake Logs".
    - [ ] Build date filter UI (e.g., "Today", "This Week", "This Month") to pass arguments to the reporting service.
    - [ ] Map the fetched data to `<ReportCard />` components.
    - [ ] Add an "Export" or "Print" dummy button (optional, for extra polish).
    - [ ] Add a Manager Logout button using the global authentication state logic.