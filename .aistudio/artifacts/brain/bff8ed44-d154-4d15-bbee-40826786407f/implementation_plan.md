# Two-Step Transaction Entry & Preview Confirmation

Convert the transaction creation flow from an immediate-save single form into an intuitive, polished **2-step workflow** across both the Quick Add Bottom Sheet and the full Add/Edit Transaction Screen. Step 1 collects transaction parameters with live input validation, and Step 2 presents a high-clarity **Confirmation & Preview screen** featuring a full receipt summary, balance forecast calculation, back/edit capability, and a final "Confirm & Save" action.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed based on your preferences:

- **Confirmed Decision 1 — Multi-Step Transition**: Step 1 transitions smoothly into Step 2 within the same bottom sheet and screen using animated layout transitions, avoiding disjointed dialog popups.
- **Confirmed Decision 2 — Full Receipt Summary**: Step 2 displays a complete transaction summary (Type badge, Amount, Category with icon badge, From/To Accounts, Date & Time, Note, Receipt preview, Fee/Exchange rate) alongside the interactive Balance Forecast Card (`TransactionBalancePreviewCard`).
- **Confirmed Decision 3 — Unified Support Across Forms**: Both the Quick Add Bottom Sheet (`AddTransactionBottomSheet.kt`) and the full Add/Edit Screen (`AddEditTransactionScreen.kt`) implement this two-step confirmation flow, with hardware/system back button handling to return safely from Step 2 to Step 1 without data loss.

---

## 1. Overview & Core Concept

- **What It Does**: Separates entering transaction details from the permanent save action. In Step 1, the user enters amount, account, category, date, notes, and receipt photo. Tapping "পরবর্তী" / "Next" verifies all required fields and transitions to Step 2. In Step 2, the user reviews a clear receipt card and the projected balance impact before tapping "নিশ্চিত করুন ও সংরক্ষণ" ("Confirm & Save").
- **Target Audience / Persona**: Users managing personal and shop finances who need confidence that every transaction is logged to the correct account and category with exact amounts before balances are altered.
- **Key Value**:
  - **Prevents Costly Accounting Errors**: Users visually verify the before-and-after account balance and category categorization before committing.
  - **Zero Data Loss on Revisions**: Users can easily tap "আগের ধাপ" / "Back" or swipe back from Step 2 to edit any field in Step 1 with all entered data completely intact.
  - **Clean, Uncluttered UI**: Step 1 focuses purely on data entry without premature balance cards crowding the viewport; Step 2 focuses purely on verification and commitment.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Step 1: Data Entry & Validation**:
     - User selects transaction type (Expense, Income, Transfer) and enters amount using quick-amount chips (+50, +100, +500, +1000, +5000) or manual keypad.
     - User selects Account, Category, Date/Time, Note, and optional Receipt image.
     - The bottom action row shows:
       - **বাতিল (Cancel)**: Closes the sheet/screen.
       - **পরবর্তী (Next)**: Prominent button with an arrow icon (`Icons.AutoMirrored.Filled.ArrowForward`).
     - Tapping **পরবর্তী**: Validates that amount > 0, account is valid, and (for transfers) source and destination accounts differ. If validation passes, smoothly transitions to Step 2.
  2. **Step 2: Preview & Confirmation**:
     - **Header**: Step progress pill (`ধাপ ২ এর ২` / `Step 2 of 2`) and transaction type badge (খরচ / আয় / ট্রান্সফার).
     - **Hero Amount Banner**: Large bold amount with currency symbol (`৳ ৫০০.০০`), styled with color-coding (Expense Red, Income Green, Transfer Blue).
     - **Full Receipt Card**:
       - Category with vibrant icon and color badge.
       - Source Account (and Target Account for Transfers).
       - Formatted Date & Time (in Bengali or English per user setting).
       - Note / Remarks (if entered).
       - Receipt photo thumbnail with tap-to-expand preview (if attached).
       - Transfer fee & exchange rate details (if applicable).
     - **Balance Forecast Card**: The rich `TransactionBalancePreviewCard` showing current balance, deducted/added amount, and the resulting future balance.
     - **Bottom Actions**:
       - **আগের ধাপ (Back)**: Secondary button with back arrow (`Icons.AutoMirrored.Filled.ArrowBack`) to return to Step 1 and tweak any values.
       - **নিশ্চিত করুন (Confirm & Save)**: Primary high-contrast button with checkmark icon (`Icons.Default.Check`) that executes the repository save, triggers sync, and dismisses the bottom sheet / screen with positive feedback.
  3. **Back Navigation Handling (`BackHandler`)**:
     - When on Step 2, pressing the Android system back gesture/button takes the user back to Step 1 instead of abruptly dismissing the dialog or screen.

