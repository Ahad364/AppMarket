package com.appmarket.store

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

const val NOTIFICATION_CHANNEL_ID = "appmarket_general"

/** Every phone listens to this topic. The Admin Panel sends to it through the Worker. */
const val ALL_USERS_TOPIC = "all_users"

@HiltAndroidApp
class MarketApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "সাধারণ বার্তা",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "নতুন অ্যাপ ও গুরুত্বপূর্ণ ঘোষণা" }
        )

        // Works without login. Failures are retried by Firebase automatically.
        FirebaseMessaging.getInstance().subscribeToTopic(ALL_USERS_TOPIC)
    }
}
