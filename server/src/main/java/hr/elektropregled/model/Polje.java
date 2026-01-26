package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "polje")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Polje {
    @Id
    @Column(name = "id_polje")
    private Integer idPolje;

    @NotNull(message = "Naponska razina je obavezna")
    @Column(name = "nap_razina", nullable = false)
    private Double napRazina;

    @NotBlank(message = "Oznaka vrste je obavezna")
    @Column(name = "ozn_vr_polje", nullable = false, length = 20)
    private String oznVrPolje;

    @NotBlank(message = "Naziv je obavezan")
    @Column(name = "naz_polje", nullable = false, length = 100)
    private String nazPolje;

    @NotNull(message = "Postrojenje je obavezno")
    @ManyToOne
    @JoinColumn(name = "id_postr", nullable = false)
    private Postrojenje postrojenje;
}