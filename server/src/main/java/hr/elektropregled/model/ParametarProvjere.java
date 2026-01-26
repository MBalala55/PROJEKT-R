package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parametar_provjere")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametarProvjere {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametra")
    private Integer idParametra;

    @NotBlank(message = "Naziv parametra je obavezan")
    @Column(name = "naz_parametra", nullable = false, length = 100)
    private String nazParametra;

    @NotBlank(message = "Tip podataka je obavezan")
    @Column(name = "tip_podataka", nullable = false, length = 20)
    private String tipPodataka; // BOOLEAN, NUMERIC, TEXT

    @Column(name = "min_vrijednost")
    private Double minVrijednost;

    @Column(name = "max_vrijednost")
    private Double maxVrijednost;

    @Column(name = "mjerna_jedinica", length = 20)
    private String mjernaJedinica;

    @NotNull(message = "Obaveznost je obavezna")
    @Column(name = "obavezan", nullable = false)
    private Boolean obavezan;

    @NotNull(message = "Redoslijed je obavezan")
    @Column(name = "redoslijed", nullable = false)
    private Integer redoslijed;

    @Column(name = "opis", columnDefinition = "TEXT")
    private String opis;

    @NotNull(message = "Vrsta uređaja je obavezna")
    @ManyToOne
    @JoinColumn(name = "id_vr_ured", nullable = false)
    private VrstaUredaja vrstaUredaja;
}