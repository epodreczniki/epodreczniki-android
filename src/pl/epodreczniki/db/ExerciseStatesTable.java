package pl.epodreczniki.db;

import android.database.sqlite.SQLiteDatabase;

public class ExerciseStatesTable {

	public static final String TABLE_NAME = "exercise_states";
	
	public static final String C_ID = "id";
	
	public static final String C_LOCAL_USER_ID = "local_user_id";
	
	public static final String C_WOMI_ID = "womi_id";
	
	public static final String C_MD_CONTENT_ID = "md_content_id";
	
	public static final String C_MD_VERSION = "md_version";
	
	public static final String C_VALUE = "value";
	
	public static final String[] COLUMNS = {
		C_ID, C_LOCAL_USER_ID, C_WOMI_ID, 
		C_MD_CONTENT_ID, C_MD_VERSION,C_VALUE
	};
	
	public static final String CREATE_STATEMENT = "create table if not exists "
			+TABLE_NAME
			+"("
			+C_ID + " integer primary key,"
			+C_LOCAL_USER_ID + " integer not null,"
			+C_WOMI_ID + " text not null,"
			+C_MD_CONTENT_ID + " text not null,"
			+C_MD_VERSION + " integer not null,"
			+C_VALUE + " text,"
			+" UNIQUE("+C_LOCAL_USER_ID+","+C_WOMI_ID+","+C_MD_CONTENT_ID+","+C_MD_VERSION+")"
			+");";
	
	public static final String DROP_STATEMENT = "drop table if exists "+TABLE_NAME;
	
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
