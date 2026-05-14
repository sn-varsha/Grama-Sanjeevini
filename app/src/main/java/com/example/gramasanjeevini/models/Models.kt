package com.example.gramasanjeevini.models

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val role: String = "",          // "consumer" or "pharmacist"
    val phone: String = "",         // optional contact number
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Pharmacy(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "Not provided",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class InventoryItem(
    val id: String = "",
    val ownerId: String = "",
    val pharmacyId: String = "",
    val pharmacyName: String = "",
    val pharmacyPhone: String = "",
    val pharmacyLat: Double = 0.0,
    val pharmacyLng: Double = 0.0,
    val name: String = "",
    val searchName: String = "",
    val quantity: Int = 0,
    @get:PropertyName("isLifeSaving")
    @set:PropertyName("isLifeSaving")
    var isLifeSaving: Boolean = false,
    val expiryDate: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
