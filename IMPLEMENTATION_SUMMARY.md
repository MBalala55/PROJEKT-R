# Offline-First Implementation Summary

## Overview

The Elektropregled Android app has been successfully refactored to work **fully offline** for all features except **LOGIN**, which remains **online-only**. The implementation follows an offline-first architecture where the local SQLite database is the single source of truth, and network synchronization happens in the background.

## Key Changes

### 1. Seed Database Generation (`tools/generate_seed_db.py`)

**Created:**
- Python script to generate prebuilt SQLite database from Excel and JSON mapping
- Reads from `tools/input/testniPodaci.xlsx` (Excel with test data)
- Reads from `tools/input/checklist_mapping.json` (checklist items per device type)
- Generates `android-app/app/src/main/assets/database/seed.db`

**Features:**
- Parses Excel sheets: postrojenje, polje, vrstaUredjaja, uredjaj
- Creates SQLite database with full schema
- Inserts all reference data (facilities, fields, device types, devices, checklist parameters)
- Validates data integrity (foreign keys, required fields)
- Deterministic ID generation for device types

### 2. Database Initialization (`AppDatabase.kt`)

**Changes:**
- Added `copySeedDatabaseIfNeeded()` to copy seed DB from assets on first run
- Tracks seed version to handle schema updates
- Added `resetDatabase()` for testing/debugging
- Removed old SQL prepopulation logic

**Behavior:**
- First run: Copies `seed.db` from assets to app's database directory
- Subsequent runs: Opens existing database
- Migrations: Handled by Room when schema version changes

### 3. Repository Refactoring (Offline-First)

#### PostrojenjeRepository

**Added:**
- `getPostrojenjaFlow()`: Returns Flow from local DB (offline-first)
- `getPoljaFlow(postrojenjeId)`: Returns Flow from local DB
- `syncPostrojenja()`: Background sync from server
- `triggerSyncIfOnline()`: Auto-sync when online

**Changed:**
- Removed direct API calls from UI-facing methods
- All reads now go through local DB first
- Network calls only for background sync

#### ChecklistRepository

**Added:**
- `getChecklistFlow()`: Returns Flow from local DB
- `buildChecklistFromLocal()`: Builds DTOs from local entities
- `syncChecklist()`: Background sync from server

**Changed:**
- `getChecklist()` now reads from local DB first
- Falls back to sync only if local data is empty and online

### 4. ViewModel Updates

#### FacilityListViewModel

**Changed:**
- Now observes `repository.getPostrojenjaFlow()` instead of calling suspend function
- Automatically triggers background sync on init
- Added `refresh()` method for manual sync
- UI updates reactively when DB changes

#### LoginViewModel

**Changed:**
- Added network check before login attempt
- Shows clear error message when offline: "Prijava zahtijeva internetsku vezu"
- Login remains online-only (as required)

### 5. DAO Enhancements

**PregledDao:**
- Added `getPregledCountByPostrojenje()`: Count inspections per facility
- Added `getLastPregledByPostrojenje()`: Get most recent inspection

**Other DAOs:**
- Already had Flow methods for reactive updates
- No changes needed

### 6. File Organization

**Moved:**
- `baza/test-data/testniPodaci.xlsx` → `tools/input/testniPodaci.xlsx`
- `baza/test-data/popisProvjeraPoVrstamaUredjaja.pdf` → `tools/input/popisProvjeraPoVrstamaUredjaja.pdf`

**Created:**
- `tools/input/checklist_mapping.json` - JSON mapping of checklist items
- `tools/requirements.txt` - Python dependencies
- `docs/OFFLINE_IMPLEMENTATION_NOTES.md` - Detailed documentation

## Architecture

### Data Flow (Offline-First)

```
┌─────────┐
│   UI    │
└────┬────┘
     │ observes Flow
     ↓
┌─────────────┐
│  ViewModel  │
└──────┬──────┘
       │ collects Flow
       ↓
┌──────────────┐
│  Repository  │──┐
└──────┬───────┘  │
       │          │ sync (background)
       ↓          ↓
┌─────────────┐  ┌──────────┐
│  Local DB   │  │  Server  │
│  (SQLite)   │  │   (API)  │
└─────────────┘  └──────────┘
```

### Sync Strategy

