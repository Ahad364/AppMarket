package com.appmarket.admin.data

import android.content.Context
import android.net.Uri
import com.appmarket.admin.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
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

private const val MAX_ICON_BYTES = 2 * 1024 * 1024
private const val MAX_SCREENSHOT_BYTES = 3 * 1024 * 1024
private const val MAX_BANNER_BYTES = 3 * 1024 * 1024

/** Uploads an app icon to the Worker (/upload-icon). Returns the public icon URL. */
@Singleton
class IconUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    private val http = OkHttpClient.Builder()
        .writeTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .build()

    /** Uploads (or replaces) the 16:9 banner for an app. Returns the public banner URL. */
    suspend fun uploadBanner(uri: Uri, packageName: String): String = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("ছবি পড়া যায়নি")
        if (bytes.isEmpty() || bytes.size > MAX_BANNER_BYTES) {
            throw IllegalStateException("ব্যানার খালি বা ৩ MB এর বেশি")
        }

        val mime = context.contentResolver.getType(uri) ?: "image/png"
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/upload-banner")
            .header("Authorization", "Bearer $idToken")
            .header("X-Package-Name", packageName)
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw IllegalStateException(message ?: "ব্যানার আপলোড ব্যর্থ (HTTP ${resp.code})")
            }
            JSONObject(text).getString("bannerUrl")
        }
    }

    /** Deletes the banner file in R2. The app record is cleared separately. */
    suspend fun deleteBanner(packageName: String) = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/delete-banner")
            .header("Authorization", "Bearer $idToken")
            .header("X-Package-Name", packageName)
            .post(ByteArray(0).toRequestBody(null))
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("ব্যানার মুছতে ব্যর্থ (HTTP ${resp.code})")
        }
    }

    /** Uploads one screenshot. A random token keeps old screenshot files from being overwritten. */
    suspend fun uploadScreenshot(uri: Uri, packageName: String): String = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("ছবি পড়া যায়নি")
        if (bytes.isEmpty() || bytes.size > MAX_SCREENSHOT_BYTES) {
            throw IllegalStateException("স্ক্রিনশট খালি বা ৩ MB এর বেশি")
        }

        val token = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val mime = context.contentResolver.getType(uri) ?: "image/png"
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/upload-screenshot")
            .header("Authorization", "Bearer $idToken")
            .header("X-Package-Name", packageName)
            .header("X-Shot-Id", token)
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw IllegalStateException(message ?: "স্ক্রিনশট আপলোড ব্যর্থ (HTTP ${resp.code})")
            }
            JSONObject(text).getString("screenshotUrl")
        }
    }

    suspend fun upload(uri: Uri, packageName: String): String = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("ছবি পড়া যায়নি")
        if (bytes.isEmpty() || bytes.size > MAX_ICON_BYTES) {
            throw IllegalStateException("ছবি খালি বা ২ MB এর বেশি")
        }

        val mime = context.contentResolver.getType(uri) ?: "image/png"
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/upload-icon")
            .header("Authorization", "Bearer $idToken")
            .header("X-Package-Name", packageName)
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw IllegalStateException(message ?: "আইকন আপলোড ব্যর্থ (HTTP ${resp.code})")
            }
            JSONObject(text).getString("iconUrl")
        }
    }
}
