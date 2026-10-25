package com.appmarket.store.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appmarket.store.data.StoreRepository
import com.appmarket.store.data.ThemePrefs
import com.appmarket.store.model.AdminUser
import com.appmarket.store.model.AppItem
import com.appmarket.store.model.Review
import com.appmarket.store.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ---------- Auth ----------

data class AuthUiState(val loading: Boolean = false, val error: String? = null, val done: Boolean = false)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repo: StoreRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) = execute { repo.signIn(email, password) }
    fun signUp(name: String, email: String, password: String) = execute { repo.signUp(name, email, password) }
    fun google(idToken: String) = execute { repo.signInWithGoogle(idToken) }

    private fun execute(block: suspend () -> Unit) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            _state.value = try {
                block()
                AuthUiState(done = true)
            } catch (e: Exception) {
                AuthUiState(error = e.message ?: "Something went wrong")
            }
        }
    }
}

// ---------- Home (listing + search + category filter + load more) ----------

const val PAGE_SIZE = 20

data class HomeUiState(
    val query: String = "",
    val category: String? = null,
    val apps: List<AppItem> = emptyList(),
    val featured: List<AppItem> = emptyList(),
    val limit: Int = PAGE_SIZE,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: StoreRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState(loading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val categories = listOf("Productivity", "Games", "Social", "Tools", "Education")

    init { load() }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
        load()
    }

    fun onCategory(c: String?) {
        _state.value = _state.value.copy(category = if (_state.value.category == c) null else c)
        load()
    }

    fun loadMore() {
        _state.value = _state.value.copy(limit = _state.value.limit + PAGE_SIZE)
        load()
    }

    fun refresh() = load()

    /** True when this device has the app installed and the listing has a newer version. */
    fun updateAvailable(app: AppItem): Boolean = repo.updateAvailable(app)

    private fun load() {
        val snapshot = _state.value
        _state.value = snapshot.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val apps = if (snapshot.query.isBlank()) {
                    repo.listApps(snapshot.category, snapshot.limit)
                } else {
                    repo.searchApps(snapshot.query)
                        .filter { snapshot.category == null || it.category == snapshot.category }
                }
                // Featured banner only on the plain home listing.
                val featured = if (snapshot.query.isBlank() && snapshot.category == null) {
                    runCatching { repo.listFeatured() }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                _state.value = _state.value.copy(apps = apps, featured = featured, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}

// ---------- Detail (install + reviews + favorites + admin moderation) ----------

sealed class InstallState {
    object Idle : InstallState()
    data class Downloading(val progress: Float) : InstallState()
    data class ReadyToInstall(val file: File) : InstallState()
    data class Failed(val message: String) : InstallState()
}

data class DetailUiState(
    val app: AppItem? = null,
    val install: InstallState = InstallState.Idle,
    val reviewError: String? = null,
    val reviewSaved: Boolean = false,
    val isAdmin: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repo: StoreRepository
) : ViewModel() {

    val appId: String = checkNotNull(savedStateHandle["appId"])

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val reviews: StateFlow<List<Review>> = repo.reviewsFlow(appId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isFavorite: StateFlow<Boolean> = repo.favoritesFlow()
        .map { appId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val myUid: String? = repo.currentUid

    init {
        loadApp()
        loadAdmin()
    }

    fun loadApp() {
        viewModelScope.launch {
            _state.value = _state.value.copy(app = repo.getApp(appId))
        }
    }

    private fun loadAdmin() {
        viewModelScope.launch {
            val admin = runCatching { repo.isAdmin() }.getOrDefault(false)
            _state.value = _state.value.copy(isAdmin = admin)
        }
    }

    fun installedVersion(packageName: String): Long? = repo.installedVersionCode(packageName)

    fun updateAvailable(app: AppItem): Boolean = repo.updateAvailable(app)

    fun toggleFavorite(on: Boolean) {
        viewModelScope.launch {
            try {
                repo.setFavorite(appId, on)
            } catch (e: Exception) {
                _state.value = _state.value.copy(reviewError = e.message)
            }
        }
    }

    fun download(app: AppItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(install = InstallState.Downloading(0f))
            try {
                val file = repo.downloadApk(app) { p ->
                    _state.value = _state.value.copy(install = InstallState.Downloading(p))
                }
                _state.value = _state.value.copy(install = InstallState.ReadyToInstall(file))
            } catch (e: Exception) {
                _state.value = _state.value.copy(install = InstallState.Failed(e.message ?: "Download failed"))
            }
        }
    }

    /** Adds or updates the current user's review. */
    fun submitReview(rating: Int, text: String) {
        viewModelScope.launch {
            try {
                repo.submitReview(appId, rating, text)
                _state.value = _state.value.copy(reviewSaved = true, reviewError = null)
                loadApp() // refresh the average rating shown on screen
            } catch (e: Exception) {
                _state.value = _state.value.copy(reviewError = e.message, reviewSaved = false)
            }
        }
    }

    /** Works for the review's author and for admins; the database rules enforce both. */
    fun deleteReview(reviewUid: String) {
        viewModelScope.launch {
            try {
                repo.deleteReview(appId, reviewUid)
                _state.value = _state.value.copy(reviewError = null, reviewSaved = false)
                loadApp()
            } catch (e: Exception) {
                _state.value = _state.value.copy(reviewError = e.message)
            }
        }
    }
}

// ---------- Profile ----------

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: StoreRepository,
    private val themePrefs: ThemePrefs
) : ViewModel() {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val isDark: StateFlow<Boolean> = themePrefs.isDark

    init {
        viewModelScope.launch {
            _profile.value = repo.currentProfile()
            _isAdmin.value = runCatching { repo.isAdmin() }.getOrDefault(false)
        }
    }

    fun setDark(value: Boolean) = themePrefs.setDark(value)

    fun signOut() = repo.signOut()
}

// ---------- My apps: favorites + installed ----------

data class MyAppsUiState(
    val tab: Int = 0,
    val apps: List<AppItem> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MyAppsViewModel @Inject constructor(private val repo: StoreRepository) : ViewModel() {
    private val _state = MutableStateFlow(MyAppsUiState())
    val state: StateFlow<MyAppsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.favoritesFlow().collect { favs ->
                _state.value = _state.value.copy(favorites = favs)
            }
        }
        load()
    }

    /** 0 = favorites, 1 = installed on this device */
    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(apps = repo.listApps(limit = 100), loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun toggleFavorite(appId: String, on: Boolean) {
        viewModelScope.launch {
            try {
                repo.setFavorite(appId, on)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun isInstalled(app: AppItem): Boolean = repo.installedVersionCode(app.packageName) != null

    fun updateAvailable(app: AppItem): Boolean = repo.updateAvailable(app)
}

// ---------- Admin: user management ----------

data class AdminUiState(
    val tab: Int = 0,               // 0 = users, 1 = apps
    val users: List<AdminUser> = emptyList(),
    val apps: List<AppItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val currentUid: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(private val repo: StoreRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState(currentUid = repo.currentUid))
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val users = repo.listUsers()
                val apps = repo.listAllAppsForAdmin()
                _state.value = _state.value.copy(users = users, apps = apps, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "লোড করা যায়নি")
            }
        }
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(tab = tab)
    }

    /** Hidden apps are not listed to users; admins still see them here. */
    fun setApproved(appId: String, approved: Boolean) {
        viewModelScope.launch {
            try {
                repo.setAppApproved(appId, approved)
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
}
