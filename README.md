# Ogledna mobilna aplikacija za redoviti mjesečni pregled elemenata elektroenergetskih postrojenja

## Brzi start

### Development (Lokalno)

```bash
# 1. Kloniraj repo
git clone https://github.com/your-username/R.git
cd R

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

## Kontakt

Za pitanja ili probleme s API-jem:
- Provjeri [API_DOKUMENTACIJA.md](docs/API_DOKUMENTACIJA.md)
- Ili [QUICK_REFERENCE.md](docs/QUICK_REFERENCE.md)

## Licencija

[LICENSE](LICENSE)

---