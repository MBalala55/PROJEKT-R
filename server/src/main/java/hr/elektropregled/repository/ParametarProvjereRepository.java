package hr.elektropregled.repository;

import hr.elektropregled.model.ParametarProvjere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametarProvjereRepository extends JpaRepository<ParametarProvjere, Integer> {
	java.util.List<ParametarProvjere> findByVrstaUredaja_IdVrUredOrderByRedoslijedAsc(Integer idVrUred);
}