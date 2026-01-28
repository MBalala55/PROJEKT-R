package hr.elektropregled.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PostrojenjeSummary", description = "Sažetak postrojenja s brojem i zadnjim pregledom")
public class PostrojenjeSummaryDto {
    @Schema(description = "ID postrojenja")
    private Integer idPostr;

    @Schema(description = "Naziv postrojenja")
    private String nazPostr;

    @Schema(description = "Lokacija")
    private String lokacija;

    @Schema(description = "Oznaka vrste postrojenja")
    private String oznVrPostr;

    @Schema(description = "Ukupan broj pregleda")
    private Long totalPregleda;

    @Schema(description = "Datum/vrijeme zadnjeg pregleda (kraj, ako postoji; inače početak)")
    private LocalDateTime zadnjiPregled;

    @Schema(description = "Korisničko ime korisnika koji je obavio zadnji pregled")
    private String zadnjiKorisnik;
}