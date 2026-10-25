package com.appmarket.store.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import com.appmarket.store.model.AppItem
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Full-width-style 16:9 banner for a featured app that has a banner image. */
@Composable
private fun FeaturedBanner(app: AppItem, onClick: () -> Unit) {
    AsyncImage(
        model = app.bannerUrl,
        contentDescription = app.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(300.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FeaturedCard(app: AppItem, onClick: () -> Unit) {
    MarketCard(modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        AppIcon(url = app.iconUrl, name = app.name, size = 64.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            app.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            app.category,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AppMarket",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "সেরা অ্যাপ খুঁজুন ও ইনস্টল করুন",
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
            IconButton(onClick = onOpenProfile) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("অ্যাপ খুঁজুন") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vm.categories.forEach { c ->
                FilterChip(
                    selected = state.category == c,
                    onClick = { vm.onCategory(c) },
                    label = { Text(c) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (state.featured.isNotEmpty()) {
            Text(
                "ফিচার্ড",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.featured, key = { it.id }) { app ->
                    if (app.bannerUrl.isNotBlank()) {
                        FeaturedBanner(app = app, onClick = { onOpenApp(app.id) })
                    } else {
                        FeaturedCard(app = app, onClick = { onOpenApp(app.id) })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(
            "সব অ্যাপ",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        when {
            state.loading && state.apps.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Text(
                "ত্রুটি: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            state.apps.isEmpty() -> Text(
                "কোনো অ্যাপ পাওয়া যায়নি।",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.apps, key = { it.id }) { app ->
                    AppCard(
                        app = app,
                        updateAvailable = vm.updateAvailable(app),
                        onClick = { onOpenApp(app.id) }
                    )
                }
                // "Load more" only for the plain listing; search returns its own results.
                if (state.query.isBlank() && state.apps.size >= state.limit) {
                    item {
                        OutlinedButton(
                            onClick = vm::loadMore,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("আরও দেখুন") }
                    }
                }
            }
        }
    }
}
