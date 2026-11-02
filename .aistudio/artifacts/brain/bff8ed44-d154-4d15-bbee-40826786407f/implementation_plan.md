# High-Performance Cloud Sync Optimization & Local Preference Isolation

This plan optimizes the Hisab synchronization engine by replacing sequential network queries with parallel coroutines and Firestore `WriteBatch` operations, while isolating all UI preferences (language, dark mode, numeral system) to remain strictly device-local without triggering unnecessary cloud sync or UI status badge changes.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed in Phase 1 and govern the architectural strategy:

- **Confirmed Decision 1 (Device-Local Settings)**: All UI preferences (Language switch, Dark Mode toggle, System Theme toggle, and Numeral System) will remain strictly device-local (persisted in `SharedPreferences` and local `AppDatabase` Room table). Changing these settings will never trigger Firestore network calls, `markSaving()` spinners, or cloud synchronization.
- **Confirmed Decision 2 (Sync Acceleration Pipeline)**: Full synchronization (`syncAllWithRoom`) will transition from a 12-step sequential waterfall to a high-speed 2-phase parallel pipeline utilizing Kotlin Coroutines (`async`/`awaitAll`) and Firestore `WriteBatch` (up to 500 records per single batch network round-trip), dramatically slashing synchronization latency from 15–25 seconds down to 1–2 seconds.
- **Confirmed Decision 3 (Responsive Status Feedback)**: The artificial delay in `markSaved()` will be refined from 2,200ms to a crisp 800ms so visual confirmation is snappy and does not give the false perception of prolonged background processing.

---

## 1. Overview & Core Concept

### What It Does
- **Local Preference Decoupling**: Language switching (বাংলা ↔ English) and Dark Mode toggling will apply immediately in-memory and on-disk without dispatching network operations or changing the top app sync status.
- **High-Throughput Parallel Synchronization**: Restructures `syncAllWithRoom` to fetch subcollections concurrently while maintaining strict relational foreign-key integrity (Accounts and Categories synced first, followed by Transactions, Budgets, Debts, Goals, Contacts, and Shop Credits in parallel).
- **Atomic Bulk Uploads (`WriteBatch`)**: When local records need to be pushed to Firestore, writes are combined into atomic batches instead of executing isolated sequential HTTP/gRPC requests in loops.
- **Direct Authenticated UID Resolution**: Bypasses slow fallback email queries when the user is already authenticated with their primary Firebase UID.

### Target Audience & Persona
- Daily personal finance and small business users in Bangladesh who expect zero UI lag when customizing the app's look and feel, and who require fast, reliable ledger synchronization across devices even on mobile data connections (2G/3G/4G).

### Key Value
- **Zero-Friction Customization**: Switching themes or languages happens in <16ms with zero network traffic.
- **Fast Cloud Reconciliation**: Data sync is up to 5x faster, consuming less mobile battery and data bandwidth.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Instant Theme & Language Toggle**:
   - User navigates to Settings or taps the language / theme toggle.
   - Screen theme (Light/Dark) or UI locale (বাংলা/English) toggles immediately with fluid animated transitions.
   - The top header `RealTimeStatusBadge` remains undisturbed in its normal synchronized state without displaying "সেভ হচ্ছে..." ("Saving...").

2. **Snappy Manual or Background Synchronization**:
   - User opens the app, reconnects to Wi-Fi/Mobile Data, or taps the Sync badge in the header.
   - The status badge transitions to `Syncing` (rotating emerald indicator).
   - All collections synchronize concurrently in 1–2 seconds.
   - The badge transitions to `Synced` with a subtle checkmark and auto-dismisses smoothly.

3. **Transaction Creation & Real-Time Upload**:
   - Creating or editing an expense, income, or transfer immediately updates local Room balances and the transaction feed.
   - The transaction is uploaded asynchronously in the background.
   - The save confirmation pill provides an 800ms visual cue before returning to steady-state `Synced`.

