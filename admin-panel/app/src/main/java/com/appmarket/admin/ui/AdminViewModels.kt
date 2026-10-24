package com.appmarket.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appmarket.admin.data.AdminRepository
import com.appmarket.admin.model.AdminApp
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
    val bannedOnly: Boolean = false
)

@HiltViewModel
class AdminHomeViewModel @Inject constructor(private val repo: AdminRepository) : ViewModel() {
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
                _state.value = _state.value.copy(
                    users = users,
                    apps = apps,
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

    fun signOut() {
        repo.signOut()
        _state.value = HomeState(signedOut = true)
    }
}
