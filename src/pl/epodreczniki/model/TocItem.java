package pl.epodreczniki.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class TocItem {

	private String id;
	
	private String title;
	
	private String pathRef;
	
	@SerializedName("isTeacher")
	private Boolean teacher;
	
	private String numbering;
	
	private String[] contentStatus;
	
	private List<TocItem> children;
	
	public String getId(){
		return id;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getPathRef(){
		return pathRef;
	}
	
	public Boolean isTeacher(){
		return teacher;
	} 
	
	public String getNumbering(){
		return numbering;
	}
	
	public String[] getContentStatus(){
		return Arrays.copyOf(contentStatus, contentStatus.length);
	}
	
	public List<TocItem> getChildren(){
		return children==null?Collections.<TocItem>emptyList():Collections.unmodifiableList(children);
	}
	
	public boolean hasChildren(){
		return children!=null&&children.size()>0;
	}
	
}
