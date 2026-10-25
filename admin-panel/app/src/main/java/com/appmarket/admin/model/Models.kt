package com.appmarket.admin.model

data class AdminUser(
    val uid: String,
    val name: String,
    val email: String,
    val banned: Boolean
)

data class AdminApp(
    val id: String,
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val description: String,
    val category: String,
    val iconUrl: String,
    val approved: Boolean,
    val featured: Boolean = false
)

/** A home-screen slider banner. It is not tied to an app; it may open one when tapped. */
data class AdminSlider(
    val id: String,
    val imageUrl: String,
    val appId: String,
    val createdAt: Long
)
