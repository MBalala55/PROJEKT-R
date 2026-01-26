package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "uredaj", uniqueConstraints = {
    @UniqueConstraint(columnNames = "tv_broj", name = "uq_uredaj_tv_broj")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Uredaj {
    @Id
    @Column(name = "id_ured")
    private Integer idUred;

    @NotBlank(message = "Natpisna pločica je obavezna")
    @Column(name = "natp_plocica", nullable = false, length = 20)
    private String natpPlocica;

    @NotBlank(message = "Tvorički broj je obavezan")
    @Column(name = "tv_broj", nullable = false, length = 30, unique = true)
    private String tvBroj;

    @NotNull(message = "Postrojenje je obavezno")
    @ManyToOne
    @JoinColumn(name = "id_postr", nullable = false)
    private Postrojenje postrojenje;

    @ManyToOne
    @JoinColumn(name = "id_polje")
    private Polje polje;

    @NotNull(message = "Vrsta uređaja je obavezna")
    @ManyToOne
    @JoinColumn(name = "id_vr_ured", nullable = false)
    private VrstaUredaja vrstaUredaja;
}