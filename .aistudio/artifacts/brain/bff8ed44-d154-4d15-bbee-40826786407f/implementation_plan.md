# Implementation Plan - Transaction Resulting Balance Preview

## Problem Statement
When adding or editing a transaction (Expense, Income, or Transfer), users want to see what the resulting balance of the account(s) will be *before* confirming/saving the transaction. If an expense causes the balance to drop below zero, a warning alert should also be shown.

## Proposed Changes

### 1. `AddTransactionBottomSheet.kt` & `AddEditTransactionScreen.kt`
- Compute the selected account's current balance from `accountsWithBalances`.
- Parse the entered amount (`amountText.toDoubleOrNull() ?: 0.0`) and optional fee.
- Calculate the resulting balance based on transaction type:
  - **Expense**: `currentBalance - amount - fee`
  - **Income**: `currentBalance + amount`
  - **Transfer**: Source account `currentBalance - amount - fee`, and Target account `targetBalance + (amount * exchangeRate)`.
- Display a summary card near the save button showing:
  - Current Balance -> Resulting Balance
  - If resulting balance is negative (for expense or source account in transfer), show a clear warning banner/alert (e.g. "Warning: Account balance will become negative!").
- Ensure localization (`bn` vs `en`) support for the labels.

## Verification Plan
- Compile the app using `compile_applet`.
- Verify that entering an expense or transfer updates the resulting balance preview in real-time.
- Verify negative balance warning appears when applicable.
