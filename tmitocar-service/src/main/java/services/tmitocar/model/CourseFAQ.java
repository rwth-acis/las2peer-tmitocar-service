package services.tmitocar.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "coursefaq")
public class CourseFAQ {
    @Id
    private Integer courseid;
    private String answer;
    private String intent;

    public Integer getCourseId() {
        return courseid;
    }

    public String getAnswer() {
        return answer;
    }

    public String getIntent() {
        return intent;
    }

    public void setCourseId(Integer courseid) {
        this.courseid = courseid;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
