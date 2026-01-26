package hr.elektropregled.repository;

import hr.elektropregled.model.Uredaj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UredajRepository extends JpaRepository<Uredaj, Integer> {
    java.util.List<Uredaj> findByPostrojenje_IdPostrOrderByIdUredAsc(Integer idPostr);
    
    java.util.List<Uredaj> findByPostrojenje_IdPostrAndPolje_IdPoljeOrderByIdUredAsc(Integer idPostr, Integer idPolje);
    
    java.util.List<Uredaj> findByPostrojenje_IdPostrAndPoljeIsNullOrderByIdUredAsc(Integer idPostr);
    
    @Query("SELECT COUNT(u) FROM Uredaj u WHERE u.postrojenje.idPostr = :idPostr AND u.polje IS NULL")
    Long countByPostrojenjeAndPoljeIsNull(@Param("idPostr") Integer idPostr);
}