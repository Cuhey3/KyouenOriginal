package kyouen;

import static kyouen.WikiUtil.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsr107cache.Cache;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

public class Record {
	String[][] record;
	String[] status;
	String record_string;
	int maxCount;
	final Pattern p_skip1 = Pattern.compile("(出典|注釈|脚注|外部リンク)");
	final Pattern p_year = Pattern.compile("^''' ?([0-9]+年) ?'''$");
	final Pattern p_theater = Pattern.compile("^劇場版アニメ$");
	final Pattern p_fukikae = Pattern
			.compile("^(吹き替え[（\\(][^\\(\\)（）]+[\\)）]|海外ドラマ)$");
	final Pattern p_skip1_el = Pattern
			.compile("(概要|人物|エピソード|プライベート|生い立ち|時代|受賞歴|特技|趣味|嗜好|"
					+ "交[遊友流]|[来経略]歴|特色|逸話|関わり|関連|声優|歌手(活動)?)");
	final Pattern p_skip5 = Pattern
			.compile("(太字'''は([^<>])*?(キャラクター|主役|放送中)|==[ 　]*($|※)|"
					+ "===|^[0-9]{4}年'''$|^時期未定'''$|^[^']+'''$)");
	final Pattern p_y_early = Pattern.compile("^(19)?([2-9][0-9])年?$");
	final Pattern p_y_late = Pattern.compile("^(20)?([0-1][0-9])年?$");

	public String[][] getRecord(String name, int shift, boolean force)
			throws IOException {
		maxCount = 0;
		record = null;
		boolean putflag = false;
		Cache cache = null;
		cache = getCache(3600 * 24 * 1);
		DatastoreService datastore = null;
		Entity seiyu = null;

		String namelist = WakeBackends.namelist;
		if (namelist.split(name.split("[　_＿]")[0] + ",").length == 2) {
			if (namelist.split(name.split("[　_＿]")[0] + ",")[1].split(",")[0]
					.equals("&i=1"))
				shift = 1;
			else if (!namelist.split(name.split("[　_＿]")[0] + ",")[1]
					.split(",")[0].isEmpty())
				name = name.split("[　_＿]")[0]
						+ namelist.split(name.split("[　_＿]")[0] + ",")[1]
								.split(",")[0];
		}
		String content_string = (String) (cache.get(name + ",content"));
		if (content_string == null || content_string.isEmpty() || force) {
			datastore = DatastoreServiceFactory.getDatastoreService();
			Key key = KeyFactory.createKey("seiyu", name);
			try {
				seiyu = datastore.get(key);
			} catch (Throwable t) {
				seiyu = new Entity("seiyu", name);
			}
			try {
				content_string = ((Text) seiyu.getProperty("content"))
						.getValue();
				if (content_string == null || force)
					throw new Throwable();
			} catch (Throwable t) {
				content_string = WikiUtil.getBacklink(name);
				seiyu.setProperty("content", new Text(content_string));
				// if(false)
				datastore.put(seiyu);
			}
			cache.put(name + ",content", content_string);
		}
		try {
			record_string = (String) (cache.get(name + ",record"));
			if (!force) {
				if (record_string != null && !record_string.isEmpty()) {
					stringToArray(record_string);
					return record; // cacheが nullでない かつ forceでない とき return
				} else {
					datastore = DatastoreServiceFactory.getDatastoreService();
					Key key = KeyFactory.createKey("seiyu", name);
					seiyu = datastore.get(key);
					Text text = (Text) seiyu.getProperty("record");
					cache.put(name + ",record", text.getValue());
					stringToArray(text.getValue());
					return record; // cacheが null かつ forceでない かつ date + 12h
									// > now のとき return
				}
			}
			// ここから先は forceのとき あるいは
			// あるいは cache が nullのとき かつ cache が古いとき の処理　（もう変えた）
			// if (!force)

			throw new Throwable();
		} catch (Throwable t) {
			putflag = true;
			record = new String[9][2000];
			status = new String[9];
			String text = getTextarea(name);
			multiEq(text, 2 + shift, shift);
			StringBuilder sb = new StringBuilder();
			int i;
			for (i = 0; record[0][i] != null; i++) {
				for (int j = 0; j < 9; j++) {
					sb.append("<>" + record[j][i]);
				}
			}
			String make_string = new String(sb);
			Integer mf = WikiUtil.getMF(name);
			cache.put(name + ",record", mf + "" + make_string);
			try {
				if (putflag) {
					datastore = DatastoreServiceFactory.getDatastoreService();
					Key key = KeyFactory.createKey("seiyu", name);
					try {
						seiyu = datastore.get(key);
					} catch (Throwable u) {
						seiyu = new Entity("seiyu", name);
					}
					seiyu.setProperty("mf", mf);
					seiyu.setProperty("record", new Text(mf + "" + make_string));
					seiyu.setProperty("date", System.currentTimeMillis());
					// if(false)
					datastore.put(seiyu);
				}
			} catch (Throwable s) {
			}
			stringToArray(make_string);
			return record;
		}
	}

	public void multiEq(String block, int mode, int shift) throws IOException {
		for (String div_block : block.split("<br />={" + mode + "} ")) {
			status[mode - 2 - shift] = div_block.split("=", 2)[0].replaceAll(
					" $", "").replaceAll("^.*<br />.*$", "");
			if (mode == 4 + shift)
				tripleQuot(div_block);
			else
				multiEq(div_block, mode + 1, shift);
		}
	}

