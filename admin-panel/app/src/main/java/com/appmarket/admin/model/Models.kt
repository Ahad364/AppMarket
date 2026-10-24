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
    val approved: Boolean
)
