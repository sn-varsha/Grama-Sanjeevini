package com.example.gramasanjeevini.utils

import java.util.Locale
import kotlin.math.*

object Utils {
    // Default location: Davanagere City Center
    const val DEFAULT_LAT = 14.4644
    const val DEFAULT_LNG = 75.9218

    /**
     * Calculates the distance between two points in KM using Haversine formula.
     * Returns -1.0 if coordinates are invalid (approx 0.0).
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Check for invalid/empty coordinates (Null Island or very close to it)
        if (abs(lat1) < 0.01 && abs(lon1) < 0.01) return -1.0
        if (abs(lat2) < 0.01 && abs(lon2) < 0.01) return -1.0
        
        val r = 6371.0 // Radius of earth in KM
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Formats a phone number for the dialer.
     * Prefixes with +91 to ensure Indian formatting in system dialer and avoid US-style () format.
     */
    fun formatForDialer(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            phone.startsWith("+") -> phone
            else -> if (digits.isNotEmpty()) "+91$digits" else ""
        }
    }
    
    /**
     * Formats a phone number for display: +91 XXXXX XXXXX
     */
    fun formatDisplayPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
            digits.length == 12 && digits.startsWith("91") -> "+91 ${digits.substring(2, 7)} ${digits.substring(7)}"
            else -> phone
        }
    }
}
