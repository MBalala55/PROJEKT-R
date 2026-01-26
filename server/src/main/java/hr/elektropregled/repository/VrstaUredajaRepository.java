package hr.elektropregled.repository;

import hr.elektropregled.model.VrstaUredaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VrstaUredajaRepository extends JpaRepository<VrstaUredaja, Integer> {
    Optional<VrstaUredaja> findByOznVrUred(String oznVrUred);
    Optional<VrstaUredaja> findByNazVrUred(String nazVrUred);
}