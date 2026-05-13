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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
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
import com.example.gramasanjeevini.models.InventoryItem
import com.example.gramasanjeevini.models.Pharmacy
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacistApp(onSignOut: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser ?: return
    val context = LocalContext.current

    var pharmacy by remember { mutableStateOf<Pharmacy?>(null) }
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var showAddForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var isLoadingPharmacy by remember { mutableStateOf(true) }

    LaunchedEffect(user.uid) {
        db.collection("pharmacies")
            .whereEqualTo("ownerId", user.uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents[0]
                    val p = doc.toObject(Pharmacy::class.java)?.copy(id = doc.id)
                    p?.let { pharmacyObj ->
                        pharmacy = pharmacyObj
                        db.collection("inventory")
                            .whereEqualTo("pharmacyId", pharmacyObj.id)
                            .get()
                            .addOnSuccessListener { invSnap ->
                                inventory = invSnap.documents.mapNotNull { invDoc -> 
                                    invDoc.toObject(InventoryItem::class.java)?.copy(id = invDoc.id) 
                                }.sortedByDescending { it.updatedAt }
                            }
                    }
                }
                isLoadingPharmacy = false
            }
            .addOnFailureListener { e ->
                isLoadingPharmacy = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    if (isLoadingPharmacy) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0D9488))
        }
        return
    }

    if (pharmacy == null) {
        PharmacyRegistration(user.uid) { p -> pharmacy = p }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pharmacist Portal", color = Color.White)
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
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = pharmacy?.name ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(text = pharmacy?.address ?: "", fontSize = 14.sp, color = Color.Gray)
                        Text(text = "Phone: ${pharmacy?.phone ?: ""}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { 
                            editingItem = null
                            showAddForm = !showAddForm 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                        Text(if (showAddForm) " Close" else " Add Stock")
                    }
                }
            }

            if (showAddForm || editingItem != null) {
                AddMedicineForm(pharmacy!!, editingItem) { newItem ->
                    if (editingItem != null) {
                        inventory = inventory.map { if (it.id == newItem.id) newItem else it }.sortedByDescending { it.updatedAt }
                        editingItem = null
                    } else {
                        inventory = (listOf(newItem) + inventory).sortedByDescending { it.updatedAt }
                        showAddForm = false
                    }
                }
            }

            Text(
                "Current Stock", 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )

            if (inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No medicines in stock. Add some using the button above.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(inventory) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE6F4F1),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MedicalServices,
                                            contentDescription = null,
                                            tint = Color(0xFF0D9488),
                                            modifier = Modifier.padding(12.dp).size(28.dp)
                                        )
                                    }
                                },
                                headlineContent = { Text(item.name, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 20.sp) },
                                supportingContent = { 
                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                        Text("Available Quantity: ${item.quantity}", color = Color.DarkGray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        if (item.isLifeSaving) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                                Text("Life-Saving", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { 
                                            editingItem = item
                                            showAddForm = false
                                        }) { Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF0D9488), modifier = Modifier.size(28.dp)) }
                                        
                                        IconButton(onClick = {
                                            db.collection("inventory").document(item.id).delete()
                                                .addOnSuccessListener {
                                                    inventory = inventory.filter { it.id != item.id }
                                                    Toast.makeText(context, "Item removed", Toast.LENGTH_SHORT).show()
                                                }
                                        }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(28.dp)) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun PharmacyRegistration(userId: String, onRegistered: (Pharmacy) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission = it }

    var userLat by remember { mutableDoubleStateOf(0.0) }
    var userLng by remember { mutableDoubleStateOf(0.0) }
    var isLocationSet by remember { mutableStateOf(false) }

    val mapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            userLat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            userLng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            isLocationSet = true
            Toast.makeText(context, "Store Location Set!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Register Your Pharmacy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, 
                    label = { Text("Pharmacy Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color.Black)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = address, onValueChange = { address = it }, 
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color.Black)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, 
                    label = { Text("Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color.Black)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (!hasLocationPermission) {
                            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { loc ->
                                    val intent = Intent(context, LocationPickerActivity::class.java).apply {
                                        if (loc != null) {
                                            putExtra("lat", loc.latitude)
                                            putExtra("lng", loc.longitude)
                                        }
                                    }
                                    mapLauncher.launch(intent)
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isLocationSet) Color(0xFF16A34A) else Color(0xFF0D9488)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLocationSet) "Location Fixed ✓" else "Set Store Location on Map")
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF0D9488))
                } else {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && address.isNotBlank() && phone.isNotBlank() && isLocationSet) {
                                isLoading = true
                                val db = FirebaseFirestore.getInstance()
                                val newId = UUID.randomUUID().toString()
                                val newPharm = Pharmacy(
                                    id = newId, 
                                    ownerId = userId, 
                                    name = name, 
                                    address = address, 
                                    phone = phone, 
                                    lat = userLat, 
                                    lng = userLng
                                )
                                db.collection("pharmacies").document(newId).set(newPharm).addOnSuccessListener {
                                    isLoading = false
                                    onRegistered(newPharm)
                                }.addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please fill all fields and set location", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Complete Registration", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AddMedicineForm(pharmacy: Pharmacy, existingItem: InventoryItem? = null, onAdded: (InventoryItem) -> Unit) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var quantity by remember { mutableStateOf(existingItem?.quantity?.toString() ?: "") }
    var daysToExpiry by remember { mutableStateOf("30") }
    var isLifeSaving by remember { mutableStateOf(existingItem?.isLifeSaving ?: false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0D9488))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(if (existingItem == null) "Add New Medicine" else "Edit Medicine", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D9488))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, 
                    label = { Text("Medicine Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(color = Color.Black)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = quantity, onValueChange = { quantity = it }, 
                    label = { Text("Qty") },
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(color = Color.Black)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = daysToExpiry, onValueChange = { daysToExpiry = it }, 
                label = { Text("Days to Expiry") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(color = Color.Black)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isLifeSaving, onCheckedChange = { isLifeSaving = it })
                Text("Mark as Life-Saving (Emergency)", color = Color.Red, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF0D9488))
            } else {
                Button(
                    onClick = {
                        if (name.isNotBlank() && quantity.isNotBlank()) {
                            isLoading = true
                            val db = FirebaseFirestore.getInstance()
                            val id = existingItem?.id ?: UUID.randomUUID().toString()
                            val expiryDate = System.currentTimeMillis() + (daysToExpiry.toLongOrNull() ?: 30L) * 86400000L
                            val item = InventoryItem(
                                id = id, 
                                ownerId = pharmacy.ownerId, 
                                pharmacyId = pharmacy.id,
                                pharmacyName = pharmacy.name, 
                                pharmacyPhone = pharmacy.phone,
                                pharmacyLat = pharmacy.lat, 
                                pharmacyLng = pharmacy.lng,
                                name = name, 
                                searchName = name.lowercase(), 
                                quantity = quantity.toIntOrNull() ?: 0,
                                isLifeSaving = isLifeSaving, 
                                expiryDate = expiryDate,
                                updatedAt = System.currentTimeMillis()
                            )
                            db.collection("inventory").document(id).set(item).addOnSuccessListener { 
                                isLoading = false
                                Toast.makeText(context, if (existingItem == null) "Medicine added" else "Medicine updated", Toast.LENGTH_SHORT).show()
                                onAdded(item) 
                            }.addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Name and Quantity are required", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(if (existingItem == null) "Save to Inventory" else "Update Medicine", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
