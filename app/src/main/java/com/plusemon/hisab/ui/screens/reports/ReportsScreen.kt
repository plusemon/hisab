package com.plusemon.hisab.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionWithDetails
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DetailTopAppBar
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.theme.AmberTertiary
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = settings.numeralSystem == "bn"
    val hideBalances = settings.hideBalances
    val currSymbol = settings.currencySymbol
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()

    val allTransactions by viewModel.transactions.collectAsState()

    var selectedPeriod by remember { mutableStateOf("THIS_MONTH") }
    var drilldownCategory by remember { mutableStateOf<Category?>(null) }

    val (startDate, endDate) = remember(selectedPeriod) {
        Formatters.getStartAndEndForPeriod(selectedPeriod)
    }

    val report = remember(allTransactions, startDate, endDate) {
        viewModel.computePeriodReport(startDate, endDate)
    }

    // Drilldown Dialog
    if (drilldownCategory != null) {
        val catTransactions = remember(allTransactions, drilldownCategory, startDate, endDate) {
            allTransactions.filter {
                it.category?.id == drilldownCategory!!.id &&
                        it.transaction.dateTimestamp in startDate..endDate
            }
        }

        CategoryDrilldownDialog(
            category = drilldownCategory!!,
            transactions = catTransactions,
            isBangla = isBn,
            useBnDigits = useBnDigits,
            hideBalances = hideBalances,
            currencySymbol = currSymbol,
            onDismiss = { drilldownCategory = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onNavigateBack != null) {
                DetailTopAppBar(
                    title = Localization.getString(Localization.Key.REPORTS, isBn),
                    onNavigateBack = onNavigateBack,
                    isNetworkConnected = isNetworkConnected
                )
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            if (onNavigateBack == null) {
                // Screen Header Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.getString(Localization.Key.REPORTS, isBn),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Period Selector Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf(
                    Pair("THIS_MONTH", if (isBn) "এই মাস" else "This Month"),
                    Pair("LAST_MONTH", if (isBn) "গত মাস" else "Last Month"),
                    Pair("THIS_YEAR", if (isBn) "এই বছর" else "This Year"),
                    Pair("ALL", if (isBn) "সব সময়" else "All Time")
                )

                items(periods) { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = { selectedPeriod = key },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            // Overview 4-Metric Grid (Flat Card Design System)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Income Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.getString(Localization.Key.INCOME_THIS_MONTH, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            CurrencyAmountText(
                                amount = report.totalIncome,
                                currencySymbol = currSymbol,
                                useBanglaDigits = useBnDigits,
                                hideBalances = hideBalances,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }

                    // Total Expense Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.getString(Localization.Key.EXPENSES_THIS_MONTH, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            CurrencyAmountText(
                                amount = report.totalExpense,
                                currencySymbol = currSymbol,
                                useBanglaDigits = useBnDigits,
                                hideBalances = hideBalances,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Net Savings Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.getString(Localization.Key.NET_SAVINGS, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            CurrencyAmountText(
                                amount = report.netSavings,
                                currencySymbol = currSymbol,
                                useBanglaDigits = useBnDigits,
                                hideBalances = hideBalances,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (report.netSavings >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
                            )
                        }
                    }

                    // Savings Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AmberTertiary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.getString(Localization.Key.SAVINGS_RATE, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val rateStr = "${report.savingsRate.toInt()}%"
                            Text(
                                text = if (useBnDigits) Formatters.toBanglaDigits(rateStr) else rateStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Expense Breakdown Chart Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
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
                        text = if (isBn) "ক্যাটাগরি অনুযায়ী ব্যয়ের বিবরণী" else "Category Expense Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (report.categoryBreakdown.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.PieChart,
                            title = if (isBn) "কোনো ব্যয়ের তথ্য নেই" else "No expense data for this period",
                            description = if (isBn) "নির্বাচিত সময়ে কোনো খরচ রেকর্ড করা হয়নি।" else "There are no expenses recorded in the selected time range."
                        )
                    } else {
                        // Donut Chart Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                items = report.categoryBreakdown,
                                totalExpense = report.totalExpense
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBn) "মোট ব্যয়" else "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                CurrencyAmountText(
                                    amount = report.totalExpense,
                                    currencySymbol = currSymbol,
                                    useBanglaDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category Percentage Breakdown Bars
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            report.categoryBreakdown.forEach { (cat, amount) ->
                                val percentage = if (report.totalExpense > 0) (amount / report.totalExpense).toFloat() else 0f
                                val catColor = parseColorHex(cat.colorHex)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { drilldownCategory = cat }
                                        .padding(vertical = 4.dp),
                                    color = Color.Transparent
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryIconBadge(
                                                iconName = cat.iconName,
                                                colorHex = cat.colorHex,
                                                size = 32.dp,
                                                iconSize = 16.dp
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Text(
                                                text = cat.localizedName(isBn),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )

                                            CurrencyAmountText(
                                                amount = amount,
                                                currencySymbol = currSymbol,
                                                useBanglaDigits = useBnDigits,
                                                hideBalances = hideBalances,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            val percentStr = "${(percentage * 100).toInt()}%"
                                            Text(
                                                text = if (useBnDigits) Formatters.toBanglaDigits(percentStr) else percentStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { percentage },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = catColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Spending Trend Bar Chart (if data available)
            if (report.dailyBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
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
                            text = if (isBn) "দৈনিক ব্যয়ের গতিপ্রকৃতি" else "Daily Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DailySpendingBarChart(
                            dailyData = report.dailyBreakdown.takeLast(10),
                            currSymbol = currSymbol,
                            useBnDigits = useBnDigits
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    items: List<Pair<Category, Double>>,
    totalExpense: Double
) {
    Canvas(modifier = Modifier.size(150.dp)) {
        if (totalExpense <= 0) return@Canvas

        var startAngle = -90f
        val strokeWidth = 24.dp.toPx()

        items.forEach { (cat, amount) ->
            val sweep = ((amount / totalExpense) * 360f).toFloat()
            val color = parseColorHex(cat.colorHex)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )

            startAngle += sweep
        }
    }
}

@Composable
fun DailySpendingBarChart(
    dailyData: List<Pair<String, Double>>,
    currSymbol: String,
    useBnDigits: Boolean
) {
    val maxSpend = dailyData.maxOfOrNull { it.second } ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        dailyData.forEach { (dateStr, spend) ->
            val heightFraction = if (maxSpend > 0) (spend / maxSpend).toFloat().coerceIn(0.08f, 1f) else 0.08f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height((100 * heightFraction).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(ExpenseRed)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val shortDate = dateStr.split(" ").take(2).joinToString(" ")
                Text(
                    text = shortDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CategoryDrilldownDialog(
    category: Category,
    transactions: List<TransactionWithDetails>,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconBadge(
                            iconName = category.iconName,
                            colorHex = category.colorHex,
                            size = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = category.localizedName(isBangla),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (transactions.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.BarChart,
                        title = Localization.getString(Localization.Key.EMPTY_TRANSACTIONS, isBangla),
                        description = if (isBangla) "এই ক্যাটাগরিতে কোনো লেনদেন নেই।" else "No transactions recorded for this category."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions, key = { it.transaction.id }) { txItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (txItem.transaction.note.isNotBlank()) txItem.transaction.note else txItem.account.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = Formatters.formatDate(txItem.transaction.dateTimestamp, isBangla),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                CurrencyAmountText(
                                    amount = txItem.transaction.amount,
                                    currencySymbol = currencySymbol,
                                    useBanglaDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
