package com.plusemon.hisab.ui.screens.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.data.model.AccountType
import com.plusemon.hisab.data.model.TransactionWithDetails
import com.plusemon.hisab.data.model.UserAccount
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.DetailTopAppBar
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.components.getIconByName
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.screens.dashboard.TransactionRowItem
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.viewmodel.AccountWithBalance
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: HisabViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = settings.numeralSystem == "bn"
    val hideBalances = settings.hideBalances
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()
    val currSymbol = settings.currencySymbol

    val accountsWithBalances by viewModel.accountsWithBalances.collectAsState()
    val archivedAccountsWithBalances by viewModel.archivedAccountsWithBalances.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()

    var editingAccount by remember { mutableStateOf<UserAccount?>(null) }
    var isAddingAccount by remember { mutableStateOf(false) }
    var isArchivedExpanded by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<UserAccount?>(null) }
    var selectedAccountForDetail by remember { mutableStateOf<AccountWithBalance?>(null) }

    BackHandler(enabled = selectedAccountForDetail != null) {
        selectedAccountForDetail = null
    }

    if (isAddingAccount || editingAccount != null) {
        AddEditAccountDialog(
            account = editingAccount,
            isBangla = isBn,
            onDismiss = {
                isAddingAccount = false
                editingAccount = null
            },
            onSave = { name, type, currency, startingBalance, colorHex, iconName ->
                if (editingAccount == null) {
                    viewModel.addAccount(name, type, currency, startingBalance, colorHex, iconName)
                } else {
                    viewModel.updateAccount(
                        id = editingAccount!!.id,
                        name = name,
                        type = type,
                        currencyCode = currency,
                        startingBalance = startingBalance,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                }
                isAddingAccount = false
                editingAccount = null
            }
        )
    }

    if (accountToDelete != null) {
        val target = accountToDelete!!
        DeleteConfirmationDialog(
            title = if (isBn) "অ্যাকাউন্ট মুছে ফেলবেন?" else "Delete Account?",
            message = if (isBn)
                "আপনি কি নিশ্চিত যে '${target.name}' অ্যাকাউন্টটি মুছে ফেলতে চান? যদি এতে কোনো পূর্ববর্তী লেনদেন থাকে তবে এটি মোছা যাবে না (সেক্ষেত্রে অ্যাকাউন্টটি আর্কাইভ রাখতে পারেন)।"
            else
                "Are you sure you want to delete '${target.name}'? If it contains past transactions, it cannot be deleted and should remain archived.",
            itemDetail = "${target.name} • ${if (isBn) target.type.labelBn else target.type.labelEn}",
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteAccount(target.id)
                accountToDelete = null
                if (selectedAccountForDetail?.account?.id == target.id) {
                    selectedAccountForDetail = null
                }
            },
            onDismiss = {
                accountToDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectedAccountForDetail != null) {
                val currentAcc = selectedAccountForDetail!!.account
                DetailTopAppBar(
                    title = currentAcc.name,
                    onNavigateBack = { selectedAccountForDetail = null },
                    isNetworkConnected = isNetworkConnected,
                    actions = {
                        IconButton(
                            onClick = { editingAccount = currentAcc },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("topbar_edit_account_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isBn) "সম্পাদন করুন" else "Edit",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = { accountToDelete = currentAcc },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("topbar_delete_account_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isBn) "মুছুন" else "Delete",
                                tint = ExpenseRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            } else {
                DetailTopAppBar(
                    title = Localization.getString(Localization.Key.MY_ACCOUNTS, isBn),
                    onNavigateBack = onNavigateBack,
                    isNetworkConnected = isNetworkConnected,
                    actions = {
                        IconButton(
                            onClick = { isAddingAccount = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("topbar_add_account_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = Localization.getString(Localization.Key.ADD_ACCOUNT, isBn),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedAccountForDetail == null) {
                FloatingActionButton(
                    onClick = { isAddingAccount = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_account_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
                }
            }
        }
    ) { padding ->
        if (selectedAccountForDetail != null) {
            val currentAcc = selectedAccountForDetail!!.account
            val latestItem = accountsWithBalances.find { it.account.id == currentAcc.id }
                ?: archivedAccountsWithBalances.find { it.account.id == currentAcc.id }
                ?: selectedAccountForDetail!!
            AccountDetailContent(
                item = latestItem,
                transactions = allTransactions.filter {
                    it.transaction.accountId == currentAcc.id || it.transaction.toAccountId == currentAcc.id
                },
                isBangla = isBn,
                useBnDigits = useBnDigits,
                hideBalances = hideBalances,
                currencySymbol = currSymbol,
                modifier = modifier.padding(padding)
            )
        } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Net Worth Summary Banner (Flat Card Design System)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = Localization.getString(Localization.Key.NET_WORTH, isBn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CurrencyAmountText(
                        amount = totalBalance,
                        currencySymbol = currSymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Accounts List
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (accountsWithBalances.isEmpty() && archivedAccountsWithBalances.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Add,
                            title = Localization.getString(Localization.Key.EMPTY_ACCOUNTS, isBn),
                            description = if (isBn) "নতুন অ্যাকাউন্ট তৈরি করতে নিচের + বাটনে চাপ দিন" else "Tap + below to create a new account",
                            actionLabel = Localization.getString(Localization.Key.ADD_ACCOUNT, isBn),
                            onActionClick = { isAddingAccount = true }
                        )
                    }
                } else if (accountsWithBalances.isEmpty() && archivedAccountsWithBalances.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isBn) "বর্তমানে কোনো সক্রিয় অ্যাকাউন্ট নেই" else "No active accounts",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBn) "আপনার অ্যাকাউন্টগুলো নিচে আর্কাইভ করা রয়েছে। নিচের সেকশন থেকে যেকোনো অ্যাকাউন্ট পুনরায় চালু করতে পারেন অথবা নতুন অ্যাকাউন্ট যুক্ত করতে পারেন।"
                                    else "All accounts are archived. You can restore them from the section below or add a new account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(accountsWithBalances, key = { it.account.id }) { item ->
                        AccountDetailedItem(
                            item = item,
                            isBangla = isBn,
                            useBnDigits = useBnDigits,
                            hideBalances = hideBalances,
                            currencySymbol = currSymbol,
                            onAccountClick = { selectedAccountForDetail = item },
                            onEdit = { editingAccount = item.account },
                            onArchive = {
                                isArchivedExpanded = true
                                viewModel.archiveAccount(item.account.id, true)
                            },
                            onDelete = { accountToDelete = item.account }
                        )
                    }
                }

                // Archived Accounts Expandable Section
                if (archivedAccountsWithBalances.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isArchivedExpanded = !isArchivedExpanded }
                                .testTag("archived_accounts_header"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = Localization.getString(Localization.Key.ARCHIVED_ACCOUNTS, isBn),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) {
                                                val countStr = archivedAccountsWithBalances.size.toString()
                                                Text(
                                                    text = if (useBnDigits) Formatters.toBanglaDigits(countStr) else countStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = Localization.getString(Localization.Key.ARCHIVED_ACCOUNTS_SUBTITLE, isBn),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isArchivedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isArchivedExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isArchivedExpanded) {
                        items(archivedAccountsWithBalances, key = { "archived_${it.account.id}" }) { item ->
                            ArchivedAccountDetailedItem(
                                item = item,
                                isBangla = isBn,
                                useBnDigits = useBnDigits,
                                hideBalances = hideBalances,
                                currencySymbol = currSymbol,
                                onAccountClick = { selectedAccountForDetail = item },
                                onEdit = { editingAccount = item.account },
                                onUnarchive = { viewModel.archiveAccount(item.account.id, false) },
                                onDelete = { accountToDelete = item.account }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun AccountDetailedItem(
    item: AccountWithBalance,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onAccountClick: () -> Unit = {},
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val acc = item.account
    val accColor = parseColorHex(acc.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onAccountClick() }
            .testTag("account_item_${acc.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(acc.iconName),
                    contentDescription = null,
                    tint = accColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = acc.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBangla) acc.type.labelBn else acc.type.labelEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                CurrencyAmountText(
                    amount = item.balance,
                    currencySymbol = currencySymbol,
                    useBanglaDigits = useBnDigits,
                    hideBalances = hideBalances,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.balance < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedAccountDetailedItem(
    item: AccountWithBalance,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onAccountClick: () -> Unit = {},
    onEdit: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    val acc = item.account
    val accColor = parseColorHex(acc.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onAccountClick() }
            .testTag("archived_account_item_${acc.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(acc.iconName),
                    contentDescription = null,
                    tint = accColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = acc.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = Localization.getString(Localization.Key.ARCHIVED_BADGE, isBangla),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (isBangla) acc.type.labelBn else acc.type.labelEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                CurrencyAmountText(
                    amount = item.balance,
                    currencySymbol = currencySymbol,
                    useBanglaDigits = useBnDigits,
                    hideBalances = hideBalances,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.balance < 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    IconButton(
                        onClick = onUnarchive,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Unarchive,
                            contentDescription = Localization.getString(Localization.Key.UNARCHIVE, isBangla),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Account Detail Content showing account details and associated transactions.
 * Designed with clean visual hierarchy, without redundant duplicate titles inside body cards.
 */
@Composable
fun AccountDetailContent(
    item: AccountWithBalance,
    transactions: List<TransactionWithDetails>,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val acc = item.account
    val accColor = parseColorHex(acc.colorHex)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Account Overview Card (NO redundant duplicate title inside card)
        item {
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
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconByName(acc.iconName),
                                contentDescription = null,
                                tint = accColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isBangla) acc.type.labelBn else acc.type.labelEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isBangla) "বর্তমান ব্যালেন্স" else "Current Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    CurrencyAmountText(
                        amount = item.balance,
                        currencySymbol = currencySymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (item.balance < 0) ExpenseRed else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBangla) "শুরুর ব্যালেন্স: ${Formatters.formatAmount(acc.startingBalance, currencySymbol, useBnDigits)}" else "Starting Balance: ${Formatters.formatAmount(acc.startingBalance, currencySymbol, useBnDigits)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "(${acc.currencyCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Transactions Header
        item {
            Text(
                text = if (isBangla) "অ্যাকাউন্টের সাম্প্রতিক লেনদেন" else "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (transactions.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = if (isBangla) "কোনো লেনদেন নেই" else "No Transactions",
                    description = if (isBangla) "এই অ্যাকাউন্টে এখনও কোনো লেনদেন রেকর্ড করা হয়নি" else "No transactions recorded for this account yet"
                )
            }
        } else {
            items(transactions, key = { it.transaction.id }) { tx ->
                TransactionRowItem(
                    item = tx,
                    isBangla = isBangla,
                    useBnDigits = useBnDigits,
                    hideBalances = hideBalances,
                    currencySymbol = currencySymbol,
                    onClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    account: UserAccount?,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, currency: String, startingBalance: Double, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.CASH) }
    var startingBalanceText by remember { mutableStateOf(account?.startingBalance?.toString() ?: "0.0") }
    var selectedColorHex by remember { mutableStateOf(account?.colorHex ?: "#0F766E") }
    var selectedIconName by remember { mutableStateOf(account?.iconName ?: "account_balance_wallet") }

    val colors = listOf("#0F766E", "#DC2626", "#D97706", "#2563EB", "#7C3AED", "#16A34A", "#64748B")
    val icons = listOf("account_balance_wallet", "payments", "account_balance", "credit_card", "store", "savings")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (account == null) Localization.getString(Localization.Key.ADD_ACCOUNT, isBangla)
                        else if (isBangla) "অ্যাকাউন্ট সম্পাদনা" else "Edit Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Localization.getString(Localization.Key.NAME, isBangla)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Account Type Selector
                Text(
                    text = if (isBangla) "অ্যাকাউন্টের ধরন" else "Account Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(AccountType.values().toList()) { type ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedType = type }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = if (isBangla) type.labelBn else type.labelEn,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = startingBalanceText,
                    onValueChange = { startingBalanceText = it },
                    label = { Text(if (isBangla) "প্রারম্ভিক ব্যালেন্স" else "Starting Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Color Palette
                Text(
                    text = if (isBangla) "রঙ নির্ধারণ করুন" else "Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        val color = parseColorHex(hex)
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = hex }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Icon Selector
                Text(
                    text = if (isBangla) "আইকন" else "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    icons.forEach { iconName ->
                        val isSelected = selectedIconName == iconName
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIconName = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconByName(iconName),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val startBal = startingBalanceText.toDoubleOrNull() ?: 0.0
                                onSave(name.trim(), selectedType, "BDT", startBal, selectedColorHex, selectedIconName)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_account_btn")
                    ) {
                        Text(Localization.getString(Localization.Key.SAVE, isBangla), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
