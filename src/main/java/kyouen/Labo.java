//年がトークンに入るのを修正しる

package kyouen;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static myUtil.WordCapsule.*;

//import net.sf.jsr107cache.Cache;

@SuppressWarnings("serial")
public class Labo extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("utf-8");
		if(req.getParameter("remove") != null){ 
		Assistant as = new Assistant();
		as.setSearchResult();
		as.removeSearchResult(req.getParameter("remove"));
		as.makePersistentSearchResult();
		}
/*		String[] brackets = {"[[,]],a"};
		String[] result = bracketToCapsule("[[はっぴーカッピ|はっぴ〜カッピ]] (木下スグリ役)\n[[ゆるゆり|ゆるゆり♪♪]] (吉川ちなつ役)",brackets);
		for(String r: result){
			resp.getWriter().println(r + "<br />");
		}*/
		/*
		Record rec = new Record();
		String name = req.getParameter("op");
		WikiUtil.getTextarea(name);
		String[][] record = rec.getRecord(name, 0, false);
		int len1 = record.length;
		int len2 = record[0].length;
		for(int i = 0; i < len2; i++){
			for(int j = 0; j < len1; j++){
				resp.getWriter().println(record[j][i] + "/");
			}
			resp.getWriter().println("<br />");
		}
		*/
	}
}
