package pl.epodreczniki.db;

import android.database.sqlite.SQLiteDatabase;

public class NotesTable {
	
	public static final String TABLE_NAME = "notes";
	
	public static final String C_ID = "id"; 
	
	public static final String C_NOTE_ID = "note_id";	
	
	public static final String C_USER_ID = "user_id";
	
	public static final String C_HANDBOOK_ID = "handbook_id";
	
	public static final String C_MODULE_ID = "module_id";
	
	public static final String C_PAGE_ID = "page_id";
	
	public static final String C_PAGE_IDX = "page_idx";
	
	public static final String C_LOCATION = "location";
	
	public static final String C_SUBJECT = "subject";
	
	public static final String C_VALUE = "value";
	
	public static final String C_TYPE = "type";
	
	public static final String C_ACCEPTED = "accepted";
	
	public static final String C_REFERENCE_TO = "reference_to";
	
	public static final String C_REFERENCED_BY = "referenced_by";
	
	public static final String C_MODIFY_TIME = "modify_time";
	
	public static final String C_LOCAL_NOTE_ID = "local_note_id";
	
	public static final String C_LOCAL_USER_ID = "local_user_id";
	
	public static final String[] COLUMNS = {
		C_ID,C_LOCAL_NOTE_ID,C_LOCAL_USER_ID,C_NOTE_ID,C_USER_ID,
		C_HANDBOOK_ID,C_MODULE_ID,C_PAGE_ID,C_PAGE_IDX,C_LOCATION,
		C_SUBJECT,C_VALUE,C_TYPE,C_ACCEPTED,C_REFERENCE_TO,C_REFERENCED_BY,
		C_MODIFY_TIME
	};
	
	private static final String CREATE_STATEMENT = "create table if not exists "
			+TABLE_NAME
			+"("
			+ C_ID + " integer primary key,"			
			+ C_LOCAL_NOTE_ID + " text,"
			+ C_LOCAL_USER_ID + " integer not null,"
			+ C_NOTE_ID + " text,"
			+ C_USER_ID + " text,"
			+ C_HANDBOOK_ID + " text not null,"
			+ C_MODULE_ID + " text not null,"
			+ C_PAGE_ID + " text not null,"
			+ C_PAGE_IDX + " integer,"
			+ C_LOCATION + " text not null,"
			+ C_SUBJECT + " text,"
			+ C_VALUE + " text,"
			+ C_TYPE + " integer not null,"
			+ C_ACCEPTED + " integer,"
			+ C_REFERENCE_TO + " text,"
			+ C_REFERENCED_BY + " text,"
			+ C_MODIFY_TIME + " text,"					
			+ "UNIQUE("+C_LOCAL_NOTE_ID+")"
			+");";
	
	public static void onCreate(SQLiteDatabase db){
		db.execSQL(CREATE_STATEMENT);		
	}
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
		switch(oldVersion){
		case 1:
		case 2:
			db.execSQL(CREATE_STATEMENT);
		}		
	}
	
}
