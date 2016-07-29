package pl.epodreczniki.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Note implements Parcelable{
	
	public static final Parcelable.Creator<Note> CREATOR = new Parcelable.Creator<Note>() {

		@Override
		public Note createFromParcel(Parcel source) {
			final ClassLoader c = Note.class.getClassLoader();
			Note.Builder b = new Note.Builder();
			b.withId((Long) source.readValue(c));
			b.withLocalNoteId((String) source.readValue(c));
			b.withLocalUserId((Long) source.readValue(c));
			b.withNoteId((String) source.readValue(c));
			b.withUserId((String) source.readValue(c));
			b.withHandbookId((String) source.readValue(c));
			b.withModuleId((String) source.readValue(c));
			b.withPageId((String) source.readValue(c));
			b.withPageIdx((Integer) source.readValue(c));			
			LocationPart[] parts = (LocationPart[]) source.readValue(c);
			b.withLocation(parts);
			b.withSubject((String) source.readValue(c));
			b.withValue((String) source.readValue(c));
			b.withType((Integer) source.readValue(c));
			b.withAccepted((Boolean) source.readValue(c));
			b.withReferenceTo((String) source.readValue(c));
			b.withReferencedBy((String) source.readValue(c));
			b.withModifyTime((String) source.readValue(c));			
			return b.build();
		}

		@Override
		public Note[] newArray(int size) {
			return new Note[size];
		}
	};
	
	private Long id;

	private String localNoteId;	
	
	private Long localUserId;
	
	private String noteId;
		
	private String userId;
	
	private String handbookId;
	
	private String moduleId;
	
	private String pageId;
	
	private int pageIdx;
	
	private LocationPart[] location;
	
	private String subject;
	
	private String value;
	
	private Integer type;
	
	private Boolean accepted;
	
	private String referenceTo;
	
	private String referencedBy;
	
	private String modifyTime;
	
	public Note(Builder b){
		this.id = b.id;
		this.localNoteId = b.localNoteId;
		this.localUserId = b.localUserId;
		this.noteId = b.noteId;
		this.userId = b.userId;
		this.handbookId = b.handbookId;
		this.moduleId = b.moduleId;
		this.pageId = b.pageId;
		this.pageIdx = b.pageIdx;
		this.location = b.location.toArray(new LocationPart[b.location.size()]);
		this.subject = b.subject;
		this.value = b.value;
		this.type = b.type;
		this.accepted = b.accepted;
		this.referenceTo = b.referenceTo;
		this.referencedBy = b.referencedBy;
		this.modifyTime = b.modifyTime;
	}
	
	public Long getId(){
		return id;
	}
	
	public String getLocalNoteId(){
		return localNoteId;
	}
	
	public Long getLocalUserId(){
		return localUserId;
	}
	
	public String getNoteId(){
		return noteId;
	}
	
	public String getUserId(){
		return userId;
	}
	
	public String getHandbookId(){
		return handbookId;
	}
	
	public String getModuleId(){
		return moduleId;
	}
	
	public String getPageId(){
		return pageId;
	}
	
	public int getPageIdx(){
		return pageIdx;
	}
	
	public LocationPart[] getLocation(){
		return location;
	}
	
	public String getSubject(){
		return subject;
	}
	
	public String getValue(){
		return value;
	}
	
	public Integer getType(){
		return type;
	}
	
	public Boolean getAccepted(){
		return accepted;
	}
	
	public String getReferenceTo(){
		return referenceTo;
	}
	
	public String getReferencedBy(){
		return referencedBy;
	}
	
	public String getModifyTime(){
		return modifyTime;
	}
	
	public boolean isBookmark(){
		return type<4;
	}
	
	@Override
	public int describeContents() {	
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(id);
		dest.writeValue(localNoteId);
		dest.writeValue(localUserId);
		dest.writeValue(noteId);
		dest.writeValue(userId);
		dest.writeValue(handbookId);
		dest.writeValue(moduleId);
		dest.writeValue(pageId);
		dest.writeValue(pageIdx);
		dest.writeValue(location);
		dest.writeValue(subject);
		dest.writeValue(value);
		dest.writeValue(type);
		dest.writeValue(accepted);
		dest.writeValue(referenceTo);
		dest.writeValue(referencedBy);
		dest.writeValue(modifyTime);
	}	
	
	public static class Builder{
		
		private Long id;
		
		private String localNoteId;
		
		private Long localUserId;
		
		private String noteId;
		
		private String userId;
		
		private String handbookId;
		
		private String moduleId;
		
		private String pageId;
		
		private int pageIdx;
		
		private List<LocationPart> location;
		
		private String subject;
		
		private String value;
		
		private Integer type;
		
		private Boolean accepted;
		
		private String referenceTo;
		
		private String referencedBy;
		
		private String modifyTime;
		
		public Builder(){
			location = new ArrayList<LocationPart>();
		}
		
		public static Builder fromNote(Note note){
			Builder res = new Builder();
			res.id = note.id;
			res.localNoteId = note.localNoteId;
			res.localUserId = note.localUserId;
			res.noteId = note.noteId;
			res.userId = note.userId;
			res.handbookId = note.handbookId;
			res.moduleId = note.moduleId;
			res.pageId = note.pageId;
			res.pageIdx = note.pageIdx;
			res.location = Arrays.asList(note.location);
			res.subject = note.subject;
			res.value = note.value;
			res.type = note.type;
			res.accepted = note.accepted;
			res.referenceTo = note.referenceTo;
			res.referencedBy = note.referencedBy;
			res.modifyTime = note.modifyTime;
			return res;
		}
		
		public Builder withId(Long id){
			this.id = id;
			return this;
		}
		
		public Builder withLocalNoteId(String localNoteId){
			this.localNoteId = localNoteId;
			return this;
		}
		
		public Builder withLocalUserId(Long localUserId){
			this.localUserId = localUserId;
			return this;
		}
		
		public Builder withNoteId(String noteId){
			this.noteId = noteId;
			return this;
		}
		
		public Builder withUserId(String userId){
			this.userId = userId;
			return this;
		}
		
		public Builder withHandbookId(String handbookId){
			this.handbookId = handbookId;
			return this;
		}	
		
		public Builder withModuleId(String moduleId){
			this.moduleId = moduleId;
			return this;
		}
		
		public Builder withPageId(String pageId){
			this.pageId = pageId;
			return this;
		}
		
		public Builder withPageIdx(int pageIdx){
			this.pageIdx = pageIdx;
			return this;
		}
		
		public Builder withLocationPart(LocationPart locationPart){
			this.location.add(locationPart);
			return this;
		}
		
		private Builder withLocation(LocationPart[] location){
			if(location!=null){
				for(LocationPart p : location){
					this.location.add(p);
				}	
			}				
			return this;
		}
		
		public Builder withSubject(String subject){
			this.subject = subject;
			return this;
		}
		
		public Builder withValue(String value){
			this.value = value;
			return this;
		}
		
		public Builder withType(Integer type){
			this.type = type;
			return this;
		}
		
		public Builder withAccepted(Boolean accepted){
			this.accepted = accepted;
			return this;
		}
		
		public Builder withReferenceTo(String referenceTo){
			this.referenceTo = referenceTo;
			return this;
		}
		
		public Builder withReferencedBy(String referencedBy){
			this.referencedBy = referencedBy;
			return this;
		}
		
		public Builder withModifyTime(String modifyTime){
			this.modifyTime = modifyTime;
			return this;
		}
		
		public Note build(){
			return new Note(this);
		}
		
	}
	
	public static class LocationPart implements Parcelable{
		
		public static final Parcelable.Creator<LocationPart> CREATOR = new Parcelable.Creator<Note.LocationPart>() {

			@Override
			public LocationPart createFromParcel(Parcel source) {
				LocationPart res = new LocationPart();
				res.sid = (String) source.readValue(Note.class.getClassLoader());
				List<Range> ranges = new ArrayList<Range>();
				source.readTypedList(ranges, Range.CREATOR);
				res.ranges = ranges;
				return res;
			}

			@Override
			public LocationPart[] newArray(int size) {
				return new LocationPart[size];
			}
		};
		
		private String sid;
		
		private List<Range> ranges;
		
		public String getSid(){
			return sid;
		}
		
		public List<Range> getRanges(){
			return Collections.unmodifiableList(ranges);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeValue(sid);
			dest.writeTypedList(ranges);
		}
		
	}
	
	public static class Range implements Parcelable{
		
		public static final Parcelable.Creator<Range> CREATOR = new Parcelable.Creator<Note.Range>() {

			@Override
			public Range createFromParcel(Parcel source) {
				Range res = new Range();
				res.backward = (Boolean) source.readValue(Note.class.getClassLoader());
				res.characterRange = (CharacterRange) source.readValue(Note.class.getClassLoader());
				return res;
			}

			@Override
			public Range[] newArray(int size) {
				return new Range[size];
			}
		};
		
		private Boolean backward;
		
		private CharacterRange characterRange;
		
		public Boolean isBackward(){
			return backward;
		}
		
		public CharacterRange getCharacterRange(){
			return characterRange;
		}

		@Override
		public int describeContents() {			
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeValue(backward);
			dest.writeValue(characterRange);			
		}
		
	}
		
	public static class CharacterRange implements Parcelable{
		
		public static final Parcelable.Creator<CharacterRange> CREATOR = new Parcelable.Creator<Note.CharacterRange>() {

			@Override
			public CharacterRange createFromParcel(Parcel source) {
				CharacterRange res = new CharacterRange();
				res.start = (Integer) source.readValue(Note.class.getClassLoader());
				res.end = (Integer) source.readValue(Note.class.getClassLoader());								
				return res;
			}

			@Override
			public CharacterRange[] newArray(int size) {
				return new CharacterRange[size];
			}
		};
		
		private Integer start;
		
		private Integer end;				
		
		public Integer getStart(){
			return start;
		}
		
		public Integer getEnd(){
			return end;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeValue(start);
			dest.writeValue(end);
		}
		
	}	
	
}
