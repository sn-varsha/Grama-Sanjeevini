package com.example.gramasanjeevini.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun ConsumerApp(userProfile: UserProfile?, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(navController, onSignOut)
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
    
    // User coordinates: Use profile if available and not 0.0, else fallback to Davanagere region
    var userLat by remember { 
        mutableDoubleStateOf(
            if (userProfile != null && Math.abs(userProfile.lat) > 0.01) userProfile.lat else Utils.DEFAULT_LAT 
        ) 
    }
    var userLng by remember { 
        mutableDoubleStateOf(
            if (userProfile != null && Math.abs(userProfile.lng) > 0.01) userProfile.lng else Utils.DEFAULT_LNG 
        ) 
    }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
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
                                        val db = FirebaseFirestore.getInstance()
                                        db.collection("inventory").get()
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
                                                // Sort by distance (unknown distance -1.0 goes to end)
                                                results = found.sortedBy { if (it.second >= 0) it.second else Double.MAX_VALUE }
                                                isLoading = false
                                            }
                                            .addOnFailureListener { isLoading = false }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                modifier = Modifier.padding(end = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Search")
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    
                    if (!hasLocationPermission) {
                        Text(
                            "Location permission is required for accurate distance calculation.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                                        if (item.isLifeSaving) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                                Text("Life-Saving", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(text = "${item.quantity} in stock", color = if (item.quantity > 0) Color(0xFF16A34A) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val distanceText = if (distance >= 0) {
                                        "${String.format(Locale.getDefault(), "%.2f", distance)} km"
                                    } else {
                                        "Distance unknown"
                                    }
                                    Text(text = "${item.pharmacyName} ($distanceText)", color = Color.DarkGray)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            if (Math.abs(item.pharmacyLat) > 0.01) {
                                                val gmmIntentUri = Uri.parse("google.navigation:q=${item.pharmacyLat},${item.pharmacyLng}")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(mapIntent)
                                            } else {
                                                android.widget.Toast.makeText(context, "Pharmacy location not set", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Get Directions") }
                                    
                                    Button(
                                        onClick = {
                                            if (item.pharmacyPhone.isNotBlank()) {
                                                val dialerPhone = Utils.formatForDialer(item.pharmacyPhone)
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:$dialerPhone")
                                                }
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Call Shop") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
