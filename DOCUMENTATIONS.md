# Document everything you do here.
## Phase 2 — Vehicle Check-In Module
**Author**: Pascal
**Branch**: feature/check-in (suggested)
**Date**: 2026-05-10
**Status**: Complete

---

### Files Created / Modified

| File | Action | Location |
|------|--------|----------|
| `app/build.gradle.kts` | Modified | `app/` — added CameraX, Compressor, Coil |
| `AndroidManifest.xml` | Modified | `app/src/main/` — added CAMERA permission |
| `CameraCapture.kt` | Created | `ui/components/` |
| `VehicleService.kt` | Created | `service/` |
| `CheckInScreen.kt` | Created | `ui/screens/` |

---

### What Each File Does

**`CameraCapture.kt`**
Reusable composable with three states:
- **Permission denied** — shows a "GRANT ACCESS" button that
  re-launches the runtime permission request
- **Live viewfinder** — CameraX `PreviewView` with an orange
  capture button overlaid at the bottom center
- **Photo taken** — shows a thumbnail preview with a "CAPTURED"
  badge and a "RETAKE PHOTO" button to discard and reshoot

After capture, the raw image is passed through `Compressor`
(NFR-5) targeting 1280×720 at 70% JPEG quality before the
URI is returned to the parent via `onImageCaptured`.

**`VehicleService.kt`**
Three suspend functions:
- `uploadConditionImage(uri)` — pushes the compressed image to
  Firebase Storage under `condition_photos/` and returns the
  public download URL
- `createCheckInRecord(vehicle, uid, name)` — writes the vehicle
  document to Firestore with a server timestamp (FR-1.3) and
  creates an empty `tasks` sub-collection so Member 3's
  `RepairService` can attach tasks without needing to create the
  collection itself
- `getCheckedInVehicles()` — queries vehicles with
  `status == "checked_in"` ordered newest first, for use by the
  mechanic's vehicle list

**`CheckInScreen.kt`**
A scrollable form screen with:
- `GarageTextField` for license plate (auto-uppercased) and KM
- `CameraCapture` composable embedded in the form
- Full validation before submission — blocks if any field is
  empty or no photo has been taken (FR-1.1, FR-1.3)
- Two-step async submission: image upload first, then Firestore
  record creation. If upload fails the record is never written
- Success shows a `Toast` with the plate number and navigates
  back. Failure shows an inline error banner

---

### Firestore Structure Written by This Module

```
/vehicles/{auto-id}
    licensePlate:      String   // e.g. "N 123-456 WB"
    initialKm:         Int      // e.g. 84320
    conditionImageUrl: String   // Firebase Storage download URL
    checkedInByUid:    String   // mechanic's Firebase Auth UID
    checkedInByName:   String   // mechanic's display name
    checkInTimestamp:  Timestamp // server timestamp
    status:            String   // "checked_in"

    /tasks/_init
        initialized: true       // placeholder so sub-collection exists
```

Member 3 (`RepairService`) reads `/vehicles/{id}/tasks`.
Member 4 (`ReportService`) reads `/vehicles` filtered by timestamp.
Both depend on this structure — do not rename fields.

---

### Design System Compliance
All UI matches the garage industrial theme from `LoginScreen.kt`:
- `GarageBackground()`, `GarageButton()`, `GarageTextField()`
  reused throughout
- `FontFamily.Monospace` for all text
- `SafetyOrange` for all icons, borders, and accents
- ALL CAPS labels with letter spacing

---

## Phase 3 — Collaborative Repairs Module
**Author**: Enoch (Member 3)
**Branch**: feature/collaborative-repairs (suggested)
**Date**: 2026-05-20
**Status**: In Progress

---

### Commit 1: RepairTask Model

**Files Created**
| File | Action | Location |
|------|--------|----------|
| `RepairTask.kt` | Created | `model/` |

**What It Does**

This commit introduces two data classes that represent repair tasks in the system:

