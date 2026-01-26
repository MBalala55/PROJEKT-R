package hr.elektropregled.controller;

import hr.elektropregled.dto.LoginRequest;
import hr.elektropregled.dto.LoginResponse;
import hr.elektropregled.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Endpointi za autentifikaciju")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Prijava korisnika", description = "Provjerava korisničko ime i lozinku te vraća JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uspješna prijava",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Neispravan zahtjev (nedostaju polja)",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Korisničko ime ili lozinka nisu ispravni",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Greška na serveru",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}