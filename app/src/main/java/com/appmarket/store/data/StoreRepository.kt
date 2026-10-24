package com.appmarket.store.data

import android.content.Context
import com.appmarket.store.BuildConfig
import com.appmarket.store.model.AppItem
import com.appmarket.store.model.Review
import com.appmarket.store.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Single entry point for auth, catalogue, reviews and APK download.
 *
 * Realtime Database layout:
 *   apps/{appId}                     name, nameLower, category, apkPath, ratingSum, ratingCount, ...
 *   reviews/{appId}/{uid}            rating (1..5), text, userName, createdAt
 *   users/{uid}                      name, email
 */
@Singleton
class StoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val http: OkHttpClient
) {

    private val root: DatabaseReference get() = database.reference
    private val apps: DatabaseReference get() = root.child("apps")

    // ---------- Auth & profile ----------

    val isSignedIn: Boolean get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        saveProfile()
    }

    suspend fun signUp(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.updateProfile(userProfileChangeRequest { displayName = name })?.await()
        saveProfile()
    }

    suspend fun signInWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        saveProfile()
    }

    fun signOut() = auth.signOut()

    suspend fun currentProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        val snap = root.child("users").child(user.uid).get().await()
        return UserProfile(
            uid = user.uid,
            name = snap.child("name").getValue(String::class.java) ?: user.displayName ?: "User",
            email = user.email.orEmpty()
        )
    }

    private suspend fun saveProfile() {
        val user = auth.currentUser ?: return
        root.child("users").child(user.uid).updateChildren(
            mapOf(
                "name" to (user.displayName ?: "User"),
                "email" to user.email.orEmpty()
            )
        ).await()
    }

    // ---------- Catalogue: listing & search ----------

    suspend fun listApps(category: String? = null): List<AppItem> {
        val snap = apps.orderByChild("ratingCount").limitToLast(100).get().await()
        return snap.children.mapNotNull { it.toAppItem() }
            .filter { category == null || it.category == category }
            .sortedByDescending { it.ratingCount }
    }

    /** Case-insensitive prefix search on the lower-cased name field. */
    suspend fun searchApps(query: String): List<AppItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return listApps()
        val snap = apps.orderByChild("nameLower")
            .startAt(q)
            .endAt(q + "")
            .limitToFirst(30)
            .get().await()
        return snap.children.mapNotNull { it.toAppItem() }
    }

    suspend fun getApp(appId: String): AppItem? =
        apps.child(appId).get().await().toAppItem()

    // ---------- Reviews & ratings ----------

    fun reviewsFlow(appId: String): Flow<List<Review>> = callbackFlow {
        val ref = root.child("reviews").child(appId)
            .orderByChild("createdAt")
            .limitToLast(50)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children.mapNotNull { it.toReview() }
                        .sortedByDescending { it.createdAtMillis }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Saves the current user's review and adjusts the app's rating totals.
     * The totals change in a transaction; the review itself is a second write.
     */
    suspend fun submitReview(appId: String, rating: Int, text: String) {
        require(rating in 1..5) { "Rating must be 1-5" }
        val user = auth.currentUser ?: throw IllegalStateException("Login required")
        val appRef = apps.child(appId)
        val reviewRef = root.child("reviews").child(appId).child(user.uid)

        val previous: Long? = reviewRef.child("rating").get().await().getValue(Long::class.java)

        // Rules allow writes only to the ratingSum / ratingCount children, so transact on each child.
        val sumDelta: Long = if (previous == null) rating.toLong() else rating.toLong() - previous
        val countDelta: Long = if (previous == null) 1L else 0L

        appRef.child("ratingSum").runTransactionAwait { data ->
            data.value = (data.getValue(Long::class.java) ?: 0L) + sumDelta
        }
        appRef.child("ratingCount").runTransactionAwait { data ->
            data.value = (data.getValue(Long::class.java) ?: 0L) + countDelta
        }

        reviewRef.setValue(
            mapOf(
                "uid" to user.uid,
                "userName" to (user.displayName ?: "User"),
                "rating" to rating,
                "text" to text.trim().take(500),
                "createdAt" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    // ---------- Download ----------

    /**
     * Downloads the APK from the Cloudflare Worker with the Firebase ID token.
     * Reports progress 0f..1f. A complete cached file is reused.
     */
    suspend fun downloadApk(app: AppItem, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val file = File(dir, "${app.packageName}-${app.versionCode}.apk")
        if (file.exists() && file.length() > 0 && (app.sizeBytes == 0L || file.length() == app.sizeBytes)) {
            onProgress(1f)
            return file
        }

        val user = auth.currentUser ?: throw IllegalStateException("Login required")
        val idToken = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not get login token")

        val request = Request.Builder()
            .url("${BuildConfig.DOWNLOAD_BASE_URL}/${app.apkPath}")
            .header("Authorization", "Bearer $idToken")
            .build()

        withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Download failed (HTTP ${resp.code})")
                val body = resp.body ?: throw IllegalStateException("Empty response")
                val total = body.contentLength().takeIf { it > 0 } ?: app.sizeBytes
                val tmp = File(dir, file.name + ".part")

                body.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            done += read
                            if (total > 0) onProgress(done.toFloat() / total)
                        }
                    }
                }
                if (file.exists()) file.delete()
                tmp.renameTo(file)
            }
        }
        return file
    }

    // ---------- Helpers ----------

    /** Runs a Realtime Database transaction and suspends until it completes. */
    private suspend fun DatabaseReference.runTransactionAwait(update: (MutableData) -> Unit) {
        suspendCancellableCoroutine<Unit> { cont ->
            runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    update(currentData)
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (error != null) cont.resumeWithException(error.toException())
                    else cont.resume(Unit)
                }
            })
        }
    }

    private fun DataSnapshot.str(key: String): String =
        child(key).getValue(String::class.java).orEmpty()

    private fun DataSnapshot.num(key: String): Long =
        child(key).getValue(Long::class.java) ?: 0L

    private fun DataSnapshot.toAppItem(): AppItem? {
        if (!exists()) return null
        return AppItem(
            id = key.orEmpty(),
            name = str("name"),
            developer = str("developer"),
            packageName = str("packageName"),
            versionName = str("versionName"),
            versionCode = num("versionCode").toInt(),
            description = str("description"),
            category = str("category"),
            iconUrl = str("iconUrl"),
            apkPath = str("apkPath"),
            sizeBytes = num("sizeBytes"),
            ratingSum = num("ratingSum"),
            ratingCount = num("ratingCount")
        )
    }

    private fun DataSnapshot.toReview(): Review? {
        if (!exists()) return null
        return Review(
            uid = key.orEmpty(),
            userName = str("userName"),
            rating = num("rating").toInt(),
            text = str("text"),
            createdAtMillis = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        )
    }
}
