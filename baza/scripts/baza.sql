CREATE TABLE Korisnik
(
  id_korisnika SERIAL NOT NULL,
  ime VARCHAR(50) NOT NULL,
  prezime VARCHAR(50) NOT NULL,
  korisnicko_ime VARCHAR(30) NOT NULL,
  lozinka VARCHAR(255) NOT NULL,
  uloga VARCHAR(20) NOT NULL,
  CONSTRAINT pk_korisnik PRIMARY KEY (id_korisnika),
  CONSTRAINT uq_korisnik_korisnicko_ime UNIQUE (korisnicko_ime),
  CONSTRAINT chk_uloga CHECK (uloga IN ('ADMIN', 'RADNIK'))
);

CREATE TABLE Postrojenje
(
  id_postr INT NOT NULL,
  ozn_vr_postr VARCHAR(10) NOT NULL,
  naz_postr VARCHAR(100) NOT NULL,
  lokacija VARCHAR(150),
  CONSTRAINT pk_postrojenje PRIMARY KEY (id_postr),
  CONSTRAINT uq_postrojenje_naz_postr UNIQUE (naz_postr)
);

CREATE TABLE Polje
(
  id_polje INT NOT NULL,
  nap_razina DECIMAL(5,1) NOT NULL,
  ozn_vr_polje VARCHAR(20) NOT NULL,
  naz_polje VARCHAR(100) NOT NULL,
  id_postr INT NOT NULL,
  CONSTRAINT pk_polje PRIMARY KEY (id_polje),
  CONSTRAINT fk_polje_postrojenje FOREIGN KEY (id_postr) REFERENCES Postrojenje(id_postr),
  CONSTRAINT chk_nap_razina CHECK (nap_razina IN (400.0, 220.0, 120.0, 110.0, 35.0, 30.0, 25.5, 20.0, 10.0))
);

CREATE TABLE VrstaUredaja
(
  id_vr_ured SERIAL NOT NULL,
  ozn_vr_ured VARCHAR(5) NOT NULL,
  naz_vr_ured VARCHAR(50) NOT NULL,
  CONSTRAINT pk_vrsta_uredaja PRIMARY KEY (id_vr_ured),
  CONSTRAINT uq_vrsta_uredaja_ozn_vr_ured UNIQUE (ozn_vr_ured),
  CONSTRAINT uq_vrsta_uredaja_naz_vr_ured UNIQUE (naz_vr_ured)
);

CREATE TABLE Uredaj
(
  id_ured INT NOT NULL,
  natp_plocica VARCHAR(20) NOT NULL,
  tv_broj VARCHAR(30) NOT NULL,
  id_postr INT NOT NULL,
  id_polje INT,
  id_vr_ured INT NOT NULL,
  CONSTRAINT pk_uredaj PRIMARY KEY (id_ured),
  CONSTRAINT uq_uredaj_tv_broj UNIQUE (tv_broj),
  CONSTRAINT fk_uredaj_postrojenje FOREIGN KEY (id_postr) REFERENCES Postrojenje(id_postr),
  CONSTRAINT fk_uredaj_polje FOREIGN KEY (id_polje) REFERENCES Polje(id_polje),
  CONSTRAINT fk_uredaj_vrsta_uredaja FOREIGN KEY (id_vr_ured) REFERENCES VrstaUredaja(id_vr_ured)
);

CREATE TABLE Pregled
(
  id_preg SERIAL NOT NULL,
  lokalni_id UUID NOT NULL,
  status_sync VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  pocetak TIMESTAMP NOT NULL,
  kraj TIMESTAMP,
  napomena VARCHAR(255),
  sync_error TEXT,
  id_korisnika INT NOT NULL,
  id_postr INT NOT NULL,
  CONSTRAINT pk_pregled PRIMARY KEY (id_preg),
  CONSTRAINT uq_pregled_lokalni_id UNIQUE (lokalni_id),
  CONSTRAINT fk_pregled_korisnik FOREIGN KEY (id_korisnika) REFERENCES Korisnik(id_korisnika),
  CONSTRAINT fk_pregled_postrojenje FOREIGN KEY (id_postr) REFERENCES Postrojenje(id_postr),
  CONSTRAINT chk_status_sync CHECK (status_sync IN ('PENDING', 'SYNCING', 'SYNCED', 'FAILED')),
  CONSTRAINT chk_pregled_pocetak CHECK (pocetak <= CURRENT_TIMESTAMP),
  CONSTRAINT chk_pregled_trajanje CHECK (kraj IS NULL OR kraj >= pocetak)
);

CREATE TABLE ParametarProvjere
(
  id_parametra SERIAL NOT NULL,
  naz_parametra VARCHAR(100) NOT NULL,
  tip_podataka VARCHAR(20) NOT NULL,
  min_vrijednost DECIMAL(10,2),
  max_vrijednost DECIMAL(10,2),
  mjerna_jedinica VARCHAR(20),
  obavezan BOOLEAN NOT NULL DEFAULT TRUE,
  redoslijed INT NOT NULL,
  opis TEXT,
  id_vr_ured INT NOT NULL,
  CONSTRAINT pk_parametar_provjere PRIMARY KEY (id_parametra),
  CONSTRAINT fk_parametar_provjere_vrsta_uredaja FOREIGN KEY (id_vr_ured) REFERENCES VrstaUredaja(id_vr_ured),
  CONSTRAINT chk_tip_podataka CHECK (tip_podataka IN ('BOOLEAN', 'NUMERIC', 'TEXT')),
  CONSTRAINT chk_min_max CHECK (min_vrijednost IS NULL OR max_vrijednost IS NULL OR min_vrijednost <= max_vrijednost),
  CONSTRAINT chk_mjerna_jedinica CHECK (
    (tip_podataka = 'NUMERIC' AND mjerna_jedinica IS NOT NULL) OR
    (tip_podataka IN ('BOOLEAN', 'TEXT'))
  )
);

