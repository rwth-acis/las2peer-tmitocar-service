package services.tmitocar.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import services.tmitocar.model.Personmapping;

public interface PersonMappingRepository extends JpaRepository<Personmapping, UUID>{

    @Query(value = "SELECT pm FROM Personmapping pm WHERE pm.personid = :personid")
    Personmapping findByPersonId(@Param("personid") Integer personid);
    
}
