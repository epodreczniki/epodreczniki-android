package pl.epodreczniki.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ExerciseState implements Parcelable {

	public static final Parcelable.Creator<ExerciseState> CREATOR = new Parcelable.Creator<ExerciseState>() {

		@Override
		public ExerciseState createFromParcel(Parcel source) {
			final ClassLoader c = ExerciseState.class.getClassLoader();
			Builder b = new Builder();
			b.withId((Long) source.readValue(c));
			b.withLocalUserId((Long) source.readValue(c));
			b.withMdContentId((String) source.readValue(c));
			b.withMdVersion((Integer) source.readValue(c));
			b.withWomiId((String) source.readValue(c));
			b.withValue((String) source.readValue(c));
			return b.build();
		}

		@Override
		public ExerciseState[] newArray(int size) {
			return new ExerciseState[size];
		}
		
	};
	
	private Long id;
	
	private Long localUserId;
	
	private String mdContentId;
	
	private Integer mdVersion;
	
	private String womiId;
	
	private String value;
	
	public ExerciseState(Builder b){
		this.id = b.id;
		this.localUserId = b.localUserId;
		this.mdContentId = b.mdContentId;
		this.mdVersion = b.mdVersion;
		this.womiId = b.womiId;
		this.value = b.value;
	}
	
	public Long getId(){
		return id;
	}
	
	public Long getLocalUserId(){
		return localUserId;
	}
	
	public String getMdContentId(){
		return mdContentId;
	}
	
	public Integer getMdVersion(){
		return mdVersion;
	}
	
	public String getWomiId(){
		return womiId;
	}
	
	public String getValue(){
		return value;
	}
	
	public Builder buildUpon(){
		return new Builder(this);
	}
	
	@Override
	public int describeContents() {	
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(id);
		dest.writeValue(localUserId);
		dest.writeValue(mdContentId);
		dest.writeValue(mdVersion);
		dest.writeValue(womiId);
		dest.writeValue(value);
	}
	
	public static class Builder{
		
		private Long id;
		
		private Long localUserId;
		
		private String mdContentId;
		
		private Integer mdVersion;
		
		private String womiId;
		
		private String value;
		
		public Builder(){}
		
		private Builder(ExerciseState state){
			this.id = state.id;
			this.localUserId = state.localUserId;
			this.mdContentId = state.mdContentId;
			this.mdVersion = state.mdVersion;
			this.womiId = state.womiId;
			this.value = state.value;			
		}
		
		public Builder withId(Long id){
			this.id = id;
			return this;
		}
		
		public Builder withLocalUserId(Long localUserId){
			this.localUserId = localUserId;
			return this;
		}
		
		public Builder withMdContentId(String mdContentId){
			this.mdContentId = mdContentId;
			return this;
		}
		
		public Builder withMdVersion(Integer mdVersion){
			this.mdVersion = mdVersion;
			return this;
		}		
		
		public Builder withWomiId(String womiId){
			this.womiId = womiId;
			return this;
		}
		
		public Builder withValue(String value){
			this.value = value;
			return this;
		}
		
		public ExerciseState build(){
			return new ExerciseState(this);
		}
		
	}	
	
}
