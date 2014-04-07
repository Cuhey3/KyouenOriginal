package kyouen;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.IllegalDataException;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.InvalidValueException;

import net.sf.jsr107cache.Cache;

@SuppressWarnings("serial")
public class Assistant extends HttpServlet {

	SearchResult searchResult;
	HumanProp humanProp;
	Cache cache;
	DatastoreService datastore;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");

		setSearchResult();
		extendedLinkedHashMap<String, String> elhm = this.searchResult
				.getRootMap();
		resp.getWriter()
				.println(
						"<h2>あいまい検索された単語と、検索結果の対応表</h2><table border=\"1\" cellspacing=\"0\" ><tr><th>検索語</th><th>検索結果</th></tr>");
		for (String key : elhm.keySet()) {
			resp.getWriter().println(
					"<tr><td><a href=\"/co?n=" + key + "\">" + key
							+ "</a></td><td>" + elhm.get(key) + "</td></tr>");
		}

		if (req.getParameter("hp") != null) {
			resp.getWriter().println("</table><a href=\"/\">戻る</a>");
			makePersistentSearchResult();
			setHumanProp();
			makePersistentHumanProp();
			LinkedHashMap<String, LinkedHashMap<String, String>> rootMap = humanProp
					.getRootMap();
			resp.getWriter()
					.println(
							"<table border=\"1\" cellspacing=\"0\"><tr><th>名前</th><th>テンプレート</th><th>性別</th></tr>");
			for (String name : rootMap.keySet()) {
				if (!rootMap.containsKey(name) || rootMap.get(name) == null) {
					resp.getWriter().println(name + "にはエラーがあります");
					continue;
				}
				resp.getWriter().println(
						"<tr><td><a href=\"http://ja.wikipedia.org/wiki/"
								+ name + "\">" + name + "</a></td>");
				if (rootMap.get(name).containsKey("t"))
					resp.getWriter().println(
							"<td>" + rootMap.get(name).get("t") + "</td>");
				else
					resp.getWriter().println("<td></td>");
				if (rootMap.get(name).containsKey("g"))
					resp.getWriter().println(
							"<td>" + rootMap.get(name).get("g") + "</td>");
				else
					resp.getWriter().println("<td></td>");
				resp.getWriter().println("</tr>");
			}
			resp.getWriter().println("</table>");
		}
	}

	public Assistant() {
		cache = WikiUtil.getCache(60 * 60 * 24 * 365);
	}

	protected void startDatastore() {
		if (this.datastore == null)
			this.datastore = DatastoreServiceFactory.getDatastoreService();
	}

	protected Entity getEntity(String idname) {
		Key key = KeyFactory.createKey("assistant", idname);
		Entity entity = null;
		try {
			entity = datastore.get(key);
		} catch (EntityNotFoundException e) {
		}
		return entity;
	}

	// search を使って result を得る
	// 基本はこのメソッドしか使わない
	public String getSearchResultWord(String search) throws IOException {
		if (search.isEmpty())
			return null;
		if (this.searchResult == null)
			setSearchResult();
		String result = searchResult.getResult(search);

		if (result == null) {
			try {
				// あいまい検索
				result = WikiUtil.getAimaiSearchResult(search);
			} catch (IOException e) {
			}
			// あいまい検索でもダメならIOException
			if (result == null)
				throw new IOException();
		}

		if (!search.equals(result))
			updateSearchResult(search, result);
		return result;
	}

	// キャッシュ から searchResult を set、
	// それができなかったらデータストアからsearchResult を読み出して setするクラス
	protected void setSearchResult() {
		// キャッシュ読み取り
		try {
			this.searchResult = (SearchResult) cache.get("searchResult");
			if (searchResult == null)
				throw new InvalidValueException(null);
		} catch (InvalidValueException ive) {
			if (searchResult == null) {
				// データストア読み取り
				startDatastore();
				Entity searchResultEntity = getEntity("searchResult");
				Element element = null;
				// xml読み出し
				String searchResultString = ((Text) searchResultEntity
						.getProperty("xml")).getValue();
				try {
					element = new SAXBuilder().build(
							new StringReader(searchResultString))
							.getRootElement();
				} catch (JDOMException e) {
				} catch (IOException e) {
				}
				// xmlString から extendedLinkedHashMap へ変換
				extendedLinkedHashMap<String, String> elhm = new extendedLinkedHashMap<String, String>();
				Iterator<Element> itr = element.getChildren("s").iterator();
				while (itr.hasNext()) {
					Element e = (Element) itr.next();
					elhm.put(e.getAttributeValue("search"),
							e.getAttributeValue("result"));
				}
				// SearchResultインスタンス生成、cacheへプット
				SearchResult searchResult = new SearchResult(elhm);
				cache.put("searchResult", searchResult);
				this.searchResult = searchResult;
			}
		}
	}

	public void updateSearchResult(String search, String result) {
		extendedLinkedHashMap<String, String> elhm = searchResult.getRootMap();
		elhm.remove(search);
		elhm.put(search, result);
		SearchResult sr = new SearchResult(elhm);
		cache.put("searchResult", sr);
	}

	public void removeSearchResult(String search) {
		extendedLinkedHashMap<String, String> elhm = searchResult.getRootMap();
		elhm.remove(search);
		SearchResult sr = new SearchResult(elhm);
		cache.put("searchResult", sr);
	}

	// 具体的な prop値を得る
	public String getHumanPropValue(String name, String prop)
			throws IOException {
		if (name.isEmpty())
			return null;
		if (humanProp == null)
			setHumanProp();
		if (humanProp == null)
			throw new NullPointerException();
		LinkedHashMap<String, String> human = humanProp.getHuman(name);
		if (human == null)
			return null;
		return human.get(prop);
	}

	// prop をマップごと返す
	public LinkedHashMap<String, String> getHuman(String name) {
		if (name.isEmpty())
			return null;
		if (humanProp == null)
			setHumanProp();
		LinkedHashMap<String, String> human = humanProp.getHuman(name);
		return human;
	}

	protected void setHumanProp() {
		// キャッシュ読み取り
		try {
			humanProp = (HumanProp) cache.get("humanProp");
			if (humanProp == null)
				throw new InvalidValueException(null);
		} catch (InvalidValueException ive) {
			if (humanProp == null) {
				// データストア読み取り
				startDatastore();
				Entity humanPropEntity = getEntity("humanProp");
				Element element = null;
				// xml読み出し
				String humanPropString = ((Text) humanPropEntity
						.getProperty("xml")).getValue();
				try {
					element = new SAXBuilder().build(
							new StringReader(humanPropString)).getRootElement();
				} catch (JDOMException e) {
				} catch (IOException e) {
				}
				// xmlString から LinkedHashMap へ変換
				LinkedHashMap<String, LinkedHashMap<String, String>> lhm = new LinkedHashMap<String, LinkedHashMap<String, String>>();
				Iterator<Element> itr = element.getChildren("h").iterator();
				while (itr.hasNext()) {
					Element e = (Element) itr.next();
					LinkedHashMap<String, String> human = new LinkedHashMap<String, String>();
					for (Attribute att : e.getAttributes()) {
						human.put(att.getName(), att.getValue());
					}
					lhm.put(e.getText(), human);
				}
				// HumanPropインスタンス生成、cacheへプット
				HumanProp humanProp = new HumanProp(lhm);
				cache.put("humanProp", humanProp);
				this.humanProp = humanProp;
			}
		}
	}

	protected void updateHumanProp(String name,
			LinkedHashMap<String, String> human) {
		LinkedHashMap<String, LinkedHashMap<String, String>> lhm = humanProp
				.getRootMap();
		lhm.put(name, human);
		HumanProp hp = new HumanProp(lhm);
		cache.put("humanProp", hp);
	}

	public void removeHumanProp(String name) {
		LinkedHashMap<String, LinkedHashMap<String, String>> lhm = humanProp
				.getRootMap();
		lhm.remove(name);
		HumanProp hp = new HumanProp(lhm);
		cache.put("humanProp", hp);
	}

	public void makePersistentSearchResult() {
		try {
			this.searchResult = (SearchResult) cache.get("searchResult");
			extendedLinkedHashMap<String, String> elhm = searchResult
					.getRootMap();
			Element root = new Element("root");
			for (String key : elhm.keySet()) {
				if (key == null || elhm.get(key) == null)
					continue;
				try {
					Element child = new Element("s");
					child.setAttribute(new Attribute("search", key));
					child.setAttribute(new Attribute("result", elhm.get(key)));
					root.addContent(child);
				} catch (IllegalDataException ide) {
					continue;
				}
			}
			XMLPersistence(root,"searchResult");
		} catch (InvalidValueException ive) {
		}
	}

	public void makePersistentHumanProp() {
		try {
			this.humanProp = (HumanProp) cache.get("humanProp");
			LinkedHashMap<String, LinkedHashMap<String, String>> lhm = humanProp
					.getRootMap();
			Element root = new Element("root");
			for (String key : lhm.keySet()) {
				if (key == null || lhm.get(key) == null
						|| lhm.get(key).get("t") == null)
					continue;
				Element child = new Element("h");
				child.setText(key);
				child.setAttribute(new Attribute("t", lhm.get(key).get("t")));
				if (lhm.get(key).get("g") != null)
					child.setAttribute(new Attribute("g", lhm.get(key).get("g")));
				root.addContent(child);
			}
			XMLPersistence(root,"humanProp");
		} catch (InvalidValueException ive) {
		}
	}
	protected void XMLPersistence(Element root,String propName){
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		String xmlString = outputter.outputString(document);
		startDatastore();
		Entity entity = getEntity(propName);
		entity.setProperty("xml", new Text(xmlString));
		// if (false)
		this.datastore.put(entity);		
	}
}

