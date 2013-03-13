package dev.bitcoin.bitcointalk;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import dev.bitcoin.bitcointalk.model.Category;
import dev.bitcoin.bitcointalk.model.Post;
import dev.bitcoin.bitcointalk.model.Topic;


public class BitcoinTalkAPITest {

	private BitcoinTalkWAPScraper bitcoinTalk;

	@Before
	public void setUp() throws MalformedURLException, IOException {
		bitcoinTalk = new BitcoinTalkWAPScraper();
		bitcoinTalk.init();
	}
	
	@Test
	public void testParseId() {
		assertTrue(bitcoinTalk.parseId("76369.0;wap2").equals("76369"));
	}
	
	@Test
	public void testQueryCategories() {
		final List<Category> categories = bitcoinTalk.getCategories(false);
		assertTrue(categories.size() == 4);
		assertTrue(categories.contains(new Category("Bitcoin")));
	}
	
	@Test
	public void testQueryBoards() {
		final List<Topic> topics = bitcoinTalk.getTopics("1.0");
		log(topics);
		assertTrue(topics.size() > 0);
	}
	
	@Test
	public void testQueryPages() {
		final int pages = bitcoinTalk.getPages("85687.0");
		assertTrue(pages > 2658);
	}
	
	@Test
	public void testQueryPosts() {
		final List<Post> posts = bitcoinTalk.getPosts("37069.0");
		log(posts);
		assertTrue(posts.size() > 0);
	}
	
	
	
	private void log(Object log) {
		System.out.println(log);
	}
}
