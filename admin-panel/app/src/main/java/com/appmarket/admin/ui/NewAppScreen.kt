package com.appmarket.admin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val NEW_APP_CATEGORIES = listOf("Productivity", "Games", "Social", "Tools", "Education")
private val APK_MIME_TYPES = arrayOf("application/vnd.android.package-archive", "application/octet-stream")

@Composable
private fun NewAppSection(title: String, content: @Composable () -> Unit) {
    AdminCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun NewAppScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: NewAppViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateCompat()

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.pickApk(uri)
    }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) vm.update(state.copy(iconUri = uri))
    }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, enabled = !state.busy) {
                Text("← ফিরে যান", style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "নতুন অ্যাপ যোগ করুন",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "APK বাছলে তথ্য স্বয়ংক্রিয়ভাবে পূরণ হবে, প্রয়োজনে বদলাতে পারবেন",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. APK
        NewAppSection("১. APK ফাইল") {
            OutlinedButton(
                onClick = { apkPicker.launch(APK_MIME_TYPES) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (state.apkUri == null) "APK ফাইল বাছুন" else "APK বদলান",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (state.apkUri != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "প্যাকেজ: ${state.packageName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "ভার্সন: ${state.versionName} (${state.versionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 2. Details (filled from the APK, editable)
        NewAppSection("২. অ্যাপের তথ্য") {
            OutlinedTextField(
                value = state.name,
                onValueChange = { vm.update(state.copy(name = it)) },
                label = { Text("অ্যাপের নাম") },
                singleLine = true,
                enabled = !state.busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.developer,
                onValueChange = { vm.update(state.copy(developer = it)) },
                label = { Text("ডেভেলপার") },
                singleLine = true,
                enabled = !state.busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { vm.update(state.copy(description = it)) },
                label = { Text("বিবরণ") },
                maxLines = 5,
                enabled = !state.busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "ক্যাটাগরি",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NEW_APP_CATEGORIES.forEach { c ->
                    FilterChip(
                        selected = state.category == c,
                        onClick = { vm.update(state.copy(category = c)) },
                        label = { Text(c) },
                        enabled = !state.busy
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.versionName,
                    onValueChange = { vm.update(state.copy(versionName = it)) },
                    label = { Text("ভার্সন নাম") },
                    singleLine = true,
                    enabled = !state.busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.versionCode,
                    onValueChange = { vm.update(state.copy(versionCode = it.filter { ch -> ch.isDigit() })) },
                    label = { Text("ভার্সন কোড") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !state.busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Icon: upload from phone, or paste a URL
        NewAppSection("৩. আইকন") {
            OutlinedButton(
                onClick = { iconPicker.launch("image/*") },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (state.iconUri == null) "ফোন থেকে ছবি বাছুন" else "ছবি বাছা হয়েছে (বদলাতে চাপুন)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            OutlinedTextField(
                value = state.iconUrlText,
                onValueChange = { vm.update(state.copy(iconUrlText = it)) },
                label = { Text("অথবা আইকন URL") },
                singleLine = true,
                enabled = !state.busy && state.iconUri == null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Notification
        NewAppSection("৪. নোটিফিকেশন") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.notify,
                    onCheckedChange = { vm.update(state.copy(notify = it)) },
                    enabled = !state.busy
                )
                Text(
                    "সেভ হলে সবাইকে নোটিফিকেশন পাঠান",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Progress and messages
        if (state.busy) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
            )
        }
        if (state.status.isNotBlank()) {
            Text(
                state.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = vm::save,
            enabled = !state.busy && state.apkUri != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("অ্যাপ যোগ করুন", style = MaterialTheme.typography.labelLarge) }
    }
}
