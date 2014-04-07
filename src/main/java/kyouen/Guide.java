package kyouen;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.*;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;

@SuppressWarnings("serial")
public class Guide extends HttpServlet {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		Cache cache = null;
		try {
			CacheFactory cacheFactory = CacheManager.getInstance()
					.getCacheFactory();
			Map props = new HashMap();
			// 完全キャッシュ有効期間15分
			props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 15);
			cache = cacheFactory.createCache(props);
		} catch (CacheException e) {
		}
		String column = (String) cache.get("namecolumn");
		if (column == null) {
			WakeBackends wb = new WakeBackends();
			wb.newColumn();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(
				"<html><head><title>共演検索マシーン - 共演の基準</title><meta name=\"viewport\" content=\"user-scalable=no\" /><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />")
				.append("<meta name=\"description\" content=\"共演検索マシーンの説明です。\">")
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
				.append(column).append("</select></div></form></div><div id=\"wrap\">")
				.append("<br /><div id=\"space\"></div>")
				.append("<div id=\"guide\"><h2>ガイド - 共演の基準</h2>")
				.append("共演検索マシーンが「共演作品である」と判断する際の基準をご説明します。")
				.append("<br />Wikipediaの記事を比較する際に、以下の三点をチェックしています。<ol><li>作品のタイトル</li><li>作品のカテゴリ</li><li>公開された年</li></ol>")
				.append("<dl><dt>作品のタイトルについて</dt><dd>Wikipediaの記事の中で、出演作品リストと判断されたブロックの各行から、</dd>")
				.append("<dd>キャラクター名と判断される部分を除いた文字列を作品タイトルと判断しており、これが両者で一致する必要があります。</dd>")
				.append("<dd>この時、記号文字の差異・アルファベットの大小はある程度許容されます。スペースの有無、-と〜など。</dd>")
				.append("<dd>許容されない記号文字としては!や?があります。</dd>")
				.append("<dt>作品のカテゴリについて</dt>")
				.append("<dd>「テレビアニメ」「劇場アニメ」「ドラマCD」「ゲーム」等を指します。</dd>")
				.append("<dd>Wikipediaの見出しからこれらを取得し、各出演作品に付加しています。このカテゴリが両者で一致する必要があります。</dd>")
				.append("<dd>Wikipediaでは、同一の作品でも記事によってカテゴリの表記がゆれます。</dd>")
				.append("<dd>イベントの様子を収めたDVDソフトを「実写」とするか「その他」とするかは記事によります。</dd>")
				.append("<dd>また海外制作のテレビアニメを吹き替えた際に「吹き替え（アニメ）」とするか単に「テレビアニメ」とするか等でも違いがあります。</dd>")
				.append("<dd>これらの差異を、現状では一部のケースを除き許容（修正）できていません。（後述の“部分一致”でフォローしています。）</dd>")
				.append("<dd>基本的には両者のカテゴリ名が同一である必要があります。</dd>")
				.append("<dt>公開された年について</dt>")
				.append("<dd>Wikipediaでは、テレビアニメであれば放映年（正確には、対象の声優が出演した回の放映年）、</dd><dd>ゲームであれば発売年、映画であれば公開年が各出演作品には付加されています。</dd>")
				.append("<dd>これも参照しており、そのルールはやや複雑です。以下のいずれかを満たせばよいものとします。</dd>")
				.append("<dd><ul>" +
						"<li>公開された年が完全に同一である。たとえば2005年・2005年など。</li>" +
						"<li>公開された年が空欄である。片方、あるいは両方。たとえば2007年・（空欄）。</li>" +
						"<li>片方、あるいは両方の役名が“太字”、主要なキャラクターであり、なおかつ、太字でない方のキャラクターは、太字のキャラクターよりも後の年に登場している。</li></ul></dd>")
				.append("<dd>条件を満たさないのは、違う年が明記されており、なおかつお互いが主要キャラクターでない場合。</dd>")
				.append("<dd>あるいは、違う年が明記されており、なおかつ非主要キャラクターが主要キャラクターよりも前の年に登場している場合です。</dd></dl>")
				.append("<br />タイトル・カテゴリ・放映年と、全ての条件を満たしたものが「完全一致」となり、この件数が簡易履歴に表示されます。また、ランキングにも登録されます。")
				.append("<h2>部分一致</h2>")
				.append("表記のゆれなどの理由で、本来は共演しているのに漏れてしまったタイトルがあります。それらをすくい上げる意味で、部分一致カテゴリを用意しています。<br />")
				.append("部分一致が参照しているのは<i>“Wikipediaで各出演タイトルに関連づけられているリンク先”</i>です。<br />")
				.append("桑谷夏子さんの出演タイトルに「魔法先生ネギま! 〜白き翼 ALA ALBA〜」がありますが、Wikipediaでタイトル名をクリックすると<br />")
				.append("Wikipediaの記事「魔法先生ネギま!_(OVA)」にジャンプします。共演検索マシーンでは、このジャンプ先の記事が同一であれば、")
				.append("<br />いかにタイトル・カテゴリ・公開された年が違っていても“部分一致”としてリストの下段に表示します。")
				.append("<h2>当サイトの「共演」の定義</h2>")
				.append("以上のことから、当サイトでいう「共演」とはかなり範囲の広い言葉になっています。<br />")
				.append("厳密には共演とは、「同じスタジオで一緒にアフレコした」ということを指すのだと思いますが、<br />")
				.append("当サイトでは共演とは、<b>「同一カテゴリの同一タイトルに、同じ年に関わっていた（あるいは違う年に関わっていた）」</b>ということを指します。<br />")
				.append("これはWikipediaをソースとしていることからくる仕様です。<br />ご利用の際や、話題にされる際はどうかこの点にご留意ください。")
				.append("</div></div></body></html>");
		resp.getWriter().println(new String(sb));
	}
}
