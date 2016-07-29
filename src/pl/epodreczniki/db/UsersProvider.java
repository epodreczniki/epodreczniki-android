package pl.epodreczniki.db;

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

public class UsersProvider extends ContentProvider {

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
		qb.setTables(UsersTable.TABLE_NAME);
		switch (uriType) {
		case UriHelper.USERS:
			break;
		case UriHelper.USERS_BY_LOCAL_USER_ID:
			final String localUserId = uri.getLastPathSegment();
			Util.checkLong(localUserId);
			qb.appendWhere(UsersTable.C_LOCAL_USER_ID + "=" + localUserId);
			break;
		case UriHelper.USERS_BY_USER_NAME:
			final String userName = uri.getLastPathSegment();
			qb.appendWhere(UsersTable.C_USER_NAME + "='"+userName+"'");
			break;
		default:
			throw new IllegalArgumentException("uri not matched: " + uri);
		}
		SQLiteDatabase dBase = db.getWritableDatabase();
		Cursor c = qb.query(dBase, projection, selection, selectionArgs, null,
				null, sortOrder);
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
		if (uriType == UriHelper.USERS) {
			final SQLiteDatabase dBase = db.getWritableDatabase();
			final long rowId = dBase
					.insert(UsersTable.TABLE_NAME, null, values);
			if (rowId > -1) {
				Uri res = UriHelper.userByLocalUserId(rowId);
				getContext().getContentResolver().notifyChange(
						UriHelper.USERS_URI, null);
				getContext().getContentResolver().notifyChange(res, null);
				return res;
			}
			throw new SQLException("failed to insert: " + uri);
		} else {
			throw new IllegalArgumentException(
					"Uri not supported for insertion: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int uriType = UriHelper.matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case UriHelper.USERS:
			rowsDeleted = dBase.delete(UsersTable.TABLE_NAME, selection, selectionArgs);
			break;
		case UriHelper.USERS_BY_LOCAL_USER_ID:
			final String localUserId = uri.getLastPathSegment();
			Util.checkLong(localUserId);
			rowsDeleted = dBase.delete(UsersTable.TABLE_NAME,
					UsersTable.C_LOCAL_USER_ID
					+"="
					+localUserId
					+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":""),
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("unsupported uri: "+uri);
		}
	    if(rowsDeleted>0){
	    	getContext().getContentResolver().notifyChange(UriHelper.USERS_URI, null);
	    	getContext().getContentResolver().notifyChange(uri, null);
	    }
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final int uriType = UriHelper.matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();
		int rowsUpdated = 0;
		switch(uriType){
		case UriHelper.USERS:
			rowsUpdated = dBase.update(UsersTable.TABLE_NAME, values, selection, selectionArgs);
			break;
		case UriHelper.USERS_BY_LOCAL_USER_ID:
			final String localUserId = uri.getLastPathSegment();
			Util.checkLong(localUserId);
			rowsUpdated = dBase.update(UsersTable.TABLE_NAME, values, 
					UsersTable.C_LOCAL_USER_ID
					+"="
					+localUserId
					+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":""), 
					selectionArgs);
			break;
			default:
				throw new IllegalArgumentException("unsupported uri: "+uri);
		}
		if(rowsUpdated>0){
			getContext().getContentResolver().notifyChange(UriHelper.USERS_URI, null);
	    	getContext().getContentResolver().notifyChange(uri, null);
		}
		return rowsUpdated;
	}	

	public static class UriHelper {

		public static final String SCHEME = "content";

		public static final String AUTHORITY = "pl.epodreczniki.usersprovider";

		public static final String SEG_USER = "user";

		public static final String SEG_LOCAL_USER_ID = "localuserid";
		
		public static final String SEG_USER_NAME = "username";

		public static final int USERS = 1;

		public static final int USERS_BY_LOCAL_USER_ID = 2;
		
		public static final int USERS_BY_USER_NAME = 3;

		public static final Uri USERS_URI = new Uri.Builder().scheme(SCHEME)
				.authority(AUTHORITY).appendPath(SEG_USER).build();

		private static final UriMatcher matcher = new UriMatcher(
				UriMatcher.NO_MATCH) {
			{
				addURI(AUTHORITY, pathForMatcher(), USERS);
				addURI(AUTHORITY, pathForMatcher(SEG_LOCAL_USER_ID),
						USERS_BY_LOCAL_USER_ID);
				addURI(AUTHORITY, pathForMatcher(SEG_USER_NAME), 
						USERS_BY_USER_NAME);
			}
		};

		public static Uri userByLocalUserId(long localUserId) {
			return USERS_URI.buildUpon().appendPath(SEG_LOCAL_USER_ID)
					.appendEncodedPath(String.valueOf(localUserId)).build();
		}
		
		public static Uri userByUserName(String userName) {
			return USERS_URI.buildUpon().appendPath(SEG_USER_NAME)
					.appendEncodedPath(userName).build();
		}

		public static String pathForMatcher(String... segments) {
			StringBuilder res = new StringBuilder(SEG_USER);
			for (String seg : segments) {
				res.append("/").append(seg).append("/*");
			}
			return res.toString();
		}

	}

}
