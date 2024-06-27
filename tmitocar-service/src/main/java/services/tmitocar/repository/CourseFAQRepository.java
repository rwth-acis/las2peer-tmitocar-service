package services.tmitocar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import services.tmitocar.model.CourseFAQ;

public interface CourseFAQRepository extends JpaRepository<CourseFAQ, Integer>{

    @Query(value = "SELECT cfaq FROM CourseFAQ cfaq WHERE cfaq.courseid = :courseid")
    List<CourseFAQ> findFAQsByCourseId(@Param("courseid") Integer courseid);

    @Query(value = "SELECT cfaq FROM CourseFAQ cfaq WHERE cfaq.courseid = :courseid AND cfaq.intent = :intent")
    List<CourseFAQ> findByCourseIdAndIntent(@Param("courseid") Integer courseid, @Param("intent") String intent);

    @Query(value = "SELECT cfaq FROM CourseFAQ cfaq WHERE cfaq.intent = :intent")
    List<CourseFAQ> findByIntent(@Param("intent") String intent);
}
