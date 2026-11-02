# Blocking Loading Dialog for Account & Critical Operations

A resilient, elevated modal loading dialog designed to block user interaction and provide reassuring visual feedback during high-stakes account operations including Google Sign-In, sign-out, account deletion, and complete data resets.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification and will govern the implementation:

- **Confirmed Scope of Operations**: Auth sign-in (`signInWithGoogle`), sign-out (`signOut`), account deletion (`deleteAccount`), and complete data reset (`clearAllData`).
- **Confirmed Visual Direction**: Elevated M3 dialog card with smooth circular progress indicator, distinct botanical brand accents, bold contextual title, and bilingual descriptive status message.
- **Confirmed Safety & Error Handling**: 15-second timeout guard with automatic dismissal and error notification to prevent app freezes caused by network drops or stalled Firestore calls.

---

### 1. Overview & Core Concept

When users perform irreversible or state-altering account tasks—such as signing into Google, signing out of their profile, wiping all local and cloud data, or deleting their account permanently—the app currently performs operations asynchronously without locking the UI. This can permit accidental duplicate taps, rapid navigation presses, or visual confusion during asynchronous network transitions.

**Key Value & Capabilities Delivered:**
- **Full Touch Barrier**: Intercepts all touch and back events during processing via non-dismissible modal properties (`dismissOnBackPress = false`, `dismissOnClickOutside = false`).
- **Bilingual Contextual Feedback**: Displays localized Bangla and English titles and status descriptions tailored to the active operation.
- **Fail-Safe Timeout**: Incorporates a 15-second timeout guard using Kotlin coroutines that cleanly tears down the dialog and notifies the user with a descriptive toast/snackbar if a network operation stalls.

---

### 2. User Experience & Visual Design

#### Visual Identity & Styling
- **Aesthetic**: Polished Material 3 dialog styled with Hisab's botanical emerald palette (`0xFF1B5E20` / `0xFF004D40`), surface elevation (6.dp), rounded card geometry (24.dp corner radius), and subtle outline borders.
- **Progress & Iconography**: An indeterminate `CircularProgressIndicator` with custom stroke width (4.dp) paired with an operation icon (e.g., Google auth key, exit icon, or trash icon) and dynamic loading indicator.
- **Responsive & Safe**: Adapts seamlessly to Dark Mode, respects system insets, and enforces accessible text sizing (`sp`) and contrast ratios.

#### Key User Flows
1. **Google Sign-In Flow**:
   User selects an account from the Credential Manager picker -> The blocking loading dialog immediately appears with *"Signing into Google..."* / *"গুগল অ্যাকাউন্টে সাইন ইন হচ্ছে..."* -> On successful authentication and initial cloud handshake, the dialog automatically dismisses and transitions to the main dashboard.
2. **Sign-Out Flow**:
   User confirms Sign Out in Settings or More screen -> The blocking dialog appears with *"Signing out..."* / *"সাইন আউট হচ্ছে..."* -> Local caches and credentials are safely cleared -> App seamlessly resets to `AuthScreen`.
3. **Delete Account Flow**:
   User confirms permanent deletion -> Dialog appears with *"Deleting account and cloud data..."* / *"অ্যাকাউন্ট ও সমস্ত তথ্য মুছে ফেলা হচ্ছে..."* -> Firestore collections and Room records are wiped -> App returns to fresh unauthenticated state.
4. **Data Reset Flow**:
   User selects Clear All Data -> Dialog appears with *"Clearing all records..."* / *"সব হিসাব মুছে ফেলা হচ্ছে..."* -> Clean slate established without risk of interrupted database writes.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Global Overlay Placement at `MainActivity` Root**
  - *Chosen Approach*: Host the `BlockingLoadingDialog` at the root of `HisabTheme` in `MainActivity.kt` rather than duplicating dialog logic inside individual screens (`AuthScreen`, `SettingsScreen`, `MoreScreen`).
  - *Why*: Guarantees that whether an operation is initiated from the authentication gate, settings menu, or drawer, the UI is reliably protected with a single centralized source of truth.
  - *Alternatives Considered*: Per-screen loading overlays (higher maintenance, prone to visual glitches during route transitions).

- **Decision 2: Coroutine-Level 15s Timeout Wrapper**
  - *Chosen Approach*: Implement a structured `withBlockingLoading` extension in `HisabViewModel` wrapping execution inside `kotlinx.coroutines.withTimeout(15000L)`.
  - *Why*: Eliminates boilerplate `try/finally` blocks and guarantees dialog dismissal even if Firebase SDK calls hang due to dropped cellular or Wi-Fi connections.
  - *Alternatives Considered*: Manual timer or indefinite waiting (leaves user trapped if network connection hangs indefinitely).

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                      MainActivity                      │
│   (HisabTheme root, listens to blockingLoadingState)   │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
   ┌───────────────────┐       ┌────────────────────────┐
   │ Content Screens   │       │ BlockingLoadingDialog  │
   │ (Auth/Settings/   │       │ (Elevated Card, Modal, │
   │  More/Dashboard)  │       │  Bilingual Text, M3)   │
   └─────────┬─────────┘       └────────────────────────┘
             │ (User Action)             ▲
             ▼                           │ (Emits state)
   ┌─────────────────────────────────────┴──────────────┐
   │                  HisabViewModel                    │
   │  _blockingLoadingState: StateFlow<LoadingState?>   │
   │  withBlockingLoading(title, message, timeout = 15s)│
   └─────────────────────┬──────────────────────────────┘
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
┌──────────────────┐            ┌──────────────────┐
│  AuthRepository  │            │FirestoreRepository│
│ (Google / Creds) │            │ (Cloud Wipe/Sync)│
└──────────────────┘            └──────────────────┘
```

#### State & Model Mapping
- `BlockingLoadingState`:
  - `title`: Localized string resource / text (e.g., "Signing Out")
  - `message`: Localized descriptive hint (e.g., "Please wait while we secure your account session...")
  - `cancellable`: Boolean (false by default)
- `HisabViewModel`:
  - Exposes `blockingLoadingState: StateFlow<BlockingLoadingState?>`
  - Encapsulates `signInWithGoogle`, `signOut`, `deleteAccount`, and `clearAllData` with `withBlockingLoading`
- `BlockingLoadingDialog` Component:
  - Compose `Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false))`
  - Test tags: `blocking_loading_dialog`, `blocking_loading_spinner`, `blocking_loading_title`, `blocking_loading_message`
