package com.plusemon.hisab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.TransferBlue

/**
 * Step 2: Comprehensive, elegant confirmation and preview view.
 * Displays step indicator, prominent hero amount, full transaction receipt,
 * balance forecast impact, and clear Back / Confirm & Save action buttons.
 */
@Composable
fun TransactionConfirmationView(
    transactionType: TransactionType,
    amount: Double,
    currencySymbol: String,
    isBn: Boolean,
    accountName: String?,
    targetAccountName: String? = null,
    category: Category? = null,
    dateTimestamp: Long,
    note: String,
    receiptUriStr: String? = null,
    fee: Double = 0.0,
    exchangeRate: Double = 1.0,
    currentBalance: Double,
    resultingBalance: Double,
    targetCurrentBalance: Double? = null,
    resultingTargetBalance: Double? = null,
    onBackClick: () -> Unit,
    onConfirmSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = when (transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    val typeLabel = when (transactionType) {
        TransactionType.EXPENSE -> Localization.getString(Localization.Key.EXPENSE_SHORT, isBn)
        TransactionType.INCOME -> Localization.getString(Localization.Key.INCOME_SHORT, isBn)
        TransactionType.TRANSFER -> Localization.getString(Localization.Key.TRANSFER_SHORT, isBn)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_confirmation_step_view")
    ) {
        // Step 2 Progress Header & Type Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString(Localization.Key.STEP_2_PROGRESS, isBn) + " • " + Localization.getString(Localization.Key.CONFIRM_TRANSACTION, isBn),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = themeColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (transactionType) {
                            TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                            TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                            TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Amount Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = themeColor.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.22f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (transactionType) {
                        TransactionType.EXPENSE -> if (isBn) "মোট খরচ" else "Total Expense"
                        TransactionType.INCOME -> if (isBn) "মোট আয়" else "Total Income"
                        TransactionType.TRANSFER -> if (isBn) "মোট স্থানান্তরিত অর্থ" else "Total Transfer"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = (if (transactionType == TransactionType.EXPENSE) "- " else if (transactionType == TransactionType.INCOME) "+ " else "") +
                            Formatters.formatAmount(amount, currencySymbol, isBn),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColor,
                    textAlign = TextAlign.Center
                )

                if (transactionType == TransactionType.TRANSFER && fee > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (isBn) "+ ফি: " else "+ Fee: ") + Formatters.formatAmount(fee, currencySymbol, isBn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Detailed Receipt Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString(Localization.Key.TRANSACTION_SUMMARY, isBn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                // Account(s)
                if (transactionType == TransactionType.TRANSFER) {
                    SummaryItemRow(
                        label = Localization.getString(Localization.Key.FROM_ACCOUNT, isBn),
                        value = accountName ?: (if (isBn) "অ্যাকাউন্ট" else "Account"),
                        icon = Icons.Default.AccountBalanceWallet
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryItemRow(
                        label = Localization.getString(Localization.Key.TO_ACCOUNT, isBn),
                        value = targetAccountName ?: (if (isBn) "গন্তব্য অ্যাকাউন্ট" else "Target Account"),
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        iconTint = TransferBlue
                    )
                } else {
                    SummaryItemRow(
                        label = Localization.getString(Localization.Key.ACCOUNT, isBn),
                        value = accountName ?: (if (isBn) "অ্যাকাউন্ট" else "Account"),
                        icon = Icons.Default.AccountBalanceWallet
                    )
                }

                // Category (for Expense & Income)
                if (transactionType != TransactionType.TRANSFER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.getString(Localization.Key.CATEGORY, isBn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (category != null) {
                                CategoryIconBadge(
                                    iconName = category.iconName,
                                    colorHex = category.colorHex,
                                    size = 22.dp,
                                    iconSize = 13.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.localizedName(isBn),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = if (isBn) "নির্দিষ্ট নয়" else "Uncategorized",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Date & Time
                Spacer(modifier = Modifier.height(8.dp))
                SummaryItemRow(
                    label = Localization.getString(Localization.Key.DATE, isBn),
                    value = Formatters.formatDateTime(dateTimestamp, isBn),
                    icon = Icons.Default.CalendarToday
                )

                // Note (if any)
                if (note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryItemRow(
                        label = Localization.getString(Localization.Key.NOTE, isBn),
                        value = note,
                        icon = Icons.AutoMirrored.Filled.Notes
                    )
                }

                // Exchange Rate (if transfer & != 1.0)
                if (transactionType == TransactionType.TRANSFER && exchangeRate != 1.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryItemRow(
                        label = Localization.getString(Localization.Key.EXCHANGE_RATE, isBn),
                        value = exchangeRate.toString(),
                        icon = Icons.Default.Info
                    )
                }

                // Receipt thumbnail (if attached)
                if (receiptUriStr != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.getString(Localization.Key.ATTACHED_RECEIPT, isBn),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = receiptUriStr,
                                contentDescription = "Receipt Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "সংযুক্ত আছে" else "Attached",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Balance Preview Forecast Card
        TransactionBalancePreviewCard(
            transactionType = transactionType,
            currentBalance = currentBalance,
            resultingBalance = resultingBalance,
            targetCurrentBalance = targetCurrentBalance,
            resultingTargetBalance = resultingTargetBalance,
            currencySymbol = currencySymbol,
            isBn = isBn,
            accountName = accountName,
            targetAccountName = targetAccountName,
            amount = amount
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Helpful confirmation guidance notice
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString(Localization.Key.CONFIRMATION_NOTICE, isBn),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom Action Buttons: Back (to step 1) and Confirm & Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("preview_back_step_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString(Localization.Key.PREVIOUS_STEP, isBn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
            }

            Button(
                onClick = onConfirmSaveClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("preview_confirm_save_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.getString(Localization.Key.CONFIRM_AND_SAVE, isBn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SummaryItemRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
