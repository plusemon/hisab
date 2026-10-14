# Complete Cloud Sync & User Profile Restoration Architecture

A comprehensive end-to-end cloud synchronization and restore system for **Hisab**, ensuring zero data loss across devices, reinstalls, and app restarts. All user financial records (Transactions, Accounts, Categories, Budgets, Savings Goals, Debts/Loans & Repayments, Shop Credit Purchases/Payments, Recurring Rules) and personalization preferences (Language, Numerals, Currency, Theme, Security PIN) are backed up to Firebase Firestore and seamlessly restored upon login.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned during Phase 1 clarification:

- **Confirmed Decision 1 (Sync Scope)**: Full synchronization of **all 10 entity collections and user settings**:
  - `transactions`, `accounts`, `categories`
  - `budgets`, `savings_goals`
  - `loans_debts`, `loan_repayments`
  - `vendors`, `shop_credit_purchases`, `shop_credit_payments`
  - `recurring_rules`
  - `user_settings` (Language, Numeral System, Default Currency, Currency Symbol, Theme, PIN lock status)
- **Confirmed Decision 2 (Login Restoration Strategy)**: Cloud-priority clean restoration on fresh login:
  - When a user signs in, the app queries Firestore first.
  - If remote cloud data exists, it downloads and writes directly to Room DB without polluting the account with hardcoded template accounts or duplicate default categories.
  - If it is genuinely a brand-new user with zero cloud records, standard localized Bangladeshi starter accounts and categories are initialized and saved to cloud.
- **Confirmed Decision 3 (Sync Triggers & Frequency)**:
  - Instant background write-through on every create, edit, archive, and delete event.
  - Full bidirectional sync reconciliation on login and app launch.
  - Manual on-demand sync button in the App Top Bar / Settings / More screens with tactile sync indicators.
  - Offline-first resilience: local Room DB operations always succeed immediately; when online, mutations sync to Firestore without blocking the UI.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms Hisab into a robust offline-first, cloud-backed personal finance application. Any action performed on one device or session is persisted to Firebase Firestore under the authenticated user's isolated subcollections (`users/{userId}/*`). Upon signing in on any new device or reinstalling the app, all existing data—from account balances and transaction histories to custom category icons, monthly budget targets, debt ledgers, and Bengali/English language preferences—is completely restored.
- **Target Audience**: Users managing personal, household, and small business finances in Bangladesh and worldwide who require complete peace of mind that their financial records and personalized app settings are permanently safe.
- **Key Value**: Eliminates data loss vulnerabilities, ensures seamless multi-device continuity, and restores user-configured preferences instantly upon Google authentication.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Sign-In & Automatic Restoration Flow**:
   - User signs in with Google on a clean install.
   - Screen displays a smooth transition indicator: *"Restoring your financial data from cloud..."* / *"আপনার ক্লাউড ডাটা রিস্টোর হচ্ছে..."*.
   - All cloud accounts, categories, transactions, budgets, debts, and user preferences are pulled down.
   - UI immediately reflects the user's saved language, numeral format (বাংলা vs English digits), theme, and custom accounts.
2. **Instant Mutation Feedback**:
   - Whenever a user adds/edits an expense, creates a budget, or updates their currency/PIN settings, the `RealTimeStatusBadge` shows a subtle saving indicator and transitions to "Saved" (`সংরক্ষিত`) within milliseconds.
   - If offline, the badge cleanly indicates "Offline" with automatic sync upon reconnecting.
3. **Manual Sync Action**:
   - Tapping the sync icon in `HisabTopBar` or Settings displays a rotating refresh indicator, performs delta sync, and pops a localized toast with the exact number of synchronized items.

### Visual Styling & Status Tokens
- **Sync Status Badge**:
  - `Syncing`: Subtle rotating sync glyph with primary container background.
  - `Synced`: Delicate emerald check badge (`#10B981`) that fades gently after 2.5s.
  - `Offline`: Slate gray indicator (`#64748B`) with an offline cloud icon.
- **Progress & Restoration**:
  - Unobtrusive linear progress indicator during initial data hydration.
  - No flickering or empty-state flashes; Room database reactive flows update dynamically as Firestore snapshots deserialize.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Cloud-First Seed Gate on Authentication**:
  - *Chosen Approach*: On user login, check Firestore first before inserting default seed accounts. If remote accounts or transactions exist, skip default seeding and restore remote records.
  - *Why*: Previously, `ensureUserDataSeeded` created Cash, bKash, and default categories with auto-generated IDs before Firestore sync completed, causing duplicate accounts and conflicting IDs.
- **Decision 2: Comprehensive `FirestoreRepository` Methods for All Models**:
  - *Chosen Approach*: Expand `FirestoreRepository` and `FirestoreRepositoryImpl` to cover every Room entity with dedicated save/delete/sync handlers, plus a master `syncAllWithRoom(userId)` method that coordinates all subcollections.
  - *Why*: Provides clean separation of concerns, complete coverage of user preferences and all financial modules, and predictable serialization.
- **Decision 3: Settings & Preferences Cloud Sync**:
  - *Chosen Approach*: Store `UserSettings` at `/users/{userId}/settings/preferences`. Sync changes from both local `SharedPreferences` and Room `UserSettings` entity to Firestore.
  - *Why*: Allows UI configuration (Bangla numbers, dark mode, default currency) to follow the user across devices without requiring manual reconfiguration.
- **Decision 4: Hardened Entity Validation in Security Rules**:
  - *Chosen Approach*: Expand `firestore.rules` and `firebase-blueprint.json` to define schema validation for all subcollections under `/users/{userId}`.
  - *Why*: Enforces zero-trust database security while maintaining clean multi-tenant isolation per user UID.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          User Interface                                │
