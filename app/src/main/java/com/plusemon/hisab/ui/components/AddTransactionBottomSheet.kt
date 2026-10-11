package com.plusemon.hisab.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.screens.transactions.AccountSelectorChips
import com.plusemon.hisab.ui.screens.transactions.CategoryGridSelector
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.TransferBlue
import com.plusemon.hisab.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.launch

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

    val initialPageIndex = when (initialType) {
        TransactionType.EXPENSE -> 0
        TransactionType.INCOME -> 1
        TransactionType.TRANSFER -> 2
    }

    val pagerState = rememberPagerState(initialPage = initialPageIndex) { 3 }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var receiptUriStr by remember { mutableStateOf<String?>(null) }
    var dateTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
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

    // Photo picker launcher (Zero-permission Photo Picker)
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
                        note = note,
                        receiptUri = receiptUriStr,
                        exchangeRate = 1.0
                    )
                    closeSheet()
                }
            }
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
        modifier = Modifier.testTag("add_transaction_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBn) "নতুন লেনদেন" else "New Transaction",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBn) "ট্যাব পরিবর্তন করতে ডানে/বামে সোয়াইপ করুন" else "Swipe left / right to change tabs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { closeSheet() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isBn) "বন্ধ করুন" else "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Swipeable Tabs (Expense, Income, Transfer)
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp)),
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        val indicatorColor = when (pagerState.currentPage) {
                            0 -> ExpenseRed
                            1 -> IncomeGreen
                            else -> TransferBlue
                        }
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = indicatorColor
                        )
                    }
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (pagerState.currentPage == 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = Localization.getString(Localization.Key.EXPENSE_SHORT, isBn),
                            fontWeight = FontWeight.Bold,
                            color = if (pagerState.currentPage == 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("dialog_tab_expense")
                )

                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (pagerState.currentPage == 1) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = Localization.getString(Localization.Key.INCOME_SHORT, isBn),
                            fontWeight = FontWeight.Bold,
                            color = if (pagerState.currentPage == 1) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("dialog_tab_income")
                )

                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = if (pagerState.currentPage == 2) TransferBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = Localization.getString(Localization.Key.TRANSFER_SHORT, isBn),
                            fontWeight = FontWeight.Bold,
                            color = if (pagerState.currentPage == 2) TransferBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("dialog_tab_transfer")
                )
            }

            // Horizontal Pager with Left / Right swipe transitions
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1
            ) { page ->
                val pageType = when (page) {
                    0 -> TransactionType.EXPENSE
                    1 -> TransactionType.INCOME
                    else -> TransactionType.TRANSFER
                }

                val pageCategories = remember(categories, pageType) {
                    categories.filter { it.type == pageType }
                }

                val currentSelectedCategoryId = when (pageType) {
                    TransactionType.EXPENSE -> selectedExpenseCategoryId
                    TransactionType.INCOME -> selectedIncomeCategoryId
                    TransactionType.TRANSFER -> null
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Big Amount Input Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.getString(Localization.Key.AMOUNT, isBn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currSymbol,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (pageType) {
                                        TransactionType.EXPENSE -> ExpenseRed
                                        TransactionType.INCOME -> IncomeGreen
                                        TransactionType.TRANSFER -> TransferBlue
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = {
                                        if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            amountText = it
                                            errorMessage = null
                                        }
                                    },
                                    placeholder = { Text("0.00", fontSize = 32.sp, fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when (pageType) {
                                            TransactionType.EXPENSE -> ExpenseRed
                                            TransactionType.INCOME -> IncomeGreen
                                            TransactionType.TRANSFER -> TransferBlue
                                        }
                                    ),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .testTag("dialog_amount_input_${pageType.name.lowercase()}")
                                )
                            }

                            // Quick amount chips (+50, +100, +500, +1000, +5000)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(50, 100, 500, 1000, 5000).forEach { quickVal ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                                val nextVal = currentVal + quickVal
                                                amountText = if (nextVal % 1.0 == 0.0) nextVal.toInt().toString() else nextVal.toString()
                                                errorMessage = null
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "+$quickVal",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (pageType == TransactionType.TRANSFER) {
                        // TRANSFER ACCOUNTS
                        Text(
                            text = Localization.getString(Localization.Key.FROM_ACCOUNT, isBn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        AccountSelectorChips(
                            accounts = availableAccounts,
                            selectedId = selectedAccountId,
                            onSelect = { selectedAccountId = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = Localization.getString(Localization.Key.TO_ACCOUNT, isBn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        AccountSelectorChips(
                            accounts = availableAccounts,
                            selectedId = selectedToAccountId,
                            onSelect = { selectedToAccountId = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it },
                            label = { Text(Localization.getString(Localization.Key.FEE, isBn)) },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // EXPENSE / INCOME ACCOUNT
                        Text(
                            text = Localization.getString(Localization.Key.ACCOUNT, isBn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (availableAccounts.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { viewModel.ensureDefaultData() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBn) "ডিফল্ট অ্যাকাউন্ট তৈরি করতে ট্যাপ করুন" else "Tap to seed default accounts",
                                        style = MaterialTheme.typography.bodyMedium,
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Grid Selector
                        Text(
                            text = Localization.getString(Localization.Key.CATEGORY, isBn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (pageCategories.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { viewModel.ensureDefaultData() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBn) "ক্যাটাগরি লোড করতে ট্যাপ করুন" else "Tap to reload categories",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            CategoryGridSelector(
                                categories = pageCategories,
                                selectedCategoryId = currentSelectedCategoryId,
                                isBangla = isBn,
                                onSelect = { catId ->
                                    if (pageType == TransactionType.EXPENSE) {
                                        selectedExpenseCategoryId = catId
                                    } else {
                                        selectedIncomeCategoryId = catId
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date & Time Display
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Formatters.formatDateTime(dateTimestamp, isBn),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Note field
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(Localization.getString(Localization.Key.NOTE, isBn)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_note_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Receipt photo attachment
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
                            }
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.getString(Localization.Key.RECEIPT, isBn), fontWeight = FontWeight.Bold)
                        }

                        if (receiptUriStr != null) {
                            IconButton(onClick = { receiptUriStr = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove receipt", tint = ExpenseRed)
                            }
                        }
                    }

                    if (receiptUriStr != null) {
                        AsyncImage(
                            model = receiptUriStr,
                            contentDescription = "Receipt Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val selectedAccountWithBal = accountsWithBalances.find { it.account.id == selectedAccountId }
                    val currentBalance = selectedAccountWithBal?.balance ?: 0.0
                    val amountVal = amountText.toDoubleOrNull() ?: 0.0
                    val feeVal = feeText.toDoubleOrNull() ?: 0.0

                    val resultingBalance = when (pageType) {
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
                        transactionType = pageType,
                        currentBalance = currentBalance,
                        resultingBalance = resultingBalance,
                        targetCurrentBalance = targetCurrentBalance,
                        resultingTargetBalance = resultingTargetBalance,
                        currencySymbol = currSymbol,
                        isBn = isBn
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = { handleSave(pageType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("dialog_save_tx_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (pageType) {
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.TRANSFER -> TransferBlue
                            },
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.getString(Localization.Key.SAVE, isBn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
