package com.example.gramasanjeevini

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gramasanjeevini.models.UserProfile
import com.example.gramasanjeevini.ui.ConsumerApp
import com.example.gramasanjeevini.ui.LandingScreen
import com.example.gramasanjeevini.ui.LoginScreen
import com.example.gramasanjeevini.ui.PharmacistApp
import com.example.gramasanjeevini.ui.theme.GramaSanjeeviniTheme
import com.example.gramasanjeevini.ui.theme.LocalIsDarkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Dark mode state — lifted here so it persists across screens
            var isDarkMode by remember { mutableStateOf(false) }

            GramaSanjeeviniTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(LocalIsDarkTheme provides isDarkMode) {

                    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
                    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
                    var isLoadingProfile by remember { mutableStateOf(false) }
                    var showLanding by remember { mutableStateOf(true) }

                    // Request location permissions at startup
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }

                    // Auth State Listener
                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            user = auth.currentUser
                            if (user == null) userProfile = null
                        }
                        FirebaseAuth.getInstance().addAuthStateListener(listener)
                        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
                    }

                    // Fetch Profile when user signs in
                    LaunchedEffect(user) {
                        if (user != null) {
                            isLoadingProfile = true
                            FirebaseFirestore.getInstance().collection("users")
                                .document(user!!.uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    userProfile = doc.toObject(UserProfile::class.java)
                                    isLoadingProfile = false
                                }
                                .addOnFailureListener { isLoadingProfile = false }
                        }
                    }

                    if (user == null) {
                        if (showLanding) {
                            LandingScreen(onGetStarted = { showLanding = false })
                        } else {
                            LoginScreen()
                        }
                    } else if (isLoadingProfile) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        if (userProfile?.role == "pharmacist") {
                            PharmacistApp(onSignOut = { FirebaseAuth.getInstance().signOut() })
                        } else {
                            ConsumerApp(
                                userProfile = userProfile,
                                onSignOut = { FirebaseAuth.getInstance().signOut() },
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { isDarkMode = !isDarkMode },
                                onUserProfileUpdated = { updated -> userProfile = updated }
                            )
                        }
                    }
                }
            }
        }
    }
}
