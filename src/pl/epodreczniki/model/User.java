package pl.epodreczniki.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable{
	
	public static final int ACCOUNT_TYPE_INITIAL = 0;
	
	public static final int ACCOUNT_TYPE_ADMIN = 1;
	
	public static final int ACCOUNT_TYPE_USER = 2;
	
	public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {

		@Override
		public User createFromParcel(Parcel source) {
			final ClassLoader c = User.class.getClassLoader();			
			final Builder res = new Builder()
			.withLocalUserId((Long) source.readValue(c))
			.withUserName((String) source.readValue(c))
			.withAccountType((Integer) source.readValue(c))
			.withPassword64((String) source.readValue(c))
			.withSalt64((String) source.readValue(c))
			.withRecoveryQuestion((String) source.readValue(c))
			.withRecoveryAnswer64((String) source.readValue(c))
			.withRecoverySalt64((String) source.readValue(c))
			.withSettings((Settings) source.readValue(c));
			return res.build();
		}

		@Override
		public User[] newArray(int size) {
			return new User[size];
		}
	};

	private long localUserId;
	
	private String userName;

	private int accountType;
	
	private String password64;
	
	private String salt64;	
	
	private String recoveryQuestion;
	
	private String recoveryAnswer64;
	
	private String recoverySalt64;
	
	private Settings settings;
	
	public User(Builder b){
		this.localUserId = b.localUserId;
		this.userName = b.userName;
		this.accountType = b.accountType;
		this.password64 = b.password64;
		this.salt64 = b.salt64;	
		this.recoveryQuestion = b.recoveryQuestion;
		this.recoveryAnswer64 = b.recoveryAnswer64;
		this.recoverySalt64 = b.recoverySalt64;
		this.settings = b.settings;
	}
	
	public long getLocalUserId(){
		return localUserId;
	}
	
	public String getUserName(){
		return userName;
	}
	
	public int getAccountType(){
		return accountType;
	}
	
	public String getPassword64(){
		return password64;
	}
	
	public String getSalt64(){
		return salt64;
	}	
	
	public String getRecoveryQuestion(){
		return recoveryQuestion;
	}
	
	public String getRecoveryAnswer64(){
		return recoveryAnswer64;
	}
	
	public String getRecoverySalt64(){
		return recoverySalt64;
	}
	
	public Settings getSettings(){
		return settings;
	}
	
	public boolean isInitialAccount(){
		return accountType==ACCOUNT_TYPE_INITIAL;
	}
	
	public boolean isAdmin(){
		return accountType==ACCOUNT_TYPE_ADMIN;
	}
	
	public boolean isRegularUser(){
		return accountType==ACCOUNT_TYPE_USER;
	}
	
	public boolean canOpenAdminSettings(){
		return accountType==ACCOUNT_TYPE_ADMIN || accountType==ACCOUNT_TYPE_INITIAL;
	}
	
	public boolean canManageBooks(){
		return canOpenAdminSettings()||settings.canManageBooks();
	}

	@Override
	public int describeContents() {		
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(localUserId);
		dest.writeValue(userName);
		dest.writeValue(accountType);
		dest.writeValue(password64);
		dest.writeValue(salt64);		
		dest.writeValue(recoveryQuestion);
		dest.writeValue(recoveryAnswer64);
		dest.writeValue(recoverySalt64);
		dest.writeValue(settings);
	}
	
	public Builder buildUpon(){
		Builder res = new Builder();
		res.localUserId = this.localUserId;
		res.userName = this.userName;
		res.accountType = this.accountType;
		res.password64 = this.password64;
		res.salt64 = this.salt64;
		res.recoveryQuestion = this.recoveryQuestion;
		res.recoveryAnswer64 = this.recoveryAnswer64;
		res.settings = this.settings;
		return res;
	}
	
	public static class Builder{
		
		private long localUserId;
		
		private String userName;
		
		private int accountType;
		
		private String password64;
		
		private String salt64;	

		private String recoveryQuestion;
		
		private String recoveryAnswer64;
		
		private String recoverySalt64;
		
		private Settings settings;
		
		public Builder withLocalUserId(long localUserId){
			this.localUserId = localUserId;
			return this;
		}
		
		public Builder withUserName(String userName){
			this.userName = userName;
			return this;
		}
		
		public Builder withAccountType(int accountType){
			this.accountType = accountType;
			return this;
		}
		
		public Builder withPassword64(String password64){
			this.password64 = password64;
			return this;
		}
		
		public Builder withSalt64(String salt64){
			this.salt64 = salt64;
			return this;
		}		
		
		public Builder withRecoveryQuestion(String recoveryQuestion){
			this.recoveryQuestion = recoveryQuestion;
			return this;
		}
		
		public Builder withRecoveryAnswer64(String recoveryAnswer64){
			this.recoveryAnswer64 = recoveryAnswer64;
			return this;
		}
		
		public Builder withRecoverySalt64(String recoverySalt64){
			this.recoverySalt64 = recoverySalt64;
			return this;
		}
		
		public Builder withSettings(Settings settings){
			this.settings = settings;
			return this;
		}
		
		public User build(){
			return new User(this);
		}
		
	}
	
}
