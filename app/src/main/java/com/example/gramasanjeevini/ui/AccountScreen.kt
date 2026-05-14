package com.example.gramasanjeevini.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gramasanjeevini.models.UserProfile
import com.example.gramasanjeevini.utils.Utils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    userProfile: UserProfile?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onSignOut: () -> Unit,
    onUserProfileUpdated: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    // Edit profile state
    var showEditForm by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userProfile?.name ?: "") }
    var editPhone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    // Location state
    var isUpdatingLocation by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Map launcher for location picker
    val mapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            val address = result.data?.getStringExtra("address") ?: ""
            if (user != null && lat != 0.0) {
                isUpdatingLocation = true
                db.collection("users").document(user.uid)
                    .update(mapOf("lat" to lat, "lng" to lng))
                    .addOnSuccessListener {
                        isUpdatingLocation = false
                        locationStatus = if (address.isNotBlank()) address
                        else "%.4f°, %.4f°".format(lat, lng)
                        val updated = userProfile?.copy(lat = lat, lng = lng) ?: return@addOnSuccessListener
                        onUserProfileUpdated(updated)
                        Toast.makeText(context, "Location updated!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        isUpdatingLocation = false
                        Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Save profile edits
    fun saveProfile() {
        if (user == null) return
        if (editName.isBlank()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        db.collection("users").document(user.uid)
            .update(mapOf("name" to editName.trim(), "phone" to editPhone.trim()))
            .addOnSuccessListener {
                isSaving = false
                showEditForm = false
                val updated = userProfile?.copy(
                    name = editName.trim(),
                    phone = editPhone.trim()
                )
                if (updated != null) onUserProfileUpdated(updated)
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    val cardColor = if (isDarkMode) Color(0xFF1A2533) else Color.White
    val bgColor = if (isDarkMode) Color(0xFF0F1923) else Color(0xFFF3F4F6)
    val textPrimary = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF111827)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF6B7280)
    val dividerColor = if (isDarkMode) Color(0xFF2E3D52) else Color(0xFFF3F4F6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Account",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D9488))
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile Hero Card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0D9488), Color(0xFF0A7C6E))
                            )
                        )
                        .padding(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Avatar with initials
                        val initials = (userProfile?.name ?: "?")
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .take(2)
                            .joinToString("")

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = initials.ifEmpty { "?" },
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = userProfile?.name ?: "User",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = userProfile?.email ?: "",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )

                        if (userProfile?.phone?.isNotBlank() == true) {
                            Text(
                                text = userProfile.phone,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (userProfile?.role == "pharmacist") "⚕ Pharmacist" else "👤 Consumer",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Edit Profile Section ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AccountSectionHeader(
                        icon = Icons.Default.Edit,
                        title = "Edit Profile",
                        isDarkMode = isDarkMode,
                        trailingContent = {
                            TextButton(onClick = { showEditForm = !showEditForm }) {
                                Text(
                                    if (showEditForm) "Cancel" else "Edit",
                                    color = Color(0xFF0D9488),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = showEditForm,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, null, tint = Color(0xFF0D9488))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = TextStyle(color = textPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0D9488),
                                    unfocusedBorderColor = dividerColor,
                                    focusedLabelColor = Color(0xFF0D9488)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, null, tint = Color(0xFF0D9488))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = TextStyle(color = textPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0D9488),
                                    unfocusedBorderColor = dividerColor,
                                    focusedLabelColor = Color(0xFF0D9488)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isSaving) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF0D9488),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { saveProfile() },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                                ) {
                                    Icon(Icons.Default.Check, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    // Show current info when form is closed
                    if (!showEditForm) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(
                            label = "Name",
                            value = userProfile?.name ?: "—",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = dividerColor
                        )
                        ProfileInfoRow(
                            label = "Phone",
                            value = userProfile?.phone?.ifBlank { "Not set" } ?: "Not set",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            // ── Location Section ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AccountSectionHeader(
                        icon = Icons.Default.LocationOn,
                        title = "My Location",
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val currentLat = userProfile?.lat ?: 0.0
                    val currentLng = userProfile?.lng ?: 0.0

                    if (locationStatus != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = locationStatus!!,
                                fontSize = 13.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (currentLat != 0.0) {
                        Text(
                            text = "Current: %.4f°, %.4f°".format(currentLat, currentLng),
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    } else {
                        Text(
                            text = "Location not set",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isUpdatingLocation) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color(0xFF0D9488),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                // Open LocationPickerActivity to pick new location
                                val lat = userProfile?.lat?.takeIf { it != 0.0 } ?: Utils.DEFAULT_LAT
                                val lng = userProfile?.lng?.takeIf { it != 0.0 } ?: Utils.DEFAULT_LNG
                                val intent = Intent(context, LocationPickerActivity::class.java).apply {
                                    putExtra("lat", lat)
                                    putExtra("lng", lng)
                                }
                                mapLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                        ) {
                            Icon(Icons.Default.MyLocation, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Update My Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // ── Appearance Section ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AccountSectionHeader(
                        icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        title = "Appearance",
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = textPrimary
                            )
                            Text(
                                text = if (isDarkMode) "Switch to light theme" else "Switch to dark theme",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0D9488),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
            }

            // ── Sign Out ──────────────────────────────────────────────────
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun AccountSectionHeader(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val textPrimary = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF111827)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0D9488).copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0D9488),
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textPrimary
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = textSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
    }
}
