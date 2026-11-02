# Lending & Borrowing Transaction History & Repayment Ledger

Enable users to view a comprehensive, chronological transaction history for every lending and borrowing account directly from a dedicated details bottom sheet when clicking any loan card, with full visibility into the initial loan disbursement, individual repayment installments, account mappings, and repayment deletion with automatic balance rollback.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed based on your preferences:
> - **Entry Point**: Clicking on any Loan & Borrow card opens a dedicated **Transaction History & Ledger Bottom Sheet**.
> - **Display Scope**: Displays both the **initial loan creation** (amount given/taken, account, date, note) and **all subsequent repayment installments** in chronological order.
> - **Management & Rollback**: Allows **deleting individual repayment entries** with automatic balance recalculation, loan status rollback (e.g. from Settled back to Partial/Unpaid), and reversal of the linked account ledger reconciliation transaction.

---

## 1. Overview & Core Concept

Currently, the **Lending & Borrowing** tab in Hisab calculates and displays aggregate numbers (*Paid: ৳ 2,000*, *Left: ৳ 5,000*, *Remaining: ৳ 5,000*, *Total: ৳ 7,000*) but provides no visual record of when payments took place, which accounts were used, or what notes were attached.

This feature introduces a full **Loan Transaction History & Ledger**:
- **Audit Trail**: View the exact origin of the loan and every installment payment made over time.
- **Account Ledger Transparency**: Shows which payment method / user account (e.g., Cash, bKash, Bank) received or paid each installment.
- **Accidental Entry Recovery**: Gives the user full control to delete erroneous repayment records, cleanly reverting the debt balance and linked account balance.

---

## 2. User Experience & Visual Design

### 2.1 Loan Card Affordances (`LoanDebtItemCard`)
- The loan card is made explicitly clickable with subtle visual cues (`Modifier.clickable(onClick = { onOpenDetails(debt) })`).
- A subtle "History / বিবরণ ও ইতিহাস" pill or button with a receipt/clock icon is integrated on the card header/footer so users immediately understand tapping opens the full ledger.
- Existing actions like **"Record Payment"** and **"Mark Settled"** remain directly accessible on the card without extra taps.

### 2.2 Dedicated Detail & History Bottom Sheet (`LoanDetailsBottomSheet`)
When clicking a card, a bottom sheet slides up containing:
1. **Header & Contact Info**:
   - Person Avatar, Contact Name, and Phone Number.
   - Quick "Call" button (`ContactUtils.dialPhoneNumber`) and status badges (*Gave Money / Took Money*, *Partially Paid / Settled / Overdue*).
2. **Financial Progress Card**:
   - Total Principal Amount, Total Repaid, Remaining Balance.
   - Visual progress bar showing repayment percentage.
   - Due date indicator (with overdue warning if applicable).
