package com.plusemon.hisab.ui.screens.recurring

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.data.model.Frequency
import com.plusemon.hisab.data.model.RecurringRule
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.DetailTopAppBar
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

    var showAddDialog by remember { mutableStateOf(false) }
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

    if (showAddDialog) {
        AddRecurringRuleDialog(
            accounts = accountsWithBalances.map { it.account },
            categories = categories,
            isBangla = isBn,
            onDismiss = { showAddDialog = false },
            onSave = { accountId, categoryId, amount, type, freq, note ->
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
                showAddDialog = false
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
                        onClick = { showAddDialog = true },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
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
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBn) "কোনো পুনরাবৃত্ত লেনদেনের নিয়ম নেই" else "No recurring rules configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recurringRules, key = { it.id }) { rule ->
                        val acc = accountsWithBalances.firstOrNull { it.account.id == rule.accountId }?.account
                        val cat = categories.firstOrNull { it.id == rule.categoryId }
                        val isExpense = rule.type == TransactionType.EXPENSE

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cat != null) {
                                    CategoryIconBadge(
                                        iconName = cat.iconName,
                                        colorHex = cat.colorHex,
                                        size = 42.dp,
                                        iconSize = 22.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat?.localizedName(isBn) ?: (if (isBn) "সাধারণ" else "General"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${acc?.name ?: ""} • ${if (isBn) rule.frequency.labelBn else rule.frequency.labelEn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (rule.note.isNotBlank()) {
                                        Text(
                                            text = rule.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                                    Row(modifier = Modifier.padding(top = 4.dp)) {
                                        IconButton(
                                            onClick = { viewModel.togglePauseRecurringRule(rule.id, rule.isPaused) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (rule.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = if (rule.isPaused) "Resume" else "Pause",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { ruleToDelete = rule },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = ExpenseRed
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
fun AddRecurringRuleDialog(
    accounts: List<com.plusemon.hisab.data.model.UserAccount>,
    categories: List<com.plusemon.hisab.data.model.Category>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (accountId: Long, categoryId: Long?, amount: Double, type: TransactionType, freq: Frequency, note: String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val typeCategories = remember(categories, selectedType) { categories.filter { it.type == selectedType } }
    var selectedCategoryId by remember { mutableStateOf<Long?>(typeCategories.firstOrNull()?.id) }
    var selectedFreq by remember { mutableStateOf(Frequency.MONTHLY) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = if (isBangla) "স্বয়ংক্রিয় নিয়ম তৈরি করুন" else "Create Recurring Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(Localization.getString(Localization.Key.AMOUNT, isBangla)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Frequency Selector
                Text(
                    text = Localization.getString(Localization.Key.FREQUENCY, isBangla),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Frequency.values().forEach { freq ->
                        val isSelected = selectedFreq == freq
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFreq = freq }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isBangla) freq.labelBn else freq.labelEn,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Localization.getString(Localization.Key.NOTE, isBangla)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

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
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && selectedAccountId > 0) {
                                onSave(selectedAccountId, selectedCategoryId, amt, selectedType, selectedFreq, note)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Localization.getString(Localization.Key.SAVE, isBangla), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
