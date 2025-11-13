CREATE TABLE korisnik (
    korisnik_id      BIGINT NOT NULL PRIMARY KEY,
    ime              VARCHAR(20) NOT NULL,
    prezime          VARCHAR(20) NOT NULL,
    korisnicko_ime   VARCHAR(20) NOT NULL UNIQUE,
    lozinka_hash     VARCHAR(255) NOT NULL
);

CREATE TABLE postrojenje (
    postrojenje_id   BIGINT NOT NULL PRIMARY KEY,
    naziv            VARCHAR(50) NOT NULL UNIQUE,
    sifra            VARCHAR(20),
    lokacija         VARCHAR(50)
);

CREATE TABLE polje (
    polje_id             BIGINT NOT NULL PRIMARY KEY,
    postrojenje_id       BIGINT NOT NULL REFERENCES postrojenje(postrojenje_id),
    naziv                VARCHAR(50) NOT NULL,
    naponska_razina      INTEGER NOT NULL
);

CREATE TABLE vrsta_uredjaja (
    vrsta_uredjaja_id    BIGINT NOT NULL PRIMARY KEY,
    oznaka               VARCHAR(10) NOT NULL,
    naziv                VARCHAR(50) NOT NULL
);

CREATE TABLE uredjaj (
    uredjaj_id           BIGINT NOT NULL PRIMARY KEY,
    postrojenje_id       BIGINT NOT NULL REFERENCES postrojenje(postrojenje_id),
    polje_id             BIGINT REFERENCES polje(polje_id),
    vrsta_uredjaja_id    BIGINT NOT NULL REFERENCES vrsta_uredjaja(vrsta_uredjaja_id),
    oznaka               VARCHAR(20) NOT NULL,
    tvornicki_broj       VARCHAR(50)
);

CREATE TABLE stavka (
    stavka_id   		BIGINT NOT NULL PRIMARY KEY,
    vrsta_uredjaja_id 	BIGINT NOT NULL REFERENCES vrsta_uredjaja(vrsta_uredjaja_id),
    naziv             	VARCHAR(150) NOT NULL,
    tip					VARCHAR(10) NOT NULL
						CHECK (tip IN ('BOOL', 'INT', 'NUMERIC', 'TEXT')),
    jedinica_mjere   	VARCHAR(20)
);

CREATE TABLE pregled (
    pregled_id        BIGINT NOT NULL PRIMARY KEY,
    korisnik_id       BIGINT NOT NULL REFERENCES korisnik(korisnik_id),
    postrojenje_id    BIGINT NOT NULL REFERENCES postrojenje(postrojenje_id),
    datum             DATE NOT NULL,
    napomena          TEXT
);

CREATE TABLE pregled_stavke (
    pregled_stavke_id	BIGINT NOT NULL PRIMARY KEY,
    pregled_id       	BIGINT NOT NULL REFERENCES pregled(pregled_id),
    uredjaj_id     		BIGINT NOT NULL REFERENCES uredjaj(uredjaj_id),
    stavka_id   		BIGINT NOT NULL REFERENCES stavka(stavka_id),
    pregledano        	BOOLEAN NOT NULL DEFAULT FALSE,
    vrijednost_broj 	NUMERIC(12,3),
    vrijednost_text  	TEXT,
    neispravnost     	BOOLEAN,
    opis_neispravnosti 	TEXT
);