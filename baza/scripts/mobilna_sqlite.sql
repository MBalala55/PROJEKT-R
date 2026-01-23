-- SQLite baza za mobilnu Android aplikaciju
-- za Room Database

-- VAŽNO: Prije izvršavanja upita, omogući foreign key constraints
-- PRAGMA foreign_keys = ON;

CREATE TABLE Korisnik
(
  id_korisnika INTEGER PRIMARY KEY AUTOINCREMENT,
  ime TEXT NOT NULL,
  prezime TEXT NOT NULL,
  korisnicko_ime TEXT NOT NULL UNIQUE,
  lozinka TEXT NOT NULL,
  uloga TEXT NOT NULL CHECK (uloga IN ('ADMIN', 'RADNIK'))
);

CREATE TABLE Postrojenje
(
  id_postr INTEGER NOT NULL PRIMARY KEY,
  ozn_vr_postr TEXT NOT NULL,
  naz_postr TEXT NOT NULL UNIQUE,
  lokacija TEXT
);

CREATE TABLE Polje
(
  id_polje INTEGER NOT NULL PRIMARY KEY,
  nap_razina REAL NOT NULL,
  ozn_vr_polje TEXT NOT NULL,
  naz_polje TEXT NOT NULL,
  id_postr INTEGER NOT NULL,
  FOREIGN KEY (id_postr) REFERENCES Postrojenje(id_postr),
  CHECK (nap_razina IN (400.0, 220.0, 120.0, 110.0, 35.0, 30.0, 25.5, 20.0, 10.0))
);

CREATE TABLE VrstaUredaja
(
  id_vr_ured INTEGER PRIMARY KEY AUTOINCREMENT,
  ozn_vr_ured TEXT NOT NULL UNIQUE,
  naz_vr_ured TEXT NOT NULL UNIQUE
);

CREATE TABLE Uredaj
(
  id_ured INTEGER NOT NULL PRIMARY KEY,
  natp_plocica TEXT NOT NULL,
  tv_broj TEXT NOT NULL UNIQUE,
  id_postr INTEGER NOT NULL,
  id_polje INTEGER,
  id_vr_ured INTEGER NOT NULL,
  FOREIGN KEY (id_postr) REFERENCES Postrojenje(id_postr),
  FOREIGN KEY (id_polje) REFERENCES Polje(id_polje),
  FOREIGN KEY (id_vr_ured) REFERENCES VrstaUredaja(id_vr_ured)
);

CREATE TABLE Pregled
(
  id_preg INTEGER PRIMARY KEY AUTOINCREMENT,
  lokalni_id TEXT NOT NULL UNIQUE,  -- UUID generiran offline
  server_id INTEGER,  -- ID s servera nakon sinkronizacije (NULL dok nije synced)
  status_sinkronizacije TEXT NOT NULL DEFAULT 'PENDING',
  pocetak TEXT NOT NULL,  -- ISO 8601: '2024-01-15T10:30:00'
  kraj TEXT,  -- NULL dok pregled traje
  napomena TEXT,
  sync_error TEXT,
  id_korisnika INTEGER NOT NULL,
  id_postr INTEGER NOT NULL,
  FOREIGN KEY (id_korisnika) REFERENCES Korisnik(id_korisnika),
  FOREIGN KEY (id_postr) REFERENCES Postrojenje(id_postr),
  CHECK (status_sinkronizacije IN ('PENDING', 'SYNCING', 'SYNCED', 'FAILED')),
  CHECK (kraj IS NULL OR kraj >= pocetak)
);

CREATE TABLE ParametarProvjere
(
  id_parametra INTEGER PRIMARY KEY AUTOINCREMENT,
  naz_parametra TEXT NOT NULL,
  tip_podataka TEXT NOT NULL,
  min_vrijednost REAL,
  max_vrijednost REAL,
  mjerna_jedinica TEXT,
  obavezan INTEGER NOT NULL DEFAULT 1,  -- 0 = FALSE, 1 = TRUE
  redoslijed INTEGER NOT NULL,
  opis TEXT,
  id_vr_ured INTEGER NOT NULL,
  FOREIGN KEY (id_vr_ured) REFERENCES VrstaUredaja(id_vr_ured),
  CHECK (tip_podataka IN ('BOOLEAN', 'NUMERIC', 'TEXT')),
  CHECK (min_vrijednost IS NULL OR max_vrijednost IS NULL OR min_vrijednost <= max_vrijednost),
  CHECK (
    (tip_podataka = 'NUMERIC' AND mjerna_jedinica IS NOT NULL) OR
    (tip_podataka IN ('BOOLEAN', 'TEXT'))
  )
);

CREATE TABLE StavkaPregleda
(
  id_stavke INTEGER PRIMARY KEY AUTOINCREMENT,
  lokalni_id TEXT NOT NULL UNIQUE,  -- UUID spremljen kao TEXT
  server_id INTEGER,  -- ID s servera nakon sinkronizacije
  vrijednost_bool INTEGER,
  vrijednost_num REAL,
  vrijednost_txt TEXT,
  napomena TEXT,
  vrijeme_unosa TEXT NOT NULL DEFAULT (datetime('now')),  -- ISO 8601 format
  id_preg INTEGER NOT NULL,
  id_ured INTEGER NOT NULL,
  id_parametra INTEGER NOT NULL,
  FOREIGN KEY (id_preg) REFERENCES Pregled(id_preg) ON DELETE CASCADE,
  FOREIGN KEY (id_ured) REFERENCES Uredaj(id_ured),
  FOREIGN KEY (id_parametra) REFERENCES ParametarProvjere(id_parametra),
  UNIQUE (id_preg, id_parametra, id_ured),
  CHECK (
    (vrijednost_bool IS NOT NULL AND vrijednost_num IS NULL AND vrijednost_txt IS NULL) OR
    (vrijednost_bool IS NULL AND vrijednost_num IS NOT NULL AND vrijednost_txt IS NULL) OR
    (vrijednost_bool IS NULL AND vrijednost_num IS NULL AND vrijednost_txt IS NOT NULL) OR
    (vrijednost_bool IS NULL AND vrijednost_num IS NULL AND vrijednost_txt IS NULL)
  )
);

CREATE INDEX idx_pregled_status ON Pregled(status_sinkronizacije);
CREATE INDEX idx_pregled_korisnik ON Pregled(id_korisnika);
CREATE INDEX idx_pregled_postrojenje ON Pregled(id_postr);
CREATE INDEX idx_stavka_pregled ON StavkaPregleda(id_preg);
CREATE INDEX idx_stavka_uredaj ON StavkaPregleda(id_ured);

-- Indeksi za statičke tablice (za JOIN operacije)
CREATE INDEX idx_uredaj_postrojenje ON Uredaj(id_postr);
CREATE INDEX idx_uredaj_polje ON Uredaj(id_polje);
CREATE INDEX idx_uredaj_vrsta ON Uredaj(id_vr_ured);
CREATE INDEX idx_parametar_vrsta ON ParametarProvjere(id_vr_ured);
CREATE INDEX idx_parametar_redoslijed ON ParametarProvjere(id_vr_ured, redoslijed);
CREATE INDEX idx_polje_postrojenje ON Polje(id_postr);