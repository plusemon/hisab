package com.plusemon.hisab.ui.screens.debts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import com.plusemon.hisab.domain.util.ContactPhoneOption
import com.plusemon.hisab.domain.util.ContactUtils
import com.plusemon.hisab.domain.util.SelectContactPhoneDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.plusemon.hisab.data.model.Contact
import com.plusemon.hisab.data.model.DebtType
import com.plusemon.hisab.data.model.LoanDebt
import com.plusemon.hisab.data.model.ShopCreditPayment
import com.plusemon.hisab.data.model.ShopCreditPurchase
import com.plusemon.hisab.data.model.Vendor
import com.plusemon.hisab.domain.util.Formatters
import com.plusemon.hisab.domain.util.Localization
import com.plusemon.hisab.ui.components.CurrencyAmountText
import com.plusemon.hisab.ui.components.DeleteConfirmationDialog
import com.plusemon.hisab.ui.components.DetailTopAppBar
import com.plusemon.hisab.ui.components.EmptyStateView
import com.plusemon.hisab.ui.theme.ExpenseRed
import com.plusemon.hisab.ui.theme.IncomeGreen
import com.plusemon.hisab.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: HisabViewModel,
    onNavigateBack: () -> Unit,
    onDetailStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isBn = settings.language == "bn"
    val useBnDigits = settings.numeralSystem == "bn"
    val hideBalances = settings.hideBalances
    val currSymbol = settings.currencySymbol
    val context = LocalContext.current
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()

    val debtPagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    val allDebts by viewModel.loansDebts.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val vendors by viewModel.vendors.collectAsState()
    val shopCreditPurchases by viewModel.shopCreditPurchases.collectAsState()
    val shopCreditPayments by viewModel.shopCreditPayments.collectAsState()
    val accounts by viewModel.accountsWithBalances.collectAsState()

    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var selectedVendorId by remember { mutableStateOf<Long?>(null) }

    androidx.compose.runtime.LaunchedEffect(selectedContactId, selectedVendorId) {
        onDetailStateChanged(selectedContactId != null || selectedVendorId != null)
    }

    var showAddLoanBottomSheet by remember { mutableStateOf(false) }
    var recordingPaymentDebt by remember { mutableStateOf<LoanDebt?>(null) }

    var showAddCreditBottomSheet by remember { mutableStateOf(false) }
    var showAddVendorBottomSheet by remember { mutableStateOf(false) }
    var settlingVendor by remember { mutableStateOf<Vendor?>(null) }
    var debtToDelete by remember { mutableStateOf<LoanDebt?>(null) }
    var editingContactPhone by remember { mutableStateOf<Contact?>(null) }
    var editingVendorPhone by remember { mutableStateOf<Vendor?>(null) }

    if (editingContactPhone != null) {
        EditContactPhoneDialog(
            contact = editingContactPhone!!,
            isBangla = isBn,
            onDismiss = { editingContactPhone = null },
            onSave = { newPhone ->
                viewModel.updateContact(editingContactPhone!!.copy(phone = newPhone))
                editingContactPhone = null
            }
        )
    }

    if (editingVendorPhone != null) {
        EditVendorPhoneDialog(
            vendor = editingVendorPhone!!,
            isBangla = isBn,
            onDismiss = { editingVendorPhone = null },
            onSave = { newPhone ->
                viewModel.updateVendorPhone(editingVendorPhone!!.id, newPhone)
                editingVendorPhone = null
            }
        )
    }

    if (debtToDelete != null) {
        val target = debtToDelete!!
        val amountStr = Formatters.formatAmount(target.remainingAmount, currSymbol, useBnDigits)
        val personOrNote = target.personName + (if (target.note.isNotBlank()) " • " + target.note else "")
        val typeLabel = if (target.type == DebtType.OWED_TO_ME) (if (isBn) "পাওনা" else "Receivable") else (if (isBn) "দেনা" else "Payable")
        DeleteConfirmationDialog(
            title = if (isBn) "ধার/দেনার হিসাব মুছে ফেলবেন?" else "Delete Loan/Debt Entry?",
            message = if (isBn)
                "আপনি কি নিশ্চিত যে এই ধার/দেনার এন্ট্রিটি মুছে ফেলতে চান?"
            else
                "Are you sure you want to delete this loan/debt record?",
            itemDetail = "$personOrNote • $typeLabel: $amountStr",
            isBangla = isBn,
            onConfirm = {
                viewModel.deleteLoanDebt(target.id)
                debtToDelete = null
            },
            onDismiss = {
                debtToDelete = null
            }
        )
    }

    val totalOwedToMe = allDebts.filter { it.type == DebtType.OWED_TO_ME && !it.isSettled }.sumOf { it.remainingAmount }
    val totalIOwe = allDebts.filter { it.type == DebtType.I_OWE && !it.isSettled }.sumOf { it.remainingAmount }
    val totalShopCreditOwed = shopCreditPurchases.filter { !it.isSettled }.sumOf { it.remainingAmount }

    val activeContact = contacts.find { it.id == selectedContactId }
    val activeVendor = vendors.find { it.id == selectedVendorId }

    if (showAddLoanBottomSheet) {
        AddLoanEntryBottomSheet(
            contacts = contacts,
            accounts = accounts,
            initialContactId = selectedContactId,
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { showAddLoanBottomSheet = false },
            onSave = { contactId, personName, accountId, amount, type, dueDate, note, phone ->
                viewModel.addLoanDebt(contactId ?: 0L, personName, accountId, amount, type, dueDate, note, phone)
                showAddLoanBottomSheet = false
            }
        )
    }

    if (recordingPaymentDebt != null) {
        RecordPaymentBottomSheet(
            debt = recordingPaymentDebt!!,
            accounts = accounts,
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { recordingPaymentDebt = null },
            onConfirm = { accountId, paymentAmount, note ->
                viewModel.recordDebtPayment(recordingPaymentDebt!!.id, accountId, paymentAmount, note)
                recordingPaymentDebt = null
            }
        )
    }

    if (showAddVendorBottomSheet) {
        AddNewVendorBottomSheet(
            isBangla = isBn,
            onDismiss = { showAddVendorBottomSheet = false },
            onSave = { name, phone, locationNote, tag ->
                viewModel.addVendor(name, phone, locationNote, tag)
                showAddVendorBottomSheet = false
            }
        )
    }

    if (showAddCreditBottomSheet) {
        AddShopCreditBottomSheet(
            vendors = vendors,
            purchases = shopCreditPurchases,
            initialVendorId = selectedVendorId,
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { showAddCreditBottomSheet = false },
            onSave = { vendorId, vendorName, amount, dueDate, note, phone, locationNote, tag ->
                viewModel.addShopCreditPurchase(vendorId ?: 0L, vendorName, 1L, amount, dueDate, note, phone, locationNote, tag)
                showAddCreditBottomSheet = false
            },
            onAddNewVendor = { name, phone, locationNote, tag, onCreated ->
                viewModel.addVendor(name, phone, locationNote, tag) { newVendor ->
                    onCreated(newVendor)
                }
            }
        )
    }

    if (settlingVendor != null) {
        val vPurchases = shopCreditPurchases.filter { it.vendorId == settlingVendor!!.id }
        val vRemaining = vPurchases.filter { !it.isSettled }.sumOf { it.remainingAmount }
        SettleShopCreditBottomSheet(
            vendor = settlingVendor!!,
            defaultAmount = vRemaining,
            accounts = accounts,
            isBangla = isBn,
            currencySymbol = currSymbol,
            onDismiss = { settlingVendor = null },
            onConfirm = { accountId, amount, note ->
                viewModel.settleShopCredit(settlingVendor!!.id, accountId, amount, note)
                settlingVendor = null
            }
        )
    }

    BackHandler(enabled = selectedContactId != null || selectedVendorId != null) {
        selectedContactId = null
        selectedVendorId = null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (selectedContactId != null && activeContact != null) {
                val phoneToCall = activeContact.phone.ifBlank {
                    allDebts.firstOrNull {
                        (it.contactId == activeContact.id || it.personName.equals(activeContact.name, true)) && it.phone.isNotBlank()
                    }?.phone ?: ""
                }
                DetailTopAppBar(
                    title = activeContact.name,
                    onNavigateBack = {
                        selectedContactId = null
                        selectedVendorId = null
                    },
                    isNetworkConnected = isNetworkConnected,
                    actions = {
                        if (phoneToCall.isNotBlank()) {
                            IconButton(
                                onClick = { ContactUtils.dialPhoneNumber(context, phoneToCall, isBn) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("topbar_call_contact_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = if (isBn) "কল করুন" else "Call",
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { editingContactPhone = activeContact },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("edit_contact_phone_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isBn) "নম্বর এডিট করুন" else "Edit Phone",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            } else if (selectedVendorId != null && activeVendor != null) {
                DetailTopAppBar(
                    title = activeVendor.name,
                    onNavigateBack = {
                        selectedContactId = null
                        selectedVendorId = null
                    },
                    isNetworkConnected = isNetworkConnected,
                    actions = {
                        if (activeVendor.phone.isNotBlank()) {
                            IconButton(
                                onClick = { ContactUtils.dialPhoneNumber(context, activeVendor.phone, isBn) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("call_vendor_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = if (isBn) "কল করুন" else "Call",
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { editingVendorPhone = activeVendor },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("edit_vendor_phone_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isBn) "নম্বর এডিট করুন" else "Edit Phone",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val willArchive = !activeVendor.isArchived
                                viewModel.archiveVendor(activeVendor.id, willArchive)
                                if (willArchive) {
                                    selectedVendorId = null
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("archive_vendor_btn")
                        ) {
                            Icon(
                                imageVector = if (activeVendor.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (activeVendor.isArchived) Localization.getString(Localization.Key.UNARCHIVE, isBn) else (if (isBn) "আর্কাইভ করুন" else "Archive"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedContactId == null && selectedVendorId == null) {
                FloatingActionButton(
                    onClick = {
                        if (debtPagerState.currentPage == 0) showAddLoanBottomSheet = true else showAddCreditBottomSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_hub_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedContactId == null && selectedVendorId == null) {
                TabRow(
                    selectedTabIndex = debtPagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    indicator = { tabPositions ->
                        if (debtPagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[debtPagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = debtPagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                debtPagerState.animateScrollToPage(0)
                            }
                        },
                        text = {
                            Text(
                                text = if (isBn) "ধার ও ঋণ" else "Lending & Borrowing",
                                fontWeight = if (debtPagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_lending")
                    )
                    Tab(
                        selected = debtPagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                debtPagerState.animateScrollToPage(1)
                            }
                        },
                        text = {
                            Text(
                                text = if (isBn) "দোকান বাকি" else "Shop Credit",
                                fontWeight = if (debtPagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_shop_credit")
                    )
                }

                HorizontalPager(
                    state = debtPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        DebtsOverviewContent(
                            contacts = contacts,
                            debts = allDebts,
                            totalReceivable = totalOwedToMe,
                            totalPayable = totalIOwe,
                            isBangla = isBn,
                            useBnDigits = useBnDigits,
                            hideBalances = hideBalances,
                            currencySymbol = currSymbol,
                            onContactClick = { contactId -> selectedContactId = contactId },
                            onAddDebtClick = { showAddLoanBottomSheet = true }
                        )
                    } else {
                        ShopCreditOverviewContent(
                            vendors = vendors,
                            purchases = shopCreditPurchases,
                            totalOwed = totalShopCreditOwed,
                            isBangla = isBn,
                            useBnDigits = useBnDigits,
                            hideBalances = hideBalances,
                            currencySymbol = currSymbol,
                            onVendorClick = { vendorId -> selectedVendorId = vendorId },
                            onAddCreditClick = { showAddCreditBottomSheet = true },
                            onAddNewVendorClick = { showAddVendorBottomSheet = true },
                            onArchiveVendor = { vId, archived -> viewModel.archiveVendor(vId, archived) }
                        )
                    }
                }
            } else if (selectedContactId != null && activeContact != null) {
                ContactLedgerContent(
                    contact = activeContact,
                    debts = allDebts.filter { it.contactId == activeContact.id || it.personName.equals(activeContact.name, true) },
                    isBangla = isBn,
                    useBnDigits = useBnDigits,
                    hideBalances = hideBalances,
                    currencySymbol = currSymbol,
                    onRecordPayment = { debt -> recordingPaymentDebt = debt },
                    onDeleteDebt = { debt -> debtToDelete = debt },
                    onAddEntryClick = { showAddLoanBottomSheet = true },
                    onUpdateContactPhone = { updatedContact, newPhone ->
                        viewModel.updateContact(updatedContact.copy(phone = newPhone))
                    }
                )
            } else if (selectedVendorId != null && activeVendor != null) {
                VendorLedgerContent(
                    vendor = activeVendor,
                    purchases = shopCreditPurchases.filter { it.vendorId == activeVendor.id },
                    payments = shopCreditPayments.filter { it.vendorId == activeVendor.id },
                    isBangla = isBn,
                    useBnDigits = useBnDigits,
                    hideBalances = hideBalances,
                    currencySymbol = currSymbol,
                    onSettleClick = { settlingVendor = activeVendor },
                    onAddPurchaseClick = { showAddCreditBottomSheet = true },
                    onArchiveVendor = { vId, archived ->
                        viewModel.archiveVendor(vId, archived)
                        selectedVendorId = null
                    },
                    onEditPhoneClick = { editingVendorPhone = activeVendor }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// LENDING & BORROWING CONTENT (FLAT DESIGN SYSTEM)
// -------------------------------------------------------------
@Composable
fun DebtsOverviewContent(
    contacts: List<Contact>,
    debts: List<LoanDebt>,
    totalReceivable: Double,
    totalPayable: Double,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onContactClick: (Long) -> Unit,
    onAddDebtClick: () -> Unit
) {
    val isBn = isBangla
    val contactSummaries = remember(contacts, debts) {
        contacts.map { contact ->
            val contactDebts = debts.filter { it.contactId == contact.id || it.personName.equals(contact.name, true) }
            val owedToMe = contactDebts.filter { it.type == DebtType.OWED_TO_ME && !it.isSettled }.sumOf { it.remainingAmount }
            val iOwe = contactDebts.filter { it.type == DebtType.I_OWE && !it.isSettled }.sumOf { it.remainingAmount }
            val net = owedToMe - iOwe
            Triple(contact, net, contactDebts)
        }.sortedByDescending { kotlin.math.abs(it.second) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Flat cards for Receivable & Payable (No soft pastel fills)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBn) "মোট পাওনা (সবাই দেবে)" else "Total Receivable",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CurrencyAmountText(
                            amount = totalReceivable,
                            currencySymbol = currencySymbol,
                            useBanglaDigits = useBnDigits,
                            hideBalances = hideBalances,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBn) "মোট দেনা (আমি দেব)" else "Total Payable",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CurrencyAmountText(
                            amount = totalPayable,
                            currencySymbol = currencySymbol,
                            useBanglaDigits = useBnDigits,
                            hideBalances = hideBalances,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "কন্টাক্টসমূহ (লেনদেন অনুসারে)" else "Contacts Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (contactSummaries.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = Localization.getString(Localization.Key.EMPTY_DEBTS, isBn),
                    description = if (isBn) "কারো কাছে টাকা পাওনা বা দেনা থাকলে এখানে হিসাব রাখতে পারেন।" else "Track money owed to you or money you owe to others here.",
                    actionLabel = if (isBn) "+ নতুন ধার যোগ করুন" else "+ Record Loan/Debt",
                    onActionClick = onAddDebtClick
                )
            }
        } else {
            items(contactSummaries, key = { it.first.id }) { (contact, net, contactDebts) ->
                    val isReceivable = net > 0
                    val isPayable = net < 0
                    val badgeColor = when {
                        isReceivable -> IncomeGreen
                        isPayable -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactClick(contact.id) },
                        shape = RoundedCornerShape(12.dp),
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (contact.phone.isNotBlank()) {
                                    Text(
                                        text = contact.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "${contactDebts.size} ${if (isBn) "টি এন্ট্রি" else "entries"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (contact.phone.isNotBlank()) {
                                val context = LocalContext.current
                                IconButton(
                                    onClick = { ContactUtils.dialPhoneNumber(context, contact.phone, isBn) },
                                    modifier = Modifier.size(36.dp).testTag("quick_call_${contact.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = if (isBn) "কল করুন" else "Call",
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                CurrencyAmountText(
                                    amount = kotlin.math.abs(net),
                                    currencySymbol = currencySymbol,
                                    useBanglaDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                                Text(
                                    text = when {
                                        isReceivable -> if (isBn) "পাবেন" else "Receivable"
                                        isPayable -> if (isBn) "দেবেন" else "Payable"
                                        else -> if (isBn) "পরিশোধিত" else "Settled"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun EditContactPhoneDialog(
    contact: Contact,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf(contact.phone) }
    var phoneOptionsForSelection by remember { mutableStateOf<List<ContactPhoneOption>?>(null) }
    var pendingContactName by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ContactUtils.extractContact(context, uri)
            if (result != null) {
                when {
                    result.phones.isEmpty() -> {
                        Toast.makeText(
                            context,
                            if (isBangla) "এই কন্টাক্টে কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone numbers found in this contact",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    result.phones.size == 1 -> {
                        phone = result.phones.first().rawNumber
                    }
                    else -> {
                        pendingContactName = result.name
                        phoneOptionsForSelection = result.phones
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        contactPickerLauncher.launch(null)
    }

    val onPickContact = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contactPickerLauncher.launch(null)
        }
    }

    if (phoneOptionsForSelection != null) {
        SelectContactPhoneDialog(
            contactName = pendingContactName,
            options = phoneOptionsForSelection!!,
            isBangla = isBangla,
            onSelectPhone = { selected ->
                phone = selected.rawNumber
                phoneOptionsForSelection = null
            },
            onDismiss = {
                phoneOptionsForSelection = null
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = if (isBangla) "ফোন নম্বর পরিবর্তন / যুক্ত করুন" else "Update Phone Number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onPickContact,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBangla) "ফোনের কন্টাক্ট থেকে চুজ করুন" else "Pick from Phone Contacts")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isBangla) "মোবাইল নম্বর" else "Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(phone.trim())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBangla) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ContactLedgerContent(
    contact: Contact,
    debts: List<LoanDebt>,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onRecordPayment: (LoanDebt) -> Unit,
    onDeleteDebt: (LoanDebt) -> Unit,
    onAddEntryClick: () -> Unit,
    onUpdateContactPhone: ((Contact, String) -> Unit)? = null
) {
    val isBn = isBangla
    val context = LocalContext.current
    var showEditPhoneDialog by remember { mutableStateOf(false) }

    val phoneToCall = contact.phone.ifBlank { debts.firstOrNull { it.phone.isNotBlank() }?.phone ?: "" }

    if (showEditPhoneDialog) {
        EditContactPhoneDialog(
            contact = contact,
            isBangla = isBn,
            onDismiss = { showEditPhoneDialog = false },
            onSave = { newPhone ->
                onUpdateContactPhone?.invoke(contact, newPhone)
                showEditPhoneDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
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
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (phoneToCall.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { ContactUtils.dialPhoneNumber(context, phoneToCall, isBn) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = IncomeGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phoneToCall,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IncomeGreen
                                )
                            }
                        } else {
                            Text(
                                text = if (isBn) "কোনো ফোন নম্বর যুক্ত নেই" else "No phone number linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons on Details page: Balanced styling between Call/Add Phone and Add Entry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (phoneToCall.isNotBlank()) {
                        Button(
                            onClick = { ContactUtils.dialPhoneNumber(context, phoneToCall, isBn) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncomeGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("contact_details_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = if (isBn) "কল করুন" else "Call",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "কল করুন" else "Call",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (onUpdateContactPhone != null) {
                        OutlinedButton(
                            onClick = { showEditPhoneDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("contact_details_add_phone_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "নম্বর যুক্ত করুন" else "Add Phone",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = onAddEntryClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("contact_details_add_entry_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "এন্ট্রি যোগ করুন" else "Add Entry",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "লেনদেনের ইতিহাস ও বাকি" else "Entries History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (debts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = Localization.getString(Localization.Key.EMPTY_DEBTS, isBn),
                    description = if (isBn) "এই কন্টাক্টের কোনো হিসাব পাওয়া যায়নি।" else "No debt or loan records found for this contact.",
                    actionLabel = if (isBn) "+ এন্ট্রি যোগ করুন" else "+ Add Entry",
                    onActionClick = onAddEntryClick
                )
            }
        } else {
            items(debts, key = { it.id }) { debt ->
                LoanDebtItemCard(
                    debt = debt,
                    isBangla = isBn,
                    useBnDigits = useBnDigits,
                    hideBalances = hideBalances,
                    currencySymbol = currencySymbol,
                    onRecordPayment = { onRecordPayment(debt) },
                    onDelete = { onDeleteDebt(debt) }
                )
            }
        }
    }
}

@Composable
fun LoanDebtItemCard(
    debt: LoanDebt,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val isBn = isBangla
    val isOwedToMe = debt.type == DebtType.OWED_TO_ME
    val mainColor = if (isOwedToMe) IncomeGreen else ExpenseRed
    val progress = if (debt.amount > 0) (debt.paidAmount / debt.amount).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeContainer = if (isOwedToMe) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                val badgeText = if (isOwedToMe) Color(0xFF15803D) else Color(0xFFB91C1C)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeContainer,
                    border = BorderStroke(1.dp, badgeText.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (isOwedToMe) (if (isBn) "দিলাম (পাওনা)" else "Gave Money") else (if (isBn) "নিলাম (দেনা)" else "Took Money"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (debt.isSettled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = Localization.getString(Localization.Key.SETTLE, isBn),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isBn) "মুছুন" else "Delete",
                            tint = ExpenseRed.copy(alpha = 0.90f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isBn) "বাকি পরিমাণ" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmountText(
                        amount = debt.remainingAmount,
                        currencySymbol = currencySymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = mainColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBn) "মূল পরিমাণ" else "Total Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmountText(
                        amount = debt.amount,
                        currencySymbol = currencySymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (debt.dueDate != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (isBn) "পরিশোধের শেষ তারিখ:" else "Due:"} ${Formatters.formatDate(debt.dueDate, isBn)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "পরিশোধের অগ্রগতি" else "Repayment Progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = mainColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = mainColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (!debt.isSettled) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRecordPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mainColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString(Localization.Key.RECORD_PAYMENT, isBn),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SHOP CREDIT (দোকান বাকি) CONTENT & VENDOR MANAGEMENT
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopCreditOverviewContent(
    vendors: List<Vendor>,
    purchases: List<ShopCreditPurchase>,
    totalOwed: Double,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onVendorClick: (Long) -> Unit,
    onAddCreditClick: () -> Unit,
    onAddNewVendorClick: () -> Unit = {},
    onArchiveVendor: (Long, Boolean) -> Unit
) {
    val isBn = isBangla
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf(0) } // 0: Highest Balance, 1: Alphabetical, 2: Most Recent

    val activeVendors = remember(vendors) { vendors.filter { !it.isArchived } }
    val archivedVendors = remember(vendors) { vendors.filter { it.isArchived } }
    var isArchivedExpanded by remember { mutableStateOf(false) }

    val vendorSummaries = remember(activeVendors, purchases, searchQuery, selectedTag, sortBy) {
        val summaries = activeVendors.map { vendor ->
            val vPurchases = purchases.filter { it.vendorId == vendor.id }
            val owed = vPurchases.filter { !it.isSettled }.sumOf { it.remainingAmount }
            val lastActive = vPurchases.maxOfOrNull { it.dateTimestamp } ?: vendor.createdAt
            Triple(vendor, owed, lastActive)
        }

        val owedVendorsCount = summaries.count { it.second > 0 }
        val totalVendorsCount = summaries.size

        val filtered = summaries.filter { (vendor, _, _) ->
            val matchesSearch = vendor.name.contains(searchQuery, true) || vendor.phone.contains(searchQuery, true) || vendor.categoryTag.contains(searchQuery, true)
            val matchesTag = selectedTag == null || vendor.categoryTag.equals(selectedTag, true)
            matchesSearch && matchesTag
        }

        val sorted = when (sortBy) {
            0 -> filtered.sortedByDescending { it.second } // Highest Balance
            1 -> filtered.sortedBy { it.first.name } // Alphabetical
            2 -> filtered.sortedByDescending { it.third } // Most Recently Active
            else -> filtered
        }

        Triple(sorted, owedVendorsCount, totalVendorsCount)
    }

    val (displaySummaries, owedVendorsCount, totalVendorsCount) = vendorSummaries

    val archivedSummaries = remember(archivedVendors, purchases, searchQuery) {
        val summaries = archivedVendors.map { vendor ->
            val vPurchases = purchases.filter { it.vendorId == vendor.id }
            val owed = vPurchases.filter { !it.isSettled }.sumOf { it.remainingAmount }
            val lastActive = vPurchases.maxOfOrNull { it.dateTimestamp } ?: vendor.createdAt
            Triple(vendor, owed, lastActive)
        }
        if (searchQuery.isBlank()) {
            summaries
        } else {
            summaries.filter { (vendor, _, _) ->
                vendor.name.contains(searchQuery, true) || vendor.phone.contains(searchQuery, true) || vendor.categoryTag.contains(searchQuery, true)
            }
        }
    }

    val availableTags = remember(activeVendors) {
        activeVendors.map { it.categoryTag }.filter { it.isNotBlank() }.distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card (Flat card with border, no soft red fill)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "মোট দোকান বাকি" else "Total Shop Credit Owed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isBn) "$owedVendorsCount টি দোকান / মোট $totalVendorsCount টি" else "Owed to $owedVendorsCount of $totalVendorsCount vendors",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CurrencyAmountText(
                        amount = totalOwed,
                        currencySymbol = currencySymbol,
                        useBanglaDigits = useBnDigits,
                        hideBalances = hideBalances,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }

        // Section Title & Add New Shop Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "দোকানের তালিকা" else "Shop Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddNewVendorClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBn) "নতুন শপ" else "Add Shop", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar & Sort Dropdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isBn) "দোকান খুঁজুন..." else "Search vendors...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { sortExpanded = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .size(50.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBn) "সর্বোচ্চ বাকি" else "Highest Balance") },
                            onClick = { sortBy = 0; sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isBn) "নামানুসারে" else "Alphabetical") },
                            onClick = { sortBy = 1; sortExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isBn) "সাম্প্রতিক সক্রিয়" else "Most Recently Active") },
                            onClick = { sortBy = 2; sortExpanded = false }
                        )
                    }
                }
            }
        }

        // Optional Tag Filters Horizontally
        if (availableTags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text(Localization.getString(Localization.Key.ALL, isBn)) }
                    )
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag.equals(tag, true),
                            onClick = { selectedTag = if (selectedTag.equals(tag, true)) null else tag },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        // Vendors List or Empty State using EmptyStateView
        if (activeVendors.isEmpty() && archivedVendors.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Store,
                    title = if (isBn) "কোনো দোকান বাকি নেই" else "No shop credit recorded",
                    description = if (isBn) "মুদির দোকান, ফার্মেসি বা অন্যান্য দোকানের বাকি হিসাব রাখুন।" else "Keep track of credit purchases from local grocery, pharmacy, or retail stores.",
                    actionLabel = if (isBn) "+ দোকান বাকি যোগ করুন" else "+ Add Shop Credit",
                    onActionClick = onAddCreditClick
                )
            }
        } else {
                if (activeVendors.isEmpty() && archivedVendors.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isBn) "কোনো সক্রিয় দোকান বাকি নেই (সবগুলো আর্কাইভ করা)" else "No active shop credits (all shops are archived)",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (displaySummaries.isEmpty() && activeVendors.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isBn) "অনুসন্ধানের সাথে কোনো দোকান মেলেনি" else "No shops match the search",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(displaySummaries, key = { it.first.id }) { (vendor, owedAmount, lastActive) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVendorClick(vendor.id) },
                            shape = RoundedCornerShape(12.dp),
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
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = if (owedAmount > 0) ExpenseRed else IncomeGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vendor.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val tag = if (vendor.categoryTag.isNotBlank()) vendor.categoryTag else if (isBn) "সাধারণ দোকান" else "General Store"
                                    val subtitle = if (vendor.phone.isNotBlank()) "$tag • ${vendor.phone}" else tag
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (vendor.phone.isNotBlank()) {
                                    val context = LocalContext.current
                                    IconButton(
                                        onClick = { ContactUtils.dialPhoneNumber(context, vendor.phone, isBn) },
                                        modifier = Modifier.size(36.dp).testTag("quick_call_vendor_${vendor.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = if (isBn) "কল করুন" else "Call",
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    CurrencyAmountText(
                                        amount = owedAmount,
                                        currencySymbol = currencySymbol,
                                        useBanglaDigits = useBnDigits,
                                        hideBalances = hideBalances,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (owedAmount > 0) ExpenseRed else IncomeGreen
                                    )
                                    Text(
                                        text = if (owedAmount > 0) (if (isBn) "বাকি আছে" else "Owed") else (if (isBn) "পরিশোধিত" else "Settled"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (owedAmount > 0) ExpenseRed else IncomeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ARCHIVED SHOPS SECTION
                if (archivedVendors.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isArchivedExpanded = !isArchivedExpanded }
                                .testTag("archived_shops_toggle"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                                text = Localization.getString(Localization.Key.ARCHIVED_SHOPS, isBn),
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
                                                val countStr = archivedVendors.size.toString()
                                                Text(
                                                    text = if (useBnDigits) Formatters.toBanglaDigits(countStr) else countStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = Localization.getString(Localization.Key.ARCHIVED_SHOPS_SUBTITLE, isBn),
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
                        items(archivedSummaries, key = { "archived_vendor_${it.first.id}" }) { (vendor, owedAmount, _) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVendorClick(vendor.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = vendor.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = Localization.getString(Localization.Key.ARCHIVED_BADGE, isBn),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        val tag = if (vendor.categoryTag.isNotBlank()) vendor.categoryTag else if (isBn) "সাধারণ দোকান" else "General Store"
                                        val subtitle = if (vendor.phone.isNotBlank()) "$tag • ${vendor.phone}" else tag
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        CurrencyAmountText(
                                            amount = owedAmount,
                                            currencySymbol = currencySymbol,
                                            useBanglaDigits = useBnDigits,
                                            hideBalances = hideBalances,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (owedAmount > 0) ExpenseRed else IncomeGreen
                                        )
                                        Text(
                                            text = if (owedAmount > 0) (if (isBn) "বাকি আছে" else "Owed") else (if (isBn) "পরিশোধিত" else "Settled"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (owedAmount > 0) ExpenseRed else IncomeGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { onArchiveVendor(vendor.id, false) },
                                        modifier = Modifier.size(36.dp).testTag("restore_vendor_${vendor.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Unarchive,
                                            contentDescription = Localization.getString(Localization.Key.UNARCHIVE, isBn),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
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

@Composable
fun VendorLedgerContent(
    vendor: Vendor,
    purchases: List<ShopCreditPurchase>,
    payments: List<ShopCreditPayment>,
    isBangla: Boolean,
    useBnDigits: Boolean,
    hideBalances: Boolean,
    currencySymbol: String,
    onSettleClick: () -> Unit,
    onAddPurchaseClick: () -> Unit,
    onArchiveVendor: (Long, Boolean) -> Unit,
    onEditPhoneClick: (() -> Unit)? = null
) {
    val isBn = isBangla
    val context = LocalContext.current
    val totalRemaining = purchases.filter { !it.isSettled }.sumOf { it.remainingAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (vendor.isArchived) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "এই দোকানটি বর্তমানে আর্কাইভ করা আছে" else "This shop is currently archived",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(
                                onClick = { onArchiveVendor(vendor.id, false) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Unarchive,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.getString(Localization.Key.UNARCHIVE, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vendor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (vendor.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { ContactUtils.dialPhoneNumber(context, vendor.phone, isBn) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = IncomeGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = vendor.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = if (isBn) "কোনো ফোন নম্বর যুক্ত নেই" else "No phone number linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (vendor.locationNote.isNotBlank()) {
                            Text(
                                text = vendor.locationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons for phone: Call or Add Phone, matching debtor/creditor (ContactLedgerContent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (vendor.phone.isNotBlank()) {
                        Button(
                            onClick = { ContactUtils.dialPhoneNumber(context, vendor.phone, isBn) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncomeGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("vendor_details_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = if (isBn) "কল করুন" else "Call",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "কল করুন" else "Call",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (onEditPhoneClick != null) {
                            OutlinedButton(
                                onClick = onEditPhoneClick,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("vendor_details_edit_phone_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "নম্বর এডিট" else "Edit Phone",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (onEditPhoneClick != null) {
                        OutlinedButton(
                            onClick = onEditPhoneClick,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("vendor_details_add_phone_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "ফোন নম্বর যুক্ত করুন" else "Add Phone Number",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                            text = if (isBn) "মোট বাকি" else "Total Owed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CurrencyAmountText(
                            amount = totalRemaining,
                            currencySymbol = currencySymbol,
                            useBanglaDigits = useBnDigits,
                            hideBalances = hideBalances,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddPurchaseClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "নতুন বাকি" else "Add")
                        }

                        if (totalRemaining > 0) {
                            Button(
                                onClick = onSettleClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                            ) {
                                Text(if (isBn) "বাকি শোধ" else "Settle")
                            }
                        }
                    }
                }
            }
        }
    }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "ক্রয় ও পেমেন্ট ইতিহাস" else "History Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (purchases.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Store,
                    title = if (isBn) "কোনো ক্রয়ের ইতিহাস নেই" else "No purchases recorded",
                    description = if (isBn) "এই দোকানের নতুন বাকি বা কেনাকাটা যোগ করুন।" else "No shop credit purchases found for this vendor.",
                    actionLabel = if (isBn) "+ নতুন বাকি যোগ করুন" else "+ Add Purchase",
                    onActionClick = onAddPurchaseClick
                )
            }
        } else {
            items(purchases, key = { it.id }) { purchase ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Formatters.formatDate(purchase.dateTimestamp, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (purchase.isSettled) {
                                    Text(
                                        text = if (isBn) "পরিশোধিত" else "Settled",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (purchase.note.isNotBlank()) purchase.note else if (isBn) "বাকি পণ্য ক্রয়" else "Credit Purchase",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                CurrencyAmountText(
                                    amount = purchase.amount,
                                    currencySymbol = currencySymbol,
                                    useBanglaDigits = useBnDigits,
                                    hideBalances = hideBalances,
                                    style = MaterialTheme.typography.titleMedium,
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

// Bottom Sheets Implementation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanEntryBottomSheet(
    contacts: List<Contact>,
    accounts: List<com.plusemon.hisab.ui.viewmodel.AccountWithBalance>,
    initialContactId: Long?,
    initialType: DebtType = DebtType.OWED_TO_ME,
    isBangla: Boolean,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (contactId: Long?, personName: String, accountId: Long, amount: Double, type: DebtType, dueDate: Long?, note: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialPageIndex = if (initialType == DebtType.I_OWE) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPageIndex) { 2 }

    var name by remember { mutableStateOf(contacts.find { it.id == initialContactId }?.name ?: "") }
    var phone by remember { mutableStateOf(contacts.find { it.id == initialContactId }?.phone ?: "") }
    var amountText by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L) }
    var note by remember { mutableStateOf("") }

    val quickAmounts = listOf(500, 1000, 2000, 5000, 10000)

    var phoneOptionsForSelection by remember { mutableStateOf<List<ContactPhoneOption>?>(null) }
    var pendingContactName by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ContactUtils.extractContact(context, uri)
            if (result != null) {
                if (result.name.isNotBlank()) {
                    name = result.name
                }
                when {
                    result.phones.isEmpty() -> {
                        Toast.makeText(
                            context,
                            if (isBangla) "এই কন্টাক্টে কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone numbers found in this contact",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    result.phones.size == 1 -> {
                        phone = result.phones.first().rawNumber
                    }
                    else -> {
                        pendingContactName = result.name
                        phoneOptionsForSelection = result.phones
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        contactPickerLauncher.launch(null)
    }

    val onPickContactClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contactPickerLauncher.launch(null)
        }
    }

    if (phoneOptionsForSelection != null) {
        SelectContactPhoneDialog(
            contactName = pendingContactName,
            options = phoneOptionsForSelection!!,
            isBangla = isBangla,
            onSelectPhone = { selected ->
                phone = selected.rawNumber
                phoneOptionsForSelection = null
            },
            onDismiss = {
                phoneOptionsForSelection = null
            }
        )
    }

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
                        text = if (isBangla) "ধার / দেনার হিসাব" else "Record Loan / Debt",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBangla) "টাকা দেওয়া বা নেওয়ার হিসাব রাখুন" else "Track lending or borrowing with contacts",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Swipeable Tab Indicator (Gave Money vs Took Money)
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            color = if (pagerState.currentPage == 0) IncomeGreen else ExpenseRed,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        modifier = Modifier.testTag("tab_gave_money"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (pagerState.currentPage == 0) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "টাকা দিলাম (পাবো)" else "Gave Money",
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 0) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        modifier = Modifier.testTag("tab_took_money"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (pagerState.currentPage == 1) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "টাকা নিলাম (দেবো)" else "Took Money",
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == 1) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pager content with Horizontal Swipe
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val isGave = page == 0
                val accentColor = if (isGave) IncomeGreen else ExpenseRed

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding()
                ) {
                    // Existing quick contact chips if available (integrated directly above person name)
                    if (contacts.isNotEmpty()) {
                        Text(
                            text = if (isBangla) "সাম্প্রতিক কন্টাক্টসমূহ:" else "Recent Contacts:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(contacts.take(8), key = { it.id }) { contact ->
                                FilterChip(
                                    selected = name == contact.name,
                                    onClick = {
                                        name = contact.name
                                        if (contact.phone.isNotBlank()) {
                                            phone = contact.phone
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text(contact.name, fontSize = 12.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Person Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isBangla) "ব্যক্তির নাম *" else "Person Name *") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = onPickContactClick,
                                modifier = Modifier.testTag(if (isGave) "pick_contact_btn_gave" else "pick_contact_btn_took")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mobile Number Input
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isBangla) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone Number (Optional)") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            IconButton(onClick = onPickContactClick) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_phone_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account Selector
                    if (accounts.isNotEmpty()) {
                        Text(
                            text = if (isGave)
                                (if (isBangla) "কোন অ্যাকাউন্ট থেকে টাকা দিলেন?" else "Which account did you give from?")
                            else
                                (if (isBangla) "কোন অ্যাকাউন্টে টাকা জমা হলো?" else "Which account was money received in?"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(accounts, key = { it.account.id }) { accWithBal ->
                                val acc = accWithBal.account
                                val isSelected = selectedAccountId == acc.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAccountId = acc.id },
                                    label = {
                                        Text("${acc.name} (${currencySymbol}${accWithBal.balance.toInt()})", fontSize = 12.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                        selectedLabelColor = accentColor
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(Localization.getString(Localization.Key.AMOUNT, isBangla) + " *") },
                        prefix = {
                            Text(
                                text = "$currencySymbol ",
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Amount Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickAmounts) { amt ->
                            FilterChip(
                                selected = amountText == amt.toString(),
                                onClick = { amountText = amt.toString() },
                                label = { Text("+$currencySymbol$amt", fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Note Input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(Localization.getString(Localization.Key.NOTE, isBangla)) },
                        placeholder = { Text(if (isBangla) "যেমন: জরুরি ধার, ব্যবসার জন্য" else "e.g. Emergency loan, business") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_input")
                    )

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
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                if (name.isNotBlank() && amt > 0) {
                                    val matched = contacts.find { it.name.equals(name.trim(), true) }
                                    val activeType = if (isGave) DebtType.OWED_TO_ME else DebtType.I_OWE
                                    onSave(matched?.id, name.trim(), selectedAccountId, amt, activeType, null, note.trim(), phone.trim())
                                }
                            },
                            enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("save_debt_button")
                        ) {
                            Text(
                                text = if (isGave)
                                    (if (isBangla) "টাকা দেওয়া সেভ করুন" else "Save Lent Entry")
                                else
                                    (if (isBangla) "টাকা নেওয়া সেভ করুন" else "Save Borrowed Entry"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun AddLoanEntryDialog(
    contacts: List<Contact>,
    accounts: List<com.plusemon.hisab.ui.viewmodel.AccountWithBalance>,
    initialContactId: Long?,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (contactId: Long?, personName: String, accountId: Long, amount: Double, type: DebtType, dueDate: Long?, note: String, phone: String) -> Unit
) {
    AddLoanEntryBottomSheet(
        contacts = contacts,
        accounts = accounts,
        initialContactId = initialContactId,
        isBangla = isBangla,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentBottomSheet(
    debt: LoanDebt,
    accounts: List<com.plusemon.hisab.ui.viewmodel.AccountWithBalance>,
    isBangla: Boolean,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: Double, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf(debt.remainingAmount.toString()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L) }
    var note by remember { mutableStateOf("") }
    val isOwedToMe = debt.type == DebtType.OWED_TO_ME

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
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.getString(Localization.Key.RECORD_PAYMENT, isBangla),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${debt.personName} • ${if (isOwedToMe) (if (isBangla) "পাওনা আদায়" else "Collection") else (if (isBangla) "দেনা পরিশোধ" else "Repayment")}",
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

            // Account selection
            if (accounts.isNotEmpty()) {
                Text(
                    text = if (isOwedToMe)
                        (if (isBangla) "কোন অ্যাকাউন্টে টাকা জমা হলো?" else "Deposit Account")
                    else
                        (if (isBangla) "কোন অ্যাকাউন্ট থেকে পরিশোধ করলেন?" else "Payment Account"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts, key = { it.account.id }) { accWithBal ->
                        val acc = accWithBal.account
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.name} (${currencySymbol}${accWithBal.balance.toInt()})", fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(Localization.getString(Localization.Key.AMOUNT, isBangla)) },
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Localization.getString(Localization.Key.NOTE, isBangla)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onConfirm(selectedAccountId, amt, note.trim())
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = Localization.getString(Localization.Key.SAVE, isBangla),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun RecordPaymentDialog(
    debt: LoanDebt,
    accounts: List<com.plusemon.hisab.ui.viewmodel.AccountWithBalance>,
    isBangla: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: Double, note: String) -> Unit
) {
    RecordPaymentBottomSheet(
        debt = debt,
        accounts = accounts,
        isBangla = isBangla,
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopCreditBottomSheet(
    vendors: List<Vendor>,
    purchases: List<ShopCreditPurchase>,
    initialVendorId: Long?,
    isBangla: Boolean,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onSave: (vendorId: Long?, vendorName: String, amount: Double, dueDate: Long?, note: String, phone: String, locationNote: String, tag: String) -> Unit,
    onAddNewVendor: ((name: String, phone: String, locationNote: String, categoryTag: String, onCreated: (Vendor) -> Unit) -> Unit)? = null
) {
    val activeVendors = remember(vendors) { vendors.filter { !it.isArchived } }
    var selectedVendor by remember(vendors, initialVendorId) {
        mutableStateOf(activeVendors.find { it.id == initialVendorId })
    }
    var vendorSearchQuery by remember {
        mutableStateOf(selectedVendor?.name ?: "")
    }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(selectedVendor?.phone ?: "") }
    var locationNote by remember { mutableStateOf(selectedVendor?.locationNote ?: "") }
    var categoryTag by remember { mutableStateOf(selectedVendor?.categoryTag ?: "General") }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showCreateVendorBottomSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var phoneOptionsForSelection by remember { mutableStateOf<List<ContactPhoneOption>?>(null) }
    var pendingContactName by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ContactUtils.extractContact(context, uri)
            if (result != null) {
                if (selectedVendor == null && vendorSearchQuery.isBlank() && result.name.isNotBlank()) {
                    vendorSearchQuery = result.name
                }
                when {
                    result.phones.isEmpty() -> {
                        Toast.makeText(
                            context,
                            if (isBangla) "এই কন্টাক্টে কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone numbers found in this contact",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    result.phones.size == 1 -> {
                        phone = result.phones.first().rawNumber
                    }
                    else -> {
                        pendingContactName = result.name
                        phoneOptionsForSelection = result.phones
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        contactPickerLauncher.launch(null)
    }

    val onPickContactClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contactPickerLauncher.launch(null)
        }
    }

    if (phoneOptionsForSelection != null) {
        SelectContactPhoneDialog(
            contactName = pendingContactName,
            options = phoneOptionsForSelection!!,
            isBangla = isBangla,
            onSelectPhone = { selected ->
                phone = selected.rawNumber
                phoneOptionsForSelection = null
            },
            onDismiss = {
                phoneOptionsForSelection = null
            }
        )
    }

    if (showCreateVendorBottomSheet) {
        AddNewVendorBottomSheet(
            initialName = if (selectedVendor == null) vendorSearchQuery.trim() else "",
            isBangla = isBangla,
            onDismiss = { showCreateVendorBottomSheet = false },
            onSave = { vName, vPhone, vLocation, vTag ->
                if (onAddNewVendor != null) {
                    onAddNewVendor(vName, vPhone, vLocation, vTag) { newVendor ->
                        selectedVendor = newVendor
                        vendorSearchQuery = newVendor.name
                        phone = newVendor.phone
                        locationNote = newVendor.locationNote
                        categoryTag = newVendor.categoryTag
                    }
                } else {
                    vendorSearchQuery = vName
                    phone = vPhone
                    locationNote = vLocation
                    categoryTag = vTag
                }
                showCreateVendorBottomSheet = false
            }
        )
    }

    val inputFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF161F30),
        unfocusedContainerColor = Color(0xFF161F30),
        disabledContainerColor = Color(0xFF161F30),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color(0xFF2A374E),
        focusedTextColor = Color(0xFFF8FAFC),
        unfocusedTextColor = Color(0xFFF8FAFC),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color(0xFF94A3B8),
        focusedPlaceholderColor = Color(0xFF64748B),
        unfocusedPlaceholderColor = Color(0xFF64748B),
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = Color(0xFF94A3B8),
        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedTrailingIconColor = Color(0xFF94A3B8),
        cursorColor = MaterialTheme.colorScheme.primary
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF101726),
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            // Header: Store Icon + Title & Subtitle + Explicit Close Icon (✕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "দোকান বাকি রেকর্ড করুন" else "Add Shop Credit",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "দোকানের বাকির হিসাব লিখে রাখুন" else "Record purchase on credit from shop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("shop_credit_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Store Selector Label - Clean single trigger design (redundant label-level duplicate removed)
            Text(
                text = if (isBangla) "শপ / দোকান নির্বাচন করুন *" else "Select Store / Shop *",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (selectedVendor != null) {
                val vendor = selectedVendor!!
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161F30),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vendor.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            if (vendor.categoryTag.isNotBlank() || vendor.phone.isNotBlank()) {
                                Text(
                                    text = listOfNotNull(
                                        vendor.categoryTag.ifBlank { null },
                                        vendor.phone.ifBlank { null }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                selectedVendor = null
                                vendorSearchQuery = ""
                                phone = ""
                                locationNote = ""
                                categoryTag = "General"
                                dropdownExpanded = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = vendorSearchQuery,
                        onValueChange = {
                            vendorSearchQuery = it
                            dropdownExpanded = true
                        },
                        placeholder = {
                            Text(
                                text = if (isBangla) "দোকানের নাম খুঁজুন বা লিখুন..." else "Search or type store name...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        },
                        trailingIcon = {
                            if (vendorSearchQuery.isNotBlank()) {
                                IconButton(onClick = { vendorSearchQuery = ""; dropdownExpanded = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            } else {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Dropdown",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("shop_credit_vendor_input")
                    )

                    val filteredVendors = activeVendors.filter {
                        it.name.contains(vendorSearchQuery, ignoreCase = true)
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded && selectedVendor == null,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color(0xFF161F30))
                    ) {
                        filteredVendors.take(5).forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(v.name, fontWeight = FontWeight.SemiBold, color = Color(0xFFF8FAFC))
                                        if (v.phone.isNotBlank() || v.categoryTag.isNotBlank()) {
                                            Text(
                                                "${v.categoryTag} ${if (v.phone.isNotBlank()) "• ${v.phone}" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    selectedVendor = v
                                    vendorSearchQuery = v.name
                                    phone = v.phone
                                    locationNote = v.locationNote
                                    categoryTag = v.categoryTag
                                    dropdownExpanded = false
                                }
                            )
                        }

                        if (filteredVendors.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFF2A374E))
                        }

                        // Pinned "+ Add New Shop" action with syntax typo fixed (+ Add New Shop)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "নতুন শপ" else "Add New Shop",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                showCreateVendorBottomSheet = true
                            },
                            modifier = Modifier.testTag("shop_credit_add_new_shop_btn")
                        )
                    }
                }

                if (activeVendors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeVendors.take(6).forEach { v ->
                            FilterChip(
                                selected = selectedVendor?.id == v.id,
                                onClick = {
                                    selectedVendor = v
                                    vendorSearchQuery = v.name
                                    phone = v.phone
                                    locationNote = v.locationNote
                                    categoryTag = v.categoryTag
                                    dropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text(v.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = {
                    Text(
                        text = if (isBangla) "দোকানের মোবাইল নম্বর (ঐচ্ছিক)" else "Store Phone Number (Optional)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    IconButton(
                        onClick = onPickContactClick,
                        modifier = Modifier.testTag("shop_credit_pick_phone_btn")
                    ) {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = inputFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("shop_credit_phone_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = {
                    Text(
                        text = (if (isBangla) "টাকার পরিমাণ" else "Amount") + " *",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = inputFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("shop_credit_amount_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        text = if (isBangla) "পণ্যের বিবরণ (ঐচ্ছিক)" else "Items / Note (Optional)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                placeholder = {
                    Text(
                        text = if (isBangla) "যেমন: চাল, ডাল, তেল" else "e.g. Rice, oil, milk",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF64748B)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = inputFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("shop_credit_note_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            val amt = amountText.toDoubleOrNull() ?: 0.0
            val isFormValid = (selectedVendor != null || vendorSearchQuery.isNotBlank()) && amt > 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("shop_credit_cancel_btn")
                ) {
                    Text(
                        text = Localization.getString(Localization.Key.CANCEL, isBangla),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = {
                        val finalName = selectedVendor?.name ?: vendorSearchQuery.trim()
                        if (finalName.isNotBlank() && amt > 0) {
                            val matched = selectedVendor ?: vendors.find { it.name.equals(finalName, true) }
                            onSave(
                                matched?.id,
                                finalName,
                                amt,
                                null,
                                note.trim(),
                                phone.trim().ifBlank { selectedVendor?.phone ?: "" },
                                selectedVendor?.locationNote ?: locationNote.trim(),
                                selectedVendor?.categoryTag ?: categoryTag.trim()
                            )
                        }
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("shop_credit_save_btn")
                ) {
                    Text(
                        text = Localization.getString(Localization.Key.SAVE, isBangla),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun AddShopCreditDialog(
    vendors: List<Vendor>,
    purchases: List<ShopCreditPurchase>,
    initialVendorId: Long?,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (vendorId: Long?, vendorName: String, amount: Double, dueDate: Long?, note: String, phone: String, locationNote: String, tag: String) -> Unit,
    onAddNewVendor: ((name: String, phone: String, locationNote: String, categoryTag: String, onCreated: (Vendor) -> Unit) -> Unit)? = null
) {
    AddShopCreditBottomSheet(
        vendors = vendors,
        purchases = purchases,
        initialVendorId = initialVendorId,
        isBangla = isBangla,
        onDismiss = onDismiss,
        onSave = onSave,
        onAddNewVendor = onAddNewVendor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewVendorBottomSheet(
    initialName: String = "",
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, locationNote: String, categoryTag: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    var locationNote by remember { mutableStateOf("") }
    var categoryTag by remember { mutableStateOf("") }

    var phoneOptionsForSelection by remember { mutableStateOf<List<ContactPhoneOption>?>(null) }
    var pendingContactName by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ContactUtils.extractContact(context, uri)
            if (result != null) {
                if (result.name.isNotBlank()) {
                    name = result.name
                }
                when {
                    result.phones.isEmpty() -> {
                        Toast.makeText(
                            context,
                            if (isBangla) "এই কন্টাক্টে কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone numbers found in this contact",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    result.phones.size == 1 -> {
                        phone = result.phones.first().rawNumber
                    }
                    else -> {
                        pendingContactName = result.name
                        phoneOptionsForSelection = result.phones
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        contactPickerLauncher.launch(null)
    }

    val onPickContactClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contactPickerLauncher.launch(null)
        }
    }

    if (phoneOptionsForSelection != null) {
        SelectContactPhoneDialog(
            contactName = pendingContactName,
            options = phoneOptionsForSelection!!,
            isBangla = isBangla,
            onSelectPhone = { selected ->
                phone = selected.rawNumber
                phoneOptionsForSelection = null
            },
            onDismiss = {
                phoneOptionsForSelection = null
            }
        )
    }

    val presetCategories = remember(isBangla) {
        if (isBangla) listOf("মুদি দোকান", "ফার্মেসি", "কাঁচাবাজার", "রেস্তোরাঁ", "হার্ডওয়্যার", "জেনারেল স্টোর")
        else listOf("Grocery", "Pharmacy", "Fresh Market", "Restaurant", "Hardware", "General")
    }

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
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "নতুন শপ যুক্ত করুন" else "Add New Shop",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "দোকানের বিবরণ সংরক্ষণ করুন" else "Save store information",
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

            // Pick from Contacts Button
            OutlinedButton(
                onClick = onPickContactClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vendor_pick_contact_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ContactPhone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "ফোনের কন্টাক্ট থেকে চুজ করুন" else "Pick from Phone Contacts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isBangla) "দোকানের নাম *" else "Store / Shop Name *") },
                leadingIcon = {
                    Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    IconButton(onClick = onPickContactClick) {
                        Icon(Icons.Default.ContactPhone, contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("vendor_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isBangla) "দোকানের ধরন / ক্যাটাগরি" else "Category / Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetCategories.forEach { cat ->
                    FilterChip(
                        selected = categoryTag.equals(cat, ignoreCase = true),
                        onClick = {
                            categoryTag = if (categoryTag.equals(cat, ignoreCase = true)) "" else cat
                        },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(if (isBangla) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone Number (Optional)") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    IconButton(onClick = onPickContactClick) {
                        Icon(Icons.Default.Contacts, contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("vendor_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = locationNote,
                onValueChange = { locationNote = it },
                label = { Text(if (isBangla) "ঠিকানা / এলাকা (ঐচ্ছিক)" else "Location / Address (Optional)") },
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        if (name.isNotBlank()) {
                            onSave(name.trim(), phone.trim(), locationNote.trim(), categoryTag.ifBlank { "General" }.trim())
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = Localization.getString(Localization.Key.SAVE, isBangla),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun AddNewVendorDialog(
    initialName: String = "",
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, locationNote: String, categoryTag: String) -> Unit
) {
    AddNewVendorBottomSheet(
        initialName = initialName,
        isBangla = isBangla,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

@Composable
fun EditVendorPhoneDialog(
    vendor: Vendor,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf(vendor.phone) }
    var phoneOptionsForSelection by remember { mutableStateOf<List<ContactPhoneOption>?>(null) }
    var pendingContactName by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = ContactUtils.extractContact(context, uri)
            if (result != null) {
                when {
                    result.phones.isEmpty() -> {
                        Toast.makeText(
                            context,
                            if (isBangla) "এই কন্টাক্টে কোনো ফোন নম্বর পাওয়া যায়নি" else "No phone numbers found in this contact",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    result.phones.size == 1 -> {
                        phone = result.phones.first().rawNumber
                    }
                    else -> {
                        pendingContactName = result.name
                        phoneOptionsForSelection = result.phones
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        contactPickerLauncher.launch(null)
    }

    val onPickContact = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contactPickerLauncher.launch(null)
        }
    }

    if (phoneOptionsForSelection != null) {
        SelectContactPhoneDialog(
            contactName = pendingContactName,
            options = phoneOptionsForSelection!!,
            isBangla = isBangla,
            onSelectPhone = { selected ->
                phone = selected.rawNumber
                phoneOptionsForSelection = null
            },
            onDismiss = {
                phoneOptionsForSelection = null
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = if (isBangla) "দোকানের ফোন নম্বর পরিবর্তন / যুক্ত করুন" else "Update Shop Phone Number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = vendor.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onPickContact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_vendor_pick_contact_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "ফোনের কন্টাক্ট থেকে চুজ করুন" else "Pick from Phone Contacts",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isBangla) "মোবাইল নম্বর" else "Phone Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = onPickContact) {
                            Icon(Icons.Default.Contacts, contentDescription = if (isBangla) "কন্টাক্ট থেকে চুজ করুন" else "Pick from Contacts", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_vendor_phone_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(phone.trim())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBangla) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleShopCreditBottomSheet(
    vendor: Vendor,
    defaultAmount: Double,
    accounts: List<com.plusemon.hisab.ui.viewmodel.AccountWithBalance>,
    isBangla: Boolean,
    currencySymbol: String = "৳",
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: Double, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf(defaultAmount.toString()) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L) }
    var note by remember { mutableStateOf("") }

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
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "দোকানের বাকি পরিশোধ" else "Settle Shop Credit",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${vendor.name} • ${if (isBangla) "বাকি টাকা শোধ করুন" else "Clear outstanding credit"}",
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

            if (accounts.isNotEmpty()) {
                Text(
                    text = if (isBangla) "কোন অ্যাকাউন্ট থেকে পরিশোধ করবেন?" else "Payment Account",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { accWithBal ->
                        val acc = accWithBal.account
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.name} (${currencySymbol}${accWithBal.balance.toInt()})", fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(Localization.getString(Localization.Key.AMOUNT, isBangla) + " *") },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Localization.getString(Localization.Key.NOTE, isBangla)) },
                placeholder = { Text(if (isBangla) "যেমন: নগদ পরিশোধ, বিকাশ" else "e.g. Cash settlement, bKash") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onConfirm(selectedAccountId, amt, note.trim())
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = if (isBangla) "পরিশোধ নিশ্চিত করুন" else "Confirm Settle",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Backward compatibility forwarder
@Composable
fun SettleShopCreditDialog(
    vendor: Vendor,
    defaultAmount: Double,
    accounts: List<com.plusemon.hisab.ui.viewmodel.AccountWithBalance>,
    isBangla: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: Double, note: String) -> Unit
) {
    SettleShopCreditBottomSheet(
        vendor = vendor,
        defaultAmount = defaultAmount,
        accounts = accounts,
        isBangla = isBangla,
        currencySymbol = currencySymbol,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

