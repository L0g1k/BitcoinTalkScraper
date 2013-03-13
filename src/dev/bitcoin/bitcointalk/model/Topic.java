package dev.bitcoin.bitcointalk.model;

public class Topic {

	public String title;
	public String topicId;
	
	public Topic(String title, String topicId) {
		this.title = title;
		this.topicId = topicId;
	}

	@Override
	public String toString() {
		return "Topic [title=" + title + ", topicId=" + topicId + "]";
	}
	
}
