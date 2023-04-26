package i5.las2peer.services.tmitocar.pojo;

/**
 * This class represents the response returned by our TMitocar Wrapper service after processing a request.
 * We upload the files to our mongodb instance and return the file IDs.
 * The response contains file IDs for the uploaded text, feedback, and graph files.
 */
public class TmitocarResponse {
    private String uploadFileId;
    private String feedbackFileId;
    private String graphFileId;

    /**
     * Constructs a TmitocarResponse object with the specified upload file ID.
     *
     * @param uploadFileId the ID of the uploaded text file
     */
    public TmitocarResponse(String uploadFileId){
        this(uploadFileId, null);
    }

    /**
     * Constructs a TmitocarResponse object with the specified upload file and feedback file IDs.
     *
     * @param uploadFileId the ID of the uploaded text file
     * @param feedbackFileId the ID of the feedback file
     */
    public TmitocarResponse(String uploadFileId, String feedbackFileId){
        this(uploadFileId,feedbackFileId,null);
    }

    /**
     * Constructs a TmitocarResponse object with the specified upload file, feedback file, and graph file IDs.
     *
     * @param uploadFileId the ID of the uploaded text file
     * @param feedbackFileId the ID of the feedback file
     * @param graphFileId the ID of the graph file
     */
    public TmitocarResponse(String uploadFileId, String feedbackFileId, String graphFileId){
        this.uploadFileId = uploadFileId;
        this.feedbackFileId = feedbackFileId;
        this.graphFileId = graphFileId;
    }

    /**
     * Returns the ID of the uploaded text file.
     *
     * @return the upload file ID
     */
    public String getUploadFileId() {
        return uploadFileId;
    }

    /**
     * Sets the ID of the uploaded text file.
     *
     * @param uploadFileId the upload file ID to set
     */
    public void setUploadFileId(String uploadFileId) {
        this.uploadFileId = uploadFileId;
    }

    /**
     * Returns the ID of the feedback file.
     *
     * @return the feedback file ID
     */
    public String getFeedbackFileId() {
        return feedbackFileId;
    }

    /**
     * Sets the ID of the feedback file.
     *
     * @param feedbackFileId the feedback file ID to set
     */
    public void setFeedbackFileId(String feedbackFileId) {
        this.feedbackFileId = feedbackFileId;
    }

    /**
     * Returns the ID of the graph file.
     *
     * @return the graph file ID
     */
    public String getGraphFileId() {
        return graphFileId;
    }

    /**
     * Sets the ID of the graph file.
     *
     * @param graphFileId the graph file ID to set
     */
    public void setGraphFileId(String graphFileId) {
        this.graphFileId = graphFileId;
    } 
}