package pl.epodreczniki.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

public class Settings implements Parcelable{
	
	public static final int LIST_TYPE_FLAT = 0;
	
	public static final int LIST_TYPE_PAGER = 1;
	
	public static final Parcelable.Creator<Settings> CREATOR = new Parcelable.Creator<Settings>() {

		@Override
		public Settings createFromParcel(Parcel source) {
			final ClassLoader c = Settings.class.getClassLoader();
			final Builder res = new Builder()			
			.withManageBooks((Boolean) source.readValue(c))
			.withManageAccounts((Boolean) source.readValue(c))
			.withTeacher((Boolean) source.readValue(c))
			.withPasswordRequired((Boolean) source.readValue(c))
			.withDisplayName((String) source.readValue(c))
			.withFontSize((Integer) source.readValue(c))			
			.withPreferredListType((Integer) source.readValue(c))
			.withBookFilterEducationLevel((String) source.readValue(c))
			.withBookFilterClass((String) source.readValue(c));
			return res.build();
		}

		@Override
		public Settings[] newArray(int size) {
			return new Settings[size];
		}
	};
	
	private static final Gson GSON = new Gson();
			
	private boolean manageBooks;
	
	private boolean manageAccounts;
	
	private boolean teacher;
	
	private boolean passwordRequired;
	
	private String displayName;
	
	private int fontSize;		

	private int preferredListType;
	
	private String bookFilterEducationLevel;
	
	private String bookFilterClass;
	
	public Settings(Builder b){
		this.manageBooks = b.manageBooks;
		this.manageAccounts = b.manageAccounts;
		this.teacher = b.teacher;
		this.passwordRequired = b.passwordRequired;
		this.displayName = b.displayName;
		this.fontSize = b.fontSize;
		this.preferredListType = b.preferredListType;
		this.bookFilterEducationLevel = b.bookFilterEducationLevel;
		this.bookFilterClass = b.bookFilterClass;
	}
	
	public boolean canManageBooks(){
		return manageBooks;
	}	
	
	public boolean canManageAccounts(){
		return manageAccounts;	
	}
	
	public boolean isTeacher(){
		return teacher;
	}
	
	public boolean isPasswordRequired(){
		return passwordRequired;
	}
	
	public String getDisplayName(){
		return displayName;
	}
	
	public int getFontSize(){
		return fontSize;
	}		
	
	public int getPreferredListType(){
		return preferredListType;
	}
	
	public String getBookFilterEducationLevel(){
		return bookFilterEducationLevel;
	}
	
	public String getBookFilterClass(){
		return bookFilterClass;
	}
	
	public Builder buildUpon(){
		return new Builder()
		.withManageBooks(this.manageBooks)
		.withManageAccounts(this.manageAccounts)
		.withTeacher(this.teacher)
		.withPasswordRequired(this.passwordRequired)
		.withDisplayName(this.displayName)
		.withFontSize(this.fontSize)
		.withPreferredListType(this.preferredListType)
		.withBookFilterEducationLevel(this.bookFilterEducationLevel)
		.withBookFilterClass(this.bookFilterClass);
	}
	
	public String toJsonString(){
		return GSON.toJson(this);
	}
	
	public static Settings fromJsonString(String jsonString){
		return GSON.fromJson(jsonString, Settings.class);
	}
	
	public static String toJsonString(Settings settings){
		return GSON.toJson(settings);
	}	
	
	public static String getInitialSettings(){
		final Builder res = new Builder()		
		.withManageBooks(true)
		.withManageAccounts(true)
		.withTeacher(false)
		.withPasswordRequired(false)
		.withDisplayName("Admin")
		.withFontSize(100)
		.withPreferredListType(0);
		return GSON.toJson(res.build());
	}
	
	public static Settings getDefaultUserSettings(){
		final Builder res = new Builder()
		.withManageBooks(false)
		.withManageAccounts(false)
		.withTeacher(false)
		.withPasswordRequired(true)
		.withFontSize(100)
		.withPreferredListType(0);
		return res.build();
	}

	@Override
	public int describeContents() {		
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(manageBooks);
		dest.writeValue(manageAccounts);
		dest.writeValue(teacher);
		dest.writeValue(passwordRequired);
		dest.writeValue(displayName);
		dest.writeValue(fontSize);	
		dest.writeValue(preferredListType);
		dest.writeValue(bookFilterEducationLevel);
		dest.writeValue(bookFilterClass);
	}
	
	public static class Builder{
		
		private boolean manageBooks;
		
		private boolean manageAccounts;
		
		private boolean teacher;
		
		private boolean passwordRequired;
		
		private String displayName;
		
		private int fontSize;
		
		private int preferredListType;
		
		private String bookFilterEducationLevel;
		
		private String bookFilterClass;
		
		public Builder withManageBooks(boolean manageBooks){
			this.manageBooks = manageBooks;
			return this;
		}
		
		public Builder withManageAccounts(boolean manageAccounts){
			this.manageAccounts = manageAccounts;
			return this;
		}
		
		public Builder withTeacher(boolean teacher){
			this.teacher = teacher;
			return this;
		}
		
		public Builder withPasswordRequired(boolean passwordRequired){
			this.passwordRequired = passwordRequired;
			return this;
		}
		
		public Builder withDisplayName(String displayName){
			this.displayName = displayName;
			return this;
		}
		
		public Builder withFontSize(int fontSize){
			this.fontSize = fontSize;
			return this;
		}
		
		public Builder withPreferredListType(int preferredListType){
			this.preferredListType = preferredListType;
			return this;
		}
		
		public Builder withBookFilterEducationLevel(String bookFilterEducationLevel){
			this.bookFilterEducationLevel = bookFilterEducationLevel;
			return this;
		}
		
		public Builder withBookFilterClass(String bookFilterClass){
			this.bookFilterClass = bookFilterClass;
			return this;
		}
		
		public Settings build(){
			return new Settings(this);
		}
		
	}
	
}
