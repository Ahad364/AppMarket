package com.appmarket.store.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appmarket.store.BuildConfig

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
    var newName by remember(name) { mutableStateOf(name) }
    val trimmed = newName.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text(tr("← ফিরে যান", "← Back")) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (profile == null) tr("লোড হচ্ছে…", "Loading…") else name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Change name
        MarketCard {
            Text(
                tr("নাম পরিবর্তন", "Change name"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(tr("নতুন নাম", "New name")) },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.updateName(trimmed) },
                enabled = !saving && trimmed.isNotEmpty() && trimmed != name,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (saving) tr("সেভ হচ্ছে…", "Saving…") else tr("সেভ করুন", "Save"))
            }
            when (nameResult) {
                NameResult.SAVED -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("নাম সেভ হয়েছে", "Name saved"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                NameResult.FAILED -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("নাম সেভ করা যায়নি", "Could not save name"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                NameResult.NONE -> Unit
            }
        }

        // Account info
        MarketCard {
            Text(
                tr("অ্যাকাউন্ট", "Account"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Email",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Dark mode
        MarketCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tr("ডার্ক মোড", "Dark mode"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = isDark, onCheckedChange = vm::setDark)
            }
        }

        // Language
        MarketCard {
            Text(
                tr("ভাষা", "Language"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // About and version check
        MarketCard {
            Text(
                tr("অ্যাপ সম্পর্কে", "About"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                tr(
                    "ভার্সন ${BuildConfig.VERSION_NAME} (কোড ${BuildConfig.VERSION_CODE})",
                    "Version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})"
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.checkUpdate() },
                enabled = update.status != UpdateStatus.CHECKING,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(tr("আপডেট চেক", "Check for update")) }

            val message = when (update.status) {
                UpdateStatus.CHECKING -> tr("চেক হচ্ছে…", "Checking…")
                UpdateStatus.LATEST -> tr("আপনি সর্বশেষ ভার্সন ব্যবহার করছেন", "You are on the latest version")
                UpdateStatus.AVAILABLE -> tr(
                    "নতুন ভার্সন আছে: ${update.versionName}",
                    "New version available: ${update.versionName}"
                )
                UpdateStatus.FAILED -> tr("আপডেট চেক করা যায়নি", "Could not check for updates")
                UpdateStatus.IDLE -> ""
            }
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (update.status == UpdateStatus.AVAILABLE && update.downloadUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(tr("নতুন ভার্সন ডাউনলোড", "Download update")) }
            }
        }

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = onOpenMyApps,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(tr("আমার অ্যাপ", "My apps"), style = MaterialTheme.typography.labelLarge) }

        OutlinedButton(
            onClick = onOpenDownloads,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(tr("আমার ডাউনলোড", "My downloads"), style = MaterialTheme.typography.labelLarge) }

        OutlinedButton(
            onClick = {
                vm.signOut()
                onSignedOut()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) { Text(tr("লগ আউট", "Log out"), style = MaterialTheme.typography.labelLarge) }
    }
}
