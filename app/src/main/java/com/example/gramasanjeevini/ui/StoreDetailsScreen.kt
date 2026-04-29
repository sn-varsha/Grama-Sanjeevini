package com.example.gramasanjeevini.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gramasanjeevini.data.MOCK_CLINICS
import com.example.gramasanjeevini.data.MOCK_MEDICINES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailsScreen(navController: NavController, storeId: Int?) {
    val clinic = MOCK_CLINICS.find { it.id == storeId }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMedicines = if (searchQuery.isNotBlank()) {
        MOCK_MEDICINES.filter { it.name.contains(searchQuery, ignoreCase = true) || it.use.contains(searchQuery, ignoreCase = true) }
    } else {
        MOCK_MEDICINES
    }

    if (clinic == null) {
        navController.popBackStack()
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        TopAppBar(
            title = { Text(clinic.name) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search medicines...") },
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Available Medicines", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredMedicines) { med ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(med.name, fontWeight = FontWeight.Bold, color = if (med.inStock) Color.Black else Color.Gray)
                                Text(med.use, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (med.inStock) "In Stock" else "Out of Stock",
                                    color = if (med.inStock) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(med.price, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
