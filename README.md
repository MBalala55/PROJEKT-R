# Ogledna mobilna aplikacija za redoviti mjesečni pregled elemenata elektroenergetskih postrojenja

## 📁 Struktura Projekta

```
PROJEKT-R/
├── android-app/          # 📱 Android aplikacija (vidi android-app/README.md)
├── server/               # 🖥️ Spring Boot backend
├── baza/                 # 💾 Database scripts
└── docs/                 # 📚 Dokumentacija
```

## Brzi start

### Backend Development (Lokalno)

```bash
# 1. Kloniraj repo
git clone https://github.com/your-username/PROJEKT-R.git
cd PROJEKT-R

# 2. Kreiraj PostgreSQL bazu
psql -U postgres
CREATE DATABASE elektropregled;

# 3. Učitaj schema
psql -U postgres -d elektropregled -f baza/scripts/baza.sql

# 4. Pokreni backend
cd server
mvn spring-boot:run

# 5. Testiraj API
# Swagger UI: http://localhost:8080/api/swagger-ui.html
# Login: POST http://localhost:8080/api/v1/auth/login
```

### Android Development

Za Android aplikaciju, vidi **[android-app/README.md](android-app/README.md)**.

**Brzi start:**
```bash
cd android-app
# Otvori android-app folder u Android Studio
```

### Production (Render)

Slijedi **[RENDER_DEPLOY.md](RENDER_DEPLOY.md)** za detaljne korake.

## Tehnologije

### Backend
- **Java 17** + **Spring Boot 3.2.1**
- **PostgreSQL 15+**
- **JPA/Hibernate** sa native SQL DISTINCT ON optimizacijom
- **Spring Security** sa JWT (jjwt 0.12.3, HS512)
- **Swagger/OpenAPI** za dokumentaciju
- **Maven** za build

### Zavisnosti
```xml
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- postgresql:postgresql
- io.jsonwebtoken:jjwt (0.12.3)
- org.springdoc:springdoc-openapi-starter-webmvc-ui
```

## API Endpointi

| Metoda | Endpoint | Opis |
|--------|----------|------|
| POST | `/v1/auth/login` | Login + dobivanje JWT tokena |
| GET | `/v1/postrojenja` | Lista svih postrojenja |
| GET | `/v1/postrojenja/{id}/polja` | Lista polja u postrojenju |
| GET | `/v1/postrojenja/{id}/checklist?id_polje={id}` | Checklist uređaja + parametri |
| POST | `/v1/pregled/sync` | Sinkronizacija pregleda |

**Puna dokumentacija:** [API_DOKUMENTACIJA.md](docs/API_DOKUMENTACIJA.md)

## Autentifikacija

Svi zahtjevi trebaju JWT token u Authorization headeru:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtbWFya292aWMiLCJpYXQiOjE3Njk0NDE5MjUsImV4cCI6MTc2OTUyODMyNX0...
```

Token se dobija iz login endpointa:
```http
POST /v1/auth/login
Content-Type: application/json

{
  "korisnicko_ime": "mmarkovic",
  "lozinka": "pass123"
}
```

## Baza Podataka

### Schema
- **Korisnik**
- **Postrojenje**
- **Polje**
- **VrstaUredaja**
- **Uređaj**
- **ParametarProvjere**
- **Pregled**
- **StavkaPregleda**

### Učitavanje Podataka

```bash
# Učitaj CSV datoteke u PostgreSQL
psql -U postgres -d elektropregled -f baza/scripts/baza.sql

# Ili koristi DBeaver / pgAdmin
# File → Import data from CSV
```

## Testiranje

### Integration Tests (Spring Boot)

```bash
cd server
mvn test
```

### Manual Testing

Koristi **[test-api.http](server/test-api.http)** sa VS Code REST Client extenzijom:
- Klikni "Send Request" iznad svakog zahtjeva
- Automatski učitava token iz login-a

### Swagger UI

```
http://localhost:8080/api/swagger-ui.html
```

Svi endpointi su dokumentirani sa primjerima request/response-a.

## Dokumentacija

1. **[API_DOKUMENTACIJA.md](docs/API_DOKUMENTACIJA.md)** - Kompletan API pregled sa primjerima
2. **[QUICK_REFERENCE.md](docs/QUICK_REFERENCE.md)** - Brza referenca sa svim endpointima
3. **[IMPLEMENTACIJSKA_UPUTSTVA.md](docs/IMPLEMENTACIJSKA_UPUTSTVA.md)** - Android tim setup guide
4. **[RENDER_DEPLOY.md](RENDER_DEPLOY.md)** - Deployment na Render.com

## Deployment

### Opcije

- **Development:** `localhost:8080` (PostgreSQL lokalno)
- **Production:** **Render.com** (Free tier available)

### Build za Production

```bash
cd server

