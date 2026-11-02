# Debt Tracking UI Module with Paid/Unpaid Status Indicators

A specialized, comprehensive UI module for tracking lending and borrowing records in Hisab, featuring dedicated status tabs for 'All', 'Unpaid', and 'Paid' entries, color-coded visual status indicators with progress bars, summary metric cards, and rapid actions including partial repayments, one-tap settlement, and direct contact calling.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during Phase 1 clarification and shape this implementation:

- **Confirmed Decision 1 (Status Filtering Architecture)**: Dedicated tabs for **All**, **Unpaid** (Active/Pending), and **Paid** (Settled) records with live badge counts, allowing users to instantly isolate outstanding loans from cleared records.
- **Confirmed Decision 2 (Visual Status Representation)**: Color-coded pill badges featuring status icons (Emerald Green with checkmark for *Paid*, Amber with clock for *Unpaid / Partial*, and Red with warning for *Overdue*) accompanied by an animated progress bar displaying repayment percentage.
- **Confirmed Decision 3 (Primary Card Actions)**: Every debt record item directly provides three high-frequency actions: **Quick Partial Payment**, **Mark as Settled** (one-tap full clearance), and **Call Contact** (direct dialer launch).

---

## 1. Overview & Core Concept

### What It Does
- **Dedicated Debt Tracking Experience**: Upgrades the lending and borrowing section in Hisab with a dedicated records feed alongside the contact ledger.
- **Granular Status Segmentation**: Users can switch between **All**, **Unpaid** (remaining balance > 0), and **Paid** (settled records) at any time.
- **Rich Status Badges & Progress Indicators**: Displays clear visual badges indicating whether a record is *Paid* (100% cleared), *Unpaid* (0% paid), *Partially Paid* (with progress bar), or *Overdue* (past due date).
- **Direct Operational Controls**: Users can record repayments, settle debts immediately, call borrowers/lenders, or search records without navigating nested layers.

### Target Audience & Persona
- Individuals, freelancers, and shop owners who actively lend or borrow money and need to know exactly who has paid, who owes money, and when payments are overdue.

### Key Value
- Replaces ambiguous debt lists with unambiguous visual status states, saving time and preventing forgotten receivables.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Browsing Debts by Status**:
   - The user opens the **Lending & Borrowing** tab in the Debts screen.
   - At the top, three summary metric cards display **Total Receivable (Unpaid)**, **Total Payable (Unpaid)**, and **Total Settled**.
   - Below the metrics, dedicated segmented tabs allow switching between:
     - **All Records** `[ সব (12) ]`
     - **Unpaid** `[ অপরিশোধিত (8) ]`
     - **Paid** `[ পরিশোধিত (4) ]`
   - A secondary sub-filter row allows filtering by type: **All Types**, **Lent (Gave / পাওনা)**, and **Borrowed (Took / দেনা)**.

2. **Inspecting Debt Status & Repayment Progress**:
   - Each debt card displays:
     - Person name, avatar initial, and linked phone number.
     - Debt direction pill (e.g., *Gave Money / পাওনা* in green, *Took Money / দেনা* in red).
     - **Status Badge**:
       - *Paid*: Green background, CheckCircle icon, "পরিশোধিত" (Paid).
       - *Unpaid*: Amber/Yellow background, Schedule icon, "বাকি" (Unpaid).
       - *Partially Paid*: Blue/Amber badge with "আংশিক (50% পরিশোধিত)".
       - *Overdue*: Red badge with Warning icon if due date has passed.
     - Large typography for remaining balance alongside original total amount.
     - Repayment progress bar with percentage indicator.
     - Due date tag with calendar icon if scheduled.

3. **Executing Primary Actions**:
   - **Quick Partial Payment**: Tapping "Record Payment" opens the repayment sheet to log an installment with account deduction.
   - **One-Tap Mark as Settled**: Tapping "Mark as Settled" allows the user to settle the remaining balance in full with account confirmation.
   - **Call Contact**: Tapping the phone icon immediately triggers the phone dialer.

4. **Viewing Within Contact Ledger**:
   - When drilling into a specific contact's ledger, the same All / Unpaid / Paid tabs and status badges are available to audit individual transaction history.

