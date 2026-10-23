package com.appmarket.store.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.appmarket.store.model.AppItem
import java.util.Locale

/** Thin alias so screens read StateFlows consistently. */
@Composable
fun <T> StateFlow<T>.collectAsStateCompat() = collectAsState()

@Composable
fun AppCard(app: AppItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = app.iconUrl.ifBlank { null },
                contentDescription = app.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleMedium)
                Text(app.developer, style = MaterialTheme.typography.bodySmall)
                RatingRow(app.averageRating, app.ratingCount)
            }
        }
    }
}

@Composable
fun RatingRow(average: Float, count: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (count == 0L) "New" else String.format(Locale.US, "%.1f (%d)", average, count),
            fontSize = 12.sp
        )
    }
}

/** Tappable 1..5 star picker. */
@Composable
fun StarPicker(value: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { i ->
            Box(modifier = Modifier.clickable { onChange(i) }) {
                Icon(
                    imageVector = if (i <= value) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "$i stars",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
