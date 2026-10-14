package com.plusemon.hisab.ui.screens.categories

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.hisab.data.model.Category
import com.plusemon.hisab.data.model.TransactionType
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CategoryIconBadge
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.DetailTopAppBar
import com.plusemon.hisab.ui.components.getIconByName
import com.plusemon.hisab.ui.components.parseColorHex
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: HisabViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val allCategories by viewModel.categories.collectAsState()
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    val currentType = if (pagerState.currentPage == 0) TransactionType.EXPENSE else TransactionType.INCOME

    val expenseCategories = remember(allCategories) {
        allCategories.filter { it.type == TransactionType.EXPENSE }
    }
    val incomeCategories = remember(allCategories) {
        allCategories.filter { it.type == TransactionType.INCOME }
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DetailTopAppBar(
                title = if (isBn) "ক্যাটাগরি ব্যবস্থাপনা" else "Manage Categories",
                onNavigateBack = onNavigateBack,
                isNetworkConnected = isNetworkConnected,
                actions = {
                    IconButton(
                        onClick = {
                            categoryToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("topbar_add_category_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isBn) "নতুন ক্যাটাগরি" else "Add Category",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row (Expense vs Income) with swipeable HorizontalPager
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = if (pagerState.currentPage == 0) ExpenseRed else IncomeGreen
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
                            text = if (isBn) "খরচের ক্যাটাগরি" else "Expenses",
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_expense_categories")
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
                            text = if (isBn) "আয়ের ক্যাটাগরি" else "Income",
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (pagerState.currentPage == 1) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_income_categories")
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val categoriesForPage = if (page == 0) expenseCategories else incomeCategories

                if (categoriesForPage.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBn) "কোন ক্যাটাগরি পাওয়া যায়নি" else "No categories found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categoriesForPage, key = { it.id }) { cat ->
                            CategoryListItem(
                                category = cat,
                                isBangla = isBn,
                                onEdit = {
                                    categoryToEdit = cat
                                    showAddEditDialog = true
                                },
                                onDelete = {
                                    categoryToDelete = cat
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Bottom Sheet
    if (showAddEditDialog) {
        CategoryEditBottomSheet(
            category = categoryToEdit,
            defaultType = currentType,
            isBangla = isBn,
            onDismiss = {
                showAddEditDialog = false
                categoryToEdit = null
            },
            onSave = { nameBn, nameEn, type, iconName, colorHex ->
                if (categoryToEdit != null) {
                    viewModel.updateCategory(
                        id = categoryToEdit!!.id,
                        nameBn = nameBn,
                        nameEn = nameEn,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                } else {
                    viewModel.addCategory(
                        nameBn = nameBn,
                        nameEn = nameEn,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                }
                showAddEditDialog = false
                categoryToEdit = null
            }
        )
    }

    // Delete Confirmation
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        DeleteConfirmationDialog(
            title = if (isBn) "ক্যাটাগরি মুছে ফেলতে চান?" else "Delete Category?",
            message = if (isBn)
                "আপনি কি নিশ্চিত যে '${if (isBn) cat.nameBn else cat.nameEn}' ক্যাটাগরি মুছে ফেলতে চান?"
            else
                "Are you sure you want to delete '${cat.nameEn}'?",
            itemDetail = if (isBn) cat.nameBn else cat.nameEn,
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteCategory(cat.id)
                categoryToDelete = null
            },
            onDismiss = {
                categoryToDelete = null
            }
        )
    }
}

@Composable
private fun CategoryListItem(
    category: Category,
    isBangla: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconName = category.iconName,
                colorHex = category.colorHex,
                size = 44.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBangla) category.nameBn else category.nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBangla) category.nameEn else category.nameBn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("edit_category_${category.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isBangla) "সম্পাদনা" else "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_category_${category.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = if (isBangla) "মুছুন" else "Delete",
                    tint = ExpenseRed,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditBottomSheet(
    category: Category?,
    defaultType: TransactionType,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (nameBn: String, nameEn: String, type: TransactionType, iconName: String, colorHex: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameBn by remember { mutableStateOf(category?.nameBn ?: "") }
    var nameEn by remember { mutableStateOf(category?.nameEn ?: "") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "shopping_cart") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#0F766E") }

    val iconsList = listOf(
        "shopping_cart", "restaurant", "directions_bus", "receipt_long",
        "shopping_bag", "medical_services", "movie", "school",
        "payments", "store", "laptop", "trending_up",
        "card_giftcard", "account_balance", "account_balance_wallet", "savings"
    )

    val colorsList = listOf(
        "#0F766E", "#2563EB", "#7C3AED", "#DB2777",
        "#EA580C", "#D97706", "#16A34A", "#0891B2",
        "#4F46E5", "#DC2626", "#475569", "#059669"
    )

    val isEditing = category != null
    val targetType = category?.type ?: defaultType

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) {
                        if (isBangla) "ক্যাটাগরি সম্পাদনা" else "Edit Category"
                    } else {
                        if (isBangla) "নতুন ক্যাটাগরি তৈরি" else "New Category"
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

            // Name EN
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(if (isBangla) "ইংরেজি নাম (যেমন: Groceries)" else "English Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_name_en_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name BN
            OutlinedTextField(
                value = nameBn,
                onValueChange = { nameBn = it },
                label = { Text(if (isBangla) "বাংলা নাম (যেমন: কাঁচাবাজার)" else "Bangla Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_name_bn_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Color Selector
            Text(
                text = if (isBangla) "রং নির্বাচন করুন" else "Select Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 6.dp, end = 16.dp, bottom = 4.dp)
            ) {
                items(colorsList) { hex ->
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(parseColorHex(hex))
                            .clickable { selectedColor = hex }
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Icon Selector
            Text(
                text = if (isBangla) "আইকন নির্বাচন করুন" else "Select Icon",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 6.dp, end = 16.dp, bottom = 4.dp)
            ) {
                items(iconsList) { iconKey ->
                    val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) parseColorHex(selectedColor).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) parseColorHex(selectedColor) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedIcon = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconKey),
                            contentDescription = iconKey,
                            tint = if (isSelected) parseColorHex(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }

                Spacer(modifier = Modifier.width(10.dp))

                val isValid = nameEn.isNotBlank() || nameBn.isNotBlank()
                Button(
                    onClick = {
                        val finalEn = nameEn.trim().ifEmpty { nameBn.trim() }
                        val finalBn = nameBn.trim().ifEmpty { nameEn.trim() }
                        if (finalEn.isNotBlank()) {
                            onSave(finalBn, finalEn, targetType, selectedIcon, selectedColor)
                        }
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_category_button")
                ) {
                    Text(if (isBangla) "সংরক্ষণ" else "Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
