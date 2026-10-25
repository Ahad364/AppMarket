package com.appmarket.admin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appmarket.admin.data.AdminRepository
import com.appmarket.admin.data.ApkUploader
import com.appmarket.admin.data.IconUploader
import com.appmarket.admin.data.NotificationSender
import com.appmarket.admin.model.AdminApp
import com.appmarket.admin.model.AdminSlider
import com.appmarket.admin.model.AdminUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------- Login ----------

data class LoginState(
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val repo: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        _state.value = LoginState(loading = true)
        viewModelScope.launch {
            _state.value = try {
                repo.signIn(email, password)
                LoginState(done = true)
            } catch (e: Exception) {
                LoginState(error = e.message ?: "লগইন ব্যর্থ হয়েছে")
            }
        }
    }
}

// ---------- Dashboard (users + apps) ----------

data class HomeState(
    val tab: Int = 0,                 // 0 = users, 1 = apps
    val users: List<AdminUser> = emptyList(),
    val apps: List<AdminApp> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val currentUid: String? = null,
    val signedOut: Boolean = false,
    val userQuery: String = "",
    val bannedOnly: Boolean = false,
    val uploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val notice: String? = null,
    val sliders: List<AdminSlider> = emptyList()
)

@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val repo: AdminRepository,
    private val uploader: ApkUploader,
    private val notifier: NotificationSender,
    private val iconUploader: IconUploader
) : ViewModel() {

    fun toggleFeatured(app: AdminApp) {
        viewModelScope.launch {
            try {
                repo.setFeatured(app.id, !app.featured)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    /** Uploads one slider banner. appId may be empty (the slider then opens nothing). */
    fun addSlider(uri: Uri, appId: String) {
        _state.value = _state.value.copy(uploading = true, uploadProgress = 0f, error = null, notice = null)
        viewModelScope.launch {
            try {
                // The same random id names the image file in R2 and the database record.
                val sliderId = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
                val url = iconUploader.uploadSlider(uri, sliderId)
                repo.addSlider(sliderId, url, appId)
                _state.value = _state.value.copy(uploading = false, notice = "স্লাইডার যোগ হয়েছে")
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(uploading = false, error = e.message ?: "স্লাইডার আপলোড ব্যর্থ")
            }
        }
    }

    fun deleteSlider(slider: AdminSlider) {
        viewModelScope.launch {
            try {
                repo.deleteSlider(slider.id)
                runCatching { iconUploader.deleteSlider(slider.id) }
                _state.value = _state.value.copy(notice = "স্লাইডার মুছে ফেলা হয়েছে")
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "স্লাইডার মোছা যায়নি")
            }
        }
    }
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Anyone who is not in admins/{uid} is signed out before seeing any data.
            if (!repo.verifyAdmin()) {
                repo.signOut()
                _state.value = HomeState(signedOut = true)
            } else {
                refresh()
            }
        }
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val users = repo.loadUsers()
                val apps = repo.loadApps()
                val sliders = runCatching { repo.loadSliders() }.getOrDefault(_state.value.sliders)
                _state.value = _state.value.copy(
                    users = users,
                    apps = apps,
                    sliders = sliders,
                    loading = false,
                    currentUid = repo.currentUid
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "লোড করা যায়নি")
            }
        }
    }

    fun setBanned(uid: String, banned: Boolean) {
        viewModelScope.launch {
            try {
                repo.setBanned(uid, banned)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun setApproved(appId: String, approved: Boolean) {
        viewModelScope.launch {
            try {
                repo.setApproved(appId, approved)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun deleteApp(appId: String) {
        viewModelScope.launch {
            try {
                repo.deleteApp(appId)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun onUserQuery(query: String) {
        _state.value = _state.value.copy(userQuery = query)
    }

    fun toggleBannedOnly() {
        _state.value = _state.value.copy(bannedOnly = !_state.value.bannedOnly)
    }

    fun updateApp(
        app: AdminApp,
        name: String,
        description: String,
        category: String,
        versionName: String,
        versionCode: Int
    ) {
        viewModelScope.launch {
            try {
                repo.updateApp(app.id, name, description, category, versionName, versionCode)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun uploadApk(app: AdminApp, uri: Uri, versionName: String, versionCode: Int, notify: Boolean) {
        _state.value = _state.value.copy(uploading = true, uploadProgress = 0f, error = null, notice = null)
        viewModelScope.launch {
            try {
                val result = uploader.upload(uri, app.packageName, versionCode) { p ->
                    _state.value = _state.value.copy(uploadProgress = p)
                }
                repo.updateApkInfo(app.id, result.apkPath, result.sizeBytes, versionName, versionCode)

                // Only a higher version number triggers the "update" notification.
                var message = "\"${app.name}\" এর APK আপলোড হয়েছে"
                if (notify && versionCode > app.versionCode) {
                    _state.value = _state.value.copy(uploadProgress = 1f)
                    try {
                        notifier.sendToAllUsers(
                            title = "আপডেট: ${app.name}".take(65),
                            body = "নতুন ভার্সন $versionName এখন পাওয়া যাচ্ছে".take(240)
                        )
                        message += ", নোটিফিকেশন পাঠানো হয়েছে"
                    } catch (e: Exception) {
                        message += ", কিন্তু নোটিফিকেশন যায়নি: ${e.message}"
                    }
                }
                _state.value = _state.value.copy(uploading = false, notice = message)
                refresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    uploading = false,
                    error = e.message ?: "আপলোড ব্যর্থ হয়েছে"
                )
            }
        }
    }

    fun sendNotification(title: String, body: String) {
        _state.value = _state.value.copy(error = null, notice = null)
        viewModelScope.launch {
            try {
                notifier.sendToAllUsers(title, body)
                _state.value = _state.value.copy(notice = "নোটিফিকেশন পাঠানো হয়েছে")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "নোটিফিকেশন পাঠানো যায়নি")
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = HomeState(signedOut = true)
    }
}
