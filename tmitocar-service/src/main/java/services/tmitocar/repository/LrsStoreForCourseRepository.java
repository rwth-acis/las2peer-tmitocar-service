package services.tmitocar.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import services.tmitocar.model.LrsStoreForCourse;

public interface LrsStoreForCourseRepository extends JpaRepository<LrsStoreForCourse, Integer>{
    
    @Query(value = "SELECT lrs FROM LrsStoreForCourse lrs WHERE lrs.courseid = :courseid")
    LrsStoreForCourse findLrsByCourseId(@Param("courseid") Integer courseid);

}
