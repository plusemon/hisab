package com.plusemon.hisab.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
import com.plusemon.hisab.data.model.User
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

    private val userDao = db.userDao()
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

    private suspend fun <T> Task<T>.awaitTask(timeoutMs: Long = 10000L): T = withContext(Dispatchers.IO) {
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

    private fun DocumentSnapshot.safeLong(field: String): Long? {
        return try {
            when (val raw = get(field)) {
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull() ?: raw.toDoubleOrNull()?.toLong()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.safeDouble(field: String): Double? {
        return try {
            when (val raw = get(field)) {
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.safeBoolean(field: String): Boolean? {
        return try {
            when (val raw = get(field)) {
                is Boolean -> raw
                is String -> raw.toBooleanStrictOrNull()
                is Number -> raw.toInt() != 0
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.safeString(field: String): String? {
        return try {
            get(field)?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveCandidateUserIds(userId: String, firestore: FirebaseFirestore): List<String> {
        val candidates = linkedSetOf<String>()
        if (userId.isNotBlank()) candidates.add(userId)

        val authPrefs = context.getSharedPreferences("hisab_auth_prefs", Context.MODE_PRIVATE)
        val prefUid = authPrefs.getString("current_user_id", null)
        val prefEmail = authPrefs.getString("current_user_email", null)
        if (!prefUid.isNullOrBlank()) candidates.add(prefUid)

        val localUser = try { userDao.getUserDirect(userId) } catch (e: Exception) { null }
        val emailSet = linkedSetOf<String>()
        localUser?.email?.takeIf { it.isNotBlank() }?.let {
            emailSet.add(it)
            emailSet.add(it.lowercase())
        }
        prefEmail?.takeIf { it.isNotBlank() }?.let {
            emailSet.add(it)
            emailSet.add(it.lowercase())
        }

        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val fbUser = FirebaseAuth.getInstance().currentUser
                fbUser?.uid?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                fbUser?.email?.takeIf { it.isNotBlank() }?.let {
                    emailSet.add(it)
                    emailSet.add(it.lowercase())
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        for (email in emailSet) {
            candidates.add(email)
            try {
                val snap = firestore.collection(COLLECTION_USERS)
                    .whereEqualTo("email", email)
                    .get()
                    .awaitTask(5000L)
                for (doc in snap.documents) {
                    if (doc.id.isNotBlank()) candidates.add(doc.id)
                    doc.safeString("uid")?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                }
            } catch (e: Exception) {
                // Ignore if security rules restrict collection query
            }
        }

        return candidates.filter { it.isNotBlank() }
    }

    private suspend fun ensureLocalUserAndForeignKeys(
        userId: String,
        requiredAccountIds: Set<Long> = emptySet(),
        requiredVendorIds: Set<Long> = emptySet()
    ) {
        // 1. Ensure User exists in Room so ForeignKey(User::class) never fails
        if (userDao.getUserDirect(userId) == null) {
            val authPrefs = context.getSharedPreferences("hisab_auth_prefs", Context.MODE_PRIVATE)
            val email = authPrefs.getString("current_user_email", null) ?: "$userId@hisab.local"
            val name = authPrefs.getString("current_user_name", null) ?: "User"
            val photo = authPrefs.getString("current_user_photo", null)
            val isGoogle = authPrefs.getBoolean("current_user_is_google", true)
            userDao.insertUser(
                User(
                    id = userId,
                    email = email,
                    displayName = name,
                    photoUrl = photo,
                    isGoogleUser = isGoogle
                )
            )
        }

        // 2. Ensure all referenced Account IDs exist in Room so ForeignKey(UserAccount::class) never fails
        val existingAccounts = accountDao.getAllAccountsList(userId)
        val existingAccountIds = existingAccounts.map { it.id }.toMutableSet()

        val targetAccountIds = requiredAccountIds.toMutableSet()
        if (existingAccountIds.isEmpty() && targetAccountIds.isEmpty()) {
            targetAccountIds.addAll(listOf(1L, 2L, 3L, 4L))
        }

        val missingAccounts = mutableListOf<UserAccount>()
        for (accId in targetAccountIds) {
            if (accId > 0L && !existingAccountIds.contains(accId)) {
                val template = when (accId) {
                    1L -> UserAccount(
                        id = 1L,
                        userId = userId,
                        name = "Cash",
                        type = AccountType.CASH,
                        currencyCode = "BDT",
                        startingBalance = 0.0,
                        colorHex = "#0F766E",
                        iconName = "payments"
                    )
                    2L -> UserAccount(
                        id = 2L,
                        userId = userId,
                        name = "bKash",
                        type = AccountType.MOBILE_WALLET,
                        currencyCode = "BDT",
                        startingBalance = 0.0,
                        colorHex = "#E11475",
                        iconName = "account_balance_wallet"
                    )
                    3L -> UserAccount(
                        id = 3L,
                        userId = userId,
                        name = "Nagad",
                        type = AccountType.MOBILE_WALLET,
                        currencyCode = "BDT",
                        startingBalance = 0.0,
                        colorHex = "#F97316",
                        iconName = "account_balance_wallet"
                    )
                    4L -> UserAccount(
                        id = 4L,
                        userId = userId,
                        name = "Bank Account",
                        type = AccountType.BANK,
                        currencyCode = "BDT",
                        startingBalance = 0.0,
                        colorHex = "#2563EB",
                        iconName = "account_balance"
                    )
                    else -> UserAccount(
                        id = accId,
                        userId = userId,
                        name = "Account #$accId",
                        type = AccountType.CASH,
                        currencyCode = "BDT",
                        startingBalance = 0.0,
                        colorHex = "#0F766E",
                        iconName = "account_balance_wallet"
                    )
                }
                missingAccounts.add(template)
                existingAccountIds.add(accId)
            }
        }
        if (missingAccounts.isNotEmpty()) {
            for (acc in missingAccounts) {
                try {
                    accountDao.insertAccount(acc)
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback insertAccount ${acc.id} failed: ${e.message}")
                }
            }
        }

        // 3. Ensure all referenced Vendor IDs exist in Room so ForeignKey(Vendor::class) never fails
        if (requiredVendorIds.isNotEmpty()) {
            val existingVendorIds = vendorDao.getAllVendorsList(userId).map { it.id }.toMutableSet()
            for (vId in requiredVendorIds) {
                if (vId > 0L && !existingVendorIds.contains(vId)) {
                    try {
                        vendorDao.insertVendor(
                            Vendor(
                                id = vId,
                                userId = userId,
                                name = "Shop #$vId"
                            )
                        )
                        existingVendorIds.add(vId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Fallback insertVendor $vId failed: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun ensureDefaultCategoriesIfEmpty(userId: String) {
        if (categoryDao.getCategoryCount(userId) == 0) {
            val defaultCategories = listOf(
                Category(userId = userId, nameBn = "খাবার", nameEn = "Food & Dining", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#F97316", isDefault = true),
                Category(userId = userId, nameBn = "বাজার ও মুদি", nameEn = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart", colorHex = "#10B981", isDefault = true),
                Category(userId = userId, nameBn = "যাতায়াত", nameEn = "Transport", type = TransactionType.EXPENSE, iconName = "directions_bus", colorHex = "#2563EB", isDefault = true),
                Category(userId = userId, nameBn = "বিল ও ইউটিলিটি", nameEn = "Bills & Utilities", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = "#EF4444", isDefault = true),
                Category(userId = userId, nameBn = "কেনাকাটা", nameEn = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#EC4899", isDefault = true),
                Category(userId = userId, nameBn = "চিকিৎসা ও স্বাস্থ্য", nameEn = "Healthcare", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = "#0D9488", isDefault = true),
                Category(userId = userId, nameBn = "বিনোদন ও ভ্রমণ", nameEn = "Entertainment", type = TransactionType.EXPENSE, iconName = "movie", colorHex = "#7C3AED", isDefault = true),
                Category(userId = userId, nameBn = "শিক্ষা", nameEn = "Education", type = TransactionType.EXPENSE, iconName = "school", colorHex = "#F59E0B", isDefault = true),
                Category(userId = userId, nameBn = "অন্যান্য খরচ", nameEn = "Other Expense", type = TransactionType.EXPENSE, iconName = "category", colorHex = "#64748B", isDefault = true),
                Category(userId = userId, nameBn = "দোকান বাকি পরিশোধ", nameEn = "Shop Credit Payment", type = TransactionType.EXPENSE, iconName = "store", colorHex = "#EF4444", isDefault = true),
                Category(userId = userId, nameBn = "ধার প্রদান", nameEn = "Loan Given", type = TransactionType.EXPENSE, iconName = "payments", colorHex = "#EF4444", isDefault = true),
                Category(userId = userId, nameBn = "ধার পরিশোধ", nameEn = "Loan Repayment", type = TransactionType.EXPENSE, iconName = "payment", colorHex = "#F97316", isDefault = true),
                Category(userId = userId, nameBn = "বেতন", nameEn = "Salary", type = TransactionType.INCOME, iconName = "payments", colorHex = "#10B981", isDefault = true),
                Category(userId = userId, nameBn = "ব্যবসা", nameEn = "Business", type = TransactionType.INCOME, iconName = "store", colorHex = "#2563EB", isDefault = true),
                Category(userId = userId, nameBn = "ফ্রিল্যান্সিং", nameEn = "Freelancing", type = TransactionType.INCOME, iconName = "laptop", colorHex = "#7C3AED", isDefault = true),
                Category(userId = userId, nameBn = "বিনিয়োগ ও লভ্যাংশ", nameEn = "Investments", type = TransactionType.INCOME, iconName = "trending_up", colorHex = "#F59E0B", isDefault = true),
                Category(userId = userId, nameBn = "উপহার ও অনুদান", nameEn = "Gifts & Grants", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#EC4899", isDefault = true),
                Category(userId = userId, nameBn = "অন্যান্য আয়", nameEn = "Other Income", type = TransactionType.INCOME, iconName = "attach_money", colorHex = "#0D9488", isDefault = true),
                Category(userId = userId, nameBn = "ধার গ্রহণ", nameEn = "Loan Received", type = TransactionType.INCOME, iconName = "account_balance_wallet", colorHex = "#10B981", isDefault = true),
                Category(userId = userId, nameBn = "ধার ফেরত গ্রহণ", nameEn = "Loan Collection", type = TransactionType.INCOME, iconName = "savings", colorHex = "#0D9488", isDefault = true)
            )
            try {
                categoryDao.insertCategories(defaultCategories)
            } catch (e: Exception) {
                for (cat in defaultCategories) {
                    try { categoryDao.insertCategory(cat) } catch (_: Exception) {}
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
            val candidateIds = resolveCandidateUserIds(userId, firestore)
            var anyQuerySucceeded = false
            var lastError: Exception? = null

            for (candId in candidateIds) {
                val userDocRef = firestore.collection(COLLECTION_USERS).document(candId)

                try {
                    val txSnap = userDocRef.collection(COLLECTION_TRANSACTIONS).limit(1).get().awaitTask(8000L)
                    anyQuerySucceeded = true
                    if (!txSnap.isEmpty) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }

                try {
                    val accountsSnap = userDocRef.collection(COLLECTION_ACCOUNTS).limit(1).get().awaitTask(8000L)
                    anyQuerySucceeded = true
                    if (!accountsSnap.isEmpty) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }

                try {
                    val debtSnap = userDocRef.collection(COLLECTION_LOANS_DEBTS).limit(1).get().awaitTask(6000L)
                    anyQuerySucceeded = true
                    if (!debtSnap.isEmpty) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }

                try {
                    val vendorSnap = userDocRef.collection(COLLECTION_VENDORS).limit(1).get().awaitTask(6000L)
                    anyQuerySucceeded = true
                    if (!vendorSnap.isEmpty) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }

                try {
                    val purchaseSnap = userDocRef.collection(COLLECTION_SHOP_CREDIT_PURCHASES).limit(1).get().awaitTask(6000L)
                    anyQuerySucceeded = true
                    if (!purchaseSnap.isEmpty) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }

                try {
                    val catSnap = userDocRef.collection(COLLECTION_CATEGORIES).limit(1).get().awaitTask(6000L)
                    anyQuerySucceeded = true
                    if (!catSnap.isEmpty) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }

                try {
                    val settingsDoc = userDocRef.collection(COLLECTION_SETTINGS).document(DOC_PREFERENCES).get().awaitTask(5000L)
                    anyQuerySucceeded = true
                    if (settingsDoc.exists()) return@withContext Result.success(true)
                } catch (e: Exception) {
                    lastError = e
                }
            }

            if (!anyQuerySucceeded && lastError != null) {
                return@withContext Result.failure(lastError)
            }

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
            ensureLocalUserAndForeignKeys(userId)

            val candidateIds = resolveCandidateUserIds(userId, firestore)
            Log.i(TAG, "Checking cloud data across candidate IDs for $userId: $candidateIds")

            val accountsMap = linkedMapOf<Long, UserAccount>()
            val categoriesMap = linkedMapOf<Long, Category>()
            val transactionsMap = linkedMapOf<Long, TransactionRecord>()
            val budgetsMap = linkedMapOf<Long, Budget>()
            val goalsMap = linkedMapOf<Long, SavingsGoal>()
            val contactsMap = linkedMapOf<Long, Contact>()
            val debtsMap = linkedMapOf<Long, LoanDebt>()
            val repaymentsMap = linkedMapOf<Long, LoanRepayment>()
            val vendorsMap = linkedMapOf<Long, Vendor>()
            val purchasesMap = linkedMapOf<Long, ShopCreditPurchase>()
            val paymentsMap = linkedMapOf<Long, ShopCreditPayment>()
            val rulesMap = linkedMapOf<Long, RecurringRule>()
            var restoredSettingsObj: UserSettings? = null
            var anyNetworkCallSucceeded = false
            var lastNetworkException: Exception? = null

            onProgress?.invoke("Restoring accounts and balances...", 0.18f)

            for (candId in candidateIds) {
                val userDocRef = firestore.collection(COLLECTION_USERS).document(candId)

                // 1. Fetch Accounts from candidate
                try {
                    val snap = userDocRef.collection(COLLECTION_ACCOUNTS).get().awaitTask(10000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: "Account"
                        val typeStr = doc.safeString("type") ?: AccountType.CASH.name
                        val type = try { AccountType.valueOf(typeStr) } catch (e: Exception) { AccountType.CASH }
                        val currencyCode = doc.safeString("currencyCode") ?: "BDT"
                        val startingBalance = doc.safeDouble("startingBalance") ?: 0.0
                        val colorHex = doc.safeString("colorHex") ?: "#0F766E"
                        val iconName = doc.safeString("iconName") ?: "payments"
                        val isArchived = doc.safeBoolean("isArchived") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                        val incoming = UserAccount(
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
                        val existing = accountsMap[id]
                        // Prefer account entry with non-zero startingBalance if one candidate had 0-balance defaults
                        if (existing == null || (existing.startingBalance == 0.0 && incoming.startingBalance != 0.0)) {
                            accountsMap[id] = incoming
                        }
                    }
                } catch (e: Exception) {
                    lastNetworkException = e
                    Log.w(TAG, "Accounts fetch for candidate $candId failed: ${e.message}")
                }

                // 2. Fetch Categories from candidate
                try {
                    val snap = userDocRef.collection(COLLECTION_CATEGORIES).get().awaitTask(10000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val nameBn = doc.safeString("nameBn") ?: ""
                        val nameEn = doc.safeString("nameEn") ?: ""
                        val typeStr = doc.safeString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val iconName = doc.safeString("iconName") ?: "category"
                        val colorHex = doc.safeString("colorHex") ?: "#64748B"
                        val isDefault = doc.safeBoolean("isDefault") ?: false
                        val isArchived = doc.safeBoolean("isArchived") ?: false

                        if (!categoriesMap.containsKey(id)) {
                            categoriesMap[id] = Category(
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
                    }
                } catch (e: Exception) {
                    lastNetworkException = e
                    Log.w(TAG, "Categories fetch for candidate $candId failed: ${e.message}")
                }

                // 3. Fetch Transactions from candidate
                try {
                    val snap = userDocRef.collection(COLLECTION_TRANSACTIONS).get().awaitTask(12000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val typeStr = doc.safeString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.safeString("note") ?: ""
                        val fee = doc.safeDouble("fee") ?: 0.0
                        val categoryId = doc.safeLong("categoryId")
                        val toAccountId = doc.safeLong("toAccountId")
                        val receiptUri = doc.safeString("receiptUri")
                        val exchangeRate = doc.safeDouble("exchangeRate") ?: 1.0
                        val isRecurringInstance = doc.safeBoolean("isRecurringInstance") ?: false
                        val recurringRuleId = doc.safeLong("recurringRuleId")
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                        if (!transactionsMap.containsKey(id)) {
                            transactionsMap[id] = TransactionRecord(
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
                    }
                } catch (e: Exception) {
                    lastNetworkException = e
                    Log.w(TAG, "Transactions fetch for candidate $candId failed: ${e.message}")
                }

                // 4. Fetch Budgets
                try {
                    val snap = userDocRef.collection(COLLECTION_BUDGETS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val categoryId = doc.safeLong("categoryId")
                        val monthYear = doc.safeString("monthYear") ?: continue
                        val limitAmount = doc.safeDouble("limitAmount") ?: 0.0
                        if (!budgetsMap.containsKey(id)) {
                            budgetsMap[id] = Budget(
                                id = id,
                                userId = userId,
                                categoryId = categoryId,
                                monthYear = monthYear,
                                limitAmount = limitAmount
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Budgets fetch for candidate $candId failed: ${e.message}")
                }

                // 5. Fetch Savings Goals
                try {
                    val snap = userDocRef.collection(COLLECTION_SAVINGS_GOALS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: ""
                        val targetAmount = doc.safeDouble("targetAmount") ?: 0.0
                        val currentSavedAmount = doc.safeDouble("currentSavedAmount") ?: 0.0
                        val targetDate = doc.safeLong("targetDate")
                        val note = doc.safeString("note") ?: ""
                        val colorHex = doc.safeString("colorHex") ?: "#0D9488"
                        val iconName = doc.safeString("iconName") ?: "savings"
                        val isCompleted = doc.safeBoolean("isCompleted") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        if (!goalsMap.containsKey(id)) {
                            goalsMap[id] = SavingsGoal(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Goals fetch for candidate $candId failed: ${e.message}")
                }

                // 6. Fetch Contacts
                try {
                    val snap = userDocRef.collection(COLLECTION_CONTACTS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: ""
                        val phone = doc.safeString("phone") ?: ""
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        if (!contactsMap.containsKey(id)) {
                            contactsMap[id] = Contact(
                                id = id,
                                userId = userId,
                                name = name,
                                phone = phone,
                                createdAt = createdAt
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Contacts fetch for candidate $candId failed: ${e.message}")
                }

                // 7. Fetch Loans & Debts
                try {
                    val snap = userDocRef.collection(COLLECTION_LOANS_DEBTS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val contactId = doc.safeLong("contactId") ?: 0L
                        val personName = doc.safeString("personName") ?: ""
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val paidAmount = doc.safeDouble("paidAmount") ?: 0.0
                        val typeStr = doc.safeString("type") ?: DebtType.OWED_TO_ME.name
                        val type = try { DebtType.valueOf(typeStr) } catch (e: Exception) { DebtType.OWED_TO_ME }
                        val dueDate = doc.safeLong("dueDate")
                        val note = doc.safeString("note") ?: ""
                        val phone = doc.safeString("phone") ?: ""
                        val isSettled = doc.safeBoolean("isSettled") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        val transactionId = doc.safeLong("transactionId")
                        if (!debtsMap.containsKey(id)) {
                            debtsMap[id] = LoanDebt(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Debts fetch for candidate $candId failed: ${e.message}")
                }

                // 8. Fetch Loan Repayments
                try {
                    val snap = userDocRef.collection(COLLECTION_LOAN_REPAYMENTS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val loanId = doc.safeLong("loanId") ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.safeString("note") ?: ""
                        val transactionId = doc.safeLong("transactionId")
                        if (!repaymentsMap.containsKey(id)) {
                            repaymentsMap[id] = LoanRepayment(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Repayments fetch for candidate $candId failed: ${e.message}")
                }

                // 9. Fetch Vendors
                try {
                    val snap = userDocRef.collection(COLLECTION_VENDORS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: ""
                        val phone = doc.safeString("phone") ?: ""
                        val locationNote = doc.safeString("locationNote") ?: ""
                        val categoryTag = doc.safeString("categoryTag") ?: ""
                        val isArchived = doc.safeBoolean("isArchived") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        if (!vendorsMap.containsKey(id)) {
                            vendorsMap[id] = Vendor(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Vendors fetch for candidate $candId failed: ${e.message}")
                }

                // 10. Fetch Shop Credit Purchases
                try {
                    val snap = userDocRef.collection(COLLECTION_SHOP_CREDIT_PURCHASES).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val vendorId = doc.safeLong("vendorId") ?: continue
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val paidAmount = doc.safeDouble("paidAmount") ?: 0.0
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val dueDate = doc.safeLong("dueDate")
                        val note = doc.safeString("note") ?: ""
                        val isSettled = doc.safeBoolean("isSettled") ?: false
                        if (!purchasesMap.containsKey(id)) {
                            purchasesMap[id] = ShopCreditPurchase(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Shop purchases fetch for candidate $candId failed: ${e.message}")
                }

                // 11. Fetch Shop Credit Payments
                try {
                    val snap = userDocRef.collection(COLLECTION_SHOP_CREDIT_PAYMENTS).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val vendorId = doc.safeLong("vendorId") ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.safeString("note") ?: ""
                        val transactionId = doc.safeLong("transactionId")
                        if (!paymentsMap.containsKey(id)) {
                            paymentsMap[id] = ShopCreditPayment(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Shop payments fetch for candidate $candId failed: ${e.message}")
                }

                // 12. Fetch Recurring Rules
                try {
                    val snap = userDocRef.collection(COLLECTION_RECURRING_RULES).get().awaitTask(8000L)
                    anyNetworkCallSucceeded = true
                    for (doc in snap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val categoryId = doc.safeLong("categoryId")
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val typeStr = doc.safeString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val freqStr = doc.safeString("frequency") ?: Frequency.MONTHLY.name
                        val frequency = try { Frequency.valueOf(freqStr) } catch (e: Exception) { Frequency.MONTHLY }
                        val startDate = doc.safeLong("startDate") ?: System.currentTimeMillis()
                        val endDate = doc.safeLong("endDate")
                        val lastGeneratedDate = doc.safeLong("lastGeneratedDate") ?: 0L
                        val isPaused = doc.safeBoolean("isPaused") ?: false
                        val note = doc.safeString("note") ?: ""
                        if (!rulesMap.containsKey(id)) {
                            rulesMap[id] = RecurringRule(
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
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Recurring rules fetch for candidate $candId failed: ${e.message}")
                }

                // 13. Fetch Settings
                if (restoredSettingsObj == null) {
                    try {
                        val settingsDoc = userDocRef.collection(COLLECTION_SETTINGS).document(DOC_PREFERENCES).get().awaitTask(6000L)
                        anyNetworkCallSucceeded = true
                        if (settingsDoc.exists()) {
                            val lang = settingsDoc.safeString("language") ?: "bn"
                            val numeral = settingsDoc.safeString("numeralSystem") ?: "bn"
                            val currency = settingsDoc.safeString("defaultCurrency") ?: "BDT"
                            val symbol = settingsDoc.safeString("currencySymbol") ?: "৳"
                            val hideBal = settingsDoc.safeBoolean("hideBalances") ?: false
                            val pinEn = settingsDoc.safeBoolean("pinEnabled") ?: false
                            val pin = settingsDoc.safeString("pinCode") ?: ""
                            val dailyReminder = settingsDoc.safeBoolean("dailyReminderEnabled") ?: true
                            val dark = settingsDoc.safeBoolean("isDarkMode") ?: false
                            val systemTheme = settingsDoc.safeBoolean("useSystemTheme") ?: true

                            restoredSettingsObj = UserSettings(
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
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Settings fetch for candidate $candId failed: ${e.message}")
                    }
                }
            }

            if (!anyNetworkCallSucceeded && lastNetworkException != null) {
                return@withContext Result.failure(lastNetworkException)
            }

            val remoteAccounts = accountsMap.values.toList()
            val remoteCategories = categoriesMap.values.toList()
            val remoteTransactions = transactionsMap.values.toList()
            val budgets = budgetsMap.values.toList()
            val goals = goalsMap.values.toList()
            val contacts = contactsMap.values.toList()
            val debts = debtsMap.values.toList()
            val repayments = repaymentsMap.values.toList()
            val vendors = vendorsMap.values.toList()
            val purchases = purchasesMap.values.toList()
            val payments = paymentsMap.values.toList()
            val rules = rulesMap.values.toList()

            val hasCloudData = remoteAccounts.isNotEmpty() ||
                    remoteTransactions.isNotEmpty() ||
                    remoteCategories.isNotEmpty() ||
                    budgets.isNotEmpty() ||
                    goals.isNotEmpty() ||
                    contacts.isNotEmpty() ||
                    debts.isNotEmpty() ||
                    repayments.isNotEmpty() ||
                    vendors.isNotEmpty() ||
                    purchases.isNotEmpty() ||
                    payments.isNotEmpty() ||
                    rules.isNotEmpty() ||
                    restoredSettingsObj != null

            if (!hasCloudData) {
                Log.i(TAG, "No cloud data found across candidate IDs for user $userId")
                return@withContext Result.success(CloudRestoreSummary(hasCloudData = false))
            }

            Log.i(
                TAG,
                "Restoring cloud data for user $userId: accounts=${remoteAccounts.size}, " +
                        "txs=${remoteTransactions.size}, cats=${remoteCategories.size}, debts=${debts.size}, vendors=${vendors.size}"
            )

            // 1. Insert Accounts
            if (remoteAccounts.isNotEmpty()) {
                for (acc in remoteAccounts) {
                    try {
                        accountDao.insertAccount(acc)
                    } catch (e: Exception) {
                        Log.w(TAG, "Insert account ${acc.id} failed: ${e.message}")
                    }
                }
            }

            // Collect all referenced account IDs and vendor IDs from child records to guarantee ForeignKeys!
            val requiredAccountIds = mutableSetOf<Long>()
            remoteTransactions.forEach { tx ->
                requiredAccountIds.add(tx.accountId)
                tx.toAccountId?.let { requiredAccountIds.add(it) }
            }
            debts.forEach { requiredAccountIds.add(it.accountId) }
            repayments.forEach { requiredAccountIds.add(it.accountId) }
            payments.forEach { requiredAccountIds.add(it.accountId) }
            rules.forEach { requiredAccountIds.add(it.accountId) }
            if (remoteAccounts.isEmpty()) {
                requiredAccountIds.addAll(listOf(1L, 2L, 3L, 4L))
            }

            onProgress?.invoke("Restoring categories...", 0.35f)

            // 2. Insert Categories & ensure defaults if empty
            if (remoteCategories.isNotEmpty()) {
                for (cat in remoteCategories) {
                    try {
                        categoryDao.insertCategory(cat)
                    } catch (e: Exception) {
                        Log.w(TAG, "Insert category ${cat.id} failed: ${e.message}")
                    }
                }
            }
            ensureDefaultCategoriesIfEmpty(userId)

            // 3. Insert Vendors BEFORE Shop Credit Purchases & Payments
            if (vendors.isNotEmpty()) {
                for (v in vendors) {
                    try {
                        vendorDao.insertVendor(v)
                    } catch (e: Exception) {
                        Log.w(TAG, "Insert vendor ${v.id} failed: ${e.message}")
                    }
                }
            }

            val requiredVendorIds = mutableSetOf<Long>()
            purchases.forEach { requiredVendorIds.add(it.vendorId) }
            payments.forEach { requiredVendorIds.add(it.vendorId) }

            // Ensure all foreign keys (User, Accounts, Vendors) exist in Room BEFORE inserting Transactions/Debts/Purchases/Payments!
            ensureLocalUserAndForeignKeys(
                userId = userId,
                requiredAccountIds = requiredAccountIds,
                requiredVendorIds = requiredVendorIds
            )

            onProgress?.invoke("Restoring transactions...", 0.55f)

            // 4. Insert Transactions safely
            var insertedTxCount = 0
            if (remoteTransactions.isNotEmpty()) {
                try {
                    transactionDao.insertTransactions(remoteTransactions)
                    insertedTxCount = remoteTransactions.size
                } catch (e: Exception) {
                    Log.w(TAG, "Batch insertTransactions failed (${e.message}), falling back to per-item insert")
                    for (tx in remoteTransactions) {
                        try {
                            ensureLocalUserAndForeignKeys(
                                userId = userId,
                                requiredAccountIds = setOfNotNull(tx.accountId, tx.toAccountId)
                            )
                            transactionDao.insertTransaction(tx)
                            insertedTxCount++
                        } catch (inner: Exception) {
                            Log.w(TAG, "Single insertTransaction ${tx.id} failed: ${inner.message}")
                        }
                    }
                }
            }

            onProgress?.invoke("Restoring budgets and savings goals...", 0.72f)

            // 5. Insert Budgets
            var budgetsCount = 0
            for (b in budgets) {
                try {
                    budgetDao.insertBudgets(listOf(b))
                    budgetsCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert budget ${b.id} failed: ${e.message}")
                }
            }

            // 6. Insert Savings Goals
            var goalsCount = 0
            for (g in goals) {
                try {
                    savingsGoalDao.insertGoal(g)
                    goalsCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert goal ${g.id} failed: ${e.message}")
                }
            }

            onProgress?.invoke("Restoring loans, debts and shops...", 0.85f)

            // 7. Insert Contacts
            for (c in contacts) {
                try {
                    contactDao.insertContact(c)
                } catch (e: Exception) {
                    Log.w(TAG, "Insert contact ${c.id} failed: ${e.message}")
                }
            }

            // 8. Insert Loans & Debts
            var debtsCount = 0
            for (d in debts) {
                try {
                    loanDebtDao.insertDebt(d)
                    debtsCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert debt ${d.id} failed: ${e.message}")
                }
            }

            // 9. Insert Loan Repayments
            var repaymentsCount = 0
            for (r in repayments) {
                try {
                    loanRepaymentDao.insertRepayment(r)
                    repaymentsCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert repayment ${r.id} failed: ${e.message}")
                }
            }

            // 10. Insert Shop Credit Purchases
            var shopPurchasesCount = 0
            for (p in purchases) {
                try {
                    shopCreditPurchaseDao.insertPurchase(p)
                    shopPurchasesCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert purchase ${p.id} failed: ${e.message}")
                }
            }

            // 11. Insert Shop Credit Payments
            var shopPaymentsCount = 0
            for (pay in payments) {
                try {
                    shopCreditPaymentDao.insertPayment(pay)
                    shopPaymentsCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert payment ${pay.id} failed: ${e.message}")
                }
            }

            // 12. Insert Recurring Rules
            var recurringCount = 0
            for (rule in rules) {
                try {
                    recurringDao.insertRule(rule)
                    recurringCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Insert recurring rule ${rule.id} failed: ${e.message}")
                }
            }

            onProgress?.invoke("Applying settings and preferences...", 0.94f)

            // 13. Restore User Settings
            var settingsRestored = false
            restoredSettingsObj?.let { restoredSettings ->
                try {
                    settingsDao.insertOrUpdate(restoredSettings)
                    val prefs = context.getSharedPreferences("hisab_settings_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("language", restoredSettings.language)
                        .putString("numeral_system", restoredSettings.numeralSystem)
                        .putBoolean("is_dark_mode", restoredSettings.isDarkMode)
                        .putBoolean("use_system_theme", restoredSettings.useSystemTheme)
                        .apply()
                    settingsRestored = true
                } catch (e: Exception) {
                    Log.w(TAG, "Restoring user settings failed: ${e.message}")
                }
            }

            val finalAccountsCount = accountDao.getAllAccountsList(userId).size

            onProgress?.invoke("Restoration complete!", 1.0f)
            Log.i(TAG, "Successfully restored cloud data for user $userId (accounts=$finalAccountsCount, txs=$insertedTxCount)")

            val summary = CloudRestoreSummary(
                accountsCount = finalAccountsCount,
                transactionsCount = insertedTxCount,
                categoriesCount = categoryDao.getAllCategoriesList(userId).size,
                budgetsCount = budgetsCount,
                goalsCount = goalsCount,
                debtsCount = debtsCount,
                repaymentsCount = repaymentsCount,
                vendorsCount = vendorDao.getAllVendorsList(userId).size,
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
            ensureLocalUserAndForeignKeys(userId)
            val candidateIds = resolveCandidateUserIds(userId, firestore)
            var syncedCount = 0

            // 1. Sync Accounts
            syncedCount += syncAccounts(userId, candidateIds, firestore)

            // 2. Sync Categories
            syncedCount += syncCategories(userId, candidateIds, firestore)

            // 3. Sync Vendors (before Shop Credit Purchases/Payments)
            syncedCount += syncVendors(userId, candidateIds, firestore)

            // 4. Sync Transactions (ensures referenced Account IDs exist first)
            syncedCount += syncTransactions(userId, candidateIds, firestore)

            // 5. Sync Budgets
            syncedCount += syncBudgets(userId, candidateIds, firestore)

            // 6. Sync Savings Goals
            syncedCount += syncSavingsGoals(userId, candidateIds, firestore)

            // 7. Sync Contacts
            syncedCount += syncContacts(userId, candidateIds, firestore)

            // 8. Sync Loans & Debts
            syncedCount += syncLoansDebts(userId, candidateIds, firestore)

            // 9. Sync Loan Repayments
            syncedCount += syncLoanRepayments(userId, candidateIds, firestore)

            // 10. Sync Shop Credit Purchases & Payments
            syncedCount += syncShopCredit(userId, candidateIds, firestore)

            // 11. Sync Recurring Rules
            syncedCount += syncRecurringRules(userId, candidateIds, firestore)

            // 12. Sync User Preferences / Settings
            syncUserSettings(userId, candidateIds, firestore)

            Log.i(TAG, "syncAllWithRoom complete for $userId. Total items synchronized: $syncedCount")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.w(TAG, "syncAllWithRoom failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncAccounts(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteAccountsMap = linkedMapOf<Long, UserAccount>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_ACCOUNTS)
                val remoteSnap = try { collection.get().awaitTask(8000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: "Account"
                        val typeStr = doc.safeString("type") ?: AccountType.CASH.name
                        val type = try { AccountType.valueOf(typeStr) } catch (e: Exception) { AccountType.CASH }
                        val currencyCode = doc.safeString("currencyCode") ?: "BDT"
                        val startingBalance = doc.safeDouble("startingBalance") ?: 0.0
                        val colorHex = doc.safeString("colorHex") ?: "#0F766E"
                        val iconName = doc.safeString("iconName") ?: "payments"
                        val isArchived = doc.safeBoolean("isArchived") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                        val incoming = UserAccount(
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
                        val existing = remoteAccountsMap[id]
                        if (existing == null || (existing.startingBalance == 0.0 && incoming.startingBalance != 0.0)) {
                            remoteAccountsMap[id] = incoming
                        }
                    }
                }
            }

            val localAccountsBefore = accountDao.getAllAccountsList(userId).associateBy { it.id }
            val remoteAccounts = remoteAccountsMap.values.map { remoteAcc ->
                val localAcc = localAccountsBefore[remoteAcc.id]
                if (localAcc != null && remoteAcc.startingBalance == 0.0 && localAcc.startingBalance != 0.0) {
                    remoteAcc.copy(startingBalance = localAcc.startingBalance)
                } else {
                    remoteAcc
                }
            }

            if (remoteAccounts.isNotEmpty()) {
                for (acc in remoteAccounts) {
                    try {
                        accountDao.insertAccount(acc)
                        count++
                    } catch (e: Exception) {
                        Log.w(TAG, "syncAccounts insertAccount ${acc.id} error: ${e.message}")
                    }
                }
            }

            val localAccounts = accountDao.getAllAccountsList(userId)
            val remoteIds = remoteAccountsMap.keys
            for (acc in localAccounts) {
                val remoteMatch = remoteAccountsMap[acc.id]
                if (!remoteIds.contains(acc.id) || (remoteMatch != null && remoteMatch.startingBalance == 0.0 && acc.startingBalance != 0.0)) {
                    saveAccount(userId, acc)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncAccounts error: ${e.message}")
        }
        return count
    }

    private suspend fun syncCategories(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteCatsMap = linkedMapOf<Long, Category>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_CATEGORIES)
                val remoteSnap = try { collection.get().awaitTask(8000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val nameBn = doc.safeString("nameBn") ?: ""
                        val nameEn = doc.safeString("nameEn") ?: ""
                        val typeStr = doc.safeString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val iconName = doc.safeString("iconName") ?: "category"
                        val colorHex = doc.safeString("colorHex") ?: "#64748B"
                        val isDefault = doc.safeBoolean("isDefault") ?: false
                        val isArchived = doc.safeBoolean("isArchived") ?: false

                        if (!remoteCatsMap.containsKey(id)) {
                            remoteCatsMap[id] = Category(
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
                    }
                }
            }

            if (remoteCatsMap.isNotEmpty()) {
                for (cat in remoteCatsMap.values) {
                    try {
                        categoryDao.insertCategory(cat)
                        count++
                    } catch (e: Exception) {
                        Log.w(TAG, "syncCategories insert ${cat.id} error: ${e.message}")
                    }
                }
            }

            val localCats = categoryDao.getAllCategoriesList(userId)
            val remoteIds = remoteCatsMap.keys
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

    private suspend fun syncTransactions(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteMap = linkedMapOf<Long, TransactionRecord>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_TRANSACTIONS)
                val remoteSnap = try { collection.get().awaitTask(10000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val typeStr = doc.safeString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.safeString("note") ?: ""
                        val fee = doc.safeDouble("fee") ?: 0.0
                        val categoryId = doc.safeLong("categoryId")
                        val toAccountId = doc.safeLong("toAccountId")
                        val receiptUri = doc.safeString("receiptUri")
                        val exchangeRate = doc.safeDouble("exchangeRate") ?: 1.0
                        val isRecurringInstance = doc.safeBoolean("isRecurringInstance") ?: false
                        val recurringRuleId = doc.safeLong("recurringRuleId")
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                        if (!remoteMap.containsKey(id)) {
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
                    }
                }
            }

            if (remoteMap.isNotEmpty()) {
                val requiredAccountIds = mutableSetOf<Long>()
                remoteMap.values.forEach { tx ->
                    requiredAccountIds.add(tx.accountId)
                    tx.toAccountId?.let { requiredAccountIds.add(it) }
                }
                ensureLocalUserAndForeignKeys(userId = userId, requiredAccountIds = requiredAccountIds)

                for (tx in remoteMap.values) {
                    try {
                        transactionDao.insertTransaction(tx)
                        count++
                    } catch (e: Exception) {
                        Log.w(TAG, "syncTransactions insert ${tx.id} error: ${e.message}")
                    }
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

    private suspend fun syncBudgets(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteBudgetsMap = linkedMapOf<Long, Budget>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_BUDGETS)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val categoryId = doc.safeLong("categoryId")
                        val monthYear = doc.safeString("monthYear") ?: continue
                        val limitAmount = doc.safeDouble("limitAmount") ?: 0.0
                        if (!remoteBudgetsMap.containsKey(id)) {
                            remoteBudgetsMap[id] = Budget(
                                id = id,
                                userId = userId,
                                categoryId = categoryId,
                                monthYear = monthYear,
                                limitAmount = limitAmount
                            )
                        }
                    }
                }
            }

            for (b in remoteBudgetsMap.values) {
                try {
                    budgetDao.insertBudgets(listOf(b))
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "syncBudgets insert ${b.id} error: ${e.message}")
                }
            }

            val localBudgets = budgetDao.getAllBudgetsList(userId)
            val remoteIds = remoteBudgetsMap.keys
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

    private suspend fun syncSavingsGoals(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteGoalsMap = linkedMapOf<Long, SavingsGoal>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_SAVINGS_GOALS)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: ""
                        val targetAmount = doc.safeDouble("targetAmount") ?: 0.0
                        val currentSavedAmount = doc.safeDouble("currentSavedAmount") ?: 0.0
                        val targetDate = doc.safeLong("targetDate")
                        val note = doc.safeString("note") ?: ""
                        val colorHex = doc.safeString("colorHex") ?: "#0D9488"
                        val iconName = doc.safeString("iconName") ?: "savings"
                        val isCompleted = doc.safeBoolean("isCompleted") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        if (!remoteGoalsMap.containsKey(id)) {
                            remoteGoalsMap[id] = SavingsGoal(
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
                    }
                }
            }

            for (g in remoteGoalsMap.values) {
                try {
                    savingsGoalDao.insertGoal(g)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "syncSavingsGoals insert ${g.id} error: ${e.message}")
                }
            }

            val localGoals = savingsGoalDao.getAllGoalsList(userId)
            val remoteIds = remoteGoalsMap.keys
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

    private suspend fun syncContacts(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteContactsMap = linkedMapOf<Long, Contact>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_CONTACTS)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: ""
                        val phone = doc.safeString("phone") ?: ""
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        if (!remoteContactsMap.containsKey(id)) {
                            remoteContactsMap[id] = Contact(id = id, userId = userId, name = name, phone = phone, createdAt = createdAt)
                        }
                    }
                }
            }

            for (c in remoteContactsMap.values) {
                try {
                    contactDao.insertContact(c)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "syncContacts insert ${c.id} error: ${e.message}")
                }
            }

            val localContacts = contactDao.getAllContactsList(userId)
            val remoteIds = remoteContactsMap.keys
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

    private suspend fun syncLoansDebts(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteDebtsMap = linkedMapOf<Long, LoanDebt>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_LOANS_DEBTS)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val contactId = doc.safeLong("contactId") ?: 0L
                        val personName = doc.safeString("personName") ?: ""
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val paidAmount = doc.safeDouble("paidAmount") ?: 0.0
                        val typeStr = doc.safeString("type") ?: DebtType.OWED_TO_ME.name
                        val type = try { DebtType.valueOf(typeStr) } catch (e: Exception) { DebtType.OWED_TO_ME }
                        val dueDate = doc.safeLong("dueDate")
                        val note = doc.safeString("note") ?: ""
                        val phone = doc.safeString("phone") ?: ""
                        val isSettled = doc.safeBoolean("isSettled") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        val transactionId = doc.safeLong("transactionId")
                        if (!remoteDebtsMap.containsKey(id)) {
                            remoteDebtsMap[id] = LoanDebt(
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
                    }
                }
            }

            if (remoteDebtsMap.isNotEmpty()) {
                val reqAccounts = remoteDebtsMap.values.map { it.accountId }.toSet()
                ensureLocalUserAndForeignKeys(userId = userId, requiredAccountIds = reqAccounts)
                for (d in remoteDebtsMap.values) {
                    try {
                        loanDebtDao.insertDebt(d)
                        count++
                    } catch (e: Exception) {
                        Log.w(TAG, "syncLoansDebts insert ${d.id} error: ${e.message}")
                    }
                }
            }

            val localDebts = loanDebtDao.getAllDebtsList(userId)
            val remoteIds = remoteDebtsMap.keys
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

    private suspend fun syncLoanRepayments(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteRepaymentsMap = linkedMapOf<Long, LoanRepayment>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_LOAN_REPAYMENTS)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val loanId = doc.safeLong("loanId") ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.safeString("note") ?: ""
                        val transactionId = doc.safeLong("transactionId")
                        if (!remoteRepaymentsMap.containsKey(id)) {
                            remoteRepaymentsMap[id] = LoanRepayment(
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
                    }
                }
            }

            if (remoteRepaymentsMap.isNotEmpty()) {
                val reqAccounts = remoteRepaymentsMap.values.map { it.accountId }.toSet()
                ensureLocalUserAndForeignKeys(userId = userId, requiredAccountIds = reqAccounts)
                for (r in remoteRepaymentsMap.values) {
                    try {
                        loanRepaymentDao.insertRepayment(r)
                        count++
                    } catch (e: Exception) {
                        Log.w(TAG, "syncLoanRepayments insert ${r.id} error: ${e.message}")
                    }
                }
            }

            val localRepayments = loanRepaymentDao.getAllRepaymentsList(userId)
            val remoteIds = remoteRepaymentsMap.keys
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

    private suspend fun syncVendors(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteVendorsMap = linkedMapOf<Long, Vendor>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_VENDORS)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val name = doc.safeString("name") ?: ""
                        val phone = doc.safeString("phone") ?: ""
                        val locationNote = doc.safeString("locationNote") ?: ""
                        val categoryTag = doc.safeString("categoryTag") ?: ""
                        val isArchived = doc.safeBoolean("isArchived") ?: false
                        val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                        if (!remoteVendorsMap.containsKey(id)) {
                            remoteVendorsMap[id] = Vendor(
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
                    }
                }
            }

            for (v in remoteVendorsMap.values) {
                try {
                    vendorDao.insertVendor(v)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "syncVendors insert ${v.id} error: ${e.message}")
                }
            }

            val localVendors = vendorDao.getAllVendorsList(userId)
            val remoteIds = remoteVendorsMap.keys
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

    private suspend fun syncShopCredit(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remotePurchasesMap = linkedMapOf<Long, ShopCreditPurchase>()
            val remotePaymentsMap = linkedMapOf<Long, ShopCreditPayment>()

            for (candId in candidateIds) {
                val pColl = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_SHOP_CREDIT_PURCHASES)
                val pSnap = try { pColl.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (pSnap != null) {
                    for (doc in pSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val vendorId = doc.safeLong("vendorId") ?: continue
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val paidAmount = doc.safeDouble("paidAmount") ?: 0.0
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val dueDate = doc.safeLong("dueDate")
                        val note = doc.safeString("note") ?: ""
                        val isSettled = doc.safeBoolean("isSettled") ?: false
                        if (!remotePurchasesMap.containsKey(id)) {
                            remotePurchasesMap[id] = ShopCreditPurchase(
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
                    }
                }

                val payColl = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_SHOP_CREDIT_PAYMENTS)
                val paySnap = try { payColl.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (paySnap != null) {
                    for (doc in paySnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val vendorId = doc.safeLong("vendorId") ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val dateTimestamp = doc.safeLong("dateTimestamp") ?: System.currentTimeMillis()
                        val note = doc.safeString("note") ?: ""
                        val transactionId = doc.safeLong("transactionId")
                        if (!remotePaymentsMap.containsKey(id)) {
                            remotePaymentsMap[id] = ShopCreditPayment(
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
                    }
                }
            }

            val reqVendors = (remotePurchasesMap.values.map { it.vendorId } + remotePaymentsMap.values.map { it.vendorId }).toSet()
            val reqAccounts = remotePaymentsMap.values.map { it.accountId }.toSet()
            if (reqVendors.isNotEmpty() || reqAccounts.isNotEmpty()) {
                ensureLocalUserAndForeignKeys(
                    userId = userId,
                    requiredAccountIds = reqAccounts,
                    requiredVendorIds = reqVendors
                )
            }

            for (p in remotePurchasesMap.values) {
                try {
                    shopCreditPurchaseDao.insertPurchase(p)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "syncShopCredit insert purchase ${p.id} error: ${e.message}")
                }
            }

            val localPurchases = shopCreditPurchaseDao.getAllPurchasesList(userId)
            val rPIds = remotePurchasesMap.keys
            for (p in localPurchases) {
                if (!rPIds.contains(p.id)) {
                    saveShopCreditPurchase(userId, p)
                    count++
                }
            }

            for (pay in remotePaymentsMap.values) {
                try {
                    shopCreditPaymentDao.insertPayment(pay)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "syncShopCredit insert payment ${pay.id} error: ${e.message}")
                }
            }

            val localPayments = shopCreditPaymentDao.getAllPaymentsList(userId)
            val rPayIds = remotePaymentsMap.keys
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

    private suspend fun syncRecurringRules(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore): Int {
        var count = 0
        try {
            val remoteRulesMap = linkedMapOf<Long, RecurringRule>()
            for (candId in candidateIds) {
                val collection = firestore.collection(COLLECTION_USERS).document(candId).collection(COLLECTION_RECURRING_RULES)
                val remoteSnap = try { collection.get().awaitTask(6000L) } catch (e: Exception) { null }
                if (remoteSnap != null) {
                    for (doc in remoteSnap.documents) {
                        val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                        val accountId = doc.safeLong("accountId") ?: 1L
                        val categoryId = doc.safeLong("categoryId")
                        val amount = doc.safeDouble("amount") ?: 0.0
                        val typeStr = doc.safeString("type") ?: TransactionType.EXPENSE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
                        val freqStr = doc.safeString("frequency") ?: Frequency.MONTHLY.name
                        val frequency = try { Frequency.valueOf(freqStr) } catch (e: Exception) { Frequency.MONTHLY }
                        val startDate = doc.safeLong("startDate") ?: System.currentTimeMillis()
                        val endDate = doc.safeLong("endDate")
                        val lastGeneratedDate = doc.safeLong("lastGeneratedDate") ?: 0L
                        val isPaused = doc.safeBoolean("isPaused") ?: false
                        val note = doc.safeString("note") ?: ""
                        if (!remoteRulesMap.containsKey(id)) {
                            remoteRulesMap[id] = RecurringRule(
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
                    }
                }
            }

            if (remoteRulesMap.isNotEmpty()) {
                val reqAccounts = remoteRulesMap.values.map { it.accountId }.toSet()
                ensureLocalUserAndForeignKeys(userId = userId, requiredAccountIds = reqAccounts)
                for (r in remoteRulesMap.values) {
                    try {
                        recurringDao.insertRule(r)
                        count++
                    } catch (e: Exception) {
                        Log.w(TAG, "syncRecurringRules insert ${r.id} error: ${e.message}")
                    }
                }
            }

            val localRules = recurringDao.getAllRulesList(userId)
            val remoteIds = remoteRulesMap.keys
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

    private suspend fun syncUserSettings(userId: String, candidateIds: List<String>, firestore: FirebaseFirestore) {
        try {
            var settingsDoc: DocumentSnapshot? = null
            for (candId in candidateIds) {
                val doc = try {
                    firestore.collection(COLLECTION_USERS)
                        .document(candId)
                        .collection(COLLECTION_SETTINGS)
                        .document(DOC_PREFERENCES)
                        .get()
                        .awaitTask(5000L)
                } catch (e: Exception) {
                    null
                }
                if (doc != null && doc.exists()) {
                    settingsDoc = doc
                    break
                }
            }

            val local = settingsDao.getSettingsDirect(userId)
            if (settingsDoc != null && settingsDoc.exists()) {
                val lang = settingsDoc.safeString("language") ?: local?.language ?: "bn"
                val numeral = settingsDoc.safeString("numeralSystem") ?: local?.numeralSystem ?: "bn"
                val currency = settingsDoc.safeString("defaultCurrency") ?: local?.defaultCurrency ?: "BDT"
                val symbol = settingsDoc.safeString("currencySymbol") ?: local?.currencySymbol ?: "৳"
                val hideBal = settingsDoc.safeBoolean("hideBalances") ?: local?.hideBalances ?: false
                val pinEn = settingsDoc.safeBoolean("pinEnabled") ?: local?.pinEnabled ?: false
                val pin = settingsDoc.safeString("pinCode") ?: local?.pinCode ?: ""
                val dailyReminder = settingsDoc.safeBoolean("dailyReminderEnabled") ?: local?.dailyReminderEnabled ?: true
                val dark = settingsDoc.safeBoolean("isDarkMode") ?: local?.isDarkMode ?: false
                val systemTheme = settingsDoc.safeBoolean("useSystemTheme") ?: local?.useSystemTheme ?: true

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
