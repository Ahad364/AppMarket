package com.appmarket.store.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.appmarket.store.data.StoreRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val DETAIL = "detail/{appId}"
    const val PROFILE = "profile"
    fun detail(appId: String) = "detail/$appId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repo: StoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
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
                onSignedOut = {
                    nav.navigate(Routes.AUTH) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
