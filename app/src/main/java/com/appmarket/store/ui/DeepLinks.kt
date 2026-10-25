package com.appmarket.store.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds an app id from an incoming appmarket://app/{id} link until the home screen can open it.
 * If the user is not signed in yet, the id waits here until sign-in finishes.
 */
object DeepLinks {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun set(appId: String) {
        _pending.value = appId
    }

    fun consume() {
        _pending.value = null
    }
}
