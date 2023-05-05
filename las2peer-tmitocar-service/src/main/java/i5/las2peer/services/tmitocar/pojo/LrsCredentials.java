package i5.las2peer.services.tmitocar.pojo;

public class LrsCredentials {
    private String clientKey;
    private String clientSecret;

    public LrsCredentials(String key, String secret){
        this.clientKey = key;
        this.clientSecret = secret;
    }

    public String getClientKey() {
        return clientKey;
    }
    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
    public String getClientSecret() {
        return clientSecret;
    }
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
