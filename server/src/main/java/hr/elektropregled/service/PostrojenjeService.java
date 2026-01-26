package hr.elektropregled.service;

import hr.elektropregled.dto.ChecklistDeviceDto;
import hr.elektropregled.dto.ChecklistParametarDto;
import hr.elektropregled.dto.PoljeDto;
import hr.elektropregled.dto.PostrojenjeSummaryDto;
import hr.elektropregled.exception.NotFoundException;
import hr.elektropregled.exception.ValidationException;
import hr.elektropregled.model.ParametarProvjere;
import hr.elektropregled.model.StavkaPregleda;
import hr.elektropregled.model.Uredaj;
import hr.elektropregled.repository.ParametarProvjereRepository;
import hr.elektropregled.repository.PoljeRepository;
import hr.elektropregled.repository.PostrojenjeRepository;
import hr.elektropregled.repository.PregledRepository;
import hr.elektropregled.repository.StavkaPregledaRepository;
import hr.elektropregled.repository.UredajRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostrojenjeService {
    private final PostrojenjeRepository postrojenjeRepository;
    private final PregledRepository pregledRepository;
    private final UredajRepository uredajRepository;
    private final ParametarProvjereRepository parametarProvjereRepository;
    private final StavkaPregledaRepository stavkaPregledaRepository;
    private final PoljeRepository poljeRepository;

    public PostrojenjeService(PostrojenjeRepository postrojenjeRepository,
                              PregledRepository pregledRepository,
                              UredajRepository uredajRepository,
                              ParametarProvjereRepository parametarProvjereRepository,
                              StavkaPregledaRepository stavkaPregledaRepository,
                              PoljeRepository poljeRepository) {
        this.postrojenjeRepository = postrojenjeRepository;
        this.pregledRepository = pregledRepository;
        this.uredajRepository = uredajRepository;
        this.parametarProvjereRepository = parametarProvjereRepository;
        this.stavkaPregledaRepository = stavkaPregledaRepository;
        this.poljeRepository = poljeRepository;
    }

    public List<PostrojenjeSummaryDto> listPostrojenjaWithStats() {
        Map<Integer, PregledRepository.PostrojenjePregledAgg> aggByPostrojenje = pregledRepository.aggregateByPostrojenje()
                .stream()
                .collect(Collectors.toMap(PregledRepository.PostrojenjePregledAgg::getIdPostr, a -> a));

        return postrojenjeRepository.findAll(Sort.by("idPostr")).stream()
                .map(p -> {
                    PregledRepository.PostrojenjePregledAgg agg = aggByPostrojenje.get(p.getIdPostr());
                    Long total = agg != null ? agg.getTotal() : 0L;
                    LocalDateTime lastDate = agg != null ? agg.getLastDate() : null;
                    return new PostrojenjeSummaryDto(
                            p.getIdPostr(),
                            p.getNazPostr(),
                            p.getLokacija(),
                            p.getOznVrPostr(),
                            total,
                            lastDate
                    );
                })
                .toList();
    }

    public List<PoljeDto> listPolja(Integer postrojenjeId) {
        postrojenjeRepository.findById(postrojenjeId)
                .orElseThrow(() -> new NotFoundException("Postrojenje nije pronađeno"));

        List<PoljeDto> result = new ArrayList<>();

        // Dohvati polja s brojem uređaja
        List<PoljeRepository.PoljeWithCount> polja = poljeRepository.findByPostrojenjeWithCount(postrojenjeId);
        for (PoljeRepository.PoljeWithCount p : polja) {
            result.add(new PoljeDto(
                    p.getIdPolje(),
                    p.getNazPolje(),
                    p.getNapRazina(),
                    p.getOznVrPolje(),
                    p.getBrojUredaja()
            ));
        }

        // Provjeri ima li uređaja bez polja
        Long bezPolja = uredajRepository.countByPostrojenjeAndPoljeIsNull(postrojenjeId);
        if (bezPolja > 0) {
            result.add(new PoljeDto(null, "Direktno na postrojenju", null, null, bezPolja));
        }

        return result;
    }

    public List<ChecklistDeviceDto> getChecklist(Integer postrojenjeId, Integer idPolje) {
        postrojenjeRepository.findById(postrojenjeId)
                .orElseThrow(() -> new NotFoundException("Postrojenje nije pronađeno"));

        if (idPolje == null) {
            throw new ValidationException("Parametar id_polje je obavezan");
        }

        // Dohvati uređaje za odabrano polje (ili null za direktno na postrojenju)
        List<Uredaj> uredaji;
        if (idPolje == 0) {
            // Virtualno polje: uređaji bez polja
            uredaji = uredajRepository.findByPostrojenje_IdPostrAndPoljeIsNullOrderByIdUredAsc(postrojenjeId);
        } else {
            uredaji = uredajRepository.findByPostrojenje_IdPostrAndPolje_IdPoljeOrderByIdUredAsc(postrojenjeId, idPolje);
        }

        if (uredaji.isEmpty()) {
            return List.of();
        }

        // Batch query: dohvati sve zadnje stavke za sve uređaje odjednom
        List<Integer> uredajIds = uredaji.stream().map(Uredaj::getIdUred).toList();
        List<StavkaPregleda> latestStavke = stavkaPregledaRepository.findLatestByUredajIds(uredajIds);

        // Mapa: (idUred, idParametra) -> StavkaPregleda
        Map<String, StavkaPregleda> stavkaMap = latestStavke.stream()
                .collect(Collectors.toMap(
                        s -> s.getUredaj().getIdUred() + "_" + s.getParametarProvjere().getIdParametra(),
                        s -> s
                ));

        List<ChecklistDeviceDto> deviceDtos = new ArrayList<>();

        for (Uredaj uredaj : uredaji) {
            List<ParametarProvjere> parametri = parametarProvjereRepository
                    .findByVrstaUredaja_IdVrUredOrderByRedoslijedAsc(uredaj.getVrstaUredaja().getIdVrUred());

            List<ChecklistParametarDto> paramDtoList = new ArrayList<>();
            for (ParametarProvjere parametar : parametri) {
                String key = uredaj.getIdUred() + "_" + parametar.getIdParametra();
                StavkaPregleda last = stavkaMap.get(key);

                Boolean defaultBool = null;
                Double defaultNum = null;
                String defaultTxt = null;
                LocalDateTime lastAt = null;

                if (last != null) {
                    defaultBool = last.getVrijednostBool();
                    defaultNum = last.getVrijednostNum();
                    defaultTxt = last.getVrijednostTxt();
                    lastAt = resolvePregledTime(last);
                } else if ("BOOLEAN".equalsIgnoreCase(parametar.getTipPodataka())) {
                    defaultBool = Boolean.TRUE; // preselect ispravno
                }

                paramDtoList.add(new ChecklistParametarDto(
                        parametar.getIdParametra(),
                        parametar.getNazParametra(),
                        parametar.getTipPodataka(),
                        parametar.getMinVrijednost(),
                        parametar.getMaxVrijednost(),
                        parametar.getMjernaJedinica(),
                        parametar.getObavezan(),
                        parametar.getRedoslijed(),
                        defaultBool,
                        defaultNum,
                        defaultTxt,
                        lastAt
                ));
            }

            Integer poljeId = uredaj.getPolje() != null ? uredaj.getPolje().getIdPolje() : null;
            String poljeNaz = uredaj.getPolje() != null ? uredaj.getPolje().getNazPolje() : null;
            Double napRazina = uredaj.getPolje() != null ? uredaj.getPolje().getNapRazina() : null;

            deviceDtos.add(new ChecklistDeviceDto(
                    uredaj.getIdUred(),
                    uredaj.getNatpPlocica(),
                    uredaj.getTvBroj(),
                    uredaj.getVrstaUredaja().getOznVrUred(),
                    uredaj.getVrstaUredaja().getNazVrUred(),
                    poljeId,
                    poljeNaz,
                    napRazina,
                    paramDtoList
            ));
        }

        return deviceDtos;
    }

    private LocalDateTime resolvePregledTime(StavkaPregleda stavka) {
        LocalDateTime kraj = stavka.getPregled().getKraj();
        if (kraj != null) {
            return kraj;
        }
        LocalDateTime pocetak = stavka.getPregled().getPocetak();
        if (pocetak != null) {
            return pocetak;
        }
        return stavka.getPregled().getCreatedAt();
    }
}