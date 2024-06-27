package services.tmitocar.model;
import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
public class Course {
    @Id
    private Integer id;
    private Integer basecourseid;
    private String surname;
    private Timestamp validfrom;
    private Timestamp validuntil;
    private String shortname;

    public Integer getId() {
        return id;
    }

    public Integer getBaseCourseId() {
        return basecourseid;
    }

    public String getSurname() {
        return surname;
    }

    public Timestamp getValidFrom() {
        return validfrom;
    }

    public Timestamp getValidUntil() {
        return validuntil;
    }

    public String getShortName() {
        return shortname;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setBaseCourseId(Integer basecourseid) {
        this.basecourseid = basecourseid;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setValidFrom(Timestamp validfrom) {
        this.validfrom = validfrom;
    }

    public void setValidUntil(Timestamp validuntil) {
        this.validuntil = validuntil;
    }

    public void setShortName(String shortname) {
        this.shortname = shortname;
    }
    
}
