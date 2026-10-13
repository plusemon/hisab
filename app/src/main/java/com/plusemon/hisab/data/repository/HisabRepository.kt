package com.plusemon.hisab.data.repository

import com.plusemon.hisab.data.local.AppDatabase
import com.plusemon.hisab.data.model.AccountType
import com.plusemon.hisab.data.model.Budget
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.Contact
import com.plusemon.hisab.data.model.DebtType
import com.plusemon.hisab.data.model.Frequency
import com.plusemon.hisab.data.model.LoanDebt
import com.plusemon.hisab.data.model.LoanRepayment
import com.plusemon.hisab.data.model.RecurringRule
import com.plusemon.hisab.data.model.SavingsGoal
import com.plusemon.hisab.data.model.ShopCreditPayment
import com.plusemon.hisab.data.model.ShopCreditPurchase
import com.plusemon.hisab.data.model.TransactionRecord
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.TransactionWithDetails
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.data.model.UserSettings
import com.plusemon.hisab.data.model.Vendor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.Calendar

class HisabRepository(
    private val db: AppDatabase
) {
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val contactDao = db.contactDao()
    private val loanDebtDao = db.loanDebtDao()
    private val loanRepaymentDao = db.loanRepaymentDao()
    private val vendorDao = db.vendorDao()
    private val shopCreditPurchaseDao = db.shopCreditPurchaseDao()
    private val shopCreditPaymentDao = db.shopCreditPaymentDao()
    private val recurringDao = db.recurringDao()
    private val settingsDao = db.settingsDao()

    // Accounts & Balance Calculation
    fun getActiveAccounts(userId: String): Flow<List<UserAccount>> =
        accountDao.getActiveAccounts(userId)

    fun getAllAccounts(userId: String): Flow<List<UserAccount>> =
        accountDao.getAllAccounts(userId)

    suspend fun getAccountById(accountId: Long, userId: String): UserAccount? =
        accountDao.getAccountById(accountId, userId)

    suspend fun addAccount(account: UserAccount): Long = withContext(Dispatchers.IO) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: UserAccount) = withContext(Dispatchers.IO) {
        accountDao.updateAccount(account)
    }

    suspend fun archiveAccount(accountId: Long, userId: String, isArchived: Boolean) = withContext(Dispatchers.IO) {
        accountDao.setArchived(accountId, userId, isArchived)
    }

    suspend fun deleteAccount(accountId: Long, userId: String): Boolean = withContext(Dispatchers.IO) {
        val count = transactionDao.getTransactionCountForAccount(userId, accountId)
        if (count > 0) return@withContext false
        accountDao.deleteAccount(accountId, userId)
        return@withContext true
    }

    fun getTransactionsWithDetails(userId: String): Flow<List<TransactionWithDetails>> {
        return combine(
            transactionDao.getAllTransactions(userId),
            accountDao.getAllAccounts(userId),
            categoryDao.getAllCategories(userId)
        ) { transactions, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            transactions.mapNotNull { t ->
                val fromAcc = accountMap[t.accountId]
                if (fromAcc != null) {
                    val toAcc = t.toAccountId?.let { accountMap[it] }
                    val cat = t.categoryId?.let { categoryMap[it] }
                    TransactionWithDetails(
                        transaction = t,
                        account = fromAcc,
                        toAccount = toAcc,
                        category = cat
                    )
                } else null
            }
        }
    }

    fun calculateAccountBalances(
        userId: String
    ): Flow<Map<Long, Double>> {
        return combine(
            accountDao.getAllAccounts(userId),
            transactionDao.getAllTransactions(userId),
            loanDebtDao.getAllDebts(userId),
            loanRepaymentDao.getAllRepaymentsForUser(userId)
        ) { accounts, transactions, loans, repayments ->
            val balanceMap = mutableMapOf<Long, Double>()
            for (acc in accounts) {
                balanceMap[acc.id] = acc.startingBalance
            }

            for (t in transactions) {
                when (t.type) {
                    TransactionType.INCOME -> {
                        val curr = balanceMap[t.accountId] ?: 0.0
                        balanceMap[t.accountId] = curr + t.amount
                    }
                    TransactionType.EXPENSE -> {
                        val curr = balanceMap[t.accountId] ?: 0.0
                        balanceMap[t.accountId] = curr - t.amount
                    }
                    TransactionType.TRANSFER -> {
                        val fromCurr = balanceMap[t.accountId] ?: 0.0
                        balanceMap[t.accountId] = fromCurr - (t.amount + t.fee)

                        if (t.toAccountId != null) {
                            val toCurr = balanceMap[t.toAccountId] ?: 0.0
                            val receivedAmount = t.amount * (if (t.exchangeRate > 0) t.exchangeRate else 1.0)
                            balanceMap[t.toAccountId] = toCurr + receivedAmount
                        }
                    }
                }
            }

            // Loans & Debts account adjustments (only for legacy records without linked transaction records):
            val loanMap = loans.associateBy { it.id }
            for (debt in loans) {
                if (debt.transactionId == null) {
                    val curr = balanceMap[debt.accountId] ?: 0.0
                    if (debt.type == DebtType.OWED_TO_ME) {
                        balanceMap[debt.accountId] = curr - debt.amount
                    } else {
                        balanceMap[debt.accountId] = curr + debt.amount
                    }
                }
            }

            // Repayments account adjustments (only for legacy records without linked transaction records):
            for (rep in repayments) {
                if (rep.transactionId == null) {
                    val loan = loanMap[rep.loanId]
                    if (loan != null) {
                        val curr = balanceMap[rep.accountId] ?: 0.0
                        if (loan.type == DebtType.OWED_TO_ME) {
                            balanceMap[rep.accountId] = curr + rep.amount
                        } else {
                            balanceMap[rep.accountId] = curr - rep.amount
                        }
                    }
                }
            }

            balanceMap
        }
    }

    // Transactions
    suspend fun insertTransaction(transaction: TransactionRecord): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionRecord) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long, userId: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(id, userId)
    }

    suspend fun getTransactionById(id: Long, userId: String): TransactionRecord? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(id, userId)
    }

    suspend fun importTransactions(transactions: List<TransactionRecord>) = withContext(Dispatchers.IO) {
        transactionDao.insertTransactions(transactions)
    }

    // Categories
    fun getActiveCategories(userId: String): Flow<List<Category>> =
        categoryDao.getActiveCategories(userId)

    fun getCategoriesByType(userId: String, type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(userId, type)

    suspend fun addCategory(category: Category): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(categoryId: Long, userId: String): Boolean = withContext(Dispatchers.IO) {
        val count = transactionDao.getTransactionCountForCategory(userId, categoryId)
        if (count > 0) {
            categoryDao.setArchived(categoryId, userId, true)
            return@withContext false
        }
        categoryDao.deleteCategory(categoryId, userId)
        return@withContext true
    }

    // Budgets
    fun getBudgetsForMonth(userId: String, monthYear: String): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(userId, monthYear)

    suspend fun setBudget(budget: Budget): Long = withContext(Dispatchers.IO) {
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudget(id: Long, userId: String) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudget(id, userId)
    }

    // Savings Goals
    fun getSavingsGoals(userId: String): Flow<List<SavingsGoal>> =
        savingsGoalDao.getAllGoals(userId)

    suspend fun addSavingsGoal(goal: SavingsGoal): Long = withContext(Dispatchers.IO) {
        savingsGoalDao.insertGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        savingsGoalDao.updateGoal(goal)
    }

    suspend fun adjustGoalSavings(id: Long, userId: String, delta: Double) = withContext(Dispatchers.IO) {
        val goal = savingsGoalDao.getGoalById(id, userId) ?: return@withContext
        val newAmount = (goal.currentSavedAmount + delta).coerceAtLeast(0.0)
        val completed = newAmount >= goal.targetAmount
        savingsGoalDao.updateSavedAmount(id, userId, newAmount, completed)
    }

    suspend fun deleteSavingsGoal(id: Long, userId: String) = withContext(Dispatchers.IO) {
        savingsGoalDao.deleteGoal(id, userId)
    }

    // Contacts & Lending/Borrowing
    fun getContacts(userId: String): Flow<List<Contact>> = contactDao.getAllContacts(userId)

    suspend fun addContact(contact: Contact): Long = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(id: Long, userId: String) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(id, userId)
    }

    fun getLoansAndDebts(userId: String): Flow<List<LoanDebt>> = loanDebtDao.getAllDebts(userId)
    fun getLoanRepayments(userId: String): Flow<List<LoanRepayment>> = loanRepaymentDao.getAllRepaymentsForUser(userId)
    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepayment>> = loanRepaymentDao.getRepaymentsForLoan(loanId)

    private suspend fun getOrCreateCategory(
        userId: String,
        type: TransactionType,
        nameBn: String,
        nameEn: String,
        iconName: String,
        colorHex: String
    ): Long {
        val existing = categoryDao.getCategoriesByTypeSync(userId, type)
        val found = existing.find { it.nameEn == nameEn || it.nameBn == nameBn }
        if (found != null) return found.id
        val newCat = Category(
            userId = userId,
            nameBn = nameBn,
            nameEn = nameEn,
            type = type,
            iconName = iconName,
            colorHex = colorHex,
            isDefault = true
        )
        return categoryDao.insertCategory(newCat)
    }

    suspend fun addLoanDebt(debt: LoanDebt): Long = withContext(Dispatchers.IO) {
        var cId = debt.contactId
        val trimmedName = debt.personName.trim()
        val trimmedPhone = debt.phone.trim()

        if (cId == 0L && trimmedName.isNotBlank()) {
            val existingContact = contactDao.getContactByName(trimmedName, debt.userId)
            if (existingContact != null) {
                cId = existingContact.id
                if (trimmedPhone.isNotBlank() && (existingContact.phone.isBlank() || existingContact.phone != trimmedPhone)) {
                    contactDao.updateContact(existingContact.copy(phone = trimmedPhone))
                }
            } else {
                val newContact = Contact(userId = debt.userId, name = trimmedName, phone = trimmedPhone)
                cId = contactDao.insertContact(newContact)
            }
        } else if (cId != 0L && trimmedPhone.isNotBlank()) {
            val existing = contactDao.getContactById(cId, debt.userId)
            if (existing != null && (existing.phone.isBlank() || existing.phone != trimmedPhone)) {
                contactDao.updateContact(existing.copy(phone = trimmedPhone))
            }
        }

        // Automatic transaction creation for account ledger reconciliation
        val txType = if (debt.type == DebtType.OWED_TO_ME) TransactionType.EXPENSE else TransactionType.INCOME
        val catNameBn = if (debt.type == DebtType.OWED_TO_ME) "ধার প্রদান" else "ধার গ্রহণ"
        val catNameEn = if (debt.type == DebtType.OWED_TO_ME) "Loan Given" else "Loan Received"
        val catIcon = if (debt.type == DebtType.OWED_TO_ME) "payments" else "account_balance_wallet"
        val catColor = if (debt.type == DebtType.OWED_TO_ME) "#EF4444" else "#10B981"

        val catId = getOrCreateCategory(
            userId = debt.userId,
            type = txType,
            nameBn = catNameBn,
            nameEn = catNameEn,
            iconName = catIcon,
            colorHex = catColor
        )

        val txNote = when (debt.type) {
            DebtType.OWED_TO_ME -> "ধার প্রদান: $trimmedName ${if (debt.note.isNotBlank()) "(${debt.note})" else ""}".trim()
            DebtType.I_OWE -> "ধার গ্রহণ: $trimmedName ${if (debt.note.isNotBlank()) "(${debt.note})" else ""}".trim()
        }

        val tx = TransactionRecord(
            userId = debt.userId,
            accountId = debt.accountId,
            categoryId = catId,
            amount = debt.amount,
            type = txType,
            dateTimestamp = debt.createdAt,
            note = txNote
        )
        val txId = transactionDao.insertTransaction(tx)

        val finalDebt = debt.copy(
            contactId = cId,
            personName = trimmedName,
            phone = trimmedPhone,
            transactionId = txId
        )
        loanDebtDao.insertDebt(finalDebt)
    }

    suspend fun recordLoanRepayment(loanId: Long, userId: String, accountId: Long, amount: Double, note: String): Long = withContext(Dispatchers.IO) {
        val debt = loanDebtDao.getDebtById(loanId, userId) ?: return@withContext 0L

        // Repayment transaction creation for account ledger reconciliation:
        val txType = if (debt.type == DebtType.OWED_TO_ME) TransactionType.INCOME else TransactionType.EXPENSE
        val catNameBn = if (debt.type == DebtType.OWED_TO_ME) "ধার ফেরত গ্রহণ" else "ধার পরিশোধ"
        val catNameEn = if (debt.type == DebtType.OWED_TO_ME) "Loan Collection" else "Loan Repayment"
        val catIcon = if (debt.type == DebtType.OWED_TO_ME) "savings" else "payment"
        val catColor = if (debt.type == DebtType.OWED_TO_ME) "#0D9488" else "#F97316"

        val catId = getOrCreateCategory(
            userId = userId,
            type = txType,
            nameBn = catNameBn,
            nameEn = catNameEn,
            iconName = catIcon,
            colorHex = catColor
        )

        val trimmedName = debt.personName.trim()
        val txNote = when (debt.type) {
            DebtType.OWED_TO_ME -> "ধার ফেরত গ্রহণ: $trimmedName ${if (note.isNotBlank()) "($note)" else ""}".trim()
            DebtType.I_OWE -> "ধার পরিশোধ: $trimmedName ${if (note.isNotBlank()) "($note)" else ""}".trim()
        }

        val now = System.currentTimeMillis()
        val tx = TransactionRecord(
            userId = userId,
            accountId = accountId,
            categoryId = catId,
            amount = amount,
            type = txType,
            dateTimestamp = now,
            note = txNote
        )
        val txId = transactionDao.insertTransaction(tx)

        val repayment = LoanRepayment(
            userId = userId,
            loanId = loanId,
            accountId = accountId,
            amount = amount,
            dateTimestamp = now,
            note = note,
            transactionId = txId
        )
        val repId = loanRepaymentDao.insertRepayment(repayment)
        val newPaid = debt.paidAmount + amount
        val settled = newPaid >= debt.amount
        loanDebtDao.recordRepayment(loanId, userId, newPaid, settled)
        repId
    }

    suspend fun deleteLoanDebt(id: Long, userId: String) = withContext(Dispatchers.IO) {
        val debt = loanDebtDao.getDebtById(id, userId)
        if (debt != null) {
            debt.transactionId?.let { txId ->
                transactionDao.deleteTransactionById(txId, userId)
            }
            val repayments = loanRepaymentDao.getRepaymentsForLoanSync(id)
            for (rep in repayments) {
                rep.transactionId?.let { repTxId ->
                    transactionDao.deleteTransactionById(repTxId, userId)
                }
            }
        }
        loanDebtDao.deleteDebt(id, userId)
    }

    // Shop Credit & Vendors
    fun getVendors(userId: String): Flow<List<Vendor>> = vendorDao.getAllVendors(userId)
    fun getShopCreditPurchases(userId: String): Flow<List<ShopCreditPurchase>> = shopCreditPurchaseDao.getAllPurchases(userId)
    fun getShopCreditPayments(userId: String): Flow<List<ShopCreditPayment>> = shopCreditPaymentDao.getAllPayments(userId)

    suspend fun addVendor(vendor: Vendor): Long = withContext(Dispatchers.IO) {
        vendorDao.insertVendor(vendor)
    }

    suspend fun updateVendor(vendor: Vendor) = withContext(Dispatchers.IO) {
        vendorDao.updateVendor(vendor)
    }

    suspend fun updateVendorPhone(vendorId: Long, userId: String, phone: String) = withContext(Dispatchers.IO) {
        val existing = vendorDao.getVendorById(vendorId, userId)
        if (existing != null) {
            vendorDao.updateVendor(existing.copy(phone = phone))
        }
    }

    suspend fun archiveVendor(vendorId: Long, userId: String, isArchived: Boolean) = withContext(Dispatchers.IO) {
        vendorDao.archiveVendor(vendorId, userId, isArchived)
    }

    suspend fun addShopCreditPurchase(
        vendorId: Long,
        vendorName: String,
        userId: String,
        amount: Double,
        dueDate: Long?,
        note: String,
        phone: String,
        locationNote: String,
        categoryTag: String
    ): Long = withContext(Dispatchers.IO) {
        var vId = vendorId
        if (vId == 0L && vendorName.isNotBlank()) {
            val newVendor = Vendor(userId = userId, name = vendorName, phone = phone, locationNote = locationNote, categoryTag = categoryTag)
            vId = vendorDao.insertVendor(newVendor)
        } else if (vId > 0L && phone.isNotBlank()) {
            val existing = vendorDao.getVendorById(vId, userId)
            if (existing != null && existing.phone != phone) {
                vendorDao.updateVendor(existing.copy(phone = phone))
            }
        }
        val purchase = ShopCreditPurchase(
            userId = userId,
            vendorId = vId,
            amount = amount,
            dueDate = dueDate,
            note = note
        )
        shopCreditPurchaseDao.insertPurchase(purchase)
    }

    suspend fun settleShopCredit(
        vendorId: Long,
        userId: String,
        accountId: Long,
        amount: Double,
        note: String
    ): Long = withContext(Dispatchers.IO) {
        val categories = categoryDao.getCategoriesByTypeSync(userId, TransactionType.EXPENSE)
        val categoryId = categories.find { it.nameEn == "Shop Credit Payment" }?.id ?: categories.firstOrNull()?.id ?: 1L

        val tx = TransactionRecord(
            userId = userId,
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            type = TransactionType.EXPENSE,
            dateTimestamp = System.currentTimeMillis(),
            note = if (note.isNotBlank()) note else "Shop Credit Payment"
        )
        val txId = transactionDao.insertTransaction(tx)

        val payment = ShopCreditPayment(
            userId = userId,
            vendorId = vendorId,
            accountId = accountId,
            amount = amount,
            note = note,
            transactionId = txId
        )
        val paymentId = shopCreditPaymentDao.insertPayment(payment)

        val unsettled = shopCreditPurchaseDao.getUnsettledPurchasesForVendorSync(vendorId, userId)
        var remainingPayment = amount

        for (purchase in unsettled) {
            if (remainingPayment <= 0.0) break
            val remPurchase = purchase.remainingAmount
            if (remainingPayment >= remPurchase) {
                shopCreditPurchaseDao.updatePaidAmount(purchase.id, purchase.amount, true)
                remainingPayment -= remPurchase
            } else {
                val newPaid = purchase.paidAmount + remainingPayment
                shopCreditPurchaseDao.updatePaidAmount(purchase.id, newPaid, false)
                remainingPayment = 0.0
            }
        }

        paymentId
    }

    // Recurring Rules
    fun getRecurringRules(userId: String): Flow<List<RecurringRule>> = recurringDao.getAllRules(userId)

    suspend fun addRecurringRule(rule: RecurringRule): Long = withContext(Dispatchers.IO) {
        recurringDao.insertRule(rule)
    }

    suspend fun setRecurringPaused(id: Long, userId: String, isPaused: Boolean) = withContext(Dispatchers.IO) {
        recurringDao.setPaused(id, userId, isPaused)
    }

    suspend fun deleteRecurringRule(id: Long, userId: String) = withContext(Dispatchers.IO) {
        recurringDao.deleteRule(id, userId)
    }

    suspend fun processRecurringTransactions(userId: String) = withContext(Dispatchers.IO) {
        val rules = recurringDao.getActiveRulesList(userId)
        val now = System.currentTimeMillis()

        for (rule in rules) {
            val lastGen = if (rule.lastGeneratedDate > 0) rule.lastGeneratedDate else rule.startDate
            var nextDue = calculateNextDueDate(lastGen, rule.frequency)

            val generatedList = mutableListOf<TransactionRecord>()
            var latestTimestamp = rule.lastGeneratedDate

            while (nextDue <= now) {
                if (rule.endDate != null && nextDue > rule.endDate) break

                generatedList.add(
                    TransactionRecord(
                        userId = userId,
                        accountId = rule.accountId,
                        categoryId = rule.categoryId,
                        amount = rule.amount,
                        type = rule.type,
                        dateTimestamp = nextDue,
                        note = "${rule.note} (Auto)",
                        isRecurringInstance = true,
                        recurringRuleId = rule.id
                    )
                )
                latestTimestamp = nextDue
                nextDue = calculateNextDueDate(nextDue, rule.frequency)
            }

            if (generatedList.isNotEmpty()) {
                transactionDao.insertTransactions(generatedList)
                recurringDao.updateLastGenerated(rule.id, userId, latestTimestamp)
            }
        }
    }

    private fun calculateNextDueDate(fromTimestamp: Long, frequency: Frequency): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromTimestamp
        when (frequency) {
            Frequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            Frequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            Frequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    // Settings
    fun getUserSettings(userId: String): Flow<UserSettings?> = settingsDao.getSettings(userId)

    suspend fun updateSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun clearAllData(userId: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactionsForUser(userId)
        budgetDao.deleteAllBudgetsForUser(userId)
        savingsGoalDao.deleteAllGoalsForUser(userId)
        contactDao.deleteAllContactsForUser(userId)
        loanDebtDao.deleteAllDebtsForUser(userId)
        loanRepaymentDao.deleteAllRepaymentsForUser(userId)
        vendorDao.deleteAllVendorsForUser(userId)
        shopCreditPurchaseDao.deleteAllPurchasesForUser(userId)
        shopCreditPaymentDao.deleteAllPaymentsForUser(userId)
        recurringDao.deleteAllRulesForUser(userId)
    }
}
