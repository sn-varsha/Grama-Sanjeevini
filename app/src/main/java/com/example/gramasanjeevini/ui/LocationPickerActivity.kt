package com.example.gramasanjeevini.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class LocationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val initialLat = intent.getDoubleExtra("lat", 14.4644)
        val initialLng = intent.getDoubleExtra("lng", 75.9218)
        val initialPos = GeoPoint(initialLat, initialLng)

        setContent {
            var pickedLocation by remember { mutableStateOf(initialPos) }
            val lifecycleOwner = LocalLifecycleOwner.current
            
            // Create and remember the MapView instance
            val mapView = remember {
                MapView(this).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(initialPos)
                }
            }

            // Manage MapView lifecycle
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = {
                        mapView.apply {
                            val marker = Marker(this)
                            marker.position = initialPos
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "Picked Location"
                            overlays.add(marker)
                            
                            val eventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        pickedLocation = it
                                        marker.position = it
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = {
                            val resultIntent = Intent().apply {
                                putExtra("lat", pickedLocation.latitude)
                                putExtra("lng", pickedLocation.longitude)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm This Location")
                    }
                }
                
                Card(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Text(
                        "Tap on map to set your location",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black
                    )
                }
            }
        }
    }
}
