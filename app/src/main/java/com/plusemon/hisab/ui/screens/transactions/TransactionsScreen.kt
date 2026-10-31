package com.plusemon.hisab.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.TransactionWithDetails
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.components.TransactionFilterBottomSheet
import com.plusemon.hisab.ui.components.getIconByName
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.screens.dashboard.TransactionRowItem
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.TransferBlue
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: HisabViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = settings.numeralSystem == "bn"
    val hideBalances = settings.hideBalances
    val currSymbol = settings.currencySymbol

    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accountsWithBalances.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedPeriodFilter by remember { mutableStateOf("THIS_MONTH") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<TransactionWithDetails?>(null) }

    val hasActiveFilters = selectedTypeFilter != null || selectedAccountId != null || selectedPeriodFilter != "THIS_MONTH"
    val activeFilterCount = (if (selectedTypeFilter != null) 1 else 0) +
            (if (selectedAccountId != null) 1 else 0) +
            (if (selectedPeriodFilter != "THIS_MONTH") 1 else 0)

    if (txToDelete != null) {
        val target = txToDelete!!
        val amountStr = Formatters.formatAmount(target.transaction.amount, currSymbol, useBnDigits)
        val catOrAcc = target.category?.localizedName(isBn) ?: target.account.name
        val detail = "$amountStr • $catOrAcc${if (target.transaction.note.isNotBlank()) " • " + target.transaction.note else ""}"
        DeleteConfirmationDialog(
            title = if (isBn) "লেনদেন মুছে ফেলবেন?" else "Delete Transaction?",
            message = if (isBn) "আপনি কি নিশ্চিত যে এই লেনদেনটি মুছে ফেলতে চান? লেনদেনটি মুছে ফেললে মোট ব্যালেন্স সেই অনুযায়ী সমন্বয় হবে।" else "Are you sure you want to delete this transaction? Your total balance will be updated accordingly.",
            itemDetail = detail,
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteTransaction(target.transaction)
                txToDelete = null
            },
            onDismiss = {
                txToDelete = null
            }
        )
    }

    // Filter logic including Account Filter
    val filteredTransactions = remember(allTransactions, searchQuery, selectedTypeFilter, selectedPeriodFilter, selectedAccountId) {
        val (start, end) = Formatters.getStartAndEndForPeriod(selectedPeriodFilter)
        allTransactions.filter { item ->
            val matchesPeriod = if (selectedPeriodFilter == "ALL") true else item.transaction.dateTimestamp in start..end
            val matchesType = selectedTypeFilter == null || item.transaction.type == selectedTypeFilter
            val matchesAccount = selectedAccountId == null ||
                    item.transaction.accountId == selectedAccountId ||
                    (item.transaction.type == TransactionType.TRANSFER && item.transaction.toAccountId == selectedAccountId)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase()
                item.transaction.note.lowercase().contains(q) ||
                        (item.category?.nameBn?.lowercase()?.contains(q) == true) ||
                        (item.category?.nameEn?.lowercase()?.contains(q) == true) ||
                        item.account.name.lowercase().contains(q) ||
                        (item.toAccount?.name?.lowercase()?.contains(q) == true) ||
                        item.transaction.amount.toString().contains(q)
            }
            matchesPeriod && matchesType && matchesAccount && matchesSearch
        }
    }

    // Group by Date for Daily Subtotals
    val groupedByDate = remember(filteredTransactions) {
        filteredTransactions.groupBy {
            Formatters.formatDate(it.transaction.dateTimestamp, isBn)
        }
    }

    if (showFilterSheet) {
        TransactionFilterBottomSheet(
            initialPeriod = selectedPeriodFilter,
            initialType = selectedTypeFilter,
            initialAccountId = selectedAccountId,
            accounts = accounts,
            isBangla = isBn,
            useBanglaDigits = useBnDigits,
            calculateMatchCount = { p, t, a ->
                val (start, end) = Formatters.getStartAndEndForPeriod(p)
                allTransactions.count { item ->
                    val matchesPeriod = if (p == "ALL") true else item.transaction.dateTimestamp in start..end
                    val matchesType = t == null || item.transaction.type == t
                    val matchesAccount = a == null ||
                            item.transaction.accountId == a ||
                            (item.transaction.type == TransactionType.TRANSFER && item.transaction.toAccountId == a)
                    val matchesSearch = if (searchQuery.isBlank()) true else {
                        val q = searchQuery.lowercase()
                        item.transaction.note.lowercase().contains(q) ||
                                (item.category?.nameBn?.lowercase()?.contains(q) == true) ||
                                (item.category?.nameEn?.lowercase()?.contains(q) == true) ||
                                item.account.name.lowercase().contains(q) ||
                                (item.toAccount?.name?.lowercase()?.contains(q) == true) ||
                                item.transaction.amount.toString().contains(q)
                    }
                    matchesPeriod && matchesType && matchesAccount && matchesSearch
                }
            },
            onApply = { p, t, a ->
                selectedPeriodFilter = p
                selectedTypeFilter = t
                selectedAccountId = a
                showFilterSheet = false
            },
            onReset = {
                selectedPeriodFilter = "THIS_MONTH"
                selectedTypeFilter = null
                selectedAccountId = null
            },
            onDismiss = {
                showFilterSheet = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search & Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = Localization.getString(Localization.Key.SEARCH_PLACEHOLDER, isBn),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_search_input")
                )

                // Filter Trigger Button with active Badge
                Surface(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasActiveFilters) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (hasActiveFilters) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("open_filter_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = if (useBnDigits) Formatters.toBanglaDigits(activeFilterCount.toString()) else activeFilterCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = if (isBn) "ফিল্টার" else "Filter",
                                tint = if (hasActiveFilters) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Compact Active Filter Pills (Shown only when non-default filters are active)
            AnimatedVisibility(
                visible = hasActiveFilters,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("active_filters_row")
                ) {
                    // Period Chip (if changed from default THIS_MONTH)
                    if (selectedPeriodFilter != "THIS_MONTH") {
                        val periodLabel = when (selectedPeriodFilter) {
                            "LAST_MONTH" -> if (isBn) "গত মাস" else "Last Month"
                            "THIS_YEAR" -> if (isBn) "এই বছর" else "This Year"
                            "ALL" -> if (isBn) "সব সময়" else "All Time"
                            else -> selectedPeriodFilter
                        }
                        item(key = "active_period") {
                            FilterChip(
                                selected = true,
                                onClick = { showFilterSheet = true },
                                label = { Text(periodLabel, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedPeriodFilter = "THIS_MONTH" }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("active_filter_period")
                            )
                        }
                    }

                    // Type Chip (if selected)
                    val activeTypeFilter = selectedTypeFilter
                    if (activeTypeFilter != null) {
                        val typeLabel = when (activeTypeFilter) {
                            TransactionType.EXPENSE -> Localization.getString(Localization.Key.EXPENSE_SHORT, isBn)
                            TransactionType.INCOME -> Localization.getString(Localization.Key.INCOME_SHORT, isBn)
                            TransactionType.TRANSFER -> Localization.getString(Localization.Key.TRANSFER_SHORT, isBn)
                        }
                        item(key = "active_type") {
                            FilterChip(
                                selected = true,
                                onClick = { showFilterSheet = true },
                                label = { Text(typeLabel, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedTypeFilter = null }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("active_filter_type")
                            )
                        }
                    }

                    // Account Chip (if selected)
                    if (selectedAccountId != null) {
                        val accountName = accounts.find { it.account.id == selectedAccountId }?.account?.name ?: (if (isBn) "অ্যাকাউন্ট" else "Account")
                        item(key = "active_account") {
                            FilterChip(
                                selected = true,
                                onClick = { showFilterSheet = true },
                                label = { Text(accountName, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedAccountId = null }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("active_filter_account")
                            )
                        }
                    }

                    // Clear All Button
                    item(key = "clear_all_button") {
                        TextButton(
                            onClick = {
                                selectedPeriodFilter = "THIS_MONTH"
                                selectedTypeFilter = null
                                selectedAccountId = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("clear_all_filters_btn")
                        ) {
                            Text(
                                text = Localization.getString(Localization.Key.CLEAR_ALL, isBn),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Transaction List or Empty State
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isFiltering = selectedAccountId != null || selectedTypeFilter != null || searchQuery.isNotBlank() || selectedPeriodFilter != "THIS_MONTH"
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = Localization.getString(Localization.Key.EMPTY_TRANSACTIONS, isBn),
                        description = if (selectedAccountId != null) {
                            if (isBn) "এই অ্যাকাউন্টের জন্য কোনো লেনদেন পাওয়া যায়নি।" else "No transactions found for this account."
                        } else {
                            if (isBn) "পছন্দের ফিল্টারে বা সময়ে কোনো লেনদেন পাওয়া যায়নি।" else "No transactions match your current search or filter criteria."
                        },
                        actionLabel = if (isFiltering) {
                            if (isBn) "ফিল্টার রিসেট করুন" else "Reset Filters"
                        } else {
                            if (isBn) "+ নতুন হিসাব যোগ করুন" else "+ Add Transaction"
                        },
                        onActionClick = {
                            if (isFiltering) {
                                selectedAccountId = null
                                selectedTypeFilter = null
                                selectedPeriodFilter = "THIS_MONTH"
                                searchQuery = ""
                            } else {
                                onNavigateToAddTransaction()
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedByDate.forEach { (dateStr, itemsForDate) ->
                        val dayExpense = itemsForDate.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                        val dayIncome = itemsForDate.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }

                        // Date Header with Subtotals
                        item(key = "header_$dateStr") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (dayExpense > 0) {
                                        Text(
                                            text = "-${Formatters.formatAmount(dayExpense, currSymbol, useBnDigits, hideBalances)}",
                                             style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                    if (dayIncome > 0) {
                                        Text(
                                            text = "+${Formatters.formatAmount(dayIncome, currSymbol, useBnDigits, hideBalances)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    }
                                }
                            }
                        }

                        // List of Transactions for that Date
                        // Direct TransactionRowItem with onLongClick for safe deletion
                        // Allowing frictionless horizontal page swiping (Feature 2)
                        items(itemsForDate, key = { it.transaction.id }) { txItem ->
                            TransactionRowItem(
                                item = txItem,
                                isBangla = isBn,
                                useBnDigits = useBnDigits,
                                hideBalances = hideBalances,
                                currencySymbol = currSymbol,
                                onClick = { onTransactionClick(txItem) },
                                onLongClick = { txToDelete = txItem }
                            )
                        }
                    }
                }
            }
        }
    }
}