3. **Chronological Transaction History Section**:
   - Section title: **"লেনদেন ও কিস্তির ইতিহাস (Transaction & Payment History)"** with an entry count badge.
   - **Initial Loan Disbursement Item**:
     - Distinct icon (TrendingUp for Gave Money / TrendingDown for Took Money).
     - "মূল ঋণ / Initial Loan Disbursement" label.
     - Timestamp, account name (e.g., Cash / bKash), amount, and initial note.
   - **Repayment Installment Items**:
     - Installment number badge (e.g., #1, #2, #3).
     - Formatted date and time (e.g., "12 Oct 2026, 03:45 PM").
     - Account used (e.g., "bKash" or "Cash Account").
     - Repayment amount in green/teal.
     - Running balance calculation (remaining debt after this installment).
     - Optional payment note if entered.
     - **Delete action** icon button with confirmation dialog for safe rollback.
4. **Bottom Actions**:
   - **"+ Record Payment"** button if unsettled.
   - **"Mark Settled"** button if remaining balance > 0.
   - **"Delete Loan"** button if user wishes to delete the entire loan record.

### 2.3 Visual Polish & Design System Alignment
- Follows Material Design 3 guidelines: rounded container cards (`RoundedCornerShape(16.dp)`), high-contrast badges, surface elevation, and clear typography.
- Full bilingual support (English & Bengali) using existing `Localization.getString` and Bengali numerals (`Formatters.toBanglaDigits`).
- Smooth entry animation and full Edge-to-Edge window inset handling.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Bottom Sheet vs New Navigation Route**
  - *Chosen Approach*: `ModalBottomSheet` with drag handle and edge-to-edge padding.
  - *Why*: Allows instant inspection of payment history without losing place or filter state on the Lending & Borrowing list, providing a fast, lightweight mobile experience.
- **Decision 2: Transaction Linkage & Balance Rollback Strategy**
  - *Chosen Approach*: When a `LoanRepayment` is deleted:
    1. Recalculate `debt.paidAmount = max(0.0, debt.paidAmount - repayment.amount)`.
    2. Reset `isSettled = false` if `newPaidAmount < debt.amount`.
    3. Delete linked `TransactionRecord` from Room so account balances stay reconciled.
    4. Propagate deletion and updates to Firestore via `FirestoreRepository`.
  - *Why*: Maintains complete accounting integrity between loan balances, account balances, and cloud backup.

---

## 4. Technical Architecture & Data Strategy

### 4.1 System & Component Diagram

```
┌────────────────────────────────────────────────────────┐
│                      DebtsScreen                       │
│  ┌──────────────────────────────────────────────────┐  │
│  │              LoanDebtItemCard                    │  │
│  │  - Shows amounts, progress bar, action buttons   │  │
│  │  - Click -> opens LoanDetailsBottomSheet         │  │
│  └────────────────────────┬─────────────────────────┘  │
└───────────────────────────┼────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│               LoanDetailsBottomSheet                   │
│  - Loan Summary & Due Date                             │
│  - Initial Loan Transaction Card                       │
│  - Repayment Installments Timeline (LazyColumn)        │
│    * Date & Time                                       │
│    * Amount & Running Balance                          │
│    * Payment Account Name                              │
│    * Delete Repayment Option with Confirmation         │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                     HisabViewModel                     │
│  - loanRepayments: StateFlow<List<LoanRepayment>>     │
│  - accounts: StateFlow<List<AccountWithBalance>>       │
│  - deleteLoanRepayment(repaymentId, loanId)            │
│  - recordLoanRepayment(loanId, accountId, amount, note)│
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                    HisabRepository                     │
│  - deleteLoanRepayment(repaymentId, loanId, userId)   │
│    1. Reverse paidAmount on LoanDebtDao               │
│    2. Delete linked TransactionRecord on Dao          │
│    3. Delete LoanRepayment on Dao                     │
│    4. Sync changes via FirestoreRepository            │
└────────────────────────────────────────────────────────┘
```

### 4.2 Data Flow & Component Mapping
1. **Data Querying**: `HisabViewModel.loanRepayments` is already collected from Room. In `LoanDetailsBottomSheet`, repayments for the selected debt are filtered: `repayments.filter { it.loanId == selectedDebt.id }.sortedByDescending { it.dateTimestamp }`.
2. **Account Name Resolution**: Each repayment's `accountId` is resolved to `userAccount.name` from `viewModel.accounts`.
3. **Initial Loan Transaction**: Created from `selectedDebt.createdAt` and `selectedDebt.accountId`, giving users the full story from loan inception to current status.
4. **Delete & Rollback**:
   - `HisabRepository.deleteLoanRepayment(repaymentId, loanId, userId)` executes an atomic update reverting `paidAmount` and deleting the associated account transaction.
   - `FirestoreRepository.deleteLoanRepayment` and `FirestoreRepository.saveLoanDebt` keep Firebase in sync.

---

## 5. Verification Plan

1. **Compilation Verification**: Run `compile_applet` to ensure syntax, imports, and type consistency across `HisabRepository`, `HisabViewModel`, and `DebtsScreen`.
2. **UI & Interaction Verification**:
   - Tap any loan debt card -> verify `LoanDetailsBottomSheet` opens cleanly.
   - Verify initial loan creation details (timestamp, amount, account).
   - Record a new payment -> verify it immediately appears in the installment history with running balance updated.
   - Delete an installment -> verify confirmation dialog appears, installment is removed, and loan card remaining balance increments back up correctly.
   - Test both English and Bengali UI strings.
