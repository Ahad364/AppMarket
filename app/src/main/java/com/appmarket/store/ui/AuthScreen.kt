package com.appmarket.store.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appmarket.store.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(onAuthenticated: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateCompat()
    val ctx = LocalContext.current

    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.done) {
        if (state.done) onAuthenticated()
    }

    val googleClient = remember {
        GoogleSignIn.getClient(
            ctx,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(ctx.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            account.idToken?.let { vm.google(it) }
        } catch (_: Exception) {
            // user cancelled; error text is shown via state if sign-in fails
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AppMarket", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(
            if (isSignUp) "নতুন অ্যাকাউন্ট তৈরি করুন" else "অ্যাপ খুঁজুন ও ইনস্টল করুন",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        if (isSignUp) {
            OutlinedTextField(name, { name = it }, label = { Text("নাম") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("Password (৬+ অক্ষর)") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (isSignUp) vm.signUp(name, email, password) else vm.signIn(email, password)
            },
            enabled = !state.loading && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.loading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
            else Text(if (isSignUp) "Sign up" else "Log in")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { googleLauncher.launch(googleClient.signInIntent) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Google দিয়ে চালিয়ে যান") }

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "আগে থেকেই অ্যাকাউন্ট আছে? Log in" else "নতুন? Sign up")
        }
    }
}
