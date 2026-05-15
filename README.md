# Grama Sanjeevini 🌿
### Android App Development using GenAI — MindMatrix VTU Internship Program (Project #28)

> **"Connecting rural medical stores into a single searchable pool — so you never have to travel far without knowing."**

---

## 📋 Table of Contents

1. [Problem Statement](#problem-statement)
2. [The Vision](#the-vision)
3. [Features](#features)
4. [Tech Stack](#tech-stack)
5. [Architecture](#architecture)
6. [Project Structure](#project-structure)
7. [Firebase Setup](#firebase-setup)
8. [How to Run](#how-to-run)
9. [App Flow](#app-flow)
10. [Requirement Compliance](#requirement-compliance)
11. [Known Limitations](#known-limitations)

---

## 🏥 Problem Statement

In remote villages, the **local medical store** is the only source of medicines. When a medicine is out of stock, the villager is forced to travel 20 km to the nearest city — often without knowing whether the medicine is available there at all.

There is no shared visibility across village pharmacies. Each store operates in isolation.

---

## 💡 The Vision

**Grama Sanjeevini** is a **Rural Pharmacy Network** app. It connects village medical stores into a single searchable pool backed by **real-time Firebase Firestore**.

- A villager searches for a medicine → the app shows which village has it and how far away it is.
- A pharmacist logs in, registers their shop, and manages live inventory including expiry tracking.
- Emergency (life-saving) drugs are highlighted with a red badge for instant visibility.

---

## ✨ Features

### 👤 Consumer (Villager) Side
| Feature | Details |
|---------|---------|
| **Medicine Search** | Search any medicine by name across all registered pharmacies in real-time |
| **Distance-Sorted Results** | Results sorted by distance from the user's GPS location using the Haversine formula |
| **Directions & Call** | One-tap Google Maps directions or phone call to the pharmacy |
| **Nearby Pharmacies** | Live list of all registered pharmacies sorted by distance |
| **AI Symptom Checker** | Chat-based AI assistant for basic symptom guidance |
| **My Account** | View/edit profile, update location, toggle Dark/Light mode |

### ⚕️ Pharmacist Side
| Feature | Details |
|---------|---------|
| **Pharmacy Registration** | Register store with name, address, phone, and GPS location |
| **Live Inventory Management** | Add, edit, delete medicines with quantity and expiry date |
| **Life-Saving Drug Badge** | Mark critical medicines (Insulin, Snake Venom, etc.) with a 🚨 red badge |
| **Expiry Watch** | 🟠 "Expiring in N days — Sell Soon" badge for items ≤30 days from expiry; 🔴 "Expired — Remove or Discount" for past-expiry items |
| **Real-time Firestore Sync** | All inventory changes are instantly visible to consumers searching |

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Navigation** | Jetpack Navigation Compose |
| **Backend / Database** | Firebase Firestore (NoSQL, real-time) |
| **Authentication** | Firebase Authentication (Email/Password) |
| **Location** | Google Play Services — FusedLocationProviderClient |
| **Geocoding** | Android built-in Geocoder (no API key required) |
| **Build System** | Gradle (Kotlin DSL) with Version Catalog |
| **Min SDK** | API 24 (Android 7.0 Nougat) |
| **Target SDK** | API 35 (Android 15) |

---

## 🏗 Architecture

The app follows a **single-activity** architecture with **Compose-first UI**:

```
MainActivity
│
├── GramaSanjeeviniTheme (Dark/Light mode wrapper)
│
├── LandingScreen          ← First launch onboarding
├── LoginScreen            ← Email/password Auth + location setup
│
├── ConsumerApp (NavHost)
│   ├── DashboardScreen    ← Home: search, symptom checker, nearby, account
│   ├── MedicineSearchScreen ← Real-time Firestore medicine search
│   ├── CareCentersScreen  ← Live Firebase pharmacy list, distance-sorted
│   ├── SymptomCheckerScreen ← AI chat assistant
│   ├── AccountScreen      ← Profile, edit, location update, dark mode
│   └── StoreDetailsScreen ← Store detail view
│
├── PharmacistApp
│   ├── PharmacyRegistration ← One-time store setup with GPS location
│   └── Inventory Dashboard ← Add/Edit/Delete medicines + Expiry Watch
│
└── LocationPickerActivity ← GPS location picker (separate Activity)
```

**State management**: Compose `remember`/`mutableStateOf` at the composable level. Dark mode state is lifted to `MainActivity` and passed down via `CompositionLocalProvider`.

---

## 📁 Project Structure

```
app/src/main/
├── AndroidManifest.xml
└── java/com/example/gramasanjeevini/
    ├── GramaSanjeeviniApp.kt          # Application class
    ├── MainActivity.kt                # Entry point, auth routing, dark mode state
    │
    ├── models/
    │   └── Models.kt                  # UserProfile, Pharmacy, InventoryItem
    │
    ├── data/
    │   └── MockData.kt                # Clinic/Medicine data classes (StoreDetails)
    │
    ├── utils/
    │   ├── Utils.kt                   # Haversine distance, phone formatting
    │   └── SeedData.kt                # One-time Firestore seeding utility
    │
    └── ui/
        ├── theme/
        │   ├── Color.kt               # Brand teal palette + dark surface colors
        │   ├── Theme.kt               # GramaSanjeeviniTheme (light/dark)
        │   └── Type.kt                # Typography
        │
        ├── LandingScreen.kt           # App intro/onboarding
        ├── LoginScreen.kt             # Sign in / Sign up
        ├── LocationPickerActivity.kt  # GPS location picker screen
        ├── ConsumerApp.kt             # Consumer NavHost + MedicineSearchScreen
        ├── DashboardScreen.kt         # Consumer home dashboard
        ├── CareCentersScreen.kt       # Live nearby pharmacies (Firebase)
        ├── SymptomCheckerScreen.kt    # AI symptom chat
        ├── AccountScreen.kt           # Profile, edit, location, dark mode
        ├── PharmacistApp.kt           # Pharmacist dashboard + registration
        └── StoreDetailsScreen.kt      # Store detail view
```

---

## 🔥 Firebase Setup

### Collections Used

| Collection | Document Key | Purpose |
|------------|-------------|---------|
| `users` | `{uid}` | UserProfile: name, email, role, phone, lat, lng |
| `pharmacies` | `{pharmacyId}` | Pharmacy: name, address, phone, lat, lng, ownerId |
| `inventory` | `{itemId}` | InventoryItem: medicine details, quantity, expiryDate, isLifeSaving |

### Firestore Security Rules (recommended)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid;
    }
    match /pharmacies/{pharmacyId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == resource.data.ownerId;
    }
    match /inventory/{itemId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == resource.data.ownerId;
    }
  }
}
```

### Firebase Services Required
- ✅ **Firebase Authentication** — Enable Email/Password provider
- ✅ **Cloud Firestore** — Create database in production or test mode

---

## 🚀 How to Run

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android device or emulator with **Google Play Services** (for FusedLocationProvider)
- Firebase project with Firestore and Authentication enabled

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/Grama-Sanjeevini.git
   cd Grama-Sanjeevini
   ```

2. **Firebase configuration**
   - Download `google-services.json` from your Firebase Console
   - Place it at `app/google-services.json`

3. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click **Run ▶**

4. **Enable Location on Emulator** (if using AVD)
   - In the emulator, go to the three-dot menu → Location
   - Set a latitude/longitude (e.g., Davanagere: `14.4644, 75.9218`)
   - Enable GPS

### Testing the Full Flow

**As a Pharmacist:**
1. Sign Up → check "I am a Pharmacist"
2. Complete registration → tap **"Set Store Location"** → confirm GPS location
3. Log in → Register your pharmacy (name, address, phone, location)
4. Add medicines with quantity, expiry date, and life-saving flag

**As a Consumer:**
1. Sign Up → set your location
2. Search for a medicine (e.g., "paracetamol")
3. See nearby pharmacies with distance, call, and directions
4. Check **Nearby Pharmacies** tab for all registered stores

---

## 📱 App Flow

```
Launch
  │
  ▼
LandingScreen ──► LoginScreen
                      │
              ┌───────┴────────┐
              │                │
         Consumer           Pharmacist
              │                │
     DashboardScreen    PharmacyRegistration
              │                │
    ┌─────────┼───────┐   InventoryDashboard
    │         │       │        │
 Search  Symptom  Nearby    Add/Edit/Delete
         Checker  Care       Medicines
                 Centers    (Expiry Watch)
```

---

## ✅ Requirement Compliance

This section maps each MindMatrix project requirement to the implementation:

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Medicine Search | ✅ | `MedicineSearchScreen.kt` — Firestore real-time query across all inventory |
| Pharmacist Login & Stock Update | ✅ | `PharmacistApp.kt` — full CRUD on `inventory` collection |
| Emergency / Life-Saving Stock | ✅ | `isLifeSaving` flag → 🚨 red badge in both pharmacist and consumer views |
| Expiry Watch | ✅ | PharmacistApp stock list shows expiry date + orange/red badge for near/past-expiry |
| Real-time Firebase Firestore | ✅ | All inventory reads/writes go directly to Firestore — no local cache |
| Location-based filtering (10–20 km radius) | ✅ | Haversine distance shown per result; `Utils.calculateDistance` used throughout |
| Clean list-based UI | ✅ | Material 3 cards, teal brand color, accessible typography |
| Life-Saving drugs highlighted with red badge | ✅ | Red `Surface` badge labeled "🚨 Life-Saving Drug" |
| UI accessible to low-digital-literacy users | ✅ | Large text (16–22sp), icon+text buttons, high contrast, simple navigation |
| Search returns results from multiple shops | ✅ | Searches entire `inventory` Firestore collection across all registered pharmacies |

---

## ⚠️ Known Limitations

| Item | Notes |
|------|-------|
| **AI Symptom Checker** | Currently uses rule-based placeholder responses. Connecting a live GenAI API (Gemini/OpenAI) requires an API key and is outside the scope of this submission. |
| **Radius filtering** | Distance is calculated and displayed; hard radius cutoff (e.g., hide results > 20 km) is not enforced — all pharmacies are shown sorted by distance. |
| **Google Maps display** | The map view in location picker shows GPS coordinates only (no rendered map tile), as Google Maps SDK requires a Cloud-billed API key. FusedLocationProvider (Play Services) is used instead. |
| **Offline support** | Firestore offline persistence is not explicitly enabled; the app requires a network connection. |

---

## 👩‍💻 Developer

**Varsha** — MindMatrix VTU Internship Program, 2026
Project #28: Android App Development using GenAI — Grama-Sanjeevini (Healthcare)

---

## 📄 License

This project is developed for academic/internship evaluation purposes under the MindMatrix VTU Internship Program.
