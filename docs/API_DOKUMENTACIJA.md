# Elektropregled API - Dokumentacija

## Pregled

Backend API za mobilnu aplikaciju za digitalizaciju elektropregleda. API koristi JWT tokene za autentifikaciju i vraća JSON odgovore.

- **Base URL:** `https://elektropregled-api.onrender.com/api` (production)
- **Verzija:** 1.0.0
- **Autentifikacija:** JWT Bearer token
- **Swagger UI:** https://elektropregled-api.onrender.com/api/swagger-ui.html

---

## Autentifikacija

### 1. Login - Dobivanje JWT Tokena

**Endpoint:** `POST /v1/auth/login`

**Opis:** Prijavljivanje korisnika i dobivanje JWT tokena. Token se koristi za sve zaštićene endpointe.

**Request:**
```http
POST https://elektropregled-api.onrender.com/api/v1/auth/login
Content-Type: application/json

{
  "korisnicko_ime": "mmarkovic",
  "lozinka": "pass123"
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtbWFya292aWMiLCJpYXQiOjE3Njk0NDE5MjUsImV4cCI6MTc2OTUyODMyNX0.abc123...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "username": "mmarkovic"
}
```

**Polja odgovora:**
- `access_token`: JWT token za sve sljedeće zahtjeve (valjan 24 sata)
- `token_type`: Uvijek "Bearer"
- `expires_in`: Vijek trajanja tokena u sekundama (86400 = 24h)
- `username`: Korisničko ime prijavljivanja

**Greške:**
- **400 Bad Request** - Nedostaju parametri ili nisu popunjeni
- **401 Unauthorized** - Krivo korisničko ime ili lozinka

---

## Postrojenja - Lista i Filtriranje

### 2. Lista svih postrojenja

**Endpoint:** `GET /v1/postrojenja`

**Opis:** Vraća popis svih postrojenja s brojem pregleda i datumom zadnjeg pregleda.

**Request:**
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja
Authorization: Bearer YOUR_TOKEN
```

**Response (200 OK):**
```json
[
  {
    "idPostr": 1001,
    "nazPostr": "ZAGREB 1",
    "lokacija": "Zagreb",
    "oznVrPostr": "TS",
    "totalPregleda": 5,
    "zadnjiPregled": "2026-01-20T14:30:00",
    "zadnjiKorisnik": "mmarkovic"
  },
  {
    "idPostr": 1002,
    "nazPostr": "SPLIT 1",
    "lokacija": "Split",
    "oznVrPostr": "TS",
    "totalPregleda": 3,
    "zadnjiPregled": "2026-01-18T10:15:00",
    "zadnjiKorisnik": "pkralj"
  }
]
```

**Polja odgovora:**
- `idPostr`: Jedinstveni ID postrojenja
- `nazPostr`: Naziv postrojenja
- `lokacija`: Lokacija postrojenja
- `oznVrPostr`: Oznaka vrste postrojenja (TS = Transformatorska stanica)
- `totalPregleda`: Ukupan broj obavljenih pregleda
- `zadnjiPregled`: Datum/vrijeme zadnjeg pregleda (null ako nema pregleda)
- `zadnjiKorisnik`: Korisničko ime korisnika koji je obavio zadnji pregled (null ako nema pregleda)

**Greške:**
- **401 Unauthorized** - Neispravan ili nedostaje JWT token

---

### 3. Lista polja u postrojenju

**Endpoint:** `GET /v1/postrojenja/{id}/polja`

**Opis:** Vraća sve elektroenergetske vode (polja) u postrojenju s brojem uređaja u svakom polju. Ako postoje uređaji bez polja, prikazuje se virtualno polje "Direktno na postrojenju".

**Request:**
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/polja
Authorization: Bearer YOUR_TOKEN
```

**Response (200 OK):**
```json
[
  {
    "idPolje": 50001,
    "nazPolje": "400 kV polje 1",
    "napRazina": 400.0,
    "oznVrPolje": "VN",
    "brojUredaja": 12
  },
  {
    "idPolje": 50002,
    "nazPolje": "110 kV polje A",
    "napRazina": 110.0,
    "oznVrPolje": "VN",
    "brojUredaja": 8
  },
  {
    "idPolje": null,
    "nazPolje": "Direktno na postrojenju",
    "napRazina": null,
    "oznVrPolje": null,
    "brojUredaja": 3
  }
]
```

**Polja odgovora:**
- `idPolje`: ID polja (null za uređaje direktno na postrojenju)
- `nazPolje`: Naziv polja/vode
- `napRazina`: Naponska razina u kV (null ako nije primjenjivo)
- `brojUredaja`: Broj uređaja u tom polju

