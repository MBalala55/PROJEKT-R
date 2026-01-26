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
    }

    @Query("SELECT p.postrojenje.idPostr as idPostr, COUNT(p.idPreg) as total, " +
            "MAX(COALESCE(p.kraj, p.pocetak, p.createdAt)) as lastDate " +
            "FROM Pregled p GROUP BY p.postrojenje.idPostr")
    java.util.List<PostrojenjePregledAgg> aggregateByPostrojenje();
}