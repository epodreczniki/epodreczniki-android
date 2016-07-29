package pl.epodreczniki.model;

import java.util.Comparator;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JSONBook extends AbstractBook {

	@SerializedName("md_authors")
	private List<JSONAuthor> mdAuthors;

	private List<JSONFormat> formats;
	
	private List<JSONFormat> covers;

	@SerializedName("md_school")
	private JSONSchool mdSchool;

	@SerializedName("md_subject")
	private JSONSubject mdSubject;	
	
	public List<JSONAuthor> getMdAuthors() {
		return mdAuthors;
	}

	public void setMdAuthors(List<JSONAuthor> mdAuthors) {
		this.mdAuthors = mdAuthors;
	}

	public List<JSONFormat> getFormats() {
		return formats;
	}
	
	public List<JSONFormat> getCovers(){
		return covers;
	}

	public void setFormats(List<JSONFormat> formats) {
		this.formats = formats;
	}

	public JSONSchool getMdSchool() {
		return mdSchool;
	}

	public void setMdSchool(JSONSchool mdSchool) {
		this.mdSchool = mdSchool;
	}

	public JSONSubject getMdSubject() {
		return mdSubject;
	}

	public void setMdSubject(JSONSubject mdSubject) {
		this.mdSubject = mdSubject;
	}

	public static class JSONAuthor {
		private String id;

		@SerializedName("md_first_name")
		private String mdFirstName;

		@SerializedName("md_surname")
		private String mdSurname;

		@SerializedName("md_institution")
		private String mdInstitution;

		@SerializedName("md_email")
		private String mdEmail;

		@SerializedName("md_full_name")
		private String mdFullName;
		
		@SerializedName("full_name")
		private String fullName;

		@SerializedName("role_type")
		private String roleType;
		
		private int order;
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getMdFirstName() {
			return mdFirstName;
		}

		public void setMdFirstName(String mdFirstName) {
			this.mdFirstName = mdFirstName;
		}

		public String getMdSurname() {
			return mdSurname;
		}

		public void setMdSurname(String mdSurname) {
			this.mdSurname = mdSurname;
		}

		public String getMdInstitution() {
			return mdInstitution;
		}

		public void setMdInstitution(String mdInstitution) {
			this.mdInstitution = mdInstitution;
		}

		public String getMdEmail() {
			return mdEmail;
		}

		public void setMdEmail(String mdEmail) {
			this.mdEmail = mdEmail;
		}

		public String getMdFullName() {
			return mdFullName;
		}

		public void setMdFullName(String mdFullName) {
			this.mdFullName = mdFullName;
		}
		
		public String getFullName(){
			return fullName;
		}
		
		public String getRoleType(){
			return roleType;
		}
		
		public int getOrder(){
			return order;
		}
		
		public static class JSONAuthorComparator implements Comparator<JSONAuthor>{

			@Override
			public int compare(JSONAuthor lhs, JSONAuthor rhs) {				
				if(lhs==null && rhs==null){
					return 0;
				}
				if(lhs==null){
					return 1;
				}
				if(rhs==null){
					return -1;
				}				
				if(lhs.getOrder()<rhs.getOrder()){
					return -1;
				}else if(lhs.getOrder()>rhs.getOrder()){
					return 1;
				}
				return 0;
			}
			
		}
		
	}

	public static class JSONFormat {
		private String format;

		private String url;
		
		private Long size;

		public String getFormat() {
			return format;
		}

		public String getUrl() {
			return url;
		}
		
		public Long getSize(){
			return size;
		}
		
	}	

	public static class JSONSchool {

		private Long id;

		@SerializedName("md_education_level")
		private String mdEducationLevel;

		@SerializedName("ep_class")
		private Integer epClass;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getMdEducationLevel() {
			return mdEducationLevel;
		}

		public void setMdEducationLevel(String mdEducationLevel) {
			this.mdEducationLevel = mdEducationLevel;
		}

		public Integer getEpClass() {
			return epClass;
		}

		public void setEpClass(Integer epClass) {
			this.epClass = epClass;
		}
	}

	public static class JSONSubject {

		private Long id;

		@SerializedName("md_name")
		private String mdName;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getMdName() {
			return mdName;
		}

		public void setMdName(String mdName) {
			this.mdName = mdName;
		}

	}

}
