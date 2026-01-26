package hr.elektropregled.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za sinkronizaciju pregleda s mobilne aplikacije")
public class PregledSyncRequest {
    @NotNull
    @Valid
    @JsonProperty("pregled")
    @Schema(description = "Podatci pregleda")
    private PregledDto pregled;

    @NotEmpty
    @Valid
    @JsonProperty("stavke")
    @Schema(description = "Lista stavki pregleda (najmanje jedna)")
    private List<StavkaDto> stavke;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Pregled elektroenergetskog postrojenja")
    public static class PregledDto {
        @NotNull
        @JsonProperty("lokalni_id")
        @Schema(description = "Jedinstveni UUID generiran na mobilnoj aplikaciji", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID lokalniId;

        @NotNull
        @JsonProperty("id_korisnika")
        @Schema(description = "ID korisnika koji provodi pregled", example = "1")
        private Integer idKorisnika;

        @NotNull
        @JsonProperty("id_postr")
        @Schema(description = "ID postrojenja", example = "1")
        private Integer idPostr;

        @NotNull
        @JsonProperty("pocetak")
        @Schema(description = "Vrijeme početka pregleda (ISO 8601)", example = "2026-01-26T10:30:00")
        private LocalDateTime pocetak;

        @JsonProperty("kraj")
        @Schema(description = "Vrijeme kraja pregleda (ISO 8601)", example = "2026-01-26T11:00:00")
        private LocalDateTime kraj;

        @JsonProperty("napomena")
        @Schema(description = "Dodatne bilješke o pregledu", example = "Pregled izvršen bez problema")
        private String napomena;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Stavka pregleda (pojedini parametar)")
    public static class StavkaDto {
        @NotNull
        @JsonProperty("lokalni_id")
        @Schema(description = "Jedinstveni UUID stavke generiran na mobilnoj aplikaciji", example = "550e8400-e29b-41d4-a716-446655440001")
        private UUID lokalniId;

        @NotNull
        @JsonProperty("id_ured")
        @Schema(description = "ID uređaja koji se provjerava", example = "1")
        private Integer idUred;

        @NotNull
        @JsonProperty("id_parametra")
        @Schema(description = "ID parametra koji se provjerava", example = "1")
        private Integer idParametra;

        @JsonProperty("vrijednost_bool")
        @Schema(description = "Vrijednost logičkog tipa (true/false)", example = "true")
        private Boolean vrijednostBool;

        @JsonProperty("vrijednost_num")
        @Schema(description = "Vrijednost numeričkog tipa", example = "45.5")
        private Double vrijednostNum;

        @JsonProperty("vrijednost_txt")
        @Schema(description = "Vrijednost tekstualnog tipa", example = "Stanje OK")
        private String vrijednostTxt;

        @JsonProperty("napomena")
        @Schema(description = "Napomena o stavki", example = "Mjerenje obavljeno termalom kamerom")
        private String napomena;

        @JsonProperty("vrijeme_unosa")
        @Schema(description = "Vrijeme unosa stavke (ISO 8601)", example = "2026-01-26T10:35:00")
        private LocalDateTime vrijemeUnosa;
    }
}