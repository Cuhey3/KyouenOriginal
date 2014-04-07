package kyouen;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

@SuppressWarnings("serial")
public class Labo2 extends HttpServlet {
	HttpServletRequest req;
	HttpServletResponse resp;
	String[] nameArray, contents1, contents2;
	StringBuilder sb;
	final Pattern p_p = Pattern.compile("(ノート:|利用者[:‐]|Template:|Wikipedia:)");
	final Pattern p_s = Pattern
			.compile("^(.*?)(_?(シリーズ)?の登場(人物|キャラクター)?(.*)|シリーズ|_\\(.*\\))$");

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		this.req = request;
		this.resp = response;
		initialize();
		resp.getWriter()
				.println(
						"<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />");
		if (nameArray[1] == null || nameArray[2] == null) {
			resp.getWriter()
					.println(
							"<h2>指定した二人の名前が両方含まれているWikipedia記事を探すよ☆</h2><form action=\"\" method=\"GET\">一人目<input type=\"text\" name=\"n\" /><br />二人目<input type=\"text\" name=\"m\" /><br /><input type=\"submit\" value=\"調べる\"></form>（名前は正しく入れてね★）");
		} else {
			if (!nameArray[1].equals("debag")) {
				contents1 = getBacklink(nameArray[1]).replaceAll("<<>>", "")
						.split("<>");
				contents2 = getBacklink(nameArray[2]).replaceAll("<<>>", "")
						.split("<>");
				headerWrite();
				for (String n : contents1)
					for (String m : contents2) {
						if (n.equals(m)) {
							resp.getWriter().println(
									"<a href=\"http://ja.wikipedia.org/wiki/"
											+ n + "\">" + n + "</a><br />");
							break;
						}
					}
			} else
				resp.getWriter().println(getBacklink(nameArray[2]));
		}
	}

	public void initialize() {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		nameArray = new String[3];
		nameArray[1] = req.getParameter("n");
		nameArray[2] = req.getParameter("m");
	}

	public void headerWrite() throws IOException {
		resp.getWriter()
				.println(
						"<style type=\"text/css\"><!-- a {text-decoration:none;}--></style>");
		resp.getWriter().println(
				"<h2>" + nameArray[1] + " と " + nameArray[2]
						+ " が両方含まれている記事の一覧</h2>");
	}

	public String getBacklink(String name) throws IOException {
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
						+ name);
		StringBuilder backlinkTitle = new StringBuilder();
		StringBuilder backlinkSeriesTitle = new StringBuilder();
		try {
			// backlinksエレメントをターゲットに
			Element element = new SAXBuilder().build(url).getRootElement()
					.getChild("query").getChild("backlinks");
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
				if(namelist.indexOf("," + s.split("[ 　_＿]")[0] + ",",0) != -1)
					continue;
				if (p_s.matcher(s).find())
					backlinkSeriesTitle.append("<>" + s);
				else
					backlinkTitle.append("<>" + s);
			}
		} catch (JDOMException e) {
		}
		return new String(backlinkTitle) + "<<>>"
				+ new String(backlinkSeriesTitle);
	}
}