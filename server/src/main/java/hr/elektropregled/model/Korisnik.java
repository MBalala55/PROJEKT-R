package hr.elektropregled.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "korisnik", uniqueConstraints = {
    @UniqueConstraint(columnNames = "korisnicko_ime", name = "uq_korisnik_korisnicko_ime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Korisnik {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_korisnika")
    private Integer idKorisnika;

    @NotBlank(message = "Ime je obavezno")
    @Column(name = "ime", nullable = false, length = 50)
    private String ime;

    @NotBlank(message = "Prezime je obavezno")
    @Column(name = "prezime", nullable = false, length = 50)
    private String prezime;

    @NotBlank(message = "Korisničko ime je obavezno")
    @Column(name = "korisnicko_ime", nullable = false, length = 30, unique = true)
    private String korisnickoIme;

    @NotBlank(message = "Lozinka je obavezna")
    @Column(name = "lozinka", nullable = false, length = 255)
    private String lozinka;

    @NotBlank(message = "Uloga je obavezna")
    @Column(name = "uloga", nullable = false, length = 20)
    private String uloga; // ADMIN, RADNIK
}