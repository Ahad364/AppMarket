package com.appmarket.store.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appmarket.store.data.StoreRepository
import com.appmarket.store.model.AppItem
import com.appmarket.store.model.Review
import com.appmarket.store.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// ---------- Home (listing + search + category filter) ----------

data class HomeUiState(
    val query: String = "",
    val category: String? = null,
    val apps: List<AppItem> = emptyList(),
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

    fun refresh() = load()

    private fun load() {
        val snapshot = _state.value
        _state.value = snapshot.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val apps = if (snapshot.query.isBlank()) {
                    repo.listApps(snapshot.category)
                } else {
                    repo.searchApps(snapshot.query)
                        .filter { snapshot.category == null || it.category == snapshot.category }
                }
                _state.value = _state.value.copy(apps = apps, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}

// ---------- Detail (install + reviews) ----------

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
    val reviewSaved: Boolean = false
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

    init { loadApp() }

    fun loadApp() {
        viewModelScope.launch {
            _state.value = _state.value.copy(app = repo.getApp(appId))
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
}

// ---------- Profile ----------

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repo: StoreRepository) : ViewModel() {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    init {
        viewModelScope.launch { _profile.value = repo.currentProfile() }
    }

    fun signOut() = repo.signOut()
}
