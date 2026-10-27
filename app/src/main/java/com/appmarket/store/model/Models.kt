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
    val ratingCount: Long,
    val approved: Boolean = true,  // admins can hide an app; hidden apps are not listed
    val screenshots: List<String> = emptyList(),  // public image URLs, max 6
    val bannerUrl: String = ""                    // 16:9 home banner, shown for featured apps
) {
    val averageRating: Float
        get() = if (ratingCount == 0L) 0f else ratingSum.toFloat() / ratingCount
}

/** One slider banner on the user home screen. appId is empty when the slider opens nothing. */
data class HomeSlider(
    val id: String,
    val imageUrl: String,
    val appId: String
)

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

data class AdminUser(
    val uid: String,
    val name: String,
    val email: String,
    val banned: Boolean
)

/** One entry in the user's download history, stored under users/{uid}/downloads/{appId}. */
data class DownloadRecord(
    val appId: String,
    val name: String,
    val packageName: String,
    val versionName: String,
    val iconUrl: String,
    val atMillis: Long
)

/** Latest published version of the AppMarket app itself, read from appInfo in the database. */
data class LatestAppInfo(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String
)
