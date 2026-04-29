package com.example.gramasanjeevini.models

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val role: String = "", // "consumer" or "pharmacist"
    val createdAt: Long = System.currentTimeMillis()
)

data class Pharmacy(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class InventoryItem(
    val id: String = "",
    val ownerId: String = "",
    val pharmacyId: String = "",
    val pharmacyName: String = "",
    val pharmacyLat: Double = 0.0,
    val pharmacyLng: Double = 0.0,
    val name: String = "",
    val searchName: String = "",
    val quantity: Int = 0,
    val isLifeSaving: Boolean = false,
    val expiryDate: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
