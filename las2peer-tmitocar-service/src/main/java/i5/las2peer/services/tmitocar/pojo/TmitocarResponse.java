package i5.las2peer.services.tmitocar.pojo;

public class TmitocarResponse {
	private String uploadFileId;
	private String feedbackFileId;
	private String graphFileId;

	public TmitocarResponse(String uploadFileId){
		this(uploadFileId, null);
	}
	
	public TmitocarResponse(String uploadFileId, String feedbackFileId){
		this(uploadFileId,feedbackFileId,null);
	}

	public TmitocarResponse(String uploadFileId, String feedbackFileId, String graphFileId){
		this.uploadFileId = uploadFileId;
		this.feedbackFileId = feedbackFileId;
		this.graphFileId = graphFileId;
	}

	public String getUploadFileId() {
		return uploadFileId;
	}
	public void setUploadFileId(String uploadFileId) {
		this.uploadFileId = uploadFileId;
	}
	public String getFeedbackFileId() {
		return feedbackFileId;
	}
	public void setFeedbackFileId(String feedbackFileId) {
		this.feedbackFileId = feedbackFileId;
	}
	public String getGraphFileId() {
		return graphFileId;
	}
	public void setGraphFileId(String graphFileId) {
		this.graphFileId = graphFileId;
	} 
}
