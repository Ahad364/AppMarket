package com.appmarket.admin.data

import com.appmarket.admin.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks the Worker (/notify) to send a push to every phone subscribed to "all_users".
 * The Worker checks that the caller is an admin, so the FCM key never leaves the server.
 */
@Singleton
class NotificationSender @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val http = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    /** imageUrl is the app logo to show in the notification. Leave it empty for a plain message. */
    suspend fun sendToAllUsers(title: String, body: String, imageUrl: String = "") = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")

        val payload = JSONObject()
            .put("title", title)
            .put("body", body)
            .put("imageUrl", imageUrl)
            .toString()
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/notify")
            .header("Authorization", "Bearer $idToken")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw IllegalStateException(message ?: "নোটিফিকেশন পাঠানো যায়নি (HTTP ${resp.code})")
            }
        }
    }
}
