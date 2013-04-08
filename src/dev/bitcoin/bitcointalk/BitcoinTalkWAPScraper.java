package dev.bitcoin.bitcointalk;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.MasonTagTypes;
import net.htmlparser.jericho.MicrosoftConditionalCommentTagTypes;
import net.htmlparser.jericho.PHPTagTypes;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import dev.bitcoin.bitcointalk.model.Category;
import dev.bitcoin.bitcointalk.model.CategoryBoard;
import dev.bitcoin.bitcointalk.model.HasCategoryBoards;
import dev.bitcoin.bitcointalk.model.Post;
import dev.bitcoin.bitcointalk.model.Topic;

public class BitcoinTalkWAPScraper {
	enum LinkType { TOPIC, CHILD, OTHER }
	private static String base = "https://bitcointalk.org/index.php";
	private static String bitcoinTalk = base+"?wap2";
	private static int maxNesting = 1;
	// Sanity tests to avoid accidentally recursing forever, or accidentally downloading the same content over and over.
	private static int currentNesting = 0;
	private static List<String> boardsDownloaded = new ArrayList<String>();
	
	private Source source; 
	static boolean childBoardDebug = false;
	static String childBoardDebugString = "";
	
	protected Category addCategory(List<Category> categories, Element element) {
		Category currentCategory;
		final String title = element.getContent().toString();
		Category category = new Category(title);
		currentCategory = category;
		categories.add(category);
		return currentCategory;
	}

	protected void addCategoryBoard(Category currentCategory, Element element) {
		Element boardTitleElement = element.getFirstElement("a");
		addChildBoard(boardTitleElement, currentCategory, true);
	}

	private void addChildBoard(Element linkElement, HasCategoryBoards parent, boolean searchForChildren) {
		final String title = linkElement.getContent().toString();
		final Map<String, String> queryMap = getQueryMap(linkElement);
		String boardId = parseId(queryMap.get("board"));
		if(boardId != null) {
			CategoryBoard categoryBoard = new CategoryBoard(title, boardId);
			
			if(searchForChildren && !boardsDownloaded.contains(boardId)){
				try {
					getChildBoards(boardId, categoryBoard);
				} finally {
					boardsDownloaded.add(boardId);
					if(childBoardDebug && categoryBoard.childBoards !=null && !categoryBoard.childBoards.isEmpty()) {
						List<CategoryBoard> childBoards = categoryBoard.childBoards;
						childBoardDebugString += boardId + ": \n [";
						for (CategoryBoard categoryBoard2 : childBoards) {
							childBoardDebugString += "['" + categoryBoard2.boardName + "'," + categoryBoard2.boardId + "],";
							
						}
						childBoardDebugString = childBoardDebugString.substring(0, childBoardDebugString.length() - 1);
						childBoardDebugString += "],\n";
					}
				}
			}
			parent.addBoard(categoryBoard);
		}
	}
	