### Visual Identity & Theme
- **Color Palette**: Retains the rich Emerald (`#0F766E`) and Jade (`#10B981`) primary palette with clean M3 container surfaces and dark-slate backgrounds (`#0F172A`) for dark mode.
- **Badge Transitions**: Seamless `AnimatedContent` crossfades between Synced, Syncing, and Saved states without abrupt layout shifts in the `DashboardHeader`.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Device-Local UI Preferences Isolation
- **Chosen Approach**: Remove Firestore network dispatch (`firestoreRepository.saveUserSettings(...)`) and `markSaving()` from `updateSettings(...)`. Settings are saved exclusively to Android `SharedPreferences` and local SQLite (`Room`). Remove `syncUserSettings` from `syncAllWithRoom`.
- **Why**: Language, numeral format, and dark mode are personal device preferences (e.g. a user might prefer dark mode on a tablet and light mode on a phone). Transmitting them across the wire slows down the UI, triggers unnecessary Firestore read/write operations, and pollutes the sync state badge.
- **Alternatives Considered**: Debouncing network requests (still causes unwanted cloud writes and status changes).

### Decision 2: 2-Phase Directed Concurrency in `syncAllWithRoom`
- **Chosen Approach**:
  - **Phase 1 (Parent Dependencies)**: Run `syncAccounts`, `syncCategories`, and `syncVendors` in parallel using `async { ... }`. Wait for all 3 to resolve so foreign-key rows exist locally in Room.
  - **Phase 2 (Dependent Ledger Tables)**: Run `syncTransactions`, `syncBudgets`, `syncSavingsGoals`, `syncContacts`, `syncLoansDebts`, `syncLoanRepayments`, `syncShopCredit`, and `syncRecurringRules` in parallel via `awaitAll()`.
- **Why**: Maintains Room foreign-key integrity (transactions cannot be inserted before accounts/categories exist) while collapsing 12 sequential network round-trips into just 2 concurrent phases.
- **Alternatives Considered**: Completely unconstrained parallel sync (causes SQLite foreign-key constraint violations); keeping sequential sync (too slow).

