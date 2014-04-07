package kyouen;

import static kyouen.WakeBackends.namelist;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;

import net.sf.jsr107cache.Cache;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.urlfetch.FetchOptions.Builder;

@SuppressWarnings("serial")
	public class DataModel extends HttpServlet {
	int shift = 0;
	String wikiName, inputName;
	String cursive_syllabary = "ん";
	String mf = "100";
	String record_str = "";
	String content_str = "";
	String[] content[];
	String[][] record;
	String textarea;
	boolean registered = false;
	Long date;

	public DataModel(String s) {
		inputName = (s.trim()).split("[ 　_＿]")[0];
		Pattern p_inputName = Pattern.compile(inputName
				+ ",([^,]*),(-1|1),([^,0-9]+)");
		Matcher m = p_inputName.matcher(namelist);
		if (m.find()) {
			registered = true;
			if (m.group(1).equals("&i=1")) {
				shift = 1;
				wikiName = inputName;
			} else
				wikiName = inputName + m.group(1);
			mf = m.group(2);
			cursive_syllabary = m.group(3);
		} else
			wikiName = inputName;
	}

	public void setTextarea() throws IOException {
		URL url = new URL(
				"http://ja.wikipedia.org/w/index.php?action=edit&title="
						+ wikiName);
		HTTPRequest http_request = new HTTPRequest(url, HTTPMethod.GET, Builder
				.disallowTruncate().setDeadline(15.0));
		HTTPResponse http_response = URLFetchServiceFactory
				.getURLFetchService().fetch(http_request);
		textarea = new String(http_response.getContent(), "utf-8");
		textarea = textarea.replaceAll("\n", "<br />");
		textarea = textarea.replaceAll("^.*?" + "<textarea[^>/]*>" + "(.*?)"
				+ "</textarea>.*$", "$1");
		textarea = textarea.replaceAll("(<br />=+)" + "([^= ])", "$1 $2");
		textarea = textarea.replaceAll("(<br />\\*+)" + "([^\\* ])", "$1 $2");
		textarea = textarea.replaceAll("&lt;", "<");
		textarea = textarea.replaceAll("(" + "<!--.*?-->" + "|"
				+ "(<ref[^>/]*)" + "(/>" + "|" + ">.*?</ref>" + ")" + ")", "");
	}

	public void setRecord() throws IOException {
		record_str = strageAccess(DataName.record);
		if (record_str.isEmpty())
			setRecord(true);
	}

	public void setContent() throws IOException {
		content_str = strageAccess(DataName.content);
		if (content_str.isEmpty())
			setContent(true);
	}

	public void setAll() throws IOException {
		setRecord();
		setContent();
	}

	public void setRecord(boolean force) throws IOException {
		String[][] record = new String[9][2000];
		String[] status = new String[9];
		setTextarea();
		multiEq(2+shift);
	}

	public void setContent(boolean force) throws IOException {
	}

	public void setAll(boolean force) throws IOException {
		setRecord(true);
		setContent(true);
	}

	public String strageAccess(DataName d) {
		Cache cache = WikiUtil.getCache(60 * 60 * 48);
		String data = (String) cache.get(inputName + "," + d.name());
		if (data == null) {
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Key key = KeyFactory.createKey("seiyu", inputName);
			Entity seiyu = null;
			try {
				seiyu = datastore.get(key);
			} catch (EntityNotFoundException e) {
				return "";
			}
			data = ((Text) seiyu.getProperty(d.name())).getValue();
		}
		return data;
	}

	public static enum DataName {
		record, content
	}

	public void multiEq(int mode) {
		
	}
}
