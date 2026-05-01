package com.example.gramasanjeevini.utils

import com.example.gramasanjeevini.models.InventoryItem
import com.example.gramasanjeevini.models.Pharmacy
import com.google.firebase.firestore.FirebaseFirestore

object SeedData {
    fun seedDatabase() {
        val db = FirebaseFirestore.getInstance()
        
        val pharmacyId = "pharmacy_001"
        val pharmacistId = "pharmacist_001" 
        
        // Davanagere coordinates: 14.4644, 75.9218
        val mockPharmacy = Pharmacy(
            id = pharmacyId,
            ownerId = pharmacistId,
            name = "Sanjeevini Medical Store, Davanagere",
            address = "PB Road, Near Davanagere Railway Station",
            phone = "+91 98765 43210",
            lat = 14.4644,
            lng = 75.9218
        )

        val mockItems = listOf(
            InventoryItem(
                id = "item_001",
                ownerId = pharmacistId,
                pharmacyId = pharmacyId,
                pharmacyName = mockPharmacy.name,
                pharmacyPhone = mockPharmacy.phone,
                pharmacyLat = mockPharmacy.lat,
                pharmacyLng = mockPharmacy.lng,
                name = "Paracetamol 500mg",
                searchName = "paracetamol",
                quantity = 100,
                isLifeSaving = false,
                updatedAt = System.currentTimeMillis()
            ),
            InventoryItem(
                id = "item_002",
                ownerId = pharmacistId,
                pharmacyId = pharmacyId,
                pharmacyName = mockPharmacy.name,
                pharmacyPhone = mockPharmacy.phone,
                pharmacyLat = mockPharmacy.lat,
                pharmacyLng = mockPharmacy.lng,
                name = "Insulin Glargine",
                searchName = "insulin",
                quantity = 10,
                isLifeSaving = true,
                updatedAt = System.currentTimeMillis()
            )
        )

        // Upload Pharmacy
        db.collection("pharmacies").document(pharmacyId).set(mockPharmacy)
        
        // Upload Inventory
        mockItems.forEach { item ->
            db.collection("inventory").document(item.id).set(item)
        }
    }
}
