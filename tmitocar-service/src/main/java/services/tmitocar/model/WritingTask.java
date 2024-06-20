package services.tmitocar.model;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

@Entity
@Table(name = "writingtask")
public class WritingTask {
    
    private Integer courseid;
    @Id
    private Integer nr;
    private String text;
    private String title;
    private Timestamp validfrom;
    private Timestamp validuntil;

    public Integer getCourseId() {
        return courseid;
    }

    public Integer getNr() {
        return nr;
    }

    public String getText(){
        return text;
    }

    public String getTitle(){
        return title;
    }

    public Timestamp getValidFrom(){
        return validfrom;
    }

    public Timestamp getValidUntil(){
        return validuntil;
    }

    public void setCourseId(Integer courseid) {
        this.courseid = courseid;
    }

    public void setNr(Integer nr) {
        this.nr = nr;
    }

    public void setText(String text){
        this.text = text;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setValidFrom(Timestamp validfrom){
        this.validfrom = validfrom;
    }
    
    public void setValidUntil(Timestamp validuntil){
        this.validuntil = validuntil;
    }
}
