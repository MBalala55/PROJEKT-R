package hr.elektropregled.repository;

import hr.elektropregled.model.Polje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PoljeRepository extends JpaRepository<Polje, Integer> {
    interface PoljeWithCount {
        Integer getIdPolje();
        String getNazPolje();
        Double getNapRazina();
        String getOznVrPolje();
        Long getBrojUredaja();
    }
    
    @Query("""
        SELECT p.idPolje as idPolje, p.nazPolje as nazPolje, p.napRazina as napRazina,
               p.oznVrPolje as oznVrPolje, COUNT(u.idUred) as brojUredaja
        FROM Polje p
        LEFT JOIN Uredaj u ON p.idPolje = u.polje.idPolje
        WHERE p.postrojenje.idPostr = :idPostr
        GROUP BY p.idPolje, p.nazPolje, p.napRazina, p.oznVrPolje
        ORDER BY p.napRazina DESC, p.nazPolje
        """)
    java.util.List<PoljeWithCount> findByPostrojenjeWithCount(@Param("idPostr") Integer idPostr);
}