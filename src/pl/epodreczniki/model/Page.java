package pl.epodreczniki.model;

import com.google.gson.annotations.SerializedName;

public class Page {
	
	@SerializedName("isTeacher")
	private Boolean teacher;
	
	private String path;
	
	private String idRef;
	
	private String moduleId;
	
	private String pageId;
	
	public Boolean isTeacher(){
		return teacher;
	}
	
	public String getPath(){
		return path;	
	}
	
	public String getIdRef(){
		return idRef;
	}
	
	public String getModuleId(){
		return moduleId;
	}
	
	public String getPageId(){
		return pageId;
	}

}