# Clean build
mvn clean package -DskipTests -P prod

# Rezultat: target/elektropregled-server-1.0.0.jar
```

### Environment Varijable (Production)

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=elektropregled
DB_USER=postgres
DB_PASSWORD=password

JWT_SECRET=your-secret-key-at-least-256-bits
JWT_EXPIRATION=86400000

SPRING_PROFILES_ACTIVE=prod
PORT=8080
```

## Android Integracija

### Retrofit2 Setup

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://elektropregled-api.onrender.com/api")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(ElektropregledApiService::class.java)
```

### Offline Mode

- Koristi Room Database za lokalnu bazu
- Sync sa serverom kad je dostupan internet
- Sve stavke imaju `lokalni_id` (UUID) za mapiranje

## Workflow

### Tehničar Pregledava Postrojenje

1. **Login** → dobija JWT token
2. **Lista postrojenja** → odabere postrojenje (npr. ZAGREB 1)
3. **Lista polja** → odabere polje (npr. 400 kV polje 1)
4. **Checklist** → vidi uređaje i parametre, popunjava vrijednosti
5. **Sinkronizacija** → sprema na server sa ID mapiranjem

## Konfiguracija

### Development (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/elektropregled
spring.datasource.username=postgres
spring.datasource.password=bazepodataka
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

### Production (application-prod.properties)
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
server.port=${PORT:8080}
```

## Android Aplikacija

Android mobilna aplikacija za digitalizaciju elektropregleda s offline-first pristupom. Aplikacija omogućava tehničarima da obavljaju preglede bez internetske veze, a rezultati se sinkroniziraju s serverom kada je dostupan internet.

### 📱 Kako Pokrenuti

**Android aplikacija se nalazi u `android-app/` folderu.**

Za detaljne upute, vidi **[android-app/README.md](android-app/README.md)**.

**Brzi start:**
```bash
git clone https://github.com/your-username/PROJEKT-R.git
cd PROJEKT-R/android-app
# Otvori android-app folder u Android Studio
```

### 🏗️ Tehnologije

- **Kotlin** - glavni programski jezik
- **Android SDK** - minSdk 31 (Android 12+), targetSdk 34
- **Room Database** - lokalna SQLite baza podataka
- **Retrofit** - REST API komunikacija
- **MVVM arhitektura** - ViewModel + Repository pattern
- **WorkManager** - pozadinska sinkronizacija
- **EncryptedSharedPreferences** - sigurno spremanje JWT tokena

### 📂 Struktura Projekta

```
PROJEKT-R/
├── android-app/          # 📱 Android aplikacija
│   ├── app/              # Android app modul
│   ├── build.gradle      # Gradle build config
│   ├── settings.gradle   # Project settings
│   └── README.md         # Detaljne upute za Android
├── server/               # 🖥️ Spring Boot backend
├── baza/                 # 💾 Database scripts
└── docs/                 # 📚 Dokumentacija
```

### Offline Mode

Aplikacija je dizajnirana za **offline-first** rad:

1. **Kreiranje pregleda offline:**
   - Korisnik može kreirati i popuniti preglede bez internetske veze
   - Svi podaci se spremaju lokalno u Room bazu podataka
   - Svaki pregled i stavka imaju jedinstveni `lokalni_id` (UUID)

2. **Sinkronizacija:**
   - Kada je dostupan internet, korisnik može sinkronizirati preglede
   - Aplikacija automatski pokušava sinkronizaciju u pozadini (WorkManager)
   - Status sinkronizacije: PENDING, SYNCING, SYNCED, FAILED

3. **Persistencija:**
   - Svi podaci se čuvaju lokalno čak i nakon zatvaranja aplikacije
   - Aplikacija može raditi potpuno offline

### Kako Testirati Sinkronizaciju

