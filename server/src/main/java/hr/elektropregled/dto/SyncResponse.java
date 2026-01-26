package hr.elektropregled.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Odgovor na sinkronizacijski zahtjev")
public class SyncResponse {
    @Schema(description = "Je li sinkronizacija uspješna", example = "true")
    private boolean success;

    @Schema(description = "Poruka rezultata", example = "Pregled je uspješno sinkroniziran")
    private String message;

    @JsonProperty("server_pregled_id")
    @Schema(description = "ID pregleda generiran na serveru", example = "123")
    private Integer serverPregledId;

    @JsonProperty("id_mappings")
    @Schema(description = "Mapiranje lokalnih ID-eva iz mobilne aplikacije na server ID-eve")
    private IdMappings idMappings;

    @Schema(description = "Vremenska oznaka odgovora")
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Mapiranje lokalnih i server ID-eva")
    public static class IdMappings {
        @Schema(description = "Mapiranje pregleda")
        private PregledMapping pregled;

        @Schema(description = "Mapiranje stavki pregleda")
        private List<StavkaMapping> stavke;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Mapiranje pregleda lokalnog ID-a na server ID")
    public static class PregledMapping {
        @JsonProperty("lokalni_id")
        @Schema(description = "UUID iz mobilne aplikacije", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID lokalniId;

        @JsonProperty("server_id")
        @Schema(description = "ID dodijeljen na serveru", example = "123")
        private Integer serverId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Mapiranje stavke lokalnog ID-a na server ID")
    public static class StavkaMapping {
        @JsonProperty("lokalni_id")
        @Schema(description = "UUID stavke iz mobilne aplikacije", example = "550e8400-e29b-41d4-a716-446655440001")
        private UUID lokalniId;

        @JsonProperty("server_id")
        @Schema(description = "ID stavke dodijeljen na serveru", example = "456")
        private Integer serverId;
    }
}