package pl.epodreczniki.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class BooksTable {

	public static final String TABLE_NAME = "books";
	
	public static final String C_MD_CONTENT_ID = "md_content_id";
	
	public static final String C_MD_TITLE = "md_title";	
	
	public static final String C_MD_ABSTRACT = "md_abstract";
	
	public static final String C_MD_PUBLISHED = "md_published";
	
	public static final String C_MD_VERSION = "md_version";
	
	public static final String C_MD_LANGUAGE = "md_language";
	
	public static final String C_MD_LICENSE = "md_license";	
	
	public static final String C_MD_CREATED = "md_created";
	
	public static final String C_MD_REVISED = "md_revised";
	
	public static final String C_COVER = "cover";
	
	public static final String C_LINK = "link";
	
	public static final String C_MD_SUBTITLE = "md_subtitle";
	
	public static final String C_APP_VERSION = "app_version";
	
	public static final String C_AUTHORS = "authors";
	
	public static final String C_MAIN_AUTHOR = "main_author";
	
	public static final String C_EDUCATION_LEVEL = "education_level";
	
	public static final String C_CLASS = "class";
	
	public static final String C_SUBJECT = "subject";
	
	public static final String C_ZIP = "zip";
	
	public static final String C_EXTRACTED_SIZE = "extracted_size";
	
	public static final String C_STATUS = "status";
	
	public static final String C_TRANSFER_ID = "transfer_id";
	
	public static final String C_LOCAL_PATH = "local_path";
	
	public static final String C_COVER_STATUS = "cover_status";
	
	public static final String C_COVER_TRANSFER_ID = "cover_transfer_id";
	
	public static final String C_COVER_LOCAL_PATH = "cover_local_path";	
	
	public static final String C_BYTES_SO_FAR = "bytes_so_far";
	
	public static final String C_BYTES_TOTAL = "bytes_total";
	
	public static final String C_ZIP_LOCAL = "zip_local";	
	
	public static final String[] COLUMNS = {
		C_MD_CONTENT_ID, C_MD_TITLE, C_MD_ABSTRACT, 
		C_MD_PUBLISHED, C_MD_VERSION, C_MD_LANGUAGE, 
		C_MD_LICENSE, C_MD_CREATED, C_MD_REVISED, 
		C_COVER, C_LINK, C_MD_SUBTITLE, C_AUTHORS, C_MAIN_AUTHOR, 
		C_EDUCATION_LEVEL, C_CLASS, C_SUBJECT, C_ZIP, 
		C_EXTRACTED_SIZE, C_STATUS, C_TRANSFER_ID, C_LOCAL_PATH,
		C_COVER_STATUS, C_COVER_TRANSFER_ID, C_COVER_LOCAL_PATH,
		C_BYTES_SO_FAR, C_BYTES_TOTAL, C_ZIP_LOCAL, C_APP_VERSION
	};
			
	private static final String CREATE_STATEMENT = "create table "
			+TABLE_NAME
			+"("
			+ C_MD_CONTENT_ID + " text not null,"
			+ C_MD_TITLE + " text,"			
			+ C_MD_ABSTRACT + " text,"
			+ C_MD_PUBLISHED + " integer,"
			+ C_MD_VERSION + " integer not null,"
			+ C_MD_LANGUAGE + " text,"
			+ C_MD_LICENSE + " text,"
			+ C_MD_CREATED + " text,"
			+ C_MD_REVISED + " text,"			
			+ C_COVER + " text, "
			+ C_LINK + " text,"
			+ C_MD_SUBTITLE + " text,"
			+ C_AUTHORS + " text,"
			+ C_MAIN_AUTHOR + " text,"
			+ C_EDUCATION_LEVEL +" text,"
			+ C_CLASS + " integer,"
			+ C_SUBJECT + " text,"
			+ C_ZIP + " text,"
			+ C_EXTRACTED_SIZE +" integer,"
			+ C_STATUS + " integer not null default 0,"
			+ C_TRANSFER_ID + " integer,"
			+ C_LOCAL_PATH + " text,"
			+ C_COVER_STATUS + " integer not null default 0,"
			+ C_COVER_TRANSFER_ID + " integer,"
			+ C_COVER_LOCAL_PATH + " text, "
			+ C_BYTES_SO_FAR + " integer default 0, "
			+ C_BYTES_TOTAL + " integer default 0, "
			+ C_ZIP_LOCAL + " text,"
			+ C_APP_VERSION + " integer not null default 1,"
			+ "UNIQUE("+C_MD_CONTENT_ID+","+C_MD_VERSION+")"
			+");";
	
	private static final String ALTER_STATEMENT_V1_V2 = "alter table "
			+TABLE_NAME
			+" add column "
			+ C_APP_VERSION
			+" integer not null default 1;";
	
	private static final String UPDATE_DEFAULT_STATEMENT_V1_V2 = "update "
			+TABLE_NAME
			+" set "
			+C_APP_VERSION
			+"=1;";
	
	public static void onCreate(SQLiteDatabase db){
		db.execSQL(CREATE_STATEMENT);
	}
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){		
		switch(oldVersion){
		case 1:
			Log.e("BOOKS TABLE","UPDATE 1->2");
			db.execSQL(ALTER_STATEMENT_V1_V2);
			db.execSQL(UPDATE_DEFAULT_STATEMENT_V1_V2);
		case 2:
			db.delete(TABLE_NAME, null, null);
		}				
	}	
	
}
