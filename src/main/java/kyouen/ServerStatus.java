package kyouen;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.apphosting.api.ApiProxy;

@SuppressWarnings("serial")
public class ServerStatus extends HttpServlet {
	String serverID;
	String otherServerID;
	String maintenance_str;
	boolean maintenance;

	public ServerStatus() {
		serverID = ApiProxy.getCurrentEnvironment().getAppId();
		otherServerID = serverID.equals("seiyukyouen") ? "seiyufan"
				: "seiyukyouen";
		setMaintenance();
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
	response.setContentType("text/html");
	response.setCharacterEncoding("utf-8");
	try{
	String s = request.getParameter("n");
	DataModel dataModel = new DataModel(s);
	dataModel.setAll();
	response.getWriter().println(dataModel.inputName);
	response.getWriter().println(dataModel.wikiName);
	response.getWriter().println(dataModel.shift);
	response.getWriter().println(dataModel.mf);
	response.getWriter().println(dataModel.cursive_syllabary);
	response.getWriter().println(dataModel.record_str);
	response.getWriter().println(dataModel.content_str);
	}catch(NullPointerException e){
		response.getWriter().println("input string is null.");
	}
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		response.getWriter().println("serverID:" + serverID);
		response.getWriter().println("otherServerID:" + otherServerID);
		response.getWriter().println("maintenance:" + maintenance_str);
		String input = request.getParameter("sflag");
		if (input != null) {
			if (input.equals("true_change")) {
				if (setMaintenance("true"))
					response.getWriter()
							.println(
									"server status has been changed to \"maintenance\".");
			} else if (input.equals("false_change")) {
				response.getWriter().println(
						"server status has been changed to \"normal\".");
				setMaintenance("false");
			}
		}
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity serv = null;
		try {
			serv = datastore.get(KeyFactory.createKey("server_status", "this"));
		} catch (EntityNotFoundException e) {
			serv = new Entity("server_status", "this");
			serv.setProperty("maintenance_str", "false");
			if(false)datastore.put(serv);
		}
		try {
			response.getWriter().println("<br />");
			String log[] = ((Text) serv.getProperty("log")).getValue().split(
					",");
			for (String s : log)
				response.getWriter().println(
						"<a href=\"" + s + "&debag=true\">" + s + "</a><br />");
		} catch (Throwable t) {
		}

	}

	public void setMaintenance() {
		maintenance_str = (String) (WikiUtil.getCache(60 * 60 * 24)
				.get("maintenance_str"));
		if (maintenance_str == null || maintenance_str.isEmpty()) {
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity serv = null;
			try {
				serv = datastore.get(KeyFactory.createKey("server_status",
						"this"));
			} catch (EntityNotFoundException e) {
				serv = new Entity("server_status", "this");
				serv.setProperty("maintenance_str", "false");
				if(false)datastore.put(serv);
			}
			WikiUtil.getCache(60 * 60 * 24).put(
					"maintenance_str",
					maintenance_str = (String) serv
							.getProperty("maintenance_str"));
		}
		if (maintenance_str.equals("true"))
			maintenance = true;
		else
			maintenance = false;
	}

	public boolean setMaintenance(String s) {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity serv = null;
		try {
			serv = datastore.get(KeyFactory.createKey("server_status", "this"));
			serv.setProperty("maintenance_str", s);
		} catch (EntityNotFoundException e) {
			serv = new Entity("server_status", "this");
			serv.setProperty("maintenance_str", s);
		}
		WikiUtil.getCache(60 * 60 * 24).put("maintenance_str",
				maintenance_str = (String) serv.getProperty("maintenance_str"));
		if(false)datastore.put(serv);
		return true;
	}
}