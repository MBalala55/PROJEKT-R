package hr.elektropregled.controller;

import hr.elektropregled.dto.ChecklistDeviceDto;
import hr.elektropregled.dto.PoljeDto;
import hr.elektropregled.dto.PostrojenjeSummaryDto;
import hr.elektropregled.service.PostrojenjeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/postrojenja")
@Tag(name = "Postrojenja", description = "Pregled postrojenja, polja i generiranje checklist-a")
public class PostrojenjeController {
    private final PostrojenjeService postrojenjeService;

    public PostrojenjeController(PostrojenjeService postrojenjeService) {
        this.postrojenjeService = postrojenjeService;
    }

    @GetMapping
    @Operation(summary = "Lista postrojenja", description = "Vraća sva postrojenja s brojem pregleda i datumom zadnjeg pregleda")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspjeh",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PostrojenjeSummaryDto.class)))),
            @ApiResponse(responseCode = "401", description = "Neautorizirano", content = @Content),
            @ApiResponse(responseCode = "500", description = "Greška na serveru", content = @Content)
    })
    public ResponseEntity<List<PostrojenjeSummaryDto>> listPostrojenja() {
        return ResponseEntity.ok(postrojenjeService.listPostrojenjaWithStats());
    }

    @GetMapping("/{id}/polja")
    @Operation(summary = "Lista polja u postrojenju",
            description = "Vraća sva polja u postrojenju s brojem uređaja. Uključuje virtualno polje 'Direktno na postrojenju' ako postoje uređaji bez polja.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspjeh",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PoljeDto.class)))),
            @ApiResponse(responseCode = "401", description = "Neautorizirano", content = @Content),
            @ApiResponse(responseCode = "404", description = "Postrojenje nije pronađeno", content = @Content),
            @ApiResponse(responseCode = "500", description = "Greška na serveru", content = @Content)
    })
    public ResponseEntity<List<PoljeDto>> listPolja(@PathVariable("id") Integer idPostrojenja) {
        return ResponseEntity.ok(postrojenjeService.listPolja(idPostrojenja));
    }

    @GetMapping("/{id}/checklist")
    @Operation(summary = "Checklist za polje u postrojenju",
            description = "Generira checklist za uređaje u odabranom polju s default vrijednostima (zadnji pregled, inače ispravno za BOOLEAN). " +
                    "Parametar id_polje je OBAVEZAN. Za uređaje bez polja koristi id_polje=0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspjeh",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ChecklistDeviceDto.class)))),
            @ApiResponse(responseCode = "400", description = "Nedostaje parametar id_polje", content = @Content),
            @ApiResponse(responseCode = "401", description = "Neautorizirano", content = @Content),
            @ApiResponse(responseCode = "404", description = "Postrojenje nije pronađeno", content = @Content),
            @ApiResponse(responseCode = "500", description = "Greška na serveru", content = @Content)
    })
    public ResponseEntity<List<ChecklistDeviceDto>> getChecklist(
            @PathVariable("id") Integer idPostrojenja,
            @Parameter(description = "ID polja (obavezno; koristi 0 za uređaje bez polja)", required = true)
            @RequestParam("id_polje") Integer idPolje) {
        return ResponseEntity.ok(postrojenjeService.getChecklist(idPostrojenja, idPolje));
    }
}