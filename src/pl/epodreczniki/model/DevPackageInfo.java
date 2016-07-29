package pl.epodreczniki.model;

import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class DevPackageInfo {

	private DevPackageState state;
	
	private List<DevPackageFormat> formats;	
	
	@SerializedName("app_version_android")
	private int appVersion;
	
	@SerializedName("md_content_id")
	private String mdContentId;
	
	@SerializedName("md_version")
	private int mdVersion;
	
	@SerializedName("md_title")
	private String mdTitle;
	
	public DevPackageState getState(){
		return state;
	}
	
	public List<DevPackageFormat> getFormats(){
		return Collections.unmodifiableList(formats);
	}
	
	public int getAppVersion(){
		return appVersion;
	}
	
	public String getMdContentId(){
		return mdContentId;
	}
	
	public int getMdVersion(){
		return mdVersion;
	}
	
	public String getMdTitle(){
		return mdTitle;
	}
	
	public static class DevPackageState{
		
		private boolean ready;
		
		private String comment;
		
		@SerializedName("lastmodified")
		private String lastModified;
		
		@SerializedName("lasttransformed")
		private String lastTransformed;		
		
		public boolean isReady(){
			return ready;
		}
		
		public String getComment(){
			return comment;
		}
		
		public String getLastModified(){
			return lastModified;
		}
		
		public String getLastTransformed(){
			return lastTransformed;
		}
		
	}
	
	public static class DevPackageFormat{
		
		private String format;
		
		private String url;
		
		private Long size;
		
		public String getFormat(){
			return format;
		}
		
		public String getUrl(){
			return url;
		} 
		
		public Long getSize(){
			return size;
		}
		
	}
	
}
