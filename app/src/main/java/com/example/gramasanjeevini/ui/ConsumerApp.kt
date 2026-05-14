package com.example.gramasanjeevini.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gramasanjeevini.models.InventoryItem
import com.example.gramasanjeevini.models.UserProfile
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun ConsumerApp(
    userProfile: UserProfile?,
    onSignOut: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onUserProfileUpdated: (UserProfile) -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                navController = navController,
                userProfile = userProfile,
                onSignOut = onSignOut
            )
        }
        composable("search") {
            MedicineSearchScreen(navController, userProfile, onSignOut)
        }
        composable("symptom_checker") {
            SymptomCheckerScreen(navController)
        }
        composable("care_centers") {
            CareCentersScreen(navController, userProfile)
        }
        composable("account") {
            AccountScreen(
                navController = navController,
                userProfile = userProfile,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onSignOut = onSignOut,
                onUserProfileUpdated = onUserProfileUpdated
            )
        }
        composable(
            route = "store_details/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.IntType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getInt("storeId")
            StoreDetailsScreen(navController, storeId)
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineSearchScreen(navController: NavController, userProfile: UserProfile?, onSignOut: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<InventoryItem, Double>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var userLat by remember { mutableDoubleStateOf(userProfile?.lat ?: Utils.DEFAULT_LAT) }
    var userLng by remember { mutableDoubleStateOf(userProfile?.lng ?: Utils.DEFAULT_LNG) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun refreshLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Fetching real-time location...", Toast.LENGTH_SHORT).show()
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                        Toast.makeText(context, "Location updated: ${String.format(Locale.getDefault(), "%.4f, %.4f", userLat, userLng)}", Toast.LENGTH_SHORT).show()
                        if (results.isNotEmpty()) {
                            results = results.map { (item, _) ->
                                val dist = Utils.calculateDistance(userLat, userLng, item.pharmacyLat, item.pharmacyLng)
                                Pair(item, dist)
                            }.sortedBy { if (it.second >= 0) it.second else Double.MAX_VALUE }
                        }
                    } else {
                        Toast.makeText(context, "GPS Signal weak. Ensure location is enabled.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Medicine", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Home, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Refresh Location", tint = Color.White)
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D9488))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF3F4F6))) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Find Medicine Nearby", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Enter medicine name...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.Black),
                        trailingIcon = {
                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        isLoading = true
                                        FirebaseFirestore.getInstance().collection("inventory").get()
                                            .addOnSuccessListener { snapshot ->
                                                val found = mutableListOf<Pair<InventoryItem, Double>>()
                                                val lowerQuery = searchQuery.lowercase()
                                                for (doc in snapshot.documents) {
                                                    val item = doc.toObject(InventoryItem::class.java)
                                                    if (item != null && item.searchName.contains(lowerQuery)) {
                                                        val dist = Utils.calculateDistance(userLat, userLng, item.pharmacyLat, item.pharmacyLng)
                                                        found.add(Pair(item, dist))
                                                    }
                                                }
                                                results = found.sortedBy { if (it.second >= 0) it.second else Double.MAX_VALUE }
                                                isLoading = false
                                            }
                                            .addOnFailureListener { isLoading = false }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                modifier = Modifier.padding(end = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Search") }
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0D9488))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(results) { (item, distance) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val distText = if (distance >= 0) String.format(Locale.getDefault(), "%.2f km", distance) else "Distance N/A"
                                    Text(text = "${item.pharmacyName} ($distText)", color = Color.DarkGray)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            if (item.pharmacyLat != 0.0) {
                                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$userLat,$userLng&destination=${item.pharmacyLat},${item.pharmacyLng}&travelmode=driving")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(mapIntent)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Directions, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Directions")
                                    }
                                    Button(
                                        onClick = {
                                            if (item.pharmacyPhone.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.pharmacyPhone}"))
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Call")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