**URL Parametri:**
- `{id}`: ID postrojenja (npr. 1001)

**Greške:**
- **401 Unauthorized** - Neispravan ili nedostaje JWT token
- **404 Not Found** - Postrojenje nije pronađeno

---

## Čitanja/Pregledi - Checklist

### 4. Checklist za polje - Uređaji i Parametri Provjere

**Endpoint:** `GET /v1/postrojenja/{id}/checklist?id_polje={polje_id}`

**Opis:** Vraća kompletnu listu uređaja u odabranom polju sa svim parametrima provjere. Svaki parametar ima pre-popunjene default vrijednosti.

**Request - Za polje:**
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/checklist?id_polje=50001
Authorization: Bearer YOUR_TOKEN
```

**Request - Za uređaje bez polja:**
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/checklist?id_polje=0
Authorization: Bearer YOUR_TOKEN
```

**Response (200 OK):**
```json
[
  {
    "idUred": 1,
    "natpPlocica": "TR-001",
    "tvBroj": "123456789",
    "oznVrUred": "PR",
    "nazVrUred": "Prekidač",
    "idPolje": 50001,
    "nazPolje": "400 kV polje 1",
    "napRazina": 400.0,
    "parametri": [
      {
        "idParametra": 1,
        "nazParametra": "Vanjsko stanje",
        "tipPodataka": "BOOLEAN",
        "minVrijednost": null,
        "maxVrijednost": null,
        "mjernaJedinica": null,
        "obavezan": true,
        "redoslijed": 1,
        "defaultVrijednostBool": true,
        "defaultVrijednostNum": null,
        "defaultVrijednostTxt": null,
        "zadnjaProveraDatum": "2026-01-20T10:30:00",
        "opis": "Provjera čistoće i stanja porculanskih izolatora"
      },
      {
        "idParametra": 6,
        "nazParametra": "Tlak plina SF6",
        "tipPodataka": "NUMERIC",
        "minVrijednost": 0.1,
        "maxVrijednost": 30.0,
        "mjernaJedinica": "bara",
        "obavezan": true,
        "redoslijed": 6,
        "defaultVrijednostBool": null,
        "defaultVrijednostNum": 15.5,
        "defaultVrijednostTxt": null,
        "zadnjaProveraDatum": "2026-01-20T10:30:00",
        "opis": "Očitanje tlaka SF6 plina"
      }
    ]
  }
]
```

**Polja odgovora - Uređaj:**
- `idUred`: Jedinstveni ID uređaja
- `natpPlocica`: Natpisna pločica
- `tvBroj`: Tvorički broj
- `nazVrUred`: Naziv vrste uređaja
- `parametri`: Niz parametara za provjeru

**Polja odgovora - Parametar:**
- `idParametra`: ID parametra
- `nazParametra`: Naziv što se provjerava
- `tipPodataka`: BOOLEAN, NUMERIC, ili TEXT
- `minVrijednost` / `maxVrijednost`: Dozvoljeni raspon (samo za NUMERIC)
- `mjernaJedinica`: Mjerna jedinica (samo za NUMERIC)
- `obavezan`: Mora li se popuniti
- `defaultVrijednostBool/Num/Txt`: Pre-popunjene vrijednosti
- `zadnjaProveraDatum`: Datum zadnjeg pregleda

**URL Parametri:**
- `{id}`: ID postrojenja
- `id_polje`: ID polja (obavezno!) - koristi 0 za uređaje bez polja

**Greške:**
- **400 Bad Request** - Nedostaje parametar `id_polje`
- **401 Unauthorized** - Neispravan token
- **404 Not Found** - Postrojenje nije pronađeno

---

## Sinkronizacija - Slanje Pregleda na Server

### 5. Sinkronizacija Pregleda (Sync)

**Endpoint:** `POST /v1/pregled/sync`

**Opis:** Sprema pregled i sve stavke (vrijednosti parametara) na server. Server provjerava sve validacije i vraća ID-eve.

