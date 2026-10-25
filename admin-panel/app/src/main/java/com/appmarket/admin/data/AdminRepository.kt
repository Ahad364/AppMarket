package com.appmarket.admin.data

import com.appmarket.admin.model.AdminApp
import com.appmarket.admin.model.AdminUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Admin-only data access. The Realtime Database rules are the real gate:
 * a non-admin account cannot read users/bans or write apps, even if it edits this app.
 */
@Singleton
class AdminRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    private val root get() = database.reference

    val isSignedIn: Boolean get() = auth.currentUser != null

    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        if (!verifyAdmin()) {
            auth.signOut()
            throw IllegalStateException("এই অ্যাকাউন্টের অ্যাডমিন অনুমতি নেই।")
        }
    }

    fun signOut() = auth.signOut()

    /** Reads admins/{uid} for the signed-in user. */
    suspend fun verifyAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return root.child("admins").child(uid).get().await().getValue(Boolean::class.java) == true
    }

    suspend fun loadUsers(): List<AdminUser> {
        val usersSnap = root.child("users").get().await()
        val bansSnap = root.child("bans").get().await()
        return usersSnap.children.map { u ->
            val uid = u.key.orEmpty()
            AdminUser(
                uid = uid,
                name = u.child("name").getValue(String::class.java).orEmpty(),
                email = u.child("email").getValue(String::class.java).orEmpty(),
                banned = bansSnap.child(uid).getValue(Boolean::class.java) == true
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun loadApps(): List<AdminApp> {
        val appsSnap = root.child("apps").get().await()
        val featuredSnap = root.child("featured").get().await()
        return appsSnap.children.map { a ->
            AdminApp(
                id = a.key.orEmpty(),
                name = a.child("name").getValue(String::class.java).orEmpty(),
                packageName = a.child("packageName").getValue(String::class.java).orEmpty(),
                versionName = a.child("versionName").getValue(String::class.java).orEmpty(),
                iconUrl = a.child("iconUrl").getValue(String::class.java).orEmpty(),
                versionCode = a.child("versionCode").getValue(Long::class.java)?.toInt() ?: 0,
                description = a.child("description").getValue(String::class.java).orEmpty(),
                category = a.child("category").getValue(String::class.java).orEmpty(),
                approved = a.child("approved").getValue(Boolean::class.java) != false,
                featured = featuredSnap.child(a.key.orEmpty()).getValue(Boolean::class.java) == true,
                screenshots = a.child("screenshots").children.mapNotNull { it.getValue(String::class.java) },
                bannerUrl = a.child("bannerUrl").getValue(String::class.java).orEmpty()
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun setBanned(uid: String, banned: Boolean) {
        val ref = root.child("bans").child(uid)
        if (banned) ref.setValue(true).await() else ref.removeValue().await()
    }

    /** Edits app details. The rules allow this only for admins. nameLower is kept in sync for search. */
    suspend fun updateApp(
        appId: String,
        name: String,
        description: String,
        category: String,
        versionName: String,
        versionCode: Int
    ) {
        root.child("apps").child(appId).updateChildren(
            mapOf(
                "name" to name,
                "nameLower" to name.lowercase(),
                "description" to description,
                "category" to category,
                "versionName" to versionName,
                "versionCode" to versionCode
            )
        ).await()
    }

    /** Saves the uploaded APK path and the version info the admin entered. */
    suspend fun updateApkInfo(
        appId: String,
        apkPath: String,
        sizeBytes: Long,
        versionName: String,
        versionCode: Int
    ) {
        root.child("apps").child(appId).updateChildren(
            mapOf(
                "apkPath" to apkPath,
                "sizeBytes" to sizeBytes,
                "versionName" to versionName,
                "versionCode" to versionCode
            )
        ).await()
    }

    /** True when an app with this package name already exists. */
    suspend fun packageExists(packageName: String): Boolean {
        val snap = root.child("apps").orderByChild("packageName").equalTo(packageName).get().await()
        return snap.exists()
    }

    /** Creates a new approved app record with zero rating and returns its id. */
    suspend fun createApp(
        name: String,
        developer: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        description: String,
        category: String,
        iconUrl: String,
        apkPath: String,
        sizeBytes: Long
    ): String {
        val ref = root.child("apps").push()
        ref.setValue(
            mapOf(
                "name" to name,
                "nameLower" to name.lowercase(),
                "developer" to developer,
                "packageName" to packageName,
                "versionName" to versionName,
                "versionCode" to versionCode,
                "description" to description,
                "category" to category,
                "iconUrl" to iconUrl,
                "apkPath" to apkPath,
                "sizeBytes" to sizeBytes,
                "ratingSum" to 0L,
                "ratingCount" to 0L,
                "approved" to true
            )
        ).await()
        return ref.key.orEmpty()
    }

    /** Pins or unpins an app on the user home banner. */
    suspend fun setFeatured(appId: String, featured: Boolean) {
        val ref = root.child("featured").child(appId)
        if (featured) ref.setValue(true).await() else ref.removeValue().await()
    }

    /** Replaces the screenshot URL list. An empty list removes the node. */
    suspend fun setScreenshots(appId: String, urls: List<String>) {
        root.child("apps").child(appId).child("screenshots")
            .setValue(if (urls.isEmpty()) null else urls).await()
    }

    /** Saves the banner URL, or removes it when url is null. */
    suspend fun setBanner(appId: String, url: String?) {
        root.child("apps").child(appId).child("bannerUrl").setValue(url).await()
    }

    suspend fun setApproved(appId: String, approved: Boolean) {
        root.child("apps").child(appId).child("approved").setValue(approved).await()
    }

    /** Removes the app and all of its reviews. */
    suspend fun deleteApp(appId: String) {
        root.child("reviews").child(appId).removeValue().await()
        root.child("apps").child(appId).removeValue().await()
    }
}
