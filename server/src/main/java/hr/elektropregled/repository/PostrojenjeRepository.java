package hr.elektropregled.repository;

import hr.elektropregled.model.Postrojenje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostrojenjeRepository extends JpaRepository<Postrojenje, Integer> {
}