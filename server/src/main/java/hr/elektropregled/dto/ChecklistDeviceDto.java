package hr.elektropregled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChecklistDevice", description = "Uređaj s parametrima za provjeru i default vrijednostima")
public class ChecklistDeviceDto {
    private Integer idUred;
    private String natpPlocica;
    private String tvBroj;
    private String oznVrUred;
    private String nazVrUred;
    private Integer idPolje;
    private String nazPolje;
    private Double napRazina;
    private List<ChecklistParametarDto> parametri;
}