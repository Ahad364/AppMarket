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
private const val MAX_SLIDER_BYTES = 3 * 1024 * 1024

/** Uploads images to the Worker: app icons and home-screen slider banners. */
@Singleton
class IconUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    private val http = OkHttpClient.Builder()
        .writeTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .build()

    /** Uploads one slider banner (16:9). Returns the public image URL. */
    suspend fun uploadSlider(uri: Uri, sliderId: String): String = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("ছবি পড়া যায়নি")
        if (bytes.isEmpty() || bytes.size > MAX_SLIDER_BYTES) {
            throw IllegalStateException("স্লাইডার ছবি খালি বা ৩ MB এর বেশি")
        }

        val mime = context.contentResolver.getType(uri) ?: "image/png"
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/upload-slider")
            .header("Authorization", "Bearer $idToken")
            .header("X-Slider-Id", sliderId)
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw IllegalStateException(message ?: "স্লাইডার আপলোড ব্যর্থ (HTTP ${resp.code})")
            }
            JSONObject(text).getString("sliderUrl")
        }
    }

    /** Removes the slider image file from R2. The database record is removed separately. */
    suspend fun deleteSlider(sliderId: String) = withContext(Dispatchers.IO) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("লগইন প্রয়োজন")
        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/delete-slider")
            .header("Authorization", "Bearer $idToken")
            .header("X-Slider-Id", sliderId)
            .post(ByteArray(0).toRequestBody(null))
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("স্লাইডার ছবি মুছতে ব্যর্থ (HTTP ${resp.code})")
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
