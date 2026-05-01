package com.gramasanjeevini.data

data class Clinic(
    val id: Int,
    val name: String,
    val distance: String,
    val address: String,
    val phone: String,
    val status: String,
    val type: String,
    val rating: Double,
    val imageUrl: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Medicine(
    val id: Int,
    val name: String,
    val use: String,
    val price: String,
    val inStock: Boolean
)

val MOCK_CLINICS = listOf(
    Clinic(
        id = 1,
        name = "District Hospital, Davanagere",
        distance = "2.4 km",
        address = "Near Bapuji Hospital Road",
        phone = "+91 81922 21100",
        status = "Open 24/7",
        type = "Govt Hospital",
        rating = 4.2,
        imageUrl = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=500&q=80",
        lat = 14.4673,
        lng = 75.9216
    ),
    Clinic(
        id = 2,
        name = "City Pharmacy & Health Center",
        distance = "3.1 km",
        address = "P J Extension, Davanagere",
        phone = "+91 98450 12345",
        status = "Closes at 10 PM",
        type = "Private Clinic",
        rating = 4.5,
        imageUrl = "https://images.unsplash.com/photo-1586282391129-76a6df230234?auto=format&fit=crop&w=500&q=80",
        lat = 14.4640,
        lng = 75.9250
    ),
    Clinic(
        id = 3,
        name = "Vishwa Health Care, Davanagere",
        distance = "5.5 km",
        address = "Hadadi Road, Davanagere",
        phone = "+91 800 123 4567",
        status = "Open 24/7",
        type = "Multi-speciality",
        rating = 4.0,
        imageUrl = "https://images.unsplash.com/photo-1538108149393-cebb3010b96b?auto=format&fit=crop&w=500&q=80",
        lat = 14.4550,
        lng = 75.9180
    )
)

val MOCK_MEDICINES = listOf(
    Medicine(1, "Paracetamol 500mg", "Fever & Pain Relief", "₹15", true),
    Medicine(2, "Amoxicillin 250mg", "Antibiotic", "₹45", true),
    Medicine(3, "Cough Syrup Adulsa", "Cough Relief", "₹60", false),
    Medicine(4, "Vitamin C Zinc", "Immunity Booster", "₹30", true),
    Medicine(5, "ORS Powder", "Rehydration", "₹20", true),
    Medicine(6, "Ibuprofen 400mg", "Pain Relief", "₹25", true),
    Medicine(7, "Cetirizine 10mg", "Allergy Relief", "₹18", true),
    Medicine(8, "Azithromycin 500mg", "Antibiotic", "₹75", false)
)
