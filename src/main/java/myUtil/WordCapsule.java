package myUtil;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordCapsule {
	public static String delimL = "＼＼";
	public static String delimR = "／／";
	public static String[] bracketToCapsule(String text, String[] brackets) {
		ArrayList<String> capsuled = new ArrayList<String>();
		for (String bracket : brackets) {
			String[] bracketSplit = bracket.split(",");
			if (bracketSplit.length != 3)
				continue;
			String startBracket = bracketSplit[0];	
			String endBracket = bracketSplit[1];
			String andOr = bracketSplit[2];
			Pattern p_bracket = null;
			if (andOr.equals("a")) {
				p_bracket = Pattern.compile("^(.*)("
						+ Pattern.quote(startBracket) + ".*?"
						+ Pattern.quote(endBracket) + ")(.*)$",Pattern.DOTALL);
			} else if (andOr.equals("o")) {
				p_bracket = Pattern.compile("^(.*)(["
						+ Pattern.quote(startBracket) + "].*?["
						+ Pattern.quote(endBracket) + "])(.*)$",Pattern.DOTALL);
			} else
				continue;
			Matcher m;
			while ((m = p_bracket.matcher(text)).find()) {
				capsuled.add(m.group(2));
				int size = capsuled.size();
				text = m.group(1) + delimL + size + delimR + m.group(3);
			}
		}
		int size = capsuled.size();
		String[] result = new String[size+1];
		result[0] = text;
		for(int i = 0; i < size; i++){
			result[i+1] = capsuled.get(i);
		}
		return result;
	}
}
