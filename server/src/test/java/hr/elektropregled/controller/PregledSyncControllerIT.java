package hr.elektropregled.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.elektropregled.model.*;
import hr.elektropregled.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PregledSyncControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PregledRepository pregledRepository;
    @Autowired
    private StavkaPregledaRepository stavkaPregledaRepository;
    @Autowired
    private KorisnikRepository korisnikRepository;
    @Autowired
    private PostrojenjeRepository postrojenjeRepository;
    @Autowired
    private UredajRepository uredajRepository;
    @Autowired
    private ParametarProvjereRepository parametarProvjereRepository;
    @Autowired
    private VrstaUredajaRepository vrstaUredajaRepository;

    private Integer korisnikId;
    private Integer postrojenjeId;
    private Integer uredajId;
    private Integer paramBoolId;
    private Integer paramNumId;

    @BeforeEach
    void setUp() {
        // clean tables respecting FK order
        stavkaPregledaRepository.deleteAll();
        pregledRepository.deleteAll();
        uredajRepository.deleteAll();
        parametarProvjereRepository.deleteAll();
        vrstaUredajaRepository.deleteAll();
        postrojenjeRepository.deleteAll();
        korisnikRepository.deleteAll();

        Korisnik korisnik = new Korisnik();
        korisnik.setIme("Marko");
        korisnik.setPrezime("Markovic");
        korisnik.setKorisnickoIme("mmarkovic");
        korisnik.setLozinka("pass123");
        korisnik.setUloga("RADNIK");
        korisnikId = korisnikRepository.save(korisnik).getIdKorisnika();

        Postrojenje postrojenje = new Postrojenje();
        postrojenje.setIdPostr(1);
        postrojenje.setOznVrPostr("TP");
        postrojenje.setNazPostr("TP Centar");
        postrojenje.setLokacija("Lokacija 1");
        postrojenjeId = postrojenjeRepository.save(postrojenje).getIdPostr();

        VrstaUredaja vrsta = new VrstaUredaja();
        vrsta.setOznVrUred("PK");
        vrsta.setNazVrUred("Prekidac");
        vrsta = vrstaUredajaRepository.save(vrsta);

        Uredaj uredaj = new Uredaj();
        uredaj.setIdUred(1);
        uredaj.setNatpPlocica("PK1");
        uredaj.setTvBroj("TV-001");
        uredaj.setPostrojenje(postrojenje);
        uredaj.setVrstaUredaja(vrsta);
        uredajId = uredajRepository.save(uredaj).getIdUred();

        ParametarProvjere boolParam = new ParametarProvjere();
        boolParam.setNazParametra("Vidna Ispravnost");
        boolParam.setTipPodataka("BOOLEAN");
        boolParam.setObavezan(true);
        boolParam.setRedoslijed(1);
        boolParam.setOpis("Vizualna provjera");
        boolParam.setVrstaUredaja(vrsta);
        paramBoolId = parametarProvjereRepository.save(boolParam).getIdParametra();

        ParametarProvjere numParam = new ParametarProvjere();
        numParam.setNazParametra("Temperatura Kontakata");
        numParam.setTipPodataka("NUMERIC");
        numParam.setMinVrijednost(10.0);
        numParam.setMaxVrijednost(80.0);
        numParam.setMjernaJedinica("C");
        numParam.setObavezan(true);
        numParam.setRedoslijed(2);
        numParam.setOpis("Mjerenje temp");
        numParam.setVrstaUredaja(vrsta);
        paramNumId = parametarProvjereRepository.save(numParam).getIdParametra();
    }

    @Test
    void shouldSyncPregledAndStavke() throws Exception {
        String payload = validPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                korisnikId,
                postrojenjeId,
                uredajId,
                paramBoolId,
                paramNumId,
                45.0
        );

        mockMvc.perform(post("/api/v1/pregled/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.server_pregled_id").isNumber())
                .andExpect(jsonPath("$.id_mappings.pregled.server_id").isNumber())
                .andExpect(jsonPath("$.id_mappings.stavke", hasSize(2)));
    }

    @Test
    void shouldReturnConflictOnDuplicatePregled() throws Exception {
        UUID pregledId = UUID.randomUUID();
        String payload = validPayload(
                pregledId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                korisnikId,
                postrojenjeId,
                uredajId,
                paramBoolId,
                paramNumId,
                45.0
        );

        mockMvc.perform(post("/api/v1/pregled/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pregled/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("sinkroniziran")));
    }

    @Test
    void shouldReturnNotFoundWhenKorisnikMissing() throws Exception {
        String payload = validPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                9999,
                postrojenjeId,
                uredajId,
                paramBoolId,
                paramNumId,
                45.0
        );

        mockMvc.perform(post("/api/v1/pregled/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Korisnik")));
    }

    @Test
    void shouldValidateNumericRange() throws Exception {
        String payload = validPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                korisnikId,
                postrojenjeId,
                uredajId,
                paramBoolId,
                paramNumId,
                200.0 // out of range (> 80)
        );

        mockMvc.perform(post("/api/v1/pregled/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Veća")));
    }

    private String validPayload(UUID pregledId,
                                UUID stavkaBoolId,
                                UUID stavkaNumId,
                                Integer korisnik,
                                Integer postrojenje,
                                Integer uredaj,
                                Integer paramBool,
                                Integer paramNum,
                                Double numericValue) throws Exception {

        Map<String, Object> pregled = Map.of(
                "lokalni_id", pregledId,
                "pocetak", LocalDateTime.of(2026, 1, 26, 10, 30),
                "id_korisnika", korisnik,
                "id_postr", postrojenje,
                "napomena", "Integration test"
        );

        Map<String, Object> stavkaBool = Map.of(
                "lokalni_id", stavkaBoolId,
                "id_parametra", paramBool,
                "id_ured", uredaj,
                "vrijednost_bool", true
        );

        Map<String, Object> stavkaNum = Map.of(
                "lokalni_id", stavkaNumId,
                "id_parametra", paramNum,
                "id_ured", uredaj,
                "vrijednost_num", numericValue
        );

        return objectMapper.writeValueAsString(Map.of(
                "pregled", pregled,
                "stavke", List.of(stavkaBool, stavkaNum)
        ));
    }
}
