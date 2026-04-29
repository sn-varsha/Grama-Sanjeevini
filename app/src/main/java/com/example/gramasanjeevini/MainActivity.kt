package com.example.gramasanjeevini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gramasanjeevini.ui.ConsumerApp
import com.example.gramasanjeevini.ui.LandingScreen
import com.example.gramasanjeevini.ui.LoginScreen
import com.example.gramasanjeevini.ui.PharmacistApp
import com.example.gramasanjeevini.ui.theme.GramaSanjeeviniTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GramaSanjeeviniTheme {
                var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
                var userRole by remember { mutableStateOf<String?>(null) }
                var isLoadingRole by remember { mutableStateOf(false) }
                var showLanding by remember { mutableStateOf(true) }
                
                // Auth State Listener
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        user = auth.currentUser
                        if (user == null) {
                            userRole = null
                        }
                    }
                    FirebaseAuth.getInstance().addAuthStateListener(listener)
                    onDispose {
                        FirebaseAuth.getInstance().removeAuthStateListener(listener)
                    }
                }

                // Fetch Role when user signs in
                LaunchedEffect(user) {
                    if (user != null) {
                        isLoadingRole = true
                        FirebaseFirestore.getInstance().collection("users")
                            .document(user!!.uid)
                            .get()
                            .addOnSuccessListener { doc ->
                                userRole = doc.getString("role")
                                isLoadingRole = false
                            }
                            .addOnFailureListener {
                                isLoadingRole = false
                            }
                    }
                }

                if (user == null) {
                    if (showLanding) {
                        LandingScreen(onGetStarted = { showLanding = false })
                    } else {
                        LoginScreen()
                    }
                } else if (isLoadingRole) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (userRole == "pharmacist") {
                        PharmacistApp(onSignOut = { FirebaseAuth.getInstance().signOut() })
                    } else {
                        ConsumerApp(onSignOut = { FirebaseAuth.getInstance().signOut() })
                    }
                }
            }
        }
    }
}
