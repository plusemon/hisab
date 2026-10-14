package com.plusemon.hisab.ui.screens.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.Frequency
import com.plusemon.hisab.data.model.RecurringRule
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.DetailTopAppBar
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.components.getIconByName
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.screens.transactions.AccountSelectorChips
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: HisabViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = settings.numeralSystem == "bn"
    val hideBalances = settings.hideBalances
    val currSymbol = settings.currencySymbol
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()

    val recurringRules by viewModel.recurringRules.collectAsState()
    val accountsWithBalances by viewModel.accountsWithBalances.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val availableAccounts = remember(accountsWithBalances) { accountsWithBalances.map { it.account } }

    var showAddEditSheet by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<RecurringRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<RecurringRule?>(null) }

    if (ruleToDelete != null) {
        val target = ruleToDelete!!
        val cat = categories.firstOrNull { it.id == target.categoryId }
        val acc = accountsWithBalances.firstOrNull { it.account.id == target.accountId }?.account
        val amountStr = Formatters.formatAmount(target.amount, currSymbol, useBnDigits)
        val catOrAcc = cat?.localizedName(isBn) ?: acc?.name ?: (if (isBn) "সাধারণ" else "General")
        val freqLabel = if (isBn) target.frequency.labelBn else target.frequency.labelEn
        val detail = "$catOrAcc • $freqLabel • $amountStr"

        DeleteConfirmationDialog(
            title = if (isBn) "স্বয়ংক্রিয় নিয়ম মুছে ফেলবেন?" else "Delete Recurring Rule?",
            message = if (isBn)
                "আপনি কি নিশ্চিত যে এই স্বয়ংক্রিয় লেনদেনের নিয়মটি মুছে ফেলতে চান?"
            else
                "Are you sure you want to delete this recurring transaction rule?",
            itemDetail = detail,
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteRecurringRule(target.id)
                ruleToDelete = null
            },
            onDismiss = {
                ruleToDelete = null
            }
        )
    }

    if (showAddEditSheet) {
        AddEditRecurringRuleBottomSheet(
            ruleToEdit = ruleToEdit,
            accounts = availableAccounts,
            categories = categories,
            currencySymbol = currSymbol,
            isBangla = isBn,
            onDismiss = {
                showAddEditSheet = false
                ruleToEdit = null
            },
            onSave = { ruleId, accountId, categoryId, amount, type, freq, note ->
                if (ruleId == null || ruleId == 0L) {
                    viewModel.addRecurringRule(
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        type = type,
                        frequency = freq,
                        startDate = System.currentTimeMillis(),
                        endDate = null,
                        note = note
                    )
                } else {
                    viewModel.updateRecurringRule(
                        id = ruleId,
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        type = type,
                        frequency = freq,
                        note = note
                    )
                }
                showAddEditSheet = false
                ruleToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            DetailTopAppBar(
                title = Localization.getString(Localization.Key.RECURRING_TRANSACTIONS, isBn),
                onNavigateBack = onNavigateBack,
                isNetworkConnected = isNetworkConnected,
                actions = {
                    IconButton(
                        onClick = {
                            ruleToEdit = null
                            showAddEditSheet = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("topbar_add_recurring_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isBn) "নতুন পুনরাবৃত্তি যোগ করুন" else "Add Recurring",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (recurringRules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.Repeat,
                        title = if (isBn) "কোনো পুনরাবৃত্ত লেনদেনের নিয়ম নেই" else "No Recurring Rules",
                        description = if (isBn)
                            "নিয়মিত খরচ বা আয়ের হিসাব স্বয়ংক্রিয়ভাবে যুক্ত করতে উপরের + বাটনে চাপ দিন।"
                        else
                            "Automate repeated income or expense entries by tapping the + button in the header.",
                        actionLabel = if (isBn) "+ স্বয়ংক্রিয় নিয়ম যোগ করুন" else "+ Add Recurring Rule",
                        onActionClick = {
                            ruleToEdit = null
                            showAddEditSheet = true
                        }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recurringRules, key = { it.id }) { rule ->
                        val acc = accountsWithBalances.firstOrNull { it.account.id == rule.accountId }?.account
                        val cat = categories.firstOrNull { it.id == rule.categoryId }
                        val isExpense = rule.type == TransactionType.EXPENSE

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    ruleToEdit = rule
                                    showAddEditSheet = true
                                }
                                .testTag("recurring_card_${rule.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cat != null) {
                                    CategoryIconBadge(
                                        iconName = cat.iconName,
                                        colorHex = cat.colorHex,
                                        size = 42.dp,
                                        iconSize = 22.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = if (isExpense) ExpenseRed else IncomeGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat?.localizedName(isBn) ?: (if (isBn) "সাধারণ" else "General"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${acc?.name ?: (if (isBn) "অ্যাকাউন্ট নেই" else "No Account")} • ${if (isBn) rule.frequency.labelBn else rule.frequency.labelEn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (rule.note.isNotBlank()) {
                                        Text(
                                            text = rule.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    CurrencyAmountText(
                                        amount = rule.amount,
                                        currencySymbol = currSymbol,
                                        useBanglaDigits = useBnDigits,
                                        hideBalances = hideBalances,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpense) ExpenseRed else IncomeGreen
                                    )

                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                ruleToEdit = rule
                                                showAddEditSheet = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("edit_recurring_${rule.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = if (isBn) "সম্পাদনা" else "Edit",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.togglePauseRecurringRule(rule.id, rule.isPaused) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("pause_recurring_${rule.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (rule.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = if (rule.isPaused) "Resume" else "Pause",
                                                tint = if (rule.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { ruleToDelete = rule },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("delete_recurring_${rule.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = ExpenseRed.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringRuleBottomSheet(
    ruleToEdit: RecurringRule?,
    accounts: List<UserAccount>,
    categories: List<Category>,
    currencySymbol: String,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (id: Long?, accountId: Long, categoryId: Long?, amount: Double, type: TransactionType, freq: Frequency, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing = ruleToEdit != null
    var selectedType by remember(ruleToEdit) {
        mutableStateOf(ruleToEdit?.type ?: TransactionType.EXPENSE)
    }

    var selectedAccountId by remember(ruleToEdit, accounts) {
        mutableStateOf(
            ruleToEdit?.accountId
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }

    val typeCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    var selectedCategoryId by remember(ruleToEdit, selectedType) {
        mutableStateOf<Long?>(
            ruleToEdit?.categoryId ?: typeCategories.firstOrNull()?.id
        )
    }

    var selectedFreq by remember(ruleToEdit) {
        mutableStateOf(ruleToEdit?.frequency ?: Frequency.MONTHLY)
    }

    var amountText by remember(ruleToEdit) {
        mutableStateOf(
            if (ruleToEdit != null && ruleToEdit.amount > 0) {
                if (ruleToEdit.amount % 1.0 == 0.0) ruleToEdit.amount.toLong().toString() else ruleToEdit.amount.toString()
            } else ""
        )
    }

    var note by remember(ruleToEdit) {
        mutableStateOf(ruleToEdit?.note ?: "")
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) {
                        if (isBangla) "স্বয়ংক্রিয় নিয়ম সম্পাদনা" else "Edit Recurring Rule"
                    } else {
                        if (isBangla) "নতুন স্বয়ংক্রিয় নিয়ম" else "Create Recurring Rule"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Type Toggle (Expense / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        selectedCategoryId = categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
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
                                text = if (isBangla) "খরচ" else "Expense",
                                fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
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
                        selectedCategoryId = categories.firstOrNull { it.type == TransactionType.INCOME }?.id
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
                                text = if (isBangla) "আয়" else "Income",
                                fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        selectedLabelColor = IncomeGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount TextField
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(Localization.getString(Localization.Key.AMOUNT, isBangla)) },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_amount_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Account Selection
            Text(
                text = if (isBangla) "অ্যাকাউন্ট নির্বাচন করুন" else "Select Account",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (accounts.isEmpty()) {
                Text(
                    text = if (isBangla) "কোনো সক্রিয় অ্যাকাউন্ট নেই" else "No active accounts available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                AccountSelectorChips(
                    accounts = accounts,
                    selectedId = selectedAccountId,
                    onSelect = { selectedAccountId = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selection (Optional, removable if not necessary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString(Localization.Key.CATEGORY, isBangla),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedCategoryId != null) {
                    TextButton(
                        onClick = { selectedCategoryId = null },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isBangla) "ক্যাটাগরি বাদ দিন" else "Remove Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            val currentCategory = categories.firstOrNull { it.id == selectedCategoryId }
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = currentCategory?.localizedName(isBangla) ?: (if (isBangla) "সাধারণ / কোনো ক্যাটাগরি নেই" else "General / No Category"),
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        if (currentCategory != null) {
                            CategoryIconBadge(
                                iconName = currentCategory.iconName,
                                colorHex = currentCategory.colorHex,
                                size = 30.dp,
                                iconSize = 16.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Repeat,
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
                        .testTag("recurring_category_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isBangla) "সাধারণ / কোনো ক্যাটাগরি নেই" else "General / No Category",
                                fontWeight = FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            selectedCategoryId = null
                            categoryDropdownExpanded = false
                        }
                    )

                    typeCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.localizedName(isBangla), fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                CategoryIconBadge(
                                    iconName = cat.iconName,
                                    colorHex = cat.colorHex,
                                    size = 28.dp,
                                    iconSize = 16.dp
                                )
                            },
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Frequency Selector
            Text(
                text = Localization.getString(Localization.Key.FREQUENCY, isBangla),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Frequency.values().forEach { freq ->
                    val isSelected = selectedFreq == freq
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFreq = freq },
                        label = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBangla) freq.labelBn else freq.labelEn,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note TextField
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Localization.getString(Localization.Key.NOTE, isBangla)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_note_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                }

                Spacer(modifier = Modifier.width(10.dp))

                val isValid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0 && selectedAccountId > 0L

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedAccountId > 0) {
                            onSave(
                                ruleToEdit?.id,
                                selectedAccountId,
                                selectedCategoryId,
                                amt,
                                selectedType,
                                selectedFreq,
                                note.trim()
                            )
                        }
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("save_recurring_btn")
                ) {
                    Text(
                        text = if (isEditing) {
                            if (isBangla) "আপডেট করুন" else "Update Rule"
                        } else {
                            Localization.getString(Localization.Key.SAVE, isBangla)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
