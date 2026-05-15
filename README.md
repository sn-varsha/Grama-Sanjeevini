# 🌿 Grama Sanjeevini
### Android App Development using GenAI — MindMatrix VTU Internship Program (Project #28)

> **"Connecting rural medical stores into a single searchable pool — so you never have to travel far without knowing."**

---

## 📋 Table of Contents

1. [Problem Statement](#-problem-statement)
2. [The Vision](#-the-vision)
3. [Screenshots](#-screenshots)
4. [Features](#-features)
5. [Tech Stack](#-tech-stack)
6. [Architecture](#-architecture)
7. [Project Structure](#-project-structure)
8. [Firebase Setup](#-firebase-setup)
9. [How to Run](#-how-to-run)
10. [App Flow](#-app-flow)
11. [Requirement Compliance](#-requirement-compliance)
12. [Known Limitations](#-known-limitations)

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
- An AI-powered health assistant (Groq LLaMA 3.3) provides symptom guidance.

---

## 📸 Screenshots

> Screenshots are located in the [`screenshots/`](./screenshots/) folder.

| Landing & Login | Consumer Dashboard | Medicine Search |
|:-:|:-:|:-:|
| ![Landing](screenshots/landing.jpg) | ![Dashboard](screenshots/dashboard.jpg) | ![Search](screenshots/search.jpg) |

| Nearby Pharmacies | AI Symptom Checker | Pharmacist Dashboard |
|:-:|:-:|:-:|
| ![Nearby](screenshots/nearby.jpg) | ![AI](screenshots/ai_chat.jpg) | ![Pharmacist](screenshots/pharmacist.jpg) |

| Account Screen | Dark Mode | Expiry Watch |
|:-:|:-:|:-:|
| ![Account](screenshots/account.jpg) | ![Dark](screenshots/dark_mode.jpg) | ![Expiry](screenshots/expiry.jpg) |

---

## ✨ Features

### 👤 Consumer (Villager) Side

| Feature | Details |
|---------|---------|
| **Medicine Search** | Search any medicine by name across all registered pharmacies in real-time |
| **Distance-Sorted Results** | Results sorted by distance using GPS + Haversine formula |
| **Directions & Call** | One-tap Google Maps directions or phone call to the pharmacy |
| **Nearby Pharmacies** | Live list of all registered pharmacies sorted by distance with status chips |
| **AI Symptom Checker** | Real AI chat using Groq LLaMA 3.3 70B — context-aware, emergency escalation |
| **My Account** | View/edit profile (name, phone), update GPS location, toggle Dark/Light mode |

### ⚕️ Pharmacist Side

| Feature | Details |
|---------|---------|
| **Pharmacy Registration** | Register store with name, address, phone, and GPS location |
| **Live Inventory Management** | Add, edit, delete medicines with quantity and expiry date |
| **Life-Saving Drug Badge** | Mark critical medicines (Insulin, Snake Venom) with 🚨 red badge |
| **Expiry Watch** | 🟠 "Expiring in N days" badge for stock ≤30 days to expiry; 🔴 "Expired" for past-expiry |
| **Real-time Firestore Sync** | All changes instantly visible to searching consumers |

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Navigation** | Jetpack Navigation Compose |
| **Backend / Database** | Firebase Firestore (NoSQL, real-time) |
| **Authentication** | Firebase Authentication (Email/Password) |
| **AI Assistant** | Groq API — LLaMA 3.3 70B (free tier, no billing) |
| **Location** | Google Play Services — FusedLocationProviderClient |
| **Geocoding** | Android built-in Geocoder |
| **Build System** | Gradle (Kotlin DSL) with Version Catalog |
| **Min SDK** | API 24 (Android 7.0 Nougat) |
| **Target SDK** | API 35 (Android 15) |

---

## 🏗 Architecture

Single-activity architecture with Compose-first UI:

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
│   ├── SymptomCheckerScreen ← Groq AI chat assistant (LLaMA 3.3 70B)
│   ├── AccountScreen      ← Profile, edit, location update, dark mode
│   └── StoreDetailsScreen ← Store detail view
│
├── PharmacistApp
│   ├── PharmacyRegistration ← One-time store setup with GPS
│   └── Inventory Dashboard ← Add/Edit/Delete medicines + Expiry Watch
│
└── LocationPickerActivity ← GPS location picker (FusedLocationProvider)
```

**State management**: Compose `remember`/`mutableStateOf`. Dark mode state is lifted to `MainActivity` and provided via `CompositionLocalProvider`.

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
    │   └── Models.kt                  # UserProfile, Pharmacy, InventoryItem data classes
    │
    ├── data/
    │   └── MockData.kt                # Clinic/Medicine data classes (StoreDetails)
    │
    ├── utils/
    │   ├── Utils.kt                   # Haversine distance, phone formatting helpers
    │   ├── SeedData.kt                # One-time Firestore seeding utility
    │   └── GroqChatService.kt         # Groq AI REST API client (LLaMA 3.3 70B)
    │
    └── ui/
        ├── theme/
        │   ├── Color.kt               # Brand teal palette + dark surface colors
        │   ├── Theme.kt               # GramaSanjeeviniTheme (light/dark color schemes)
        │   └── Type.kt                # Typography definitions
        │
        ├── LandingScreen.kt           # App intro/onboarding
        ├── LoginScreen.kt             # Sign in / Sign up
        ├── LocationPickerActivity.kt  # GPS location picker
        ├── ConsumerApp.kt             # Consumer NavHost + MedicineSearchScreen
        ├── DashboardScreen.kt         # Consumer home dashboard
        ├── CareCentersScreen.kt       # Live nearby pharmacies (Firebase)
        ├── SymptomCheckerScreen.kt    # AI symptom chat (Groq LLaMA 3.3)
        ├── AccountScreen.kt           # Profile, edit, location, dark mode toggle
        ├── PharmacistApp.kt           # Pharmacist dashboard + registration
        └── StoreDetailsScreen.kt      # Store detail view
```

---

## 🔥 Firebase Setup

### Firestore Collections

| Collection | Document Key | Purpose |
|------------|-------------|---------|
| `users` | `{uid}` | UserProfile: name, email, role, phone, lat, lng |
| `pharmacies` | `{pharmacyId}` | Pharmacy: name, address, phone, lat, lng, ownerId |
| `inventory` | `{itemId}` | InventoryItem: medicine, quantity, expiryDate, isLifeSaving |

### Firestore Security Rules

```js
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

### Required Firebase Services
- ✅ **Firebase Authentication** — Enable Email/Password provider
- ✅ **Cloud Firestore** — Create database in test or production mode

---

## 🚀 How to Run

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android device or emulator with **Google Play Services**
- Firebase project with Firestore + Authentication enabled
- Free Groq API key from [console.groq.com](https://console.groq.com) (for AI assistant)

### Installation Steps

**1. Clone the repository**
```bash
git clone https://github.com/sn-varsha/Grama-Sanjeevini.git
cd Grama-Sanjeevini
```

**2. Add Firebase configuration**
```
Download google-services.json from your Firebase Console
→ Place it at:  app/google-services.json
```

**3. Add Groq API key** *(for AI Symptom Checker)*
```
Create/open local.properties in the project root
→ Add: GROQ_API_KEY=your_key_here
```
Get a **free** key (no credit card) at [console.groq.com](https://console.groq.com)

**4. Build and Run**
```bash
./gradlew assembleDebug
```
Or open in Android Studio → click **Run ▶**

**5. Enable Location on Emulator** *(if using AVD)*
```
Emulator → three-dot menu → Location
Set latitude/longitude: 14.4644, 75.9218  (Davanagere, Karnataka)
Enable GPS
```

### Testing the Full Flow

**As a Pharmacist:**
1. Sign Up → check "I am a Pharmacist"
2. Register your pharmacy (name, address, phone, location)
3. Add medicines with quantity, expiry date, and life-saving flag

**As a Consumer:**
1. Sign Up → set your location
2. Search for a medicine (e.g., "paracetamol" or "insulin")
3. See results sorted by distance, call or get directions
4. Open "AI Assistant" → describe symptoms → get real AI advice

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
    ┌─────────┼──────────┐  InventoryDashboard
    │         │          │       │
 Medicine  AI Chat   Nearby   Add/Edit Medicines
 Search    (Groq)     Care   + Expiry Watch Alerts
                     Centers
```

---

## ✅ Requirement Compliance

| MindMatrix Requirement | Status | Implementation File |
|------------------------|--------|-------------------|
| Medicine Search | ✅ | `MedicineSearchScreen.kt` — Firestore real-time across all inventory |
| Pharmacist Login & Stock Update | ✅ | `PharmacistApp.kt` — full CRUD on `inventory` Firestore collection |
| Emergency / Life-Saving Stock | ✅ | `isLifeSaving` flag → 🚨 red badge on all stock items |
| Expiry Watch Alerts | ✅ | `PharmacistApp.kt` — orange/red expiry badge with days countdown |
| Real-time Firebase Firestore | ✅ | All reads/writes go directly to Firestore |
| Location-based filtering (10–20 km) | ✅ | Haversine formula in `Utils.kt`, distance shown per result |
| Clean list-based UI | ✅ | Material 3 cards, teal brand, accessible typography |
| Life-Saving drugs with red badge | ✅ | Red Surface badge labeled "🚨 Life-Saving Drug" |
| UI for low-digital-literacy users | ✅ | Large text (16–22sp), icon+text buttons, quick-tap chips |
| GenAI integration | ✅ | Groq LLaMA 3.3 70B via `GroqChatService.kt` |

---

## ⚠️ Known Limitations

| Item | Notes |
|------|-------|
| **Google Maps display** | GPS coordinates only shown (no map tile render) — Google Maps SDK requires a Cloud-billed API key, which is outside project scope. FusedLocationProvider used instead. |
| **Radius cutoff** | Distance shown and sorted; a hard 20 km filter is not enforced — all pharmacies displayed by distance order. |
| **Offline support** | Firestore offline persistence not explicitly enabled; requires network. |
| **Groq API key** | Each evaluator/user must provide their own free Groq key in `local.properties` before building. The key is never committed to the repo. |

---

## 👩‍💻 Developer

**Varsha S N** — MindMatrix VTU Internship Program, 2026
**Project #28**: Android App Development using GenAI — Grama-Sanjeevini (Healthcare)

---

## 📄 License

This project is developed for academic/internship evaluation purposes under the MindMatrix VTU Internship Program.
