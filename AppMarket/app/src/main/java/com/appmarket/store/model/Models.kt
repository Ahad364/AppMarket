package com.appmarket.store.model

data class AppItem(
    val id: String,
    val name: String,
    val developer: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val description: String,
    val category: String,
    val iconUrl: String,
    val apkPath: String,          // Firebase Storage path, e.g. "apks/com.example.app-3.apk"
    val sizeBytes: Long,
    val ratingSum: Long,
    val ratingCount: Long
) {
    val averageRating: Float
        get() = if (ratingCount == 0L) 0f else ratingSum.toFloat() / ratingCount
}

data class Review(
    val uid: String,
    val userName: String,
    val rating: Int,
    val text: String,
    val createdAtMillis: Long
)

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String
)