1. **`MechanicNote`** — A single note written by a mechanic about a repair task
   - `text` — the actual message/observation written by the mechanic
   - `mechanicUid` — who wrote it (for database tracking)
   - `mechanicName` — who wrote it (for UI display to team members)
   - `timestamp` — when they wrote it (maintains chronological order)

2. **`RepairTask`** — A single repair job that needs to be completed on a vehicle
   - `id` — unique identifier (created by Firestore)
   - `description` — what needs to be fixed (e.g., "Replace clutch", "Fix brakes")
   - `isCompleted` — whether the job is done (true/false)
   - `completedByUid` — which mechanic finished it (empty if not done)
   - `completedByName` — mechanic's name for display (empty if not done)
   - `completedAt` — exact timestamp when it was completed (null if not done)
   - `notes` — list of all mechanic notes about this task (audit trail)

**Why This Matters**

These models allow mechanics to:
- See what work needs doing on a vehicle
- Mark tasks complete with their name attached (accountability — NFR-1)
- Leave notes for other team members
- See the history of who did what and when

**Firestore Structure This Creates**

The models expect tasks to live here in Firestore:
```
/vehicles/{vehicleId}
    /tasks/{taskId}
        description:      String
        isCompleted:      Boolean
        completedByUid:   String
        completedByName:  String
        completedAt:      Number (timestamp)
        notes:            [
            {text, mechanicUid, mechanicName, timestamp},
            ...
        ]
```

**Next Steps**

Next commit will create `RepairService.kt` to read and write these tasks to Firestore in real-time.

---

### Commit 2: RepairService — Read-Only Functions

**Files Created**
| File | Action | Location |
|------|--------|----------|
| `RepairService.kt` | Created | `service/` |

**What It Does**

This commit introduces `RepairService` as a singleton object that handles all real-time communication with Firestore for repair tasks. It contains:

1. **`convertFirestoreDocToRepairTask()` (Private Helper)**
   - Takes raw Firestore document data and converts it to our `RepairTask` Kotlin model
   - Handles extraction and mapping of all fields
   - Safely handles missing fields with default values
   - Converts the `notes` array from Firestore Maps into `MechanicNote` objects

2. **`subscribeToVehicleTasks(vehicleId: String): Flow<List<RepairTask>>`**
   - Returns a **Flow** — think of it as a "stream" of data updates
   - Sets up a Firestore snapshot listener that watches `/vehicles/{id}/tasks`
   - Automatically sends task updates whenever ANY change happens in Firestore
   - Multiple mechanics viewing the same vehicle will all receive the same updates in real-time
   - Automatically filters out the `_init` placeholder document (created during check-in)
   - Cleans up the listener when the screen navigates away (saves bandwidth/battery)

**Key Concept: Flow as a Real-Time Stream**

A Flow is like a fire hose of data:
- Instead of asking Firestore "give me tasks" once, you open the valve and say "tell me whenever tasks change"
- Firestore pushes updates immediately
- The UI screen (composables) listens to this stream and re-renders when data arrives
- Multiple mechanics seeing the same vehicle instantly see each other's changes

**Firestore Queries This Creates**

```kotlin
// Listen to all tasks for a specific vehicle, forever
/vehicles/{vehicleId}/tasks (all documents)
```

**Error Handling**

- If the Firestore listener encounters an error (network down, permission denied), the Flow closes with the error
- If a document fails to convert, it's skipped and logged
- Composables can catch these errors with `try-catch` in their `LaunchedEffect`

**Next Steps**

Next commit will add write functions to `RepairService` (toggle task completion and add notes).

---

### Commit 3: RepairService — Write Functions (Mutations)

**Files Modified**
| File | Action | Location |
|------|--------|----------|
| `RepairService.kt` | Modified | `service/` — added 2 write functions |

**What It Does**

This commit extends `RepairService` with two suspend functions that allow mechanics to UPDATE tasks in Firestore:

