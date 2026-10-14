package com.plusemon.hisab.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.plusemon.hisab.data.local.AccountDao
import com.plusemon.hisab.data.local.AppDatabase
import com.plusemon.hisab.data.local.CategoryDao
import com.plusemon.hisab.data.local.SettingsDao
import com.plusemon.hisab.data.local.UserDao
import com.plusemon.hisab.data.model.AccountType
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.User
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.data.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val firestoreRepository: FirestoreRepository? = null
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisab_auth_prefs", Context.MODE_PRIVATE)

    private val userDao: UserDao = db.userDao()
    private val accountDao: AccountDao = db.accountDao()
    private val categoryDao: CategoryDao = db.categoryDao()
    private val settingsDao: SettingsDao = db.settingsDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        val savedUserId = prefs.getString("current_user_id", null)
        val savedEmail = prefs.getString("current_user_email", null)
        val savedName = prefs.getString("current_user_name", null)
        val savedPhoto = prefs.getString("current_user_photo", null)
        val savedIsGoogle = prefs.getBoolean("current_user_is_google", false)
        if (!savedUserId.isNullOrEmpty() && !savedEmail.isNullOrEmpty()) {
            _currentUser.value = User(
                id = savedUserId,
                email = savedEmail,
                displayName = savedName ?: "User",
                photoUrl = savedPhoto,
                isGoogleUser = savedIsGoogle
            )
        }
    }

    suspend fun loadInitialUser() = withContext(Dispatchers.IO) {
        val savedUserId = prefs.getString("current_user_id", null)
        val savedEmail = prefs.getString("current_user_email", null)

        var user: User? = null
        if (!savedUserId.isNullOrEmpty()) {
            user = userDao.getUserDirect(savedUserId)
        }
        if (user == null && !savedEmail.isNullOrEmpty()) {
            user = userDao.getUserByEmail(savedEmail)
        }
        if (user == null) {
            // Check Firebase Auth if available
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (fbUser != null && !fbUser.email.isNullOrEmpty()) {
                        user = userDao.getUserByEmail(fbUser.email!!)
                        if (user == null) {
                            user = User(
                                id = fbUser.uid,
                                email = fbUser.email!!,
                                displayName = fbUser.displayName ?: fbUser.email!!.substringBefore("@"),
                                photoUrl = fbUser.photoUrl?.toString(),
                                isGoogleUser = true
                            )
                            userDao.insertUser(user)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthRepository", "Firebase auth check failed: ${e.message}")
            }
        }
        if (user == null) {
            val allUsers = userDao.getAllUsersDirect()
            if (allUsers.isNotEmpty()) {
                user = allUsers.first()
            }
        }

        if (user != null) {
            if (user.photoUrl.isNullOrEmpty()) {
                val savedPhoto = prefs.getString("current_user_photo", null)
                if (!savedPhoto.isNullOrEmpty()) {
                    val updated = user.copy(photoUrl = savedPhoto)
                    userDao.updateUser(updated)
                    user = updated
                } else {
                    try {
                        if (FirebaseApp.getApps(context).isNotEmpty()) {
                            val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            val fbPhoto = fbUser?.photoUrl?.toString()
                            if (!fbPhoto.isNullOrEmpty() && (fbUser.email == user.email || user.isGoogleUser)) {
                                val updated = user.copy(photoUrl = fbPhoto, isGoogleUser = true)
                                userDao.updateUser(updated)
                                user = updated
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("AuthRepository", "Firebase photoUrl check error: ${e.message}")
                    }
                }
            }
            ensureUserDataSeeded(user.id)
            saveSession(user)
        } else {
            val cached = _currentUser.value
            if (cached != null) {
                userDao.insertUser(cached)
                ensureUserDataSeeded(cached.id)
                saveSession(cached)
            } else {
                _currentUser.value = null
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun signInWithGoogle(
        uid: String? = null,
        email: String,
        displayName: String,
        photoUrl: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            var user = userDao.getUserByEmail(email)
            val userId = user?.id ?: uid ?: UUID.randomUUID().toString()

            if (user == null) {
                // Create new Google User with real credentials
                user = User(
                    id = userId,
                    email = email,
                    displayName = displayName.ifBlank { "User" },
                    photoUrl = photoUrl,
                    isGoogleUser = true
                )
                userDao.insertUser(user)
                ensureUserDataSeeded(userId)
            } else {
                // Update photo / name if needed, ALWAYS preserve existing user.id to maintain foreign keys
                val updated = user.copy(
                    displayName = displayName.ifBlank { user.displayName },
                    photoUrl = photoUrl ?: user.photoUrl
                )
                userDao.updateUser(updated)
                user = updated
                ensureUserDataSeeded(user.id)
            }

            // Sync user document to Firestore with real uid and email
            syncFirestoreUser(user.id, email, displayName, photoUrl)

            saveSession(user)
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to process Google sign in: ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "Failed to sign in with Google")
        }
    }

    private fun syncFirestoreUser(uid: String, email: String, displayName: String, photoUrl: String?) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val firestore = FirebaseFirestore.getInstance()
                val userData = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "isGoogleUser" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid).set(userData, SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firestore sync skipped or error: ${e.message}")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            return@withContext AuthResult.Error("Please fill in both email and password")
        }
        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext AuthResult.Error("No account found with this email. Please create an account.")

        if (user.isGoogleUser && user.passwordHash == null) {
            return@withContext AuthResult.Error("This account was created with Google Sign-In. Please tap 'Continue with Google'.")
        }

        val hashed = hashPassword(pass)
        if (user.passwordHash != hashed) {
            return@withContext AuthResult.Error("Incorrect password. Please verify and try again.")
        }

        ensureUserDataSeeded(user.id)
        saveSession(user)
        AuthResult.Success(user)
    }

    suspend fun signUpWithEmail(name: String, email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || pass.isBlank()) {
            return@withContext AuthResult.Error("Please fill in all fields.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (pass.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters long.")
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext AuthResult.Error("An account with this email already exists. Please Sign In.")
        }

        val newId = UUID.randomUUID().toString()
        val user = User(
            id = newId,
            email = trimmedEmail,
            displayName = trimmedName,
            isGoogleUser = false,
            passwordHash = hashPassword(pass)
        )
        userDao.insertUser(user)
        ensureUserDataSeeded(newId)

        saveSession(user)
        AuthResult.Success(user)
    }

    suspend fun ensureUserDataSeeded(userId: String) = withContext(Dispatchers.IO) {
        val accCount = accountDao.getAccountCount(userId)
        if (accCount == 0) {
            // Check if remote cloud data exists first!
            var hasRestoredCloudData = false
            if (firestoreRepository != null) {
                try {
                    val restoreResult = firestoreRepository.restoreCloudUserData(userId)
                    if (restoreResult.isSuccess && restoreResult.getOrNull() == true) {
                        hasRestoredCloudData = true
                        Log.i("AuthRepository", "Remote cloud data cleanly restored for user $userId")
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Cloud restore attempt during seed check: ${e.message}")
                }
            }

            // If remote data was found and restored, we don't inject default starter accounts/categories
            if (hasRestoredCloudData || accountDao.getAccountCount(userId) > 0) {
                return@withContext
            }

            // Genuinely brand new user with no existing cloud or local records:
            // 1. Seed Accounts
            val defaultAccounts = listOf(
                UserAccount(
                    userId = userId,
                    name = "Cash",
                    type = AccountType.CASH,
                    currencyCode = "BDT",
                    startingBalance = 0.0,
                    colorHex = "#0F766E",
                    iconName = "payments"
                ),
                UserAccount(
                    userId = userId,
                    name = "bKash",
                    type = AccountType.MOBILE_WALLET,
                    currencyCode = "BDT",
                    startingBalance = 0.0,
                    colorHex = "#E11475",
                    iconName = "account_balance_wallet"
                ),
                UserAccount(
                    userId = userId,
                    name = "Nagad",
                    type = AccountType.MOBILE_WALLET,
                    currencyCode = "BDT",
                    startingBalance = 0.0,
                    colorHex = "#F97316",
                    iconName = "account_balance_wallet"
                ),
                UserAccount(
                    userId = userId,
                    name = "Bank Account",
                    type = AccountType.BANK,
                    currencyCode = "BDT",
                    startingBalance = 0.0,
                    colorHex = "#2563EB",
                    iconName = "account_balance"
                )
            )
            accountDao.insertAccounts(defaultAccounts)

            // Upload starter accounts to cloud
            firestoreRepository?.let { repo ->
                val insertedAccounts = accountDao.getAllAccountsList(userId)
                for (acc in insertedAccounts) {
                    try { repo.saveAccount(userId, acc) } catch (e: Exception) {}
                }
            }
        }

        // 2. Seed Expense & Income Categories if none exist
        val catCount = categoryDao.getCategoryCount(userId)
        if (catCount == 0) {
            val defaultCategories = listOf(
                // Expense Categories
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

                // Income Categories
                Category(userId = userId, nameBn = "বেতন", nameEn = "Salary", type = TransactionType.INCOME, iconName = "payments", colorHex = "#10B981", isDefault = true),
                Category(userId = userId, nameBn = "ব্যবসা", nameEn = "Business", type = TransactionType.INCOME, iconName = "store", colorHex = "#2563EB", isDefault = true),
                Category(userId = userId, nameBn = "ফ্রিল্যান্সিং", nameEn = "Freelancing", type = TransactionType.INCOME, iconName = "laptop", colorHex = "#7C3AED", isDefault = true),
                Category(userId = userId, nameBn = "বিনিয়োগ ও লভ্যাংশ", nameEn = "Investments", type = TransactionType.INCOME, iconName = "trending_up", colorHex = "#F59E0B", isDefault = true),
                Category(userId = userId, nameBn = "উপহার ও অনুদান", nameEn = "Gifts & Grants", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#EC4899", isDefault = true),
                Category(userId = userId, nameBn = "অন্যান্য আয়", nameEn = "Other Income", type = TransactionType.INCOME, iconName = "attach_money", colorHex = "#0D9488", isDefault = true),
                Category(userId = userId, nameBn = "ধার গ্রহণ", nameEn = "Loan Received", type = TransactionType.INCOME, iconName = "account_balance_wallet", colorHex = "#10B981", isDefault = true),
                Category(userId = userId, nameBn = "ধার ফেরত গ্রহণ", nameEn = "Loan Collection", type = TransactionType.INCOME, iconName = "savings", colorHex = "#0D9488", isDefault = true)
            )
            categoryDao.insertCategories(defaultCategories)

            // Upload starter categories to cloud
            firestoreRepository?.let { repo ->
                val insertedCats = categoryDao.getAllCategoriesList(userId)
                for (cat in insertedCats) {
                    try { repo.saveCategory(userId, cat) } catch (e: Exception) {}
                }
            }
        }

        // 3. Seed Default User Settings if not present
        val existingSettings = settingsDao.getSettingsDirect(userId)
        if (existingSettings == null) {
            val settingsPrefs = context.getSharedPreferences("hisab_settings_prefs", Context.MODE_PRIVATE)
            val preferredLang = settingsPrefs.getString("language", "bn") ?: "bn"
            val preferredNumeral = settingsPrefs.getString("numeral_system", if (preferredLang == "bn") "bn" else "en") ?: "bn"

            val settings = UserSettings(
                userId = userId,
                language = preferredLang,
                numeralSystem = preferredNumeral,
                defaultCurrency = "BDT",
                currencySymbol = "৳"
            )
            settingsDao.insertOrUpdate(settings)

            // Upload starter settings to cloud
            firestoreRepository?.let { repo ->
                try { repo.saveUserSettings(userId, settings) } catch (e: Exception) {}
            }
        }
    }

    private fun saveSession(user: User) {
        prefs.edit()
            .putString("current_user_id", user.id)
            .putString("current_user_email", user.email)
            .putString("current_user_name", user.displayName)
            .putString("current_user_photo", user.photoUrl)
            .putBoolean("current_user_is_google", user.isGoogleUser)
            .apply()
        _currentUser.value = user
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase sign out error: ${e.message}")
        }
        _currentUser.value = null
    }

    suspend fun deleteAccountAndData(userId: String) = withContext(Dispatchers.IO) {
        db.userDao().deleteUser(userId)
        prefs.edit().clear().apply()
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase sign out error: ${e.message}")
        }
        _currentUser.value = null
    }
}