CREATE TABLE StavkaPregleda
(
  id_stavke SERIAL NOT NULL,
  lokalni_id UUID NOT NULL,
  vrijednost_bool BOOLEAN,
  vrijednost_num DECIMAL(10,2),
  vrijednost_txt VARCHAR(255),
  napomena VARCHAR(255),
  vrijeme_unosa TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_preg INT NOT NULL,
  id_ured INT NOT NULL,
  id_parametra INT NOT NULL,
  CONSTRAINT pk_stavka_pregleda PRIMARY KEY (id_stavke),
  CONSTRAINT uq_stavka_pregleda_lokalni_id UNIQUE (lokalni_id),
  CONSTRAINT uq_stavka_unique_check UNIQUE (id_preg, id_parametra, id_ured),
  CONSTRAINT fk_stavka_pregleda_pregled FOREIGN KEY (id_preg) REFERENCES Pregled(id_preg),
  CONSTRAINT fk_stavka_pregleda_uredaj FOREIGN KEY (id_ured) REFERENCES Uredaj(id_ured),
  CONSTRAINT fk_stavka_pregleda_parametar_provjere FOREIGN KEY (id_parametra) REFERENCES ParametarProvjere(id_parametra),
  CONSTRAINT chk_jedna_vrijednost CHECK (
    (vrijednost_bool IS NOT NULL AND vrijednost_num IS NULL AND vrijednost_txt IS NULL) OR
    (vrijednost_bool IS NULL AND vrijednost_num IS NOT NULL AND vrijednost_txt IS NULL) OR
    (vrijednost_bool IS NULL AND vrijednost_num IS NULL AND vrijednost_txt IS NOT NULL) OR
    (vrijednost_bool IS NULL AND vrijednost_num IS NULL AND vrijednost_txt IS NULL)
  )
);

-- Indeksi na Foreign Key kolonama (JOIN operacije)
-- Kritični za READ performanse jer su statički podaci često JOIN-ani
CREATE INDEX idx_polje_postrojenje ON Polje(id_postr);
CREATE INDEX idx_uredaj_postrojenje ON Uredaj(id_postr);
CREATE INDEX idx_uredaj_polje ON Uredaj(id_polje);
CREATE INDEX idx_uredaj_vrsta ON Uredaj(id_vr_ured);
CREATE INDEX idx_pregled_korisnik ON Pregled(id_korisnika);
CREATE INDEX idx_pregled_postrojenje ON Pregled(id_postr);
CREATE INDEX idx_stavka_pregled ON StavkaPregleda(id_preg);
CREATE INDEX idx_stavka_uredaj ON StavkaPregleda(id_ured);
CREATE INDEX idx_stavka_parametar ON StavkaPregleda(id_parametra);
CREATE INDEX idx_parametar_vrsta ON ParametarProvjere(id_vr_ured);

-- za filtriranje i pretraživanje
CREATE INDEX idx_pregled_status ON Pregled(status_sync);
CREATE INDEX idx_pregled_datum ON Pregled(pocetak);
CREATE INDEX idx_stavka_vrijeme ON StavkaPregleda(vrijeme_unosa);

-- za redoslijed u cheklistama
CREATE INDEX idx_parametar_redoslijed ON ParametarProvjere(id_vr_ured, redoslijed);

-- pregledi po statusu i korisniku
CREATE INDEX idx_pregled_status_korisnik ON Pregled(status_sync, id_korisnika);

-- Indeks za preglede po postrojenju i datumu
CREATE INDEX idx_pregled_postrojenje_datum ON Pregled(id_postr, pocetak DESC);

-- Trigger funkcija za validaciju unosa u StavkaPregleda
CREATE OR REPLACE FUNCTION validate_stavka_vrijednost()
RETURNS TRIGGER AS $$
DECLARE
  v_tip_podataka VARCHAR;
  v_min_vrijednost DECIMAL;
  v_max_vrijednost DECIMAL;
BEGIN
  SELECT tip_podataka, min_vrijednost, max_vrijednost
  INTO v_tip_podataka, v_min_vrijednost, v_max_vrijednost
  FROM ParametarProvjere
  WHERE id_parametra = NEW.id_parametra;
  
  IF v_tip_podataka = 'BOOLEAN' AND NEW.vrijednost_bool IS NULL THEN
    NULL;
  ELSIF v_tip_podataka = 'NUMERIC' THEN
    IF NEW.vrijednost_num IS NOT NULL THEN
      IF (v_min_vrijednost IS NOT NULL AND NEW.vrijednost_num < v_min_vrijednost) OR
         (v_max_vrijednost IS NOT NULL AND NEW.vrijednost_num > v_max_vrijednost) THEN
        RAISE NOTICE 'Vrijednost % je izvan očekivanog raspona [%, %]', 
                     NEW.vrijednost_num, v_min_vrijednost, v_max_vrijednost;
      END IF;
    END IF;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_stavka_vrijednost
  BEFORE INSERT OR UPDATE ON StavkaPregleda
  FOR EACH ROW
  EXECUTE FUNCTION validate_stavka_vrijednost();