package com.appmarket.store.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MyAppsScreen(
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit,
    vm: MyAppsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateCompat()
    val shown = if (state.tab == 0) {
        state.apps.filter { it.id in state.favorites }
    } else {
        state.apps.filter { vm.isInstalled(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← ফিরে যান") }
        Text(
            "আমার অ্যাপ",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.tab == 0,
                onClick = { vm.selectTab(0) },
                label = { Text("পছন্দের") }
            )
            FilterChip(
                selected = state.tab == 1,
                onClick = { vm.selectTab(1) },
                label = { Text("ইনস্টল করা") }
            )
        }
        Spacer(Modifier.height(12.dp))

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading && state.apps.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            shown.isEmpty() -> Text(
                if (state.tab == 0) {
                    "এখনও কোনো পছন্দের অ্যাপ নেই।"
                } else {
                    "এই ডিভাইসে কোনো AppMarket অ্যাপ ইনস্টল করা নেই।"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.id }) { app ->
                    AppCard(
                        app = app,
                        updateAvailable = vm.updateAvailable(app),
                        onClick = { onOpenApp(app.id) }
                    )
                }
            }
        }
    }
}
