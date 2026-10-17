# Cloud Data Restoration & Fast Login Architecture

Resolve data loss and replacement issues when clearing app data. Decouple restoration from the login screen so authentication remains instantaneous, and introduce a dedicated post-login interactive restoration dialog with real-time progress, record summaries, cloud-priority merging, and manual restoration controls in Settings.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned based on your preferences:

- **Confirmed Decision 1 (Restoration UX)**: Interactive modal dialog displayed post-login showing animated step-by-step progress and a final record summary (e.g., counts of restored accounts, transactions, budgets).
- **Confirmed Decision 2 (Merge Strategy)**: Cloud-priority merge—all remote cloud records take precedence and are cleanly restored into the local Room database, while any unsynced local records are retained and synced.
- **Confirmed Decision 3 (Triggering)**: Automatic detection and prompt immediately after login when local database is empty or new, paired with a permanent manual "Restore from Cloud" button in Settings.
- **Critical Fix (Login Decoupling)**: Remove heavy Firestore queries and data seeding from the login screen path so login responds in milliseconds without blocking or freezing.

---

## 1. Overview & Core Concept

### What It Does
When a user clears app data or reinstalls the app and logs in with their existing account (Google Sign-In or Email), the application detects existing cloud data and safely restores all accounts, balances, transactions, categories, budgets, debts, and preferences through a smooth, interactive post-login restoration dialog. Default empty starter accounts will never overwrite or shadow cloud records.

### Root Cause of Previous Behavior
1. **Login Critical Path Block**: `ensureUserDataSeeded` was running synchronously during `signInWithGoogle` and `signInWithEmail`, causing the login button to hang on Firestore network timeouts.
2. **Premature Default Seeding**: When network calls timed out during login, the system assumed the user was brand new and inserted 4 empty default accounts (`Cash`, `bKash`, `Nagad`, `Bank`) with 0 balances, then immediately uploaded those empty defaults to Firestore, risking data loss or state corruption.
3. **ID Conflict on Restore**: Room auto-generated primary keys for the default accounts conflicted with existing remote cloud account IDs, causing existing transactions to become orphaned or linked to incorrect accounts.

---

## 2. User Experience & Visual Design

### Key User Flows

#### Flow 1: Fast Login
1. User taps "Continue with Google" or logs in with email.
2. Authentication succeeds immediately in under 500ms.
3. The app smoothly transitions to the main dashboard without waiting for Firestore queries.

#### Flow 2: Post-Login Automatic Restoration
1. On landing on the dashboard/main screen, the app checks if cloud data exists for this account.
2. If cloud data is detected and local data is empty (post-clear-data state):
   - A modal dialog appears: **"Restoring Your Data"** / **"তথ্য পুনরুদ্ধার করা হচ্ছে"**.
   - An animated botanical/teal progress ring appears with step status:
     - *Checking cloud backup...*
     - *Restoring Accounts & Balances...*
     - *Restoring Categories & Transactions...*
     - *Restoring Budgets, Debts & Goals...*
     - *Syncing Settings...*
   - Once completed, the dialog transforms into a **Success Summary**:
     - *✓ 4 Accounts restored*
     - *✓ 142 Transactions restored*
     - *✓ 8 Categories & Budgets restored*
     - Large primary button: **"Open Dashboard"** / **"ড্যাশবোর্ডে প্রবেশ করুন"**.
3. All dashboard balances, transactions, and categories refresh live.

#### Flow 3: Manual Restore via Settings
1. User navigates to Settings > Cloud Backup & Sync.
2. Taps **"Restore from Cloud"** / **"ক্লাউড থেকে পুনরুদ্ধার করুন"**.
3. A confirmation dialog appears with account details and last sync time.
4. Tapping **"Start Restore"** launches the interactive restore dialog and merges all cloud records.

---

## 3. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                        Auth Screen                          │
│  - Instant Google Sign-In & Email Sign-In (no sync blocking)│
└──────────────────────────────┬──────────────────────────────┘
                               │ Authenticated User
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity / UI                      │
│  - Observes `restoreUiState` from HisabViewModel            │
│  - Shows `CloudRestoreDialog` (Progress & Record Summary)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       HisabViewModel                        │
│  - checkForCloudDataAndRestore(userId)                      │
│  - restoreFromCloud(userId, isManual)                       │
│  - StateFlow<RestoreUiState> (Idle, Detecting, Progress,    │
│                               Success, Error)               │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌───────────────────────┐             ┌───────────────────────┐
│  FirestoreRepository  │             │     AppDatabase       │
│  - fetchCloudSummary  │             │     (Room DAOs)       │
│  - restoreAllData()   │──(Insert)──▶│  - AccountDao         │
│  (Cloud-Priority)     │             │  - TransactionDao     │
└───────────────────────┘             │  - CategoryDao        │
                                      │  - BudgetDao, etc.    │
                                      └───────────────────────┘
```

### Data Model & State Transitions

#### `RestoreUiState`
```kotlin
sealed class RestoreUiState {
    object Idle : RestoreUiState()
    object Checking : RestoreUiState()
    data class Restoring(
        val currentStep: String,
        val progressPercent: Float
    ) : RestoreUiState()
    data class Completed(
        val accountsCount: Int,
        val transactionsCount: Int,
        val categoriesCount: Int,
        val othersCount: Int
    ) : RestoreUiState()
    data class Error(val message: String) : RestoreUiState()
}
```

### Key Engineering Steps
1. **Decouple Login from Seeding**:
   - In `AuthRepository.signInWithGoogle` and `signInWithEmail`, remove synchronous blocking `ensureUserDataSeeded`.
   - Complete login and emit `currentUser` immediately.
2. **Refactor Seeding Guard**:
   - `ensureUserDataSeeded` will ONLY create default starter accounts if the user is verified to have ZERO cloud data and ZERO local data after a fast check.
3. **Cloud Priority Merge in `FirestoreRepository`**:
   - Ensure accounts, categories, and transactions are restored using their explicit cloud IDs (`id`) via Room `OnConflictStrategy.REPLACE`.
   - Update foreign keys safely to ensure no orphaned transactions.
   - Accurately return counts of restored records for user visibility.
4. **Interactive Restoration Dialog (`CloudRestoreDialog.kt`)**:
   - Build a Material 3 dialog adhering to design guidelines (teal/emerald botanical accents, rounded corners, spring transitions, Bengali + English localization).
   - Display real-time progress steps and clear record count badges.
5. **Settings Screen Integration**:
   - Add a "Restore from Cloud" action under Settings with sync metadata.
