package com.appmarket.store.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appmarket.store.model.AppItem
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Thin alias so screens read StateFlows consistently. */
@Composable
fun <T> StateFlow<T>.collectAsStateCompat() = collectAsState()

/** Rounded app icon with a soft placeholder behind it while loading. */
@Composable
fun AppIcon(url: String, name: String, size: Dp) {
    val shape = RoundedCornerShape(size * 0.25f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        AsyncImage(
            model = url.ifBlank { null },
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(size)
        )
    }
}

/** Standard white elevated surface used across all screens. */
@Composable
fun MarketCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun AppCard(app: AppItem, updateAvailable: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(url = app.iconUrl, name = app.name, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.developer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                RatingRow(app.averageRating, app.ratingCount)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.category.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        formatBytes(app.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (updateAvailable) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "আপডেট আছে",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingRow(average: Float, count: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = StarColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (count == 0L) "New" else String.format(Locale.US, "%.1f (%d)", average, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tappable 1..5 star picker. */
@Composable
fun StarPicker(value: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { i ->
            Box(modifier = Modifier.clickable { onChange(i) }) {
                Icon(
                    imageVector = if (i <= value) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "$i stars",
                    tint = if (i <= value) StarColor else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/** Human-readable APK size, e.g. "512 KB", "12.5 MB". Shows an em dash when the size is unknown. */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        else -> String.format(Locale.US, "%.0f KB", bytes / kb)
    }
}
