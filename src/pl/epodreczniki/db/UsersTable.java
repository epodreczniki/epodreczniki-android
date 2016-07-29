package pl.epodreczniki.db;

import pl.epodreczniki.model.Settings;
import android.database.sqlite.SQLiteDatabase;

public class UsersTable {

	public static final String TABLE_NAME = "users";
	
	public static final String C_LOCAL_USER_ID = "local_user_id";
	
	public static final String C_USER_NAME = "user_name";
	
	public static final String C_ACCOUNT_TYPE = "account_type";
	
	public static final String C_PASSWORD_64 = "password_64";
	
	public static final String C_SALT_64 = "salt_64";
	
	public static final String C_RECOVERY_QUESTION = "recovery_question";
	
	public static final String C_RECOVERY_ANSWER_64 = "recovery_answer_64";
		
	public static final String C_RECOVERY_SALT_64 = "recovery_salt_64";
	
	public static final String C_SETTINGS = "settings";
	
	public static final String[] COLUMNS = {
		C_LOCAL_USER_ID,C_USER_NAME,C_ACCOUNT_TYPE,
		C_PASSWORD_64,C_SALT_64,C_RECOVERY_QUESTION,C_RECOVERY_ANSWER_64,C_RECOVERY_SALT_64,C_SETTINGS
	};
	
	private static final String CREATE_STATEMENT = "create table if not exists "
			+TABLE_NAME
			+"("
			+ C_LOCAL_USER_ID + " integer primary key,"
			+ C_USER_NAME + " text not null,"
			+ C_ACCOUNT_TYPE + " integer not null,"
			+ C_PASSWORD_64 + " text,"
			+ C_SALT_64 + " text,"
			+ C_RECOVERY_QUESTION + " text,"
			+ C_RECOVERY_ANSWER_64 + " text,"
			+ C_RECOVERY_SALT_64 + " text,"
			+ C_SETTINGS + " text not null,"
			+ "UNIQUE("+C_USER_NAME+")"
			+");";
	
	private static final String INITIAL_USER_STATEMENT = "insert into "
			+TABLE_NAME
			+"("
			+ C_LOCAL_USER_ID+", "
			+ C_USER_NAME+", "
			+ C_ACCOUNT_TYPE+", "
			+ C_SETTINGS+") values (0,'admin',0,"
			+ "'"+Settings.getInitialSettings()			
			+"')";
	
	public static void onCreate(SQLiteDatabase db){		
		db.execSQL(CREATE_STATEMENT);
		db.execSQL(INITIAL_USER_STATEMENT);
	}
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
		switch(oldVersion){
		case 1:
		case 2:
			db.execSQL(CREATE_STATEMENT);
			db.execSQL(INITIAL_USER_STATEMENT);
		}			
	}
	
}
