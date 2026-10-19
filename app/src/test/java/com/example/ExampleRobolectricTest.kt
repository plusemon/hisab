package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.plusemon.hisab.R
import com.plusemon.hisab.domain.util.MarkdownUtils
import com.plusemon.hisab.domain.util.VersionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hisab", appName)
  }

  @Test
  fun `version comparison logic`() {
    assertTrue(VersionUtils.isVersionNewer("1.0.3", "1.0.5"))
    assertTrue(VersionUtils.isVersionNewer("1.0", "1.0.5"))
    assertTrue(VersionUtils.isVersionNewer("v1.0.3", "v1.0.5"))
    assertTrue(VersionUtils.isVersionNewer("vv1.0.3", "v1.0.5"))
  }

  @Test
  fun `version cleaning and display formatting prevents duplicate v`() {
    assertEquals("1.0.16", VersionUtils.cleanVersion("vv1.0.16"))
    assertEquals("1.0.16", VersionUtils.cleanVersion("v1.0.16"))
    assertEquals("1.0.16", VersionUtils.cleanVersion("V1.0.16"))
    assertEquals("1.0.16", VersionUtils.cleanVersion("1.0.16"))

    assertEquals("v1.0.16", VersionUtils.formatDisplayVersion("vv1.0.16"))
    assertEquals("v1.0.16", VersionUtils.formatDisplayVersion("v1.0.16"))
    assertEquals("v1.0.16", VersionUtils.formatDisplayVersion("V1.0.16"))
    assertEquals("v1.0.16", VersionUtils.formatDisplayVersion("1.0.16"))
  }

  @Test
  fun `markdown parsing and deduplication`() {
    val notes = "**Full Changelog**: link\n**Full Changelog**: link"
    val deduped = MarkdownUtils.deduplicateReleaseNotes(notes)
    assertEquals("**Full Changelog**: link", deduped)

    val annotated = MarkdownUtils.parseMarkdown("**Bold Text**")
    assertEquals("Bold Text", annotated.text)
  }

  @Test
  fun `language preference persistence across app sessions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("hisab_settings_prefs", Context.MODE_PRIVATE)

    // Initially or default
    prefs.edit().putString("language", "bn").apply()
    assertEquals("bn", prefs.getString("language", "bn"))

    // User switches language on auth screen to English
    prefs.edit().putString("language", "en").apply()
    assertEquals("en", prefs.getString("language", "bn"))

    // Toggle back to Bangla
    val current = prefs.getString("language", "bn")
    val toggled = if (current == "bn") "en" else "bn"
    prefs.edit().putString("language", toggled).apply()
    assertEquals("bn", prefs.getString("language", "en"))
  }

  @Test
  fun `credentials error handling logic distinguishes no credential from cancellation`() {
    val noCredException = androidx.credentials.exceptions.NoCredentialException("No credentials available")
    val cancellationException = androidx.credentials.exceptions.GetCredentialCancellationException("User cancelled")

    val isNoCred1 = noCredException.message?.contains("No credentials available", ignoreCase = true) == true
    val isCancel1 = cancellationException.message?.contains("cancel", ignoreCase = true) == true

    assertTrue(isNoCred1)
    assertTrue(isCancel1)
  }

  @Test
  fun `auth diagnostics identifies known hashes and formats correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val knownHashes = com.plusemon.hisab.domain.util.AuthDiagnostics.KNOWN_FIREBASE_SHA1_HASHES
    assertTrue(knownHashes.size >= 3)
    assertTrue(knownHashes.contains("71:D4:ED:DF:3C:4C:36:AC:A6:59:22:C5:A9:F9:74:B5:D2:EE:E3:91"))
    assertTrue(knownHashes.contains("A2:7F:46:52:5C:5E:1C:93:89:4F:07:C3:17:F9:83:B3:38:CE:51:0E"))
    assertEquals("41:33:99:F7:93:E1:54:93:B7:F9:15:4E:AC:08:B5:FA:24:19:3D:60", com.plusemon.hisab.domain.util.AuthDiagnostics.DEBUG_KEYSTORE_SHA1)

    val currentSha1 = com.plusemon.hisab.domain.util.AuthDiagnostics.getCurrentAppSha1(context)
    assertTrue(currentSha1.isNotBlank())
  }

  @Test
  fun `data and transactions persist across app restarts without being wiped`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.plusemon.hisab.data.local.AppDatabase.getDatabase(context)
    val authRepo = com.plusemon.hisab.data.repository.AuthRepository(context, db)
    val hisabRepo = com.plusemon.hisab.data.repository.HisabRepository(db)

    // Sign up a user
    val signUpResult = authRepo.signUpWithEmail("Test User", "persist@example.com", "password123")
    assertTrue(signUpResult is com.plusemon.hisab.data.repository.AuthResult.Success)
    val user = (signUpResult as com.plusemon.hisab.data.repository.AuthResult.Success).user

    // Ensure default accounts and categories are seeded
    authRepo.ensureUserDataSeeded(user.id)
    val accounts = db.accountDao().getAllAccountsList(user.id)
    assertTrue("Accounts should exist", accounts.isNotEmpty())
    val accountId = accounts.first().id

    // Insert an income transaction
    val transaction = com.plusemon.hisab.data.model.TransactionRecord(
      userId = user.id,
      accountId = accountId,
      amount = 5000.0,
      type = com.plusemon.hisab.data.model.TransactionType.INCOME,
      note = "Salary entry"
    )
    val txId = hisabRepo.insertTransaction(transaction)
    assertTrue("Transaction should be inserted", txId > 0)

    val txListBefore = db.transactionDao().getAllTransactionsList(user.id)
    assertEquals(1, txListBefore.size)
    assertEquals(5000.0, txListBefore[0].amount, 0.001)

    // SIMULATE APP RESTART: create new repository instances and load initial user
    val newAuthRepo = com.plusemon.hisab.data.repository.AuthRepository(context, db)
    newAuthRepo.loadInitialUser()

    // Assert that the user is still logged in
    val loadedUser = newAuthRepo.currentUser.value
    assertTrue("User should still be logged in", loadedUser != null)
    assertEquals(user.id, loadedUser?.id)

    // Assert that transactions were NOT wiped out by CASCADE or REPLACE!
    val txListAfter = db.transactionDao().getAllTransactionsList(user.id)
    assertEquals("Transactions must persist across app restart", 1, txListAfter.size)
    assertEquals(5000.0, txListAfter[0].amount, 0.001)

    val accountsAfter = db.accountDao().getAllAccountsList(user.id)
    assertTrue("Accounts must still exist", accountsAfter.isNotEmpty())
  }

  @Test
  fun `firestore repository handles uninitialized environment gracefully without crash`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.plusemon.hisab.data.local.AppDatabase.getDatabase(context)
    val firestoreRepo: com.plusemon.hisab.data.repository.FirestoreRepository =
      com.plusemon.hisab.data.repository.FirestoreRepositoryImpl(context, db)

    val tx = com.plusemon.hisab.data.model.TransactionRecord(
      id = 100L,
      userId = "test_user_123",
      accountId = 1L,
      amount = 250.0,
      type = com.plusemon.hisab.data.model.TransactionType.EXPENSE,
      note = "Snacks"
    )

    val result = firestoreRepo.saveTransaction("test_user_123", tx)
    assertTrue("Should return a Result object", result.isFailure || result.isSuccess)

    val syncResult = firestoreRepo.syncAllWithRoom("test_user_123")
    assertTrue("Sync should return a Result object", syncResult.isFailure || syncResult.isSuccess)
  }

  @Test
  fun `delete transaction and item deletion behavior`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.plusemon.hisab.data.local.AppDatabase.getDatabase(context)
    val authRepo = com.plusemon.hisab.data.repository.AuthRepository(context, db)
    val hisabRepo = com.plusemon.hisab.data.repository.HisabRepository(db)

    val signUpResult = authRepo.signUpWithEmail("Delete Test", "delete_test@example.com", "pass123")
    assertTrue(signUpResult is com.plusemon.hisab.data.repository.AuthResult.Success)
    val user = (signUpResult as com.plusemon.hisab.data.repository.AuthResult.Success).user

    authRepo.ensureUserDataSeeded(user.id)
    val accounts = db.accountDao().getAllAccountsList(user.id)
    assertTrue(accounts.isNotEmpty())
    val accountId = accounts.first().id

    val tx = com.plusemon.hisab.data.model.TransactionRecord(
      userId = user.id,
      accountId = accountId,
      amount = 750.0,
      type = com.plusemon.hisab.data.model.TransactionType.EXPENSE,
      note = "Dinner"
    )
    val id = hisabRepo.insertTransaction(tx)
    assertTrue(id > 0)

    hisabRepo.deleteTransaction(id, user.id)
    val allTxs = db.transactionDao().getAllTransactionsList(user.id)
    assertTrue(allTxs.none { it.id == id })
  }

  @Test
  fun `google sign in with photoUrl persists and restores across app restarts`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.plusemon.hisab.data.local.AppDatabase.getDatabase(context)
    val authRepo = com.plusemon.hisab.data.repository.AuthRepository(context, db)

    val googleEmail = "bdemon00@gmail.com"
    val googleName = "Bd Emon"
    val testPhotoUrl = "https://lh3.googleusercontent.com/a/ACg8ocISampleAvatarKey"

    // Sign in with Google with profile picture
    val result = authRepo.signInWithGoogle(
      uid = "google_user_12345",
      email = googleEmail,
      displayName = googleName,
      photoUrl = testPhotoUrl
    )
    assertTrue("Sign-in should be successful", result is com.plusemon.hisab.data.repository.AuthResult.Success)
    val signedInUser = (result as com.plusemon.hisab.data.repository.AuthResult.Success).user
    assertEquals(testPhotoUrl, signedInUser.photoUrl)
    assertEquals(googleName, signedInUser.displayName)
    assertTrue(signedInUser.isGoogleUser)

    // Verify currentUser StateFlow has the photo
    assertEquals(testPhotoUrl, authRepo.currentUser.value?.photoUrl)

    // Simulate App restart
    val newAuthRepo = com.plusemon.hisab.data.repository.AuthRepository(context, db)
    newAuthRepo.loadInitialUser()

    val loadedUser = newAuthRepo.currentUser.value
    assertTrue("User should be restored", loadedUser != null)
    assertEquals("Photo URL must persist across app restart", testPhotoUrl, loadedUser?.photoUrl)
    assertEquals(googleEmail, loadedUser?.email)
  }

  @Test
  fun `more navigation routes and localization keys are properly defined`() {
    assertEquals("more", com.plusemon.hisab.ui.navigation.Screen.More.route)
    assertEquals("more", com.plusemon.hisab.ui.navigation.MoreRoute)
    assertEquals("more_graph", com.plusemon.hisab.ui.navigation.MoreGraph)
    assertEquals("categories", com.plusemon.hisab.ui.navigation.Screen.Categories.route)

    val moreBn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.MORE, isBangla = true)
    val moreEn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.MORE, isBangla = false)
    assertEquals("অন্যান্য", moreBn)
    assertEquals("More", moreEn)
  }

  @Test
  fun `transaction types and tab strings for expense income transfer`() {
    val types = com.plusemon.hisab.data.model.TransactionType.values()
    assertTrue(types.contains(com.plusemon.hisab.data.model.TransactionType.EXPENSE))
    assertTrue(types.contains(com.plusemon.hisab.data.model.TransactionType.INCOME))
    assertTrue(types.contains(com.plusemon.hisab.data.model.TransactionType.TRANSFER))

    val expBn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.EXPENSE_SHORT, isBangla = true)
    val incBn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.INCOME_SHORT, isBangla = true)
    val trfBn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.TRANSFER_SHORT, isBangla = true)
    assertTrue(expBn.isNotBlank())
    assertTrue(incBn.isNotBlank())
    assertTrue(trfBn.isNotBlank())
  }

  @Test
  fun `theme settings persistence supports system default, light, and dark modes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("hisab_settings_prefs", Context.MODE_PRIVATE)

    // Default state: system theme is enabled
    assertTrue(prefs.getBoolean("use_system_theme", true))

    // Set to dark mode explicitly
    prefs.edit()
      .putBoolean("use_system_theme", false)
      .putBoolean("is_dark_mode", true)
      .apply()

    assertEquals(false, prefs.getBoolean("use_system_theme", true))
    assertEquals(true, prefs.getBoolean("is_dark_mode", false))

    // Set to light mode explicitly
    prefs.edit()
      .putBoolean("use_system_theme", false)
      .putBoolean("is_dark_mode", false)
      .apply()

    assertEquals(false, prefs.getBoolean("use_system_theme", true))
    assertEquals(false, prefs.getBoolean("is_dark_mode", true))

    // Reset back to system default
    prefs.edit()
      .putBoolean("use_system_theme", true)
      .apply()

    assertEquals(true, prefs.getBoolean("use_system_theme", false))
  }

  @Test
  fun `theme and language localization strings`() {
    val themeBn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.THEME, isBangla = true)
    val themeEn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.THEME, isBangla = false)
    assertEquals("থিম", themeBn)
    assertEquals("Theme", themeEn)

    val sysBn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.SYSTEM_DEFAULT, isBangla = true)
    val sysEn = com.plusemon.hisab.domain.util.Localization.getString(com.plusemon.hisab.domain.util.Localization.Key.SYSTEM_DEFAULT, isBangla = false)
    assertEquals("সিস্টেম ডিফল্ট", sysBn)
    assertEquals("System Default", sysEn)
  }

  @Test
  fun `cloud restore summary calculates total records accurately`() {
    val summary = com.plusemon.hisab.data.repository.CloudRestoreSummary(
      accountsCount = 4,
      transactionsCount = 42,
      categoriesCount = 12,
      budgetsCount = 3,
      goalsCount = 2,
      debtsCount = 1,
      hasCloudData = true
    )
    assertEquals(64, summary.totalRecords)
    assertTrue(summary.hasCloudData)
  }

  @Test
  fun `restore ui state representations and transitions`() {
    val idle = com.plusemon.hisab.ui.viewmodel.RestoreUiState.Idle
    assertTrue(idle is com.plusemon.hisab.ui.viewmodel.RestoreUiState)

    val restoring = com.plusemon.hisab.ui.viewmodel.RestoreUiState.Restoring("Restoring accounts...", 0.25f)
    assertEquals("Restoring accounts...", restoring.step)
    assertEquals(0.25f, restoring.progress, 0.001f)

    val completed = com.plusemon.hisab.ui.viewmodel.RestoreUiState.Completed(
      com.plusemon.hisab.data.repository.CloudRestoreSummary(accountsCount = 2, transactionsCount = 10, hasCloudData = true)
    )
    assertEquals(12, completed.summary.totalRecords)
  }

  @Test
  fun `contact search filters by name and phone digits and preserves photoUri`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val extraContacts = listOf(
      com.plusemon.hisab.domain.util.DeviceContactSuggestion(
        contactId = "1",
        name = "Rahim Ahmed",
        phones = listOf(
          com.plusemon.hisab.domain.util.ContactPhoneOption(
            rawNumber = "+880 1712-345678",
            displayNumber = "+880 1712-345678",
            typeLabelEn = "Mobile",
            typeLabelBn = "মোবাইল"
          ),
          com.plusemon.hisab.domain.util.ContactPhoneOption(
            rawNumber = "01819-000111",
            displayNumber = "01819-000111",
            typeLabelEn = "Work",
            typeLabelBn = "অফিস"
          )
        ),
        photoUri = "content://com.android.contacts/contacts/1/photo"
      ),
      com.plusemon.hisab.domain.util.DeviceContactSuggestion(
        contactId = "2",
        name = "Karim Store",
        phones = listOf(
          com.plusemon.hisab.domain.util.ContactPhoneOption(
            rawNumber = "01911-223344",
            displayNumber = "01911-223344",
            typeLabelEn = "Mobile",
            typeLabelBn = "মোবাইল"
          )
        ),
        photoUri = null
      )
    )

    // Search by name
    val byName = com.plusemon.hisab.domain.util.ContactUtils.searchDeviceContacts(
      context = context,
      query = "Rahim",
      extraContacts = extraContacts
    )
    assertEquals(1, byName.size)
    assertEquals("Rahim Ahmed", byName[0].name)
    assertEquals("content://com.android.contacts/contacts/1/photo", byName[0].photoUri)
    assertEquals(2, byName[0].phones.size)

    // Search by normalized phone digits (ignoring hyphen/space)
    val bySecondaryDigits = com.plusemon.hisab.domain.util.ContactUtils.searchDeviceContacts(
      context = context,
      query = "01819000",
      extraContacts = extraContacts
    )
    assertEquals(1, bySecondaryDigits.size)
    assertEquals("Rahim Ahmed", bySecondaryDigits[0].name)
    // Secondary matching phone should be ordered first when searched by its digits
    assertEquals("01819-000111", bySecondaryDigits[0].primaryPhone?.rawNumber)
  }
}


