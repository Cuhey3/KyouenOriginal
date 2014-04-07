package kyouen;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.urlfetch.FetchOptions.Builder;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.apphosting.api.ApiProxy;

@SuppressWarnings("serial")
public class Synchro extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		String mode = req.getParameter("mode");
		String sv = ApiProxy.getCurrentEnvironment().getAppId();
		if (sv.equals("seiyufan"))
			sv = "seiyukyouen";
		else
			sv = "seiyufan";
		String[][][] sp_log_box = getLog(mode, sv);

		if (mode.equals("past")) {
			Cache cache = WikiUtil.getCache(60 * 40);
			String past_string = synchroLogAll(sp_log_box);
			String past_update = synchroLogUpdate(sp_log_box);
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity past = null;
			try {
				past = datastore.get(KeyFactory.createKey("past_string",
						"onlyOne"));
			} catch (EntityNotFoundException e) {
			}
			past.setProperty("text", new Text(past_string));
			past.setProperty("past_update", past_update);
			cache.put("past_string", past_string);
			cache.put("past_update", past_update);
			if(false)datastore.put(past);
		} else if (mode.equals("ranking")) {
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity past = null;
			try {
				past = datastore.get(KeyFactory.createKey("past_string",
						"onlyOne"));
			} catch (EntityNotFoundException e) {
			}
			String ranking_update = synchroLogUpdate(sp_log_box);
			past.setProperty("rank_update", ranking_update);
			datastore.put(past);
		} else if (mode.equals("pair")) {
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity past = null;
			try {
				past = datastore.get(KeyFactory.createKey("past_string",
						"onlyOne"));
			} catch (EntityNotFoundException e) {
			}
			String pair_update = synchroLogUpdate(sp_log_box);
			past.setProperty("pair_update", pair_update);
			if(false)datastore.put(past);
		}
	}

	public String[][][] getLog(String mode, String sv) throws IOException {
		String storeAccess = null;
		String storeAccessUpdate = null;
		if (mode.equals("past")) {
			storeAccess = "text";
			storeAccessUpdate = "past_update";
		} else if (mode.equals("ranking")) {
			storeAccess = "rank";
			storeAccessUpdate = "rank_update";
		} else if (mode.equals("pair")) {
			storeAccess = "pair";
			storeAccessUpdate = "pair_update";
		}

		URL url1 = new URL("http://" + sv + ".appspot.com/" + mode
				+ "?mokyu=true");
		HTTPRequest request1 = new HTTPRequest(url1, HTTPMethod.GET, Builder
				.disallowTruncate().setDeadline(15.0));
		HTTPResponse http_response1 = URLFetchServiceFactory
				.getURLFetchService().fetch(request1);
		String content1 = new String(http_response1.getContent(), "utf-8");
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past = null;
		try {
			past = datastore
					.get(KeyFactory.createKey("past_string", "onlyOne"));
		} catch (EntityNotFoundException e) {
		}

		String content2 = ((Text) past.getProperty(storeAccess)).getValue()
				+ "<>" + (String) past.getProperty(storeAccessUpdate);
		String[][] log_box = new String[2][2];
		log_box[0] = content1.split("<>");
		log_box[1] = content2.split("<>");
		if (log_box[0][0].isEmpty() || log_box[0][1].isEmpty()
				|| log_box[1][0].isEmpty() || log_box[1][1].isEmpty())
			throw new IOException();
		String[][][] sp_log_box = new String[2][2][];

		sp_log_box[0][0] = log_box[0][0].split(",");
		sp_log_box[1][0] = log_box[1][0].split(",");
		sp_log_box[0][1] = log_box[0][1].split(",");
		sp_log_box[1][1] = log_box[1][1].split(",");
		return sp_log_box;
	}

	public String synchroLogAll(String[][][] sp_log_box) {
		int sb1size = (sp_log_box[0][0].length - 1) / 4;
		String[][] sync_box1 = new String[2][sb1size];
		for (int i = 0; i < sb1size; i++) {
			sync_box1[0][i] = sp_log_box[0][0][i * 4 + 1] + ","
					+ sp_log_box[0][0][i * 4 + 2];
			sync_box1[1][i] = sp_log_box[0][0][i * 4 + 3] + ","
					+ sp_log_box[0][0][i * 4 + 4];
		}

		int sb2size = (sp_log_box[1][0].length - 1) / 4;
		String[][] sync_box2 = new String[2][sb2size];
		for (int i = 0; i < sb2size; i++) {
			sync_box2[0][i] = sp_log_box[1][0][i * 4 + 1] + ","
					+ sp_log_box[1][0][i * 4 + 2];
			sync_box2[1][i] = sp_log_box[1][0][i * 4 + 3] + ","
					+ sp_log_box[1][0][i * 4 + 4];
		}
		for (int i = 0; i < sb1size; i++) {
			for (int j = 0; j < sb2size; j++) {
				if (sync_box2[0][j].isEmpty())
					continue;
				if (sync_box1[0][i].equals(sync_box2[0][j])) {
					if (i > j)
						sync_box1[0][i] = "";
					else
						sync_box2[0][j] = "";
					break;
				}
			}
		}
		String test = "";
		StringBuilder sb = new StringBuilder();
		if (sb1size > sb2size) {
			for (int j = 0; j < sb2size; j++) {
				if (!sync_box2[0][j].isEmpty() && !sync_box2[0][j].replaceAll("^.*%.*$", "").isEmpty())
					sb.append("," + sync_box2[0][j] + "," + sync_box2[1][j]
							+ test);
				if (!sync_box1[0][j].isEmpty() && !sync_box1[0][j].replaceAll("^.*%.*$", "").isEmpty())
					sb.append("," + sync_box1[0][j] + "," + sync_box1[1][j]
							+ test);
			}
			for (int j = sb2size; j < sb1size; j++) {
				if (!sync_box1[0][j].isEmpty() && !sync_box1[0][j].replaceAll("^.*%.*$", "").isEmpty())
					sb.append("," + sync_box1[0][j] + "," + sync_box1[1][j]
							+ test);
			}
			return new String(sb);
		} else {
			for (int i = 0; i < sb1size; i++) {
				if (!sync_box1[0][i].isEmpty() && !sync_box1[0][i].replaceAll("^.*%.*$", "").isEmpty())
					sb.append("," + sync_box1[0][i] + "," + sync_box1[1][i]
							+ test);
				if (!sync_box2[0][i].isEmpty() && !sync_box2[0][i].replaceAll("^.*%.*$", "").isEmpty())
					sb.append("," + sync_box2[0][i] + "," + sync_box2[1][i]
							+ test);
			}
			for (int i = sb1size; i < sb2size; i++) {
				if (!sync_box2[0][i].isEmpty() && !sync_box2[0][i].replaceAll("^.*%.*$", "").isEmpty())
					sb.append("," + sync_box2[0][i] + "," + sync_box2[1][i]
							+ test);
			}
			return new String(sb);
		}
	}

	public String synchroLogUpdate(String[][][] sp_log_box) {
		int sb1size = sp_log_box[0][1].length;
		int sb2size = sp_log_box[1][1].length;
		for (int i = 0; i < sb1size; i++) {
			for (int j = 0; j < sb2size; j++) {
				if (sp_log_box[1][1][j].isEmpty())
					continue;
				if (sp_log_box[0][1][i].equals(sp_log_box[1][1][j])) {
					if (i > j)
						sp_log_box[0][1][i] = "";
					else
						sp_log_box[1][1][j] = "";
					break;
				}
			}
		}
		String test = "";
		int count = 0;
		StringBuilder sb = new StringBuilder();
		if (sb1size > sb2size) {
			for (int j = 0; j < sb2size; j++) {
				if (!sp_log_box[1][1][j].isEmpty() && !sp_log_box[1][1][j].replaceAll("^.*%.*$","").isEmpty()) {
					sb.append("," + sp_log_box[1][1][j] + test);
					count++;
					if (count > 30)
						break;
				}
				if (!sp_log_box[0][1][j].isEmpty() && !sp_log_box[0][1][j].replaceAll("^.*%.*$","").isEmpty()) {
					sb.append("," + sp_log_box[0][1][j] + test);
					count++;
					if (count > 30)
						break;
				}
			}
			if (count <= 30) {
				for (int j = sb2size; j < sb1size; j++) {
					if (!sp_log_box[0][1][j].isEmpty() && !sp_log_box[0][1][j].replaceAll("^.*%.*$","").isEmpty()) {
						sb.append("," + sp_log_box[0][1][j] + test);
						count++;
						if (count > 30)
							break;
					}
				}
			}
			return new String(sb);
		} else {
			for (int i = 0; i < sb1size; i++) {
				if (!sp_log_box[0][1][i].isEmpty() && !sp_log_box[0][1][i].replaceAll("^.*%.*$","").isEmpty()) {
					sb.append("," + sp_log_box[0][1][i] + test);
					count++;
					if (count > 30)
						break;
				}
				if (!sp_log_box[1][1][i].isEmpty() && !sp_log_box[1][1][i].replaceAll("^.*%.*$","").isEmpty()) {
					sb.append("," + sp_log_box[1][1][i] + test);
					count++;
					if (count > 30)
						break;
				}
			}
			if (count <= 30)
				for (int i = sb1size; i < sb2size; i++) {
					if (!sp_log_box[1][1][i].isEmpty() && !sp_log_box[1][1][i].replaceAll("^.*%.*$","").isEmpty()) {
						sb.append("," + sp_log_box[1][1][i] + test);
						count++;
						if (count > 30)
							break;
					}
				}
			return new String(sb);
		}
	}

}