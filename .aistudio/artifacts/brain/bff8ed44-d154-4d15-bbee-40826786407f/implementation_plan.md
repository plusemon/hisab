# Mobile-Responsive App Header Redesign

Redesign the top navigation header into a sleek, single-row mobile-responsive layout that eliminates text truncation and visual crowding across all screen widths and languages (Bengali and English). By unifying network connectivity and cloud sync states into a single adaptive status pill and moving it out of the greeting text line, the user's greeting and display name always have generous breathing room.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design choices have been confirmed from your preferences and will guide the implementation:

- **Confirmed Decision 1 — Single Compact Header Row**: Keep the header as a single compact horizontal bar with balanced proportions, adaptive spacing (`BoxWithConstraints`), and compact action controls so the greeting and user name never clip or collide with right-side buttons.
- **Confirmed Decision 2 — Unified Connectivity & Sync Pill**: Merge the separate Wi-Fi network icon and cloud sync badge into one cohesive, interactive status pill in the right-hand action cluster. Tapping the pill triggers manual cloud sync and shows a brief status tooltip/feedback.

---

## 1. Overview & Core Concept

- **What It Does**: Replaces the crowded 4-widget header arrangement (where the sync status badge expanded next to the greeting text and collided with three large right-side buttons) with a responsive single-row header.
- **Target Audience / Persona**: Mobile users tracking daily finances in Bengali or English on compact handheld screens (320dp–412dp width) as well as wider devices.
- **Key Value**:
  - **Zero Text Clipping**: Removing the expanding sync badge from the greeting line ensures `"শুভ অপরাহ্ন"` / `"Good Afternoon"` and the user's name render cleanly without awkward mid-word truncation.
  - **Unified Status Clarity**: Users see a single, unambiguous indicator for offline mode, active syncing/saving, sync errors, or synced state—instead of two competing status badges (`RealTimeStatusBadge` + `NetworkStatusIcon`).
  - **Responsive Adaptation**: Automatically adapts padding, pill density, and typography based on available screen width (`BoxWithConstraints`).

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Normal Online & Synced State**: The left side displays the user's avatar, time-of-day greeting, and full display name. The right side displays three compact, unified controls: the **Unified Status Pill** (showing a subtle emerald cloud-check/online badge), the **Language Switcher Pill** (`বাং` / `EN`), and the **Theme Switcher Button**.
  2. **Active Syncing / Saving State**: When a transaction is saved or synced, the Unified Status Pill smoothly animates to show a compact progress spinner and a concise status label (or icon-only on ultra-narrow screens `< 360dp`), without pushing or clipping the user's greeting text.
  3. **Offline State**: When network connectivity drops, the Unified Status Pill immediately reflects offline status with an amber/red offline indicator so the user knows changes are stored locally until connectivity returns.
  4. **Interactive Actions**:
     - Tapping the **Profile section** (Avatar + Greeting + Name) navigates to the user profile/settings screen.
     - Tapping the **Unified Status Pill** triggers an immediate sync (`onSyncClick`).
     - Tapping the **Language Pill** toggles between Bengali (`বাং`) and English (`EN`).
     - Tapping the **Theme Button** opens the dropdown menu for System Default, Light Mode, and Dark Mode.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Clean, modern fintech minimalism matching Hisab's signature Emerald & Slate flat design system.
  - *Color Palette & Mood*:
    - Surface background using `MaterialTheme.colorScheme.surface` with a subtle `outlineVariant` bottom divider.
    - Status accents: Emerald (`#10B981` / `#047857`) for Online & Synced, Indigo (`#4338CA`) for Syncing, Amber (`#B45309`) for Saving/Pending, and Crimson (`#DC2626`) for Offline or Sync Error—with dark-mode-aware container tints.
  - *Typography & Hierarchy*:
    - Greeting label: `labelSmall` (`11.5.sp`), muted `onSurfaceVariant`, single line with ellipsis.
    - User display name: `titleMedium` (`15.sp`, `FontWeight.SemiBold`), `onSurface`, single line with ellipsis.
    - Action pills: `labelMedium` (`12.5.sp`, `FontWeight.Bold`) with crisp 16dp–18dp icons.
  - *Component Styling & Layout*:
    - Consistent `36.dp` control height across the Unified Status Pill, Language Pill, and Theme Button with `12.dp` rounded corners and `6.dp` horizontal spacing.
    - Responsive breakpoint using `BoxWithConstraints`: on compact widths (`< 380.dp`), the Unified Status Pill shows a compact icon/spinner (expanding with short text only on error or tap), while on wider screens (`>= 380.dp`) it comfortably displays both the status icon and short label.

