package com.plusemon.hisab.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.screens.transactions.AccountSelectorChips
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.TransferBlue
import com.plusemon.hisab.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

/**
 * Modern, compact BottomSheet for adding transactions (Expense, Income, Transfer).
 * Styled consistently with the recurring transaction bottom sheet with a clean
 * single-scroll layout, compact type chips, exposed category dropdown, and an interactive,
 * changeable date & time picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    viewModel: HisabViewModel,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val currSymbol = settings.currencySymbol

    val accountsWithBalances by viewModel.accountsWithBalances.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val availableAccounts = remember(accountsWithBalances) { accountsWithBalances.map { it.account } }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var receiptUriStr by remember { mutableStateOf<String?>(null) }
    var dateTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedAccountId by remember {
        mutableStateOf(availableAccounts.firstOrNull()?.id ?: 0L)
    }
    var selectedToAccountId by remember {
        mutableStateOf(availableAccounts.getOrNull(1)?.id ?: availableAccounts.firstOrNull()?.id ?: 0L)
    }
    var selectedExpenseCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedIncomeCategoryId by remember { mutableStateOf<Long?>(null) }

    // Seed data check
    LaunchedEffect(Unit) {
        viewModel.ensureDefaultData()
    }

    // Reactively update selected account when accounts load
    LaunchedEffect(availableAccounts) {
        if (availableAccounts.isNotEmpty()) {
            if (selectedAccountId <= 0L || availableAccounts.none { it.id == selectedAccountId }) {
                selectedAccountId = availableAccounts.first().id
            }
            if (selectedToAccountId <= 0L || availableAccounts.none { it.id == selectedToAccountId }) {
                selectedToAccountId = availableAccounts.getOrNull(1)?.id ?: availableAccounts.first().id
            }
        }
    }

    // Default category selections
    LaunchedEffect(categories) {
        val expenseCats = categories.filter { it.type == TransactionType.EXPENSE }
        if (selectedExpenseCategoryId == null || expenseCats.none { it.id == selectedExpenseCategoryId }) {
            selectedExpenseCategoryId = expenseCats.firstOrNull()?.id
        }
        val incomeCats = categories.filter { it.type == TransactionType.INCOME }
        if (selectedIncomeCategoryId == null || incomeCats.none { it.id == selectedIncomeCategoryId }) {
            selectedIncomeCategoryId = incomeCats.firstOrNull()?.id
        }
    }

    // Photo picker launcher (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUriStr = uri.toString()
        }
    }

    val closeSheet = {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    val handleSave: (TransactionType) -> Unit = { activeType ->
        val amount = amountText.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            errorMessage = if (isBn) "সঠিক টাকার পরিমাণ লিখুন" else "Please enter a valid amount"
        } else {
            val effectiveAccountId = if (selectedAccountId > 0L) selectedAccountId else availableAccounts.firstOrNull()?.id ?: 0L
            if (effectiveAccountId <= 0L) {
                viewModel.ensureDefaultData()
                errorMessage = if (isBn) "অনুগ্রহ করে একটি অ্যাকাউন্ট নির্বাচন করুন বা তৈরি করুন" else "Please select or create an account"
            } else {
                val effectiveToAccountId = if (selectedToAccountId > 0L) selectedToAccountId else availableAccounts.getOrNull(1)?.id ?: effectiveAccountId

                if (activeType == TransactionType.TRANSFER && effectiveAccountId == effectiveToAccountId) {
                    errorMessage = if (isBn) "একই অ্যাকাউন্টে স্থানান্তর সম্ভব নয়" else "Source and target accounts must be different"
                } else {
                    val fee = feeText.toDoubleOrNull() ?: 0.0
                    val categoryId = when (activeType) {
                        TransactionType.EXPENSE -> selectedExpenseCategoryId
                        TransactionType.INCOME -> selectedIncomeCategoryId
                        TransactionType.TRANSFER -> null
                    }

                    viewModel.addTransaction(
                        accountId = effectiveAccountId,
                        categoryId = categoryId,
                        toAccountId = if (activeType == TransactionType.TRANSFER) effectiveToAccountId else null,
                        amount = amount,
                        fee = fee,
                        type = activeType,
                        dateTimestamp = dateTimestamp,
                        note = note.trim(),
                        receiptUri = receiptUriStr,
                        exchangeRate = 1.0
                    )
                    closeSheet()
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedUtcMillis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = dateTimestamp
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)

                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            utcCal.timeInMillis = selectedUtcMillis
                            val year = utcCal.get(Calendar.YEAR)
                            val month = utcCal.get(Calendar.MONTH)
                            val day = utcCal.get(Calendar.DAY_OF_MONTH)

                            val localCal = Calendar.getInstance()
                            localCal.set(year, month, day, hour, minute)
                            dateTimestamp = localCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(if (isBn) "ঠিক আছে" else "OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(Localization.getString(Localization.Key.CANCEL, isBn))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.testTag("add_transaction_drag_handle")
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .imePadding()
            .testTag("add_transaction_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString(Localization.Key.ADD_TRANSACTION, isBn),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { closeSheet() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = Localization.getString(Localization.Key.CANCEL, isBn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compact Type Selector (Expense / Income / Transfer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (selectedType == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = Localization.getString(Localization.Key.EXPENSE_SHORT, isBn),
                                fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialog_tab_expense"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        selectedLabelColor = ExpenseRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (selectedType == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = Localization.getString(Localization.Key.INCOME_SHORT, isBn),
                                fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialog_tab_income"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        selectedLabelColor = IncomeGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = {
                        selectedType = TransactionType.TRANSFER
                        errorMessage = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = if (selectedType == TransactionType.TRANSFER) TransferBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = Localization.getString(Localization.Key.TRANSFER_SHORT, isBn),
                                fontWeight = if (selectedType == TransactionType.TRANSFER) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialog_tab_transfer"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TransferBlue.copy(alpha = 0.15f),
                        selectedLabelColor = TransferBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Compact Amount Input with Currency Prefix
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                        amountText = it
                        errorMessage = null
                    }
                },
                label = { Text(Localization.getString(Localization.Key.AMOUNT, isBn)) },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = "$currSymbol ",
                        fontWeight = FontWeight.Bold,
                        color = when (selectedType) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_amount_input")
            )

            // Quick Add Amount Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(50, 100, 500, 1000, 5000).forEach { quickVal ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                val nextVal = currentVal + quickVal
                                amountText = if (nextVal % 1.0 == 0.0) nextVal.toLong().toString() else nextVal.toString()
                                errorMessage = null
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$quickVal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Account Selection
            if (selectedType == TransactionType.TRANSFER) {
                Text(
                    text = Localization.getString(Localization.Key.FROM_ACCOUNT, isBn),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                AccountSelectorChips(
                    accounts = availableAccounts,
                    selectedId = selectedAccountId,
                    onSelect = { selectedAccountId = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Localization.getString(Localization.Key.TO_ACCOUNT, isBn),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                AccountSelectorChips(
                    accounts = availableAccounts,
                    selectedId = selectedToAccountId,
                    onSelect = { selectedToAccountId = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text(Localization.getString(Localization.Key.FEE, isBn)) },
                    placeholder = { Text("0.00") },
                    prefix = { Text("$currSymbol ", fontWeight = FontWeight.SemiBold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = Localization.getString(Localization.Key.ACCOUNT, isBn),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (availableAccounts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.ensureDefaultData() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "ডিফল্ট অ্যাকাউন্ট তৈরি করতে ট্যাপ করুন" else "Tap to seed default accounts",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    AccountSelectorChips(
                        accounts = availableAccounts,
                        selectedId = selectedAccountId,
                        onSelect = { selectedAccountId = it }
                    )
                }
            }

            // Compact Category Selection via ExposedDropdownMenuBox
            if (selectedType != TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = Localization.getString(Localization.Key.CATEGORY, isBn),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val typeCategories = remember(categories, selectedType) {
                    categories.filter { it.type == selectedType }
                }
                val selectedCatId = if (selectedType == TransactionType.EXPENSE) selectedExpenseCategoryId else selectedIncomeCategoryId
                val currentCategory = categories.firstOrNull { it.id == selectedCatId }

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = currentCategory?.localizedName(isBn) ?: (if (isBn) "ক্যাটাগরি নির্বাচন করুন" else "Select Category"),
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            if (currentCategory != null) {
                                CategoryIconBadge(
                                    iconName = currentCategory.iconName,
                                    colorHex = currentCategory.colorHex,
                                    size = 28.dp,
                                    iconSize = 16.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("dialog_category_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        typeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.localizedName(isBn), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    CategoryIconBadge(
                                        iconName = cat.iconName,
                                        colorHex = cat.colorHex,
                                        size = 28.dp,
                                        iconSize = 16.dp
                                    )
                                },
                                onClick = {
                                    if (selectedType == TransactionType.EXPENSE) {
                                        selectedExpenseCategoryId = cat.id
                                    } else {
                                        selectedIncomeCategoryId = cat.id
                                    }
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date & Time Picker Section (CHANGEABLE)
            Text(
                text = Localization.getString(Localization.Key.DATE, isBn),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true }
                    .testTag("dialog_date_button"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Formatters.formatDateTime(dateTimestamp, isBn),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (isBn) "পরিবর্তন করুন" else "Change",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick date shortcut chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dateTimestamp = System.currentTimeMillis() }
                ) {
                    Text(
                        text = if (isBn) "আজকে" else "Today",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dateTimestamp = System.currentTimeMillis() - 86400000L }
                ) {
                    Text(
                        text = if (isBn) "গতকাল" else "Yesterday",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBn) "তারিখ বাছুন" else "Select Date",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Localization.getString(Localization.Key.NOTE, isBn)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_note_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Receipt Photo Attachment (Compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (receiptUriStr != null) {
                            if (isBn) "রসিদের ছবি পরিবর্তন" else "Change Receipt"
                        } else {
                            Localization.getString(Localization.Key.RECEIPT, isBn)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (receiptUriStr != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = receiptUriStr,
                            contentDescription = "Receipt Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { receiptUriStr = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove receipt",
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Balance Preview Card (compact, shown when amount > 0)
            val selectedAccountWithBal = accountsWithBalances.find { it.account.id == selectedAccountId }
            val currentBalance = selectedAccountWithBal?.balance ?: 0.0
            val amountVal = amountText.toDoubleOrNull() ?: 0.0
            val feeVal = feeText.toDoubleOrNull() ?: 0.0

            if (amountVal > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                val resultingBalance = when (selectedType) {
                    TransactionType.EXPENSE -> currentBalance - amountVal - feeVal
                    TransactionType.INCOME -> currentBalance + amountVal
                    TransactionType.TRANSFER -> currentBalance - amountVal - feeVal
                }

                val targetAccountWithBal = accountsWithBalances.find { it.account.id == selectedToAccountId }
                val targetCurrentBalance = targetAccountWithBal?.balance
                val resultingTargetBalance = if (targetCurrentBalance != null) {
                    targetCurrentBalance + amountVal
                } else null

                TransactionBalancePreviewCard(
                    transactionType = selectedType,
                    currentBalance = currentBalance,
                    resultingBalance = resultingBalance,
                    targetCurrentBalance = targetCurrentBalance,
                    resultingTargetBalance = resultingTargetBalance,
                    currencySymbol = currSymbol,
                    isBn = isBn,
                    accountName = selectedAccountWithBal?.account?.name,
                    targetAccountName = targetAccountWithBal?.account?.name,
                    amount = amountVal
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Cancel and Save (compact row, consistent with recurring sheet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { closeSheet() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.getString(Localization.Key.CANCEL, isBn))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = { handleSave(selectedType) },
                    modifier = Modifier
                        .testTag("dialog_save_tx_btn")
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedType) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                        },
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString(Localization.Key.SAVE, isBn),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
