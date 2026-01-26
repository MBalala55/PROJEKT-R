package hr.elektropregled.controller;

import hr.elektropregled.dto.PregledSyncRequest;
import hr.elektropregled.dto.SyncResponse;
import hr.elektropregled.exception.ValidationException;
import hr.elektropregled.service.PregledSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/pregled")
@Tag(name = "Pregled Sync", description = "Endpointi za sinkronizaciju pregleda")
public class PregledSyncController {
    private final PregledSyncService pregledSyncService;

    public PregledSyncController(PregledSyncService pregledSyncService) {
        this.pregledSyncService = pregledSyncService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Sinkronizira pregled sa stavkama",
            description = "Prima pregled i stavke iz mobilne aplikacije, validira podatke i sprema na server. " +
                    "Zahtijeva JWT token u Authorization header-u.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pregled uspješno sinkroniziran",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SyncResponse.class))),
            @ApiResponse(responseCode = "400", description = "Neispravan zahtjev (validacijska greška, out-of-range vrijednost)",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Nedostaje ili neispravan JWT token",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Korisnik, postrojenje, uređaj ili parametar nije pronađen",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Pregled ili stavka s istim lokalnim ID-om je već sinkronizirana",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Greška na serveru",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<SyncResponse> syncPregled(@Valid @RequestBody PregledSyncRequest request, 
                                                     HttpServletRequest httpRequest) {
        String username = (String) httpRequest.getAttribute("username");
        if (username == null) {
            throw new ValidationException("Neispravan ili nedostaje JWT token");
        }
        SyncResponse response = pregledSyncService.sync(request);
        return ResponseEntity.ok(response);
    }
}