package dev.bitcoin.bitcointalk.model;

import java.util.Date;

public class Post {

	public String poster;
	public String content;
	public Date date;
	
	public Post(String poster, String content) {
		this.poster = poster;
		this.content = content;
	}

	@Override
	public String toString() {
		return "Post [poster=" + poster + ", content=" + content + "]";
	}
	
}
