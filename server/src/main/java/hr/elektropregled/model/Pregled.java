package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pregled", uniqueConstraints = {
    @UniqueConstraint(columnNames = "lokalni_id", name = "uq_pregled_lokalni_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pregled {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preg")
    private Integer idPreg;

    @NotNull(message = "Lokalni ID je obavezan")
    @Column(name = "lokalni_id", nullable = false, columnDefinition = "UUID", unique = true)
    private UUID lokalniId;

    @Column(name = "server_id")
    private Integer serverId;

    @NotBlank(message = "Status sinkronizacije je obavezan")
    @Column(name = "status_sync", nullable = false, length = 20)
    private String statusSync; // PENDING, SYNCING, SYNCED, FAILED

    @NotNull(message = "Početak pregleda je obavezan")
    @Column(name = "pocetak", nullable = false)
    private LocalDateTime pocetak;

    @Column(name = "kraj")
    private LocalDateTime kraj;

    @Column(name = "napomena", length = 255)
    private String napomena;

    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;

    @NotNull(message = "Korisnik je obavezan")
    @ManyToOne
    @JoinColumn(name = "id_korisnika", nullable = false)
    private Korisnik korisnik;

    @NotNull(message = "Postrojenje je obavezno")
    @ManyToOne
    @JoinColumn(name = "id_postr", nullable = false)
    private Postrojenje postrojenje;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}