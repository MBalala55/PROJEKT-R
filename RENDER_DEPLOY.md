# Render Deploy - Uputstva za Deployment

## Korak 1: Kreiraj PostgreSQL bazu na Render

1. Otvori https://render.com
2. Login sa GitHub accountom
3. Klikni **"New"** → **"PostgreSQL"**
4. Popuni:
   - **Name:** `elektropregled-db`
   - **Region:** Frankfurt (Europe)
   - **PostgreSQL Version:** 15 (ili najnovija)
   - **Datadog API Key:** (ostavi prazno)
5. Klikni **"Create Database"**
6. **Čekaj 2-3 minute** da se kreira

---

## Korak 2: Spremi Database Credentials

Iz Render dashboard-a spremi:
```
DB_HOST=    (npr. dpg-xxxxx.postgres.render.com)
DB_PORT=    5432
DB_NAME=    (npr. elektropregled_xxx)
DB_USER=    (npr. elektropregled_user)
DB_PASSWORD=(dugi password)
```

---

## Korak 3: Kreiraj Web Service na Render

1. Klikni **"New"** → **"Web Service"**
2. Poveži GitHub:
   - Klikni **"Connect account"**
   - Odaberi repository
   - Autoriziraj
3. Popuni:
   - **Name:** `elektropregled-api`
   - **Environment:** `Docker` (ili `Node` - Render će detektirati Maven)
   - **Region:** Frankfurt
   - **Branch:** `main`
   - **Build Command:** `mvn clean package -DskipTests` (ili `./mvnw clean package -DskipTests`)
   - **Start Command:** Render će detektirati iz `Procfile`

4. **Advanced Settings:**
   - **Auto-Deploy:** ON (automatski deploy na push)
   - **Health Check Path:** `/api/swagger-ui.html` (opciono)

5. Klikni **"Create Web Service"**

---

## Korak 4: Postavi Environment Varijable

Na Render Web Service dashboard:

1. Idi na **"Environment"** tab
2. Dodaj varijable:

```
DB_HOST=dpg-xxxxx.postgres.render.com
DB_PORT=5432
DB_NAME=elektropregled_xxx
DB_USER=elektropregled_user
DB_PASSWORD=<dugi password iz baze>
JWT_SECRET=your-super-secret-jwt-key-at-least-256-bits-long-put-something-strong-here
JWT_EXPIRATION=86400000
SPRING_PROFILES_ACTIVE=prod
PORT=8080
```

3. Klikni **"Save"**

---

## Korak 5: Čekaj Build i Deploy

Render će automatski:
1. Klonirati GitHub repository
2. Buildati Maven projekt
3. Pokrenuti aplikaciju
4. Dodijeliti URL (nešto kao `https://elektropregled-api.onrender.com`)

**Čekaj 5-10 minuta za prvi build!**

---

## Korak 6: Testiraj API

Кadа je deploy gotov:

```http
GET https://elektropregled-api.onrender.com/api/swagger-ui.html
```

Login (testiraj sa test-api.http):
```http
POST https://elektropregled-api.onrender.com/api/v1/auth/login
Content-Type: application/json

{
  "korisnicko_ime": "mmarkovic",
  "lozinka": "pass123"
}
```

---

## Važne Napomene

1. **Prva baza je prazna** - trebam popuniti sa data:
   - Koristi SQL skripte iz `baza/scripts/`
   - U Render PostgreSQL UI, ili
   - Preko DBeaver-a spojenog na Render bazu

2. **Free tier limitacije:**
   - Web Service se suspendira nakon 15 minuta neaktivnosti
   - Database je mali (500 MB)
   - Dovoljan za MVP/testiranje

3. **Production JWT Secret:**
   - Generiraj sa: `openssl rand -hex 32`
   - Spremi kao `JWT_SECRET` environment varijabla

4. **Logs:**
   - Vidi na Render dashboard-u → "Logs"
   - Ako vidiš greške, provjeri environment varijable

5. **Database Migration:**
   ```
   application-prod.properties ima: spring.jpa.hibernate.ddl-auto=update
   ```
   - Može biti rizično u produkciji
   - Za production bolje je `validate` + Flyway migracije

---

## Troubleshooting

### Build Fails - "Maven wrapper not found"
```
Build Command trebaj biti:
./mvnw clean package -DskipTests
```

### Connection Refused - Baza nije dostupna
- Provjeri da je PostgreSQL baza kreirana na Render
- Provjeri DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
- Testiraj connection sa DBeaver-om

### Logs pokazuju "Properties not loaded"
- Provjeri da `SPRING_PROFILES_ACTIVE=prod` je postavljen
- Ili `application-prod.properties` nije pronađena

### API vraća 401 za sve zahtjeve
- JWT_SECRET možda nije postavljen
- Trebam biti **isti** kao na production i ne smije biti prazan

---

## Render URL Primjer

Kada je gotov deploy, URL će biti:
```
https://elektropregled-api.onrender.com/api
```

Na frontendu trebam koristiti:
```kotlin
const val BASE_URL = "https://elektropregled-api.onrender.com/api"
```