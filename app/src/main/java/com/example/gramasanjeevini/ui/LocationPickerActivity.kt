package com.example.gramasanjeevini.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gramasanjeevini.ui.theme.GramaSanjeeviniTheme
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// ─── Screen state machine ────────────────────────────────────────────────────
private sealed class LocationState {
    object Idle : LocationState()
    object Locating : LocationState()
    data class Success(val lat: Double, val lng: Double, val address: String) : LocationState()
    data class Error(val message: String) : LocationState()
}

// ─── Activity ────────────────────────────────────────────────────────────────
class LocationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read any previously set coordinates passed from caller (used as fallback only)
        val initialLat = intent.getDoubleExtra("lat", Utils.DEFAULT_LAT)
        val initialLng = intent.getDoubleExtra("lng", Utils.DEFAULT_LNG)

        setContent {
            GramaSanjeeviniTheme {
                LocationPickerScreen(
                    initialLat = initialLat,
                    initialLng = initialLng,
                    onConfirm = { lat, lng, address ->
                        // Return result to the calling screen (PharmacistApp / LoginScreen)
                        val resultIntent = Intent().apply {
                            putExtra("lat", lat)
                            putExtra("lng", lng)
                            putExtra("address", address)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onBack = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

// ─── Screen Composable ────────────────────────────────────────────────────────
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onConfirm: (lat: Double, lng: Double, address: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var locationState by remember { mutableStateOf<LocationState>(LocationState.Idle) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // ── Permission handling ──────────────────────────────────────────────────
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission.value = granted
        if (granted) {
            locationState = LocationState.Locating
        } else {
            locationState = LocationState.Error(
                "Location permission denied.\nPlease grant location permission in Settings to set your store location."
            )
        }
    }

    // ── Geocoder helper (runs on IO thread) ─────────────────────────────────
    suspend fun resolveAddress(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+: callback-based geocoder
                var result = ""
                val latch = java.util.concurrent.CountDownLatch(1)
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    result = addresses.firstOrNull()?.let { addr ->
                        listOfNotNull(
                            addr.subLocality,
                            addr.locality,
                            addr.adminArea,
                            addr.postalCode
                        ).joinToString(", ")
                    } ?: "%.4f°, %.4f°".format(lat, lng)
                    latch.countDown()
                }
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                result.ifEmpty { "%.4f°, %.4f°".format(lat, lng) }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.let { addr ->
                    listOfNotNull(
                        addr.subLocality,
                        addr.locality,
                        addr.adminArea,
                        addr.postalCode
                    ).joinToString(", ")
                } ?: "%.4f°, %.4f°".format(lat, lng)
            }
        } catch (e: Exception) {
            "%.4f°, %.4f°".format(lat, lng)
        }
    }

    // ── GPS fetch function ───────────────────────────────────────────────────
    fun fetchLocation() {
        locationState = LocationState.Locating
        scope.launch {
            try {
                val cts = CancellationTokenSource()

                // 15-second timeout watchdog
                launch {
                    delay(15_000)
                    if (locationState is LocationState.Locating) {
                        cts.cancel()
                        locationState = LocationState.Error(
                            "Location fetch timed out.\nMake sure GPS is enabled and try again."
                        )
                    }
                }

                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            scope.launch {
                                val address = resolveAddress(location.latitude, location.longitude)
                                locationState = LocationState.Success(
                                    lat = location.latitude,
                                    lng = location.longitude,
                                    address = address
                                )
                            }
                        } else {
                            locationState = LocationState.Error(
                                "GPS signal is weak.\nPlease step outdoors or enable high-accuracy mode and retry."
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        locationState = LocationState.Error(
                            "Could not get location: ${e.localizedMessage ?: "Unknown error"}\nPlease try again."
                        )
                    }
            } catch (e: Exception) {
                locationState = LocationState.Error(
                    "An unexpected error occurred.\nPlease try again."
                )
            }
        }
    }

    // ── Auto-start location fetch on permission grant ────────────────────────
    LaunchedEffect(hasPermission.value) {
        if (hasPermission.value && locationState is LocationState.Idle) {
            fetchLocation()
        }
    }

    // ── Request permission immediately if not granted ────────────────────────
    LaunchedEffect(Unit) {
        if (!hasPermission.value) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation()
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Set Store Location",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D9488)
                )
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF0FDFA), Color(0xFFF3F4F6))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when (val state = locationState) {

                // ── IDLE / LOADING ───────────────────────────────────────────
                is LocationState.Idle, is LocationState.Locating -> {
                    LoadingContent()
                }

                // ── SUCCESS ──────────────────────────────────────────────────
                is LocationState.Success -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                        exit = fadeOut()
                    ) {
                        SuccessContent(
                            state = state,
                            onRetry = { fetchLocation() },
                            onConfirm = { onConfirm(state.lat, state.lng, state.address) }
                        )
                    }
                }

                // ── ERROR ────────────────────────────────────────────────────
                is LocationState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            if (!hasPermission.value) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                fetchLocation()
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── Loading UI ───────────────────────────────────────────────────────────────
@Composable
private fun LoadingContent() {
    // Pulsing animation for the GPS icon background
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        // Pulsing GPS icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFF0D9488).copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Locating",
                tint = Color(0xFF0D9488),
                modifier = Modifier.size(56.dp)
            )
        }

        Text(
            text = "Finding Your Location",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D9488),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Please wait while we fetch\nyour current GPS coordinates…",
            fontSize = 15.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        CircularProgressIndicator(
            color = Color(0xFF0D9488),
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}

// ─── Success UI ───────────────────────────────────────────────────────────────
@Composable
private fun SuccessContent(
    state: LocationState.Success,
    onRetry: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Success icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D9488).copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location found",
                tint = Color(0xFF0D9488),
                modifier = Modifier.size(52.dp)
            )
        }

        Text(
            text = "Location Found!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D9488)
        )

        // Coordinates + address card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Address row
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE6F4F1)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Address",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9CA3AF)
                        )
                        Text(
                            text = state.address.ifEmpty { "Address not available" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827),
                            lineHeight = 21.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // Latitude row
                CoordinateRow(
                    label = "Latitude",
                    value = "%.6f°".format(state.lat),
                    iconVector = Icons.Default.North
                )

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // Longitude row
                CoordinateRow(
                    label = "Longitude",
                    value = "%.6f°".format(state.lng),
                    iconVector = Icons.Default.East
                )
            }
        }

        // Retry (secondary)
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry / Refresh Location", fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }

        // Confirm (primary)
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Confirm This Location",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ─── Coordinate detail row ─────────────────────────────────────────────────────
@Composable
private fun CoordinateRow(
    label: String,
    value: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFE6F4F1)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color(0xFF0D9488),
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF)
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
        }
    }
}

// ─── Error UI ─────────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Error icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEE2E2))
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = "Error",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(52.dp)
            )
        }

        Text(
            text = "Location Unavailable",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDC2626)
        )

        // Error detail card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color(0xFF374151),
                    lineHeight = 21.sp
                )
            }
        }

        // Retry button
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Try Again",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