	public void tripleQuot(String h4_block) throws IOException {
		for (String year_block : h4_block.split("<br />''' ?")) {
			status[3] = year_block.split("'''", 2)[0].replaceAll(" $", "")
					.replaceAll("^.*<br />.*$", "");
			singleAster(year_block);
		}
	}

	public void singleAster(String year_block) throws IOException {
		for (String single_block : year_block.split("<br />:?\\* ")) {
			status[4] = single_block.split("<br />", 2)[0].replaceAll(" $", "")
					.replaceAll("^.*<br />.*$", "");
			doubleAster(single_block);
		}
	}

	public void doubleAster(String single) throws IOException {
		int i = 0;
		for (String double_block : single.split("<br />:?\\*\\* ")) {
			double_block = double_block.replaceAll("<br />", "");
			status[5] = double_block;
			if (i > 0)
				status[4] = "laka前に倣うslk";
			addToArray(status);
			i++;
		}
	}

	public void addToArray(String status[]) throws IOException {
		Matcher m;
		if (p_skip1.matcher(status[1]).find() || status[1].isEmpty()
				|| p_skip5.matcher(status[5]).find())
			;
		else if (status[5].length() > 300)
			;
		else if (maxCount < 10
				&& (p_skip1_el.matcher(status[1]).find()
						|| status[1].equals("{{雑多な内容の箇条書き|date") || status[1]
							.equals("{{存命人物の出典明記|date")))
			;
		else {
			record[0][maxCount] = "";
			record[1][maxCount] = status[1].trim();
			record[2][maxCount] = p_theater.matcher(status[2]).find() ? "劇場アニメ"
					: status[2].trim();
			record[1][maxCount] = p_fukikae.matcher(status[2]).find() ? "吹き替え"
					: record[1][maxCount];
			record[6][maxCount] = getCharName(status[5]);
			record[5][maxCount] = cutString(status[5], record[6][maxCount]);
			record[4][maxCount] = status[4].equals("laka前に倣うslk") ? record[4][maxCount - 1]
					: record[5][maxCount];
			if (status[3].isEmpty()) {
				Matcher m_year = p_year.matcher(record[4][maxCount]);
				if (m_year.find())
					record[3][maxCount] = m_year.group(1);
				else
					record[3][maxCount] = "";
			} else
				record[3][maxCount] = status[3];
			if (record[6][maxCount].isEmpty() && maxCount > 1)
				if (record[4][maxCount].equals(record[4][maxCount - 1])) {
					record[6][maxCount] = record[6][maxCount - 1].trim();
					record[5][maxCount] = status[5].trim();
				}
			record[7][maxCount] = "";
			record[8][maxCount] = "";
			/* ここからエンコード */
			if (record[2][maxCount].equals(record[1][maxCount]))
				record[2][maxCount] = "";
			if (record[4][maxCount].equals(record[5][maxCount]))
				record[5][maxCount] = "";

			if ((m = p_y_late.matcher(record[3][maxCount])).find())
				record[3][maxCount] = m.replaceAll("$2");
			else if ((m = p_y_early.matcher(record[3][maxCount])).find())
				record[3][maxCount] = m.replaceAll("$2");

			if (record[1][maxCount].equals("テレビアニメ"))
				record[1][maxCount] = "a";
			else if (record[1][maxCount].equals("劇場アニメ"))
				record[1][maxCount] = "t";
			else if (record[1][maxCount].equals("ゲーム"))
				record[1][maxCount] = "g";
			else if (record[1][maxCount].equals("ドラマCD"))
				record[1][maxCount] = "d";
			else if (record[1][maxCount].equalsIgnoreCase("Webアニメ"))
				record[1][maxCount] = "w";
			/* エンコードここまで */

			maxCount++;
		}
	}

	public void stringToArray(String str) throws IOException {
		Matcher m;
		String[] textSplit = (str).split("(/~_/|<>)");
		record = new String[9][(textSplit.length - 1) / 9];
		int k = 0;
		for (int j = 1; j + 8 < textSplit.length; k++) {
			record[0][k] = textSplit[j];

			/* [1] */
			record[1][k] = textSplit[j + 1];
			if (record[1][k].equals("a"))
				record[1][k] = "テレビアニメ";
			else if (record[1][k].equals("t"))
				record[1][k] = "劇場アニメ";
			else if (record[1][k].equals("g"))
				record[1][k] = "ゲーム";
			else if (record[1][k].equals("d"))
				record[1][k] = "ドラマCD";
			else if (record[1][k].equals("w"))
				record[1][k] = "Webアニメ";
			/* [2] */

			record[2][k] = (textSplit[j + 2].isEmpty()) ? record[1][k]
					: textSplit[j + 2];
			record[3][k] = textSplit[j + 3];
			if ((m = p_y_late.matcher(record[3][k])).find())
				record[3][k] = "20" + m.replaceAll("$2") + "年";
			else if ((m = p_y_early.matcher(record[3][k])).find())
				record[3][k] = "19" + m.replaceAll("$2") + "年";
			record[4][k] = textSplit[j + 4];
			record[5][k] = (textSplit[j + 5].isEmpty()) ? record[4][k]
					: textSplit[j + 5];
			record[6][k] = textSplit[j + 6];
			record[7][k] = getLinkTitle(record[5][k]).trim();
			record[8][k] = clearMark(record[5][k]);
			j = j + 9;
		}
	}
}