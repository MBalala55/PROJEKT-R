package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "postrojenje", uniqueConstraints = {
    @UniqueConstraint(columnNames = "naz_postr", name = "uq_postrojenje_naz_postr")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Postrojenje {
    @Id
    @Column(name = "id_postr")
    private Integer idPostr;

    @NotBlank(message = "Oznaka vrste je obavezna")
    @Column(name = "ozn_vr_postr", nullable = false, length = 10)
    private String oznVrPostr;

    @NotBlank(message = "Naziv je obavezan")
    @Column(name = "naz_postr", nullable = false, length = 100, unique = true)
    private String nazPostr;

    @Column(name = "lokacija", length = 150)
    private String lokacija;
}