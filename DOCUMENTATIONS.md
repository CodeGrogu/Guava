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

