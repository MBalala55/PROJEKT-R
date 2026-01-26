package hr.elektropregled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChecklistParametar", description = "Parametar provjere s default vrijednostima")
public class ChecklistParametarDto {
    private Integer idParametra;
    private String nazParametra;
    private String tipPodataka;
    private Double minVrijednost;
    private Double maxVrijednost;
    private String mjernaJedinica;
    private Boolean obavezan;
    private Integer redoslijed;
    private Boolean defaultBool;
    private Double defaultNum;
    private String defaultTxt;
    private LocalDateTime zadnjiPregledAt;
}