package pl.epodreczniki.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Book extends AbstractBook implements Parcelable{

	public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {				
		
		@Override
		public Book createFromParcel(Parcel source) {
			final ClassLoader c = Book.class.getClassLoader();
			Book.Builder b = new Book.Builder();
			b.withMdContentId((String) source.readValue(c));
			b.withMdTitle((String) source.readValue(c));
			b.withMdAbstract((String) source.readValue(c));
			b.withMdPublished((Boolean) source.readValue(c));
			b.withMdVersion((Integer) source.readValue(c));
			b.withMdLanguage((String) source.readValue(c));
			b.withMdLicense((String) source.readValue(c));
			b.withMdCreated((String) source.readValue(c));
			b.withMdRevised((String) source.readValue(c));
			b.withCover((String) source.readValue(c));
			b.withLink((String) source.readValue(c));
			b.withMdSubtitle((String) source.readValue(c));
			b.withAuthors((String) source.readValue(c));
			b.withMainAuthor((String) source.readValue(c));
			b.withEducationLevel((String) source.readValue(c));
			b.withEpClass((Integer) source.readValue(c));
			b.withSubject((String) source.readValue(c));
			b.withZip((String) source.readValue(c));
			b.withSizeExtracted((Long) source.readValue(c));
			b.withStatus((Integer) source.readValue(c));
			b.withTransferId((Long) source.readValue(c));
			b.withLocalPath((String) source.readValue(c));
			b.withCoverStatus((Integer) source.readValue(c));
			b.withCoverTransferId((Long) source.readValue(c));
			b.withCoverLocalPath((String) source.readValue(c));
			b.withBytesSoFar((Integer) source.readValue(c));
			b.withBytesTotal((Integer) source.readValue(c));
			b.withZipLocal((String) source.readValue(c));
			b.withAppVersion((Integer) source.readValue(c));
			List<Book> versions = new ArrayList<Book>();
			source.readTypedList(versions, Book.CREATOR);
			for(Book v : versions){
				b.withVersion(v);
			}
			return b.build();
		}

		@Override
		public Book[] newArray(int size) {
			return new Book[size];
		}
		
	};
	
	private String cover;
	
	private String authors;
	
	private String mainAuthor;
	
	private String educationLevel;
	
	private Integer epClass;
	
	private String subject;
	
	private String zip;
	
	private Long sizeExtracted;
	
	private Integer status;
	
	private Long transferId;
	
	private String localPath;
	
	private Integer coverStatus;
	
	private Long coverTransferId;
	
	private String coverLocalPath;
	
	private Integer bytesSoFar;
	
	private Integer bytesTotal;
	
	private String zipLocal;
	
	private Integer appVersion;
	
	private List<Book> versions = new ArrayList<Book>();
	
	public Book(Builder b){
		this.mdContentId = b.mdContentId;
		this.mdTitle = b.mdTitle;
		this.mdAbstract = b.mdAbstract;
		this.mdPublished = b.mdPublished;
		this.mdVersion = b.mdVersion;
		this.mdLanguage = b.mdLanguage;
		this.mdLicense = b.mdLicense;
		this.mdCreated = b.mdCreated;
		this.mdRevised = b.mdRevised;
		this.cover = b.cover;
		this.link = b.link;
		this.mdSubtitle = b.mdSubtitle;
		this.authors = b.authors;
		this.mainAuthor = b.mainAuthor;
		this.educationLevel = b.educationLevel;
		this.epClass = b.epClass;
		this.subject = b.subject;
		this.zip = b.zip;
		this.sizeExtracted = b.sizeExtracted;
		this.status = b.status;
		this.transferId = b.transferId;
		this.localPath = b.localPath;
		this.coverStatus = b.coverStatus;
		this.coverTransferId = b.coverTransferId;
		this.coverLocalPath = b.coverLocalPath;
		this.bytesSoFar = b.bytesSoFar;
		this.bytesTotal = b.bytesTotal;
		this.zipLocal = b.zipLocal;
		this.appVersion = b.appVersion;
		for(Book ver : b.versions){
			this.versions.add(ver);
		}			
	}
	
	@Override
	public int describeContents() {	
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(mdContentId);
		dest.writeValue(mdTitle);
		dest.writeValue(mdAbstract);
		dest.writeValue(mdPublished);
		dest.writeValue(mdVersion);
		dest.writeValue(mdLanguage);
		dest.writeValue(mdLicense);
		dest.writeValue(mdCreated);
		dest.writeValue(mdRevised);
		dest.writeValue(cover);
		dest.writeValue(link);
		dest.writeValue(mdSubtitle);
		dest.writeValue(authors);
		dest.writeValue(mainAuthor);
		dest.writeValue(educationLevel);
		dest.writeValue(epClass);
		dest.writeValue(subject);
		dest.writeValue(zip);
		dest.writeValue(sizeExtracted);
		dest.writeValue(status);
		dest.writeValue(transferId);
		dest.writeValue(localPath);
		dest.writeValue(coverStatus);
		dest.writeValue(coverTransferId);
		dest.writeValue(coverLocalPath);
		dest.writeValue(bytesSoFar);
		dest.writeValue(bytesTotal);
		dest.writeValue(zipLocal);	
		dest.writeValue(appVersion);
		dest.writeTypedList(versions);		
	}
	
	public String getCover(){
		return cover;
	}

	public String getAuthors() {
		return authors;
	}

	public String getMainAuthor() {
		return mainAuthor;
	}

	public String getEducationLevel() {
		return educationLevel;
	}

	public Integer getEpClass() {
		return epClass;
	}

	public String getSubject() {
		return subject;
	}

	public String getZip() {
		return zip;
	}
	
	public Long getSizeExtracted(){
		return sizeExtracted;
	}
	
	public Integer getStatus(){
		return status;
	}
	
	public Long getTransferId(){
		return transferId;
	}
	
	public String getLocalPath(){
		return localPath;
	}
	
	public Integer getCoverStatus(){
		return coverStatus;
	}
	
	public Long getCoverTransferId(){
		return coverTransferId;
	}	
	
	public String getCoverLocalPath(){
		return coverLocalPath;
	}

	public Integer getBytesSoFar() {
		return bytesSoFar;
	}

	public Integer getBytesTotal() {
		return bytesTotal;
	}
	
	public String getZipLocal(){
		return zipLocal;
	}
	
	public Integer getAppVersion(){
		return appVersion;
	}
	
	public List<Book> getVersions(){
		return Collections.unmodifiableList(versions);
	}
	
	public void addVersion(Book book){
		if(book!=null){
			versions.add(book);
		}
	}
	
	public boolean isUpdateAvailable(){
		return !versions.isEmpty();
	}
	
	public Book getNewestVersion(){
		return versions.size()==0?this:versions.get(versions.size()-1);
	}
	
	public static class Builder{
		
		private String mdContentId;
		
		private String mdTitle;
		
		private String mdAbstract;
		
		private Boolean mdPublished;
		
		private Integer mdVersion;
		
		private String mdLanguage;
		
		private String mdLicense;
		
		private String mdCreated;
		
		private String mdRevised;
		
		private String cover;
		
		private String link;
		
		private String mdSubtitle;
		
		private String authors;
		
		private String mainAuthor;
		
		private String educationLevel;
		
		private Integer epClass;
		
		private String subject;
		
		private String zip;
		
		private Long sizeExtracted;
		
		private Integer status;
		
		private Long transferId;
		
		private String localPath;
		
		private Integer coverStatus;
		
		private Long coverTransferId;
		
		private String coverLocalPath;
		
		private Integer bytesSoFar;
		
		private Integer bytesTotal;
		
		private String zipLocal;
		
		private Integer appVersion;
		
		private List<Book> versions;
		
		public Builder(){
			versions = new ArrayList<Book>();
		}
		
		public Builder withMdContentId(String mdContentId){
			this.mdContentId = mdContentId;
			return this;
		}
		
		public Builder withMdTitle(String mdTitle){
			this.mdTitle = mdTitle;
			return this;
		}
		
		public Builder withMdAbstract(String mdAbstract){
			this.mdAbstract = mdAbstract;
			return this;
		}
		
		public Builder withMdPublished(Boolean mdPublished){
			this.mdPublished = mdPublished;
			return this;
		}
		
		public Builder withMdVersion(Integer mdVersion){
			this.mdVersion = mdVersion;
			return this;
		}
		
		public Builder withMdLanguage(String mdLanguage){
			this.mdLanguage = mdLanguage;
			return this;
		}
		
		public Builder withMdLicense(String mdLicense){
			this.mdLicense = mdLicense;
			return this;
		}
		
		public Builder withMdCreated(String mdCreated){
			this.mdCreated = mdCreated;
			return this;
		}
		
		public Builder withMdRevised(String mdRevised){
			this.mdRevised = mdRevised;
			return this;
		}
		
		public Builder withCover(String cover){
			this.cover = cover;
			return this;
		}
		
		public Builder withLink(String link){
			this.link = link;
			return this;
		}
		
		public Builder withMdSubtitle(String mdSubtitle){
			this.mdSubtitle = mdSubtitle;
			return this;
		}
		
		public Builder withAuthors(String authors){
			this.authors = authors;
			return this;
		}
		
		public Builder withMainAuthor(String mainAuthor){
			this.mainAuthor = mainAuthor;
			return this;
		}
		
		public Builder withEducationLevel(String educationLevel){
			this.educationLevel = educationLevel;
			return this;
		}
		
		public Builder withEpClass(Integer epClass){
			this.epClass = epClass;
			return this;
		}
		
		public Builder withSubject(String subject){
			this.subject = subject;
			return this;
		}
		
		public Builder withZip(String zip){
			this.zip = zip;
			return this;
		}
		
		public Builder withSizeExtracted(Long sizeExtracted){
			this.sizeExtracted = sizeExtracted;
			return this;
		}
		
		public Builder withStatus(Integer status){
			this.status = status;
			return this;			
		}		
		
		public Builder withTransferId(Long transferId){
			this.transferId = transferId;
			return this;
		}
		
		public Builder withLocalPath(String localPath){
			this.localPath = localPath;
			return this;
		}
		
		public Builder withCoverStatus(Integer coverStatus){
			this.coverStatus = coverStatus;
			return this;
		}
		
		public Builder withCoverTransferId(Long coverTransferId){
			this.coverTransferId = coverTransferId;
			return this;
		}
		
		public Builder withCoverLocalPath(String coverLocalPath){
			this.coverLocalPath = coverLocalPath;
			return this;
		}
		
		public Builder withBytesSoFar(Integer bytesSoFar){
			this.bytesSoFar = bytesSoFar;
			return this;
		}
		
		public Builder withBytesTotal(Integer bytesTotal){
			this.bytesTotal = bytesTotal;
			return this;
		}
		
		public Builder withZipLocal(String zipLocal){
			this.zipLocal = zipLocal;
			return this;
		}
		
		public Builder withAppVersion(Integer appVersion){
			this.appVersion = appVersion;
			return this;
		}
		
		public Builder withVersion(Book version){
			this.versions.add(version);
			return this;
		}
		
		public Book build(){
			return new Book(this);
		}	
		
		public String getMdContentId(){
			return mdContentId;
		}		
		
	}	
	
}