1. **`toggleTaskCompletion(vehicleId, taskId, isComplete, mechanicUid, mechanicName): Result<Unit>`**
   - Mark a repair task as complete OR undo completion (isComplete true/false)
   - When marking complete:
     - Sets the task's `isCompleted` flag to true
     - Records which mechanic did it (`completedByUid` and `completedByName`)
     - Saves exact server timestamp (`completedAt`)
   - When undoing (marking incomplete):
     - Sets `isCompleted` to false
     - Clears the mechanic attribution fields
     - Clears the timestamp
   - Returns `Result<Unit>` for error handling (success or failure with exception)
   - Enforces accountability (NFR-1): You can't complete a task anonymously

2. **`addNoteToTask(vehicleId, taskId, noteText, mechanicUid, mechanicName): Result<Unit>`**
   - Adds a single mechanic note to a task's notes array
   - Each note includes:
     - The text the mechanic wrote
     - Who wrote it (`mechanicUid` and `mechanicName`)
     - When it was written (server timestamp)
   - Uses Firestore's `arrayUnion()` helper to safely APPEND to the notes array without overwriting
   - Multiple mechanics can add notes to the same task in any order — Firestore handles conflicts automatically
   - Returns `Result<Unit>` for error handling
   - Enforces accountability (NFR-1): Every note is tagged with who wrote it

**Key Concepts Explained**

- **Result<T>**: A wrapper that holds either Success(data) or Failure(error). Lets us handle success/failure in one function.
- **arrayUnion()**: A Firestore helper that appends to an array safely, even if multiple clients are writing at the same time.
- **Server timestamp**: The exact time the Firestore server receives the update (not the device clock — much more reliable).
- **Accountability Trail**: Every change is tagged with mechanic UID/name, so managers can see who did what and when.

**Firestore Operations This Creates**

```kotlin
// Update a task's completion status
/vehicles/{vehicleId}/tasks/{taskId}
    isCompleted: Boolean
    completedByUid: String
    completedByName: String
    completedAt: Timestamp (server time)

// Append a note to a task's notes array
/vehicles/{vehicleId}/tasks/{taskId}
    notes: [
        ...existing notes...,
        {text, mechanicUid, mechanicName, timestamp}  <-- NEW
    ]
```

**Error Handling**

