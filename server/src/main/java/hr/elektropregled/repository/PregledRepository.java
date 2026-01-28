package hr.elektropregled.repository;

import hr.elektropregled.model.Pregled;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PregledRepository extends JpaRepository<Pregled, Integer> {
    Optional<Pregled> findByLokalniId(UUID lokalniId);

    interface PostrojenjePregledAgg {
        Integer getIdPostr();
        Long getTotal();
        LocalDateTime getLastDate();
        String getZadnjiKorisnik();
    }

    @Query("SELECT p.postrojenje.idPostr as idPostr, COUNT(p.idPreg) as total, " +
            "MAX(COALESCE(p.kraj, p.pocetak, p.createdAt)) as lastDate, " +
            "(SELECT k.korisnickoIme FROM Pregled p2 JOIN p2.korisnik k " +
            "WHERE p2.postrojenje.idPostr = p.postrojenje.idPostr " +
            "ORDER BY COALESCE(p2.kraj, p2.pocetak, p2.createdAt) DESC LIMIT 1) as zadnjiKorisnik " +
            "FROM Pregled p GROUP BY p.postrojenje.idPostr")
    java.util.List<PostrojenjePregledAgg> aggregateByPostrojenje();
}