@SuppressWarnings("serial")
class SearchResult implements Serializable {
	extendedLinkedHashMap<String, String> elhm;

	SearchResult(extendedLinkedHashMap<String, String> elhm) {
		this.elhm = elhm;
	}

	String getResult(String key) {
		return elhm.get(key);
	}

	extendedLinkedHashMap<String, String> getRootMap() {
		return elhm;
	}
}

@SuppressWarnings("serial")
class HumanProp implements Serializable {
	LinkedHashMap<String, LinkedHashMap<String, String>> lhm;

	public HumanProp(LinkedHashMap<String, LinkedHashMap<String, String>> lhm) {
		this.lhm = lhm;
	}

	public void putHuman(String name, String templateCode, String genderCode) {
		LinkedHashMap<String, String> human = new LinkedHashMap<String, String>();
		human.put("t", templateCode);
		human.put("g", genderCode);
		lhm.put(name, human);
	}

	public LinkedHashMap<String, String> getHuman(String name) {
		LinkedHashMap<String, String> human = lhm.get(name);
		return human;
	}

	public LinkedHashMap<String, LinkedHashMap<String, String>> getRootMap() {
		return lhm;
	}
}

@SuppressWarnings({ "serial", "rawtypes" })
class extendedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
	private static final int MAX_ENTRIES = 1000;

	protected boolean removeEldestEntry(Map.Entry eldest) {
		return size() > MAX_ENTRIES;
	}
}