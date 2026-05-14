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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.gramasanjeevini.models.Pharmacy
import com.example.gramasanjeevini.models.UserProfile
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareCentersScreen(navController: NavController, userProfile: UserProfile?) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLat by remember { mutableDoubleStateOf(userProfile?.lat ?: Utils.DEFAULT_LAT) }
    var userLng by remember { mutableDoubleStateOf(userProfile?.lng ?: Utils.DEFAULT_LNG) }

    // Pharmacy data states
    var pharmacies by remember { mutableStateOf<List<Pair<Pharmacy, Double>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Load pharmacies from Firestore and sort by distance
    fun loadPharmacies(lat: Double, lng: Double) {
        isLoading = true
        errorMsg = null
        db.collection("pharmacies").get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pharmacy::class.java)?.copy(id = doc.id)
                }.map { pharmacy ->
                    val distance = Utils.calculateDistance(lat, lng, pharmacy.lat, pharmacy.lng)
                    Pair(pharmacy, distance)
                }.sortedBy { (_, dist) ->
                    if (dist >= 0) dist else Double.MAX_VALUE
                }
                pharmacies = result
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMsg = "Failed to load pharmacies: ${e.localizedMessage}"
                isLoading = false
            }
    }

    // Refresh GPS then reload pharmacies
    fun refreshLocationAndLoad() {
        if (hasLocationPermission) {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                    }
                    loadPharmacies(userLat, userLng)
                }
                .addOnFailureListener {
                    loadPharmacies(userLat, userLng)
                }
        } else {
            loadPharmacies(userLat, userLng)
        }
    }

    // Live location updates while screen is open
    DisposableEffect(hasLocationPermission) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    userLat = it.latitude
                    userLng = it.longitude
                    // Re-sort already-loaded pharmacies with updated coords
                    pharmacies = pharmacies.map { (p, _) ->
                        Pair(p, Utils.calculateDistance(userLat, userLng, p.lat, p.lng))
                    }.sortedBy { (_, d) -> if (d >= 0) d else Double.MAX_VALUE }
                }
            }
        }
        if (hasLocationPermission) {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }
        onDispose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        refreshLocationAndLoad()
    }

    // Initial load
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            refreshLocationAndLoad()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Nearby Pharmacies",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${pharmacies.size} found near you",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshLocationAndLoad() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D9488))
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> LoadingPharmaciesState()
                errorMsg != null -> ErrorPharmaciesState(
                    message = errorMsg!!,
                    onRetry = { refreshLocationAndLoad() }
                )
                pharmacies.isEmpty() -> EmptyPharmaciesState()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Location info banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0D9488).copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Sorted by distance from your location",
                                        fontSize = 13.sp,
                                        color = Color(0xFF0D9488),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        items(pharmacies) { (pharmacy, distance) ->
                            PharmacyCard(
                                pharmacy = pharmacy,
                                distance = distance,
                                userLat = userLat,
                                userLng = userLng
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PharmacyCard(
    pharmacy: Pharmacy,
    distance: Double,
    userLat: Double,
    userLng: Double
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pharmacy icon circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE6F4F1))
                ) {
                    Icon(
                        Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pharmacy.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Pharmacy",
                        fontSize = 12.sp,
                        color = Color(0xFF0D9488),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Distance badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (distance in 0.0..2.0) Color(0xFFE6F4F1) else Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = if (distance >= 0) {
                            String.format(Locale.getDefault(), "%.1f km", distance)
                        } else "N/A",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (distance in 0.0..2.0) Color(0xFF0D9488) else Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Address row
            if (pharmacy.address.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = pharmacy.address,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 19.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Phone row
            if (pharmacy.phone.isNotBlank() && pharmacy.phone != "Not provided") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = pharmacy.phone,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Call button
                OutlinedButton(
                    onClick = {
                        if (pharmacy.phone.isNotBlank() && pharmacy.phone != "Not provided") {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${pharmacy.phone}")
                            )
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0D9488))
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // Directions button
                Button(
                    onClick = {
                        if (pharmacy.lat != 0.0) {
                            val uri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1" +
                                        "&origin=$userLat,$userLng" +
                                        "&destination=${pharmacy.lat},${pharmacy.lng}" +
                                        "&travelmode=driving"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                ) {
                    Icon(Icons.Default.Navigation, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LoadingPharmaciesState() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE5E7EB),
                                Color(0xFFF9FAFB),
                                Color(0xFFE5E7EB)
                            )
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyPharmaciesState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                "No Pharmacies Found",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Text(
                "There are no registered pharmacies nearby yet. Check back later.",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun ErrorPharmaciesState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2))
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                "Something Went Wrong",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Text(
                message,
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}
