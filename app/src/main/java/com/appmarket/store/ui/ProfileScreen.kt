package com.appmarket.store.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appmarket.store.BuildConfig
import com.appmarket.store.R

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenMyApps: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSignedOut: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateCompat()
    val isDark by vm.isDark.collectAsStateCompat()
    val lang by vm.lang.collectAsStateCompat()
    val saving by vm.nameSaving.collectAsStateCompat()
    val nameResult by vm.nameResult.collectAsStateCompat()
    val update by vm.update.collectAsStateCompat()
    val ctx = LocalContext.current

    val name = profile?.name.orEmpty()
    val email = profile?.email.orEmpty()
    var editing by remember { mutableStateOf(false) }
    var newName by remember(name) { mutableStateOf(name) }
    val trimmed = newName.trim()

    LaunchedEffect(nameResult) {
        if (nameResult == NameResult.SAVED) editing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = onBack) { Text(tr("← ফিরে যান", "← Back")) }

        ProfileHeader(
            name = if (profile == null) tr("লোড হচ্ছে…", "Loading…") else name,
            email = email
        )

        SectionTitle(tr("অ্যাকাউন্ট", "Account"))
        SettingsGroup {
            SettingsRow(
                title = tr("নাম", "Name"),
                value = name,
                onClick = { editing = !editing },
                trailing = {
                    Text(
                        if (editing) tr("বন্ধ", "Close") else tr("বদলান", "Edit"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
            AnimatedVisibility(visible = editing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(tr("নতুন নাম", "New name")) },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { vm.updateName(trimmed) },
                            enabled = !saving && trimmed.isNotEmpty() && trimmed != name,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (saving) tr("সেভ হচ্ছে…", "Saving…") else tr("সেভ করুন", "Save"))
                        }
                        OutlinedButton(
                            onClick = {
                                newName = name
                                editing = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(tr("বাতিল", "Cancel")) }
                    }
                    val nameMessage = when (nameResult) {
                        NameResult.SAVED -> tr("নাম সেভ হয়েছে", "Name saved")
                        NameResult.FAILED -> tr("নাম সেভ করা যায়নি", "Could not save name")
                        NameResult.NONE -> ""
                    }
                    if (nameMessage.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            nameMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (nameResult == NameResult.FAILED) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow(title = "Email", value = email)
        }

        SectionTitle(tr("পছন্দ", "Preferences"))
        SettingsGroup {
            SettingsRow(
                title = tr("ডার্ক মোড", "Dark mode"),
                trailing = {
                    Switch(checked = isDark, onCheckedChange = vm::setDark)
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow(
                title = tr("ভাষা", "Language"),
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = lang == "bn",
                            onClick = { vm.setLang("bn") },
                            label = { Text("বাংলা") }
                        )
                        FilterChip(
                            selected = lang == "en",
                            onClick = { vm.setLang("en") },
                            label = { Text("English") }
                        )
                    }
                }
            )
        }

        SectionTitle(tr("আমার কার্যক্রম", "My activity"))
        SettingsGroup {
            SettingsRow(
                title = tr("আমার অ্যাপ", "My apps"),
                onClick = onOpenMyApps,
                trailing = { Chevron() }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow(
                title = tr("আমার ডাউনলোড", "My downloads"),
                onClick = onOpenDownloads,
                trailing = { Chevron() }
            )
        }

        SectionTitle(tr("অ্যাপ সম্পর্কে", "About"))
        SettingsGroup {
            SettingsRow(
                title = tr("ভার্সন", "Version"),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val checking = update.status == UpdateStatus.CHECKING
            SettingsRow(
                title = tr("আপডেট চেক", "Check for updates"),
                onClick = if (checking) null else ({ vm.checkUpdate() }),
                trailing = {
                    Text(
                        if (checking) tr("চেক হচ্ছে…", "Checking…") else tr("চেক করুন", "Check"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
            val statusText = when (update.status) {
                UpdateStatus.IDLE -> ""
                UpdateStatus.CHECKING -> ""
                UpdateStatus.LATEST -> tr("আপনি সর্বশেষ ভার্সন ব্যবহার করছেন", "You are on the latest version")
                UpdateStatus.AVAILABLE -> tr(
                    "নতুন ভার্সন আছে: ${update.versionName}",
                    "New version available: ${update.versionName}"
                )
                UpdateStatus.FAILED -> tr("আপডেট চেক করা যায়নি", "Could not check for updates")
            }
            if (statusText.isNotEmpty()) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                )
            }
            if (update.status == UpdateStatus.AVAILABLE && update.downloadUrl.isNotBlank()) {
                Button(
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(tr("নতুন ভার্সন ডাউনলোড", "Download update")) }
            }
        }

        OutlinedButton(
            onClick = {
                vm.signOut()
                onSignedOut()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) { Text(tr("লগ আউট", "Log out"), style = MaterialTheme.typography.labelLarge) }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileHeader(name: String, email: String) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.default_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            email,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String = "",
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val base = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp)
    val modifier = if (onClick != null) base.clickable(onClick = onClick) else base

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (value.isNotEmpty()) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun Chevron() {
    Text(
        "›",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