Both functions wrap their Firestore calls in try-catch:
- If the update succeeds, return `Result.success(Unit)`
- If anything fails (network down, permissions denied, task doesn't exist), catch the exception and return `Result.failure(e)`
- The UI screens can check the result and show error snackbars if needed

**Next Steps**

Next commit will create the `TaskListItem` composable (basic UI for displaying a single task).

---

### Commit 4: TaskListItem Component — Basic UI

**Files Created**
| File | Action | Location |
|------|--------|----------|
| `TaskListItem.kt` | Created | `ui/components/` |

**What It Does**

This commit introduces `TaskListItem`, a reusable Jetpack Compose component that displays a single repair task as a row. It's the building block that RepairWorkflowScreen will use to populate its task list.

**Component Structure**

The TaskListItem displays:

1. **Checkbox (Material Design)**
   - Status: checked if task is complete, unchecked if incomplete
   - When tapped: calls `onTaskToggle(newState)` callback
   - Styled with `SafetyOrange` (checked) and light gray (unchecked) colors
   - Matches the industrial theme

2. **Task Description (Text)**
   - The task title (e.g., "Replace clutch", "Fix brake pads")
   - Uses Monospace font to match the garage theme
   - Bold/semibold weight for readability

3. **"Completed by [Name]" (Conditional Text)**
   - Only shows if the task is marked complete (`isCompleted == true`)
   - Shows the name of the mechanic who completed it (in `SafetyOrange` color)
   - Positioned with left padding (40.dp) to align under the task description
   - Makes it easy to see who did what work and when

**Component Signature**

```kotlin
@Composable
fun TaskListItem(
    task: RepairTask,                          // The task data
    currentMechanicUid: String,                // Current mechanic's UID
    currentMechanicName: String,               // Current mechanic's name
    onTaskToggle: (isComplete: Boolean) -> Unit, // Callback when checkbox tapped
    modifier: Modifier = Modifier              // Optional layout modifier
)
```

**Layout**

```
┌─────────────────────────────────────────┐
│ ☐ Replace clutch                         │  ← Row 1: Checkbox + Description
│   Completed by: Ahmed                    │  ← Row 2: Only if completed
└─────────────────────────────────────────┘
```

**Design System Compliance**

- ✅ Uses `SafetyOrange` for accents (checkbox, completed-by text)
- ✅ Uses `Monospace` font for all text
- ✅ Follows Material Design 3 checkbox component
- ✅ Consistent padding (16.dp) with industrial theme
- ✅ Clean, readable layout matching other screens

**How It's Used in RepairWorkflowScreen**

In the next commit, RepairWorkflowScreen will use this like:
```kotlin
LazyColumn {
    items(tasks) { task ->
        TaskListItem(
            task = task,
            currentMechanicUid = currentUser.uid,
            currentMechanicName = currentUser.name,
            onTaskToggle = { isComplete ->
                // Call RepairService to update Firestore
                RepairService.toggleTaskCompletion(...)
            }
        )
    }
}
```

**Next Steps**

Next commit will extend TaskListItem with a collapsible notes section (Commit 5).

---

### Commit 5: Extend TaskListItem — Add Notes Section

**Files Modified**
| File | Action | Location |
|------|--------|----------|
| `TaskListItem.kt` | Modified | `ui/components/` — added notes section |

**What It Does**

This commit extends `TaskListItem` with a collapsible notes section. When the mechanic expands the notes section, they can:
1. **View existing notes** — See all notes other mechanics have written, tagged with their names
2. **Write a new note** — Type a note in an input field
3. **Save the note** — Tap "SAVE NOTE" button to send it to Firestore

**New Features Added**

1. **Expand/Collapse Button**
   - Shows an up/down arrow icon (only if there are notes or task is complete)
   - Tapping the row (except the checkbox) toggles expansion
   - Starts collapsed by default (saves screen space)

2. **Mechanic Notes Display**
   - Shows all existing notes with "— [MechanicName]" prefix
   - Each note is displayed below the mechanic's name in monospace font
   - Notes are read-only (mechanics can only add, not edit)
   - Maintains chronological order (first note written, first displayed)

3. **Note Input Section**
   - `OutlinedTextField` for typing the note text (max 3 lines)
   - Limited to monospace font for consistency
   - "SAVE NOTE" button styled with `SafetyOrange`
   - Button is disabled if input is empty

**Component Signature (Updated)**

```kotlin
@Composable
fun TaskListItem(
    task: RepairTask,
    currentMechanicUid: String,
    currentMechanicName: String,
    onTaskToggle: (isComplete: Boolean) -> Unit,
    onNoteAdded: (noteText: String) -> Unit,  // NEW callback
    modifier: Modifier = Modifier
)
```

**Layout (Expanded)**

```
┌────────────────────────────────────────────┐
│ ☐ Replace clutch              ▲            │  ← Row 1: checkbox + desc + expand icon
│   Completed by: Ahmed                       │  ← Row 2: only if completed
│                                              │
│   MECHANIC NOTES:                           │  ← Row 3: notes section (when expanded)
│   — Ahmed                                    │
│     Ordered replacement part from supplier   │
│                                              │
│   — Mohamed                                  │
│     Part arrived, starting replacement       │
│                                              │
│   [Note input field                    ]    │  ← Row 4: input
│                                              │
│   [SAVE NOTE]                          [  ] │  ← Row 5: button
└────────────────────────────────────────────┘
```

**How Notes Work**

- Each note is tagged with who wrote it (mechanic name)
- Notes are stored in the task's `notes` array in Firestore
- New notes are appended automatically via `arrayUnion()`
- No notes are deleted or edited — they form an audit trail

**Next Steps**

Next commit will create RepairWorkflowScreen (the main screen).

---

### Commit 6: Create RepairWorkflowScreen — Main UI

**Files Created**
| File | Action | Location |
|------|--------|----------|
| `RepairWorkflowScreen.kt` | Created | `ui/screens/` |

**What It Does**

This is the MAIN SCREEN where all the real-time magic happens! It's where mechanics see and work on all repair tasks for a vehicle.

**Screen Flow**

1. **Screen Loads** → `RepairWorkflowScreen` is displayed
2. **LaunchedEffect Triggers** → Sets up the real-time listener by calling `subscribeToVehicleTasks(vehicleId)`
3. **Initial Data Loaded** → Firestore sends current tasks through the Flow
4. **Screen Shows Tasks** → All tasks displayed in a scrollable LazyColumn
5. **Another Mechanic Updates a Task** → Firestore listener fires immediately
6. **New Data Emitted** → Flow sends updated task list
7. **Screen Re-renders** → All viewing mechanics see the change in real-time

**Component Sections**

1. **Header**
   - Shows vehicle ID (e.g., "VEHICLE: N 123-456 WB")
   - Shows task count (e.g., "3 tasks")

2. **Loading State**
   - Shows spinner + "Loading tasks..." while waiting for Firestore
   - Important for slow networks

3. **Empty State**
   - Shows "No repair tasks yet" if vehicle has no tasks
   - Useful during initial setup

4. **Task List (Main Content)**
   - LazyColumn with `TaskListItem` components
   - Each task has unique key (task.id) for efficient re-composition
   - Scrollable if many tasks

5. **Snackbar for Feedback**
   - "Task completed!" when mechanic marks task done
   - "Note saved!" when mechanic saves a note
   - Error messages if Firestore operations fail

**The Real-Time Magic**

```
┌─────────────────────────────────────────┐
│ RepairWorkflowScreen (Mechanic A)       │
│ ┌─────────────────────────────────────┐ │
│ │ ☐ Replace clutch            ▼       │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ ☐ Fix brakes                ▼       │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
           ↑
           │ (Mechanic A sees checkbox)
           │
      Meanwhile in Firestore...
           │
    Mechanic B marks task complete
           │
           ↓
      Firestore emits new data through Flow
           │
           ↓
    RepairWorkflowScreen re-renders
           │
           ↓
┌─────────────────────────────────────────┐
│ RepairWorkflowScreen (Mechanic A)       │
│ ┌─────────────────────────────────────┐ │
│ │ ☑ Replace clutch            ▼       │ │  ← Auto-updated! No refresh button!
│ │   Completed by: Mechanic B         │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ ☐ Fix brakes                ▼       │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Component Signature**

```kotlin
@Composable
fun RepairWorkflowScreen(
    vehicleId: String,           // Which vehicle to load tasks for
    currentUser: User,           // Current mechanic (for attribution)
    modifier: Modifier = Modifier
)
```

**Callback Handlers**

- **onTaskToggle**: Calls `RepairService.toggleTaskCompletion()` in a coroutine
  - Shows success/error snackbar
  - Flow automatically emits new data → screen re-renders
  
- **onNoteAdded**: Calls `RepairService.addNoteToTask()` in a coroutine
  - Shows success/error snackbar
  - Flow automatically emits new data → screen re-renders with new note

**Design System Compliance**

- ✅ Uses industrial theme colors (SafetyOrange, Monospace fonts)
- ✅ Matches existing screen layouts
- ✅ Clear loading/empty states
- ✅ Humanized error messages
- ✅ Smooth scrolling LazyColumn

**Next Steps**

Next commit will update TODO.md to mark Phase 3 complete, and finalize DOCUMENTATIONS.md.

---

### Commit 7: Integration & Polish — Update Docs & Navigation

**Files Modified**
| File | Action | Location |
|------|--------|----------|
| `DOCUMENTATIONS.md` | Modified | Updated with Phase 3 summary |
| `TODO.md` | Modified | Mark Phase 3 tasks complete |

**What It Does**

This commit finalizes Phase 3 by:
1. Completing all documentation for Commits 4, 5, 6
2. Marking Phase 3 tasks as complete in TODO.md
3. Creating a summary of the real-time collaboration system

**Phase 3 Summary**

**What We Built:**
- ✅ `RepairTask` model with mechanic attribution
- ✅ `RepairService` with real-time Flow listeners and write functions
- ✅ `TaskListItem` component with collapsible notes
- ✅ `RepairWorkflowScreen` main UI with real-time updates

**How It Works Together:**

```
Mechanic Opens Vehicle
        ↓
RepairWorkflowScreen displays
        ↓
subscribeToVehicleTasks() opens Firestore listener
        ↓
Flow<List<RepairTask>> emitted immediately
        ↓
Screen shows all tasks (LazyColumn of TaskListItem)
        ↓
Mechanic taps checkbox OR adds note
        ↓
onTaskToggle / onNoteAdded callback fires
        ↓
coroutineScope.launch starts async operation
        ↓
RepairService.toggleTaskCompletion() / addNoteToTask() called
        ↓
Firestore document updated with mechanic attribution
        ↓
Firestore listener detects change
        ↓
NEW Flow emission with updated data
        ↓
Screen re-renders automatically (NFR-2: Visual updates)
        ↓
ALL mechanics viewing vehicle see change in REAL-TIME
        ↓
Snackbar shows success/error message
```

**Key NFRs Implemented:**

- **NFR-1 (Accountability)**: Every task toggle and note is tagged with mechanic UID/name
- **NFR-2 (Real-time updates)**: When mechanic A updates, mechanic B's screen updates automatically (zero refresh button needed)

**Integration Notes**

- RepairWorkflowScreen needs to be added to the Mechanic Navigation Graph
- Pass `vehicleId` from parent navigation
- Pass `currentUser` from AppViewModel
- Example:
  ```kotlin
  NavHost(navController, startDestination = "vehicleList") {
      composable("repairWorkflow/{vehicleId}") { backStackEntry ->
          val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
          RepairWorkflowScreen(
              vehicleId = vehicleId,
              currentUser = appViewModel.currentUser.value
          )
      }
  }
  ```

**Testing Recommendations**

Open RepairWorkflowScreen on TWO devices/simulators with the same vehicleId:
1. On Device A: Mark a task complete
2. On Device B: Watch it update automatically (no refresh!)
3. On Device B: Add a note
4. On Device A: See the note appear immediately
5. Verify mechanic names appear with every action

**Code Quality**

- ✅ Humanized comments throughout all files
- ✅ Simple Kotlin (no complex patterns)
- ✅ Proper error handling with Result<T>
- ✅ No memory leaks (Flow listeners cleaned up when screen closes)
- ✅ Snackbar feedback for all user actions

**What's NOT Implemented (For Future)**

- No retry logic for failed Firestore operations (could add)
- No optimistic updates (updates only show after Firestore confirms)
- No sorting/filtering (could add in future)
- No task creation UI (Manager does this separately)

---

## Summary: Phase 3 Complete

Phase 3 creates a **real-time collaborative repair management system**. Multiple mechanics can work on the same vehicle and see each other's progress instantly. Every action is logged for accountability, and the UI provides clear visual feedback for all operations.

**Files Created**: 4 files
**Files Modified**: 1 file (docs + TODO)
**Key Features**: Real-time Flow listeners, mechanic attribution, collapsible notes, error handling
**Learning Level**: Beginner-friendly Kotlin (no complex patterns)