- **Visual Identity & Theme**:
  - *Color Palette*:
    - Expense: Vibrant Crimson/Coral (`#EF4444`)
    - Income: Emerald Green (`#10B981`)
    - Transfer: Indigo/Sky Blue (`#3B82F6`)
    - Containers: M3 tonal elevation and soft card backgrounds (`surfaceVariant` with subtle borders).
  - *Typography*:
    - Large display numeral for hero amount (`titleLarge` / `headlineSmall`, bold).
    - Legible labels and values adhering to M3 density and localized Bengali numerals.

- **Interactive Feedback & Motion**:
  - Horizontal slide & fade transitions (`AnimatedContent` with `slideInHorizontally` / `slideOutHorizontally`) between Step 1 and Step 2.
  - Ripple feedback on all touch targets (> 48dp).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: In-Place Sheet Step Transition vs. Popup Dialog**
  - *Chosen Approach*: Maintain a single bottom sheet / screen container and swap contents via `step: Int` state (1 or 2) with `AnimatedContent`.
  - *Why*: Popup modal dialogs on top of bottom sheets cause window focus conflicts, double scrims, and awkward keyboard resizing on mobile devices. An in-place step transition is smoother and native-feeling.
- **Decision 2: Retain All State During Back-Navigation**
  - *Chosen Approach*: State variables (`amountText`, `selectedAccountId`, `selectedCategory`, `note`, `receiptUriStr`, etc.) are maintained at the top of the composable so switching between Step 1 and Step 2 never resets user input.
- **Decision 3: Keep Quick Date Chips & Quick Amount in Step 1**
  - *Chosen Approach*: Quick-entry helpers remain in Step 1 for fast input, while Step 2 is dedicated solely to review and verification.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   AddTransactionBottomSheet / Screen                   │
│                                                                        │
│  State: currentStep (1 = Input, 2 = Confirmation)                      │
│                                                                        │
│  ┌─────────────────────────────────┐   ┌─────────────────────────────┐ │
│  │             STEP 1              │   │           STEP 2            │ │
│  │       (Input & Form)            │   │   (Preview & Confirmation)  │ │
│  │                                 │   │                             │ │
│  │ • Transaction Type Chips        │   │ • Step Indicator (Step 2/2) │ │
│  │ • Amount Input + Quick Chips    │   │ • Hero Amount Display       │ │
│  │ • Account Selector Chips        │──►│ • Full Receipt Summary Card │ │
│  │ • Category Dropdown             │   │ • Balance Forecast Card     │ │
│  │ • Date & Time Selector          │   │                             │ │
│  │ • Note & Receipt Image Picker   │   │ [Back]    [Confirm & Save]  │ │
│  │                                 │   │   │              │          │ │
│  │ [Cancel]             [Next ──►] │   └───┼──────────────┼──────────┘ │
│  └─────────────────────────────────┘       │              ▼            │
│                       ▲                    │       viewModel.addTx()   │
│                       └────────────────────┘       dismiss & sync      │
│                            (Edit / Back)                               │
└────────────────────────────────────────────────────────────────────────┘
```

- **Interactive State & Logic**:
  - `var currentStep by remember { mutableStateOf(1) }`
  - In Step 1:
    - Button click executes validation:
      - Valid amount > 0: `errorMessage = null; currentStep = 2`
      - Invalid amount: `errorMessage = "অনুগ্রহ করে সঠিক পরিমাণ লিখুন"`
  - In Step 2:
    - `BackHandler(enabled = currentStep == 2) { currentStep = 1 }`
    - "Back" button: `currentStep = 1`
    - "Confirm" button: calls `handleSave(selectedType)` which writes to Room database, schedules sync, and invokes `closeSheet()`.
- **String Localization Resources**:
  - Add localized strings for Step 1 / Step 2 indicators:
    - `step_1_of_2`: `"ধাপ ১ এর ২"` / `"Step 1 of 2"`
    - `step_2_of_2`: `"ধাপ ২ এর ২"` / `"Step 2 of 2"`
    - `next_step`: `"পরবর্তী"` / `"Next"`
    - `previous_step`: `"আগের ধাপ"` / `"Back"`
    - `confirm_and_save`: `"নিশ্চিত করুন ও সংরক্ষণ"` / `"Confirm & Save"`
    - `transaction_preview`: `"লেনদেন পূর্বরূপ"` / `"Transaction Preview"`
    - `confirm_transaction`: `"লেনদেন নিশ্চিতকরণ"` / `"Confirm Transaction"`
