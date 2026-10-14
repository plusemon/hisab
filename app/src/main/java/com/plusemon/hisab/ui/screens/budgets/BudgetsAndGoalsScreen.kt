package com.plusemon.hisab.ui.screens.budgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.SavingsGoal
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.components.getIconByName
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.theme.AmberTertiary
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.viewmodel.BudgetStatus
import com.plusemon.hisab.ui.viewmodel.CategorySpendProgress
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsAndGoalsScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = settings.numeralSystem == "bn"
    val hideBalances = settings.hideBalances
    val currSymbol = settings.currencySymbol

    val categoryProgressList by viewModel.categoryProgressList.collectAsState()
    val overallBudgetProgress by viewModel.overallBudgetProgress.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    var showAddBudgetBottomSheet by remember { mutableStateOf(false) }
    var showAddGoalBottomSheet by remember { mutableStateOf(false) }
    var adjustingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var isDepositMode by remember { mutableStateOf(true) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var budgetToDelete by remember { mutableStateOf<CategorySpendProgress?>(null) }

    // Dialogs & Bottom Sheets
    if (goalToDelete != null) {
        val target = goalToDelete!!
        val amountStr = Formatters.formatAmount(target.targetAmount, currSymbol, useBnDigits)
        DeleteConfirmationDialog(
            title = if (isBn) "সঞ্চয় লক্ষ্য মুছে ফেলবেন?" else "Delete Savings Goal?",
            message = if (isBn)
                "আপনি কি নিশ্চিত যে '${target.name}' সঞ্চয় লক্ষ্যটি মুছে ফেলতে চান?"
            else
                "Are you sure you want to delete the savings goal '${target.name}'?",
            itemDetail = "${target.name} • ${if (isBn) "লক্ষ্যমাত্রা:" else "Target:"} $amountStr",
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteSavingsGoal(target.id)
                goalToDelete = null
            },
            onDismiss = {
                goalToDelete = null
            }
        )
    }

    if (budgetToDelete != null) {
        val target = budgetToDelete!!
        val isOverall = target.category.id == 0L || target.budgetLimit == overallBudgetProgress?.budgetLimit && target.category.nameEn == "Overall Budget"
        val matchedBudget = if (isOverall) {
            budgets.firstOrNull { it.categoryId == null }
        } else {
            budgets.firstOrNull { it.categoryId == target.category.id }
        }
        val budgetTitle = if (isOverall) (if (isBn) "মোট বাজেট" else "Overall Budget") else target.category.localizedName(isBn)
        val limitStr = Formatters.formatAmount(target.budgetLimit ?: 0.0, currSymbol, useBnDigits)

        DeleteConfirmationDialog(
            title = if (isBn) "বাজেট মুছে ফেলবেন?" else "Delete Budget?",
            message = if (isBn)
                "আপনি কি নিশ্চিত যে '$budgetTitle'-এর বাজেট মুছে ফেলতে চান?"
            else
                "Are you sure you want to delete the budget for '$budgetTitle'?",
            itemDetail = "$budgetTitle • ${if (isBn) "সীমা:" else "Limit:"} $limitStr",
            isBangla = isBn,
            onConfirm = {
                if (matchedBudget != null) {
                    viewModel.deleteBudget(matchedBudget.id)
                }
                budgetToDelete = null
            },
            onDismiss = {
                budgetToDelete = null
            }
        )
    }

    if (showAddBudgetBottomSheet) {
        AddEditBudgetBottomSheet(
            categories = categories.filter { it.type == TransactionType.EXPENSE },
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { showAddBudgetBottomSheet = false },
            onSave = { categoryId, limit ->
                viewModel.saveBudget(categoryId, limit)
                showAddBudgetBottomSheet = false
            }
        )
    }

    if (showAddGoalBottomSheet) {
        AddSavingsGoalBottomSheet(
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { showAddGoalBottomSheet = false },
            onSave = { name, targetAmount, targetDate, note, colorHex, iconName ->
                viewModel.addSavingsGoal(name, targetAmount, targetDate, note, colorHex, iconName)
                showAddGoalBottomSheet = false
            }
        )
    }

    if (adjustingGoal != null) {
        AdjustGoalSavingsBottomSheet(
            goal = adjustingGoal!!,
            isDeposit = isDepositMode,
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { adjustingGoal = null },
            onConfirm = { amount ->
                val delta = if (isDepositMode) amount else -amount
                viewModel.adjustGoalSavings(adjustingGoal!!.id, delta)
                adjustingGoal = null
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) showAddBudgetBottomSheet = true else showAddGoalBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("budgets_goals_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Header (Budgets vs Savings Goals)
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Text(
                            text = Localization.getString(Localization.Key.BUDGETS, isBn),
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_budgets")
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Text(
                            text = Localization.getString(Localization.Key.SAVINGS_GOALS, isBn),
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_goals")
                )
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // BUDGETS TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Overall Budget Card (if configured)
                        if (overallBudgetProgress != null) {
                            item {
                                OverallBudgetCard(
                                    progress = overallBudgetProgress!!,
                                    isBangla = isBn,
                                    useBnDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    currSymbol = currSymbol,
                                    onDelete = { budgetToDelete = overallBudgetProgress }
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (overallBudgetProgress != null) 8.dp else 0.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "ক্যাটাগরি অনুযায়ী বাজেট" else "Category Budgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val budgetedCategories = categoryProgressList.filter { it.budgetLimit != null && it.budgetLimit > 0 }
                        if (budgetedCategories.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.PieChart,
                                    title = Localization.getString(Localization.Key.EMPTY_BUDGETS, isBn),
                                    description = if (isBn) "মাসিক বাজেট নির্ধারণ করতে নিচের + বাটনে চাপ দিন।" else "Tap the + button below to set up your monthly budget.",
                                    actionLabel = if (isBn) "+ বাজেট সেট করুন" else "+ Set Budget",
                                    onActionClick = { showAddBudgetBottomSheet = true }
                                )
                            }
                        } else {
                            items(budgetedCategories, key = { it.category.id }) { item ->
                                CategoryBudgetProgressItem(
                                    progress = item,
                                    isBangla = isBn,
                                    useBnDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    currSymbol = currSymbol,
                                    onDelete = { budgetToDelete = item }
                                )
                            }
                        }
                    }
                } else {
                    // SAVINGS GOALS TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (savingsGoals.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.Savings,
                                    title = Localization.getString(Localization.Key.EMPTY_GOALS, isBn),
                                    description = if (isBn) "সঞ্চয় লক্ষ্য সেট করতে নিচের + বাটনে চাপ দিন।" else "Tap the + button below to create a savings goal.",
                                    actionLabel = if (isBn) "+ সঞ্চয় লক্ষ্য যোগ করুন" else "+ Create Savings Goal",
                                    onActionClick = { showAddGoalBottomSheet = true }
                                )
                            }
                        } else {
                            items(savingsGoals, key = { it.id }) { goal ->
                                SavingsGoalItemCard(
                                    goal = goal,
                                    isBangla = isBn,
                                    useBnDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    currSymbol = currSymbol,
                                    onDeposit = {
                                        adjustingGoal = goal
                                        isDepositMode = true
                                    },
                                    onWithdraw = {
                                        adjustingGoal = goal
                                        isDepositMode = false
                                    },
                                    onDelete = { goalToDelete = goal }
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
fun OverallBudgetCard(
    progress: CategorySpendProgress,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currSymbol: String,
    onDelete: (() -> Unit)? = null
) {
    val statusColor = when (progress.status) {
        BudgetStatus.EXCEEDED -> ExpenseRed
        BudgetStatus.WARNING -> AmberTertiary
        else -> IncomeGreen
    }

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString(Localization.Key.OVERALL_BUDGET, isBangla),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (progress.status) {
                                BudgetStatus.EXCEEDED -> Localization.getString(Localization.Key.BUDGET_EXCEEDED, isBangla)
                                BudgetStatus.WARNING -> Localization.getString(Localization.Key.BUDGET_WARNING, isBangla)
                                else -> Localization.getString(Localization.Key.BUDGET_SAFE, isBangla)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Budget",
                                tint = ExpenseRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = Localization.getString(Localization.Key.SPENT, isBangla),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmountText(
                        amount = progress.spentAmount,
                        currencySymbol = currSymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.getString(Localization.Key.LIMIT, isBangla),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmountText(
                        amount = progress.budgetLimit ?: 0.0,
                        currencySymbol = currSymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetProgressItem(
    progress: CategorySpendProgress,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currSymbol: String,
    onDelete: (() -> Unit)? = null
) {
    val statusColor = when (progress.status) {
        BudgetStatus.EXCEEDED -> ExpenseRed
        BudgetStatus.WARNING -> AmberTertiary
        else -> IncomeGreen
    }

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
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    iconName = progress.category.iconName,
                    colorHex = progress.category.colorHex,
                    size = 40.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.category.localizedName(isBangla),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val remaining = ((progress.budgetLimit ?: 0.0) - progress.spentAmount).coerceAtLeast(0.0)
                    Text(
                        text = "${Localization.getString(Localization.Key.REMAINING, isBangla)}: ${Formatters.formatAmount(remaining, currSymbol, useBnDigits, hideBalances)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(progress.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Budget",
                                tint = ExpenseRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun SavingsGoalItemCard(
    goal: SavingsGoal,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currSymbol: String,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    val progressPercent = if (goal.targetAmount > 0) (goal.currentSavedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val goalColor = parseColorHex(goal.colorHex)

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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName(goal.iconName),
                        contentDescription = null,
                        tint = goalColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.targetDate != null) {
                        Text(
                            text = "${Localization.getString(Localization.Key.TARGET_DATE, isBangla)}: ${Formatters.formatDate(goal.targetDate, isBangla)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = Localization.getString(Localization.Key.CURRENT_SAVED, isBangla),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmountText(
                        amount = goal.currentSavedAmount,
                        currencySymbol = currSymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.getString(Localization.Key.TARGET_AMOUNT, isBangla),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmountText(
                        amount = goal.targetAmount,
                        currencySymbol = currSymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.getString(Localization.Key.WITHDRAW, isBangla))
                }

                Button(
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = goalColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Localization.getString(Localization.Key.DEPOSIT, isBangla), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetBottomSheet(
    categories: List<Category>,
    isBangla: Boolean,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (categoryId: Long?, limit: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    var selectedCategoryId by remember { mutableStateOf<Long?>(categories.firstOrNull()?.id) }
    var limitText by remember { mutableStateOf("") }

    val quickAmounts = listOf(1000, 2000, 5000, 10000, 20000)

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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.getString(Localization.Key.MONTHLY_BUDGET, isBangla),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBangla) "মোড পরিবর্তন করতে ডানে/বামে সোয়াইপ করুন" else "Swipe left / right to change modes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented toggle control buttons connected dynamically to sheetPagerState
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sheetPagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            sheetPagerState.animateScrollToPage(0)
                        }
                    },
                    label = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isBangla) "ক্যাটাগরি বাজেট" else "Category Budget",
                                fontWeight = if (sheetPagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("budget_tab_category"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = sheetPagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            sheetPagerState.animateScrollToPage(1)
                        }
                    },
                    label = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isBangla) "মোট বাজেট" else "Overall Budget",
                                fontWeight = if (sheetPagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("budget_tab_overall"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inner HorizontalPager with userScrollEnabled = true for swipe gestures
            HorizontalPager(
                state = sheetPagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    if (page == 0) {
                        // Page 0: "ক্যাটাগরি বাজেট" form (Dropdown for category selection + amount input)
                        Text(
                            text = Localization.getString(Localization.Key.CATEGORY, isBangla),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (categories.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            val currentCat = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = currentCat.localizedName(isBangla),
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = {
                                        CategoryIconBadge(
                                            iconName = currentCat.iconName,
                                            colorHex = currentCat.colorHex,
                                            size = 32.dp,
                                            iconSize = 18.dp
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    categories.forEach { cat ->
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
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (isBangla) "কোনো ব্যয়ের ক্যাটাগরি পাওয়া যায়নি" else "No expense categories found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Limit Amount Field
                        Text(
                            text = Localization.getString(Localization.Key.LIMIT, isBangla),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = limitText,
                            onValueChange = { limitText = it },
                            label = { Text(if (isBangla) "টাকার পরিমাণ" else "Limit Amount") },
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
                                .testTag("budget_limit_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick suggestion chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmounts.forEach { amount ->
                                FilterChip(
                                    selected = limitText == amount.toString(),
                                    onClick = { limitText = amount.toString() },
                                    label = { Text("+$currencySymbol$amount", fontSize = 12.sp) }
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                            }

                            Button(
                                onClick = {
                                    val limit = limitText.toDoubleOrNull() ?: 0.0
                                    if (limit > 0 && selectedCategoryId != null) {
                                        onSave(selectedCategoryId, limit)
                                    }
                                },
                                enabled = (limitText.toDoubleOrNull() ?: 0.0) > 0 && selectedCategoryId != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_budget_button")
                            ) {
                                Text(
                                    text = Localization.getString(Localization.Key.SAVE, isBangla),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Page 1: "মোট বাজেট" form (Overall budget limit amount input without category picker)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) "সার্বিক মাসিক বাজেট" else "Overall Monthly Budget",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBangla)
                                            "সম্পূর্ণ মাসের সমস্ত খরচের জন্য একটি সাধারণ সামগ্রিক বাজেট সীমা। কোনো ক্যাটাগরি নির্বাচনের প্রয়োজন নেই।"
                                        else
                                            "A unified spending limit across all categories combined for the month. No category selection required.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Limit Amount Field
                        Text(
                            text = Localization.getString(Localization.Key.LIMIT, isBangla),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = limitText,
                            onValueChange = { limitText = it },
                            label = { Text(if (isBangla) "টাকার পরিমাণ" else "Limit Amount") },
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
                                .testTag("budget_limit_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick suggestion chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickAmounts.forEach { amount ->
                                FilterChip(
                                    selected = limitText == amount.toString(),
                                    onClick = { limitText = amount.toString() },
                                    label = { Text("+$currencySymbol$amount", fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                            }

                            Button(
                                onClick = {
                                    val limit = limitText.toDoubleOrNull() ?: 0.0
                                    if (limit > 0) {
                                        onSave(null, limit)
                                    }
                                },
                                enabled = (limitText.toDoubleOrNull() ?: 0.0) > 0,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_budget_button")
                            ) {
                                Text(Localization.getString(Localization.Key.SAVE, isBangla), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun AddEditBudgetDialog(
    categories: List<Category>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long?, limit: Double) -> Unit
) {
    AddEditBudgetBottomSheet(
        categories = categories,
        isBangla = isBangla,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingsGoalBottomSheet(
    isBangla: Boolean,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Double, targetDate: Long?, note: String, colorHex: String, iconName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0D9488") }
    var selectedIcon by remember { mutableStateOf("savings") }

    val quickAmounts = listOf(5000, 10000, 20000, 50000, 100000)
    val colorPresets = listOf("#0D9488", "#10B981", "#F59E0B", "#3B82F6", "#8B5CF6", "#EF4444")
    val iconPresets = listOf("savings", "flight", "directions_car", "home", "phone_android", "laptop", "shopping_bag", "school")

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.getString(Localization.Key.SAVINGS_GOALS, isBangla),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBangla) "ভবিষ্যতের জন্য সঞ্চয় লক্ষ্য তৈরি করুন" else "Create a savings goal for your future",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Localization.getString(Localization.Key.GOAL_NAME, isBangla)) },
                placeholder = { Text(if (isBangla) "যেমন: নতুন ফোন, ভ্রমণ, জরুরি তহবিল" else "e.g. New Phone, Travel, Emergency Fund") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Target Amount
            OutlinedTextField(
                value = targetAmountText,
                onValueChange = { targetAmountText = it },
                label = { Text(Localization.getString(Localization.Key.TARGET_AMOUNT, isBangla)) },
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
                modifier = Modifier.fillMaxWidth().testTag("goal_amount_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick amount chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { amount ->
                    FilterChip(
                        selected = targetAmountText == amount.toString(),
                        onClick = { targetAmountText = amount.toString() },
                        label = { Text("$currencySymbol$amount", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Selection
            Text(
                text = if (isBangla) "আইকন নির্বাচন করুন" else "Select Icon",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                iconPresets.forEach { iconName ->
                    val isSelected = selectedIcon == iconName
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) parseColorHex(selectedColor).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedIcon = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconName),
                            contentDescription = null,
                            tint = if (isSelected) parseColorHex(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selection
            Text(
                text = if (isBangla) "রঙ নির্বাচন করুন" else "Select Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colorPresets.forEach { hex ->
                    val isSelected = selectedColor == hex
                    val col = parseColorHex(hex)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(col)
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                }

                Button(
                    onClick = {
                        val target = targetAmountText.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && target > 0) {
                            onSave(name.trim(), target, null, "", selectedColor, selectedIcon)
                        }
                    },
                    enabled = name.isNotBlank() && (targetAmountText.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("save_goal_button")
                ) {
                    Text(Localization.getString(Localization.Key.SAVE, isBangla), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun AddSavingsGoalDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Double, targetDate: Long?, note: String, colorHex: String, iconName: String) -> Unit
) {
    AddSavingsGoalBottomSheet(
        isBangla = isBangla,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustGoalSavingsBottomSheet(
    goal: SavingsGoal,
    isDeposit: Boolean,
    isBangla: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    val goalColor = parseColorHex(goal.colorHex)
    val quickAddAmounts = listOf(500, 1000, 2000, 5000)

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(goalColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(goal.iconName),
                            contentDescription = null,
                            tint = goalColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isDeposit) "${Localization.getString(Localization.Key.DEPOSIT, isBangla)} (${goal.name})"
                            else "${Localization.getString(Localization.Key.WITHDRAW, isBangla)} (${goal.name})",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (isBangla) "বর্তমান সঞ্চয়:" else "Current Saved:"} $currencySymbol${goal.currentSavedAmount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(Localization.getString(Localization.Key.AMOUNT, isBangla)) },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("adjust_goal_amount_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAddAmounts.forEach { amt ->
                    FilterChip(
                        selected = amountText == amt.toString(),
                        onClick = { amountText = amt.toString() },
                        label = { Text("+$currencySymbol$amt", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Localization.getString(Localization.Key.CANCEL, isBangla))
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onConfirm(amt)
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = goalColor),
                    modifier = Modifier.weight(1f).testTag("confirm_adjust_goal_button")
                ) {
                    Text(Localization.getString(Localization.Key.SAVE, isBangla), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun AdjustGoalSavingsDialog(
    goal: SavingsGoal,
    isDeposit: Boolean,
    isBangla: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    AdjustGoalSavingsBottomSheet(
        goal = goal,
        isDeposit = isDeposit,
        isBangla = isBangla,
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
