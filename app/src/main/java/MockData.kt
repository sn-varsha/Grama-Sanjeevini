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
    val imageUrl: String
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
        name = "Primary Health Centre (PHC), Anantapur",
        distance = "2.4 km",
        address = "Main Road, Near Panchayat Office",
        phone = "+91 98765 43210",
        status = "Open 24/7",
        type = "Govt Facility",
        rating = 4.2,
        imageUrl = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=500&q=80"
    ),
    Clinic(
        id = 2,
        name = "Sanjeevini Pharmacy & Clinic",
        distance = "3.1 km",
        address = "Market Street, Block 4",
        phone = "+91 98765 43211",
        status = "Closes at 8 PM",
        type = "Private Clinic",
        rating = 4.5,
        imageUrl = "https://images.unsplash.com/photo-1586282391129-76a6df230234?auto=format&fit=crop&w=500&q=80"
    ),
    Clinic(
        id = 3,
        name = "Community Health Centre",
        distance = "5.5 km",
        address = "District Hospital Road",
        phone = "+91 800 123 4567",
        status = "Open 24/7",
        type = "Govt Facility",
        rating = 4.0,
        imageUrl = "https://images.unsplash.com/photo-1538108149393-cebb3010b96b?auto=format&fit=crop&w=500&q=80"
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
