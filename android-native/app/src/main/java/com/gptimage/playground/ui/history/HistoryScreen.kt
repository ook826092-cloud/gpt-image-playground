package com.gptimage.playground.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gptimage.playground.R
import com.gptimage.playground.data.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val items by viewModel.history.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (items.isNotEmpty()) {
                    OutlinedButton(onClick = { showClearConfirm = true }) {
                        Text(stringResource(R.string.history_clear))
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.history_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.metadata.id }) { item ->
                        HistoryCard(
                            item = item,
                            dateFormatter = dateFormatter,
                            onDelete = { pendingDeleteId = item.metadata.id }
                        )
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.history_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearConfirm = false
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    dateFormatter: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val meta = item.metadata
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            val thumb = item.thumbnailPath
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.size(12.dp))
            } else {
                Surface(
                    Modifier.size(72.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
                Spacer(Modifier.size(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    dateFormatter.format(Date(meta.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    meta.prompt.take(120).ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelChip(text = if (meta.isGenerate) "generate" else "edit")
                    Spacer(Modifier.size(6.dp))
                    LabelChip(text = meta.model)
                    Spacer(Modifier.size(6.dp))
                    LabelChip(text = "${meta.images.size} img")
                    if (meta.durationMs > 0) {
                        Spacer(Modifier.size(6.dp))
                        LabelChip(text = "${meta.durationMs / 1000}s")
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
            }
        }
    }
}

@Composable
private fun LabelChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
