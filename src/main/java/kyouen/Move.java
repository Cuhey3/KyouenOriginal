package kyouen;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;

@SuppressWarnings("serial")
public class Move extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		DatastoreService datastore = null;
		Entity past_string = null;
		try {
			datastore = DatastoreServiceFactory.getDatastoreService();
			past_string = datastore
					.get(KeyFactory.createKey("past_string", "onlyOne"));
		} catch (EntityNotFoundException e) {
		}
/*		URL url = new URL("http://7.seiyukyouen.appspot.com/ranking?mokyu=true");
		HTTPResponse http_response = URLFetchServiceFactory
				.getURLFetchService().fetch(url);
		String content = new String(http_response.getContent(), "utf-8");
		past_string.setProperty("rank", new Text(content.split("ここから")[0]));
		past_string.setProperty("rank_update", content.split("ここから")[1]);
		
		url = new URL("http://7.seiyukyouen.appspot.com/pair?mokyu=true");
		http_response = URLFetchServiceFactory
				.getURLFetchService().fetch(url);
		content = new String(http_response.getContent(), "utf-8");
		past_string.setProperty("pair", new Text(content.split("ここから")[0]));
		past_string.setProperty("pair_update", content.split("ここから")[1]);*/
		past_string.setProperty("past_update" ,"");
		datastore.put(past_string);
	}
}
