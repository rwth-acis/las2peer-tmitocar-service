package i5.las2peer.services.tmitocar.pojo;

/**
 * This class represents metadata that is used for T-MITOCAR, such as the text's
 * type, topic,
 * template, and word specifications.
 */
public class TmitocarText {
	private String text;
	private String type;
	private String topic;
	private String template;
	private String wordSpec;

	/**
	 * Returns the text represented by this object.
	 *
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the text to be represented by this object.
	 *
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Returns the type of the text represented by this object.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of the text to be represented by this object.
	 *
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the topic of the text represented by this object.
	 *
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Sets the topic of the text to be represented by this object.
	 *
	 * @param topic the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * Returns the template's filename used to generate the text represented by this object.
	 *
	 * @return filename of the template for the feedback document
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * Sets the template's filename that is used to generate the feedback document.
	 *
	 * @param template filename of the template for the feedback document
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Returns the word specifications attribute
	 *
	 * @return the word specifications
	 */
	public String getWordSpec() {
		return wordSpec;
	}

	/**
	 * Sets the word specifications attribute.
	 *
	 * @param wordSpec the word specifications to set
	 */
	public void setWordSpec(String wordSpec) {
		this.wordSpec = wordSpec;
	}
}