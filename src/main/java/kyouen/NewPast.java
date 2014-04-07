package kyouen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

@SuppressWarnings("serial")
public class NewPast extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past = null;
		try {
			past = datastore.get(KeyFactory.createKey("past", "default"));
		} catch (EntityNotFoundException e) {
		}
		Long now0 = System.currentTimeMillis();
		String dumpExplain = ((Text) past.getProperty("explain")).getValue();
		dumpExplain = dumpExplain.substring(1);
		String[] dump = dumpExplain.split("\\{");
		ArrayList<String> al1 = new ArrayList<String>();
		ArrayList<String> al2 = new ArrayList<String>();
		ArrayList<String> al3 = new ArrayList<String>();
		ArrayList<String> al4 = new ArrayList<String>();
		al1.add("佐倉綾音");
		int index1 = 0;
		al2.add("水島大宙");
		int index2 = 0;
		al3.add("佐倉綾音|水島大宙}0,1");
		al4.add("佐倉綾音|水島大宙}0,1");
		int i;
		for (String s : dump) {
			String name1 = s.split("\\|")[0];
			String name2 = s.split("\\||\\}")[1];
			if ((i = al1.indexOf(name1)) != -1) {
				al1.add(i, name1);
				al3.add(i, s);
			} else
				for (i = 0;;) {
					try {
						if (al1.get(i).compareTo(name1) < 0) {
							i = al1.lastIndexOf(al1.get(i)) + 1;
							continue;
						} else {
							al1.add(i, name1);
							al3.add(i, s);
							break;
						}
					} catch (IndexOutOfBoundsException ioobe) {
						al1.add(name1);
						al3.add(s);
						break;
					}
				}
			if ((i = al2.indexOf(name2)) != -1) {
				al2.add(i, name2);
				al4.add(i, s);
			} else
				for (i = 0;;) {
					try {
						if (al2.get(i).compareTo(name2) < 0) {
							i = al2.lastIndexOf(al2.get(i)) + 1;
							continue;
						} else {
							al2.add(i, name2);
							al4.add(i, s);
							break;
						}
					} catch (IndexOutOfBoundsException ioobe) {
						al2.add(name2);
						al4.add(s);
						break;
					}
				}
/*			if (al1.size() % 10 == 0) {
				Long now1 = System.currentTimeMillis();
				if (now1 - now0 > 45000) {
					break;
				}
			}*/
			StringBuilder sb = new StringBuilder();
			for(String o1:al1){
				sb.append("|" + o1);
			}
			String o = new String(sb);
			o = o.substring(1);
			past.setProperty("index1", new Text(o));
			
			sb = new StringBuilder();
			for(String o2:al2){
				sb.append("|" + o2);
			}
			o = new String(sb);
			o = o.substring(1);
			past.setProperty("index2", new Text(o));
			
			sb = new StringBuilder();
			for(String o3:al3){
				sb.append(">" + o3);
			}
			o = new String(sb);
			o = o.substring(1);
			past.setProperty("split1", new Text(o));			
			
			sb = new StringBuilder();
			for(String o4:al4){
				sb.append(">" + o4);
			}
			o = new String(sb);
			o = o.substring(1);
			past.setProperty("split2", new Text(o));
			if(false)
			datastore.put(past);
		}
		resp.getWriter().println("al1<br />");
		for (String s : al1) {
			resp.getWriter().println(s + "<br />");
		}
		resp.getWriter().println("al2<br />");
		for (String s : al2) {
			resp.getWriter().println(s + "<br />");
		}
		resp.getWriter().println("al3<br />");
		for (String s : al3) {
			resp.getWriter().println(s + "<br />");
		}
		resp.getWriter().println("al4<br />");
		for (String s : al4) {
			resp.getWriter().println(s + "<br />");
		}

	}

	public void cacheToDump() {
		Long now0 = System.currentTimeMillis();
		int i = 0;
		Pattern p = Pattern.compile("^(.*),([^,]+),([^,]+),(\\d+),(\\d+)$");
		Matcher m;
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past = null;
		try {
			past = datastore.get(KeyFactory.createKey("past", "default"));
		} catch (EntityNotFoundException e) {
		}
		String dumpExplain;
		if (past.getProperty("explain") == null) {
			dumpExplain = "";
		} else
			dumpExplain = ((Text) past.getProperty("explain")).getValue();
		String pastString;
		if (past.getProperty("pause") == null)
			pastString = new Past().getPastString();
		else
			pastString = ((Text) past.getProperty("pause")).getValue();

		// resp.getWriter().println("作業用テキスト長:" + pastString.length() + "\n");
		// resp.getWriter().println("ダンプ済みテキスト長" + dumpExplain.length() + "\n");
		StringBuilder sb = new StringBuilder();
		sb.append(dumpExplain);
		while ((m = p.matcher(pastString)).find()) {
			pastString = m.group(1);
			sb.append("{" + m.group(2) + "|" + m.group(3) + "}" + m.group(4)
					+ "," + m.group(5));
			i++;
			if (i % 10 == 0) {
				Long now1 = System.currentTimeMillis();
				if (now1 - now0 > 45000) {
					break;
				}
			}
		}
		dumpExplain = new String(sb);
		// resp.getWriter().println("伸長したダンプテキスト長:" + dumpExplain.length());
		// resp.getWriter().println("残った作業用テキスト長:" + pastString.length());
		Entity entity = new Entity("past", "default");
		entity.setProperty("explain", new Text(new String(sb)));
		entity.setProperty("pause", new Text(pastString));
		if(false)
		datastore.put(entity);

	}
}
