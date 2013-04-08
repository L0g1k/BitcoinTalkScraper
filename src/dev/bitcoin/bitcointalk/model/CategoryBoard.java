package dev.bitcoin.bitcointalk.model;

import java.util.ArrayList;
import java.util.List;

public class CategoryBoard implements HasCategoryBoards {

	public final String boardName;
	public List<CategoryBoard> childBoards = new ArrayList<CategoryBoard>();
	private List<Topic> firstPageTopics = new ArrayList<Topic>();
	public final String boardId;
	
	public CategoryBoard(String boardName, String boardId) {
		this.boardName = boardName;
		// TODO Auto-generated constructor stub
		this.boardId = boardId;
	}

	public void addBoard(CategoryBoard childBoard) {
		this.childBoards.add(childBoard);
	}
	@Override
	public String toString() {
		return "CategoryBoard [boardName=" + boardName + "]";
	}

	public void addTopic(Topic topic) {
		firstPageTopics.add(topic);
	}

}
