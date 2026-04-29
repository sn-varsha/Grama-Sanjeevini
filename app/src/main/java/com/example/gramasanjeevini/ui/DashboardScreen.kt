package com.example.gramasanjeevini.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Welcome back,", color = Color.Gray, fontSize = 14.sp)
                    Text("User", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Symptom Checker Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("symptom_checker") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Check Symptoms & Get AI Guidance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Describe how you feel, and our AI will provide preliminary advice.", color = Color(0xFFCCFBF1), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Start Checkup ->", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Other Services", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Nearby Clinics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("care_centers") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Column {
                        Text("Nearby Care & Stores", fontWeight = FontWeight.Medium)
                        Text("Find PHCs, Hospitals & Pharmacies", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
