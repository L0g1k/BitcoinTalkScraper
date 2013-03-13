package dev.bitcoin.bitcointalk.model;

import java.util.ArrayList;
import java.util.List;



public class Category implements HasCategoryBoards {

	List<CategoryBoard> categoryBoards = new ArrayList<CategoryBoard>();
	
	public Category(String title) {
		this.title = title;
	}

	public final String title;

	public void addBoard(CategoryBoard categoryBoard) {
		categoryBoards.add(categoryBoard);
	}
	
	public List<CategoryBoard> getBoards() {
		return categoryBoards;
	}

	@Override
	public String toString() {
		return "Category title=" + title + "[categoryBoards=" + categoryBoards +"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Category other = (Category) obj;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
	
}
