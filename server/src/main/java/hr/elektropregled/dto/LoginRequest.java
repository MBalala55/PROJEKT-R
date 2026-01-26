package hr.elektropregled.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za prijavu")
public class LoginRequest {
    @NotBlank(message = "Korisničko ime je obavezno")
    @JsonProperty("korisnicko_ime")
    @Schema(description = "Korisničko ime", example = "mmarkovic")
    private String korisnickoIme;

    @NotBlank(message = "Lozinka je obavezna")
    @JsonProperty("lozinka")
    @Schema(description = "Lozinka", example = "pass123")
    private String lozinka;
}