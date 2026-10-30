package com.appmarket.admin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.hilt.navigation.compose.hiltViewModel
import com.appmarket.admin.model.AdminApp
import com.appmarket.admin.model.AdminSlider
import com.appmarket.admin.model.AdminUser

private val APP_CATEGORIES = listOf("Productivity", "Games", "Social", "Tools", "Education")

private val APK_MIME_TYPES = arrayOf("application/vnd.android.package-archive", "application/octet-stream")

@Composable
fun AdminHomeScreen(
    onSignedOut: () -> Unit,
    onAddApp: () -> Unit = {},
    vm: AdminHomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateCompat()
    var pendingDelete by remember { mutableStateOf<AdminApp?>(null) }
    var editing by remember { mutableStateOf<AdminApp?>(null) }
    var uploadTarget by remember { mutableStateOf<AdminApp?>(null) }
    var pendingUpload by remember { mutableStateOf<Pair<AdminApp, Uri>?>(null) }
    var notifyOpen by remember { mutableStateOf(false) }
    var sliderOpen by remember { mutableStateOf(false) }
    // The app chosen in the slider dialog, kept until the image picker returns.
    var sliderAppId by remember { mutableStateOf("") }

    val sliderPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) vm.addSlider(uri, sliderAppId)
    }

    val apkPicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        val target = uploadTarget
        if (uri != null && target != null) pendingUpload = target to uri
    }

    // Reload when coming back from the "new app" screen.
    LaunchedEffect(Unit) {
        vm.refresh()
    }

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }

    // Search by name or email, optionally only banned users.
    val query = state.userQuery.trim().lowercase()
    val visibleUsers = state.users.filter { user ->
        (!state.bannedOnly || user.banned) &&
            (query.isEmpty() || user.name.lowercase().contains(query) || user.email.lowercase().contains(query))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
                .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "অ্যাডমিন প্যানেল",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    "AppMarket · ব্যবস্থাপনা",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            IconButton(onClick = vm::refresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
            TextButton(onClick = vm::signOut) {
                Text("লগ আউট", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("ব্যবহারকারী", state.users.size, Modifier.weight(1f))
            StatCard("নিষিদ্ধ", state.users.count { it.banned }, Modifier.weight(1f))
            StatCard("অ্যাপ", state.apps.size, Modifier.weight(1f))
            StatCard("লুকানো", state.apps.count { !it.approved }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = state.tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            Tab(
                selected = state.tab == 0,
                onClick = { vm.selectTab(0) },
                text = { Text("ব্যবহারকারী", style = MaterialTheme.typography.labelLarge) }
            )
            Tab(
                selected = state.tab == 1,
                onClick = { vm.selectTab(1) },
                text = { Text("অ্যাপ", style = MaterialTheme.typography.labelLarge) }
            )
        }
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onAddApp,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("নতুন অ্যাপ", style = MaterialTheme.typography.labelLarge) }
            OutlinedButton(
                onClick = { sliderOpen = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("স্লাইডার", style = MaterialTheme.typography.labelLarge) }
            OutlinedButton(
                onClick = { notifyOpen = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("নোটিফিকেশন", style = MaterialTheme.typography.labelLarge) }
        }
        Spacer(Modifier.height(12.dp))

        if (state.tab == 0) {
            OutlinedTextField(
                value = state.userQuery,
                onValueChange = vm::onUserQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("নাম বা ইমেইল দিয়ে খুঁজুন") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            Spacer(Modifier.height(8.dp))
            FilterChip(
                selected = state.bannedOnly,
                onClick = vm::toggleBannedOnly,
                label = { Text("শুধু নিষিদ্ধ") }
            )
            Spacer(Modifier.height(12.dp))
        }

        if (state.uploading) {
            LinearProgressIndicator(
                progress = { state.uploadProgress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "APK আপলোড হচ্ছে... ${(state.uploadProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        state.notice?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

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
            state.tab == 0 -> when {
                state.users.isEmpty() -> EmptyText("কোনো ব্যবহারকারী পাওয়া যায়নি।")
                visibleUsers.isEmpty() -> EmptyText("কোনো ব্যবহারকারী মিলছে না।")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visibleUsers, key = { it.uid }) { user ->
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
                            uploading = state.uploading,
                            onEdit = { editing = app },
                            onUploadApk = {
                                uploadTarget = app
                                apkPicker.launch(APK_MIME_TYPES)
                            },
                            featured = app.featured,
                            onToggleFeatured = { vm.toggleFeatured(app) },
                            onToggleVisible = { vm.setApproved(app.id, !app.approved) },
                            onDelete = { pendingDelete = app }
                        )
                    }
                }
            }
        }
    }

    editing?.let { app ->
        EditAppDialog(
            app = app,
            onDismiss = { editing = null },
            onSave = { name, description, category, versionName, versionCode ->
                vm.updateApp(app, name, description, category, versionName, versionCode)
                editing = null
            }
        )
    }

    pendingUpload?.let { (app, uri) ->
        UploadApkDialog(
            app = app,
            onDismiss = { pendingUpload = null },
            onConfirm = { versionName, versionCode, notify ->
                vm.uploadApk(app, uri, versionName, versionCode, notify)
                pendingUpload = null
            }
        )
    }

    if (sliderOpen) {
        SliderDialog(
            sliders = state.sliders,
            apps = state.apps,
            onPickImage = { appId ->
                sliderAppId = appId
                sliderPicker.launch("image/*")
            },
            onDelete = { vm.deleteSlider(it) },
            onDismiss = { sliderOpen = false }
        )
    }

    if (notifyOpen) {
        NotifyDialog(
            onDismiss = { notifyOpen = false },
            onSend = { title, body ->
                vm.sendNotification(title, body)
                notifyOpen = false
            }
        )
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
private fun SliderDialog(
    sliders: List<AdminSlider>,
    apps: List<AdminApp>,
    onPickImage: (String) -> Unit,
    onDelete: (AdminSlider) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAppId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("স্লাইডার ব্যানার") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "ইউজার অ্যাপের হোমে সার্চ বারের নিচে অটো-স্লাইড হবে। ১৬:৯ ছবি দিন (যেমন 1280×720), ৩ MB-এর কম, PNG, JPEG বা WebP।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "ছবিতে ট্যাপ করলে যে অ্যাপ খুলবে:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedAppId.isEmpty(), onClick = { selectedAppId = "" })
                    Text("কোনো অ্যাপ নয়", style = MaterialTheme.typography.bodyMedium)
                }
                apps.forEach { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAppId == app.id, onClick = { selectedAppId = app.id })
                        Text(app.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = { onPickImage(selectedAppId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("নতুন স্লাইডার ছবি যোগ করুন") }

                Spacer(Modifier.height(4.dp))
                Text(
                    "বর্তমান স্লাইডার (${sliders.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (sliders.isEmpty()) {
                    Text(
                        "এখনও কোনো স্লাইডার নেই।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                sliders.forEach { slider ->
                    val linked = apps.find { it.id == slider.appId }?.name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = slider.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(96.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(
                                linked ?: "কোনো অ্যাপ নয়",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        TextButton(onClick = { onDelete(slider) }) {
                            Text("মুছুন", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )
}

@Composable
private fun NotifyDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val valid = title.isNotBlank() && body.isNotBlank() && title.length <= 65 && body.length <= 240

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("সবাইকে নোটিফিকেশন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "সব ব্যবহারকারীর ফোনে এই বার্তা যাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("শিরোনাম (সর্বোচ্চ ৬৫)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("বার্তা (সর্বোচ্চ ২৪০)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSend(title.trim(), body.trim()) }) {
                Text("পাঠান")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun UploadApkDialog(
    app: AdminApp,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Boolean) -> Unit
) {
    var versionName by remember(app.id) { mutableStateOf(app.versionName) }
    var versionCodeText by remember(app.id) { mutableStateOf(app.versionCode.toString()) }
    var notify by remember(app.id) { mutableStateOf(true) }

    val versionCode = versionCodeText.toIntOrNull()
    val valid = versionName.isNotBlank() && versionCode != null && versionCode > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("APK আপলোড") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "\"${app.name}\" (${app.packageName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "এই ভার্সন তথ্য APK-এর সাথে মিলিয়ে দিন। ভুল হলে ইনস্টল ও আপডেট বার্তা ভুল দেখাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notify, onCheckedChange = { notify = it })
                    Text("নতুন ভার্সনের নোটিফিকেশন পাঠান", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("ভার্সন নাম (যেমন 1.2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = versionCodeText,
                    onValueChange = { versionCodeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ভার্সন কোড (সংখ্যা)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(versionName.trim(), versionCode ?: 0, notify) }
            ) { Text("আপলোড") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun EditAppDialog(
    app: AdminApp,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int) -> Unit
) {
    var name by remember(app.id) { mutableStateOf(app.name) }
    var description by remember(app.id) { mutableStateOf(app.description) }
    var category by remember(app.id) { mutableStateOf(app.category) }
    var versionName by remember(app.id) { mutableStateOf(app.versionName) }
    var versionCodeText by remember(app.id) { mutableStateOf(app.versionCode.toString()) }

    val versionCode = versionCodeText.toIntOrNull()
    val valid = name.isNotBlank() && versionName.isNotBlank() && versionCode != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("অ্যাপ এডিট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিবরণ") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "ক্যাটাগরি",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    APP_CATEGORIES.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) }
                        )
                    }
                }
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("ভার্সন নাম (যেমন 1.2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = versionCodeText,
                    onValueChange = { versionCodeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ভার্সন কোড (সংখ্যা)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(name.trim(), description.trim(), category, versionName.trim(), versionCode ?: 0)
                }
            ) { Text("সংরক্ষণ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier) {
    AdminCard(modifier = modifier) {
        Text(
            "$value",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    AdminCard {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun InitialAvatar(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
private fun UserRow(user: AdminUser, isSelf: Boolean, onToggleBan: () -> Unit) {
    val statusColor = if (user.banned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(text = user.name.ifBlank { "?" }, color = statusColor)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
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
                Spacer(Modifier.height(6.dp))
                StatusPill(
                    if (user.banned) "নিষিদ্ধ" else "সক্রিয়",
                    statusColor
                )
            }
            if (isSelf) {
                StatusPill("আপনি", MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun AppRow(
    app: AdminApp,
    uploading: Boolean,
    featured: Boolean,
    onEdit: () -> Unit,
    onUploadApk: () -> Unit,
    onToggleFeatured: () -> Unit,
    onToggleVisible: () -> Unit,
    onDelete: () -> Unit
) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconBox(url = app.iconUrl, size = 52.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    "v${app.versionName} (${app.versionCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(
                        if (app.approved) "দৃশ্যমান" else "লুকানো",
                        if (app.approved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (featured) {
                        StatusPill("★ ফিচার্ড", MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("এডিট", style = MaterialTheme.typography.labelLarge) }
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onToggleFeatured,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (featured) "★ ফিচার্ড" else "ফিচার্ড করুন",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Button(
                onClick = onUploadApk,
                enabled = !uploading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("APK আপলোড", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