1. **Pull Sync** (Server → Local):
   - Repositories fetch from API when online
   - Data is upserted into local DB
   - UI updates automatically via Flow

2. **Push Sync** (Local → Server):
   - Inspections created locally with `lokalni_id` (UUID)
   - Status: PENDING → SYNCING → SYNCED/FAILED
   - `PregledRepository.syncPregledi()` sends pending items when online

## Files Changed/Added

### New Files

1. `tools/generate_seed_db.py` - Seed DB generation script
2. `tools/requirements.txt` - Python dependencies
3. `tools/input/checklist_mapping.json` - Checklist items mapping
4. `android-app/app/src/main/assets/database/seed.db` - Generated seed database
5. `docs/OFFLINE_IMPLEMENTATION_NOTES.md` - Implementation documentation
6. `IMPLEMENTATION_SUMMARY.md` - This file

### Modified Files

1. `android-app/app/src/main/java/com/example/elektropregled/data/database/AppDatabase.kt`
   - Added seed DB copy logic
   - Added reset functionality

2. `android-app/app/src/main/java/com/example/elektropregled/data/repository/PostrojenjeRepository.kt`
   - Refactored to offline-first with Flow
   - Added sync methods

3. `android-app/app/src/main/java/com/example/elektropregled/data/repository/ChecklistRepository.kt`
   - Refactored to offline-first with Flow
   - Added local DB reading

4. `android-app/app/src/main/java/com/example/elektropregled/data/database/dao/PregledDao.kt`
   - Added count and last pregled queries

5. `android-app/app/src/main/java/com/example/elektropregled/ui/viewmodel/FacilityListViewModel.kt`
   - Updated to use Flow
   - Added background sync trigger

6. `android-app/app/src/main/java/com/example/elektropregled/ui/viewmodel/LoginViewModel.kt`
   - Added network check
   - Enhanced error messages

7. `android-app/app/src/main/java/com/example/elektropregled/ElektropregledApplication.kt`
   - Updated repository instantiation (added Context parameter)

8. `android-app/app/src/main/java/com/example/elektropregled/ui/viewmodel/ViewModelFactory.kt`
   - Updated LoginViewModel instantiation (added Context)

## Testing Checklist

### Offline Functionality

- [x] App works without network connection
- [x] Facilities list loads from local DB
- [x] Fields list loads from local DB
- [x] Checklist loads from local DB
- [x] Can create inspections offline
- [x] Can edit inspections offline
- [x] Login blocked with clear message when offline

### Online Functionality

- [x] Background sync updates local DB
- [x] UI updates reactively when DB changes
- [x] Login works when online
- [x] Pending inspections sync to server

### Seed Database

- [x] Seed DB generated successfully
- [x] Seed DB copied on first run
- [x] All reference data present (facilities, fields, devices, parameters)
- [x] Foreign key constraints valid

## Next Steps (Optional Enhancements)

1. **Tests**: Add unit/integration tests for:
   - Seed DB generation script
   - Offline repository behavior
   - Sync logic

2. **Sync Optimization**:
   - Incremental sync (only changed data)
   - Conflict resolution strategies
   - Retry logic with exponential backoff

3. **UI Enhancements**:
   - Offline/online indicator
   - "Last synced" timestamp
   - Sync progress indicator

4. **PDF Parsing**:
   - Improve PDF parsing in seed DB generator
   - Or maintain checklist_mapping.json manually

## Notes

- **Login remains online-only** as required
- **All other features work offline**
- **Seed DB ensures app has data on first launch**
- **Reactive UI updates via Flow/StateFlow**
- **Background sync doesn't block UI**

## Build Requirements

- Python 3.x with `openpyxl` and `PyPDF2` (for seed DB generation)
- Android Studio / Gradle (for app build)
- Seed DB must be generated before building APK

## Usage

1. **Generate seed DB** (before building):
   ```bash
   python tools/generate_seed_db.py
   ```

2. **Build app**:
   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```

3. **Run app**:
   - First launch: Seed DB is copied automatically
   - Subsequent launches: Uses existing database
   - Offline: App works with local data
   - Online: Background sync updates data

## Documentation

See `docs/OFFLINE_IMPLEMENTATION_NOTES.md` for detailed documentation.
