package com.plusemon.hisab.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.data.model.UserSettings
import com.plusemon.hisab.data.model.Vendor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface FirestoreRepository {
    suspend fun saveTransaction(userId: String, transaction: TransactionRecord): Result<Unit>
    suspend fun updateTransaction(userId: String, transaction: TransactionRecord): Result<Unit>
    suspend fun deleteTransaction(userId: String, transactionId: Long): Result<Unit>
    suspend fun fetchTransactions(userId: String): Result<List<TransactionRecord>>

    suspend fun saveAccount(userId: String, account: UserAccount): Result<Unit>
    suspend fun deleteAccount(userId: String, accountId: Long): Result<Unit>

    suspend fun saveCategory(userId: String, category: Category): Result<Unit>
    suspend fun deleteCategory(userId: String, categoryId: Long): Result<Unit>

    suspend fun saveBudget(userId: String, budget: Budget): Result<Unit>
    suspend fun deleteBudget(userId: String, budgetId: Long): Result<Unit>

    suspend fun saveSavingsGoal(userId: String, goal: SavingsGoal): Result<Unit>
    suspend fun deleteSavingsGoal(userId: String, goalId: Long): Result<Unit>

    suspend fun saveContact(userId: String, contact: Contact): Result<Unit>
    suspend fun deleteContact(userId: String, contactId: Long): Result<Unit>

    suspend fun saveLoanDebt(userId: String, debt: LoanDebt): Result<Unit>
    suspend fun deleteLoanDebt(userId: String, debtId: Long): Result<Unit>

    suspend fun saveLoanRepayment(userId: String, repayment: LoanRepayment): Result<Unit>
    suspend fun deleteLoanRepayment(userId: String, repaymentId: Long): Result<Unit>

    suspend fun saveVendor(userId: String, vendor: Vendor): Result<Unit>
    suspend fun deleteVendor(userId: String, vendorId: Long): Result<Unit>

    suspend fun saveShopCreditPurchase(userId: String, purchase: ShopCreditPurchase): Result<Unit>
    suspend fun deleteShopCreditPurchase(userId: String, purchaseId: Long): Result<Unit>

    suspend fun saveShopCreditPayment(userId: String, payment: ShopCreditPayment): Result<Unit>
    suspend fun deleteShopCreditPayment(userId: String, paymentId: Long): Result<Unit>

    suspend fun saveRecurringRule(userId: String, rule: RecurringRule): Result<Unit>
    suspend fun deleteRecurringRule(userId: String, ruleId: Long): Result<Unit>

    suspend fun saveUserSettings(userId: String, settings: UserSettings): Result<Unit>
    suspend fun fetchUserSettings(userId: String): Result<UserSettings?>

    suspend fun checkHasCloudData(userId: String): Result<Boolean>
    suspend fun restoreCloudUserData(
        userId: String,
        onProgress: ((step: String, progress: Float) -> Unit)? = null
    ): Result<CloudRestoreSummary>
    suspend fun restoreCloudUserData(userId: String): Result<Boolean>
    suspend fun syncAllWithRoom(userId: String): Result<Int>
    fun observeRemoteTransactions(userId: String): Flow<List<TransactionRecord>>
}

data class CloudRestoreSummary(
    val accountsCount: Int = 0,
    val transactionsCount: Int = 0,
    val categoriesCount: Int = 0,
    val budgetsCount: Int = 0,
    val goalsCount: Int = 0,
    val debtsCount: Int = 0,
    val repaymentsCount: Int = 0,
    val vendorsCount: Int = 0,
    val shopPurchasesCount: Int = 0,
    val shopPaymentsCount: Int = 0,
    val recurringCount: Int = 0,
    val settingsRestored: Boolean = false,
    val hasCloudData: Boolean = false
) {
    val totalRecords: Int
        get() = accountsCount + transactionsCount + categoriesCount + budgetsCount +
                goalsCount + debtsCount + repaymentsCount + vendorsCount +
                shopPurchasesCount + shopPaymentsCount + recurringCount
}

class FirestoreRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase
) : FirestoreRepository {

    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
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

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore instance unavailable: ${e.message}")
            null
        }
    }

    private suspend fun <T> Task<T>.awaitTask(timeoutMs: Long = 6000L): T = withContext(Dispatchers.IO) {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
                addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
                addOnCanceledListener {
                    continuation.cancel()
                }
            }
        }
    }

    // --- Transactions ---

    override suspend fun saveTransaction(
        userId: String,
        transaction: TransactionRecord
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TRANSACTIONS)
                .document(transaction.id.toString())

            val data = hashMapOf(
                "id" to transaction.id,
                "userId" to userId,
                "accountId" to transaction.accountId,
                "categoryId" to transaction.categoryId,
                "toAccountId" to transaction.toAccountId,
                "amount" to transaction.amount,
                "fee" to transaction.fee,
                "type" to transaction.type.name,
                "dateTimestamp" to transaction.dateTimestamp,
                "note" to transaction.note,
                "receiptUri" to transaction.receiptUri,
                "exchangeRate" to transaction.exchangeRate,
                "isRecurringInstance" to transaction.isRecurringInstance,
                "recurringRuleId" to transaction.recurringRuleId,
                "createdAt" to transaction.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data, SetOptions.merge()).awaitTask()
            Log.d(TAG, "Transaction ${transaction.id} saved to Firestore successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save transaction ${transaction.id}: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(
        userId: String,
        transaction: TransactionRecord
    ): Result<Unit> = withContext(Dispatchers.IO) {
        saveTransaction(userId, transaction)
    }

    override suspend fun deleteTransaction(
        userId: String,
        transactionId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TRANSACTIONS)
                .document(transactionId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete transaction $transactionId: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun fetchTransactions(userId: String): Result<List<TransactionRecord>> =
        withContext(Dispatchers.IO) {
            val firestore = getFirestore() ?: return@withContext Result.failure(
                IllegalStateException("Firebase is not initialized")
            )
            try {
                val snapshot = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_TRANSACTIONS)
                    .get()
                    .awaitTask()

                val records = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                        val accountId = doc.getLong("accountId") ?: return@mapNotNull null
                        val amount = doc.getDouble("amount") ?: 0.0
                        val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.getString("note") ?: ""
                        val fee = doc.getDouble("fee") ?: 0.0
                        val categoryId = doc.getLong("categoryId")
                        val toAccountId = doc.getLong("toAccountId")
                        val receiptUri = doc.getString("receiptUri")
                        val exchangeRate = doc.getDouble("exchangeRate") ?: 1.0
                        val isRecurringInstance = doc.getBoolean("isRecurringInstance") ?: false
                        val recurringRuleId = doc.getLong("recurringRuleId")
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        TransactionRecord(
                            id = id,
                            userId = userId,
                            accountId = accountId,
                            categoryId = categoryId,
                            toAccountId = toAccountId,
                            amount = amount,
                            fee = fee,
                            type = type,
                            dateTimestamp = dateTimestamp,
                            note = note,
                            receiptUri = receiptUri,
                            exchangeRate = exchangeRate,
                            isRecurringInstance = isRecurringInstance,
                            recurringRuleId = recurringRuleId,
                            createdAt = createdAt
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                Result.success(records)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // --- Accounts ---

    override suspend fun saveAccount(
        userId: String,
        account: UserAccount
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val accData = hashMapOf(
                "id" to account.id,
                "userId" to userId,
                "name" to account.name,
                "type" to account.type.name,
                "currencyCode" to account.currencyCode,
                "startingBalance" to account.startingBalance,
                "colorHex" to account.colorHex,
                "iconName" to account.iconName,
                "isArchived" to account.isArchived,
                "createdAt" to account.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_ACCOUNTS)
                .document(account.id.toString())
                .set(accData, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save account ${account.id}: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(userId: String, accountId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_ACCOUNTS)
                .document(accountId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Categories ---

    override suspend fun saveCategory(userId: String, category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val catData = hashMapOf(
                "id" to category.id,
                "userId" to userId,
                "nameBn" to category.nameBn,
                "nameEn" to category.nameEn,
                "type" to category.type.name,
                "iconName" to category.iconName,
                "colorHex" to category.colorHex,
                "isDefault" to category.isDefault,
                "isArchived" to category.isArchived,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CATEGORIES)
                .document(category.id.toString())
                .set(catData, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(userId: String, categoryId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CATEGORIES)
                .document(categoryId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Budgets ---

    override suspend fun saveBudget(userId: String, budget: Budget): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to budget.id,
                "userId" to userId,
                "categoryId" to budget.categoryId,
                "monthYear" to budget.monthYear,
                "limitAmount" to budget.limitAmount,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_BUDGETS)
                .document(budget.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(userId: String, budgetId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_BUDGETS)
                .document(budgetId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Savings Goals ---

    override suspend fun saveSavingsGoal(userId: String, goal: SavingsGoal): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to goal.id,
                "userId" to userId,
                "name" to goal.name,
                "targetAmount" to goal.targetAmount,
                "currentSavedAmount" to goal.currentSavedAmount,
                "targetDate" to goal.targetDate,
                "note" to goal.note,
                "colorHex" to goal.colorHex,
                "iconName" to goal.iconName,
                "isCompleted" to goal.isCompleted,
                "createdAt" to goal.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SAVINGS_GOALS)
                .document(goal.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSavingsGoal(userId: String, goalId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SAVINGS_GOALS)
                .document(goalId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Contacts ---

    override suspend fun saveContact(userId: String, contact: Contact): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to contact.id,
                "userId" to userId,
                "name" to contact.name,
                "phone" to contact.phone,
                "createdAt" to contact.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CONTACTS)
                .document(contact.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(userId: String, contactId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CONTACTS)
                .document(contactId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Loans & Debts ---

    override suspend fun saveLoanDebt(userId: String, debt: LoanDebt): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to debt.id,
                "userId" to userId,
                "contactId" to debt.contactId,
                "personName" to debt.personName,
                "accountId" to debt.accountId,
                "amount" to debt.amount,
                "paidAmount" to debt.paidAmount,
                "type" to debt.type.name,
                "dueDate" to debt.dueDate,
                "note" to debt.note,
                "phone" to debt.phone,
                "isSettled" to debt.isSettled,
                "createdAt" to debt.createdAt,
                "transactionId" to debt.transactionId,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LOANS_DEBTS)
                .document(debt.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLoanDebt(userId: String, debtId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LOANS_DEBTS)
                .document(debtId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Loan Repayments ---

    override suspend fun saveLoanRepayment(userId: String, repayment: LoanRepayment): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to repayment.id,
                "userId" to userId,
                "loanId" to repayment.loanId,
                "accountId" to repayment.accountId,
                "amount" to repayment.amount,
                "dateTimestamp" to repayment.dateTimestamp,
                "note" to repayment.note,
                "transactionId" to repayment.transactionId,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LOAN_REPAYMENTS)
                .document(repayment.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLoanRepayment(userId: String, repaymentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LOAN_REPAYMENTS)
                .document(repaymentId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Vendors ---

    override suspend fun saveVendor(userId: String, vendor: Vendor): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to vendor.id,
                "userId" to userId,
                "name" to vendor.name,
                "phone" to vendor.phone,
                "locationNote" to vendor.locationNote,
                "categoryTag" to vendor.categoryTag,
                "isArchived" to vendor.isArchived,
                "createdAt" to vendor.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_VENDORS)
                .document(vendor.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteVendor(userId: String, vendorId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_VENDORS)
                .document(vendorId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Shop Credit Purchases ---

    override suspend fun saveShopCreditPurchase(userId: String, purchase: ShopCreditPurchase): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to purchase.id,
                "userId" to userId,
                "vendorId" to purchase.vendorId,
                "amount" to purchase.amount,
                "paidAmount" to purchase.paidAmount,
                "dateTimestamp" to purchase.dateTimestamp,
                "dueDate" to purchase.dueDate,
                "note" to purchase.note,
                "isSettled" to purchase.isSettled,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SHOP_CREDIT_PURCHASES)
                .document(purchase.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteShopCreditPurchase(userId: String, purchaseId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SHOP_CREDIT_PURCHASES)
                .document(purchaseId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Shop Credit Payments ---

    override suspend fun saveShopCreditPayment(userId: String, payment: ShopCreditPayment): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to payment.id,
                "userId" to userId,
                "vendorId" to payment.vendorId,
                "accountId" to payment.accountId,
                "amount" to payment.amount,
                "dateTimestamp" to payment.dateTimestamp,
                "note" to payment.note,
                "transactionId" to payment.transactionId,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SHOP_CREDIT_PAYMENTS)
                .document(payment.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteShopCreditPayment(userId: String, paymentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SHOP_CREDIT_PAYMENTS)
                .document(paymentId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Recurring Rules ---

    override suspend fun saveRecurringRule(userId: String, rule: RecurringRule): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "id" to rule.id,
                "userId" to userId,
                "accountId" to rule.accountId,
                "categoryId" to rule.categoryId,
                "amount" to rule.amount,
                "type" to rule.type.name,
                "frequency" to rule.frequency.name,
                "startDate" to rule.startDate,
                "endDate" to rule.endDate,
                "lastGeneratedDate" to rule.lastGeneratedDate,
                "isPaused" to rule.isPaused,
                "note" to rule.note,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_RECURRING_RULES)
                .document(rule.id.toString())
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecurringRule(userId: String, ruleId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_RECURRING_RULES)
                .document(ruleId.toString())
                .delete()
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- User Settings ---

    override suspend fun saveUserSettings(userId: String, settings: UserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val data = hashMapOf(
                "userId" to userId,
                "language" to settings.language,
                "numeralSystem" to settings.numeralSystem,
                "defaultCurrency" to settings.defaultCurrency,
                "currencySymbol" to settings.currencySymbol,
                "hideBalances" to settings.hideBalances,
                "pinEnabled" to settings.pinEnabled,
                "pinCode" to settings.pinCode,
                "dailyReminderEnabled" to settings.dailyReminderEnabled,
                "isDarkMode" to settings.isDarkMode,
                "useSystemTheme" to settings.useSystemTheme,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document(DOC_PREFERENCES)
                .set(data, SetOptions.merge())
                .awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchUserSettings(userId: String): Result<UserSettings?> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document(DOC_PREFERENCES)
                .get()
                .awaitTask()

            if (!doc.exists()) {
                return@withContext Result.success(null)
            }

            val settings = UserSettings(
                userId = userId,
                language = doc.getString("language") ?: "bn",
                numeralSystem = doc.getString("numeralSystem") ?: "bn",
                defaultCurrency = doc.getString("defaultCurrency") ?: "BDT",
                currencySymbol = doc.getString("currencySymbol") ?: "৳",
                hideBalances = doc.getBoolean("hideBalances") ?: false,
                pinEnabled = doc.getBoolean("pinEnabled") ?: false,
                pinCode = doc.getString("pinCode") ?: "",
                dailyReminderEnabled = doc.getBoolean("dailyReminderEnabled") ?: true,
                isDarkMode = doc.getBoolean("isDarkMode") ?: false,
                useSystemTheme = doc.getBoolean("useSystemTheme") ?: true
            )
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Cloud Detection & Restoration ---

    override suspend fun checkHasCloudData(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            val userDocRef = firestore.collection(COLLECTION_USERS).document(userId)

            val accountsSnap = userDocRef.collection(COLLECTION_ACCOUNTS).limit(1).get().awaitTask(4000L)
            if (!accountsSnap.isEmpty) return@withContext Result.success(true)

            val txSnap = userDocRef.collection(COLLECTION_TRANSACTIONS).limit(1).get().awaitTask(4000L)
            if (!txSnap.isEmpty) return@withContext Result.success(true)

            val catSnap = userDocRef.collection(COLLECTION_CATEGORIES).limit(1).get().awaitTask(4000L)
            if (!catSnap.isEmpty) return@withContext Result.success(true)

            val settingsDoc = userDocRef.collection(COLLECTION_SETTINGS).document(DOC_PREFERENCES).get().awaitTask(3000L)
            if (settingsDoc.exists()) return@withContext Result.success(true)

            Result.success(false)
        } catch (e: Exception) {
            Log.w(TAG, "checkHasCloudData check failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restoreCloudUserData(
        userId: String,
        onProgress: ((step: String, progress: Float) -> Unit)?
    ): Result<CloudRestoreSummary> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            onProgress?.invoke("Connecting to cloud backup...", 0.08f)
            val userDocRef = firestore.collection(COLLECTION_USERS).document(userId)

            val remoteAccountsSnap = userDocRef.collection(COLLECTION_ACCOUNTS).get().awaitTask(6000L)
            val remoteTransactionsSnap = userDocRef.collection(COLLECTION_TRANSACTIONS).limit(5).get().awaitTask(6000L)
            val remoteCategoriesSnap = userDocRef.collection(COLLECTION_CATEGORIES).get().awaitTask(6000L)

            val hasCloudData = !remoteAccountsSnap.isEmpty || !remoteTransactionsSnap.isEmpty || !remoteCategoriesSnap.isEmpty

            if (!hasCloudData) {
                val settingsDoc = userDocRef.collection(COLLECTION_SETTINGS).document(DOC_PREFERENCES).get().awaitTask(4000L)
                if (!settingsDoc.exists()) {
                    Log.i(TAG, "No cloud data found for user $userId (new user)")
                    return@withContext Result.success(CloudRestoreSummary(hasCloudData = false))
                }
            }

            Log.i(TAG, "Restoring cloud data for user $userId...")
            onProgress?.invoke("Restoring accounts and balances...", 0.18f)

            // 1. Restore Accounts
            val remoteAccounts = remoteAccountsSnap.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Account"
                val typeStr = doc.getString("type") ?: AccountType.CASH.name
                val type = try { AccountType.valueOf(typeStr) } catch (e: Exception) { AccountType.CASH }
                val currencyCode = doc.getString("currencyCode") ?: "BDT"
                val startingBalance = doc.getDouble("startingBalance") ?: 0.0
                val colorHex = doc.getString("colorHex") ?: "#0F766E"
                val iconName = doc.getString("iconName") ?: "payments"
                val isArchived = doc.getBoolean("isArchived") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                UserAccount(
                    id = id,
                    userId = userId,
                    name = name,
                    type = type,
                    currencyCode = currencyCode,
                    startingBalance = startingBalance,
                    colorHex = colorHex,
                    iconName = iconName,
                    isArchived = isArchived,
                    createdAt = createdAt
                )
            }
            if (remoteAccounts.isNotEmpty()) {
                val localTxCount = transactionDao.getAllTransactionsList(userId).size
                if (localTxCount == 0) {
                    accountDao.deleteAllAccountsForUser(userId)
                }
                accountDao.insertAccounts(remoteAccounts)
            }

            onProgress?.invoke("Restoring categories...", 0.30f)

            // 2. Restore Categories
            val remoteCategories = remoteCategoriesSnap.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val nameBn = doc.getString("nameBn") ?: ""
                val nameEn = doc.getString("nameEn") ?: ""
                val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                val iconName = doc.getString("iconName") ?: "category"
                val colorHex = doc.getString("colorHex") ?: "#64748B"
                val isDefault = doc.getBoolean("isDefault") ?: false
                val isArchived = doc.getBoolean("isArchived") ?: false

                Category(
                    id = id,
                    userId = userId,
                    nameBn = nameBn,
                    nameEn = nameEn,
                    type = type,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = isDefault,
                    isArchived = isArchived
                )
            }
            if (remoteCategories.isNotEmpty()) {
                val localTxCount = transactionDao.getAllTransactionsList(userId).size
                if (localTxCount == 0) {
                    categoryDao.deleteAllCategoriesForUser(userId)
                }
                categoryDao.insertCategories(remoteCategories)
            }

            onProgress?.invoke("Restoring transactions...", 0.50f)

            // 3. Restore Transactions (full collection)
            val fullTxSnap = userDocRef.collection(COLLECTION_TRANSACTIONS).get().awaitTask(8000L)
            val remoteTransactions = fullTxSnap.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val accountId = doc.getLong("accountId") ?: return@mapNotNull null
                val amount = doc.getDouble("amount") ?: 0.0
                val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                val note = doc.getString("note") ?: ""
                val fee = doc.getDouble("fee") ?: 0.0
                val categoryId = doc.getLong("categoryId")
                val toAccountId = doc.getLong("toAccountId")
                val receiptUri = doc.getString("receiptUri")
                val exchangeRate = doc.getDouble("exchangeRate") ?: 1.0
                val isRecurringInstance = doc.getBoolean("isRecurringInstance") ?: false
                val recurringRuleId = doc.getLong("recurringRuleId")
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                TransactionRecord(
                    id = id,
                    userId = userId,
                    accountId = accountId,
                    categoryId = categoryId,
                    toAccountId = toAccountId,
                    amount = amount,
                    fee = fee,
                    type = type,
                    dateTimestamp = dateTimestamp,
                    note = note,
                    receiptUri = receiptUri,
                    exchangeRate = exchangeRate,
                    isRecurringInstance = isRecurringInstance,
                    recurringRuleId = recurringRuleId,
                    createdAt = createdAt
                )
            }
            if (remoteTransactions.isNotEmpty()) {
                transactionDao.insertTransactions(remoteTransactions)
            }

            onProgress?.invoke("Restoring budgets and savings goals...", 0.68f)

            // 4. Restore Budgets
            var budgetsCount = 0
            try {
                val budgetSnap = userDocRef.collection(COLLECTION_BUDGETS).get().awaitTask(5000L)
                val budgets = budgetSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val categoryId = doc.getLong("categoryId")
                    val monthYear = doc.getString("monthYear") ?: return@mapNotNull null
                    val limitAmount = doc.getDouble("limitAmount") ?: 0.0
                    Budget(
                        id = id,
                        userId = userId,
                        categoryId = categoryId,
                        monthYear = monthYear,
                        limitAmount = limitAmount
                    )
                }
                if (budgets.isNotEmpty()) {
                    budgetDao.insertBudgets(budgets)
                    budgetsCount = budgets.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring budgets failed: ${e.message}")
            }

            // 5. Restore Savings Goals
            var goalsCount = 0
            try {
                val goalSnap = userDocRef.collection(COLLECTION_SAVINGS_GOALS).get().awaitTask(5000L)
                val goals = goalSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val targetAmount = doc.getDouble("targetAmount") ?: 0.0
                    val currentSavedAmount = doc.getDouble("currentSavedAmount") ?: 0.0
                    val targetDate = doc.getLong("targetDate")
                    val note = doc.getString("note") ?: ""
                    val colorHex = doc.getString("colorHex") ?: "#0D9488"
                    val iconName = doc.getString("iconName") ?: "savings"
                    val isCompleted = doc.getBoolean("isCompleted") ?: false
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    SavingsGoal(
                        id = id,
                        userId = userId,
                        name = name,
                        targetAmount = targetAmount,
                        currentSavedAmount = currentSavedAmount,
                        targetDate = targetDate,
                        note = note,
                        colorHex = colorHex,
                        iconName = iconName,
                        isCompleted = isCompleted,
                        createdAt = createdAt
                    )
                }
                if (goals.isNotEmpty()) {
                    savingsGoalDao.insertGoals(goals)
                    goalsCount = goals.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring savings goals failed: ${e.message}")
            }

            onProgress?.invoke("Restoring loans, debts and shops...", 0.82f)

            // 6. Restore Contacts
            var contactsCount = 0
            try {
                val contactSnap = userDocRef.collection(COLLECTION_CONTACTS).get().awaitTask(5000L)
                val contacts = contactSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    Contact(
                        id = id,
                        userId = userId,
                        name = name,
                        phone = phone,
                        createdAt = createdAt
                    )
                }
                if (contacts.isNotEmpty()) {
                    contactDao.insertContacts(contacts)
                    contactsCount = contacts.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring contacts failed: ${e.message}")
            }

            // 7. Restore Loans & Debts
            var debtsCount = 0
            try {
                val debtSnap = userDocRef.collection(COLLECTION_LOANS_DEBTS).get().awaitTask(5000L)
                val debts = debtSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val contactId = doc.getLong("contactId") ?: 0L
                    val personName = doc.getString("personName") ?: ""
                    val accountId = doc.getLong("accountId") ?: 1L
                    val amount = doc.getDouble("amount") ?: 0.0
                    val paidAmount = doc.getDouble("paidAmount") ?: 0.0
                    val typeStr = doc.getString("type") ?: DebtType.OWED_TO_ME.name
                    val type = try { DebtType.valueOf(typeStr) } catch (e: Exception) { DebtType.OWED_TO_ME }
                    val dueDate = doc.getLong("dueDate")
                    val note = doc.getString("note") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val isSettled = doc.getBoolean("isSettled") ?: false
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val transactionId = doc.getLong("transactionId")
                    LoanDebt(
                        id = id,
                        userId = userId,
                        contactId = contactId,
                        personName = personName,
                        accountId = accountId,
                        amount = amount,
                        paidAmount = paidAmount,
                        type = type,
                        dueDate = dueDate,
                        note = note,
                        phone = phone,
                        isSettled = isSettled,
                        createdAt = createdAt,
                        transactionId = transactionId
                    )
                }
                if (debts.isNotEmpty()) {
                    loanDebtDao.insertDebts(debts)
                    debtsCount = debts.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring loans & debts failed: ${e.message}")
            }

            // 8. Restore Loan Repayments
            var repaymentsCount = 0
            try {
                val repaymentSnap = userDocRef.collection(COLLECTION_LOAN_REPAYMENTS).get().awaitTask(5000L)
                val repayments = repaymentSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val loanId = doc.getLong("loanId") ?: return@mapNotNull null
                    val accountId = doc.getLong("accountId") ?: 1L
                    val amount = doc.getDouble("amount") ?: 0.0
                    val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    val transactionId = doc.getLong("transactionId")
                    LoanRepayment(
                        id = id,
                        userId = userId,
                        loanId = loanId,
                        accountId = accountId,
                        amount = amount,
                        dateTimestamp = dateTimestamp,
                        note = note,
                        transactionId = transactionId
                    )
                }
                if (repayments.isNotEmpty()) {
                    loanRepaymentDao.insertRepayments(repayments)
                    repaymentsCount = repayments.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring repayments failed: ${e.message}")
            }

            // 9. Restore Vendors
            var vendorsCount = 0
            try {
                val vendorSnap = userDocRef.collection(COLLECTION_VENDORS).get().awaitTask(5000L)
                val vendors = vendorSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val locationNote = doc.getString("locationNote") ?: ""
                    val categoryTag = doc.getString("categoryTag") ?: ""
                    val isArchived = doc.getBoolean("isArchived") ?: false
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    Vendor(
                        id = id,
                        userId = userId,
                        name = name,
                        phone = phone,
                        locationNote = locationNote,
                        categoryTag = categoryTag,
                        isArchived = isArchived,
                        createdAt = createdAt
                    )
                }
                if (vendors.isNotEmpty()) {
                    vendorDao.insertVendors(vendors)
                    vendorsCount = vendors.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring vendors failed: ${e.message}")
            }

            // 10. Restore Shop Credit Purchases
            var shopPurchasesCount = 0
            try {
                val purchaseSnap = userDocRef.collection(COLLECTION_SHOP_CREDIT_PURCHASES).get().awaitTask(5000L)
                val purchases = purchaseSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val vendorId = doc.getLong("vendorId") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: 0.0
                    val paidAmount = doc.getDouble("paidAmount") ?: 0.0
                    val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                    val dueDate = doc.getLong("dueDate")
                    val note = doc.getString("note") ?: ""
                    val isSettled = doc.getBoolean("isSettled") ?: false
                    ShopCreditPurchase(
                        id = id,
                        userId = userId,
                        vendorId = vendorId,
                        amount = amount,
                        paidAmount = paidAmount,
                        dateTimestamp = dateTimestamp,
                        dueDate = dueDate,
                        note = note,
                        isSettled = isSettled
                    )
                }
                if (purchases.isNotEmpty()) {
                    shopCreditPurchaseDao.insertPurchases(purchases)
                    shopPurchasesCount = purchases.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring purchases failed: ${e.message}")
            }

            // 11. Restore Shop Credit Payments
            var shopPaymentsCount = 0
            try {
                val paymentSnap = userDocRef.collection(COLLECTION_SHOP_CREDIT_PAYMENTS).get().awaitTask(5000L)
                val payments = paymentSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val vendorId = doc.getLong("vendorId") ?: return@mapNotNull null
                    val accountId = doc.getLong("accountId") ?: 1L
                    val amount = doc.getDouble("amount") ?: 0.0
                    val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    val transactionId = doc.getLong("transactionId")
                    ShopCreditPayment(
                        id = id,
                        userId = userId,
                        vendorId = vendorId,
                        accountId = accountId,
                        amount = amount,
                        dateTimestamp = dateTimestamp,
                        note = note,
                        transactionId = transactionId
                    )
                }
                if (payments.isNotEmpty()) {
                    shopCreditPaymentDao.insertPayments(payments)
                    shopPaymentsCount = payments.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring payments failed: ${e.message}")
            }

            // 12. Restore Recurring Rules
            var recurringCount = 0
            try {
                val ruleSnap = userDocRef.collection(COLLECTION_RECURRING_RULES).get().awaitTask(5000L)
                val rules = ruleSnap.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val accountId = doc.getLong("accountId") ?: 1L
                    val categoryId = doc.getLong("categoryId")
                    val amount = doc.getDouble("amount") ?: 0.0
                    val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                    val freqStr = doc.getString("frequency") ?: Frequency.MONTHLY.name
                    val frequency = try { Frequency.valueOf(freqStr) } catch (e: Exception) { Frequency.MONTHLY }
                    val startDate = doc.getLong("startDate") ?: System.currentTimeMillis()
                    val endDate = doc.getLong("endDate")
                    val lastGeneratedDate = doc.getLong("lastGeneratedDate") ?: 0L
                    val isPaused = doc.getBoolean("isPaused") ?: false
                    val note = doc.getString("note") ?: ""
                    RecurringRule(
                        id = id,
                        userId = userId,
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        type = type,
                        frequency = frequency,
                        startDate = startDate,
                        endDate = endDate,
                        lastGeneratedDate = lastGeneratedDate,
                        isPaused = isPaused,
                        note = note
                    )
                }
                if (rules.isNotEmpty()) {
                    recurringDao.insertRules(rules)
                    recurringCount = rules.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring recurring rules failed: ${e.message}")
            }

            onProgress?.invoke("Applying settings and preferences...", 0.94f)

            // 13. Restore User Preferences / Settings
            var settingsRestored = false
            try {
                val settingsDoc = userDocRef.collection(COLLECTION_SETTINGS).document(DOC_PREFERENCES).get().awaitTask(4000L)
                if (settingsDoc.exists()) {
                    val lang = settingsDoc.getString("language") ?: "bn"
                    val numeral = settingsDoc.getString("numeralSystem") ?: "bn"
                    val currency = settingsDoc.getString("defaultCurrency") ?: "BDT"
                    val symbol = settingsDoc.getString("currencySymbol") ?: "৳"
                    val hideBal = settingsDoc.getBoolean("hideBalances") ?: false
                    val pinEn = settingsDoc.getBoolean("pinEnabled") ?: false
                    val pin = settingsDoc.getString("pinCode") ?: ""
                    val dailyReminder = settingsDoc.getBoolean("dailyReminderEnabled") ?: true
                    val dark = settingsDoc.getBoolean("isDarkMode") ?: false
                    val systemTheme = settingsDoc.getBoolean("useSystemTheme") ?: true

                    val restoredSettings = UserSettings(
                        userId = userId,
                        language = lang,
                        numeralSystem = numeral,
                        defaultCurrency = currency,
                        currencySymbol = symbol,
                        hideBalances = hideBal,
                        pinEnabled = pinEn,
                        pinCode = pin,
                        dailyReminderEnabled = dailyReminder,
                        isDarkMode = dark,
                        useSystemTheme = systemTheme
                    )
                    settingsDao.insertOrUpdate(restoredSettings)

                    val prefs = context.getSharedPreferences("hisab_settings_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("language", lang)
                        .putString("numeral_system", numeral)
                        .putBoolean("is_dark_mode", dark)
                        .putBoolean("use_system_theme", systemTheme)
                        .apply()
                    settingsRestored = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Restoring user settings failed: ${e.message}")
            }

            onProgress?.invoke("Restoration complete!", 1.0f)
            Log.i(TAG, "Successfully restored all cloud data for user $userId")

            val summary = CloudRestoreSummary(
                accountsCount = remoteAccounts.size,
                transactionsCount = remoteTransactions.size,
                categoriesCount = remoteCategories.size,
                budgetsCount = budgetsCount,
                goalsCount = goalsCount,
                debtsCount = debtsCount,
                repaymentsCount = repaymentsCount,
                vendorsCount = vendorsCount,
                shopPurchasesCount = shopPurchasesCount,
                shopPaymentsCount = shopPaymentsCount,
                recurringCount = recurringCount,
                settingsRestored = settingsRestored,
                hasCloudData = true
            )
            Result.success(summary)
        } catch (e: Exception) {
            Log.w(TAG, "restoreCloudUserData failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun restoreCloudUserData(userId: String): Result<Boolean> {
        val result = restoreCloudUserData(userId, null)
        return if (result.isSuccess) {
            Result.success(result.getOrNull()?.hasCloudData == true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Restore failed"))
        }
    }

    // --- Master Bidirectional Synchronization ---

    override suspend fun syncAllWithRoom(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            var syncedCount = 0

            // 1. Sync Accounts
            syncedCount += syncAccounts(userId, firestore)

            // 2. Sync Categories
            syncedCount += syncCategories(userId, firestore)

            // 3. Sync Transactions
            syncedCount += syncTransactions(userId, firestore)

            // 4. Sync Budgets
            syncedCount += syncBudgets(userId, firestore)

            // 5. Sync Savings Goals
            syncedCount += syncSavingsGoals(userId, firestore)

            // 6. Sync Contacts
            syncedCount += syncContacts(userId, firestore)

            // 7. Sync Loans & Debts
            syncedCount += syncLoansDebts(userId, firestore)

            // 8. Sync Loan Repayments
            syncedCount += syncLoanRepayments(userId, firestore)

            // 9. Sync Vendors
            syncedCount += syncVendors(userId, firestore)

            // 10. Sync Shop Credit Purchases & Payments
            syncedCount += syncShopCredit(userId, firestore)

            // 11. Sync Recurring Rules
            syncedCount += syncRecurringRules(userId, firestore)

            // 12. Sync User Preferences / Settings
            syncUserSettings(userId, firestore)

            Log.i(TAG, "syncAllWithRoom complete for $userId. Total items synchronized: $syncedCount")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.w(TAG, "syncAllWithRoom failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncAccounts(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_ACCOUNTS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteAccounts = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.getString("name") ?: "Account"
                val typeStr = doc.getString("type") ?: AccountType.CASH.name
                val type = try { AccountType.valueOf(typeStr) } catch (e: Exception) { AccountType.CASH }
                val currencyCode = doc.getString("currencyCode") ?: "BDT"
                val startingBalance = doc.getDouble("startingBalance") ?: 0.0
                val colorHex = doc.getString("colorHex") ?: "#0F766E"
                val iconName = doc.getString("iconName") ?: "payments"
                val isArchived = doc.getBoolean("isArchived") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                UserAccount(
                    id = id,
                    userId = userId,
                    name = name,
                    type = type,
                    currencyCode = currencyCode,
                    startingBalance = startingBalance,
                    colorHex = colorHex,
                    iconName = iconName,
                    isArchived = isArchived,
                    createdAt = createdAt
                )
            } ?: emptyList()

            if (remoteAccounts.isNotEmpty()) {
                accountDao.insertAccounts(remoteAccounts)
                count += remoteAccounts.size
            }

            val localAccounts = accountDao.getAllAccountsList(userId)
            val remoteIds = remoteAccounts.map { it.id }.toSet()
            for (acc in localAccounts) {
                if (!remoteIds.contains(acc.id)) {
                    saveAccount(userId, acc)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncAccounts error: ${e.message}")
        }
        return count
    }

    private suspend fun syncCategories(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_CATEGORIES)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteCats = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val nameBn = doc.getString("nameBn") ?: ""
                val nameEn = doc.getString("nameEn") ?: ""
                val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                val iconName = doc.getString("iconName") ?: "category"
                val colorHex = doc.getString("colorHex") ?: "#64748B"
                val isDefault = doc.getBoolean("isDefault") ?: false
                val isArchived = doc.getBoolean("isArchived") ?: false

                Category(
                    id = id,
                    userId = userId,
                    nameBn = nameBn,
                    nameEn = nameEn,
                    type = type,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = isDefault,
                    isArchived = isArchived
                )
            } ?: emptyList()

            if (remoteCats.isNotEmpty()) {
                categoryDao.insertCategories(remoteCats)
                count += remoteCats.size
            }

            val localCats = categoryDao.getAllCategoriesList(userId)
            val remoteIds = remoteCats.map { it.id }.toSet()
            for (cat in localCats) {
                if (!remoteIds.contains(cat.id)) {
                    saveCategory(userId, cat)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncCategories error: ${e.message}")
        }
        return count
    }

    private suspend fun syncTransactions(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_TRANSACTIONS)
            val remoteSnap = try { collection.get().awaitTask(7000L) } catch (e: Exception) { null }

            val remoteMap = mutableMapOf<Long, TransactionRecord>()
            if (remoteSnap != null) {
                for (doc in remoteSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                    val accountId = doc.getLong("accountId") ?: continue
                    val amount = doc.getDouble("amount") ?: 0.0
                    val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                    val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                    val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    val fee = doc.getDouble("fee") ?: 0.0
                    val categoryId = doc.getLong("categoryId")
                    val toAccountId = doc.getLong("toAccountId")
                    val receiptUri = doc.getString("receiptUri")
                    val exchangeRate = doc.getDouble("exchangeRate") ?: 1.0
                    val isRecurringInstance = doc.getBoolean("isRecurringInstance") ?: false
                    val recurringRuleId = doc.getLong("recurringRuleId")
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    remoteMap[id] = TransactionRecord(
                        id = id,
                        userId = userId,
                        accountId = accountId,
                        categoryId = categoryId,
                        toAccountId = toAccountId,
                        amount = amount,
                        fee = fee,
                        type = type,
                        dateTimestamp = dateTimestamp,
                        note = note,
                        receiptUri = receiptUri,
                        exchangeRate = exchangeRate,
                        isRecurringInstance = isRecurringInstance,
                        recurringRuleId = recurringRuleId,
                        createdAt = createdAt
                    )
                }

                if (remoteMap.isNotEmpty()) {
                    transactionDao.insertTransactions(remoteMap.values.toList())
                    count += remoteMap.size
                }
            }

            val localTransactions = transactionDao.getAllTransactionsList(userId)
            for (localTx in localTransactions) {
                if (!remoteMap.containsKey(localTx.id)) {
                    saveTransaction(userId, localTx)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncTransactions error: ${e.message}")
        }
        return count
    }

    private suspend fun syncBudgets(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_BUDGETS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteBudgets = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val categoryId = doc.getLong("categoryId")
                val monthYear = doc.getString("monthYear") ?: return@mapNotNull null
                val limitAmount = doc.getDouble("limitAmount") ?: 0.0
                Budget(
                    id = id,
                    userId = userId,
                    categoryId = categoryId,
                    monthYear = monthYear,
                    limitAmount = limitAmount
                )
            } ?: emptyList()

            if (remoteBudgets.isNotEmpty()) {
                budgetDao.insertBudgets(remoteBudgets)
                count += remoteBudgets.size
            }

            val localBudgets = budgetDao.getAllBudgetsList(userId)
            val remoteIds = remoteBudgets.map { it.id }.toSet()
            for (b in localBudgets) {
                if (!remoteIds.contains(b.id)) {
                    saveBudget(userId, b)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncBudgets error: ${e.message}")
        }
        return count
    }

    private suspend fun syncSavingsGoals(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_SAVINGS_GOALS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteGoals = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val targetAmount = doc.getDouble("targetAmount") ?: 0.0
                val currentSavedAmount = doc.getDouble("currentSavedAmount") ?: 0.0
                val targetDate = doc.getLong("targetDate")
                val note = doc.getString("note") ?: ""
                val colorHex = doc.getString("colorHex") ?: "#0D9488"
                val iconName = doc.getString("iconName") ?: "savings"
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                SavingsGoal(
                    id = id,
                    userId = userId,
                    name = name,
                    targetAmount = targetAmount,
                    currentSavedAmount = currentSavedAmount,
                    targetDate = targetDate,
                    note = note,
                    colorHex = colorHex,
                    iconName = iconName,
                    isCompleted = isCompleted,
                    createdAt = createdAt
                )
            } ?: emptyList()

            if (remoteGoals.isNotEmpty()) {
                savingsGoalDao.insertGoals(remoteGoals)
                count += remoteGoals.size
            }

            val localGoals = savingsGoalDao.getAllGoalsList(userId)
            val remoteIds = remoteGoals.map { it.id }.toSet()
            for (g in localGoals) {
                if (!remoteIds.contains(g.id)) {
                    saveSavingsGoal(userId, g)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncSavingsGoals error: ${e.message}")
        }
        return count
    }

    private suspend fun syncContacts(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_CONTACTS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteContacts = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val phone = doc.getString("phone") ?: ""
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                Contact(id = id, userId = userId, name = name, phone = phone, createdAt = createdAt)
            } ?: emptyList()

            if (remoteContacts.isNotEmpty()) {
                contactDao.insertContacts(remoteContacts)
                count += remoteContacts.size
            }

            val localContacts = contactDao.getAllContactsList(userId)
            val remoteIds = remoteContacts.map { it.id }.toSet()
            for (c in localContacts) {
                if (!remoteIds.contains(c.id)) {
                    saveContact(userId, c)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncContacts error: ${e.message}")
        }
        return count
    }

    private suspend fun syncLoansDebts(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_LOANS_DEBTS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteDebts = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val contactId = doc.getLong("contactId") ?: 0L
                val personName = doc.getString("personName") ?: ""
                val accountId = doc.getLong("accountId") ?: 1L
                val amount = doc.getDouble("amount") ?: 0.0
                val paidAmount = doc.getDouble("paidAmount") ?: 0.0
                val typeStr = doc.getString("type") ?: DebtType.OWED_TO_ME.name
                val type = try { DebtType.valueOf(typeStr) } catch (e: Exception) { DebtType.OWED_TO_ME }
                val dueDate = doc.getLong("dueDate")
                val note = doc.getString("note") ?: ""
                val phone = doc.getString("phone") ?: ""
                val isSettled = doc.getBoolean("isSettled") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val transactionId = doc.getLong("transactionId")
                LoanDebt(
                    id = id,
                    userId = userId,
                    contactId = contactId,
                    personName = personName,
                    accountId = accountId,
                    amount = amount,
                    paidAmount = paidAmount,
                    type = type,
                    dueDate = dueDate,
                    note = note,
                    phone = phone,
                    isSettled = isSettled,
                    createdAt = createdAt,
                    transactionId = transactionId
                )
            } ?: emptyList()

            if (remoteDebts.isNotEmpty()) {
                loanDebtDao.insertDebts(remoteDebts)
                count += remoteDebts.size
            }

            val localDebts = loanDebtDao.getAllDebtsList(userId)
            val remoteIds = remoteDebts.map { it.id }.toSet()
            for (d in localDebts) {
                if (!remoteIds.contains(d.id)) {
                    saveLoanDebt(userId, d)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncLoansDebts error: ${e.message}")
        }
        return count
    }

    private suspend fun syncLoanRepayments(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_LOAN_REPAYMENTS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteRepayments = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val loanId = doc.getLong("loanId") ?: return@mapNotNull null
                val accountId = doc.getLong("accountId") ?: 1L
                val amount = doc.getDouble("amount") ?: 0.0
                val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                val note = doc.getString("note") ?: ""
                val transactionId = doc.getLong("transactionId")
                LoanRepayment(
                    id = id,
                    userId = userId,
                    loanId = loanId,
                    accountId = accountId,
                    amount = amount,
                    dateTimestamp = dateTimestamp,
                    note = note,
                    transactionId = transactionId
                )
            } ?: emptyList()

            if (remoteRepayments.isNotEmpty()) {
                loanRepaymentDao.insertRepayments(remoteRepayments)
                count += remoteRepayments.size
            }

            val localRepayments = loanRepaymentDao.getAllRepaymentsList(userId)
            val remoteIds = remoteRepayments.map { it.id }.toSet()
            for (r in localRepayments) {
                if (!remoteIds.contains(r.id)) {
                    saveLoanRepayment(userId, r)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncLoanRepayments error: ${e.message}")
        }
        return count
    }

    private suspend fun syncVendors(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_VENDORS)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteVendors = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val phone = doc.getString("phone") ?: ""
                val locationNote = doc.getString("locationNote") ?: ""
                val categoryTag = doc.getString("categoryTag") ?: ""
                val isArchived = doc.getBoolean("isArchived") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                Vendor(
                    id = id,
                    userId = userId,
                    name = name,
                    phone = phone,
                    locationNote = locationNote,
                    categoryTag = categoryTag,
                    isArchived = isArchived,
                    createdAt = createdAt
                )
            } ?: emptyList()

            if (remoteVendors.isNotEmpty()) {
                vendorDao.insertVendors(remoteVendors)
                count += remoteVendors.size
            }

            val localVendors = vendorDao.getAllVendorsList(userId)
            val remoteIds = remoteVendors.map { it.id }.toSet()
            for (v in localVendors) {
                if (!remoteIds.contains(v.id)) {
                    saveVendor(userId, v)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncVendors error: ${e.message}")
        }
        return count
    }

    private suspend fun syncShopCredit(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            // Purchases
            val pColl = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_SHOP_CREDIT_PURCHASES)
            val pSnap = try { pColl.get().awaitTask(5000L) } catch (e: Exception) { null }
            val remotePurchases = pSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val vendorId = doc.getLong("vendorId") ?: return@mapNotNull null
                val amount = doc.getDouble("amount") ?: 0.0
                val paidAmount = doc.getDouble("paidAmount") ?: 0.0
                val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                val dueDate = doc.getLong("dueDate")
                val note = doc.getString("note") ?: ""
                val isSettled = doc.getBoolean("isSettled") ?: false
                ShopCreditPurchase(
                    id = id,
                    userId = userId,
                    vendorId = vendorId,
                    amount = amount,
                    paidAmount = paidAmount,
                    dateTimestamp = dateTimestamp,
                    dueDate = dueDate,
                    note = note,
                    isSettled = isSettled
                )
            } ?: emptyList()

            if (remotePurchases.isNotEmpty()) {
                shopCreditPurchaseDao.insertPurchases(remotePurchases)
                count += remotePurchases.size
            }

            val localPurchases = shopCreditPurchaseDao.getAllPurchasesList(userId)
            val rPIds = remotePurchases.map { it.id }.toSet()
            for (p in localPurchases) {
                if (!rPIds.contains(p.id)) {
                    saveShopCreditPurchase(userId, p)
                    count++
                }
            }

            // Payments
            val payColl = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_SHOP_CREDIT_PAYMENTS)
            val paySnap = try { payColl.get().awaitTask(5000L) } catch (e: Exception) { null }
            val remotePayments = paySnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val vendorId = doc.getLong("vendorId") ?: return@mapNotNull null
                val accountId = doc.getLong("accountId") ?: 1L
                val amount = doc.getDouble("amount") ?: 0.0
                val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                val note = doc.getString("note") ?: ""
                val transactionId = doc.getLong("transactionId")
                ShopCreditPayment(
                    id = id,
                    userId = userId,
                    vendorId = vendorId,
                    accountId = accountId,
                    amount = amount,
                    dateTimestamp = dateTimestamp,
                    note = note,
                    transactionId = transactionId
                )
            } ?: emptyList()

            if (remotePayments.isNotEmpty()) {
                shopCreditPaymentDao.insertPayments(remotePayments)
                count += remotePayments.size
            }

            val localPayments = shopCreditPaymentDao.getAllPaymentsList(userId)
            val rPayIds = remotePayments.map { it.id }.toSet()
            for (p in localPayments) {
                if (!rPayIds.contains(p.id)) {
                    saveShopCreditPayment(userId, p)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncShopCredit error: ${e.message}")
        }
        return count
    }

    private suspend fun syncRecurringRules(userId: String, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val collection = firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_RECURRING_RULES)
            val remoteSnap = try { collection.get().awaitTask(5000L) } catch (e: Exception) { null }

            val remoteRules = remoteSnap?.documents?.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val accountId = doc.getLong("accountId") ?: 1L
                val categoryId = doc.getLong("categoryId")
                val amount = doc.getDouble("amount") ?: 0.0
                val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                val freqStr = doc.getString("frequency") ?: Frequency.MONTHLY.name
                val frequency = try { Frequency.valueOf(freqStr) } catch (e: Exception) { Frequency.MONTHLY }
                val startDate = doc.getLong("startDate") ?: System.currentTimeMillis()
                val endDate = doc.getLong("endDate")
                val lastGeneratedDate = doc.getLong("lastGeneratedDate") ?: 0L
                val isPaused = doc.getBoolean("isPaused") ?: false
                val note = doc.getString("note") ?: ""
                RecurringRule(
                    id = id,
                    userId = userId,
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    lastGeneratedDate = lastGeneratedDate,
                    isPaused = isPaused,
                    note = note
                )
            } ?: emptyList()

            if (remoteRules.isNotEmpty()) {
                recurringDao.insertRules(remoteRules)
                count += remoteRules.size
            }

            val localRules = recurringDao.getAllRulesList(userId)
            val remoteIds = remoteRules.map { it.id }.toSet()
            for (r in localRules) {
                if (!remoteIds.contains(r.id)) {
                    saveRecurringRule(userId, r)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncRecurringRules error: ${e.message}")
        }
        return count
    }

    private suspend fun syncUserSettings(userId: String, firestore: FirebaseFirestore) {
        try {
            val settingsDoc = try {
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_SETTINGS)
                    .document(DOC_PREFERENCES)
                    .get()
                    .awaitTask(4000L)
            } catch (e: Exception) {
                null
            }

            val local = settingsDao.getSettingsDirect(userId)
            if (settingsDoc != null && settingsDoc.exists()) {
                val lang = settingsDoc.getString("language") ?: local?.language ?: "bn"
                val numeral = settingsDoc.getString("numeralSystem") ?: local?.numeralSystem ?: "bn"
                val currency = settingsDoc.getString("defaultCurrency") ?: local?.defaultCurrency ?: "BDT"
                val symbol = settingsDoc.getString("currencySymbol") ?: local?.currencySymbol ?: "৳"
                val hideBal = settingsDoc.getBoolean("hideBalances") ?: local?.hideBalances ?: false
                val pinEn = settingsDoc.getBoolean("pinEnabled") ?: local?.pinEnabled ?: false
                val pin = settingsDoc.getString("pinCode") ?: local?.pinCode ?: ""
                val dailyReminder = settingsDoc.getBoolean("dailyReminderEnabled") ?: local?.dailyReminderEnabled ?: true
                val dark = settingsDoc.getBoolean("isDarkMode") ?: local?.isDarkMode ?: false
                val systemTheme = settingsDoc.getBoolean("useSystemTheme") ?: local?.useSystemTheme ?: true

                val updatedSettings = UserSettings(
                    userId = userId,
                    language = lang,
                    numeralSystem = numeral,
                    defaultCurrency = currency,
                    currencySymbol = symbol,
                    hideBalances = hideBal,
                    pinEnabled = pinEn,
                    pinCode = pin,
                    dailyReminderEnabled = dailyReminder,
                    isDarkMode = dark,
                    useSystemTheme = systemTheme
                )
                settingsDao.insertOrUpdate(updatedSettings)

                val prefs = context.getSharedPreferences("hisab_settings_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("language", lang)
                    .putString("numeral_system", numeral)
                    .putBoolean("is_dark_mode", dark)
                    .putBoolean("use_system_theme", systemTheme)
                    .apply()
            } else if (local != null) {
                saveUserSettings(userId, local)
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncUserSettings error: ${e.message}")
        }
    }

    override fun observeRemoteTransactions(userId: String): Flow<List<TransactionRecord>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            close(IllegalStateException("Firebase is not initialized"))
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_TRANSACTIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore observeRemoteTransactions error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                        val accountId = doc.getLong("accountId") ?: return@mapNotNull null
                        val amount = doc.getDouble("amount") ?: 0.0
                        val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                        val type = try {
                            TransactionType.valueOf(typeStr)
                        } catch (e: Exception) {
                            TransactionType.EXPENSE
                        }
                        val dateTimestamp = doc.getLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.getString("note") ?: ""
                        val fee = doc.getDouble("fee") ?: 0.0
                        val categoryId = doc.getLong("categoryId")
                        val toAccountId = doc.getLong("toAccountId")
                        val receiptUri = doc.getString("receiptUri")
                        val exchangeRate = doc.getDouble("exchangeRate") ?: 1.0
                        val isRecurringInstance = doc.getBoolean("isRecurringInstance") ?: false
                        val recurringRuleId = doc.getLong("recurringRuleId")
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        TransactionRecord(
                            id = id,
                            userId = userId,
                            accountId = accountId,
                            categoryId = categoryId,
                            toAccountId = toAccountId,
                            amount = amount,
                            fee = fee,
                            type = type,
                            dateTimestamp = dateTimestamp,
                            note = note,
                            receiptUri = receiptUri,
                            exchangeRate = exchangeRate,
                            isRecurringInstance = isRecurringInstance,
                            recurringRuleId = recurringRuleId,
                            createdAt = createdAt
                        )
                    }
                    trySend(records)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TRANSACTIONS = "transactions"
        private const val COLLECTION_ACCOUNTS = "accounts"
        private const val COLLECTION_CATEGORIES = "categories"
        private const val COLLECTION_BUDGETS = "budgets"
        private const val COLLECTION_SAVINGS_GOALS = "savings_goals"
        private const val COLLECTION_CONTACTS = "contacts"
        private const val COLLECTION_LOANS_DEBTS = "loans_debts"
        private const val COLLECTION_LOAN_REPAYMENTS = "loan_repayments"
        private const val COLLECTION_VENDORS = "vendors"
        private const val COLLECTION_SHOP_CREDIT_PURCHASES = "shop_credit_purchases"
        private const val COLLECTION_SHOP_CREDIT_PAYMENTS = "shop_credit_payments"
        private const val COLLECTION_RECURRING_RULES = "recurring_rules"
        private const val COLLECTION_SETTINGS = "settings"
        private const val DOC_PREFERENCES = "preferences"
    }
}