│   (Dashboard, Transactions, Accounts, Budgets, Debts, Settings)        │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         HisabViewModel                                 │
│  - Reactive StateFlows (settings, accounts, transactions, budgets)     │
│  - AppSyncStatus tracking (Synced, Syncing, Offline)                   │
│  - Trigger sync on mutation & on network reconnection                  │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │ (Immediate Room DB)            │ (Background Cloud)
                    ▼                                ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│        Room Database (Local)         │  │     FirestoreRepository      │
│  - UserAccount, Category             │  │  - Save / Delete by Entity   │
│  - TransactionRecord, Budget         │  │  - Comprehensive syncAll     │
│  - SavingsGoal, Contact, LoanDebt    │  │  - Realtime snapshots        │
│  - Vendor, ShopCredit, RecurringRule │  └──────────────┬───────────────┘
│  - UserSettings                      │                 │
└──────────────────────────────────────┘                 ▼
                                          ┌──────────────────────────────┐
                                          │      Firebase Firestore      │
                                          │  /users/{uid}/accounts       │
                                          │  /users/{uid}/categories     │
                                          │  /users/{uid}/transactions   │
                                          │  /users/{uid}/budgets        │
                                          │  /users/{uid}/savings_goals  │
                                          │  /users/{uid}/loans_debts    │
                                          │  /users/{uid}/repayments     │
                                          │  /users/{uid}/vendors        │
                                          │  /users/{uid}/shop_credit    │
                                          │  /users/{uid}/recurring_rules│
                                          │  /users/{uid}/settings/pref  │
                                          └──────────────────────────────┘
```

### Entity Collection Mapping

| Entity | Room Table | Firestore Path | Key Sync Fields |
|---|---|---|---|
| `UserAccount` | `user_accounts` | `users/{uid}/accounts/{id}` | id, name, type, currency, balance, color, icon, archived |
| `Category` | `categories` | `users/{uid}/categories/{id}` | id, nameBn, nameEn, type, icon, color, isDefault, isArchived |
| `TransactionRecord` | `transactions` | `users/{uid}/transactions/{id}` | id, accountId, categoryId, amount, type, date, note, fee, exchangeRate |
| `Budget` | `budgets` | `users/{uid}/budgets/{id}` | id, categoryId, amount, monthYear, alertThreshold |
| `SavingsGoal` | `savings_goals` | `users/{uid}/savings_goals/{id}` | id, name, targetAmount, currentSavedAmount, targetDate, isCompleted |
| `Contact` | `contacts` | `users/{uid}/contacts/{id}` | id, name, phone, notes |
| `LoanDebt` | `loans_debts` | `users/{uid}/loans_debts/{id}` | id, contactId, personName, phone, amount, paidAmount, type, dueDate, settled |
| `LoanRepayment` | `loan_repayments` | `users/{uid}/loan_repayments/{id}` | id, loanId, accountId, amount, dateTimestamp, note |
| `Vendor` | `vendors` | `users/{uid}/vendors/{id}` | id, name, phone, locationNote, categoryTag, isArchived |
| `ShopCreditPurchase` | `shop_credit_purchases` | `users/{uid}/shop_credit_purchases/{id}` | id, vendorId, amount, paidAmount, dueDate, isSettled, note |
| `ShopCreditPayment` | `shop_credit_payments` | `users/{uid}/shop_credit_payments/{id}` | id, vendorId, accountId, amount, note, transactionId |
| `RecurringRule` | `recurring_rules` | `users/{uid}/recurring_rules/{id}` | id, accountId, categoryId, amount, type, frequency, startDate, isPaused |
| `UserSettings` | `user_settings` | `users/{uid}/settings/preferences` | language, numeralSystem, defaultCurrency, currencySymbol, hideBalances, isDarkMode, pinEnabled |

---

## Planned Implementation Steps

1. **`firebase-blueprint.json` & `firestore.rules` Update**:
   - Add schemas and validation rules for all 10 subcollections and the settings document.
2. **`FirestoreRepository` Overhaul**:
   - Add save/delete methods for all entities (budgets, savings goals, debts, repayments, vendors, shop credits, recurring rules, settings).
   - Implement complete two-way synchronization in `syncAllWithRoom(userId)`.
   - Implement clean `restoreCloudUserData(userId)` method that downloads and populates Room.
3. **`AuthRepository` Seed Alignment**:
   - Update `ensureUserDataSeeded` and login handlers so they check whether remote cloud data exists before injecting blank starter accounts/categories.
4. **`HisabViewModel` Mutation Hooks**:
   - Hook instant background cloud persistence into every mutation method:
     - `addAccount`, `updateAccount`, `deleteAccount`, `archiveAccount`
     - `addCategory`, `updateCategory`, `deleteCategory`
     - `setBudget`, `deleteBudget`
     - `addSavingsGoal`, `updateSavingsGoal`, `deleteSavingsGoal`, `adjustGoalSavings`
     - `addLoanDebt`, `recordLoanRepayment`, `deleteLoanDebt`
     - `addVendor`, `updateVendor`, `addShopCreditPurchase`, `settleShopCredit`
     - `addRecurringRule`, `setRecurringPaused`, `deleteRecurringRule`
     - `updateLanguage`, `updateNumeralSystem`, `updateCurrency`, `updateTheme`, `setPinCode`
5. **Compilation & Verification**:
   - Compile with `compile_applet` to ensure type-safe integration and zero build regressions.
