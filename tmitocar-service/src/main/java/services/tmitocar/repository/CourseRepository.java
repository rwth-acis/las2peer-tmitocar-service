package services.tmitocar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import services.tmitocar.model.Course;

public interface CourseRepository extends JpaRepository<Course, Integer>{
    
    @Query(value = "SELECT c FROM Course c WHERE c.id = :id")
    Course findCourseById(@Param("id") Integer id);
    
}
