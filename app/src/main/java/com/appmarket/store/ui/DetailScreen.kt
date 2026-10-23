package com.appmarket.store.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.appmarket.store.install.ApkInstaller
import com.appmarket.store.model.Review
import java.text.DateFormat
import java.util.Date

@Composable
fun DetailScreen(onBack: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateCompat()
    val reviews by vm.reviews.collectAsStateCompat()
    val ctx = LocalContext.current
    val app = state.app

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← ফিরে যান") }
        Spacer(Modifier.height(12.dp))

        if (app == null) {
            CircularProgressIndicator()
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = app.iconUrl.ifBlank { null },
                contentDescription = app.name,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(app.name, style = MaterialTheme.typography.headlineSmall)
                Text(app.developer, style = MaterialTheme.typography.bodyMedium)
                RatingRow(app.averageRating, app.ratingCount)
            }
        }

        Spacer(Modifier.height(16.dp))
        InstallSection(
            state = state.install,
            onDownload = { vm.download(app) },
            onInstall = { file ->
                if (ApkInstaller.canInstallPackages(ctx)) {
                    ApkInstaller.install(ctx, file)
                } else {
                    ctx.startActivity(ApkInstaller.unknownSourcesSettingsIntent(ctx))
                }
            }
        )

        Spacer(Modifier.height(16.dp))
        Text(app.description, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Version ${app.versionName} · ${app.sizeBytes / (1024 * 1024)} MB · ${app.category}",
            fontSize = 12.sp
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Text("রিভিউ ও রেটিং", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ReviewForm(
            error = state.reviewError,
            saved = state.reviewSaved,
            onSubmit = { rating, text -> vm.submitReview(rating, text) }
        )

        Spacer(Modifier.height(12.dp))
        reviews.forEach { ReviewItem(it) }
        if (reviews.isEmpty()) Text("এখনও কোনো রিভিউ নেই।", fontSize = 13.sp)
    }
}

@Composable
private fun InstallSection(
    state: InstallState,
    onDownload: () -> Unit,
    onInstall: (java.io.File) -> Unit
) {
    when (state) {
        InstallState.Idle -> Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("ইনস্টল করুন") }
        is InstallState.Downloading -> Column {
            Text("ডাউনলোড হচ্ছে… ${(state.progress * 100).toInt()}%")
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        }
        is InstallState.ReadyToInstall -> Button(
            onClick = { onInstall(state.file) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("এখন ইনস্টল করুন") }
        is InstallState.Failed -> Column {
            Text("ব্যর্থ: ${state.message}", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("আবার চেষ্টা করুন") }
        }
    }
}

@Composable
private fun ReviewForm(error: String?, saved: Boolean, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StarPicker(value = rating, onChange = { rating = it })
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("আপনার মতামত লিখুন (ঐচ্ছিক)") },
            maxLines = 4
        )
        Button(
            onClick = { onSubmit(rating, text) },
            enabled = rating in 1..5,
            modifier = Modifier.fillMaxWidth()
        ) { Text("রিভিউ জমা দিন") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        if (saved) Text("রিভিউ সংরক্ষিত হয়েছে।", fontSize = 13.sp)
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(review.userName, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.size(8.dp))
            Text("★ ${review.rating}", fontSize = 13.sp)
        }
        if (review.text.isNotBlank()) Text(review.text, style = MaterialTheme.typography.bodyMedium)
        Text(
            DateFormat.getDateInstance().format(Date(review.createdAtMillis)),
            fontSize = 11.sp
        )
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
