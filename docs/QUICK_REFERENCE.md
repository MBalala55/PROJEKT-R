# Elektropregled API - Quick Reference

## Base URL
```
https://elektropregled-api.onrender.com/api
```

## Autentifikacija

Svi zahtjevi trebaju JWT token u headeru:
```
Authorization: Bearer {access_token}
```

---

## Endpointi - Brza Referenca

### 1. LOGIN
```
POST /v1/auth/login
Content-Type: application/json

{
  "korisnicko_ime": "mmarkovic",
  "lozinka": "pass123"
}

Response: 200 OK
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "username": "mmarkovic"
}
```

**Greške:**
- 400 Bad Request - Nedostaju polja
- 401 Unauthorized - Krivo korisničko ime ili lozinka

---

### 2. LISTA POSTROJENJA
```
GET /v1/postrojenja
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "idPostr": 1001,
    "nazPostr": "ZAGREB 1",
    "lokacija": "Zagreb",
    "oznVrPostr": "TS",
    "totalPregleda": 5,
    "zadnjiPregled": "2026-01-20T14:30:00"
  },
  ...
]
```

**Greške:**
- 401 Unauthorized - Neispravan token

---

### 3. LISTA POLJA (u postrojenju)
```
GET /v1/postrojenja/{id}/polja
Authorization: Bearer {token}

Parametri:
- {id}: ID postrojenja (npr. 1001)

Response: 200 OK
[
  {
    "idPolje": 50001,
    "nazPolje": "400 kV polje 1",
    "napRazina": 400.0,
    "oznVrPolje": "VN",
    "brojUredaja": 12
  },
  {
    "idPolje": null,
    "nazPolje": "Direktno na postrojenju",
    "napRazina": null,
    "oznVrPolje": null,
    "brojUredaja": 3
  },
  ...
]
```

**Greške:**
- 401 Unauthorized - Neispravan token
- 404 Not Found - Postrojenje ne postoji

---

### 4. CHECKLIST (uređaji i parametri)
```
GET /v1/postrojenja/{id}/checklist?id_polje={polje_id}
Authorization: Bearer {token}

Parametri:
- {id}: ID postrojenja (npr. 1001)
- id_polje: ID polja (OBAVEZNO!) - koristi 0 za uređaje bez polja

Response: 200 OK
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
        "opis": "Provjera čistoće..."
      },
      ...
    ]
  },
  ...
]
```

**Greške:**
- 400 Bad Request - Nedostaje parametar id_polje
- 401 Unauthorized - Neispravan token
- 404 Not Found - Postrojenje ne postoji

---

### 5. SINKRONIZACIJA (slanje pregleda)
```
POST /v1/pregled/sync
Content-Type: application/json
Authorization: Bearer {token}

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
      "napomena": "OK",
      "vrijeme_unosa": "2026-01-26T10:31:00"
    },
    {
      "lokalni_id": "550e8400-e29b-41d4-a716-446655440002",
      "id_ured": 1,
      "id_parametra": 6,
      "vrijednost_bool": null,
      "vrijednost_num": 15.5,
      "vrijednost_txt": null,
      "napomena": null,
      "vrijeme_unosa": "2026-01-26T10:32:00"
    }
  ]
}

Response: 200 OK
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
      },
      {
        "lokalniId": "550e8400-e29b-41d4-a716-446655440002",
        "serverId": 102
      }
    ]
  },
  "timestamp": "2026-01-26T14:35:00Z"
}
```

**Greške:**
- 400 Bad Request - Validacijska greška
  ```json
  {
    "success": false,
    "message": "Vrijednost je veća od maksimalne dozvoljene (max: 30.0, primljeno: 35.5)",
    "timestamp": "2026-01-26T14:35:00Z"
  }
  ```
- 404 Not Found - Resurs ne postoji
  ```json
  {
    "success": false,
    "message": "Korisnik nije pronađen",
    "timestamp": "2026-01-26T14:35:00Z"
  }
  ```
- 409 Conflict - Duplikat
  ```json
  {
    "success": false,
    "message": "Pregled s ovim lokalnim ID-om je već sinkroniziran",
    "timestamp": "2026-01-26T14:35:00Z"
  }
  ```
- 401 Unauthorized - Neispravan token

---

## HTTP Primjeri (REST Client Format)

### Login
```http
POST https://elektropregled-api.onrender.com/api/v1/auth/login
Content-Type: application/json

{
  "korisnicko_ime": "mmarkovic",
  "lozinka": "pass123"
}
```

### Postrojenja
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja
Authorization: Bearer YOUR_TOKEN
```

### Polja
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/polja
Authorization: Bearer YOUR_TOKEN
```

### Checklist
```http
GET https://elektropregled-api.onrender.com/api/v1/postrojenja/1001/checklist?id_polje=50001
Authorization: Bearer YOUR_TOKEN
```

