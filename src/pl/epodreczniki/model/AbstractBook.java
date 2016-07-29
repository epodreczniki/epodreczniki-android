package pl.epodreczniki.model;

import com.google.gson.annotations.SerializedName;

public class AbstractBook {

	@SerializedName("md_content_id")
	protected String mdContentId;

	@SerializedName("md_title")
	protected String mdTitle;

	@SerializedName("md_abstract")
	protected String mdAbstract;

	@SerializedName("md_published")
	protected Boolean mdPublished;

	@SerializedName("md_version")
	protected Integer mdVersion;

	@SerializedName("md_language")
	protected String mdLanguage;

	@SerializedName("md_license")
	protected String mdLicense;

	@SerializedName("md_created")
	protected String mdCreated;

	@SerializedName("md_revised")
	protected String mdRevised;	
	
	protected String link;
	
	@SerializedName("md_subtitle")
	protected String mdSubtitle;
	
	@SerializedName("app_version_android")
	protected Integer appVersion;

	public String getMdContentId() {
		return mdContentId;
	}

	public String getMdTitle() {
		return mdTitle;
	}

	public String getMdAbstract() {
		return mdAbstract;
	}

	public Boolean getMdPublished() {
		return mdPublished;
	}

	public Integer getMdVersion() {
		return mdVersion;
	}

	public String getMdLanguage() {
		return mdLanguage;
	}

	public String getMdLicense() {
		return mdLicense;
	}

	public String getMdCreated() {
		return mdCreated;
	}

	public String getMdRevised() {
		return mdRevised;
	}		
	
	public String getLink(){
		return link;
	}

	public String getMdSubtitle() {
		return mdSubtitle;
	}
	
	public Integer getAppVersion(){
		return appVersion;
	}
	
}
