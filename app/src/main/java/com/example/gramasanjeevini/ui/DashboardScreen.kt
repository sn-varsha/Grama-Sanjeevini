package com.example.gramasanjeevini.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome back,", color = Color.Gray, fontSize = 14.sp)
                    Text("Healthy Citizen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                IconButton(onClick = onSignOut) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFF0D9488))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Main Search Action
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("search") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE6F4F1),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.padding(12.dp).fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Search Medicines", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                        Text("Find availability in nearby shops", color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Symptom Checker Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("symptom_checker") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp).fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Check Symptoms", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Get AI-powered health guidance", color = Color(0xFFCCFBF1), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Health Services", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            // Nearby Clinics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("care_centers") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.padding(10.dp).fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Nearby Care Centers", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                        Text("PHCs, Hospitals & Pharmacies", color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
