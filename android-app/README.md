# Elektropregled - Android Aplikacija

Android mobilna aplikacija za digitalizaciju elektropregleda s offline-first pristupom.

## 📋 Preduvjeti

- **Android Studio** (Hedgehog | 2023.1.1 ili noviji)
- **JDK 17** ili noviji
- **Android SDK** (minSdk 31, targetSdk 34)
- **Gradle 8.5+**

## 🚀 Kako Pokrenuti

### 1. Kloniraj Repozitorij

```bash
git clone https://github.com/your-username/PROJEKT-R.git
cd PROJEKT-R/android-app
```

### 2. Otvori Projekt u Android Studio

1. Otvori **Android Studio**
2. Odaberi **File → Open**
3. Navigiraj do `android-app` foldera i odaberi ga
4. Android Studio će automatski sinkronizirati Gradle dependencies

### 3. Konfiguriraj Backend URL (Opcionalno)

Ako koristiš custom backend URL, otvori:
```
app/src/main/java/com/example/elektropregled/data/api/ApiClient.kt
```

I promijeni `BASE_URL`:
```kotlin
private const val BASE_URL = "https://your-backend-url.com/api/"
```

**Default URL:** `https://elektropregled-api.onrender.com/api/`

### 4. Pokreni Aplikaciju

1. Poveži Android uređaj ili pokreni emulator (Android 12+)
2. Klikni **Run** (▶️) ili pritisni `Shift + F10`
3. Odaberi target device
4. Aplikacija će se instalirati i pokrenuti

## 📱 Funkcionalnosti

### Online Funkcionalnosti
- ✅ **Login** - Prijava s korisničkim imenom i lozinkom
- ✅ **Učitavanje postrojenja** - Lista svih postrojenja s informacijama
- ✅ **Učitavanje polja** - Lista polja unutar postrojenja
- ✅ **Učitavanje checkliste** - Uređaji i parametri za provjeru
- ✅ **Sinkronizacija** - Slanje pregleda na server

### Offline Funkcionalnosti
- ✅ **Kreiranje pregleda** - Rad bez interneta
- ✅ **Popunjavanje checkliste** - Unos vrijednosti offline
- ✅ **Lokalno spremanje** - Svi podaci se spremaju u lokalnu bazu
- ✅ **Automatska sinkronizacija** - WorkManager sinkronizira kada je internet dostupan

## 🏗️ Arhitektura

- **MVVM** (Model-View-ViewModel) pattern
- **Repository Pattern** (Local + Remote data sources)
- **Room Database** - Lokalna SQLite baza
- **Retrofit** - REST API komunikacija
- **WorkManager** - Pozadinska sinkronizacija
- **EncryptedSharedPreferences** - Sigurno spremanje JWT tokena

## 📂 Struktura Projekta

```
android-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/elektropregled/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/          # Retrofit API servisi
│   │   │   │   │   ├── database/     # Room entiteti i DAO
│   │   │   │   │   ├── repository/   # Repository pattern
│   │   │   │   │   └── sync/         # WorkManager sync
│   │   │   │   └── ui/
│   │   │   │       ├── screen/       # Fragments
│   │   │   │       └── viewmodel/    # ViewModels
│   │   │   └── res/                  # Layouts, drawables, strings
│   │   └── test/                      # Unit tests
│   └── build.gradle
├── gradle/
│   └── wrapper/                       # Gradle wrapper
├── build.gradle                       # Project-level build config
├── settings.gradle                    # Project settings
└── gradle.properties                 # Gradle properties
```

## 🔧 Build Konfiguracija

### SDK Versije
- **minSdk:** 31 (Android 12+)
- **compileSdk:** 34
- **targetSdk:** 34

### Glavne Dependencies
- AndroidX Core, Lifecycle, Navigation
- Room Database
- Retrofit + OkHttp
- WorkManager
- Security Crypto (EncryptedSharedPreferences)
- Material Design Components

## 🧪 Testiranje

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## 📦 Build APK

### Debug APK
```bash
./gradlew assembleDebug
```
APK će biti u: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew assembleRelease
```
APK će biti u: `app/build/outputs/apk/release/app-release.apk`

## 🔐 Autentifikacija

Aplikacija koristi **JWT (JSON Web Tokens)** za autentifikaciju:
- Token se sprema u **EncryptedSharedPreferences**
- Token se automatski dodaje u sve API zahtjeve
- Ako token istekne, korisnik se mora ponovno prijaviti

## 💾 Lokalna Baza Podataka

Aplikacija koristi **Room Database** za lokalno spremanje:
- **SQLite** baza podataka
- **Offline-first** pristup - svi podaci se spremaju lokalno
- **Automatska sinkronizacija** kada je internet dostupan

### Schema
Baza podataka koristi schema iz `mobilna_sqlite.sql`:
- Korisnik
- Postrojenje
- Polje
- VrstaUredaja
- Uredaj
- ParametarProvjere
- Pregled
- StavkaPregleda

## 🔄 Sinkronizacija

### Automatska Sinkronizacija
- **WorkManager** automatski pokušava sinkronizirati kada je internet dostupan
- Pregledi s `status_sinkronizacije = "PENDING"` se šalju na server

### Ručna Sinkronizacija
- Korisnik može ručno pokrenuti sinkronizaciju preko Sync ekrana
- Status sinkronizacije se prikazuje za svaki pregled

## 🐛 Troubleshooting

### Build Errors

**Problem:** `Gradle sync failed`
- Rješenje: Invalidate Caches / Restart u Android Studio
- Rješenje: Obriši `.gradle` folder i ponovno sync

**Problem:** `SDK not found`
- Rješenje: Instaliraj Android SDK preko Android Studio SDK Manager

### Runtime Errors

**Problem:** `404 Not Found` pri API pozivima
- Rješenje: Provjeri `BASE_URL` u `ApiClient.kt`
- Rješenje: Provjeri da je backend pokrenut

**Problem:** `FOREIGN KEY constraint failed`
- Rješenje: Obriši app data i ponovno instaliraj
- Rješenje: Provjeri da su svi entiteti spremljeni prije spremanja stavki

## 📚 Dodatna Dokumentacija

- [API Dokumentacija](../docs/API_DOKUMENTACIJA.md)
- [Quick Reference](../docs/QUICK_REFERENCE.md)
- [Implementacijska Uputstva](../docs/IMPLEMENTACIJSKA_UPUTSTVA.md)

## 👥 Kontakt

Za pitanja ili probleme, otvori issue na GitHub repozitoriju.

## 📄 Licenca

Vidi [LICENSE](../LICENSE) datoteku za detalje.
