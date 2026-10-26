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
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_APK_BYTES = 100 * 1024 * 1024

data class UploadResult(val apkPath: String, val sizeBytes: Long)

/**
 * Uploads an APK to the Cloudflare Worker (/upload-apk) with the admin's Firebase ID token.
 * The Worker checks admins/{uid} again, so this client-side flow is not the only gate.
 */
@Singleton
class ApkUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    private val http = OkHttpClient.Builder()
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    suspend fun upload(
        uri: Uri,
        packageName: String,
        versionCode: Int,
        onProgress: (Float) -> Unit
    ): UploadResult = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: throw IllegalStateException("লগইন প্রয়োজন")
        val idToken = user.getIdToken(false).await().token
            ?: throw IllegalStateException("লগইন টোকেন পাওয়া যায়নি")

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("ফাইল পড়া যায়নি")
        if (bytes.isEmpty()) throw IllegalStateException("ফাইলটি খালি")
        if (bytes.size > MAX_APK_BYTES) throw IllegalStateException("APK ১০০ MB এর বেশি হতে পারবে না")

        val request = Request.Builder()
            .url("${BuildConfig.WORKER_BASE_URL}/upload-apk")
            .header("Authorization", "Bearer $idToken")
            .header("X-Package-Name", packageName)
            .header("X-Version-Code", versionCode.toString())
            .post(ProgressBody(bytes, onProgress))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("error") }.getOrNull()
                throw IllegalStateException(message ?: "আপলোড ব্যর্থ (HTTP ${resp.code})")
            }
            val json = JSONObject(text)
            UploadResult(json.getString("apkPath"), json.getLong("sizeBytes"))
        }
    }
}

/** Writes the bytes in chunks and reports progress from 0f to 1f. */
private class ProgressBody(
    private val bytes: ByteArray,
    private val onProgress: (Float) -> Unit
) : RequestBody() {
    override fun contentType() = "application/vnd.android.package-archive".toMediaType()

    override fun contentLength(): Long = bytes.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val chunk = 64 * 1024
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + chunk, bytes.size)
            sink.write(bytes, offset, end - offset)
            offset = end
            onProgress(offset.toFloat() / bytes.size)
        }
    }
}
