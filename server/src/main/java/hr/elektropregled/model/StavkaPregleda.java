package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stavka_pregleda", uniqueConstraints = {
    @UniqueConstraint(columnNames = "lokalni_id", name = "uq_stavka_pregleda_lokalni_id"),
    @UniqueConstraint(columnNames = {"id_preg", "id_parametra", "id_ured"}, name = "uq_stavka_unique_check")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StavkaPregleda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stavke")
    private Integer idStavke;

    @NotNull(message = "Lokalni ID je obavezan")
    @Column(name = "lokalni_id", nullable = false, columnDefinition = "UUID", unique = true)
    private UUID lokalniId;

    @Column(name = "server_id")
    private Integer serverId;

    @Column(name = "vrijednost_bool")
    private Boolean vrijednostBool;

    @Column(name = "vrijednost_num")
    private Double vrijednostNum;

    @Column(name = "vrijednost_txt", length = 255)
    private String vrijednostTxt;

    @Column(name = "napomena", length = 255)
    private String napomena;

    @NotNull(message = "Vrijeme unosa je obavezno")
    @Column(name = "vrijeme_unosa", nullable = false)
    private LocalDateTime vrijemeUnosa;

    @NotNull(message = "Pregled je obavezan")
    @ManyToOne
    @JoinColumn(name = "id_preg", nullable = false)
    private Pregled pregled;

    @NotNull(message = "Uređaj je obavezan")
    @ManyToOne
    @JoinColumn(name = "id_ured", nullable = false)
    private Uredaj uredaj;

    @NotNull(message = "Parametar provjere je obavezan")
    @ManyToOne
    @JoinColumn(name = "id_parametra", nullable = false)
    private ParametarProvjere parametarProvjere;
}