### Decision 3: Firestore `WriteBatch` for Bulk Local-to-Cloud Uploads
- **Chosen Approach**: In all sync methods (transactions, accounts, budgets, etc.), accumulate un-synced local records into `firestore.batch()` and commit them in single atomic roundtrips (chunked at 450 items to stay safely within Firestore's 500-write limit).
- **Why**: Reduces 50 individual HTTP/gRPC requests down to a single network call, decreasing network latency from 15,000ms to ~300ms.
- **Alternatives Considered**: Single document writes with `awaitTask()` (current approach, causes massive lag).

### Decision 4: Priority UID Direct Lookup
- **Chosen Approach**: If `FirebaseAuth.getInstance().currentUser?.uid` is non-empty, use that UID directly for collection references (`users/{uid}/...`). Only query candidate email indexes if the authenticated user has zero documents in their primary path.
- **Why**: Eliminates 5–8 second timeout penalties caused by querying non-existent or restricted collection paths.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│   ┌───────────────────────────┐      ┌─────────────────────────────┐   │
│   │ SettingsScreen / Switches │      │ Dashboard / TopAppBar Badge │   │
│   │  (Language / Dark Mode)   │      │ (Synced / Syncing / Saved)  │   │
│   └─────────────┬─────────────┘      └──────────────▲──────────────┘   │
└─────────────────┼───────────────────────────────────┼──────────────────┘
                  │ Local Only                        │ Reactive Status
                  │ (No Network Call)                 │
┌─────────────────▼───────────────────────────────────┴──────────────────┐
│                             HisabViewModel                             │
│   ┌───────────────────────────┐      ┌─────────────────────────────┐   │
│   │   updateSettings()        │      │    syncWithFirestore()      │   │
│   │ • SharedPreferences Write │      │ • AppSyncStatus.Syncing     │   │
│   │ • Room SQLite Write       │      │ • 2-Phase Concurrency       │   │
│   │ • NO markSaving / No Cloud│      │ • 800ms Snappy markSaved()  │   │
│   └───────────────────────────┘      └──────────────┬──────────────┘   │
└─────────────────────────────────────────────────────┼──────────────────┘
                                                      │
┌─────────────────────────────────────────────────────▼──────────────────┐
│                   FirestoreRepositoryImpl Engine                       │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Phase 1 Concurrency: Parent Entities                           │   │
│   │ ┌────────────────┐   ┌────────────────┐   ┌────────────────┐   │   │
│   │ │  syncAccounts  │   │ syncCategories │   │  syncVendors   │   │   │
│   │ └────────────────┘   └────────────────┘   └────────────────┘   │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
│                                    ▼                                   │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Phase 2 Concurrency: Dependent Data (with WriteBatch)          │   │
│   │ ┌────────────────┐   ┌────────────────┐   ┌────────────────┐   │   │
│   │ │syncTransactions│   │  syncBudgets   │   │syncSavingsGoals│   │   │
│   │ ├────────────────┤   ├────────────────┤   ├────────────────┤   │   │
│   │ │  syncContacts  │   │ syncLoansDebts │   │ syncRepayments │   │   │
│   │ ├────────────────┤   ├────────────────┤   ├────────────────┤   │   │
│   │ │ syncShopCredit │   │syncRecurringRul│   │ (Settings OMIT)│   │   │
│   │ └────────────────┘   └────────────────┘   └────────────────┘   │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           ▼                                                   ▼
┌──────────────────────┐                           ┌─────────────────────┐
│ Room Database (Disk) │                           │ Firebase Cloud      │
│ • Local First        │                           │ • Firestore DB      │
│ • Zero UI Latency    │                           │ • WriteBatch Upload │
└──────────────────────┘                           └─────────────────────┘
```

### Data Model & State Boundaries

| Data Domain | Storage Mechanism | Cloud Sync Behavior | Trigger / State |
| :--- | :--- | :--- | :--- |
| **Language (bn/en)** | `SharedPreferences` + Room | **Never Synced** (Strictly Local) | Immediate UI Recomposition; no sync badge |
| **Dark / System Theme** | `SharedPreferences` + Room | **Never Synced** (Strictly Local) | Immediate UI Recomposition; no sync badge |
| **Numeral System** | `SharedPreferences` + Room | **Never Synced** (Strictly Local) | Immediate UI Recomposition; no sync badge |
| **Transactions** | Room SQLite $\leftrightarrow$ Firestore | **Instant Async + Full Bi-directional** | Immediate async upload on add/edit/delete; parallel bulk sync on refresh |
| **Accounts & Balances**| Room SQLite $\leftrightarrow$ Firestore | **Phase 1 Parallel Bi-directional** | Guaranteed precedence before dependent transaction mapping |
| **Categories & Vendors**| Room SQLite $\leftrightarrow$ Firestore | **Phase 1 Parallel Bi-directional** | Guaranteed precedence before dependent transaction mapping |
| **Budgets & Goals** | Room SQLite $\leftrightarrow$ Firestore | **Phase 2 Parallel Bi-directional** | WriteBatch atomic upload for un-synced items |
| **Debts & Shop Credit**| Room SQLite $\leftrightarrow$ Firestore | **Phase 2 Parallel Bi-directional** | WriteBatch atomic upload for un-synced items |

### Interactive Component & State Mapping

1. **`HisabViewModel.updateSettings`**:
   - Update `appPrefs` synchronously.
   - Update `_settings.value` StateFlow.
   - Persist to Room `hisabRepository.updateSettings(target)` in IO coroutine.
   - **Do NOT** call `markSaving()`, `markSaved()`, or `firestoreRepository.saveUserSettings(...)`.

2. **`FirestoreRepositoryImpl.syncAllWithRoom`**:
   - Resolve primary UID directly (`FirebaseAuth.getInstance().currentUser?.uid`).
   - Run Phase 1 coroutine block (`syncAccounts`, `syncCategories`, `syncVendors`).
   - Run Phase 2 coroutine block (`syncTransactions`, `syncBudgets`, `syncSavingsGoals`, `syncContacts`, `syncLoansDebts`, `syncLoanRepayments`, `syncShopCredit`, `syncRecurringRules`).
   - Utilize `firestore.batch()` for any local items missing on remote.
   - Return total synchronized item count.

3. **`HisabViewModel.markSaved`**:
   - Reduce delay from 2200ms to 800ms before returning to `AppSyncStatus.Synced()`.
