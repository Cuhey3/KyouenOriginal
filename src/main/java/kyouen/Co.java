package kyouen;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import net.sf.jsr107cache.*;
import static kyouen.WikiUtil.*;

public class Co extends HttpServlet {
	private static final long serialVersionUID = 1L;
	HttpServletRequest req;
	HttpServletResponse resp;
	boolean nonredirectFlag;
	int shift;
	int[] maxCount;
	Integer sideCount;
	String flush;
	String[] nameArray;
	String[][] content, series;
	String[][][] record;
	Cache cache;
	boolean force, ipFlag;
	long time;
	final Pattern p_paren = Pattern.compile("\\[\\[.*?\\]\\]");

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		this.req = req;
		this.resp = resp;
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		maintenanceFlow();
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		this.req = req;
		this.resp = resp;
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		maintenanceFlow();
	}

	/* 変数の初期化、nameSet呼び出し、メモリクリア */
	public void initialize() throws IOException {
		resp.getWriter().println(
				"<!-- initialize_start: " + (System.currentTimeMillis() - time)
						+ "-->");
		maxCount = new int[2]; // 検索結果（件数） 0.完全一致 1.部分一致
		nameArray = new String[4]; // 0.先に入力された名前 1.辞書的に早い方 2.遅い方 3.サイドバー用の入力
		record = new String[3][][]; // 0は不使用。1と2のみ
		/* クローラーのIPをはじくための処理（不完全） */
		cache = WikiUtil.getCache(60 * 3);
		String ipString = req.getRemoteAddr() != null ? req.getRemoteAddr()
				: "";
		Pattern p_ip = Pattern.compile("("
				+ "49\\.134\\.1(17\\.162|39\\.200|42\\.7|46\\.39)"
				+ "|66\\.249\\.(71\\.(23|5)1|68\\.234|67\\.85)"
				+ "|207\\.46\\.13\\.147|65\\.52\\.108\\.25)");
		ipFlag = p_ip.matcher(ipString).find() ? true : false;

		String[] validatedInput = WikiUtil.getValidatedInput(
				req.getParameter("n"), req.getParameter("m"));
		if (validatedInput[3].equals("0")) {
			nonredirectFlag = false;
			redirectToCo(validatedInput, resp);
		} else {
			nonredirectFlag = true;
			nameArray[0] = validatedInput[0];
			nameArray[1] = validatedInput[1];
			nameArray[2] = validatedInput[2];
			content = new String[3][];
			series = new String[3][];

			/* サイドメニューで使用する名前をnameArray[3]にセット */
			String radioName = req.getParameter("rn") == null ? "" : req
					.getParameter("rn").replaceAll("[ 　]+", "").trim();
			nameArray[3] = radioName.equals("") ? (req.getParameter("s") == null ? ""
					: req.getParameter("s").replaceAll("[ 　]+", "").trim())
					: radioName;
			/* ここまで */

			if (nameArray[2].equals("")) // シングル検索の時はnameArray[1]をnameArray[3]に適用
				nameArray[3] = nameArray[1];

			/* メモリクリア関係の処理。force はgetRecordで使う。 */
			if (force) {
				if (req.getParameter("c").equals("clear"))
					cache.clear();
				else if (req.getParameter("c").equals("this"))
					cache.remove(nameArray[1] + "," + nameArray[2]);
			}
			/* ここまで */

			/* 変数shift はgetRecordに引数として渡し、recordの取得位置をずらす。 */
			shift = req.getParameter("i") == null ? 0 : Integer.parseInt(req
					.getParameter("i"));
			/* ここまで */

			flush = (String) (cache.get(nameArray[1] + "," + nameArray[2])); // sideOutputとmessageを含まないキャッシュを取得
			sideCount = (Integer) (cache.get(nameArray[1] + "," + nameArray[2] // sideOutputに必要な変数sideCountを取得
					+ ",count"));
			resp.getWriter().println(
					"<!-- initialize_end: "
							+ (System.currentTimeMillis() - time) + "-->");
		}
	}

	/* initialize() ここまで */

	public void mainFlow() throws IOException {
		StringBuilder headersb = new StringBuilder();
		headerWrite(Mode.Co, nameArray, headersb);
		resp.getWriter().println(
				"<!-- mainflow_start: " + (System.currentTimeMillis() - time)
						+ "-->");
		// 出力キャッシュが揃っていなければメインの処理をする

		if (flush == null || sideCount == null || flush.isEmpty()) {
			StringBuilder sb1 = new StringBuilder(); // 出力優先度の高いStringBuilder
			StringBuilder sb2 = new StringBuilder(); // 出力優先度の低いStringBuilder
			/* メインの処理ここから */
			Record rc = new Record();

			record[1] = rc.getRecord(nameArray[1], shift, force);
			if (nameArray[2].isEmpty()) {
				setContent();
				/* シングル処理 */
				singleOut(sb1, sb2);
				sideCount = record[1][0].length;
				/* シングル処理終了 */
			} else {
				/* ダブル処理 */
				record[2] = rc.getRecord(nameArray[2], shift, force);
				setContent();
				doubleSearch(sb1, sb2);
				resp.getWriter().println(
						"<!-- doubleend: "
								+ ((System.currentTimeMillis() - time) * -1)
								+ "-->");
				if (!ipFlag && (maxCount[0] != 0 || maxCount[1] != 0)) // 検索結果が1件以上あれば検索履歴に書き込む
					new Past().putPastAndRank(nameArray, maxCount, sb1);
				resp.getWriter().println(
						"<!-- putpastend: "
								+ ((System.currentTimeMillis() - time) * -1)
								+ "-->");
				sideCount = maxCount[0] + maxCount[1];
				/* ダブル処理終了 */
			}
			/* シングル・ダブル共通処理 */
			flush = new String(sb1) + new String(sb2);
			cache.put(nameArray[1] + "," + nameArray[2], flush);
			cache.put(nameArray[1] + "," + nameArray[2] + ",count", sideCount);
			/* 共通処理終了 */
		}
		StringBuilder sb0 = new StringBuilder(); // sideOutput用
		new Past().sideOutput(sb0, sideCount, nameArray); // サイドメニューはキャッシュに含めないので逐次書き出す
		resp.getWriter().println(
				"<!-- sideoutend: "
						+ ((System.currentTimeMillis() - time) * -1) + "-->");
		resp.getWriter()
				.println(new String(headersb) + flush + new String(sb0));
	}

	public void singleOut(StringBuilder sb1, StringBuilder sb2)
			throws IOException {
		tableWrite(Prop.single_Head, sb1, sb2); // レコードはあるのでテーブルを書く
		/* レコード出力 */
		for (int i = 0; i < record[1][0].length; i++) {
			sb2.append("<tr><td class=\"genre\">")
					.append(record[1][2][i])
					.append("</td><td class=\"year\">")
					.append(record[1][3][i])
					.append("</td><td>")
					.append(record[1][4][i].equals(record[1][5][i]) ? ""
							: replaceWithWikiLink(record[1][4][i]))
					.append("</td><td class=\"title\">")
					.append(replaceWithWikiLink(record[1][5][i]))
					.append("</td><td class=\"name\"><span"
							+ contentIn(record[1][7][i], record[1][5][i], 1)
							+ ">")
					.append(cleanCharName(getClearTitle(record[1][5][i]),
							replaceWithWikiLink(record[1][6][i])))
					.append("</span></td><tr>");
		}
		/* レコード出力終了 */
		tableWrite(Prop.foot, sb1, sb2); // テーブルを完成させてメソッド終了
	}

	public void doubleSearch(StringBuilder sb1, StringBuilder sb2)
			throws IOException {
		resp.getWriter().println(
				"<!-- doublestart: "
						+ ((System.currentTimeMillis() - time) * -1) + "-->");
		maxCount[0] = 0;
		maxCount[1] = 0;
		/* ここから完全一致 */
		tableWrite(Prop.double_Head, sb1, sb2);
		for (int i = 0; i < record[1][0].length; i++) {
			if (!record[1][8][i].isEmpty())
				for (int j = 0; j < record[2][0].length; j++) {
					if (record[2][8][j].isEmpty())
						continue;
					if (record[1][1][i].equalsIgnoreCase(record[2][1][j])
							|| record[1][1][i]
									.equalsIgnoreCase(record[2][2][j])
							|| record[1][2][i]
									.equalsIgnoreCase(record[2][2][j]))
						if (record[1][3][i].equals(record[2][3][j])
								|| record[1][3][i].isEmpty()
								|| record[2][3][j].isEmpty()
								|| (record[1][3][i].compareTo(record[2][3][j]) < 0 && p_bold
										.matcher(record[1][6][i]).find())
								|| (record[1][3][i].compareTo(record[2][3][j]) > 0 && p_bold
										.matcher(record[2][6][j]).find()))
							if (record[1][8][i]
									.equalsIgnoreCase(record[2][8][j])) {
								sb2.append("<tr><td class=\"genre\">")
										.append(record[1][2][i])
										.append("</td><td class=\"year\">")
										.append(record[1][3][i]
												.compareTo(record[2][3][j]) > 0 ? record[1][3][i]
												: record[2][3][j])
										.append("</td><td class=\"as\">");
								getAssociateLink(record[1][2][i],
										record[1][5][i], sb2);
								sb2.append(
										"</td><td class=\"title\" colspan=\"2\">")
										.append(replaceWithWikiLink(record[1][5][i]))
										.append("</td><td class=\"name\"><span"
												+ contentIn(record[1][7][i],
														record[1][5][i], 1)
												+ ">")
										.append(cleanCharName(
												getClearTitle(record[1][5][i]),
												replaceWithWikiLink(record[1][6][i])))
										.append("</span></td><td class=\"name\"><span"
												+ contentIn(record[2][7][j],
														record[1][5][i], 2)
												+ ">")
										.append(cleanCharName(
												getClearTitle(record[2][5][j]),
												replaceWithWikiLink(record[2][6][j])))
										.append("</td><tr>");
								record[1][5][i] = "";
								record[2][5][j] = "";
								record[1][6][i] = "";
								record[2][6][j] = "";
								record[1][7][i] = "";
								record[2][7][j] = "";
								record[1][8][i] = "";
								record[2][8][j] = "";
								maxCount[0]++;
								break;
							}
				}
		}
		resp.getWriter().println(
				"<!-- compend: " + ((System.currentTimeMillis() - time) * -1)
						+ "-->");
		sb1.append("<i><span style=\"color:#ee0303;\">" + maxCount[0]
				+ "件がヒットしました。");
		/* 完全一致ここまで */

		/* 部分一致ここから */
		tableWrite(Prop.Middle, sb1, sb2);
		for (int i = 0; i < record[1][0].length; i++) {
			if (!record[1][7][i].isEmpty())
				for (int j = 0; j < record[2][0].length; j++) {
					if (record[1][7][i].equals(record[2][7][j])) {
						sb2.append("<tr><td class=\"genre\">").append("部分一致")
								.append("</td><td class=\"year\">").append("")
								.append("</td><td class=\"as\">");
						getAssociateLink(record[1][2][i], record[1][5][i], sb2);
						sb2.append("</td><td class=\"title\">")
								.append(replaceWithWikiLink(record[1][5][i]))
								.append("/")
								.append(record[1][2][i])
								.append("/")
								.append(record[1][3][i])
								.append("</td><td class=\"title\">")
								.append(replaceWithWikiLink(record[2][5][j]))
								.append("/")
								.append(record[2][2][j])
								.append("/")
								.append(record[2][3][j])
								.append("</td><td class=\"name\"><span"
										+ contentIn(record[1][7][i],
												record[1][5][i], 1) + ">")
								.append(cleanCharName(
										getClearTitle(record[1][5][i]),
										replaceWithWikiLink(record[1][6][i])))
								.append("</td><td class=\"name\"><span"
										+ contentIn(record[2][7][j],
												record[1][5][i], 2) + ">")
								.append(cleanCharName(
										getClearTitle(record[2][5][j]),
										replaceWithWikiLink(record[2][6][j])))
								.append("</td><tr>");
						record[2][7][j] = "";
						maxCount[1]++;
						break;
					}
				}
		}
		sb1.append("（部分一致:" + maxCount[1] + "件）</span></i>");
		if (content[1] != null && content[2] != null)
			sb1.append("　お知らせ：現在、役名を<span style=\"color:#777;font-style:italic;font-size:90%;\">薄字</span>、通常、<b>太字</b>の三段階で表示するテストを行っています。");
		/* 部分一致ここまで */
		// テーブルを完成させてメソッド終了
		tableWrite(Prop.foot, sb1, sb2);
		resp.getWriter().println(
				"<!-- partend: " + ((System.currentTimeMillis() - time) * -1)
						+ "-->");
	}

	public void tableWrite(Prop p, StringBuilder sb1, StringBuilder sb2)
			throws IOException {
		switch (p) {
		case single_Head:
			sb1.append("<div id=\"wrap\"><div id=\"contents\"><h2>")
					.append(nameArray[1].split("_\\(")[0])
					.append(" 出演リスト</h2>");
			sb2.append(
					"<table border=\"1\" cellspacing=\"0\"><tr><th class\"genre\">ジャンル</th><th class\"year\">年</th><th>シリーズ名</th>")
					.append("<th class\"title\">タイトル</th><th class\"name\"><a target=\"_blank\" href=\"http://ja.wikipedia.org/wiki/")
					.append(nameArray[1]).append("\">")
					.append(nameArray[1].split("_\\(")[0])
					.append("</a></th></tr>");
			break;
		case double_Head:
			sb1.append("<div id=\"wrap\"><div id=\"contents\"><h2>")
					.append(nameArray[1].split("_\\(")[0]).append("＆")
					.append(nameArray[2].split("_\\(")[0])
					.append(" 共演リスト</h2>");
			sb2.append(
					"<table border=\"1\" cellspacing=\"0\"><tr><th class=\"genre\">ジャンル</th><th class=\"year\">年</th>")
					.append("<th class=\"as\">商品</th><th class=\"title\"colspan=\"2\">共演タイトル</th><th class\"name\">")
					.append("<a target=\"_blank\" href=\"http://ja.wikipedia.org/wiki/")
					.append(nameArray[1])
					.append("\">")
					.append(nameArray[1].split("_\\(")[0])
					.append("</a></th><th class\"name\"><a target=\"_blank\" href=\"http://ja.wikipedia.org/wiki/")
					.append(nameArray[2]).append("\">")
					.append(nameArray[2].split("_\\(")[0])
					.append("</a></th></tr>");
			break;
		case Middle:
			sb2.append("<tr><th class\"genre\">種別</th>")
					.append("<th class\"year\"></th><th class=\"as\">商品</th><th class\"title\">部分一致タイトル1 / ジャンル / 年</th>")
					.append("<th class\"title\">部分一致タイトル2 / ジャンル / 年</th><th class\"name\"><a href=\"http://ja.wikipedia.org/wiki/")
					.append(nameArray[1])
					.append("\">")
					.append(nameArray[1].split("_\\(")[0])
					.append("</a></th><th class\"name\"><a target=\"_blank\" href=\"http://ja.wikipedia.org/wiki/")
					.append(nameArray[2]).append("\">")
					.append(nameArray[2].split("_\\(")[0])
					.append("</a></th></tr>");
			break;
		case foot:
			sb2.append("</table></div>");
			break;
		}
	}

	public enum Prop {
		single_Head, double_Head, Middle, foot
	}

	public String contentIn(String l, String r, int i) {
		if (parenNotMatch(r))
			return "";
		if (content[i] == null)
			return "";
		for (String c : content[i]) {
			if (l.equals(c))
				return "";
		}
		if (series[i] == null)
			return "";
		for (String s : series[i]) {
			if (s.isEmpty())
				continue;
			Pattern p = Pattern.compile(s, 82);
			if (p.matcher(l).find())
				return "";
		}
		return " style=\"color:#888;font-style:italic;font-size:85%;\"";
	}

	public boolean parenNotMatch(String s) {
		if (p_paren.matcher(s).find())
			return false;
		return true;
	}

	public void setContent() throws IOException {
		String s = (String) cache.get(nameArray[1] + ",content");
		if (s == null) {
			cache.put(nameArray[1] + ",content",
					WikiUtil.getBacklink(nameArray[1]));
			s = (String) cache.get(nameArray[1] + ",content");
		}
		if (s != null && !s.isEmpty() && !s.equals("<<>>")) {
			if (s.replaceAll("^(.*)<<>>$", "").isEmpty())
				series[1] = null;
			else
				series[1] = s.split("<<>>")[1].split("<>");
			content[1] = s.replaceAll("^(.*)<<>>$", "$1").split("<>");
		} else {
			content[1] = null;
			series[1] = null;
		}
		if (!nameArray[2].isEmpty()) {
			s = (String) cache.get(nameArray[2] + ",content");
			if (s == null) {
				cache.put(nameArray[2] + ",content",
						WikiUtil.getBacklink(nameArray[2]));
				s = (String) cache.get(nameArray[2] + ",content");
			}
			if (s != null && !s.isEmpty() && !s.equals("<<>>")) {
				if (s.replaceAll("^(.*)<<>>$", "").isEmpty())
					series[2] = null;
				else
					series[2] = s.split("<<>>")[1].split("<>");
				content[2] = s.replaceAll("^(.*)<<>>$", "$1").split("<>");
			} else {
				content[2] = null;
				series[2] = null;
			}
		}
		if (content[1] == null
				|| (!nameArray[2].isEmpty() && content[2] == null))
			throw new IOException();
	}

	@SuppressWarnings("rawtypes")
	public void maintenanceFlow() throws IOException {
		if (req.getParameter("c") != null)
			force = true;
		else
			force = false;
		ServerStatus serverStatus = new ServerStatus();
		String other_sv = serverStatus.otherServerID;
		boolean maintenance = serverStatus.maintenance; // メンテのフラグ
		String val = "?";
		Enumeration names = req.getParameterNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			val = val + "&" + name + "="
					+ URLEncoder.encode(req.getParameter(name), "utf-8");
		}
		val = val.replaceAll("\\?&", "?");
		String debag = req.getParameter("debag");
		if (debag != null || force) { // debag でない かつ force = true
			time = System.currentTimeMillis();
			initialize();
			if (nonredirectFlag)
				mainFlow();
		} else {
			try {
				if (maintenance) // メンテならやりきらない
					throw new Throwable();
				time = System.currentTimeMillis();
				initialize();
				if (nonredirectFlag)
					mainFlow();
			} catch (Throwable t) {
				if (maintenance) {
					/* メンテ中の例外はリダイレクトする */
					String url = "http://" + other_sv + ".appspot.com"
							+ req.getServletPath() + val;
					resp.sendRedirect(url);
				} else {
					/* メンテ中でない例外はメッセージを表示し、ログに書き込む */
					resp.getWriter().println("<h1>ERROR:</h1><br />");
					resp.getWriter()
							.println(
									"申し訳ありません。処理が正常に終了しませんでした。"
											+ "<ul><li><p>検索語を確認してください。</p>"
											+ "<ul><li><p>人物の特定に、検索エンジンおよびWikipediaを使用しています。<br />Wikipediaに希望する人物の記事（リダイレクトでない、単独の記事）があることを確認してください。</p></li>"
											+ "<li><p>Wikipediaに記事がある場合、<br />正確な人物名の他に ふりがなでの検索が有効です。こちらもお試しください。</p></li></ul>"
											+ "<li><p>アクセスが混み合っている場合があります。時間をおいて再度アクセスしていただくようお願いします。</p></li>"
											+ "<li><p>その他に、解消されない不具合を発見されましたら、お手数ですが、<br /><strong>uotaneet@gmail.com</strong> あるいは <a href=\"http://twitter.com/uotaneet\">http://twitter.com/uotaneet</a> までご連絡ください。</p></li></ul>"
											+ "<br /><a href=\"/\">TOPへ戻る</a><br /><br />");
					DatastoreService datastore = DatastoreServiceFactory
							.getDatastoreService();
					Entity serv = null;
					try {
						serv = datastore.get(KeyFactory.createKey(
								"server_status", "this"));
					} catch (EntityNotFoundException e) {
						serv = new Entity("server_status", "this");
						serv.setProperty("maintenance_str", "false");
					}
					String log;
					try {
						log = ((Text) serv.getProperty("log")).getValue();
					} catch (Throwable s) {
						log = "";
					}
					if (log == null)
						log = "";
					serv.setProperty("log", new Text(req.getServletPath()
							+ URLDecoder.decode(val, "utf-8") + "," + log));
					//if (false)
						datastore.put(serv);
				}
			}
		}
	}

	/* リダイレクトの必要があると判定した後に、正規化済みのinputを元にリダイレクトをするメソッド */
	/* Coサーブレットにロックなコードなので流用不可 */
	protected void redirectToCo(String[] validatedInput,
			HttpServletResponse resp) throws IOException {
		// エンコード。"(" と ")" を戻している。
		// 前工程で正規化が済んでいる点に注意。
		String encodedInput1 = URLEncoder.encode(validatedInput[1], "utf-8")
				.replaceAll("%28", "(").replaceAll("%29", ")");
		String encodedInput2 = URLEncoder.encode(validatedInput[2], "utf-8")
				.replaceAll("%28", "(").replaceAll("%29", ")");

		// 以下のリダイレクトルールは、
		// あくまで前工程で "リダイレクトが必要" と判定された上での物であり
		// 例えば、入力チェックする前からいきなり使うみたいなことはできない。
		if (validatedInput[1].isEmpty() && validatedInput[2].isEmpty()) {
			// 両方とも空
			resp.sendRedirect("/");
		} else if (validatedInput[1].isEmpty())
			// input[1] が空
			resp.sendRedirect("/co?n=" + encodedInput2);
		else if (validatedInput[2].isEmpty())
			// input[2] が空
			resp.sendRedirect("/co?n=" + encodedInput1);
		else
			// 両方とも正規化済みのinput
			resp.sendRedirect("/co?n=" + encodedInput1 + "&m=" + encodedInput2);
	}
}