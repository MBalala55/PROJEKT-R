package hr.elektropregled.repository;

import hr.elektropregled.model.StavkaPregleda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StavkaPregledaRepository extends JpaRepository<StavkaPregleda, Integer> {
    List<StavkaPregleda> findByPregled_IdPreg(Integer idPreg);
    Optional<StavkaPregleda> findByLokalniId(UUID lokalniId);

    Optional<StavkaPregleda> findTopByUredaj_IdUredAndParametarProvjere_IdParametraOrderByPregled_KrajDescPregled_PocetakDescIdStavkeDesc(
            Integer idUred,
            Integer idParametra
    );

    @Query(value = """
        SELECT DISTINCT ON (s.id_ured, s.id_parametra) s.*
        FROM stavka_pregleda s
        JOIN pregled p ON s.id_preg = p.id_preg
        WHERE s.id_ured IN :uredajIds
        ORDER BY s.id_ured, s.id_parametra,
                 p.kraj DESC NULLS LAST,
                 p.pocetak DESC NULLS LAST,
                 s.id_stavke DESC
        """, nativeQuery = true)
    List<StavkaPregleda> findLatestByUredajIds(@Param("uredajIds") List<Integer> uredajIds);
}