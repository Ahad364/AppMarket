package com.appmarket.admin.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val label: String
)

/** Reads the package name, version and app name from an APK, using the system's own parser. */
@Singleton
class ApkInspector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun inspect(uri: Uri): ApkInfo = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "inspect.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("ফাইল পড়া যায়নি")

        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(tmp.absolutePath, 0)
            ?: throw IllegalStateException("এটি বৈধ APK ফাইল নয়")

        // The label is read from the APK itself, so the source path must be set first.
        val label = info.applicationInfo?.let { ai ->
            ai.sourceDir = tmp.absolutePath
            ai.publicSourceDir = tmp.absolutePath
            ai.loadLabel(pm).toString()
        } ?: info.packageName

        ApkInfo(
            packageName = info.packageName,
            versionName = info.versionName.orEmpty(),
            versionCode = info.longVersionCode.toInt(),
            label = label
        )
    }
}
