package services.tmitocar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import services.tmitocar.model.WritingTask;

public interface WritingTaskRepository extends JpaRepository<WritingTask, Integer>{

    @Query(value = "SELECT wt FROM WritingTask wt WHERE wt.courseid = :courseid ORDER BY courseid ASC, nr ASC")
    List<WritingTask> findTasksByCourseId(@Param("courseid") Integer courseid);

    @Query(value = "SELECT wt FROM WritingTask wt WHERE wt.courseid = :courseid AND wt.nr = :nr")
    WritingTask findByCourseIdAndNr(@Param("courseid") Integer courseid, @Param("nr") Integer nr);
}
