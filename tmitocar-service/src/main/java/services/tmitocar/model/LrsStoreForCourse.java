package services.tmitocar.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lrsstoreforcourse")
public class LrsStoreForCourse {
    @Id
    private Integer id;
    private String clientkey;
    private String clientsecret;

    public Integer getId() {
        return id;
    }

    public String getClientkey() {
        return clientkey;
    }

    public String getClientsecret() {
        return clientsecret;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setClientkey(String clientkey) {
        this.clientkey = clientkey;
    }

    public void setClientsecret(String clientsecret) {
        this.clientsecret = clientsecret;
    }
}
