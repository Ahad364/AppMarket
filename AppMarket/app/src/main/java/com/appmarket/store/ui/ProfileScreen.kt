package com.appmarket.store.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(onBack: () -> Unit, onSignedOut: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsStateCompat()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← ফিরে যান") }
        Spacer(Modifier.height(8.dp))
        Text("প্রোফাইল", style = MaterialTheme.typography.headlineSmall)

        profile?.let {
            Text("নাম: ${it.name}", style = MaterialTheme.typography.titleMedium)
            Text("Email: ${it.email}")
        } ?: Text("লোড হচ্ছে…")

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                vm.signOut()
                onSignedOut()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Log out") }
    }
}
