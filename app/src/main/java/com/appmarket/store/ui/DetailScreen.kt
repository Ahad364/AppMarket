package com.appmarket.store.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appmarket.store.install.ApkInstaller
import com.appmarket.store.model.Review
import java.text.DateFormat
import java.util.Date

@Composable
fun DetailScreen(onBack: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateCompat()
    val reviews by vm.reviews.collectAsStateCompat()
    val isFavorite by vm.isFavorite.collectAsStateCompat()
    val ctx = LocalContext.current
    val app = state.app

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← ফিরে যান") }
        Spacer(Modifier.height(4.dp))

        if (app == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val installedVersion = vm.installedVersion(app.packageName)
        val updateAvail = vm.updateAvailable(app)
        val idleLabel = when {
            installedVersion == null -> "ইনস্টল করুন"
            updateAvail -> "আপডেট করুন"
            else -> "আবার ইনস্টল করুন"
        }

        // Screenshot gallery (admin-uploaded)
        if (app.screenshots.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                app.screenshots.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(130.dp)
                            .height(230.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        MarketCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(url = app.iconUrl, name = app.name, size = 88.dp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        app.developer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    RatingRow(app.averageRating, app.ratingCount)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.toggleFavorite(!isFavorite) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (isFavorite) "★ পছন্দে আছে" else "☆ পছন্দ করুন",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = { shareApp(ctx, app.name, app.id) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("শেয়ার করুন", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(18.dp))

            if (installedVersion != null) {
                Text(
                    if (updateAvail) {
                        "নতুন ভার্সন পাওয়া যাচ্ছে (আপনার ভার্সন: $installedVersion)"
                    } else {
                        "এই ডিভাইসে ইনস্টল করা আছে"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (updateAvail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            InstallSection(
                state = state.install,
                idleLabel = idleLabel,
                onDownload = { vm.download(app) },
                onInstall = { file ->
                    if (ApkInstaller.canInstallPackages(ctx)) {
                        ApkInstaller.install(ctx, file)
                    } else {
                        ctx.startActivity(ApkInstaller.unknownSourcesSettingsIntent(ctx))
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        MarketCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoItem("সংস্করণ", app.versionName)
                InfoItem("আকার", formatBytes(app.sizeBytes))
                InfoItem("ধরন", app.category)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "বিবরণ",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            app.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "রিভিউ ও রেটিং",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        val myReview = reviews.firstOrNull { it.uid == vm.myUid }
        // key() resets the form when the saved review changes, so the form shows the latest values.
        key(myReview?.rating, myReview?.text) {
            MarketCard {
                ReviewForm(
                    error = state.reviewError,
                    saved = state.reviewSaved,
                    initialRating = myReview?.rating ?: 0,
                    initialText = myReview?.text ?: "",
                    onSubmit = { rating, text -> vm.submitReview(rating, text) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        reviews.forEach { review ->
            ReviewItem(
                review = review,
                canDelete = state.isAdmin || review.uid == vm.myUid,
                onDelete = { vm.deleteReview(review.uid) }
            )
        }
        if (reviews.isEmpty()) {
            Text(
                "এখনও কোনো রিভিউ নেই।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shareApp(ctx: Context, name: String, appId: String) {
    // Opens a web page that forwards to the app (see Worker /app/{id}).
    val link = "${com.appmarket.store.BuildConfig.DOWNLOAD_BASE_URL}/app/$appId"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "AppMarket-এ \"$name\" অ্যাপটি দেখুন:\n$link")
    }
    ctx.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InstallSection(
    state: InstallState,
    idleLabel: String,
    onDownload: () -> Unit,
    onInstall: (java.io.File) -> Unit
) {
    val buttonShape = RoundedCornerShape(14.dp)
    when (state) {
        InstallState.Idle -> Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = buttonShape
        ) { Text(idleLabel, style = MaterialTheme.typography.labelLarge) }

        is InstallState.Downloading -> Column {
            Text(
                "ডাউনলোড হচ্ছে… ${(state.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
        }

        is InstallState.ReadyToInstall -> Button(
            onClick = { onInstall(state.file) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = buttonShape
        ) { Text("এখন ইনস্টল করুন", style = MaterialTheme.typography.labelLarge) }

        is InstallState.Failed -> Column {
            Text(
                "ব্যর্থ: ${state.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = buttonShape
            ) { Text("আবার চেষ্টা করুন", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun ReviewForm(
    error: String?,
    saved: Boolean,
    initialRating: Int,
    initialText: String,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(initialRating) }
    var text by remember { mutableStateOf(initialText) }
    val isUpdate = initialRating > 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "আপনার রেটিং",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StarPicker(value = rating, onChange = { rating = it })
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("আপনার মতামত লিখুন (ঐচ্ছিক)") },
            maxLines = 4
        )
        Button(
            onClick = { onSubmit(rating, text) },
            enabled = rating in 1..5,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (isUpdate) "রিভিউ আপডেট করুন" else "রিভিউ জমা দিন",
                style = MaterialTheme.typography.labelLarge
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (saved) {
            Text(
                "রিভিউ সংরক্ষিত হয়েছে।",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ReviewItem(review: Review, canDelete: Boolean, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    review.userName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    DateFormat.getDateInstance().format(Date(review.createdAtMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = StarColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${review.rating}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canDelete) {
                TextButton(onClick = onDelete) {
                    Text(
                        "মুছুন",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        if (review.text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                review.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
