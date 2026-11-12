package com.plusemon.hisab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.TransferBlue
import com.plusemon.hisab.ui.viewmodel.AccountWithBalance

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFilterBottomSheet(
    initialPeriod: String,
    initialType: TransactionType?,
    initialAccountId: Long?,
    initialCategoryId: Long? = null,
    accounts: List<AccountWithBalance>,
    categories: List<Category> = emptyList(),
    isBangla: Boolean,
    useBanglaDigits: Boolean,
    calculateMatchCount: (period: String, type: TransactionType?, accountId: Long?, categoryId: Long?) -> Int,
    onApply: (period: String, type: TransactionType?, accountId: Long?, categoryId: Long?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftPeriod by remember(initialPeriod) { mutableStateOf(initialPeriod) }
    var draftType by remember(initialType) { mutableStateOf(initialType) }
    var draftAccountId by remember(initialAccountId) { mutableStateOf(initialAccountId) }
    var draftCategoryId by remember(initialCategoryId) { mutableStateOf(initialCategoryId) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetContainerColor = if (isDark) Color(0xFF161F30) else MaterialTheme.colorScheme.surface
    val dragHandleColor = if (isDark) Color(0xFF475569) else MaterialTheme.colorScheme.outlineVariant

    val isAnyFilterActive = draftPeriod != "THIS_MONTH" || draftType != null || draftAccountId != null || draftCategoryId != null
    val matchCount = remember(draftPeriod, draftType, draftAccountId, draftCategoryId) {
        calculateMatchCount(draftPeriod, draftType, draftAccountId, draftCategoryId)
    }

    val displayedCategories = remember(categories, draftType) {
        when (draftType) {
            TransactionType.EXPENSE -> categories.filter { it.type == TransactionType.EXPENSE }
            TransactionType.INCOME -> categories.filter { it.type == TransactionType.INCOME }
            else -> categories
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = sheetContainerColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = dragHandleColor)
        },
        modifier = modifier.testTag("filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = Localization.getString(Localization.Key.FILTER_TITLE, isBangla),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Localization.getString(Localization.Key.FILTER_SUBTITLE, isBangla),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            draftPeriod = "THIS_MONTH"
                            draftType = null
                            draftAccountId = null
                            draftCategoryId = null
                            onReset()
                        },
                        enabled = isAnyFilterActive,
                        modifier = Modifier.testTag("filter_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.getString(Localization.Key.RESET_FILTERS, isBangla),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("filter_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Time Period
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Localization.getString(Localization.Key.TIME_PERIOD, isBangla),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val periods = listOf(
                        Pair("THIS_MONTH", if (isBangla) "এই মাস" else "This Month"),
                        Pair("LAST_MONTH", if (isBangla) "গত মাস" else "Last Month"),
                        Pair("THIS_YEAR", Localization.getString(Localization.Key.THIS_YEAR, isBangla)),
                        Pair("ALL", Localization.getString(Localization.Key.ALL_TIME, isBangla))
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        periods.forEach { (key, label) ->
                            val isSelected = draftPeriod == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { draftPeriod = key },
                                label = { Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("filter_period_$key")
                            )
                        }
                    }
                }

                // Section 2: Transaction Type
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Localization.getString(Localization.Key.TRANSACTION_TYPE, isBangla),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // All Type
                        val isAllSelected = draftType == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { draftType = null },
                            label = {
                                Text(
                                    Localization.getString(Localization.Key.ALL, isBangla),
                                    fontSize = 13.sp,
                                    fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isAllSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("filter_type_all")
                        )

                        // Expense
                        val isExpenseSelected = draftType == TransactionType.EXPENSE
                        FilterChip(
                            selected = isExpenseSelected,
                            onClick = {
                                draftType = if (isExpenseSelected) null else TransactionType.EXPENSE
                            },
                            label = {
                                Text(
                                    Localization.getString(Localization.Key.EXPENSE_SHORT, isBangla),
                                    fontSize = 13.sp,
                                    fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isExpenseSelected) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = if (isExpenseSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.12f)
                            ),
                            border = if (isExpenseSelected) BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)) else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("filter_type_expense")
                        )

                        // Income
                        val isIncomeSelected = draftType == TransactionType.INCOME
                        FilterChip(
                            selected = isIncomeSelected,
                            onClick = {
                                draftType = if (isIncomeSelected) null else TransactionType.INCOME
                            },
                            label = {
                                Text(
                                    Localization.getString(Localization.Key.INCOME_SHORT, isBangla),
                                    fontSize = 13.sp,
                                    fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isIncomeSelected) IncomeGreen else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = if (isIncomeSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen.copy(alpha = 0.12f)
                            ),
                            border = if (isIncomeSelected) BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.5f)) else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("filter_type_income")
                        )

                        // Transfer
                        val isTransferSelected = draftType == TransactionType.TRANSFER
                        FilterChip(
                            selected = isTransferSelected,
                            onClick = {
                                draftType = if (isTransferSelected) null else TransactionType.TRANSFER
                            },
                            label = {
                                Text(
                                    Localization.getString(Localization.Key.TRANSFER_SHORT, isBangla),
                                    fontSize = 13.sp,
                                    fontWeight = if (isTransferSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTransferSelected) TransferBlue else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = if (isTransferSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TransferBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TransferBlue.copy(alpha = 0.12f)
                            ),
                            border = if (isTransferSelected) BorderStroke(1.dp, TransferBlue.copy(alpha = 0.5f)) else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("filter_type_transfer")
                        )
                    }
                }

                // Section 3: Account
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = Localization.getString(Localization.Key.ACCOUNT, isBangla),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // All Accounts
                            val isAllAccSelected = draftAccountId == null
                            FilterChip(
                                selected = isAllAccSelected,
                                onClick = { draftAccountId = null },
                                label = {
                                    Text(
                                        Localization.getString(Localization.Key.ALL_ACCOUNTS, isBangla),
                                        fontSize = 13.sp,
                                        fontWeight = if (isAllAccSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isAllAccSelected) Icons.Default.Check else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("filter_account_all")
                            )

                            // Specific Accounts
                            accounts.forEach { accWithBal ->
                                val acc = accWithBal.account
                                val isSelected = draftAccountId == acc.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        draftAccountId = if (isSelected) null else acc.id
                                    },
                                    label = {
                                        Text(
                                            text = acc.name,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Check else getIconByName(acc.iconName),
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else parseColorHex(acc.colorHex)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_account_${acc.id}")
                                )
                            }
                        }
                    }
                }
                // Section 4: Category
                if (displayedCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = Localization.getString(Localization.Key.CATEGORY, isBangla),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // All Categories
                            val isAllCatSelected = draftCategoryId == null
                            FilterChip(
                                selected = isAllCatSelected,
                                onClick = { draftCategoryId = null },
                                label = {
                                    Text(
                                        Localization.getString(Localization.Key.ALL_CATEGORIES, isBangla),
                                        fontSize = 13.sp,
                                        fontWeight = if (isAllCatSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isAllCatSelected) Icons.Default.Check else Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("filter_category_all")
                            )

                            // Specific Categories
                            displayedCategories.forEach { cat ->
                                val isSelected = draftCategoryId == cat.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        draftCategoryId = if (isSelected) null else cat.id
                                    },
                                    label = {
                                        Text(
                                            text = cat.localizedName(isBangla),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Check else getIconByName(cat.iconName),
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else parseColorHex(cat.colorHex)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_category_${cat.id}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Apply Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Button(
                    onClick = {
                        onApply(draftPeriod, draftType, draftAccountId, draftCategoryId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("filter_apply_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    val countDisplay = if (useBanglaDigits) Formatters.toBanglaDigits(matchCount.toString()) else matchCount.toString()
                    val buttonText = if (isBangla) {
                        "ফলাফল দেখুন ($countDisplay টি)"
                    } else {
                        "Show Results ($countDisplay)"
                    }
                    Text(
                        text = buttonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
