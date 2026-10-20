package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ManualReviewItem
import com.example.data.SupabaseManualReviewRepository
import kotlinx.coroutines.launch

@Composable
fun ManualReviewQueueScreen(
    accessToken: String,
    onClose: () -> Unit
) {
    val repository = remember { SupabaseManualReviewRepository() }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ManualReviewItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val reasons = remember { mutableStateMapOf<String, String>() }
    val resolving = remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try { items = repository.listOpen(accessToken) }
            catch (e: Exception) { error = e.message ?: "Unable to load review queue." }
            finally { loading = false }
        }
    }
    LaunchedEffect(accessToken) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Invoice manual review", style = MaterialTheme.typography.headlineSmall)
                Text("Manual review never marks an invoice externally verified.", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClose) { Text("Close") }
        }
        Spacer(Modifier.height(12.dp))
        if (notice != null) Text(notice.orEmpty(), color = MaterialTheme.colorScheme.primary)
        if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::reload) { Text("Retry") }
        }
        if (loading) {
            CircularProgressIndicator()
        } else if (error == null && items.isEmpty()) {
            Text("No open manual-review items.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Expense \${item.expenseId}", style = MaterialTheme.typography.titleMedium)
                            Text("Reason: \${item.reasonCode}", style = MaterialTheme.typography.bodyMedium)
                            if (!item.reasonDetail.isNullOrBlank()) Text(item.reasonDetail!!, style = MaterialTheme.typography.bodySmall)
                            Text("Status: \${item.status}", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = reasons[item.id].orEmpty(),
                                onValueChange = { reasons[item.id] = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Review notes (required)") },
                                minLines = 2
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = resolving.value == null && reasons[item.id].orEmpty().trim().length >= 5,
                                    onClick = {
                                        resolving.value = item.id
                                        notice = null
                                        scope.launch {
                                            try {
                                                repository.resolve(accessToken, item.id, "not_verified", reasons[item.id].orEmpty())
                                                notice = "Review saved: invoice not verified."
                                                reload()
                                            } catch (e: Exception) {
                                                error = e.message ?: "Could not save review."
                                            } finally { resolving.value = null }
                                        }
                                    }
                                ) { Text(if (resolving.value == item.id) "Saving…" else "Not verified") }
                                OutlinedButton(
                                    enabled = resolving.value == null && reasons[item.id].orEmpty().trim().length >= 5,
                                    onClick = {
                                        resolving.value = item.id
                                        notice = null
                                        scope.launch {
                                            try {
                                                repository.resolve(accessToken, item.id, "needs_review", reasons[item.id].orEmpty())
                                                notice = "Review saved: additional review required."
                                                reload()
                                            } catch (e: Exception) {
                                                error = e.message ?: "Could not save review."
                                            } finally { resolving.value = null }
                                        }
                                    }
                                ) { Text("Needs review") }
                            }
                        }
                    }
                }
            }
        }
    }
}