	protected void addPost(final List<Post> posts, Element element) {
		try {
			final String className = element.getAttributeValue("class");
			if(className.contains("windowbg")) {
				String content = element.getContent().toString();
				final boolean looksLikeAPost = content.contains("accesskey") == false;
				if(looksLikeAPost) {
					String poster = element.getFirstElement("b").getContent().toString();
					String trimmedContent = content.substring(content.indexOf("<br />") + 6).trim();
					posts.add(new Post(poster,trimmedContent));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void addTopic(final List<Topic> topics, Element linkElement, String boardId) {
		try {
			switch(getLinkType(linkElement, boardId)){
				case TOPIC: 
					final String title = linkElement.getContent().toString();
					final Map<String, String> queryMap = getQueryMap(linkElement);
					String topicId = parseId(queryMap.get("topic"));
					topics.add(new Topic(title, topicId));
					log("Added topic " + title);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void getChildBoards(String boardId, CategoryBoard categoryBoard) {
		log("Downloading child boards of board " + boardId);
		final Source source = getSourceOrFail(generateBoardLink(boardId));
		final List<Element> allLinks = source.getAllElements("a");
		for (Element linkElement : allLinks) {
			switch(getLinkType(linkElement, boardId)){
				case CHILD: 
					if(currentNesting < maxNesting) {
						currentNesting++; 
						addChildBoard(linkElement, categoryBoard, false); 
						currentNesting=0; 
						break;
					}
			}
		}
	}
	
	private String generateBoardLink(String boardId) {
		return base + "?board=" + boardId + ";wap2";
	}
	private String generateTopicLink(String topicId) {
		return base + "?topic=" + topicId + ";wap2";
	}

	public List<Category> getCategories() {
		
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		List<Category> categories = new ArrayList<Category>();
		Category currentCategory = null;
		
		final List<Element> paragraphs = source.getAllElements("p");
		for (Element element : paragraphs) {
			final String className = element.getAttributeValue("class");
			if(className.contains("titlebg")) {
				currentCategory = addCategory(categories, element);
			} else if(className.contains("windowbg")) {
				addCategoryBoard(currentCategory, element);
			}
		}			
		
		return categories;
	}
	
	private LinkType getLinkType(Element linkElement, String boardId) {
		final Map<String, String> queryMap = getQueryMap(linkElement);
		boolean paginationLink = queryMap.containsKey("board") && queryMap.get("board").startsWith(boardId + ".");
		if(queryMap.containsKey("board") && !paginationLink)
			return LinkType.CHILD;
		else if (queryMap.containsKey("topic")) {
			return LinkType.TOPIC;
		}
		return LinkType.OTHER;
	}
	
	/**
	 * Get the total number of pages for the topic. The WAP version of the board has 5 posts per page.
	 * 
	 * @param topicId
	 * @return
	 */
	public int getPages(String topicId) {
		log("Downloading pages of topic " + topicId);
		final Source source = getSourceOrFail(generateTopicLink(topicId));
		final Element pageLinkContainer = source.getFirstElementByClass("windowbg");
		final TextExtractor textExtractor = pageLinkContainer.getTextExtractor();
		final String string = textExtractor.toString();
		int start = string.indexOf("/") + 1;
		int end = string.indexOf(")");
		String number = string.substring(start, end);
		return Integer.parseInt(number);
	}
	
	public List<Post> getPosts(String topicId) {
		log("Downloading posts of topic " + topicId);
		final List<Post> posts = new ArrayList<Post>();
		final Source source = getSourceOrFail(generateTopicLink(topicId));
		final List<Element> allElements = source.getAllElements("p");
		final List<Element> paragraphs = allElements.subList(2, allElements.size()-1);
		for (Element element : paragraphs) {
			addPost(posts, element);
		}	
		return posts;
	}

	private Map<String, String> getQueryMap(Element linkElement) {
		final String href = linkElement.getAttributeValue("href");
		URL url = null;
		try {
			url = new URL(href);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		final String query = url.getQuery();
		final Map<String, String> queryMap = getQueryMap(query);
		return queryMap;
	}

	private Map<String, String> getQueryMap(String query)  
	{  
	    String[] params = query.split("&");  
	    Map<String, String> map = new HashMap<String, String>();  
	    for (String param : params)  
	    {  
	    	
	        final String[] split = param.split("=");
	        if(split.length > 1) {
				String name = split[0];  
		        String value = split[1];  
		        map.put(name, value);  
	        }
	    }  
	    return map;  
	}
	
	protected Source getSourceOrFail(String link) {
		Source source;
		try {
			URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(60*1000);
			source = new Source(connection.getInputStream());
			source.fullSequentialParse();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't download contents for " + link, e);
		}
		return source;
	}

	public List<Topic> getTopics(String boardId) {
		final int numberOfWAPPages = 4;
		log("Downloading topics of board " + boardId);
		final List<Topic> topics = new ArrayList<Topic>();
		final List<Element> allLinks = new ArrayList<Element>();
		for (int i = 0; i < numberOfWAPPages; i++) {
			final Source source = getSourceOrFail(generateBoardLink(boardId + "." + i*9));
			final List<Element> links = source.getAllElements("a");
			allLinks.addAll(links);
		}
		for (Element linkElement : allLinks) {
			addTopic(topics, linkElement, boardId);
		}
		return topics;
		
	}

	public void init() throws MalformedURLException, IOException {
		String sourceUrlString=bitcoinTalk;
		MicrosoftConditionalCommentTagTypes.register();
		PHPTagTypes.register();
		PHPTagTypes.PHP_SHORT.deregister(); // remove PHP short tags for this example otherwise they override processing instructions
		MasonTagTypes.register();
		source = new Source(new URL(sourceUrlString));

		// Call fullSequentialParse manually as most of the source will be parsed.
		source.fullSequentialParse();
	}
	
	protected void log(final Object string) {
		System.out.println(string);
	}
	
	String parseId(String input) {
		if(input == null) return null;
		
		if(input.contains(".")) {
			input = input.substring(0, input.indexOf("."));
		}
		return input;
	}

	

	

	 

}
