package com.example.gramasanjeevini.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class LocationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val initialLat = intent.getDoubleExtra("lat", Utils.DEFAULT_LAT)
        val initialLng = intent.getDoubleExtra("lng", Utils.DEFAULT_LNG)
        val initialPos = GeoPoint(initialLat, initialLng)

        setContent {
            var pickedLocation by remember { mutableStateOf(initialPos) }
            var isLocating by remember { mutableStateOf(true) }
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    controller.setCenter(initialPos)
                }
            }

            val locationOverlay = remember {
                MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                    enableMyLocation()
                    runOnFirstFix {
                        runOnUiThread {
                            if (myLocation != null) {
                                mapView.controller.animateTo(myLocation)
                                pickedLocation = myLocation
                                // Move existing marker to my location
                                mapView.overlays.filterIsInstance<Marker>().forEach { it.position = myLocation }
                                isLocating = false
                                mapView.invalidate()
                            }
                        }
                    }
                }
            }

            // High-accuracy GMS fetch as fallback/primary
            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                val currentPos = GeoPoint(location.latitude, location.longitude)
                                pickedLocation = currentPos
                                mapView.controller.animateTo(currentPos)
                                mapView.overlays.filterIsInstance<Marker>().forEach { it.position = currentPos }
                                isLocating = false
                                mapView.invalidate()
                            }
                        }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            mapView.onResume()
                            locationOverlay.enableMyLocation()
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            mapView.onPause()
                            locationOverlay.disableMyLocation()
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = {
                        mapView.apply {
                            val marker = Marker(this)
                            marker.position = initialPos
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "Your Real Location"
                            overlays.add(marker)
                            overlays.add(locationOverlay)
                            
                            val eventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        pickedLocation = it
                                        marker.position = it
                                        locationOverlay.disableFollowLocation()
                                        invalidate()
                                    }
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }
                            overlays.add(MapEventsOverlay(eventsReceiver))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLocating) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Finding Your Real Location...", color = Color.Black)
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            val cts = CancellationTokenSource()
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        val currentPos = GeoPoint(location.latitude, location.longitude)
                                        mapView.controller.animateTo(currentPos)
                                        pickedLocation = currentPos
                                        mapView.overlays.filterIsInstance<Marker>().forEach { it.position = currentPos }
                                        mapView.invalidate()
                                        Toast.makeText(context, "Location Synced!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 24.dp),
                    containerColor = Color.White,
                    contentColor = Color(0xFF0D9488)
                ) { Icon(Icons.Default.MyLocation, contentDescription = "My Location") }

                Button(
                    onClick = {
                        val resultIntent = Intent().apply {
                            putExtra("lat", pickedLocation.latitude)
                            putExtra("lng", pickedLocation.longitude)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm This Location")
                }
            }
        }
    }
}
