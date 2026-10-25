package com.appmarket.store.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appmarket.store.NOTIFICATION_CHANNEL_ID
import com.appmarket.store.R
import com.appmarket.store.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The Worker sends a data-only message: title, body and imageUrl (the app logo, may be empty).
 * This service builds the notification itself, so the logo can be shown as the large icon,
 * both when the app is open and when it is in the background.
 */
class MarketMessagingService : FirebaseMessagingService() {

    private val http = OkHttpClient.Builder()
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    override fun onMessageReceived(message: RemoteMessage) {
        // Older messages may still carry a "notification" block instead of data.
        val title = message.data["title"] ?: message.notification?.title ?: return
        val body = message.data["body"] ?: message.notification?.body.orEmpty()
        val imageUrl = message.data["imageUrl"].orEmpty()

        val canPost = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
        if (!canPost) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)

        // Without an image (general message) the notification has no large icon.
        if (imageUrl.isNotBlank()) {
            loadLogo(imageUrl)?.let { builder.setLargeIcon(it) }
        }

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /** Downloads the app logo. Runs on the service's background thread. Returns null on any failure. */
    private fun loadLogo(url: String): Bitmap? {
        return runCatching {
            val request = Request.Builder().url(url).build()
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }
}
