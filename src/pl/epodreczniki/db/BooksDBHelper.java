package pl.epodreczniki.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BooksDBHelper extends SQLiteOpenHelper{
	
	private static final String DATABASE_NAME = "books.db";
	
	private static final int DATABASE_VERSION = 3;
	
	private static volatile BooksDBHelper instance;
	
	private BooksDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public static BooksDBHelper get(Context ctx){
		if(instance == null){
			synchronized(BooksDBHelper.class){
				if(instance==null){
					instance = new BooksDBHelper(ctx);
				}
			}
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.e("DBHELPER", "onCreate");
		BooksTable.onCreate(db);
		NotesTable.onCreate(db);
		UsersTable.onCreate(db);
		ExerciseStatesTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.e("DBHELPER", "onUpgrade from: "+oldVersion+" to: "+newVersion);
		BooksTable.onUpgrade(db, oldVersion, newVersion);
		NotesTable.onUpgrade(db, oldVersion, newVersion);
		UsersTable.onUpgrade(db, oldVersion, newVersion);
		ExerciseStatesTable.onUpgrade(db, oldVersion, newVersion);
	}

}
