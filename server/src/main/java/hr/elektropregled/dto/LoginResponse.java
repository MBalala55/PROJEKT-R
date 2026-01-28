package hr.elektropregled.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Odgovor na zahtjev prijave")
public class LoginResponse {
    @JsonProperty("access_token")
    @Schema(description = "JWT token za autentifikaciju", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @JsonProperty("token_type")
    @Schema(description = "Tip tokena", example = "Bearer")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @Schema(description = "Trajanje tokena u sekundama", example = "86400")
    private long expiresIn;

    @JsonProperty("username")
    @Schema(description = "Korisničko ime", example = "mmarkovic")
    private String username;

    @JsonProperty("user_id")
    @Schema(description = "ID korisnika", example = "1")
    private Integer userId;
}