**Request:**
```http
POST https://elektropregled-api.onrender.com/api/v1/pregled/sync
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN

{
  "pregled": {
    "lokalni_id": "550e8400-e29b-41d4-a716-446655440000",
    "pocetak": "2026-01-26T10:30:00",
    "kraj": "2026-01-26T10:45:00",
    "id_korisnika": 1,
    "id_postr": 1001,
    "napomena": "Sve je ispravno"
  },
  "stavke": [
    {
      "lokalni_id": "550e8400-e29b-41d4-a716-446655440001",
      "id_ured": 1,
      "id_parametra": 1,
      "vrijednost_bool": true,
      "vrijednost_num": null,
      "vrijednost_txt": null,
      "napomena": "Nema oštećenja"
    },
    {
      "lokalni_id": "550e8400-e29b-41d4-a716-446655440002",
      "id_ured": 1,
      "id_parametra": 6,
      "vrijednost_bool": null,
      "vrijednost_num": 15.5,
      "vrijednost_txt": null,
      "napomena": "Normalan tlak"
    }
  ]
}
```

**Polja Request - Pregled:**
- `lokalni_id` (obavezno): UUID koji generira app
- `pocetak` (obavezno): ISO 8601 format
- `kraj`: Vrijeme završetka (opcionalno)
- `id_korisnika` (obavezno): ID korisnika
- `id_postr` (obavezno): ID postrojenja
- `napomena`: Bilješke tehničara (opcionalno)

**Polja Request - Stavka:**
- `lokalni_id` (obavezno): UUID stavke
- `id_ured` (obavezno): ID uređaja
- `id_parametra` (obavezno): ID parametra
- **Točno jedno od sljedećeg:**
  - `vrijednost_bool`: Za BOOLEAN tip
  - `vrijednost_num`: Za NUMERIC tip
  - `vrijednost_txt`: Za TEXT tip
- `napomena`: Napomena tehničara (opcionalno)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Pregled je uspješno sinkroniziran",
  "serverPregledId": 42,
  "idMappings": {
    "pregled": {
      "lokalniId": "550e8400-e29b-41d4-a716-446655440000",
      "serverId": 42
    },
    "stavke": [
      {
        "lokalniId": "550e8400-e29b-41d4-a716-446655440001",
        "serverId": 101
      }
    ]
  },
  "timestamp": "2026-01-26T14:35:00Z"
}
```

**Greške:**

**400 Bad Request** - Validacijska greška:
```json
{
  "success": false,
  "message": "Vrijednost je veća od maksimalne dozvoljene (max: 30.0, primljeno: 35.5)",
  "timestamp": "2026-01-26T14:35:00Z"
}
```

**404 Not Found** - Resurs ne postoji:
```json
{
  "success": false,
  "message": "Korisnik nije pronađen",
  "timestamp": "2026-01-26T14:35:00Z"
}
```

**409 Conflict** - Duplikat:
```json
{
  "success": false,
  "message": "Pregled s ovim lokalnim ID-om je već sinkroniziran",
  "timestamp": "2026-01-26T14:35:00Z"
}
```

---

## Tipični Workflow

1. **Prijava:**
   ```http
   POST https://elektropregled-api.onrender.com/api/v1/auth/login
   ```

2. **Pregled postrojenja:**
   ```http
   GET https://elektropregled-api.onrender.com/api/v1/postrojenja
   ```

3. **Odabir polja:**
   ```http
   GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/polja
   ```

4. **Učitavanje checklistte:**
   ```http
   GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/checklist?id_polje=50001
   ```

5. **Popunjavanje vrijednosti (offline)**

6. **Sinkronizacija:**
   ```http
   POST https://elektropregled-api.onrender.com/api/v1/pregled/sync
   ```

---

## Važna Pravila

1. **Token Valjanost:** 24 sata (86400 sekundi)
2. **id_polje:** OBAVEZNO u checklist zahtjevu
3. **UUID:** Svaki `lokalni_id` mora biti jedinstven
4. **Vrijednosti:** Samo JEDNA od tri vrijednosti po stavki
5. **Datum Format:** ISO 8601 - `yyyy-MM-dd'T'HH:mm:ss`
6. **Offline Mode:** Aplikacija radi offline, sinkronizira se na internetu

---

## Status Kodovi

| Kod | Značenje |
|-----|----------|
| 200 | OK - Zahtjev je uspješan |
| 400 | Bad Request - Greška u zahtjevu |
| 401 | Unauthorized - Neispravan token |
| 404 | Not Found - Resurs ne postoji |
| 409 | Conflict - Sukobi (duplikat) |
| 500 | Internal Server Error - Greška na serveru |

---

## Testiraj API-je

### Swagger UI
```
https://elektropregled-api.onrender.com/api/swagger-ui.html
```

### VS Code REST Client
Otvori `test-api.http` iz projekta i klikni "Send Request"

### Android Tim
Koristi Retrofit2 za integraciju s API-jem

---

## Kontakt

Za pitanja ili probleme s API-jem, kontaktiraj backend tim.