1. **Offline test:**
   - Uključi Airplane Mode na uređaju
   - Prijavi se u aplikaciju (ako već nisi)
   - Kreiraj novi pregled i popuni checklist
   - Završi pregled - podaci su spremljeni lokalno
   - Restartaj aplikaciju - pregled je još uvijek tu

2. **Online test:**
   - Isključi Airplane Mode
   - Otvori Sync ekran (dodaj u navigaciju ako nije)
   - Klikni "Sinkroniziraj"
   - Provjeri da je pregled uspješno poslan na server

3. **Pozadinska sinkronizacija:**
   - Aplikacija automatski pokušava sinkronizaciju svakih 15 minuta kada je dostupan internet
   - WorkManager upravlja pozadinskom sinkronizacijom

### 📁 Detaljna Struktura Android Aplikacije

```
android-app/app/src/main/java/com/example/elektropregled/
├── data/
│   ├── api/              # Retrofit API servisi i DTOs
│   ├── database/         # Room entities, DAOs, AppDatabase
│   ├── repository/       # Repository pattern (local + remote)
│   ├── sync/             # WorkManager sync worker
│   └── TokenStorage.kt   # EncryptedSharedPreferences za JWT
├── ui/
│   ├── screen/           # Fragments (Login, FacilityList, etc.)
│   ├── viewmodel/        # ViewModels za sve ekrane
│   └── MainActivity.kt   # Glavna aktivnost
└── ElektropregledApplication.kt  # Application class s dependency injection
```

### Baza Podataka

Aplikacija koristi Room Database s shemom iz `baza/scripts/mobilna_sqlite.sql`:

- **Korisnik** - lokalni korisnici (opcionalno)
- **Postrojenje** - postrojenja s servera
- **Polje** - polja u postrojenjima
- **Pregled** - pregledi (lokalni + server ID)
- **StavkaPregleda** - stavke pregleda (vrijednosti parametara)

### Workflow

1. **Prijava:**
   - Korisnik se prijavljuje s korisničkim imenom i lozinkom
   - JWT token se sprema sigurno u EncryptedSharedPreferences
   - Token je valjan 24 sata

2. **Pregled postrojenja:**
   - Lista svih postrojenja s brojem pregleda i zadnjim pregledom
   - Overdue postrojenja (starija od 1 mjeseca) su označena crveno

3. **Odabir polja:**
   - Lista polja u odabranom postrojenju
   - Virtualno polje "Direktno na postrojenju" za uređaje bez polja

4. **Checklist i unos podataka:**
   - Lista uređaja s parametrima provjere
   - BOOLEAN parametri su defaultno "OK" (true)
   - NUMERIC parametri imaju min/max validaciju
   - TEXT parametri za napomene
   - Svi podaci se spremaju lokalno odmah

5. **Sinkronizacija:**
   - Manualna sinkronizacija preko Sync ekrana
   - Automatska pozadinska sinkronizacija (WorkManager)
   - Status sinkronizacije se prikazuje korisniku

### Važne Napomene

- **Offline-first:** Aplikacija mora raditi potpuno offline
- **Jedan pregled = jedna osoba:** Jedan pregled postrojenja mora biti završen u cijelosti
- **Default vrijednosti:** BOOLEAN parametri su defaultno "OK" za brži rad u terenu
- **UUID:** Svaki pregled i stavka imaju jedinstveni `lokalni_id` za mapiranje s serverom

### Troubleshooting

**Problem: Aplikacija se ne može prijaviti**
- Provjeri da je backend pokrenut i dostupan
- Provjeri backend URL u `ApiClient.kt`
- Provjeri korisničko ime i lozinku

**Problem: Sinkronizacija ne radi**
- Provjeri internetsku vezu
- Provjeri da je JWT token valjan (prijavi se ponovno)
- Provjeri logove u Logcat za detalje greške

**Problem: Podaci se ne spremaju offline**
- Provjeri da Room baza radi ispravno
- Provjeri logove za SQL greške
- Provjeri da su foreign key constraints omogućeni

## Kontakt

Za pitanja ili probleme s API-jem:
- Provjeri [API_DOKUMENTACIJA.md](docs/API_DOKUMENTACIJA.md)
- Ili [QUICK_REFERENCE.md](docs/QUICK_REFERENCE.md)

## Licencija

[LICENSE](LICENSE)

---