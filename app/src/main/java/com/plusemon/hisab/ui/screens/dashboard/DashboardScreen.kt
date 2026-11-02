package com.plusemon.hisab.ui.screens.dashboard

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.plusemon.hisab.data.model.DebtType
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.data.model.TransactionWithDetails
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.AccountBalanceWallet
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DashboardBalanceCard
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.components.MonthYearPickerBottomSheet
import com.plusemon.hisab.ui.components.UpdateCard
import com.plusemon.hisab.ui.components.UpdateDownloadingCard
import com.plusemon.hisab.ui.components.UpdateReadyCard
import com.plusemon.hisab.ui.components.getIconByName
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.ShopCreditAmber
import com.plusemon.hisab.ui.theme.TransferBlue
import com.plusemon.hisab.ui.viewmodel.AccountWithBalance
import com.plusemon.hisab.ui.viewmodel.BudgetStatus
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@Composable
fun DashboardScreen(
    viewModel: HisabViewModel,
    onNavigateToAddTransaction: (TransactionType) -> Unit = {},
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = false
    val hideBalances = settings.hideBalances
    val currSymbol = settings.currencySymbol

    val totalBalance by viewModel.totalBalance.collectAsState()
    val accountsWithBalances by viewModel.accountsWithBalances.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categoryProgressList by viewModel.categoryProgressList.collectAsState()
    val overallBudgetProgress by viewModel.overallBudgetProgress.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val allDebts by viewModel.loansDebts.collectAsState()
    val shopCreditPurchases by viewModel.shopCreditPurchases.collectAsState()
    val updateUiState by viewModel.updateUiState.collectAsState()

    val totalReceivable = allDebts.filter { it.type == DebtType.OWED_TO_ME && !it.isSettled }.sumOf { it.remainingAmount }
    val totalPayable = allDebts.filter { it.type == DebtType.I_OWE && !it.isSettled }.sumOf { it.remainingAmount }
    val totalShopOwed = shopCreditPurchases.filter { !it.isSettled }.sumOf { it.remainingAmount }

    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedMonthIncome by viewModel.selectedMonthIncome.collectAsState()
    val selectedMonthExpense by viewModel.selectedMonthExpense.collectAsState()
    val selectedMonthBalance by viewModel.selectedMonthBalance.collectAsState()

    var showMonthPickerSheet by remember { mutableStateOf(false) }

    if (showMonthPickerSheet) {
        MonthYearPickerBottomSheet(
            selectedMonth = selectedMonth,
            onMonthSelected = { newMonth ->
                viewModel.setSelectedMonth(newMonth)
                showMonthPickerSheet = false
            },
            onDismiss = { showMonthPickerSheet = false },
            isBangla = isBn,
            useBanglaDigits = useBnDigits
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        // In-App Updates Card (Available / Downloading Progress / Downloaded)
        when (val state = updateUiState) {
            is com.plusemon.hisab.ui.viewmodel.UpdateUiState.Available -> {
                UpdateCard(
                    updateInfo = state.updateInfo,
                    isBangla = isBn,
                    onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(state.updateInfo) },
                    onLater = { viewModel.dismissUpdate(state.updateInfo.version) }
                )
            }
            is com.plusemon.hisab.ui.viewmodel.UpdateUiState.Downloading -> {
                UpdateDownloadingCard(
                    updateInfo = state.updateInfo,
                    progress = state.progress,
                    isBangla = isBn
                )
            }
            is com.plusemon.hisab.ui.viewmodel.UpdateUiState.Downloaded -> {
                UpdateReadyCard(
                    updateInfo = state.updateInfo,
                    isBangla = isBn,
                    onInstall = { viewModel.retryInstall(state.updateInfo) }
                )
            }
            else -> {}
        }

        // Lending & Shop Credit Informational Summary Card
        if (totalReceivable > 0 || totalPayable > 0 || totalShopOwed > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onNavigateToDebts() }
                    .testTag("dashboard_debts_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "ধার ও দোকান বাকি (তথ্য)" else "Lending & Shop Credit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = if (isBn) "পাওনা" else "Receivable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CurrencyAmountText(amount = totalReceivable, currencySymbol = currSymbol, useBanglaDigits = useBnDigits, hideBalances = hideBalances, color = IncomeGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = if (isBn) "দেনা" else "Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CurrencyAmountText(amount = totalPayable, currencySymbol = currSymbol, useBanglaDigits = useBnDigits, hideBalances = hideBalances, color = ExpenseRed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = if (isBn) "দোকান বাকি" else "Shop Credit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CurrencyAmountText(amount = totalShopOwed, currencySymbol = currSymbol, useBanglaDigits = useBnDigits, hideBalances = hideBalances, color = ShopCreditAmber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Interactive Responsive Hero Balance & Monthly Overview Card
        DashboardBalanceCard(
            totalBalance = selectedMonthBalance,
            monthIncome = selectedMonthIncome,
            monthExpense = selectedMonthExpense,
            currencySymbol = currSymbol,
            useBanglaDigits = useBnDigits,
            hideBalances = hideBalances,
            isBangla = isBn,
            currentMonthYear = selectedMonth.toMonthYearString(),
            onMonthClick = { showMonthPickerSheet = true },
            onIncomeClick = onNavigateToTransactions,
            onExpenseClick = onNavigateToTransactions
        )

        // Informative pill banner when viewing past/future month
        if (!selectedMonth.isCurrent()) {
            val isDarkTheme = settings.isDarkMode
            val bannerBg = if (isDarkTheme) Color(0xFF0F766E).copy(alpha = 0.15f) else Color(0xFFF0FDFA)
            val bannerBorder = if (isDarkTheme) Color(0xFF2DD4BF).copy(alpha = 0.35f) else Color(0xFF99F6E4)
            val bannerTint = if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF0F766E)
            val bannerAction = if (isDarkTheme) Color(0xFF5EEAD4) else Color(0xFF0D9488)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.selectCurrentMonth() }
                    .testTag("reset_month_filter_banner"),
                color = bannerBg,
                border = BorderStroke(1.dp, bannerBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = bannerTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "${Formatters.formatMonthYear(selectedMonth.toMonthYearString(), true)} এর হিসাব"
                                   else "Filtered: ${Formatters.formatMonthYear(selectedMonth.toMonthYearString(), false)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = bannerTint,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = Localization.getString(Localization.Key.RESET_TO_THIS_MONTH, isBn),
                        style = MaterialTheme.typography.labelSmall,
                        color = bannerAction,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Budget Warning Alert Banner
        val budgetWarnings = categoryProgressList.filter { it.status == BudgetStatus.EXCEEDED || it.status == BudgetStatus.WARNING }
        if (budgetWarnings.isNotEmpty()) {
            val topWarning = budgetWarnings.first()
            val isExceeded = topWarning.status == BudgetStatus.EXCEEDED
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onNavigateToBudgets() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isExceeded) ExpenseRed else Color(0xFFD97706)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isExceeded) ExpenseRed else Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isExceeded) Localization.getString(Localization.Key.BUDGET_EXCEEDED, isBn)
                            else Localization.getString(Localization.Key.BUDGET_WARNING, isBn),
                            fontWeight = FontWeight.Bold,
                            color = if (isExceeded) ExpenseRed else Color(0xFFD97706),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${topWarning.category.localizedName(isBn)}: ${Formatters.formatAmount(topWarning.spentAmount, currSymbol, useBnDigits)} / ${Formatters.formatAmount(topWarning.budgetLimit ?: 0.0, currSymbol, useBnDigits)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Smart Spending Insights Card
        if (insights.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = insights.first(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // My Accounts Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Localization.getString(Localization.Key.MY_ACCOUNTS, isBn),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNavigateToAccounts) {
                Text(Localization.getString(Localization.Key.SEE_ALL, isBn), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Horizontal Carousel of Accounts
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accountsWithBalances, key = { it.account.id }) { accWithBal ->
                AccountCarouselCard(
                    item = accWithBal,
                    currSymbol = currSymbol,
                    useBnDigits = useBnDigits,
                    hideBalances = hideBalances,
                    onClick = onNavigateToAccounts
                )
            }
        }

        // Recent Transactions Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Localization.getString(Localization.Key.RECENT_TRANSACTIONS, isBn),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNavigateToTransactions) {
                Text(Localization.getString(Localization.Key.SEE_ALL, isBn), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        val recentTxs = transactions.take(6)
        if (recentTxs.isEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = Localization.getString(Localization.Key.EMPTY_TRANSACTIONS, isBn),
                    description = if (isBn) "উপরে খরচ বা আয় বাটনে ট্যাপ করে নতুন হিসাব লিখুন।" else "Tap + Expense or + Income above to record your first entry."
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentTxs.forEach { txItem ->
                    TransactionRowItem(
                        item = txItem,
                        isBangla = isBn,
                        useBnDigits = useBnDigits,
                        hideBalances = hideBalances,
                        currencySymbol = currSymbol,
                        onClick = { onTransactionClick(txItem) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AccountCarouselCard(
    item: AccountWithBalance,
    currSymbol: String,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    onClick: () -> Unit
) {
    val accColor = parseColorHex(item.account.colorHex)

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .testTag("account_card_${item.account.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName(item.account.iconName),
                        contentDescription = null,
                        tint = accColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            CurrencyAmountText(
                amount = item.balance,
                currencySymbol = currSymbol,
                useBanglaDigits = useBnDigits,
                hideBalances = hideBalances,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.balance < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    item: TransactionWithDetails,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val t = item.transaction
    val (typeColor, sign) = when (t.type) {
        TransactionType.EXPENSE -> Pair(ExpenseRed, "-")
        TransactionType.INCOME -> Pair(IncomeGreen, "+")
        TransactionType.TRANSFER -> Pair(TransferBlue, "⇄")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            )
            .testTag("tx_item_${t.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (t.type == TransactionType.TRANSFER) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = TransferBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else if (item.category != null) {
                CategoryIconBadge(
                    iconName = item.category.iconName,
                    colorHex = item.category.colorHex,
                    size = 42.dp,
                    iconSize = 20.dp
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
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                val title = when (t.type) {
                    TransactionType.TRANSFER -> if (isBangla) "টাকা স্থানান্তর" else "Transfer"
                    else -> item.category?.localizedName(isBangla) ?: if (isBangla) "সাধারণ" else "General"
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Account Name Badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (t.type == TransactionType.TRANSFER) Icons.Default.SwapHoriz else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val accountLabel = if (t.type == TransactionType.TRANSFER) {
                            "${item.account.name} → ${item.toAccount?.name ?: (if (isBangla) "অ্যাকাউন্ট" else "Account")}"
                        } else {
                            item.account.name
                        }
                        Text(
                            text = accountLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (t.note.isNotBlank()) {
                    Text(
                        text = t.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Amount & Date
            Column(horizontalAlignment = Alignment.End) {
                CurrencyAmountText(
                    amount = t.amount,
                    currencySymbol = currencySymbol,
                    useBanglaDigits = useBnDigits,
                    hideBalances = hideBalances,
                    prefix = sign,
                    color = typeColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = Formatters.formatDate(t.dateTimestamp, isBangla),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
