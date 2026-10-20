package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.FinanceEmployeeOption
import com.example.data.PendingExpenseReview
import com.example.data.SupabaseFinanceRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun FinanceOperationsScreen(accessToken: String, onClose: () -> Unit) {
    val repository = remember { SupabaseFinanceRepository() }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<List<PendingExpenseReview>>(emptyList()) }
    var employees by remember { mutableStateOf<List<FinanceEmployeeOption>>(emptyList()) }
    var selectedEmployee by remember { mutableStateOf<FinanceEmployeeOption?>(null) }
    var employeeMenu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val reasons = remember { mutableStateMapOf<String, String>() }
    var returnAmount by remember { mutableStateOf("") }
    var returnReference by remember { mutableStateOf("") }
    var returnReason by remember { mutableStateOf("") }
    var periodStart by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var periodEnd by remember { mutableStateOf(LocalDate.now().toString()) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                pending = repository.listPendingExpenses(accessToken)
                employees = repository.listEmployees(accessToken)
                if (selectedEmployee == null) selectedEmployee = employees.firstOrNull()
            } catch (e: Exception) {
                error = e.message ?: "Could not load finance operations."
            } finally { loading = false }
        }
    }
    LaunchedEffect(accessToken) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Finance operations", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onClose) { Text("Close") }
        }
        Text("Expense review, returned-cash recording and reconciliation.", style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (loading) CircularProgressIndicator() else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending expense review", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = ::reload) { Text("Refresh") }
                    }
                    Text("Acceptance is enabled only for authoritative external verification.", style = MaterialTheme.typography.bodySmall)
                }
                if (pending.isEmpty()) item { Text("No expenses awaiting review.") }
                items(pending, key = { it.id }) { expense ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(expense.merchant, style = MaterialTheme.typography.titleMedium)
                            Text("KES ${"%,.2f".format(expense.amount)} · ${expense.category}")
                            Text("Expense date: ${expense.expenseDate} · Invoice date: ${expense.invoiceDate ?: "not captured"}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: ${expense.status} · Verification: ${expense.verificationStatus}", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = reasons[expense.id].orEmpty(),
                                onValueChange = { reasons[expense.id] = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Review reason (required)") },
                                minLines = 2
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    enabled = !busy && expense.verificationStatus == "externally_verified" && reasons[expense.id].orEmpty().trim().length >= 5,
                                    onClick = {
                                        busy = true; error = null; notice = null
                                        scope.launch {
                                            try {
                                                repository.reviewExpense(accessToken, expense.id, "accept", reasons[expense.id].orEmpty())
                                                notice = "Expense accepted; ledger updated."
                                                reload()
                                            } catch (e: Exception) { error = e.message ?: "Review failed." }
                                            finally { busy = false }
                                        }
                                    }
                                ) { Text("Accept") }
                                OutlinedButton(
                                    enabled = !busy && reasons[expense.id].orEmpty().trim().length >= 5,
                                    onClick = {
                                        busy = true; error = null; notice = null
                                        scope.launch {
                                            try {
                                                repository.reviewExpense(accessToken, expense.id, "reject", reasons[expense.id].orEmpty())
                                                notice = "Expense rejected with an audit reason."
                                                reload()
                                            } catch (e: Exception) { error = e.message ?: "Review failed." }
                                            finally { busy = false }
                                        }
                                    }
                                ) { Text("Reject") }
                                TextButton(
                                    enabled = !busy && reasons[expense.id].orEmpty().trim().length >= 5,
                                    onClick = {
                                        busy = true; error = null; notice = null
                                        scope.launch {
                                            try {
                                                repository.reviewExpense(accessToken, expense.id, "needs_review", reasons[expense.id].orEmpty())
                                                notice = "Expense returned for further review."
                                                reload()
                                            } catch (e: Exception) { error = e.message ?: "Review failed." }
                                            finally { busy = false }
                                        }
                                    }
                                ) { Text("Needs review") }
                            }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Record returned cash", style = MaterialTheme.typography.titleLarge)
                            Text("Records a cash receipt only; it does not initiate a transfer.", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { employeeMenu = true }, enabled = employees.isNotEmpty()) {
                                Text(selectedEmployee?.let { "${it.fullName} (${it.employeeNumber})" } ?: "Choose employee")
                            }
                            DropdownMenu(expanded = employeeMenu, onDismissRequest = { employeeMenu = false }) {
                                employees.forEach { employee ->
                                    DropdownMenuItem(
                                        text = { Text("${employee.fullName} (${employee.employeeNumber})") },
                                        onClick = { selectedEmployee = employee; employeeMenu = false }
                                    )
                                }
                            }
                            OutlinedTextField(returnAmount, { returnAmount = it }, Modifier.fillMaxWidth(), label = { Text("Amount (KES)") }, singleLine = true)
                            OutlinedTextField(returnReference, { returnReference = it }, Modifier.fillMaxWidth(), label = { Text("Receipt/reference") }, singleLine = true)
                            OutlinedTextField(returnReason, { returnReason = it }, Modifier.fillMaxWidth(), label = { Text("Reason") }, minLines = 2)
                            Button(
                                enabled = !busy && selectedEmployee != null && returnAmount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && returnReference.trim().length >= 3 && returnReason.trim().length >= 5,
                                onClick = {
                                    val employee = selectedEmployee ?: return@Button
                                    val amount = returnAmount.toDoubleOrNull() ?: return@Button
                                    busy = true; error = null; notice = null
                                    scope.launch {
                                        try {
                                            repository.recordCashReturn(accessToken, employee.id, amount, returnReference, returnReason)
                                            notice = "Cash return recorded. No transfer was sent."
                                            returnAmount = ""; returnReference = ""; returnReason = ""
                                        } catch (e: Exception) { error = e.message ?: "Could not record cash return." }
                                        finally { busy = false }
                                    }
                                }
                            ) { Text("Record cash return") }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rebuild reconciliation", style = MaterialTheme.typography.titleLarge)
                            OutlinedButton(onClick = { employeeMenu = true }, enabled = employees.isNotEmpty()) {
                                Text(selectedEmployee?.let { "${it.fullName} (${it.employeeNumber})" } ?: "Choose employee")
                            }
                            OutlinedTextField(periodStart, { periodStart = it }, Modifier.fillMaxWidth(), label = { Text("Period start (YYYY-MM-DD)") }, singleLine = true)
                            OutlinedTextField(periodEnd, { periodEnd = it }, Modifier.fillMaxWidth(), label = { Text("Period end (YYYY-MM-DD)") }, singleLine = true)
                            Button(enabled = !busy && selectedEmployee != null, onClick = {
                                val employee = selectedEmployee ?: return@Button
                                busy = true; error = null; notice = null
                                scope.launch {
                                    try {
                                        val result = repository.rebuildReconciliation(accessToken, employee.id, periodStart, periodEnd)
                                        notice = "Reconciliation rebuilt. Outstanding: KES ${"%,.2f".format(result.optDouble("outstanding_balance", 0.0))}. Period remains open."
                                    } catch (e: Exception) { error = e.message ?: "Could not rebuild reconciliation." }
                                    finally { busy = false }
                                }
                            }) { Text("Rebuild period") }
                        }
                    }
                }
            }
        }
    }
}
