package com.appmarket.store.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appmarket.store.data.StoreRepository
import com.appmarket.store.data.ThemePrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val DETAIL = "detail/{appId}"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val MY_APPS = "myapps"
    fun detail(appId: String) = "detail/$appId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repo: StoreRepository
    @Inject lateinit var themePrefs: ThemePrefs

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not, the app works */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ needs the user's permission before notifications can show.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val dark by themePrefs.isDark.collectAsStateCompat()
            MarketTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MarketNavHost(startSignedIn = repo.isSignedIn)
                }
            }
        }
    }
}

@Composable
fun MarketNavHost(startSignedIn: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (startSignedIn) Routes.HOME else Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(onAuthenticated = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.AUTH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenApp = { nav.navigate(Routes.detail(it)) },
                onOpenProfile = { nav.navigate(Routes.PROFILE) }
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("appId") { type = NavType.StringType })
        ) {
            DetailScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { nav.popBackStack() },
                onOpenAdmin = { nav.navigate(Routes.ADMIN) },
                onOpenMyApps = { nav.navigate(Routes.MY_APPS) },
                onSignedOut = {
                    nav.navigate(Routes.AUTH) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Routes.ADMIN) {
            AdminScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.MY_APPS) {
            MyAppsScreen(
                onBack = { nav.popBackStack() },
                onOpenApp = { nav.navigate(Routes.detail(it)) }
            )
        }
    }
}
