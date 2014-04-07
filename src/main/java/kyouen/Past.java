package kyouen;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.*;

import net.sf.jsr107cache.Cache;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

@SuppressWarnings("serial")
public class Past extends HttpServlet {
	Cache cache = WikiUtil.getCache(60 * 60 * 24);

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		String column = (String) cache.get("namecolumn");
		if (column == null) {
			WakeBackends wb = new WakeBackends();
			column = wb.newColumn();
		}
		String name = req.getParameter("n") == null ? "" : req
				.getParameter("n");
		String pastString = getPastString();
		String pastUpdate = getPastUpdate();
		if (req.getParameter("mokyu") != null) {
			resp.getWriter().println(pastString + "<>" + pastUpdate);
		} else {
			String[] pastSplit = pastString.split(",");
			String blankOrNew = null;
			String[] updateSplit = pastUpdate.split(",");
			StringBuilder sb = new StringBuilder();
			sb.append(
					"<html><head><title>共演検索履歴</title><meta name=\"viewport\" content=\"user-scalable=no\" /><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />")
					.append("<meta name=\"description\" content=\"共演検索マシーンで過去に検索された共演のリストです。\">")
					.append("<meta name=\"keywords\" content=\"声優,共演,検索,アニメ\">")
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
					.append("")
					.append("\" onmouseover=\"change_flag1(true),change_flag2(false)\" onclick=\"change_flag1(false)\" />")
					.append(" ＆ <input class=\"text\" type=\"text\" name=\"m\" onmouseover=\"change_flag2(true),change_flag1(false)\" onclick=\"change_flag2(false)\"/>")
					.append(" の共演を <input type=\"submit\" value=\"調べる\" />")
					.append("<div id=\"col_style1\"><select id=\"column1\" onchange=\"ChangeSelection1(this.form, this)\">")
					.append(column)
					.append("</select></div>")
					.append("<div id=\"col_style2\"><select id=\"column2\" onchange=\"ChangeSelection2(this.form, this)\">")
					.append(column)
					.append("</select></div></form></div><div id=\"wrap\">")
					.append("<br /><div id=\"space\"></div><div id=\"contents\"><h2>最新の検索履歴500件</h2>")
					.append(pastSplit.length / 4 - 500)
					.append("件の履歴が省略されています。<br />全件を対象として、声優名でフィルタをかけることができます。<br /><form action=\"\" method=\"GET\">")
					.append("<input type=\"text\" name=\"n\" size=\"8\" /><input type=\"submit\" value=\"声優名でフィルタ\" /></form>")
					.append("<table border=\"1\"><tr><th>");
			if (!name.equals(""))
				sb.append(name + "の");
			sb.append("共演者名</th><th>完全一致(件)</th><th>部分一致(件)</th><th>新着</th></tr>");
			for (int i = 0; i * 4 + 4 < pastSplit.length; i++) {
				for (int j = 0; j < updateSplit.length; j++) {
					if (updateSplit[j].isEmpty()) {
						blankOrNew = "";
						continue;
					}
					if (updateSplit[j].equals(pastSplit[i * 4 + 1] + "＆"
							+ pastSplit[i * 4 + 2])) {
						blankOrNew = "<span style=\"color:#ee0303;\">New!</span>";
						updateSplit[j] = "";
						break;
					} else
						blankOrNew = "";
				}
				if (name.equals("")) {
					sb.append("<tr><td><a href=\"co?n=")
							.append(pastSplit[i * 4 + 1]).append("&m=")
							.append(pastSplit[i * 4 + 2]).append("\">")
							.append(pastSplit[i * 4 + 1]).append("＆")
							.append(pastSplit[i * 4 + 2]).append("</a></td>")
							.append("<td class=\"count\">")
							.append(pastSplit[i * 4 + 3])
							.append("</td><td class=\"count\">")
							.append(pastSplit[i * 4 + 4]).append("</td>")
							.append("<td>" + blankOrNew + "</td></tr>");
				} else if (name.equals(pastSplit[i * 4 + 1]))
					sb.append("<tr><td><a href=\"co?n=")
							.append(pastSplit[i * 4 + 1]).append("&m=")
							.append(pastSplit[i * 4 + 2]).append("\">")
							.append(pastSplit[i * 4 + 2]).append("</a></td>")
							.append("<td class=\"count\">")
							.append(pastSplit[i * 4 + 3])
							.append("</td><td class=\"count\">")
							.append(pastSplit[i * 4 + 4]).append("</td>")
							.append("<td>" + blankOrNew + "</td></tr>");
				else if (name.equals(pastSplit[i * 4 + 2]))
					sb.append("<tr><td><a href=\"co?n=")
							.append(pastSplit[i * 4 + 1]).append("&m=")
							.append(pastSplit[i * 4 + 2]).append("\">")
							.append(pastSplit[i * 4 + 1]).append("</a></td>")
							.append("<td class=\"count\">")
							.append(pastSplit[i * 4 + 3])
							.append("</td><td class=\"count\">")
							.append(pastSplit[i * 4 + 4]).append("</td>")
							.append("<td>" + blankOrNew + "</td></tr>");
				if (i > 498 && name.equals(""))
					break;
			}
			resp.getWriter()
					.println(
							new String(sb)
									+ "</table></div></div></div></body></html>");
		}
	}

	public void sideOutput(StringBuilder sb, int size, String[] nameArray) {
		sb.append(
				"<div id=\"side\"><h2>共演検索の履歴</h2><form action=\"\" method=\"POST\"><input type=\"radio\" name=\"s\" value=\""
						+ nameArray[1]
						+ "\" />"
						+ nameArray[1]
						+ "<input type=\"radio\" name=\"s\" value=\""
						+ nameArray[2]
						+ "\" />"
						+ nameArray[2]
						+ "<br /> 上記以外→ <input type=\"text\" name=\"rn\" size=\"8\" /><br /><input type=\"submit\" value=\"履歴を表示\" /></form><table border=\"1\"><tr><th>")
				.append(nameArray[3].equals("") ? "最近の共演検索" : "共演検索 - "
						+ nameArray[3]).append("</th><th>件数</th></tr>");
		String[] pastSplit = getPastString().split(",");
		int thisSize = nameArray[3].equals("") ? (int) (size * 2.5)
				: (pastSplit.length - 1) / 4;
		for (int i = 0; i < thisSize; i++) {
			try {
				if (pastSplit[i * 4 + 1] == null
						|| pastSplit[i * 4 + 2] == null
						|| pastSplit[i * 4 + 3] == null
						|| pastSplit[i * 4 + 4] == null)
					break;
				if (nameArray[3].equals("")
						|| pastSplit[i * 4 + 1].equals(nameArray[3])
						|| pastSplit[i * 4 + 2].equals(nameArray[3]))
					sb.append("<tr><td><a href=\"co?n=")
							.append(pastSplit[i * 4 + 1])
							.append("&m=")
							.append(pastSplit[i * 4 + 2])
							.append("\">")
							.append(nameArray[3].equals("") ? pastSplit[i * 4 + 1]
									+ "＆" + pastSplit[i * 4 + 2]
									: pastSplit[i * 4 + 1]
											.equals(nameArray[3]) ? pastSplit[i * 4 + 2]
											: pastSplit[i * 4 + 1])
							.append("</a></td>").append("<td class=\"count\">")
							.append(pastSplit[i * 4 + 3]).append("</td></tr>");
			} catch (Exception e) {
				break;
			}
		}
		sb.append("</table><a href=\"past\">全ての履歴を見る</a></div></div>");
		sb.append("</div></body></html>");
	}

	public void putPastAndRank(String[] nameArray, int[] maxCount,
			StringBuilder sb) throws IOException {
		long time = System.currentTimeMillis();
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past = null;
		try {
			past = datastore
					.get(KeyFactory.createKey("past_string", "onlyOne"));
		} catch (EntityNotFoundException e) {
		}
		past.setProperty("flag", false);
		past.setProperty("pflag", false);
		boolean putflag = false;
		// nameArray[1] = nameArray[1].split("[　_＿]")[0];
		// nameArray[2] = nameArray[2].split("[　_＿]")[0];
		sb.append("<!-- ppsetstart: "
				+ ((System.currentTimeMillis() - time) * -1) + "-->");
		String current_past_string = getPastString();
		String rank_update = (String) past.getProperty("rank_update");
		String pair_update = (String) past.getProperty("pair_update");
		Pattern p1 = Pattern.compile("^(.*?)(," + Pattern.quote(nameArray[1])
				+ "," + Pattern.quote(nameArray[2]) + ","
				+ "[0-9]+,[0-9]+)(.*)$");
		Pattern p2 = Pattern.compile("," + Pattern.quote(nameArray[1]) + ","
				+ Pattern.quote(nameArray[2]) + "," + "[0-9]+,[0-9]+");
		if (!p2.matcher(current_past_string).find()) {
			String past_update = getPastUpdate();
			past_update = "," + nameArray[1] + "＆" + nameArray[2] + past_update;
			String[] updateSplit = past_update.split(",");
			int i = 0;
			String updateRebuild = "";
			for (String sp : updateSplit) {
				if (sp.isEmpty())
					continue;
				updateRebuild += "," + sp;
				i++;
				if (i > 20)
					break;
			}
			past_update = updateRebuild;
			past.setProperty("past_update", past_update);
			cache.put("past_update", past_update);
			putflag = true;
		}
		Matcher m;
		while((m = p1.matcher(current_past_string)).find()){
			current_past_string = m.replaceAll("$1$3");
			m = p1.matcher(current_past_string);
		}
		String past_string = "," + nameArray[1] + "," + nameArray[2] + ","
				+ maxCount[0] + "," + maxCount[1]
				+ current_past_string;
		past.setProperty("text", new Text(past_string));
		cache.put("past_string", past_string);

/*		Pattern p_1 = Pattern.compile("^(.*?)([^0-9]+,)?("
				+ Pattern.quote(nameArray[1]) + ",)([^0-9]+,)?([0-9]+)(.*)$");
		Pattern p_2 = Pattern.compile("^(.*?)([^0-9]+,)?("
				+ Pattern.quote(nameArray[2]) + ",)([^0-9]+,)?([0-9]+)(.*)$");
		sb.append("<!-- pprankstart: "
				+ ((System.currentTimeMillis() - time) * -1) + "-->");
		if (maxCount[0] != 0 && WikiUtil.getMF(nameArray[1]) != 100
				&& WikiUtil.getMF(nameArray[2]) != 100) {
			Text rank_text = (Text) past.getProperty("rank");
			String oldrank_1 = "0", oldrank_2 = "0";
			String oldpair_1 = "", oldpair_2 = "";
			Matcher m_1 = p_1.matcher(rank_text.getValue());
			Matcher m_2 = p_2.matcher(rank_text.getValue());
			if (m_1.find()) {
				oldrank_1 = m_1.group(5);
				oldpair_1 = m_1.replaceAll("$2$4").replaceAll(",", "");
			}
			if (m_2.find()) {
				oldrank_2 = m_2.group(5);
				oldpair_2 = m_2.replaceAll("$2$4").replaceAll(",", "");
			}
			if (oldrank_1.equals("0") && oldrank_2.equals("0")) {
				sb.append("<script type=\"text/javascript\">window.alert(' -- 総合ランキング --\\n"
						+ nameArray[1]
						+ "＆"
						+ nameArray[2]
						+ "\\n\\nこのペアが新しくランクインしました。');</script>");
				past.setProperty("flag", true);
			} else if (maxCount[0] > Integer.parseInt(oldrank_1)
					&& maxCount[0] > Integer.parseInt(oldrank_2)
					&& !oldpair_2.equals(nameArray[1])
					&& !oldpair_1.equals(nameArray[2])) {
				sb.append("<script type=\"text/javascript\">window.alert(' -- 総合ランキング --\\n"
						+ nameArray[1]
						+ "＆"
						+ nameArray[2]
						+ "\\n\\nこのペアが新しくランクインしました。"
						+ "\\nこのペアよりも順位が下のランキングに変動があり、"
						+ "\\n特に、以下の人物はペアが組み直しになります。\\n\\n"
						+ oldpair_1
						+ "　"
						+ oldpair_2 + "');</script>");
				past.setProperty("flag", true);
			}
		}
		sb.append("<!-- pppairstart: "
				+ ((System.currentTimeMillis() - time) * -1) + "-->");
		if (maxCount[0] != 0
				&& WikiUtil.getMF(nameArray[1]) * WikiUtil.getMF(nameArray[2]) == -1) {
			Text pair_text = (Text) past.getProperty("pair");
			String oldrank_1 = "0";
			String oldrank_2 = "0";
			String oldpair_1 = "";
			String oldpair_2 = "";
			Matcher m_1 = p_1.matcher(pair_text.getValue());
			Matcher m_2 = p_2.matcher(pair_text.getValue());
			if (m_1.find()) {
				oldrank_1 = m_1.group(5);
				oldpair_1 = m_1.replaceAll("$2$4").replaceAll(",", "");
			}
			if (m_2.find()) {
				oldrank_2 = m_2.group(5);
				oldpair_2 = m_2.replaceAll("$2$4").replaceAll(",", "");
			}
			if (oldrank_1.equals("0") && oldrank_2.equals("0")) {
				sb.append("<script type=\"text/javascript\">window.alert(' -- 男女ランキング --\\n"
						+ nameArray[1]
						+ "＆"
						+ nameArray[2]
						+ "\\n\\nこのペアが新しくランクインしました。');</script>");
				past.setProperty("pflag", true);
			} else if (maxCount[0] > Integer.parseInt(oldrank_1)
					&& maxCount[0] > Integer.parseInt(oldrank_2)
					&& !oldpair_2.equals(nameArray[1])
					&& !oldpair_1.equals(nameArray[2])) {
				sb.append("<script type=\"text/javascript\">window.alert(' -- 男女ランキング --\\n"
						+ nameArray[1]
						+ "＆"
						+ nameArray[2]
						+ "\\n\\nこのペアが新しくランクインしました。"
						+ "\\nこのペアよりも順位が下のランキングに変動があり、"
						+ "\\n特に、以下の人物はペアが組み直しになります。\\n\\n"
						+ oldpair_1
						+ "　"
						+ oldpair_2 + "');</script>");
				past.setProperty("pflag", true);
			}
		}
		sb.append("<!-- ppqueuestart: "
				+ ((System.currentTimeMillis() - time) * -1) + "-->");

		if ((Boolean) past.getProperty("flag")) {
			rank_update = "," + nameArray[1] + "＆" + nameArray[2] + rank_update;
			String[] update_box = rank_update.split(",");
			int i = 0;
			String str = "";
			for (String b : update_box) {
				if (b.isEmpty())
					continue;
				str += "," + b;
				i++;
				if (i > 20)
					break;
			}
			past.setProperty("rank_update", str);
			putflag = true;
			Queue queue = QueueFactory.getDefaultQueue();
			queue.add(Builder.withUrl("/ranking?c=this").method(Method.GET));
		}
		if ((Boolean) past.getProperty("pflag")) {
			pair_update = "," + nameArray[1] + "＆" + nameArray[2] + pair_update;
			String[] update_box = pair_update.split(",");
			int i = 0;
			String str = "";
			for (String b : update_box) {
				if (b.isEmpty())
					continue;
				str += "," + b;
				i++;
				if (i > 20)
					break;
			}
			past.setProperty("pair_update", str);
			putflag = true;
			Queue queue = QueueFactory.getDefaultQueue();
			queue.add(Builder.withUrl("/pair").method(Method.GET));
		}
		sb.append("<!-- ppdostart: "
				+ ((System.currentTimeMillis() - time) * -1) + "-->");*/
		try {
			if (putflag == true)
				//if(false)
				datastore.put(past);
		} catch (Throwable t) {
		}
		sb.append("<!-- ppend: " + ((System.currentTimeMillis() - time) * -1)
				+ "-->");
	
	}

	public String getPastString() {
		String past_string = (String) cache.get("past_string");
		if (past_string != null)
			return past_string;
		Entity past_entity = null;
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		try {
			past_entity = datastore.get(KeyFactory.createKey("past_string",
					"onlyOne"));
		} catch (EntityNotFoundException e) {
		}
		past_string = ((Text) past_entity.getProperty("text")).getValue();
		return past_string;
	}

	public String getPastUpdate() {
		String past_update = (String) cache.get("past_update");
		if (past_update != null)
			return past_update;
		Entity past_entity = null;
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		try {
			past_entity = datastore.get(KeyFactory.createKey("past_string",
					"onlyOne"));
		} catch (EntityNotFoundException e) {
		}
		past_update = (String) past_entity.getProperty("past_update");
		return past_update;
	}

	public LinkedHashMap<String, int[]> convertPastToMapAndXML(boolean which)
			throws IOException {
		Long now0 = System.currentTimeMillis();
		Cache cache = WikiUtil.getCache(100000000);
		PastMap convert;
		PastMap hasredirect;
		try {
			convert = (PastMap) cache.get("pastMap");
		} catch (Throwable t) {
			convert = new PastMap();
		}
		try {
			hasredirect = (PastMap) cache.get("redirPastMap");
		} catch (Throwable t) {
			hasredirect = new PastMap();
		}
		if(convert == null)convert = new PastMap();
		if(hasredirect == null)hasredirect = new PastMap();
		String past_string = getPastString();
		past_string = past_string.split("<>")[0] + ",a";
		Pattern p = Pattern.compile("^,([^,]*?),([^,]*?),(\\d+),(\\d+)(,.*)$");
		Matcher m;
		String[] names = new String[2];
		int[] counts = new int[3];
		Assistant as = new Assistant();
		as.setHumanProp();
		LinkedHashMap<String, String> human;
		int i = 0;
		int size;
		try {
			size = convert.getRootMap().size();
		} catch (Throwable t) {
			size = 0;
		}
		while ((m = p.matcher(past_string)).find()) {
			past_string = m.group(5);
			names[0] = m.group(1);
			names[1] = m.group(2);
			String key = names[0] + "|" + names[1];
			if (convert.containsKey(key))
				continue;
			else if (hasredirect.containsKey(key))
				continue;
			i++;
			if (i % 10 == 0) {
				Long now1 = System.currentTimeMillis();
				if (now1 - now0 > 45000)
					break;
			}
			// if (names[1].compareTo(names[0]) < 0) {
			// String buff = new String(names[0]);
			// names[0] = new String(names[1]);
			// names[1] = buff;
			// }
			counts[0] = Integer.parseInt(m.group(3));
			counts[1] = Integer.parseInt(m.group(4));

			String[] result = WikiUtil.getValidatedInput(names[0], names[1]);
			human = as.humanProp.getHuman(names[0]);
			int gender1 = 9;
			if (human == null) {
				gender1 = 9;
			} else {
				String gender_str = human.get("g");
				if (gender_str == null) {
					gender1 = 8;
				} else {
					if (gender_str.equals("m")) {
						gender1 = 0;
					} else if (gender_str.equals("f")) {
						gender1 = 1;
					}
				}
			}
			human = as.humanProp.getHuman(names[1]);
			int gender2 = 9;
			if (human == null) {
				gender2 = 9;
			} else {
				String gender_str = human.get("g");
				if (gender_str == null) {
					gender2 = 8;
				} else {
					if (gender_str.equals("m")) {
						gender2 = 0;
					} else if (gender_str.equals("f")) {
						gender2 = 1;
					}
				}
			}
			if (gender1 == 9 || gender2 == 9) {
				counts[2] = 9;
			} else if (gender1 == 8 || gender2 == 8) {
				counts[2] = 8;
			} else {
				counts[2] = gender1 * 2 + gender2;
			}
			if (result[3].equals("1")) {
				convert.put(key, counts.clone());
			} else {
				hasredirect.put(key, counts.clone());
			}
		}
		cache.put("pastMap", convert);
		cache.put("redirPastMap", hasredirect);
		if (convert.getRootMap().size() == size) {

			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity entity = new Entity("past", "default");
			StringBuilder pmsb = new StringBuilder();
			pmsb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>");

			for (String key : convert.getRootMap().keySet()) {
				pmsb.append("<p n=\"" + key + "\" i=\""
						+ convert.getRootMap().get(key)[0] + "|"
						+ convert.getRootMap().get(key)[1] + "|"
						+ convert.getRootMap().get(key)[2] + "\" />");
			}
			pmsb.append("</root>");
			entity.setProperty("pastMap", new Text(new String(pmsb)));
			if(false)datastore.put(entity);
		}

		if (which)
			return convert.getRootMap();
		else
			return hasredirect.getRootMap();
	}

}

@SuppressWarnings("serial")
class PastMap implements Serializable {
	LinkedHashMap<String, int[]> rootMap;

	PastMap() {
		this.rootMap = new LinkedHashMap<String, int[]>(16384);
	}

	public void put(String key, int[] clone2) {
		rootMap.put(key, clone2);
	}

	public LinkedHashMap<String, int[]> getRootMap() {
		return rootMap;
	}

	public boolean containsKey(String key) {
		if (rootMap.containsKey(key))
			return true;
		else
			return false;
	}

	PastMap(LinkedHashMap<String, int[]> root) {
		this.rootMap = root;
	}
}