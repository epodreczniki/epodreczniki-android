package pl.epodreczniki.db;

import java.util.List;

import pl.epodreczniki.util.Util;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class ExerciseStatesProvider extends ContentProvider{
	
	private BooksDBHelper db;
	
	@Override
	public boolean onCreate() {
		db = BooksDBHelper.get(getContext());
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final int uriType = UriHelper.matcher.match(uri);
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(ExerciseStatesTable.TABLE_NAME);
		final List<String> segments = uri.getPathSegments();
		switch(uriType){
		case UriHelper.EXERCISE_STATES:
			break;
		case UriHelper.EXERCISE_STATES_BY_ID:
			final String id = uri.getLastPathSegment();
			Util.checkLong(id);
			qb.appendWhere(ExerciseStatesTable.C_ID+"="+id);
			break;
		case UriHelper.EXERCISE_STATES_BY_USER_BOOK_WOMI:
			final String localUserId = segments.get(2);
			Util.checkLong(localUserId);
			final String mdContentId = segments.get(4);
			final String mdVersion = segments.get(6);
			Util.checkLong(mdVersion);
			final String womiId = segments.get(8);
			qb.appendWhere(
					ExerciseStatesTable.C_LOCAL_USER_ID+"="+localUserId
					+" AND "+ExerciseStatesTable.C_MD_CONTENT_ID+"='"+mdContentId
					+"' AND "+ExerciseStatesTable.C_MD_VERSION+"="+mdVersion
					+" AND "+ExerciseStatesTable.C_WOMI_ID+"='"+womiId+"'");
			break;
		case UriHelper.EXERCISE_STATES_BY_BOOK_USER:
			final String mdContentId1 = segments.get(2);
			final String mdVersion1 = segments.get(4);
			Util.checkLong(mdVersion1);
			final String localUserId1 = segments.get(6);
			Util.checkLong(localUserId1);
			qb.appendWhere(
					ExerciseStatesTable.C_LOCAL_USER_ID+"="+localUserId1
					+" AND "+ExerciseStatesTable.C_MD_CONTENT_ID+"='"+mdContentId1
					+"' AND "+ExerciseStatesTable.C_MD_VERSION+"="+mdVersion1);
			break;
		default:
				throw new IllegalArgumentException("uri not matched: "+uri);
		}
		SQLiteDatabase dBase = db.getWritableDatabase();
		Cursor c = qb.query(dBase, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public String getType(Uri uri) {	
		return null;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int uriType = UriHelper.matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();
		if(uriType==UriHelper.EXERCISE_STATES){
			final long rowId = dBase.insert(ExerciseStatesTable.TABLE_NAME,null,values);
			if(rowId>-1){
				Uri res = UriHelper.exerciseStateById(rowId);
				getContext().getContentResolver().notifyChange(UriHelper.EXERCISE_STATES_URI, null);
				getContext().getContentResolver().notifyChange(res, null);
				return res;
			}
			throw new SQLException("failed to insert exercise state");
		}else{
			throw new IllegalArgumentException("uri not supported for insertion: "+uri);
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int uriType = UriHelper.matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();
		final List<String> segments = uri.getPathSegments();
		int rowsDeleted = 0;
		switch(uriType){
		case UriHelper.EXERCISE_STATES:
			rowsDeleted = dBase.delete(ExerciseStatesTable.TABLE_NAME, selection, selectionArgs);
			break;			
		case UriHelper.EXERCISE_STATES_BY_ID:
			final String id = uri.getLastPathSegment();
			Util.checkLong(id);
			rowsDeleted = dBase.delete(ExerciseStatesTable.TABLE_NAME, 
					ExerciseStatesTable.C_ID+"="+id+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":"")
					, selectionArgs);
			break;
		case UriHelper.EXERCISE_STATES_BY_USER:
			final String userId = uri.getLastPathSegment();
			Util.checkLong(userId);
			rowsDeleted = dBase.delete(ExerciseStatesTable.TABLE_NAME, 
					ExerciseStatesTable.C_LOCAL_USER_ID+"="+userId+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":"")
					, selectionArgs);
			break;
		case UriHelper.EXERCISE_STATES_BY_USER_BOOK_WOMI:		
			final String localUserId = segments.get(2);
			Util.checkLong(localUserId);
			final String mdContentId = segments.get(4);
			final String mdVersion = segments.get(6);
			Util.checkLong(mdVersion);
			final String womiId = segments.get(8);
			rowsDeleted = dBase.delete(ExerciseStatesTable.TABLE_NAME, 
					ExerciseStatesTable.C_LOCAL_USER_ID
					+"="
					+localUserId
					+" AND "
					+ExerciseStatesTable.C_MD_CONTENT_ID
					+"='"
					+mdContentId
					+"' AND "
					+ExerciseStatesTable.C_MD_VERSION
					+"="
					+mdVersion
					+" AND "
					+ExerciseStatesTable.C_WOMI_ID
					+"='"
					+womiId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'")
					, selectionArgs);
			break;		
		default:
			throw new IllegalArgumentException("uri not supported for deletion: "+uri);
		}
		getContext().getContentResolver().notifyChange(UriHelper.EXERCISE_STATES_URI, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
	    final int uriType = UriHelper.matcher.match(uri);
	    final SQLiteDatabase dBase = db.getWritableDatabase();
	    final List<String> segments = uri.getPathSegments();
	    int rowsUpdated = 0;
	    switch(uriType){
	    case UriHelper.EXERCISE_STATES:
	    	rowsUpdated = dBase.update(ExerciseStatesTable.TABLE_NAME, values, selection, selectionArgs);
	    	break;
	    case UriHelper.EXERCISE_STATES_BY_ID:
	    	final String id = uri.getLastPathSegment();
	    	rowsUpdated = dBase.update(ExerciseStatesTable.TABLE_NAME, values
	    			, ExerciseStatesTable.C_ID
	    			+"="
	    			+id
	    			+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":"")
	    			, selectionArgs);
	    	break;	    
	    case UriHelper.EXERCISE_STATES_BY_USER_BOOK_WOMI:	    	 		
	    	final String localUserId = segments.get(2);
	    	Util.checkLong(localUserId);
	    	final String mdContentId = segments.get(4);
	    	final String mdVersion = segments.get(6);
	    	Util.checkLong(mdVersion);
	    	final String womiId = segments.get(8);
	    	rowsUpdated = dBase.update(ExerciseStatesTable.TABLE_NAME, values
	    			, ExerciseStatesTable.C_LOCAL_USER_ID
	    			+"="
	    			+localUserId
	    			+" AND "
	    			+ExerciseStatesTable.C_MD_CONTENT_ID
	    			+"='"
	    			+mdContentId
	    			+"' AND "
	    			+ExerciseStatesTable.C_MD_VERSION
	    			+"="
	    			+mdVersion
	    			+" AND "
	    			+ExerciseStatesTable.C_WOMI_ID
	    			+"='"
	    			+womiId
	    			+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'")
	    			, selectionArgs);
	    	break;
	    	default:
	    		throw new IllegalArgumentException("uri not supported for update: "+uri);
	    }
	    getContext().getContentResolver().notifyChange(UriHelper.EXERCISE_STATES_URI, null);
	    getContext().getContentResolver().notifyChange(uri,null);
		return rowsUpdated;
	}
	
	public static class UriHelper{
		
		public static final String SCHEME = "content";
		
		public static final String AUTHORITY = "pl.epodreczniki.exercisestatesprovider";
		
		public static final String SEG_EXERCISE_STATE = "exercisestate";
		
		public static final String SEG_ID = "id";
		
		public static final String SEG_LOCAL_USER_ID = "localuser";
		
		public static final String SEG_WOMI_ID = "womiid";
		
		public static final String SEG_MD_CONTENT_ID = "mdcontendid";
		
		public static final String SEG_MD_VERSION = "mdversion";
		
		public static final int EXERCISE_STATES = 1;
		
		public static final int EXERCISE_STATES_BY_ID = 2;
		
		public static final int EXERCISE_STATES_BY_USER_BOOK_WOMI = 3;
		
		public static final int EXERCISE_STATES_BY_BOOK_USER = 4;
		
		public static final int EXERCISE_STATES_BY_USER = 5;
		
		public static final Uri EXERCISE_STATES_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(SEG_EXERCISE_STATE).build();
		
		private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH){{
			addURI(AUTHORITY, exerciseStatePathForMatcher(), EXERCISE_STATES);
			addURI(AUTHORITY, exerciseStatePathForMatcher(SEG_ID), EXERCISE_STATES_BY_ID);
			addURI(AUTHORITY, exerciseStatePathForMatcher(SEG_LOCAL_USER_ID), EXERCISE_STATES_BY_USER);
			addURI(AUTHORITY, exerciseStatePathForMatcher(SEG_LOCAL_USER_ID,SEG_MD_CONTENT_ID,SEG_MD_VERSION,SEG_WOMI_ID), EXERCISE_STATES_BY_USER_BOOK_WOMI);
			addURI(AUTHORITY, exerciseStatePathForMatcher(SEG_MD_CONTENT_ID,SEG_MD_VERSION,SEG_LOCAL_USER_ID), EXERCISE_STATES_BY_BOOK_USER);
		}};
		
		public static Uri exerciseStateById(long id){
			return EXERCISE_STATES_URI.buildUpon()
					.appendPath(SEG_ID)
					.appendEncodedPath(String.valueOf(id)).build();
		}
		
		public static Uri exerciseStatesByLocalUserId(long localUserId){
			return EXERCISE_STATES_URI.buildUpon()
					.appendPath(SEG_LOCAL_USER_ID)
					.appendEncodedPath(String.valueOf(localUserId)).build();
		}
		
		public static Uri exerciseStateByUserBookWomi(long localUserId, String mdContentId, int mdVersion, String womiId){
			return EXERCISE_STATES_URI.buildUpon()
					.appendPath(SEG_LOCAL_USER_ID)
					.appendEncodedPath(String.valueOf(localUserId))
					.appendPath(SEG_MD_CONTENT_ID)
					.appendEncodedPath(mdContentId)
					.appendPath(SEG_MD_VERSION)
					.appendEncodedPath(String.valueOf(mdVersion))
					.appendPath(SEG_WOMI_ID)
					.appendEncodedPath(womiId).build();
		}
		
		public static Uri exerciseStatesByBookUser(long localUserId, String mdContentId, int mdVersion){
			return EXERCISE_STATES_URI.buildUpon()
					.appendPath(SEG_MD_CONTENT_ID)
					.appendEncodedPath(mdContentId)
					.appendPath(SEG_MD_VERSION)
					.appendEncodedPath(String.valueOf(mdVersion))
					.appendPath(SEG_LOCAL_USER_ID)
					.appendEncodedPath(String.valueOf(localUserId)).build();					
		}
		
		public static String exerciseStatePathForMatcher(String... segments){
			StringBuilder res = new StringBuilder(SEG_EXERCISE_STATE);
			for(String seg : segments){
				res.append("/").append(seg).append("/*");
			}
			return res.toString();
		}
		
	}
	
}
