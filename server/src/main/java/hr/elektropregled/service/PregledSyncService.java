package hr.elektropregled.service;

import hr.elektropregled.dto.PregledSyncRequest;
import hr.elektropregled.dto.SyncResponse;
import hr.elektropregled.exception.DuplicateSyncException;
import hr.elektropregled.exception.NotFoundException;
import hr.elektropregled.exception.ValidationException;
import hr.elektropregled.model.*;
import hr.elektropregled.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PregledSyncService {
    private final PregledRepository pregledRepository;
    private final StavkaPregledaRepository stavkaPregledaRepository;
    private final KorisnikRepository korisnikRepository;
    private final PostrojenjeRepository postrojenjeRepository;
    private final UredajRepository uredajRepository;
    private final ParametarProvjereRepository parametarProvjereRepository;

    public PregledSyncService(PregledRepository pregledRepository,
                              StavkaPregledaRepository stavkaPregledaRepository,
                              KorisnikRepository korisnikRepository,
                              PostrojenjeRepository postrojenjeRepository,
                              UredajRepository uredajRepository,
                              ParametarProvjereRepository parametarProvjereRepository) {
        this.pregledRepository = pregledRepository;
        this.stavkaPregledaRepository = stavkaPregledaRepository;
        this.korisnikRepository = korisnikRepository;
        this.postrojenjeRepository = postrojenjeRepository;
        this.uredajRepository = uredajRepository;
        this.parametarProvjereRepository = parametarProvjereRepository;
    }

    @Transactional
    public SyncResponse sync(PregledSyncRequest request) {
        if (request == null || request.getPregled() == null) {
            throw new ValidationException("Pregled je obavezan");
        }

        var pregledDto = request.getPregled();
        UUID pregledLokalniId = pregledDto.getLokalniId();
        if (pregledLokalniId == null) {
            throw new ValidationException("pregled.lokalni_id je obavezan");
        }

        pregledRepository.findByLokalniId(pregledLokalniId).ifPresent(p -> {
            throw new DuplicateSyncException("Pregled s ovim lokalnim ID-om je već sinkroniziran");
        });

        Korisnik korisnik = korisnikRepository.findById(pregledDto.getIdKorisnika())
                .orElseThrow(() -> new NotFoundException("Korisnik nije pronađen"));

        Postrojenje postrojenje = postrojenjeRepository.findById(pregledDto.getIdPostr())
                .orElseThrow(() -> new NotFoundException("Postrojenje nije pronađeno"));

        Pregled pregled = new Pregled();
        pregled.setLokalniId(pregledLokalniId);
        pregled.setStatusSync("SYNCED");
        pregled.setPocetak(pregledDto.getPocetak());
        pregled.setKraj(pregledDto.getKraj());
        pregled.setNapomena(pregledDto.getNapomena());
        pregled.setKorisnik(korisnik);
        pregled.setPostrojenje(postrojenje);

        Pregled savedPregled = pregledRepository.save(pregled);

        List<SyncResponse.StavkaMapping> stavkaMappings = new ArrayList<>();

        for (PregledSyncRequest.StavkaDto stavkaDto : request.getStavke()) {
            if (stavkaDto.getLokalniId() == null) {
                throw new ValidationException("stavka.lokalni_id je obavezan");
            }

            stavkaPregledaRepository.findByLokalniId(stavkaDto.getLokalniId()).ifPresent(sp -> {
                throw new DuplicateSyncException("Stavka s ovim lokalnim ID-om je već sinkronizirana");
            });

            Uredaj uredaj = uredajRepository.findById(stavkaDto.getIdUred())
                    .orElseThrow(() -> new NotFoundException("Uređaj nije pronađen"));

            ParametarProvjere parametar = parametarProvjereRepository.findById(stavkaDto.getIdParametra())
                    .orElseThrow(() -> new NotFoundException("Parametar nije pronađen"));

            validateVrijednosti(stavkaDto, parametar);

            StavkaPregleda stavka = new StavkaPregleda();
            stavka.setLokalniId(stavkaDto.getLokalniId());
            stavka.setVrijednostBool(stavkaDto.getVrijednostBool());
            stavka.setVrijednostNum(stavkaDto.getVrijednostNum());
            stavka.setVrijednostTxt(stavkaDto.getVrijednostTxt());
            stavka.setNapomena(stavkaDto.getNapomena());
            stavka.setVrijemeUnosa(stavkaDto.getVrijemeUnosa() != null ? stavkaDto.getVrijemeUnosa() : LocalDateTime.now());
            stavka.setPregled(savedPregled);
            stavka.setUredaj(uredaj);
            stavka.setParametarProvjere(parametar);

            StavkaPregleda savedStavka = stavkaPregledaRepository.save(stavka);
            stavkaMappings.add(new SyncResponse.StavkaMapping(stavkaDto.getLokalniId(), savedStavka.getIdStavke()));
        }

        SyncResponse.IdMappings idMappings = new SyncResponse.IdMappings(
                new SyncResponse.PregledMapping(savedPregled.getLokalniId(), savedPregled.getIdPreg()),
                stavkaMappings
        );

        SyncResponse response = new SyncResponse();
        response.setSuccess(true);
        response.setMessage("Pregled je uspješno sinkroniziran");
        response.setServerPregledId(savedPregled.getIdPreg());
        response.setIdMappings(idMappings);
        response.setTimestamp(Instant.now());
        return response;
    }

    private void validateVrijednosti(PregledSyncRequest.StavkaDto stavkaDto, ParametarProvjere parametar) {
        int countNonNull = 0;
        if (stavkaDto.getVrijednostBool() != null) countNonNull++;
        if (stavkaDto.getVrijednostNum() != null) countNonNull++;
        if (stavkaDto.getVrijednostTxt() != null) countNonNull++;
        if (countNonNull > 1) {
            throw new ValidationException("Dozvoljena je samo jedna vrijednost po stavci");
        }

        String tip = parametar.getTipPodataka();
        if ("NUMERIC".equalsIgnoreCase(tip)) {
            if (stavkaDto.getVrijednostNum() == null) {
                throw new ValidationException("Vrijednost brojčana je obavezna za NUMERIC parametar");
            }
            if (parametar.getMinVrijednost() != null && stavkaDto.getVrijednostNum() < parametar.getMinVrijednost()) {
                throw new ValidationException("Vrijednost je manja od minimalne dozvoljene");
            }
            if (parametar.getMaxVrijednost() != null && stavkaDto.getVrijednostNum() > parametar.getMaxVrijednost()) {
                throw new ValidationException("Vrijednost je veća od maksimalne dozvoljene");
            }
        } else if ("BOOLEAN".equalsIgnoreCase(tip)) {
            if (stavkaDto.getVrijednostBool() == null) {
                throw new ValidationException("Vrijednost bool je obavezna za BOOLEAN parametar");
            }
        } else if ("TEXT".equalsIgnoreCase(tip)) {
            if (stavkaDto.getVrijednostTxt() == null || stavkaDto.getVrijednostTxt().isBlank()) {
                throw new ValidationException("Vrijednost tekst je obavezna za TEXT parametar");
            }
        }
    }
}