package com.example.gramasanjeevini.utils

import com.example.gramasanjeevini.models.InventoryItem
import com.example.gramasanjeevini.models.Pharmacy
import com.google.firebase.firestore.FirebaseFirestore

object SeedData {
    fun seedDatabase() {
        val db = FirebaseFirestore.getInstance()
        
        val pharmacyId = "pharmacy_001"
        val pharmacistId = "pharmacist_001" // This should ideally be a real UID after login
        
        val mockPharmacy = Pharmacy(
            id = pharmacyId,
            ownerId = pharmacistId,
            name = "Sanjeevini Medical Store",
            address = "Anantapur Main Road",
            lat = 16.5061,
            lng = 80.6480
        )

        val mockItems = listOf(
            InventoryItem(
                id = "item_001",
                ownerId = pharmacistId,
                pharmacyId = pharmacyId,
                pharmacyName = mockPharmacy.name,
                pharmacyLat = mockPharmacy.lat,
                pharmacyLng = mockPharmacy.lng,
                name = "Paracetamol 500mg",
                searchName = "paracetamol",
                quantity = 100,
                isLifeSaving = false
            ),
            InventoryItem(
                id = "item_002",
                ownerId = pharmacistId,
                pharmacyId = pharmacyId,
                pharmacyName = mockPharmacy.name,
                pharmacyLat = mockPharmacy.lat,
                pharmacyLng = mockPharmacy.lng,
                name = "Insulin Glargine",
                searchName = "insulin",
                quantity = 10,
                isLifeSaving = true
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
