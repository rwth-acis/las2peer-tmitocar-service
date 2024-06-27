package services.tmitocar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import services.tmitocar.model.Person;

public interface PersonRepository extends JpaRepository<Person, Integer>{
    
    @Query(value = "SELECT p FROM Person p WHERE p.email = :email")
    Person findByEmail(@Param("email") String email);

}