- **Interactive Feedback & Motion**:
  - Smooth `AnimatedContent` and `animateColorAsState` transitions when switching between Online/Synced, Syncing, Saving, and Offline states.
  - Proper Material ripple feedback on all interactive pills and avatar area.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Relocate Sync Status out of the Greeting Row into a Unified Status Pill**
  - *Chosen Approach*: Combine `isNetworkConnected` and `AppSyncStatus` into one unified component placed in the right-side action cluster.
  - *Why*: Previously, `RealTimeStatusBadge` sat inside the left-hand greeting `Row`, competing with the greeting text for horizontal space while a separate `NetworkStatusIcon` sat on the right. Combining them frees up ~70dp of width for the user's greeting and name while eliminating redundant status icons.
  - *Alternatives Considered*: Keeping two separate icons on the right (rejected because 4 separate buttons on the right still crowd narrow screens).

- **Decision 2: Adaptive Width Responsiveness via `BoxWithConstraints`**
  - *Chosen Approach*: Measure the header's available width dynamically and scale spacing, avatar size (`36.dp` vs `40.dp`), and status label visibility according to available horizontal space.
  - *Why*: Ensures small phones (e.g., 320dp–360dp width or large system font scales) never experience clipped text, while standard and large phones/tablets enjoy rich status labels.
  - *Alternatives Considered*: Fixed padding and fixed pill widths (rejected as it fails when Bengali text or display names are long).

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       Responsive DashboardHeader                        │
│  (BoxWithConstraints: adapts spacing & pill density by maxWidth)        │
├───────────────────────────────────┬─────────────────────────────────────┤
│  Left: Profile Identity Cluster   │  Right: Compact Action Cluster      │
│  (weight = 1f, clickable)         │  (spacedBy = 6.dp, height = 36.dp)  │
│  ┌────────┐  ┌─────────────────┐  │  ┌───────────┐ ┌────────┐ ┌──────┐  │
│  │ Avatar │  │ Greeting Text   │  │  │  Unified  │ │  Lang  │ │Theme │  │
│  │ 36-40dp│  │ Display Name    │  │  │Status Pill│ │  Pill  │ │ Menu │  │
│  └────────┘  └─────────────────┘  │  └─────┬─────┘ └───┬────┘ └──┬───┘  │
└───────────────────────────────────┴────────┼───────────┼─────────┼──────┘
                                             │           │         │
                                             ▼           ▼         ▼
                                        onSyncClick  onToggle  onSetTheme
                                                     Language     Mode
```

- **Data Model & State**:
  - **Unified Status Resolution**:
    1. If `!isNetworkConnected`: Display **Offline** state (`WifiOff` icon, offline tint, label `"অফলাইন"` / `"Offline"`).
    2. Else if `syncStatus is Saving`: Display **Saving** state (spinner + `"সংরক্ষণ..."` / `"Saving"`).
    3. Else if `syncStatus is Syncing` or `CheckingUpdates`: Display **Syncing** state (spinner + `"সিঙ্ক..."` / `"Syncing"`).
    4. Else if `syncStatus is Error`: Display **Sync Error** state (`ErrorOutline` icon + `"ত্রুটি"` / `"Error"`).
    5. Else (`Synced`, `Saved`, `Idle`): Display **Online & Synced** state (`CloudDone` icon, emerald tint, compact icon-only pill by default on handheld screens so the header stays ultra-clean, or tap-to-sync).

- **Interactive Component & State Mapping**:
  - **Profile Cluster**: Invokes `onProfileClick()` with rounded clip ripple.
  - **Unified Status Pill (`realtime_status_badge` / `network_status_icon`)**: Preserves test tags and click handler `onSyncClick()` so manual sync and existing automated tests continue to work seamlessly.
  - **Language Toggle Pill (`header_language_btn`)**: Compact pill with `Translate` icon and `"বাং"` / `"EN"` label invoking `onToggleLanguage()`.
  - **Theme Switcher (`header_theme_btn`)**: Compact square-rounded icon button opening the Material 3 `DropdownMenu` with System Default, Light Mode, and Dark Mode options.
  - **Detail Screen App Bar Consistency**: Keep `DetailTopAppBar` clean and aligned with the updated compact status indicator styling.
