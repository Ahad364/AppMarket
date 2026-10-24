package com.appmarket.admin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appmarket.admin.data.AdminRepository
import com.appmarket.admin.data.ApkInspector
import com.appmarket.admin.data.ApkUploader
import com.appmarket.admin.data.IconUploader
import com.appmarket.admin.data.NotificationSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewAppState(
    val apkUri: Uri? = null,
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: String = "",
    val name: String = "",
    val developer: String = "AppMarket",
    val description: String = "",
    val category: String = "Tools",
    val iconUri: Uri? = null,
    val iconUrlText: String = "",
    val notify: Boolean = true,
    val busy: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class NewAppViewModel @Inject constructor(
    private val repo: AdminRepository,
    private val inspector: ApkInspector,
    private val apkUploader: ApkUploader,
    private val iconUploader: IconUploader,
    private val notifier: NotificationSender
) : ViewModel() {

    private val _state = MutableStateFlow(NewAppState())
    val state: StateFlow<NewAppState> = _state.asStateFlow()

    fun update(newState: NewAppState) {
        _state.value = newState
    }

    /** Reads package name, version and app name from the APK and fills the form. */
    fun pickApk(uri: Uri) {
        _state.value = _state.value.copy(error = null, status = "APK পড়া হচ্ছে...")
        viewModelScope.launch {
            try {
                val info = inspector.inspect(uri)
                _state.value = _state.value.copy(
                    apkUri = uri,
                    packageName = info.packageName,
                    versionName = info.versionName,
                    versionCode = info.versionCode.toString(),
                    name = info.label,
                    status = ""
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "APK পড়া যায়নি", status = "")
            }
        }
    }

    fun save() {
        val s = _state.value
        val apkUri = s.apkUri
        val versionCode = s.versionCode.toIntOrNull()

        val problem = when {
            apkUri == null -> "প্রথমে APK ফাইল বাছুন"
            s.name.isBlank() -> "অ্যাপের নাম লিখুন"
            s.versionName.isBlank() -> "ভার্সন নাম লিখুন"
            versionCode == null || versionCode <= 0 -> "ভার্সন কোড সংখ্যা হতে হবে"
            else -> null
        }
        if (problem != null) {
            _state.value = s.copy(error = problem)
            return
        }

        _state.value = s.copy(busy = true, error = null, progress = 0f, status = "পরীক্ষা করা হচ্ছে...")
        viewModelScope.launch {
            try {
                if (repo.packageExists(s.packageName)) {
                    throw IllegalStateException("এই প্যাকেজ নাম আগে থেকেই আছে: ${s.packageName}")
                }

                _state.value = _state.value.copy(status = "APK আপলোড হচ্ছে...")
                val apk = apkUploader.upload(apkUri!!, s.packageName, versionCode!!) { p ->
                    _state.value = _state.value.copy(progress = p)
                }

                val iconUrl = if (s.iconUri != null) {
                    _state.value = _state.value.copy(status = "আইকন আপলোড হচ্ছে...", progress = 0f)
                    iconUploader.upload(s.iconUri, s.packageName)
                } else {
                    s.iconUrlText.trim()
                }

                _state.value = _state.value.copy(status = "অ্যাপ সেভ হচ্ছে...")
                repo.createApp(
                    name = s.name.trim(),
                    developer = s.developer.trim().ifBlank { "AppMarket" },
                    packageName = s.packageName,
                    versionName = s.versionName.trim(),
                    versionCode = versionCode,
                    description = s.description.trim(),
                    category = s.category,
                    iconUrl = iconUrl,
                    apkPath = apk.apkPath,
                    sizeBytes = apk.sizeBytes
                )

                var message = "\"${s.name.trim()}\" যোগ হয়েছে"
                if (s.notify) {
                    _state.value = _state.value.copy(status = "নোটিফিকেশন পাঠানো হচ্ছে...")
                    try {
                        notifier.sendToAllUsers(
                            title = "নতুন অ্যাপ: ${s.name.trim()}".take(65),
                            body = s.description.trim().ifBlank { "এখন AppMarket-এ পাওয়া যাচ্ছে" }.take(240)
                        )
                    } catch (e: Exception) {
                        message += ", কিন্তু নোটিফিকেশন যায়নি: ${e.message}"
                    }
                }

                _state.value = _state.value.copy(busy = false, done = true, status = message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, error = e.message ?: "সেভ করা যায়নি", status = "")
            }
        }
    }
}
