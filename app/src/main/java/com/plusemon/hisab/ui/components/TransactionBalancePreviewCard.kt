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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.theme.TransferBlue

/**
 * Modern, professional fintech Balance Preview Card for transactions and transfers.
 * Displays clean before-and-after flow, directional impact badge, account context,
 * and clear warning indicators if a deficit occurs.
 */
@Composable
fun TransactionBalancePreviewCard(
    transactionType: TransactionType,
    currentBalance: Double,
    resultingBalance: Double,
    targetCurrentBalance: Double? = null,
    resultingTargetBalance: Double? = null,
    currencySymbol: String,
    isBn: Boolean,
    accountName: String? = null,
    targetAccountName: String? = null,
    amount: Double = 0.0
) {
    val isSourceNegative = resultingBalance < 0.0
    val isTargetNegative = targetCurrentBalance != null && resultingTargetBalance != null && resultingTargetBalance < 0.0
    val isAnyNegative = isSourceNegative || isTargetNegative

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAnyNegative) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isAnyNegative) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Icon + Title + Impact Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val (headerIcon, headerTint) = when (transactionType) {
                        TransactionType.EXPENSE -> Icons.Default.ArrowDownward to ExpenseRed
                        TransactionType.INCOME -> Icons.Default.ArrowUpward to IncomeGreen
                        TransactionType.TRANSFER -> Icons.Default.SwapHoriz to TransferBlue
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(headerTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = headerTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = if (isBn) "ব্যালেন্স পূর্বাভাস" else "Balance Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!accountName.isNullOrBlank() && transactionType != TransactionType.TRANSFER) {
                            Text(
                                text = accountName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Status or Impact Badge on top right
                if (isAnyNegative) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBn) "ঘাটতি ব্যালেন্স" else "Deficit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else if (amount > 0.0) {
                    val badgeColor = when (transactionType) {
                        TransactionType.EXPENSE -> ExpenseRed
                        TransactionType.INCOME -> IncomeGreen
                        TransactionType.TRANSFER -> TransferBlue
                    }
                    val prefix = when (transactionType) {
                        TransactionType.EXPENSE -> "- "
                        TransactionType.INCOME -> "+ "
                        TransactionType.TRANSFER -> "↔ "
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = prefix + Formatters.formatAmount(amount, currencySymbol, isBn),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transactionType == TransactionType.TRANSFER && targetCurrentBalance != null && resultingTargetBalance != null) {
                // TRANSFER MODE: Two structured rows for Source and Destination
                TransferAccountRow(
                    label = if (isBn) "উৎস অ্যাকাউন্ট" else "Source Account",
                    accountName = accountName,
                    currentBalance = currentBalance,
                    resultingBalance = resultingBalance,
                    isOutflow = true,
                    currencySymbol = currencySymbol,
                    isBn = isBn,
                    isNegative = isSourceNegative
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                TransferAccountRow(
                    label = if (isBn) "প্রাপক অ্যাকাউন্ট" else "Target Account",
                    accountName = targetAccountName,
                    currentBalance = targetCurrentBalance,
                    resultingBalance = resultingTargetBalance,
                    isOutflow = false,
                    currencySymbol = currencySymbol,
                    isBn = isBn,
                    isNegative = isTargetNegative
                )
            } else {
                // EXPENSE / INCOME MODE: Modern Before -> Connector -> After Flow Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Before (Current Balance)
                    BalanceMetricBox(
                        modifier = Modifier.weight(1f),
                        label = if (isBn) "বর্তমান ব্যালেন্স" else "Current Balance",
                        amount = currentBalance,
                        currencySymbol = currencySymbol,
                        isBn = isBn,
                        highlightColor = MaterialTheme.colorScheme.onSurface
                    )

                    // Connector Arrow
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isSourceNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // After (New Balance)
                    val resultColor = when {
                        isSourceNegative -> MaterialTheme.colorScheme.error
                        transactionType == TransactionType.INCOME && amount > 0 -> IncomeGreen
                        else -> MaterialTheme.colorScheme.primary
                    }

                    BalanceMetricBox(
                        modifier = Modifier.weight(1f),
                        label = if (isBn) "পরবর্তী ব্যালেন্স" else "New Balance",
                        amount = resultingBalance,
                        currencySymbol = currencySymbol,
                        isBn = isBn,
                        highlightColor = resultColor,
                        isResult = true,
                        isNegative = isSourceNegative
                    )
                }
            }

            // Deficit Warning Alert strip
            if (isAnyNegative) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "সতর্কতা: লেনদেনের পর অ্যাকাউন্টের ব্যালেন্স ঋণাত্মক হবে!"
                            else "Warning: Transaction will result in negative balance!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    amount: Double,
    currencySymbol: String,
    isBn: Boolean,
    highlightColor: Color,
    isResult: Boolean = false,
    isNegative: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isNegative) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else if (isResult) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            if (isNegative) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            else if (isResult) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Formatters.formatAmount(amount, currencySymbol, isBn),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isResult) FontWeight.Bold else FontWeight.SemiBold,
                color = highlightColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransferAccountRow(
    label: String,
    accountName: String?,
    currentBalance: Double,
    resultingBalance: Double,
    isOutflow: Boolean,
    currencySymbol: String,
    isBn: Boolean,
    isNegative: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!accountName.isNullOrBlank()) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Formatters.formatAmount(currentBalance, currencySymbol, isBn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (isOutflow) ExpenseRed else IncomeGreen,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(13.dp)
            )
            Text(
                text = Formatters.formatAmount(resultingBalance, currencySymbol, isBn),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isNegative) MaterialTheme.colorScheme.error
                else if (!isOutflow) IncomeGreen
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
