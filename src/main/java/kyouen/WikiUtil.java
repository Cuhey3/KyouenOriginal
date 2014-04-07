// [[]](あ）（い）みたいなときにちゃんと（あ）のカッコを取らないといけないので処理考えて

package kyouen;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.appengine.api.urlfetch.FetchOptions.Builder;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class WikiUtil {
	static final Pattern p_wikiparen = Pattern.compile("^" + "(.*?)" + "\\[\\["
			+ "([^\\|\\[\\]]+)" + "\\|?" + "([^\\|\\[\\]]+)?" + "\\]\\]"
			+ "(.*)" + "$"); // replaceWikiLink,clearMark で使用
	static final Pattern p_img_href = Pattern
			.compile("^(.*<b>)([^<>]*)(</b>.*)$");
	static final Pattern p_italic = Pattern.compile("^" + "(.*?)" + "'''''"
			+ "([^']*)" + "'''''" + "(.*)" + "$");
	static final Pattern p_bold = Pattern.compile("^" + "(.*?)" + "'''"
			+ "([^']*)" + "'''" + "(.*)" + "$");
	static final Pattern p_charParen = Pattern.compile("^([^（]*)" + "（" + "("
			+ "([^（）]+)" + "）" + "(.*)$" + "|" + "(" + "[^）]*" + "（" + "[^（）]+"
			+ "）" + ")" + "([^（）]*)" + "）" + "(.*)$" + ")");
	static final Pattern p_completeparen = Pattern.compile("[\\(（].*[）\\)]");
	static final Pattern p_notparen = Pattern.compile("^(.*?)([^\\(\\)（）]*)$");
	static final Pattern p_nothrefinparen = Pattern
			.compile("(.*?)[）\\)]([^\\[]*)\\]\\][^\\(\\)（）]*$");
	final static Pattern p_dvd = Pattern
			.compile("(テレビアニメ|劇場[用版]?アニメ|Webアニメ|特撮|吹き替え|イベント|ライブ|実写|DVD)");
	final static Pattern p_cd = Pattern.compile("ラジオ|ドラマCD|BLCD|朗読|CD");
	final static Pattern p_none = Pattern.compile("その他|ナレーション");
	final static Pattern p_tempName = Pattern.compile(
			"\\{\\{(.*?)(\\||\\}\\})", Pattern.DOTALL);
	final static Pattern p_link = Pattern
			.compile("^(.*)\\[\\[((.*?)\\|(.*?)\\]\\](?!\\])|([^\\|]*?)\\]\\])(.*)$");

	/* correct_nameを元にWikipediaからgetTextareaしてくるmethod */
	public static String getTextarea(String correctName) throws IOException {
		URL url = new URL(
				"http://ja.wikipedia.org/w/api.php?action=parse&format=xml&prop=wikitext|categories&page="
						+ correctName);
		// wikitextエレメントをターゲットに
		Element root = urlToRootElement(url);
		String wikitext = root.getChild("parse").getChild("wikitext")
				.getValue();
		Pattern p = Pattern.compile("(#REDIRECT|#転送|＃転送)\\s+\\[\\[.*?\\]\\]");
		if (p.matcher(wikitext).find())
			throw new NullPointerException();
		putHumanProp(correctName, root);
		wikitext = removeTemplate(wikitext, null);
		wikitext = wikitext.replaceAll("\n", "<br />"); // 改行があると正しく認識されないのでタグに置き換える
		wikitext = wikitext.replaceAll("(<br />=+)" + "([^= ])", "$1 $2"); // "<br />=+ "（スペース含む）をカテゴリ区切りとして標準化
		wikitext = wikitext.replaceAll("(<br />\\*+)" + "([^\\* ])", "$1 $2"); // "<br />*+ "（スペース含む）をコンテンツ区切りとして標準化
		wikitext = wikitext.replaceAll("&lt;", "<"); // &lt;を<に置き換える処理は単純に整形の話なので本来ここではなくもっと後工程でやるべきかもしれない
		wikitext = wikitext.replaceAll("(" + "<!--.*?-->" + "|"
				+ "(<ref[^>/]*)" // コメントと参照の削除。当サービスには必要ないもの
				+ "(/>" + "|" + ">.*?</ref>" + ")" + ")", "");
		return wikitext;
	}

	/* Recordクラスで record[8] を生成するためのmethod。record[8] はタイトルの直接の照会に使われる */
	public static String clearMark(String s) {
		String clear_text = null, href_2;
		for (Matcher m_href = p_wikiparen.matcher(s); m_href.find(); m_href = p_wikiparen
				.matcher(clear_text)) {
			href_2 = m_href.group(3);
			href_2 = href_2 == null ? m_href.group(2) : href_2;
			clear_text = m_href.group(1) + href_2 + m_href.group(4);
		}
		clear_text = clear_text == null ? s : clear_text;
		return clear_text.replaceAll("([" + "\\[\\]\\(\\)「」:：（）\\-〜『』･・/／ 　"
				+ "]|'{2,3})", "");
	}

	/*
	 * public static String replaceWithWikiLink(String row_text) throws
	 * IOException { String link_text = null, href_1, href_2; for (Matcher
	 * m_href = p_wikiparen.matcher(row_text); m_href.find(); m_href =
	 * p_wikiparen .matcher(link_text)) { // [[href_1|href_2]]を置換している。p_hrefも参照
	 * href_1 = m_href.group(2); href_2 = m_href.group(3) == null ? href_1 :
	 * m_href.group(3); link_text = m_href.group(1) + "<a href=\"" +
	 * "http://ja.wikipedia.org/wiki/" + href_1.replaceAll("%",
	 * "%25").replaceAll("#", "%23") .replaceAll(" ", "_").replaceAll("\\?",
	 * "%3F") + "\" title=\"" + href_1 + "\" target=\"_blank\">" +
	 * href_2.replaceAll("&amp;", "&") + "</a>" + m_href.group(4); } link_text =
	 * link_text == null ? row_text : link_text; return link_text; }
	 */

	public static String replaceWithWikiLink(String wikitext)
			throws IOException {
		String linkurl;
		String linktext;
		String backword;
		String tail = "";
		Matcher m;
		while ((m = p_link.matcher(wikitext)).find()) {
			if (m.group(5) == null) {
				linkurl = URLEncoder.encode(m.group(3), "UTF-8");
				linktext = m.group(4);
			} else {
				linkurl = URLEncoder.encode(m.group(5), "UTF-8");
				linktext = m.group(5);
			}
			linkurl = linkurl.replace("*", "%2A");
			linkurl = linkurl.replace("~", "%7E");
			linkurl = linkurl.replace("+", "%20");
			wikitext = m.group(1);
			backword = m.group(6);

			tail = "<a href=\"http://ja.wikipedia.org/wiki/" + linkurl + "\">"
					+ linktext + "</a>" + backword + tail;
		}
		wikitext = wikitext + tail;
		return wikitext;
	}

	public static String getClearTitle(String row_text) {
		String clearTitle = "", href_1, href_2;
		for (Matcher m_href = p_wikiparen.matcher(row_text); m_href.find(); m_href = p_wikiparen
				.matcher(clearTitle)) {
			href_1 = m_href.group(2);
			href_2 = m_href.group(3) == null ? href_1 : m_href.group(3);
			clearTitle = m_href.group(1) + href_2.replaceAll("&amp;", "＆")
					+ m_href.group(4);
		}
		clearTitle = clearTitle.equals("") ? row_text : clearTitle;
		return clearTitle;
	}

	/* Coクラスできれいなキャラクター名を出力するためのmethod 画像検索用の置換にtitleが必要 */
	public static String cleanCharName(String title, String charName) {
		for (Matcher m_italic = p_italic.matcher(charName); m_italic.find(); m_italic = p_italic
				.matcher(charName)) {
			charName = m_italic.group(1) + "<i><b>" + m_italic.group(2)
					+ "</b></i>" + m_italic.group(3);
		}

		for (Matcher m_bold = p_bold.matcher(charName); m_bold.find(); m_bold = p_bold
				.matcher(charName)) {
			charName = m_bold.group(1) + "<b>" + m_bold.group(2) + "</b>"
					+ m_bold.group(3);
		}
		for (Matcher m_img_href = p_img_href.matcher(charName); m_img_href
				.find(); m_img_href = p_img_href.matcher(charName)) {
			charName = m_img_href.group(1)
					+ "<a target=\"_blank\" href=\"http://www.google.co.jp/search?tbm=isch&q="
					+ m_img_href.group(2) + " " + title.replaceAll("＆", "&")
					+ "\" style=\"color:#000;\">" + m_img_href.group(2)
					+ "</a>" + m_img_href.group(3);
		}
		charName = p_charParen.matcher(charName).replaceAll("$1$3$4$5$6$7")
				.replaceAll("''", "");
		return charName;
	}

	/* Recordクラスで record[7] を生成するためのmethod。record[7] は部分一致の照会に使われる */
	public static String getLinkTitle(String row_text) throws IOException {
		Matcher m_href = p_wikiparen.matcher(row_text);
		if (m_href.find()) {
			String href = m_href.group(2).replaceAll("^([^#]*)#.*$", "$1");
			return href;
		} else
			return row_text;
	}

	/* #のあとを消すか消さないかだけで、処理は非常に簡単 */

	/*
	 * Recordクラスで、生テキストからタイトルとキャラクター名を切り分ける最初のmethod
	 * これはキャラクター名を返し、それを元に生テキストからタイトルを切り分けている
	 */
	public static String getCharName(String row_text) {
		int nestNum = 0;
		String charName = "";
		if (!p_completeparen.matcher(row_text).find() // 閉じたカッコがなさそうな場合、
				|| p_nothrefinparen.matcher(row_text).find()) // あるいはカッコ自体がない場合、探索しない
			return "";
		StringBuilder charNameSB = new StringBuilder();
		while (true) {
			Matcher m_notparen = p_notparen.matcher(row_text); // 行の末尾から、カッコが含まれない部分を抽出
			m_notparen.find();
			charNameSB.insert(0, m_notparen.replaceAll("$2"));
			if (m_notparen.replaceAll("$1").endsWith("）")
					|| m_notparen.replaceAll("$1").endsWith(")")) {
				charNameSB.insert(0, "）");
				nestNum++; // カッコが含まれない部分の末尾が終わりカッコなら、nestのカウントを増やす（つまり閉じないと終わらない）
			} else {
				charNameSB.insert(0, "（");
				nestNum--; // カッコが含まれない部分の末尾が終わりカッコでないなら、カウントを減らす（つまりnest終了を試みる）
				if (m_notparen.replaceAll("$1").isEmpty() || nestNum < 0) // まだ処理を続けるべきであるにもかかわらず、カッコが含まれない部分が空か、
					break; // nestの数が負になっているならwhileループをbreakし、
							// charName = "" をreturnする
				else if (nestNum == 0) { // めでたくnestNum が 0 になったら charName
											// を生成、ただしこれで終わりではない
					charName = new String(charNameSB) + charName;
					charNameSB = new StringBuilder(); // 終わりではなく続くので
														// StringBuilder を初期化
					if (!p_completeparen.matcher( // 残った部分に閉じたカッコがないあるいはカッコ自体がない場合のみ、break。CharNameをreturn
							m_notparen.replaceAll("$1").substring(0,
									m_notparen.replaceAll("$1").length() - 1))
							.find()
							|| p_nothrefinparen.matcher(
									m_notparen.replaceAll("$1").substring(
											0,
											m_notparen.replaceAll("$1")
													.length() - 1)).find())
						break;
				}
			}
			row_text = m_notparen.replaceAll("$1").substring(0, // row_text
																// を更新して、探索を続ける
					m_notparen.replaceAll("$1").length() - 1);
		}
		return charName;
	}

	/* やってることははっきりしているが、ややスパゲッティである */

	/* C = A + B のとき C と B を引数にして、 A を返すmethod */
	public static String cutString(String str, String cut) {
		int sl = str.length();
		int cl = cut.length();
		int between = sl - cl;
		return str.substring(0, between);
	}

	/* わざわざ別methodにする意味があるのかどうか */

	public static void headerWrite(Mode m, String[] nameArray, StringBuilder sb)
			throws IOException {
		Cache cache = getCache(60 * 15);
		String column = (String) cache.get("namecolumn");
		if (column == null) {
			WakeBackends wb = new WakeBackends();
			column = wb.newColumn();
		}
		sb.append("<html><head><link href=\"amazon.ico\" /><title>")
				.append(nameArray[1].split("_\\(")[0])
				.append(nameArray[2].isEmpty() ? " 出演リスト</title>" : "＆"
						+ nameArray[2].split("_\\(")[0] + " 共演リスト</title>")
				.append("<meta name=\"viewport\" content=\"user-scalable=no\" /><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />")
				.append("<meta name=\"description\" content=\"")
				.append(nameArray[1].split("_\\(")[0])
				.append(nameArray[2].isEmpty() ? "の出演作品を、一覧で表示します。\" />" : "と"
						+ nameArray[2].split("_\\(")[0]
						+ "の共演作品を、一覧で表示します。\" />")
				.append("<meta name=\"keywords\" content=\"")
				.append(nameArray[1].split("_\\(")[0])
				.append(nameArray[2].isEmpty() ? "" : ","
						+ nameArray[2].split("_\\(")[0])
				.append(",声優,共演,検索,アニメ\" />")
				.append("<script type=\"text/javascript\">")
				.append("function ChangeSelection1(form, selection) {\n")
				.append("form.n.value = selection.options[selection.selectedIndex].value;\n")
				.append("change_flag1(false);\n")
				.append("}\n")
				.append("function ChangeSelection2(form, selection) {\n")
				.append("form.m.value = selection.options[selection.selectedIndex].value;\n")
				.append("change_flag2(false);\n")
				.append("}\n")
				.append("function change_flag1(boolean) {\n")
				.append("if(boolean)\n")
				.append("document.getElementById(\"col_style1\").style.display = \"inline\";\n")
				.append("else\n")
				.append("document.getElementById(\"col_style1\").style.display = \"none\";\n")
				.append("}\n")
				.append("function change_flag2(boolean) {\n")
				.append("if(boolean)\n")
				.append("document.getElementById(\"col_style2\").style.display = \"inline\";\n")
				.append("else\n")
				.append("document.getElementById(\"col_style2\").style.display = \"none\";\n")
				.append("}\n")
				.append("  var _gaq = _gaq || [];")
				.append("  _gaq.push(['_setAccount', 'UA-28405404-2']);")
				.append("  _gaq.push(['_trackPageview']);")
				.append("  (function() {    var ga = document.createElement('script');")
				.append(" ga.type = 'text/javascript'; ga.async = true;")
				.append("    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';")
				.append("    var s = document.getElementsByTagName('script')[0];")
				.append(" s.parentNode.insertBefore(ga, s);  })();")
				.append("</script>")
				.append("<link href=\"table.css\" rel=\"stylesheet\" type=\"text/css\" />")
				.append("</head><body onload=\"change_flag1(false),change_flag2(false)\">")
				.append("<div id=\"head\"><a href=\"/\" class=\"top\">サイトTOP</a><a href=\"guide\" class=\"guide\">ガイド</a><a href=\"past\" class=\"past\">検索履歴</a>")
				.append("<a href=\"ranking\" class=\"ranking\">総合ランキング</a><a href=\"pair\" class=\"pair\">男女ランキング</a><form action=\"co\" method=\"GET\"><input class=\"text\" type=\"text\" name=\"n\" value=\"")
				.append(nameArray[0])
				.append("\" onmouseover=\"change_flag1(true),change_flag2(false)\" onclick=\"change_flag1(false)\" />")
				.append(" ＆ <input class=\"text\" type=\"text\" name=\"m\" onmouseover=\"change_flag2(true),change_flag1(false)\" onclick=\"change_flag2(false)\"/>")
				.append(" の共演を <input type=\"submit\" value=\"調べる\" />")
				.append("<div id=\"col_style1\">")
//				.append("<select id=\"column1\" onchange=\"ChangeSelection1(this.form, this)\">")
//				.append(column)
//				.append("</select>")
				.append("</div>")
				.append("<div id=\"col_style2\">")
//				.append("<select id=\"column2\" onchange=\"ChangeSelection2(this.form, this)\">")
//				.append(column)
//				.append("</select>")
				.append("</div></form></div><br /><div id=\"space\"></div>");
	}

	public static void getAssociateLink(String genre, String title,
			StringBuilder sb) {
		Matcher m_href = p_wikiparen.matcher(title);
		String href_2 = m_href.find() ? (m_href.group(3) == null ? m_href
				.group(2) : m_href.group(3)) : title;
		String word;
		if (p_dvd.matcher(genre).find())
			word = "&index=dvd";
		else if (p_cd.matcher(genre).find())
			word = "&index=music";
		else if (p_none.matcher(genre).find())
			word = "";
		else
			word = " " + genre;
		sb.append(
				"<a target=\"_blank\" href=\"http://www.amazon.co.jp/gp/search?ie=UTF8&keywords=")
				.append(href_2.replaceAll("&amp;", "&"))
				.append(word)
				.append("&tag=kyouen0d-22&linkCode=ur2&camp=247&creative=1211\" ")
				.append("title=\"amazonで関連商品を検索\"></a>");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Cache getCache(int i) {
		Cache cache = null;
		try {
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			Map props = new HashMap();
			props.put(GCacheFactory.EXPIRATION_DELTA, i);
			cache = cacheFactory.createCache(props);
		} catch (CacheException e) {
		}
		return cache;
	}

	public static int getMF(String name) {
		String s = (String) getCache(60 * 60 * 24 * 365).get("namelist");
		if (s == null) {
			WakeBackends wb = new WakeBackends();
			wb.memSet();
			s = (String) WikiUtil.getCache(60 * 60 * 24 * 365).get("namelist");
		}
		Pattern p_name = Pattern.compile("^.*?" + name.split("[　_＿]")[0]
				+ ",[^,]*,([^,]+),.*$");
		Matcher m_name = p_name.matcher(s);
		if (m_name.find())
			return Integer.parseInt(m_name.group(1));
		else
			return 100;

	}

	public static String getBacklink(String name) throws IOException {
		String correctName = name;
		Pattern p_s = Pattern
				.compile("^(.*?)(_?(シリーズ)?の登場(人物|キャラクター)?(.*)|シリーズ|_\\(.*\\))$");
		String namelist = WakeBackends.namelist;
		if (namelist.split(name.split("[　_＿]")[0] + ",").length == 2) {
			if (namelist.split(name.split("[　_＿]")[0] + ",")[1].split(",")[0]
					.equals("&i=1"))
				; // ここ注意 &i=1を読み飛ばしている うかつに消さないように
			else if (!namelist.split(name.split("[　_＿]")[0] + ",")[1]
					.split(",")[0].isEmpty())
				name = name.split("[　_＿]")[0]
						+ namelist.split(name.split("[　_＿]")[0] + ",")[1]
								.split(",")[0];
		}
		namelist = namelist
				+ ",大沢事務所,81プロデュース,青二プロダクション,元氣プロジェクト,プロ・フィット,マウスプロモーション,賢プロダクション,ケンユウオフィス,アクセルワン,東京俳優生活協同組合,テアトル・エコー,シグマ・セブン,ぷろだくしょんバオバブ,ホーリーピーク,ゆーりんプロ,";
		URL url = new URL(
				"http://ja.wikipedia.org/w/api.php?action=query&format=xml&list=backlinks&bllimit=50000&blnamespace=0&bltitle="
						+ correctName);
		StringBuilder backlinkTitle = new StringBuilder();
		StringBuilder backlinkSeriesTitle = new StringBuilder();
		// backlinksエレメントをターゲットに
		Element element = urlToRootElement(url).getChild("query").getChild(
				"backlinks");
		// 子要素であるblのイテレータを得る
		Iterator<Element> itr = element.getChildren("bl").iterator();
		// title属性のみをListへ格納 ns != 0 なら除外
		ArrayList<String> al = new ArrayList<String>();
		while (itr.hasNext()) {
			Element e = (Element) itr.next();
			al.add(e.getAttributeValue("title"));
		}
		// シリーズもの、登場人物は デリミタ <<>> の後へ回す
		for (String s : al) {
			if (namelist.indexOf("," + s.split("[ 　_＿]")[0] + ",", 0) != -1)
				continue;
			if (p_s.matcher(s).find())
				backlinkSeriesTitle.append("<>" + s);
			else
				backlinkTitle.append("<>" + s);
		}
		return new String(backlinkTitle) + "<<>>"
				+ new String(backlinkSeriesTitle);
	}

	public static enum Mode {
		Co, Rank, Pair, Guide
	}

	public static String removeTemplate(String wikitext, String[] removeName) {
		if (removeName == null) {
			String[] array = { "Cite web", "Reflist", "JPN", "Cite journal",
					"Voice-stub", "Cite book", "声優", "存命人物の出典明記", "Notice",
					"出典の明記", "記事名の制約", "正確性", "BLP unsourced", "独自研究" };
			removeName = array.clone();
		}
		Pattern p = Pattern.compile("^(.*)(\\{\\{.*?\\}\\})(.*)$",
				Pattern.DOTALL);
		Matcher m;
		ArrayList<String> okTemp = new ArrayList<String>();
		while ((m = p.matcher(wikitext)).find()) {
			String thisTemplateName = getTemplateName(m.group(2));
			boolean removeFlag = false;
			for (String rem : removeName) {
				if (rem.equalsIgnoreCase(thisTemplateName))
					removeFlag = true;
			}
			if (removeFlag)
				wikitext = m.group(1) + m.group(3).trim();
			else {
				int index = okTemp.size();
				okTemp.add(m.group(2));
				wikitext = m.group(1) + "<<a>>" + index + "<<z>>" + m.group(3);
			}
		}
		p = Pattern.compile("^(.*)<<a>>(\\d{1,})<<z>>(.*)$", Pattern.DOTALL);
		while ((m = p.matcher(wikitext)).find()) {
			wikitext = m.group(1) + okTemp.get(Integer.parseInt(m.group(2)))
					+ m.group(3);
		}
		return wikitext;
	}

	public static String getTemplateName(String wikitext) {
		Matcher m = p_tempName.matcher(wikitext);
		if (m.find())
			return m.group(1).replace('_', ' ').trim();
		else
			return "";
	}

	public static Element urlToRootElement(URL url) throws IOException {
		HTTPRequest httpRequest = new HTTPRequest(url, HTTPMethod.GET, Builder
				.disallowTruncate().setDeadline(30.0));
		httpRequest
				.addHeader(new HTTPHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5"));
		Element element = null;
		HTTPResponse httpResponse = URLFetchServiceFactory.getURLFetchService()
				.fetch(httpRequest);
		try {
			element = new SAXBuilder().build(
					new StringReader(new String(httpResponse.getContent(),
							"utf-8"))).getRootElement();
		} catch (JDOMException e) {
			throw new IOException();
		}
		return element;
	}

	public static String getAimaiSearchResult(String searchWord)
			throws IOException {
		/* ページの取得 */
		String encodedSearchWord = URLEncoder.encode(searchWord, "UTF-8")
				.replace("*", "%2A").replace("~", "%7E").replace("+", "%20");
		URL url = new URL("http://search.yahoo.co.jp/search?p="
				+ encodedSearchWord + "%20wikipedia");
		HTTPRequest httpRequest = new HTTPRequest(url, HTTPMethod.GET, Builder
				.disallowTruncate().setDeadline(30.0));
		httpRequest
				.addHeader(new HTTPHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5"));
		String responseString = new String(URLFetchServiceFactory
				.getURLFetchService().fetch(httpRequest).getContent(), "utf-8");

		/* ページリストの作成 */
		Pattern p = Pattern.compile(
				"<h3><a href=\"http://ja.wikipedia.org/wiki/(.*?)\" >",
				Pattern.DOTALL);
		Matcher m = p.matcher(responseString);
		ArrayList<String> pageList = new ArrayList<String>();
		while (m.find()) {
			String pageName = URLDecoder.decode(m.group(1), "utf-8");
			pageList.add(pageName);
		}

		Assistant as = new Assistant();
		as.setHumanProp();

		/* 検索ワードと同じ記事名があり、それが正規化されていればそれを返す */
		for (String pageName : pageList) {
			if (pageName.equals(searchWord))
				if (as.getHuman(pageName) != null)
					return pageName;
		}

		/* 検索ワードが前方一致する記事名があり、それが正規化されていればそれを返す */
		for (String pageName : pageList) {
			if (pageName.startsWith(searchWord))
				if (as.getHuman(pageName) != null)
					return pageName;
		}

		/* 検索ワードと同じ記事名があればそれを返す */
		for (String pageName : pageList) {
			if (pageName.equals(searchWord))
				return pageName;
		}

		/* 検索ワードが前方一致する記事名があればそれを返す */
		for (String pageName : pageList) {
			if (pageName.startsWith(searchWord))
				return pageName;
		}

		/* ページリストの中に正規化済みの記事名と同じものがあればそれを返す */
		for (String pageName : pageList) {
			if (as.getHuman(pageName) != null)
				return pageName;
		}

		/* ここまでに該当しなければ検索結果の一番上を返す */
		return pageList.get(0);
	}

	protected static void putHumanProp(String correctName, Element root) {
		String wikitext = root.getChild("parse").getChild("wikitext")
				.getValue();
		TemplateReader tr = new TemplateReader(wikitext);
		tr.read();
		tr.write(correctName);
		Element element = root.getChild("parse").getChild("categories");
		Iterator<Element> itr = element.getChildren("cl").iterator();
		ArrayList<String> categories = new ArrayList<String>();
		while (itr.hasNext()) {
			Element e = (Element) itr.next();
			categories.add(e.getText());
		}
		CategoryReader cr = new CategoryReader(categories);
		cr.read();
		cr.write(correctName);
		Assistant as = new Assistant();
		if(as.getHuman(correctName).isEmpty())
			as.removeHumanProp(correctName);
		as.makePersistentHumanProp();
	}

	/*
	 * 正規化された入力を返す。 判定の途中でリダイレクトの必要が生じたら、それを input[3] に0,1のフラグとして持つ。
	 */
	public static String[] getValidatedInput(String n, String m)
			throws IOException {
		// 結果を保持する配列 input 0,1,2,3
		// 0 … 先行して入力された入力, 1 … nパラメータを受ける, 2 … mパラメータを受ける
		// 3 … リダイレクト判定格納スペース
		String[] input = { "", "", "", "" };
		boolean nonredirect = true;
		// この時点で null を排除。空白文字をシュリンク
		input[1] = n == null ? "" : n.replaceAll("[ 　]+", " ").trim();
		input[2] = m == null ? "" : m.replaceAll("[ 　]+", " ").trim();

		// 重複入力の削除。再度メソッドの最後にやる
		if (input[1].equals(input[2]))
			input[2] = "";

		if (input[1].isEmpty())
			// input[1] が空入力ならリダイレクトフラグ ON
			nonredirect = false;
		else {
			// 以下では少なくとも input[1] は空入力ではないが、
			// input[2] は空の可能性がある
			Assistant as = new Assistant();
			as.setHumanProp();
			as.setSearchResult();
			/* input[1] を検証 */
			LinkedHashMap<String, String> human1 = as.getHuman(input[1]);
			// 正規化済み人物名のチェック
			if (human1 == null) {
				// 正規化済みのチェックできないなら 検索単語チェック
				String searchResultWord = as.getSearchResultWord(input[1]);
				// 検索単語すら特定できないなら、例外を投げて終了
				if (searchResultWord == null)
					throw new IOException();
				// 検索単語と入力が異なっていたら、検索単語でリダイレクト
				else if (!searchResultWord.equals(input[1])) {
					nonredirect = false;
					input[1] = searchResultWord;
				} else
					// リダイレクトが発生しなかったら先行入力へ格納
					input[0] = new String(input[1]);
			} else
				input[0] = new String(input[1]);

			/* input[2] を検証 */
			// input[2] は空入力の可能性がある
			if (!input[2].isEmpty()) {
				// 以降の処理は input[1] の時と同じ
				LinkedHashMap<String, String> human2 = as.getHuman(input[2]);
				if (human2 == null) {
					String searchResultWord = as.getSearchResultWord(input[2]);
					if (searchResultWord == null) {
						throw new IOException();
					} else if (!searchResultWord.equals(input[2])) {
						nonredirect = false;
						input[2] = searchResultWord;
					}
				}
			}

			// 再度の重複チェック
			if (input[1].equals(input[2]))
				input[2] = "";

			// 順序チェックをかける
			// リダイレクト先で先行入力されたものを知りたいので、
			// ここではリダイレクトが発生しなかった時だけ順序を入れ替える
			if (nonredirect && !input[2].isEmpty()
					&& input[2].compareTo(input[1]) < 0) {
				String buff = new String(input[1]);
				input[1] = new String(input[2]);
				input[2] = buff;
			}
		}
		// boolean を input[3] に格納して返却
		if (nonredirect)
			input[3] = "1";
		else
			input[3] = "0";
		return input;
	}
}