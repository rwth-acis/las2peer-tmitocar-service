package services.tmitocar.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "personmapping")
public class Personmapping {
    @Id
    private UUID uuid;
    private Integer personid;

    public UUID getUuid() {
        return uuid;
    }

    public Integer getPersonId() {
        return personid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setPersonId(Integer personid) {
        this.personid = personid;
    }
}