### Sync
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
    "napomena": "Sve OK"
  },
  "stavke": [
    {
      "lokalni_id": "550e8400-e29b-41d4-a716-446655440001",
      "id_ured": 1,
      "id_parametra": 1,
      "vrijednost_bool": true,
      "napomena": "OK"
    }
  ]
}
```

---

## Tipovi Podataka - Referenca

### BOOLEAN
- Koristi za: DA/NE, OK/LOŠE, ISPRAVLJENO/NIJE
- Vrijednost: `true` ili `false`
- Sve vrijednosti postavljaju se na `true` kao default
- Primjer:
  ```json
  {
    "idParametra": 1,
    "tipPodataka": "BOOLEAN",
    "vrijednost_bool": true
  }
  ```

### NUMERIC
- Koristi za: Mjere, tlakove, temperature, itd.
- Vrijednost: Broj s decimalama
- Mora biti između `minVrijednost` i `maxVrijednost`
- Primjer:
  ```json
  {
    "idParametra": 6,
    "tipPodataka": "NUMERIC",
    "minVrijednost": 0.1,
    "maxVrijednost": 30.0,
    "mjernaJedinica": "bara",
    "vrijednost_num": 15.5
  }
  ```

### TEXT
- Koristi za: Bilješke, oznake, napomene
- Vrijednost: Tekst
- Primjer:
  ```json
  {
    "idParametra": 20,
    "tipPodataka": "TEXT",
    "vrijednost_txt": "Primjedba: Nedostaje vijak"
  }
  ```

---

## Važna Pravila

1. **Token Valjanost:** Token je valjan 24 sata (86400 sekundi) - nakon toga moraš se ponovno prijaviti

2. **id_polje u Checklist:** OBAVEZNO trebaš poslati `id_polje` parametar
   - Za polje s ID-om, npr. `id_polje=50001`
   - Za uređaje bez polja, `id_polje=0`

3. **UUID Generiranje:** Svaki `lokalni_id` mora biti jedinstven (koristi UUID.randomUUID())

4. **Vrijednosti Stavki:** Samo JEDNA od tri vrijednosti treba biti popunjena:
   - `vrijednost_bool` - SAMO za BOOLEAN tip
   - `vrijednost_num` - SAMO za NUMERIC tip
   - `vrijednost_txt` - SAMO za TEXT tip
   - Ostale trebaju biti `null`

5. **Datum/Vrijeme Format:** ISO 8601 format - `yyyy-MM-dd'T'HH:mm:ss`
   - Primjer: `2026-01-26T10:30:00`

6. **Offline Mode:** App radi offline - spremi preglede lokalno i sinkronizira kada je dostupan internet

7. **Validacija:** Server provjerava:
   - Sve obavezne parametre popunjene
   - Rangove vrijednosti (NUMERIC)
   - Tipove podataka
   - Jedinstvene lokalne ID-eve (nema duplikata)

---

## Status Kodovi

| Kod | Značenje |
|-----|----------|
| 200 | OK - zahtjev je obrađen uspješno |
| 400 | Bad Request - zahtjev ima grešku (npr. nedostaju polja) |
| 401 | Unauthorized - token nije valjan ili nedostaje |
| 404 | Not Found - traženi resurs ne postoji |
| 409 | Conflict - sukobi (npr. duplikat lokalni_id) |
| 500 | Internal Server Error - greška na serveru |

---

## Testiraj API-je

### Swagger UI
Otvori preglednik i idi na:
```
https://elektropregled-api.onrender.com/api/swagger-ui.html
```
Tu možeš direktno testirati sve endpointe preko web interfacea.

### Postman/Insomnia
Importaj test-api.http iz projekta u Postman ili Insomnia za brzo testiranje.

### VS Code REST Client
Ako koristiš VS Code s REST Client extenzijom, otvori `test-api.http` datoteku iz projekta i klikni na "Send Request" iznad svakog zahtjeva.

---

##Greške i Rješenja

### 401 Unauthorized
- **Razlog:** Token nije valjan, istekao je, ili nije poslana
- **Rješenje:** Ponovno se prijavi s `/login` i koristi novi token

### 400 Bad Request - Vrijednost je veća od maksimalne
- **Razlog:** Unio si broj veći od dozvoljenog (npr. tlak > 30 bara)
- **Rješenje:** Provjeri `minVrijednost` i `maxVrijednost` i unesi vrijednost u rasponu

### 404 Not Found - Postrojenje nije pronađeno
- **Razlog:** ID postrojenja je kriv
- **Rješenje:** Prvo pozovi `GET /postrojenja` da vidiš sve dostupne ID-eve

### 409 Conflict - Pregled s ovim lokalnim ID-om je već sinkroniziran
- **Razlog:** Pokušao si sinkronizirati isti pregled dva puta
- **Rješenje:** Koristiti novi UUID za svaki pregled, ili spremi mapiranje server ID-eva

---
