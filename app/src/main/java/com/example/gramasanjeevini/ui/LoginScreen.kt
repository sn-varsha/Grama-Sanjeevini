package com.example.gramasanjeevini.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gramasanjeevini.models.UserProfile
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isPharmacist by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLat by remember { mutableDoubleStateOf(0.0) }
    var userLng by remember { mutableDoubleStateOf(0.0) }
    var isLocationFetched by remember { mutableStateOf(false) }

    val mapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            userLat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            userLng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            isLocationFetched = true
            Toast.makeText(context, "Location saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun openMapPicker(lat: Double, lng: Double) {
        val intent = Intent(context, LocationPickerActivity::class.java).apply {
            putExtra("lat", lat)
            putExtra("lng", lng)
        }
        mapLauncher.launch(intent)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    openMapPicker(loc?.latitude ?: Utils.DEFAULT_LAT, loc?.longitude ?: Utils.DEFAULT_LNG)
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Grama Sanjeevini",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D9488),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = if (isSignUp) "Create an account" else "Welcome back",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.Black)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color.Black)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = Color.Black)
                )
                
                if (isSignUp) {
                    Text(
                        text = "Password must be at least 6 characters",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(color = Color.Black)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPharmacist,
                            onCheckedChange = { isPharmacist = it }
                        )
                        Text("I am a Pharmacist", fontSize = 14.sp, color = Color.Black)
                    }

                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                val cts = CancellationTokenSource()
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                                    .addOnSuccessListener { loc ->
                                        openMapPicker(loc?.latitude ?: Utils.DEFAULT_LAT, loc?.longitude ?: Utils.DEFAULT_LNG)
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLocationFetched) Color(0xFF16A34A) else Color(0xFF0D9488)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLocationFetched) "Location Set ✓" else "Set My Location")
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF0D9488))
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isSignUp) {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (password.length < 6) {
                                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!isLocationFetched) {
                                    Toast.makeText(context, "Please set your location on the map", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }

                            isLoading = true
                            if (isSignUp) {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid ?: ""
                                        val profile = UserProfile(
                                            name = name,
                                            email = email,
                                            role = if (isPharmacist) "pharmacist" else "consumer",
                                            lat = userLat,
                                            lng = userLng
                                        )
                                        db.collection("users").document(uid).set(profile)
                                            .addOnSuccessListener {
                                                isLoading = false
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                Toast.makeText(context, "Profile Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Auth Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        isLoading = false
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Login Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        Text(if (isSignUp) "Sign Up" else "Sign In", fontSize = 16.sp)
                    }

                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(if (isSignUp) "Already have an account? Sign In" else "New user? Create an account", color = Color(0xFF0D9488))
                    }
                }
            }
        }
    }
}
