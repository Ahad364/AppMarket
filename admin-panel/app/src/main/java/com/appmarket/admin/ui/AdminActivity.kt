package com.appmarket.admin.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appmarket.admin.data.AdminRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_NEW_APP = "newapp"

@AndroidEntryPoint
class AdminActivity : ComponentActivity() {

    @Inject lateinit var repo: AdminRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdminTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AdminNavHost(startSignedIn = repo.isSignedIn)
                }
            }
        }
    }
}

@Composable
fun AdminNavHost(startSignedIn: Boolean) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = if (startSignedIn) ROUTE_HOME else ROUTE_LOGIN
    ) {
        composable(ROUTE_LOGIN) {
            AdminLoginScreen(onLoggedIn = {
                nav.navigate(ROUTE_HOME) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
            })
        }
        composable(ROUTE_HOME) {
            AdminHomeScreen(
                onSignedOut = {
                    nav.navigate(ROUTE_LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onAddApp = { nav.navigate(ROUTE_NEW_APP) }
            )
        }
        composable(ROUTE_NEW_APP) {
            NewAppScreen(
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }
    }
}
