package com.appmarket.store.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appmarket.store.data.StoreRepository
import com.appmarket.store.model.DownloadRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    repo: StoreRepository
) : ViewModel() {
    val downloads: StateFlow<List<DownloadRecord>> = repo.downloadsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit,
    vm: DownloadsViewModel = hiltViewModel()
) {
    val list by vm.downloads.collectAsStateCompat()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text(tr("← ফিরে যান", "← Back")) }
        Text(
            tr("আমার ডাউনলোড", "My downloads"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        if (list.isEmpty()) {
            Text(
                tr("এখনো কোনো অ্যাপ ডাউনলোড করা হয়নি", "No apps downloaded yet"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.appId }) { item ->
                    MarketCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenApp(item.appId) }
                    ) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${item.packageName} • v${item.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatDownloadDate(item.atMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDownloadDate(millis: Long): String {
    if (millis <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(millis))
}
