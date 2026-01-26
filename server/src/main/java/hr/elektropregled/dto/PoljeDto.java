package hr.elektropregled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PoljeDto", description = "Polje s brojem uređaja")
public class PoljeDto {
    @Schema(description = "ID polja (null za uređaje bez polja)")
    private Integer idPolje;

    @Schema(description = "Naziv polja")
    private String nazPolje;

    @Schema(description = "Naponska razina (kV)")
    private Double napRazina;

    @Schema(description = "Oznaka vrste polja")
    private String oznVrPolje;

    @Schema(description = "Broj uređaja u polju")
    private Long brojUredaja;
}