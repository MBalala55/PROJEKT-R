package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vrsta_uredaja", uniqueConstraints = {
    @UniqueConstraint(columnNames = "ozn_vr_ured", name = "uq_vrsta_uredaja_ozn_vr_ured"),
    @UniqueConstraint(columnNames = "naz_vr_ured", name = "uq_vrsta_uredaja_naz_vr_ured")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VrstaUredaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vr_ured")
    private Integer idVrUred;

    @NotBlank(message = "Oznaka je obavezna")
    @Column(name = "ozn_vr_ured", nullable = false, length = 5, unique = true)
    private String oznVrUred;

    @NotBlank(message = "Naziv je obavezan")
    @Column(name = "naz_vr_ured", nullable = false, length = 50, unique = true)
    private String nazVrUred;
}