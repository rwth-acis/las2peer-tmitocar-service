package services.tmitocar.model;

public class TmitocarFiles {

    private String filename;
    private byte[] file;

    public TmitocarFiles() {
    }

    public TmitocarFiles(String filename, byte[] file) {
        this.filename = filename;
        this.file = file;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getFile() {
        return this.file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

}
