package com.appmarket.admin.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appmarket.admin.model.AdminApp
import com.appmarket.admin.model.AdminUser

@Composable
fun AdminHomeScreen(onSignedOut: () -> Unit, vm: AdminHomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateCompat()
    var pendingDelete by remember { mutableStateOf<AdminApp?>(null) }

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "অ্যাডমিন প্যানেল",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "AppMarket",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = vm::refresh) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = vm::signOut) {
                Text("লগ আউট", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("ব্যবহারকারী", state.users.size, Modifier.weight(1f))
            StatCard("নিষিদ্ধ", state.users.count { it.banned }, Modifier.weight(1f))
            StatCard("অ্যাপ", state.apps.size, Modifier.weight(1f))
            StatCard("লুকানো", state.apps.count { !it.approved }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.tab == 0,
                onClick = { vm.selectTab(0) },
                label = { Text("ব্যবহারকারী") }
            )
            FilterChip(
                selected = state.tab == 1,
                onClick = { vm.selectTab(1) },
                label = { Text("অ্যাপ") }
            )
        }
        Spacer(Modifier.height(12.dp))

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading && state.users.isEmpty() && state.apps.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.tab == 0 -> if (state.users.isEmpty()) {
                EmptyText("কোনো ব্যবহারকারী পাওয়া যায়নি।")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.users, key = { it.uid }) { user ->
                        UserRow(
                            user = user,
                            isSelf = user.uid == state.currentUid,
                            onToggleBan = { vm.setBanned(user.uid, !user.banned) }
                        )
                    }
                }
            }
            else -> if (state.apps.isEmpty()) {
                EmptyText("এখনও কোনো অ্যাপ নেই।")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.apps, key = { it.id }) { app ->
                        AppRow(
                            app = app,
                            onToggleVisible = { vm.setApproved(app.id, !app.approved) },
                            onDelete = { pendingDelete = app }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("অ্যাপ মুছে ফেলবেন?") },
            text = {
                Text("\"${app.name}\" এবং এর সব রিভিউ স্থায়ীভাবে মুছে যাবে। এটি ফেরানো যাবে না।")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteApp(app.id)
                    pendingDelete = null
                }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier) {
    AdminCard(modifier = modifier) {
        Text(
            "$value",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun UserRow(user: AdminUser, isSelf: Boolean, onToggleBan: () -> Unit) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name.ifBlank { "নামহীন" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (user.banned) "নিষিদ্ধ" else "সক্রিয়",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.banned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            if (isSelf) {
                Text(
                    "আপনি",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedButton(onClick = onToggleBan, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        if (user.banned) "আনব্যান" else "ব্যান",
                        color = if (user.banned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AdminApp, onToggleVisible: () -> Unit, onDelete: () -> Unit) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconBox(url = app.iconUrl, size = 48.dp)
            Spacer(Modifier.height(0.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${app.packageName} · v${app.versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (app.approved) "দৃশ্যমান" else "লুকানো",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.approved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onToggleVisible,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (app.approved) "লুকান" else "দেখান", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "মুছুন",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
