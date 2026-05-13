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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.gramasanjeevini.data.MOCK_CLINICS
import com.example.gramasanjeevini.models.UserProfile
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareCentersScreen(navController: NavController, userProfile: UserProfile?) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var userLat by remember { mutableDoubleStateOf(userProfile?.lat ?: Utils.DEFAULT_LAT) }
    var userLng by remember { mutableDoubleStateOf(userProfile?.lng ?: Utils.DEFAULT_LNG) }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun refreshLocation() {
        if (hasLocationPermission) {
            Toast.makeText(context, "Syncing real-time location...", Toast.LENGTH_SHORT).show()
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                        Toast.makeText(context, "Location synced: ${String.format(Locale.getDefault(), "%.4f, %.4f", userLat, userLng)}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "GPS Signal weak. Set emulator location.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    DisposableEffect(hasLocationPermission) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let {
                    userLat = it.latitude
                    userLng = it.longitude
                }
            }
        }
        if (hasLocationPermission) {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }
        onDispose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { 
        hasLocationPermission = it 
        if (it) refreshLocation()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        else refreshLocation()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        TopAppBar(
            title = { Text("Nearby Stores & Clinics", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { refreshLocation() }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(MOCK_CLINICS) { clinic ->
                val distance = Utils.calculateDistance(userLat, userLng, clinic.lat, clinic.lng)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate("store_details/${clinic.id}") },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(clinic.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                                Text(clinic.type, color = Color(0xFF0D9488), style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = if (distance >= 0) String.format(Locale.getDefault(), "%.2f km", distance) else "N/A",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${clinic.phone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    if (clinic.lat != 0.0) {
                                        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$userLat,$userLng&destination=${clinic.lat},${clinic.lng}&travelmode=driving")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(mapIntent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                            ) {
                                Icon(Icons.Default.Navigation, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
