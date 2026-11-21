package cn.turingzy.ledger.ui.feature.bill

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import cn.turingzy.ledger.ui.feature.bill.component.BillActionPicker
import cn.turingzy.ledger.ui.feature.bill.component.BillDeleteDialog
import cn.turingzy.ledger.ui.feature.bill.component.BillSelectionBar
import cn.turingzy.ledger.ui.feature.bill.component.BillSelectionBarPadding
import cn.turingzy.ledger.ui.feature.bill.component.BillSwipeRow
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.feature.bill.sheet.RecordCreateSheet
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
internal fun BillTransactionActions(
    dayGroups: List<DayTransactionGroup>,
    ungroupedRows: List<TransactionDetail> = emptyList(),
    viewModel: BillViewModel,
    transactionViewModel: TransactionDetailsViewModel,
    snackbarHostState: SnackbarHostState,
    onSelectionVisibleChange: (Boolean) -> Unit,
    content: @Composable (@Composable (detail: TransactionDetail, roundTop: Boolean, roundBottom: Boolean) -> Unit) -> Unit,
) {
    var selection by remember { mutableStateOf<Set<Long>?>(null) }
    var swipeId by remember { mutableStateOf<Long?>(null) }
    var picker by remember { mutableStateOf<BillActionPicker?>(null) }
    var deleteIds by remember { mutableStateOf<Set<Long>?>(null) }
    var draft by remember { mutableStateOf<Transaction?>(null) }
    var applying by remember { mutableStateOf(false) }
    val transactions = remember(dayGroups, ungroupedRows) {
        (dayGroups.flatMap { it.transactions } + ungroupedRows).associateBy { it.transaction.id }
    }
    val selecting = selection != null
    val selectedIds = selection.orEmpty().intersect(transactions.keys)
    val updateSelectionVisibility by rememberUpdatedState(onSelectionVisibleChange)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val failureMessage = stringResource(R.string.bill_action_failed)

    LaunchedEffect(selecting) { updateSelectionVisibility(selecting) }
    DisposableEffect(Unit) { onDispose { updateSelectionVisibility(false) } }
    LaunchedEffect(transactions.keys) {
        if (selecting) selection = selectedIds
        if (swipeId !in transactions.keys) swipeId = null
    }

    BackHandler(enabled = selecting) {
        if (!applying) selection = null
    }

    fun apply(ids: Set<Long>, change: TransactionBatchChange) {
        if (applying || ids.isEmpty()) return
        applying = true
        picker = null
        deleteIds = null
        scope.launch {
            try {
                viewModel.applyBatch(ids, change)
                selection = selection?.minus(ids)?.takeIf { it.isNotEmpty() }
                swipeId = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Unable to apply a bill action")
                scope.launch { snackbarHostState.showSnackbar(failureMessage) }
            } finally {
                applying = false
            }
        }
    }

    val bottomPadding = if (selecting) BillSelectionBarPadding else LocalBottomBarContentPadding.current
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarContentPadding provides bottomPadding) {
            content { detail, roundTop, roundBottom ->
                val id = detail.transaction.id
                BillSwipeRow(
                    detail = detail,
                    showDate = ungroupedRows.isNotEmpty(),
                    roundTop = roundTop,
                    roundBottom = roundBottom,
                    selecting = selecting,
                    selected = id in selectedIds,
                    enabled = !applying,
                    revealed = swipeId == id,
                    onRevealChange = { reveal ->
                        if (reveal) swipeId = id else if (swipeId == id) swipeId = null
                    },
                    onClick = {
                        when {
                            selecting -> selection = if (id in selectedIds) selectedIds - id else selectedIds + id
                            swipeId != null -> swipeId = null
                            else -> transactionViewModel.selectTransaction(id)
                        }
                    },
                    onLongClick = {
                        if (!selecting) {
                            swipeId = null
                            focusManager.clearFocus()
                            selection = setOf(id)
                        }
                    },
                    onCopy = {
                        swipeId = null
                        draft = detail.transaction.copy(
                            id = 0L, importKey = null, baseAmount = null, baseCurrency = null,
                            baseRate = null, baseRateDate = null, currencySnapshots = emptyList(),
                        )
                    },
                    onEdit = {
                        swipeId = null
                        transactionViewModel.selectTransaction(id)
                    },
                    onDelete = {
                        swipeId = null
                        deleteIds = setOf(id)
                    },
                )
            }
        }
        if (selecting) BillSelectionBar(
            selectedCount = selectedIds.size,
            allSelected = selectedIds.isNotEmpty() && selectedIds.size == transactions.size,
            enabled = !applying,
            onSelectAll = { select -> selection = if (select) transactions.keys.toSet() else emptySet() },
            onSelectAccount = { picker = BillActionPicker.Account },
            onSelectCategory = { picker = BillActionPicker.Category },
            onDelete = { deleteIds = selectedIds },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    val selectedPicker = picker
    if (selectedPicker != null) {
        val details = selectedIds.mapNotNull(transactions::get)
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        val categories by viewModel.categories.collectAsStateWithLifecycle()
        val options = remember(details, accounts, categories) { billBatchOptions(details, accounts, categories) }
        selectedPicker.Sheet(
            accounts = accounts.filter { it.account.id in options.accounts },
            categories = categories.filter { it.category.id in options.categories }.sortedBy { it.category.sortOrder },
            accountCount = { options.accounts[it].orEmpty().size },
            categoryCount = { options.categories[it].orEmpty().size },
            selectedCount = selectedIds.size,
            type = details.firstOrNull()?.transaction?.type ?: TransactionType.EXPENSE,
            onDismiss = { picker = null },
            onSelect = { change ->
                val ids = when (change) {
                    is TransactionBatchChange.Account -> options.accounts[change.accountId].orEmpty()
                    is TransactionBatchChange.Category -> options.categories[change.categoryId].orEmpty()
                    TransactionBatchChange.Delete -> selectedIds
                }
                apply(ids, change)
            },
        )
    }
    deleteIds?.let { ids ->
        BillDeleteDialog(
            count = ids.size,
            onDismiss = { deleteIds = null },
            onConfirm = { apply(ids, TransactionBatchChange.Delete) },
        )
    }
    draft?.let { transaction ->
        RecordCreateSheet(
            date = LocalDate.now(), onDismiss = { draft = null },
            viewModel = viewModel, transactionViewModel = transactionViewModel, draft = transaction,
        )
    }
}
