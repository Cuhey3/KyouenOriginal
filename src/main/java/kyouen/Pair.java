package kyouen;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.*;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;

@SuppressWarnings("serial")
public class Pair extends HttpServlet {
	String namelist;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		String force_string = req.getParameter("c");
		boolean force;
		if (force_string == null)
			force = false;
		else
			force = true;
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past_string = null;
		try {
			past_string = datastore.get(KeyFactory.createKey("past_string",
					"onlyOne"));
		} catch (EntityNotFoundException e) {
		}
		if (req.getParameter("mokyu") != null) {
			resp.getWriter().println(
					((Text) past_string.getProperty("pair")).getValue() + "<>"
							+ past_string.getProperty("pair_update"));

		} else {
			Assistant as = new Assistant();
			as.setHumanProp();
			HumanProp hp = as.humanProp;
			LinkedHashMap<String, LinkedHashMap<String, String>> rootMap = hp
					.getRootMap();
			boolean flag = (Boolean) past_string.getProperty("pflag");
			StringBuilder sb = new StringBuilder();
			String[] textSplit;
			String pairString = "";
			String[] updateSplit = ((String) past_string
					.getProperty("pair_update")).split(",");
			if (!flag && !force) {
				pairString = (String) WikiUtil.getCache(60 * 24 * 60).get(
						"pairString");
			} else {
				String text = new Past().getPastString();
				textSplit = text.split(",");
				int now_max = 0;
				int this_vol = 0;
				int where = 0;
				for (int i = 0; i * 4 + 4 < textSplit.length; i++) {
					this_vol = Integer.parseInt(textSplit[i * 4 + 3]);
					if (this_vol > now_max) {
						if (pairMatch(rootMap, textSplit[i * 4 + 1],
								textSplit[i * 4 + 2])) {
							now_max = this_vol;
							where = i;
						} else {
							textSplit[i * 4 + 3] = "0";
						}
					}
				}

				while (now_max != 0) {
					sb.append(",").append(textSplit[where * 4 + 1]).append(",")
							.append(textSplit[where * 4 + 2]).append(",")
							.append(textSplit[where * 4 + 3]).append(",")
							.append(textSplit[where * 4 + 4]);
					pairString = new String(sb);
					textSplit[where * 4 + 3] = "0";
					now_max = 0;
					this_vol = 0;
					where = 0;
					for (int i = 0; i * 4 + 4 < textSplit.length; i++) {
						this_vol = Integer.parseInt(textSplit[i * 4 + 3]);
						if (this_vol > now_max) {
							if (pairMatch(rootMap, textSplit[i * 4 + 1],
									textSplit[i * 4 + 2])) {
								Pattern p_1 = Pattern.compile(Pattern
										.quote(textSplit[i * 4 + 1]));
								Pattern p_2 = Pattern.compile(Pattern
										.quote(textSplit[i * 4 + 2]));
								if (!p_1.matcher(pairString).find()
										&& !p_2.matcher(pairString).find()) {
									now_max = this_vol;
									where = i;
								} else {
									textSplit[i * 4 + 3] = "0";
								}
							} else {
								textSplit[i * 4 + 3] = "0";
							}
						}
					}
				}
				past_string.setProperty("pair", new Text(pairString));
				past_string.setProperty("pflag", false);
				//if (false)
					datastore.put(past_string);
				WikiUtil.getCache(60 * 24 * 60 * 365).put("pairString",
						pairString);
			}
			Cache cache = null;
			try {
				CacheFactory cacheFactory = CacheManager.getInstance()
						.getCacheFactory();
				Map props = new HashMap();
				// 完全キャッシュ有効期間15分
				props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 24 * 60);
				cache = cacheFactory.createCache(props);
			} catch (CacheException e) {
			}
			String column = (String) cache.get("namecolumn");
			if (column == null) {
				WakeBackends wb = new WakeBackends();
				wb.newColumn();
			}
			StringBuilder sb_out = new StringBuilder();
			sb_out.append("<html><head><title>共演数ランキング(男女)</title>")
					.append("<meta name=\"viewport\" content=\"user-scalable=no\" /><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />")
					.append("<meta name=\"description\" content=\"男女ペア限定の、共演数ランキングです。\">")
					.append("<meta name=\"keywords\" content=\"声優,共演,検索,アニメ,ランキング\">")
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
					.append("</select></div></form></div><div id=\"wrap\"><br /><div id=\"space\"></div>")
					.append("<div id=\"contents\"><h2>共演数ランキング(男女)</h2><h3>当サイトで検索された共演の、共演数ランキングです。<i><span style=\"color:#ee0303;\">（男女ペア限定！）</span></i></h3>")
					.append("<dl><dt>ルール1</dt><dd>完全一致の件数のみを参照します。</dd>")
					.append("<dt>ルール2</dt><dd>同一人物はランキングに一度しか登場できません。</dd></dl>")
					.append("<p>注意点として、これまで検索されたことがなかった方の場合、管理人が性別の項目を手動で付けているため、<br />")
					.append("反映されるまでにタイムラグが発生します。プルダウンリストに登録された＝男女ランキングに参加可能、となります。</p>")
					.append("<table border=\"1\"><tr><th>順位</th><th>共演者名</th><th>完全一致(件)</th><th>部分一致(件)</th><th>更新</th></tr>");
			textSplit = null;
			textSplit = pairString.split(",", 1201);
			String blankOrNew = null;
			for (int i = 0; i < 300; i++) {
				try {
					for (int j = 0; j < updateSplit.length; j++) {
						if (updateSplit[j].equals(textSplit[i * 4 + 1] + "＆"
								+ textSplit[i * 4 + 2])) {
							blankOrNew = "<span style=\"color:#ee0303;\">New!</span>";
							break;
						} else
							blankOrNew = "";
					}
					sb_out.append("<tr><td>").append(i + 1)
							.append("</td><td><a href=\"co?n=")
							.append(textSplit[i * 4 + 1]).append("&m=")
							.append(textSplit[i * 4 + 2]).append("\">");
					if (getMF(rootMap, textSplit[i * 4 + 1]).equals("m")) {
						sb_out.append(textSplit[i * 4 + 1]).append("＆")
								.append(textSplit[i * 4 + 2]);
					} else {
						sb_out.append(textSplit[i * 4 + 2]).append("＆")
								.append(textSplit[i * 4 + 1]);
					}
					sb_out.append("</a></td>").append("<td class=\"count\">")
							.append(textSplit[i * 4 + 3])
							.append("</td><td class=\"count\">")
							.append(textSplit[i * 4 + 4])
							.append("</td><td>" + blankOrNew + "</td></tr>");
				} catch (Exception e) {
					break;
				}
			}
			sb_out.append("</table></div></div></div></body></html>");
			resp.getWriter().println(new String(sb_out));
		}
	}

	public boolean pairMatch(
			LinkedHashMap<String, LinkedHashMap<String, String>> humanPropRoot,
			String correctName1, String correctName2) {
		if (humanPropRoot.containsKey(correctName1)
				&& humanPropRoot.containsKey(correctName2)) {
			String gender1 = humanPropRoot.get(correctName1).get("g");
			String gender2 = humanPropRoot.get(correctName2).get("g");
			if (gender1 == null || gender2 == null)
				return false;
			if (gender1.equals("m") && gender2.equals("f"))
				return true;
			if (gender1.equals("f") && gender2.equals("m"))
				return true;
			return false;
		} else
			return false;
	}

	public String getMF(
			LinkedHashMap<String, LinkedHashMap<String, String>> humanPropRoot,
			String correctName) {
		return humanPropRoot.get(correctName).get("g");
	}
}