### Visual Identity & Theme
- **Paid Badge**: Container `#DCFCE7`, Content `#15803D`, Icon `Icons.Default.CheckCircle`.
- **Unpaid Badge**: Container `#FEF3C7`, Content `#B45309`, Icon `Icons.Default.Schedule`.
- **Partial Badge**: Container `#E0F2FE`, Content `#0369A1`, Icon `Icons.Default.Pending`.
- **Overdue Badge**: Container `#FEE2E2`, Content `#B91C1C`, Icon `Icons.Default.Warning`.
- **Progress Track**: Elevated M3 rounded bar with smooth progress interpolation.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Dual View Presentation (Direct Records Feed + Contact Grouping)
- **Chosen Approach**: Provide a toggle between **"All Entries" (সব এন্ট্রি)** and **"By Contact" (কন্টাক্ট অনুসারে)** in the Lending & Borrowing view, with the status tabs (All / Unpaid / Paid) filtering both views seamlessly.
- **Why**: Many users want to see recent unpaid debts directly chronologically without clicking into individual contacts, while others prefer viewing totals grouped by person.
- **Alternatives Considered**: Only showing contacts (requires too many clicks to see individual debt statuses).

### Decision 2: One-Tap Settle Flow with Account Selector
- **Chosen Approach**: When the user taps "Mark as Settled" on an unpaid debt, show a quick confirmation sheet/dialog allowing selection of the receiving/paying account, then record the remaining balance as a full repayment.
- **Why**: Keeps double-entry bookkeeping accurate (updating account balance and creating a transaction record) while requiring only 2 taps from the user.
- **Alternatives Considered**: Settle without selecting an account (causes account balances to fall out of sync).

### Decision 3: Overdue Detection Logic
- **Chosen Approach**: Dynamically evaluate `debt.dueDate != null && debt.dueDate < System.currentTimeMillis() && !debt.isSettled` to show an urgent Overdue badge.
- **Why**: Helps users prioritize collecting payments that are past due.

---

## 4. Technical Architecture & Component Mapping

### Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DebtsScreen (Lending & Borrowing)               │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Top Balance Cards: Total Receivable | Total Payable | Settled   │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Status Segmented Tabs: [ All (N) ]  [ Unpaid (N) ] [ Paid (N) ]│   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Type Filter Chips:  (● All)   (↗ Gave / পাওনা)  (↙ Took / দেনা) │   │
│   │ View Mode Switch:   [ Individual Entries | By Contact ]        │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ LazyColumn: LoanDebtItemCard List (Filtered by Status & Type)  │   │
│   │  ┌──────────────────────────────────────────────────────────┐  │   │
│   │  │ LoanDebtItemCard                                         │  │   │
│   │  │ • StatusBadge (Paid / Unpaid / Partial / Overdue)        │  │   │
│   │  │ • Repayment ProgressBar (0% - 100%)                      │  │   │
│   │  │ • Amounts: Remaining (Primary) vs Original Total         │  │   │
│   │  │ • Action Row: [Record Payment] [Settle Full] [Call] [⋯] │  │   │
│   │  └──────────────────────────────────────────────────────────┘  │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
                        ┌─────────────────────────┐
                        │     HisabViewModel      │
                        │ • loansDebts (Flow)     │
                        │ • recordDebtPayment()   │
                        │ • settleFullDebt()      │
                        │ • deleteLoanDebt()      │
                        └─────────────────────────┘
```

### Component & State Mapping

1. **`DebtStatusFilter` Enum**:
   - `ALL`, `UNPAID`, `PAID`
   - Filter predicate:
     - `ALL`: all debts
     - `UNPAID`: `!it.isSettled`
     - `PAID`: `it.isSettled`

2. **`DebtStatusBadge` Composable**:
   - Renders color-coded status chips based on `debt.isSettled`, `debt.paidAmount`, and `debt.dueDate`.

3. **`LoanDebtItemCard` Enhancements**:
   - Displays status badge, progress bar, formatted remaining amount, due date tags, quick call button, and action buttons (`Record Payment` and `Mark as Settled`).

4. **`SettleFullDebtDialog` Composable**:
   - Allows fast 1-tap or account-confirmed settlement of remaining balance.

5. **`ContactLedgerContent` Status Support**:
   - Adds the same status tabs inside the individual contact details view.
