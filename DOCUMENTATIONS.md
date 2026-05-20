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
