package com.plusemon.hisab.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.plusemon.hisab.data.local.AppDatabase
import com.plusemon.hisab.data.model.AccountType
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionRecord
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.UserAccount
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
    suspend fun syncAllWithRoom(userId: String): Result<Int>
    fun observeRemoteTransactions(userId: String): Flow<List<TransactionRecord>>
}

class FirestoreRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase
) : FirestoreRepository {

    private val transactionDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()

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
            Log.d(TAG, "Transaction ${transaction.id} saved to Firestore successfully for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save transaction ${transaction.id} to Firestore: ${e.message}", e)
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
            Log.d(TAG, "Transaction $transactionId deleted from Firestore for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete transaction $transactionId from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

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
            Log.d(TAG, "Account ${account.id} saved to Firestore for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save account ${account.id} to Firestore: ${e.message}", e)
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
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing transaction document ${doc.id}: ${e.message}")
                        null
                    }
                }
                Result.success(records)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch transactions from Firestore: ${e.message}", e)
                Result.failure(e)
            }
        }

    override suspend fun syncAllWithRoom(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized")
        )
        try {
            // 1. Sync accounts first to ensure foreign keys in Room are satisfied
            syncAccounts(userId, firestore)

            // 2. Sync categories to ensure category relationships are intact
            syncCategories(userId, firestore)

            // 3. Fetch remote transactions from Firestore with strict timeout
            val remoteSnapshot = try {
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_TRANSACTIONS)
                    .get()
                    .awaitTask(7000L)
            } catch (e: Exception) {
                Log.w(TAG, "Fetching remote transactions timed out or error: ${e.message}")
                null
            }

            val remoteMap = mutableMapOf<Long, TransactionRecord>()
            if (remoteSnapshot != null) {
                for (doc in remoteSnapshot.documents) {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                    val accountId = doc.getLong("accountId") ?: continue
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

                // 4. Upsert remote transactions into local Room DB immediately
                if (remoteMap.isNotEmpty()) {
                    transactionDao.insertTransactions(remoteMap.values.toList())
                }
            }

            // 5. Check if local Room DB has transactions not yet uploaded to Firestore
            val localTransactions = transactionDao.getAllTransactionsList(userId)
            var uploadedCount = 0
            for (localTx in localTransactions) {
                if (!remoteMap.containsKey(localTx.id)) {
                    try {
                        saveTransaction(userId, localTx)
                        uploadedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed uploading tx ${localTx.id}: ${e.message}")
                    }
                }
            }

            Log.i(TAG, "Sync complete for $userId: ${remoteMap.size} downloaded, $uploadedCount uploaded")
            Result.success(remoteMap.size + uploadedCount)
        } catch (e: Exception) {
            Log.w(TAG, "syncAllWithRoom failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncAccounts(userId: String, firestore: FirebaseFirestore) {
        try {
            val accountsCollection = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_ACCOUNTS)

            val remoteAccSnapshot = try {
                accountsCollection.get().awaitTask(5000L)
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching remote accounts: ${e.message}")
                null
            }

            val remoteAccounts = remoteAccSnapshot?.documents?.mapNotNull { doc ->
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
            }

            // Upload any local accounts to Firestore safely
            val localAccounts = accountDao.getAllAccountsList(userId)
            val remoteIds = remoteAccounts.map { it.id }.toSet()
            for (acc in localAccounts) {
                if (!remoteIds.contains(acc.id)) {
                    try {
                        val accData = hashMapOf(
                            "id" to acc.id,
                            "userId" to userId,
                            "name" to acc.name,
                            "type" to acc.type.name,
                            "currencyCode" to acc.currencyCode,
                            "startingBalance" to acc.startingBalance,
                            "colorHex" to acc.colorHex,
                            "iconName" to acc.iconName,
                            "isArchived" to acc.isArchived,
                            "createdAt" to acc.createdAt,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        accountsCollection.document(acc.id.toString()).set(accData, SetOptions.merge()).awaitTask(3000L)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed uploading local account ${acc.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncAccounts error: ${e.message}")
        }
    }

    private suspend fun syncCategories(userId: String, firestore: FirebaseFirestore) {
        try {
            val catCollection = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CATEGORIES)

            val remoteCatSnapshot = try {
                catCollection.get().awaitTask(5000L)
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching remote categories: ${e.message}")
                null
            }

            val remoteCats = remoteCatSnapshot?.documents?.mapNotNull { doc ->
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
            }

            // Upload any local categories to Firestore safely
            val localCats = categoryDao.getAllCategoriesList(userId)
            val remoteCatIds = remoteCats.map { it.id }.toSet()
            for (cat in localCats) {
                if (!remoteCatIds.contains(cat.id)) {
                    try {
                        val catData = hashMapOf(
                            "id" to cat.id,
                            "userId" to userId,
                            "nameBn" to cat.nameBn,
                            "nameEn" to cat.nameEn,
                            "type" to cat.type.name,
                            "iconName" to cat.iconName,
                            "colorHex" to cat.colorHex,
                            "isDefault" to cat.isDefault,
                            "isArchived" to cat.isArchived,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        catCollection.document(cat.id.toString()).set(catData, SetOptions.merge()).awaitTask(3000L)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed uploading local category ${cat.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncCategories error: ${e.message}")
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
    }
}
