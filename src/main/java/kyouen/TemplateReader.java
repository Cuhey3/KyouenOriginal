package kyouen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateReader {
	String wikitext;
	String templateText;
	String templateInitial;
	String templateName;
	String templateGender;
	TemplateRequest templateRequest;
	LinkedHashMap<String, String> requestField;
	LinkedHashMap<String, String> templateCodes;
	final Pattern p = Pattern.compile("^(.*)(\\{\\{.*?\\}\\})(.*)$",
			Pattern.DOTALL);
	final Pattern p_tempName = Pattern.compile("\\{\\{(.*?)(　|\\<|\\||\\}\\})",
			Pattern.DOTALL);

	public TemplateReader(String wikitext) {
		this.wikitext = wikitext;
		this.templateRequest = new TemplateRequest();
		this.templateCodes = this.templateRequest.getTemplateCodes();
	}

	public void setTemplateRequest(TemplateRequest tr) {
		this.templateRequest = tr;
	}

	public void read() {
		Matcher m;
		while ((m = p.matcher(wikitext)).find()) {
			wikitext = m.group(1) + m.group(3);
			String suggestTemplateText = m.group(2);
			String suggestTemplateName = getTemplateName(suggestTemplateText);
			for (String s : templateCodes.keySet()) {
				if (s.equalsIgnoreCase(suggestTemplateName)) {
					templateInitial = templateCodes.get(s);
					templateText = suggestTemplateText;
					templateName = suggestTemplateName;
					if (this.templateRequest.templateGender
							.containsKey(templateName)) {
						this.templateGender = templateRequest.templateGender
								.get(templateName);
					}
					requestField = this.templateRequest
							.getRequestField(templateInitial);
					break;
				}
			}
		}
	}

	public void write(String correctName) {
		//
		Assistant as = new Assistant();
		as.setHumanProp();
		LinkedHashMap<String, String> human = as.getHuman(correctName);
		if (human == null)
			human = new LinkedHashMap<String, String>();
		if (templateInitial != null && templateText != null) {
			human.put("t", templateInitial);
			if (!requestField.isEmpty())
				for (String rule : requestField.keySet()) {
					String fieldName = rule.split("=")[0];
					String fieldValue = rule.split("=")[1];
					String attributeSetting = requestField.get(rule);
					String attributeName = attributeSetting.split(":")[0];
					String attributeValue = attributeSetting.split(":")[1];
					Pattern p_field = Pattern.compile("\\|\\s*" + fieldName
							+ "\\s*=[^\\|]*" + fieldValue, Pattern.DOTALL);
					if (p_field.matcher(templateText).find()) {
						human.put(attributeName, attributeValue);
					}
				}
			if (this.templateGender != null) {
				human.put("g", this.templateGender);
			}
		}
		as.updateHumanProp(correctName, human);
		as.makePersistentHumanProp();
		// }
	}

	public String getTemplateName(String suggestTemplateText) {
		Matcher m = p_tempName.matcher(suggestTemplateText);
		if (m.find())
			return m.group(1).replace('_', ' ').trim();
		else
			return "";
	}
}

class TemplateRequest {
	LinkedHashMap<String, LinkedHashMap<String, String>> requestFields = new LinkedHashMap<String, LinkedHashMap<String, String>>();
	LinkedHashMap<String, String> templateCodes = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> seiyuRequestField = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> actorRequestField = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> maleModelRequestField = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> femaleModelReqeustField = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> templateGender = new LinkedHashMap<String, String>();
	LinkedHashMap<String, String> defaultCategoryInWikitext = new LinkedHashMap<String, String>();

	public TemplateRequest() {
		seiyuRequestField.put("性別=男性", "g:m");
		seiyuRequestField.put("性別=女性", "g:f");
		putRequestFields();
		putTemplateCode();
		setTemplateGender();
	}

	public void setTemplateGender() {
		templateGender.put("男性モデル", "m");
		templateGender.put("女性モデル", "f");
	}

	protected void putRequestFields() {
		requestFields.put("s", seiyuRequestField);
		requestFields.put("a", actorRequestField);
		requestFields.put("mm", maleModelRequestField);
		requestFields.put("fm", femaleModelReqeustField);
	}

	protected void putTemplateCode() {
		templateCodes.put("声優", "s");
		templateCodes.put("ActorActress", "a");
		templateCodes.put("男性モデル", "mm");
		templateCodes.put("女性モデル", "fm");
	}

	public void addTemplateFields(String initial, String rule, String encoding)
			throws IOException {
		LinkedHashMap<String, String> updatingRequestField = requestFields
				.get(initial);
		if (updatingRequestField == null)
			throw new IOException();
		if (rule.split("=").length == 1 || encoding.split(":").length == 1)
			throw new IOException();
		updatingRequestField.put(rule, encoding);
		requestFields.put(initial, updatingRequestField);
	}

	public LinkedHashMap<String, String> getRequestField(String initial) {
		return this.requestFields.get(initial);
	}

	public LinkedHashMap<String, String> getTemplateCodes() {
		return this.templateCodes;
	}
}

class CategoryReader {
	ArrayList<String> categories;
	LinkedHashMap<String, String> checkCategories = new LinkedHashMap<String, String>();
	HashSet<String> propCodes = new HashSet<String>();

	public CategoryReader(ArrayList<String> categories) {
		this.categories = categories;
		setDefaultCheckCategories();
	}

	public void setDefaultCheckCategories() {
		checkCategories.put("男性ファッションモデル", "g:m");
		checkCategories.put("男性モデル", "g:m");
		checkCategories.put("日本の男性ファッションモデル", "g:m");
		checkCategories.put("日本の男性モデル", "g:m");
		checkCategories.put("日本の男性声優", "g:m");
		checkCategories.put("女性ファッションモデル", "g:f");
		checkCategories.put("女性モデル", "g:f");
		checkCategories.put("日本の女性アダルトモデル", "g:f");
		checkCategories.put("日本の女性ファッションモデル", "g:f");
		checkCategories.put("日本の女性モデル", "g:f");
		checkCategories.put("日本の女性声優", "g:f");
	}

	public void read() {
		for (String keyCategory : checkCategories.keySet()) {
			for (String existCategory : categories) {
				if (keyCategory.equals(existCategory)) {
					propCodes.add(checkCategories.get(keyCategory));
				}
			}
		}
	}

	public void write(String correctName) {
		if (!propCodes.isEmpty()) {
			Assistant as = new Assistant();
			as.setHumanProp();
			LinkedHashMap<String, String> human = as.getHuman(correctName);
			if (human != null) {
				for (String attributeSetting : propCodes) {
					String attributeName = attributeSetting.split(":")[0];
					String attributeValue = attributeSetting.split(":")[1];
					human.put(attributeName, attributeValue);
				}
			}
			as.updateHumanProp(correctName, human);
		}
	}
}
