package com.appmarket.store.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    onOpenApp: (String) -> Unit,
    onOpenProfile: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateCompat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("AppMarket", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = vm::refresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
            IconButton(onClick = onOpenProfile) { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("অ্যাপ খুঁজুন") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.categories.forEach { c ->
                FilterChip(
                    selected = state.category == c,
                    onClick = { vm.onCategory(c) },
                    label = { Text(c) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.loading && state.apps.isEmpty() -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> Text("ত্রুটি: ${state.error}", color = MaterialTheme.colorScheme.error)
            state.apps.isEmpty() -> Text("কোনো অ্যাপ পাওয়া যায়নি।")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.apps, key = { it.id }) { app ->
                    AppCard(app = app, onClick = { onOpenApp(app.id) })
                }
            }
        }
    }
}
