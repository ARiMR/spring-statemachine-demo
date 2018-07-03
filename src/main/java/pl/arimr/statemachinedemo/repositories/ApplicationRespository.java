package pl.arimr.statemachinedemo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.arimr.statemachinedemo.domain.Application;

@Repository
public interface ApplicationRespository extends JpaRepository<Application, Long> {
}
