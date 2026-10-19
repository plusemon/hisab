package com.plusemon.hisab.domain.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.plusemon.hisab.ui.theme.IncomeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactPhoneOption(
    val rawNumber: String,
    val displayNumber: String,
    val typeLabelEn: String,
    val typeLabelBn: String
)

data class PickedContact(
    val name: String,
    val phones: List<ContactPhoneOption>,
    val photoUri: String? = null
)

data class DeviceContactSuggestion(
    val contactId: String,
    val name: String,
    val phones: List<ContactPhoneOption>,
    val photoUri: String? = null
) {
    val primaryPhone: ContactPhoneOption?
        get() = phones.firstOrNull()
}

object ContactUtils {

    @Volatile
    private var cachedContacts: List<DeviceContactSuggestion>? = null

    @Volatile
    private var lastCacheTimeMs: Long = 0L

    private const val CACHE_TTL_MS = 45_000L

    fun hasContactsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun invalidateCache() {
        cachedContacts = null
        lastCacheTimeMs = 0L
    }

    private fun mapPhoneTypeLabel(type: Int, customLabel: String?): Pair<String, String> {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> Pair("Mobile", "মোবাইল")
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> Pair("Home", "বাসা")
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> Pair("Work", "অফিস")
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> Pair("Main", "প্রধান")
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> Pair("Other", "অন্যান্য")
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> {
                val lbl = if (!customLabel.isNullOrBlank()) customLabel else "Other"
                Pair(lbl, lbl)
            }
            else -> Pair("Mobile", "মোবাইল")
        }
    }

    fun loadAllDeviceContacts(
        context: Context,
        forceRefresh: Boolean = false
    ): List<DeviceContactSuggestion> {
        if (!hasContactsPermission(context)) return emptyList()

        val now = System.currentTimeMillis()
        val existing = cachedContacts
        if (!forceRefresh && existing != null && (now - lastCacheTimeMs) < CACHE_TTL_MS) {
            return existing
        }

        val contentResolver = context.contentResolver
        val groupedMap = LinkedHashMap<String, MutableContactBuilder>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (cursor.moveToNext()) {
                    val rawName = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                    val rawNumber = if (numIdx != -1) cursor.getString(numIdx)?.trim().orEmpty() else ""
                    if (rawName.isBlank() && rawNumber.isBlank()) continue

                    val digitsOnly = rawNumber.filter { it.isDigit() }
                    if (digitsOnly.length < 3) continue

                    val contactId = if (idIdx != -1) cursor.getString(idIdx)?.trim().orEmpty() else ""
                    val groupKey = contactId.ifBlank { rawName.lowercase() }

                    val thumbUri = if (thumbIdx != -1) cursor.getString(thumbIdx)?.takeIf { it.isNotBlank() } else null
                    val fullPhotoUri = if (photoIdx != -1) cursor.getString(photoIdx)?.takeIf { it.isNotBlank() } else null
                    val resolvedPhotoUri = thumbUri ?: fullPhotoUri

                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val customLabel = if (labelIdx != -1) cursor.getString(labelIdx) else null

                    val builder = groupedMap.getOrPut(groupKey) {
                        MutableContactBuilder(
                            contactId = groupKey,
                            name = rawName.ifBlank { rawNumber },
                            photoUri = resolvedPhotoUri
                        )
                    }

                    if (builder.photoUri == null && resolvedPhotoUri != null) {
                        builder.photoUri = resolvedPhotoUri
                    }
                    if (builder.name.isBlank() && rawName.isNotBlank()) {
                        builder.name = rawName
                    }

                    val comparisonKey = if (digitsOnly.length >= 8) digitsOnly.takeLast(8) else digitsOnly
                    if (builder.seenNumberKeys.add(comparisonKey)) {
                        val (labelEn, labelBn) = mapPhoneTypeLabel(type, customLabel)
                        builder.phones.add(
                            ContactPhoneOption(
                                rawNumber = rawNumber,
                                displayNumber = rawNumber,
                                typeLabelEn = labelEn,
                                typeLabelBn = labelBn
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = groupedMap.values
            .filter { it.phones.isNotEmpty() }
            .map {
                DeviceContactSuggestion(
                    contactId = it.contactId,
                    name = it.name,
                    phones = it.phones.toList(),
                    photoUri = it.photoUri
                )
            }

        cachedContacts = result
        lastCacheTimeMs = now
        return result
    }

    private class MutableContactBuilder(
        val contactId: String,
        var name: String,
        var photoUri: String?
    ) {
        val phones = mutableListOf<ContactPhoneOption>()
        val seenNumberKeys = mutableSetOf<String>()
    }

    fun searchDeviceContacts(
        context: Context,
        query: String,
        limit: Int = 20,
        extraContacts: List<DeviceContactSuggestion> = emptyList()
    ): List<DeviceContactSuggestion> {
        val deviceContacts = loadAllDeviceContacts(context)

        // Merge extraContacts (e.g. saved in-app contacts) with deviceContacts without duplicating
        val mergedList = mutableListOf<DeviceContactSuggestion>()
        val seenNamesOrPhones = mutableSetOf<String>()

        // Index device photos by last 8 digits or lowercase name to enrich saved contacts
        val photoByPhoneKey = HashMap<String, String>()
        val photoByNameKey = HashMap<String, String>()
        for (dc in deviceContacts) {
            val photo = dc.photoUri ?: continue
            photoByNameKey[dc.name.lowercase()] = photo
            for (p in dc.phones) {
                val digits = p.rawNumber.filter { it.isDigit() }
                if (digits.length >= 6) {
                    photoByPhoneKey[digits.takeLast(8)] = photo
                }
            }
        }

        for (extra in extraContacts) {
            if (extra.name.isBlank() && extra.phones.isEmpty()) continue
            val primaryDigits = extra.primaryPhone?.rawNumber?.filter { it.isDigit() }.orEmpty()
            val phoneKey = if (primaryDigits.length >= 8) primaryDigits.takeLast(8) else primaryDigits
            val enrichedPhoto = extra.photoUri
                ?: (if (phoneKey.isNotBlank()) photoByPhoneKey[phoneKey] else null)
                ?: photoByNameKey[extra.name.lowercase()]

            val key = "${extra.name.lowercase()}|$phoneKey"
            if (seenNamesOrPhones.add(key)) {
                mergedList.add(extra.copy(photoUri = enrichedPhoto))
            }
        }

        for (dc in deviceContacts) {
            val primaryDigits = dc.primaryPhone?.rawNumber?.filter { it.isDigit() }.orEmpty()
            val phoneKey = if (primaryDigits.length >= 8) primaryDigits.takeLast(8) else primaryDigits
            val key = "${dc.name.lowercase()}|$phoneKey"
            if (seenNamesOrPhones.add(key)) {
                mergedList.add(dc)
            }
        }

        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return mergedList.take(limit)
        }

        val queryLower = trimmed.lowercase()
        val queryDigits = trimmed.filter { it.isDigit() }

        val scored = mutableListOf<Pair<Int, DeviceContactSuggestion>>()

        for (contact in mergedList) {
            val nameLower = contact.name.lowercase()
            val nameStarts = nameLower.startsWith(queryLower)
            val nameWordStarts = nameLower.split(" ").any { it.startsWith(queryLower) }
            val nameContains = nameLower.contains(queryLower)

            var phoneStarts = false
            var phoneContains = false

            for (phone in contact.phones) {
                val rawLower = phone.rawNumber.lowercase()
                val numDigits = phone.rawNumber.filter { it.isDigit() }
                if (rawLower.startsWith(queryLower) || (queryDigits.isNotEmpty() && numDigits.startsWith(queryDigits))) {
                    phoneStarts = true
                    break
                } else if (rawLower.contains(queryLower) || (queryDigits.isNotEmpty() && numDigits.contains(queryDigits))) {
                    phoneContains = true
                }
            }

            val score = when {
                nameLower == queryLower -> 100
                nameStarts -> 90
                phoneStarts -> 85
                nameWordStarts -> 80
                nameContains -> 70
                phoneContains -> 60
                else -> 0
            }

            if (score > 0) {
                // Reorder phones so the matching phone number is shown first if searched by number
                val orderedPhones = if (queryDigits.isNotEmpty() && contact.phones.size > 1) {
                    contact.phones.sortedByDescending { p ->
                        val d = p.rawNumber.filter { it.isDigit() }
                        when {
                            d.startsWith(queryDigits) -> 2
                            d.contains(queryDigits) -> 1
                            else -> 0
                        }
                    }
                } else {
                    contact.phones
                }
                scored.add(score to contact.copy(phones = orderedPhones))
            }
        }

        return scored
            .sortedWith(compareByDescending<Pair<Int, DeviceContactSuggestion>> { it.first }.thenBy { it.second.name.lowercase() })
            .map { it.second }
            .take(limit)
    }

    fun findContactPhotoUri(context: Context, name: String, phone: String): String? {
        if (!hasContactsPermission(context)) return null
        val contacts = loadAllDeviceContacts(context)
        val cleanPhoneDigits = phone.filter { it.isDigit() }
        val phoneKey = if (cleanPhoneDigits.length >= 8) cleanPhoneDigits.takeLast(8) else cleanPhoneDigits

        if (phoneKey.length >= 6) {
            contacts.firstOrNull { c ->
                c.phones.any { p ->
                    val d = p.rawNumber.filter { it.isDigit() }
                    val k = if (d.length >= 8) d.takeLast(8) else d
                    k == phoneKey
                }
            }?.photoUri?.let { return it }
        }

        if (name.isNotBlank()) {
            contacts.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }?.photoUri?.let { return it }
        }
        return null
    }

    fun extractContact(context: Context, contactUri: Uri): PickedContact? {
        val contentResolver = context.contentResolver
        var contactName = ""
        var contactId: String? = null
        var photoUri: String? = null

        // 1. Fetch Contact ID, Display Name, and Photo URI
        try {
            contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ContactsContract.Contacts.PHOTO_URI
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                    if (idIdx != -1) contactId = cursor.getString(idIdx)
                    if (nameIdx != -1) contactName = cursor.getString(nameIdx) ?: ""
                    val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx)?.takeIf { it.isNotBlank() } else null
                    val full = if (photoIdx != -1) cursor.getString(photoIdx)?.takeIf { it.isNotBlank() } else null
                    photoUri = thumb ?: full
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (contactId.isNullOrBlank()) {
            contactId = contactUri.lastPathSegment
        }

        val phones = mutableListOf<ContactPhoneOption>()
        val seenDigits = mutableSetOf<String>()

        fun addPhoneNumber(rawNumber: String?, type: Int, customLabel: String?) {
            if (rawNumber.isNullOrBlank()) return
            val clean = rawNumber.trim()
            val digitsOnly = clean.filter { it.isDigit() }
            if (digitsOnly.length < 3) return

            val comparisonKey = if (digitsOnly.length >= 8) digitsOnly.takeLast(8) else digitsOnly
            if (seenDigits.add(comparisonKey)) {
                val (labelEn, labelBn) = mapPhoneTypeLabel(type, customLabel)
                phones.add(
                    ContactPhoneOption(
                        rawNumber = clean,
                        displayNumber = clean,
                        typeLabelEn = labelEn,
                        typeLabelBn = labelBn
                    )
                )
            }
        }

        // Method A: Query Phone.CONTENT_URI with CONTACT_ID
        if (!contactId.isNullOrBlank()) {
            try {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { cursor ->
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                    while (cursor.moveToNext()) {
                        if (contactName.isBlank() && nameIdx != -1) {
                            contactName = cursor.getString(nameIdx) ?: ""
                        }
                        if (photoUri.isNullOrBlank() && thumbIdx != -1) {
                            photoUri = cursor.getString(thumbIdx)?.takeIf { it.isNotBlank() }
                        }
                        val num = if (numIdx != -1) cursor.getString(numIdx) else null
                        val type = if (typeIdx != -1) cursor.getInt(typeIdx) else -1
                        val lbl = if (labelIdx != -1) cursor.getString(labelIdx) else null
                        addPhoneNumber(num, type, lbl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Method B: Query via contactUri's Data sub-directory
        if (phones.isEmpty()) {
            try {
                val dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
                contentResolver.query(
                    dataUri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
                    ),
                    "${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                    null
                )?.use { cursor ->
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                    while (cursor.moveToNext()) {
                        if (contactName.isBlank() && nameIdx != -1) {
                            contactName = cursor.getString(nameIdx) ?: ""
                        }
                        if (photoUri.isNullOrBlank() && thumbIdx != -1) {
                            photoUri = cursor.getString(thumbIdx)?.takeIf { it.isNotBlank() }
                        }
                        val num = if (numIdx != -1) cursor.getString(numIdx) else null
                        val type = if (typeIdx != -1) cursor.getInt(typeIdx) else -1
                        val lbl = if (labelIdx != -1) cursor.getString(labelIdx) else null
                        addPhoneNumber(num, type, lbl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Method C: Direct contactUri query in case picked URI was already a Phone data row
        if (phones.isEmpty()) {
            try {
                contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                        if (contactName.isBlank() && nameIdx != -1) {
                            contactName = cursor.getString(nameIdx) ?: ""
                        }
                        if (photoUri.isNullOrBlank() && thumbIdx != -1) {
                            photoUri = cursor.getString(thumbIdx)?.takeIf { it.isNotBlank() }
                        }
                        val num = if (numIdx != -1) cursor.getString(numIdx) else null
                        val type = if (typeIdx != -1) cursor.getInt(typeIdx) else -1
                        addPhoneNumber(num, type, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return PickedContact(
            name = contactName.trim(),
            phones = phones,
            photoUri = photoUri
        )
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String, isBangla: Boolean = false) {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank()) {
            Toast.makeText(
                context,
                if (isBangla) "কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone number available",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(cleanNumber)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                if (isBangla) "কল করার কোনো অ্যাপ পাওয়া যায়নি" else "No phone dialer application found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initial = remember(name) {
        name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (name.isNotBlank() && initial != "?") {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = (size.value * 0.42f).sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.55f)
            )
        }

        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = name.ifBlank { "Contact Photo" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun ContactSearchDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    isBangla: Boolean,
    onContactSelected: (DeviceContactSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    isPhoneField: Boolean = false,
    selectedPhotoUri: String? = null,
    defaultLeadingIcon: ImageVector = if (isPhoneField) Icons.Default.Phone else Icons.Default.Person,
    onPickExternalContact: (() -> Unit)? = null,
    externalPickButtonTestTag: String? = null,
    fieldTestTag: String? = null,
    extraSuggestions: List<DeviceContactSuggestion> = emptyList(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    leadingIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var isFocused by remember { mutableStateOf(false) }
    var dropdownOpen by remember { mutableStateOf(false) }
    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(ContactUtils.hasContactsPermission(context))
    }

    var suggestions by remember { mutableStateOf<List<DeviceContactSuggestion>>(emptyList()) }

    val inlinePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            ContactUtils.invalidateCache()
            permissionRefreshTick++
            dropdownOpen = true
        }
    }

    LaunchedEffect(value, isFocused, hasPermission, permissionRefreshTick, extraSuggestions) {
        if (!isFocused && !dropdownOpen) return@LaunchedEffect
        hasPermission = ContactUtils.hasContactsPermission(context)
        val loaded = withContext(Dispatchers.IO) {
            ContactUtils.searchDeviceContacts(
                context = context,
                query = value,
                limit = 15,
                extraContacts = extraSuggestions
            )
        }
        suggestions = loaded
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val textFieldModifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    hasPermission = ContactUtils.hasContactsPermission(context)
                    dropdownOpen = true
                } else {
                    dropdownOpen = false
                }
            }
            .let { mod ->
                if (!fieldTestTag.isNullOrBlank()) mod.testTag(fieldTestTag) else mod
            }

        OutlinedTextField(
            value = value,
            onValueChange = { newText ->
                onValueChange(newText)
                dropdownOpen = true
            },
            label = label,
            placeholder = placeholder,
            leadingIcon = {
                if (!selectedPhotoUri.isNullOrBlank()) {
                    ContactAvatar(
                        name = value,
                        photoUri = selectedPhotoUri,
                        size = 28.dp
                    )
                } else {
                    Icon(
                        imageVector = defaultLeadingIcon,
                        contentDescription = null,
                        tint = leadingIconTint
                    )
                }
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onValueChange("")
                                dropdownOpen = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isBangla) "মুছুন" else "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (onPickExternalContact != null) {
                        val btnMod = if (!externalPickButtonTestTag.isNullOrBlank()) {
                            Modifier.testTag(externalPickButtonTestTag)
                        } else {
                            Modifier
                        }
                        IconButton(
                            onClick = onPickExternalContact,
                            modifier = btnMod
                        ) {
                            Icon(
                                imageVector = if (isPhoneField) Icons.Default.Contacts else Icons.Default.ContactPhone,
                                contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPhoneField) KeyboardType.Phone else KeyboardType.Text
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = colors,
            modifier = textFieldModifier
        )

        val showSuggestionsPanel = isFocused && dropdownOpen && (suggestions.isNotEmpty() || !hasPermission)

        AnimatedVisibility(
            visible = showSuggestionsPanel,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag(if (isPhoneField) "contact_suggestions_phone_dropdown" else "contact_suggestions_name_dropdown")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row with title + dismiss button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla)
                                "কন্টাক্ট সাজেশন (${suggestions.size})"
                            else
                                "Contact Suggestions (${suggestions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isBangla) "বন্ধ করুন" else "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { dropdownOpen = false }
                        )
                    }

                    if (!hasPermission) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inlinePermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isBangla)
                                        "ফোনের কন্টাক্ট সরাসরি সার্চ করতে অনুমতি দিন"
                                    else
                                        "Allow permission to search phone contacts directly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = {
                                    inlinePermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            ) {
                                Text(
                                    text = if (isBangla) "অনুমতি দিন" else "Allow",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (suggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            suggestions.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        thickness = 0.5.dp
                                    )
                                }

                                val primaryPhone = item.primaryPhone
                                val extraPhonesCount = (item.phones.size - 1).coerceAtLeast(0)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            dropdownOpen = false
                                            onContactSelected(item)
                                            focusManager.clearFocus()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("contact_suggestion_item_$index"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatar(
                                        name = item.name,
                                        photoUri = item.photoUri,
                                        size = 40.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (primaryPhone != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = primaryPhone.displayNumber,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (extraPhonesCount > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "+$extraPhonesCount টি" else "+$extraPhonesCount",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (primaryPhone != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        ) {
                                            Text(
                                                text = if (isBangla) primaryPhone.typeLabelBn else primaryPhone.typeLabelEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectContactPhoneDialog(
    contactName: String,
    options: List<ContactPhoneOption>,
    isBangla: Boolean,
    onSelectPhone: (ContactPhoneOption) -> Unit,
    onDismiss: () -> Unit,
    photoUri: String? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("select_phone_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!photoUri.isNullOrBlank() || contactName.isNotBlank()) {
                        ContactAvatar(
                            name = contactName,
                            photoUri = photoUri,
                            size = 44.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "মোবাইল নম্বর নির্বাচন করুন" else "Select Phone Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (contactName.isNotBlank()) {
                            Text(
                                text = contactName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isBangla)
                        "এই কন্টাক্টে একাধিক নম্বর রয়েছে। যে নম্বরটি এই হিসাবে যুক্ত করতে চান সেটি বেছে নিন:"
                    else
                        "This contact has multiple phone numbers. Select which number to use for this record:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Options List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        Surface(
                            onClick = { onSelectPhone(option) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_option_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.displayNumber,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val label = if (isBangla) option.typeLabelBn else option.typeLabelEn
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                }
            }
        }
    }
}
