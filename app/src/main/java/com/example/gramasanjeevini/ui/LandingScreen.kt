package com.example.gramasanjeevini.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LandingScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)) // gray-50
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and Title
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF059669) // emerald-600
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Grama Sanjeevini",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827) // gray-900
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Connecting rural medical stores into a single platform so you never have to travel far without knowing.",
            fontSize = 16.sp,
            color = Color(0xFF4B5563), // gray-600
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Feature List
        FeatureItem(
            icon = Icons.Default.Search,
            title = "Search Medicine",
            description = "Check availability across nearby shops instantly."
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FeatureItem(
            icon = Icons.Default.LocationOn,
            title = "Find Nearby",
            description = "Get exact distance to available medical stores."
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FeatureItem(
            icon = Icons.Default.CheckCircle,
            title = "Life-Saving Badges",
            description = "Quickly spot critical emergency stocks."
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Get Started Button
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // emerald-600
            shape = RoundedCornerShape(50), // rounded-full
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Get Started",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFECFDF5) // emerald-50
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF059669), // emerald-600
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF6B7280) // gray-500
            )
        }
    }
}
