package pl.epodreczniki.db;

import pl.epodreczniki.db.BooksDBHelper;
import pl.epodreczniki.db.BooksTable;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class BooksProvider extends ContentProvider{

	public static final String SCHEME = "content";
	
	public static final String AUTHORITY = "pl.epodreczniki.booksprovider";
	
	public static final String BOOKS_PATH = "book";
	
	public static final String VERSIONS_PATH = "version";
	
	private static final int BOOKS = 1;
	
	private static final int BOOKS_BY_CONTENT_ID = 2;
	
	private static final int BOOKS_BY_CONTENT_ID_AND_VERSION = 3;
	
	public static final Uri BOOKS_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(BOOKS_PATH).build();
			
	private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH){{
		addURI(AUTHORITY, BOOKS_PATH, BOOKS);
		addURI(AUTHORITY, BOOKS_PATH+"/*", BOOKS_BY_CONTENT_ID);
		addURI(AUTHORITY, BOOKS_PATH+"/*/"+VERSIONS_PATH+"/#",BOOKS_BY_CONTENT_ID_AND_VERSION);
	}};
	
	private BooksDBHelper db;
	
	@Override
	public boolean onCreate() {
		db = BooksDBHelper.get(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final int uriType = matcher.match(uri);
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(BooksTable.TABLE_NAME);
		switch (uriType) {
		case BOOKS:			
			break;
		case BOOKS_BY_CONTENT_ID:			
			qb.appendWhere(BooksTable.C_MD_CONTENT_ID + "='"
					+ uri.getLastPathSegment()+"'");
			break;
		case BOOKS_BY_CONTENT_ID_AND_VERSION:			
			qb.appendWhere(BooksTable.C_MD_CONTENT_ID + "='"+uri.getPathSegments().get(1)+"' AND "+BooksTable.C_MD_VERSION +"="+uri.getPathSegments().get(3));			
			break;
		default:
			throw new IllegalArgumentException("unknown uri: " + uri);
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
		final int uriType = matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();		
		switch (uriType) {
		case BOOKS:
			final String contentId = values.getAsString(BooksTable.C_MD_CONTENT_ID);
			final long insertRes = dBase.insert(BooksTable.TABLE_NAME, null, values);
			if (insertRes > -1) {
				Uri res = Uri.withAppendedPath(BOOKS_URI, contentId);
				getContext().getContentResolver().notifyChange(BOOKS_URI, null);
				getContext().getContentResolver().notifyChange(res, null);
				return res;
			} 			
			throw new SQLException("failed to insert for uri: "+uri);			
		default:
			throw new IllegalArgumentException("Unknown uri: " + uri);
		}			
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = matcher.match(uri);
		SQLiteDatabase dBase = db.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case BOOKS:
			rowsDeleted = dBase.delete(BooksTable.TABLE_NAME, selection,
					selectionArgs);
			break;
		case BOOKS_BY_CONTENT_ID:			
			rowsDeleted = dBase.delete(
					BooksTable.TABLE_NAME,
					BooksTable.C_MD_CONTENT_ID
							+ "='"
							+ uri.getLastPathSegment()
							+ (!TextUtils.isEmpty(selection) ? "' and ("
									+ selection + ")" : "'"), selectionArgs);
			break;
		case BOOKS_BY_CONTENT_ID_AND_VERSION:
			rowsDeleted = dBase.delete(BooksTable.TABLE_NAME, 
					BooksTable.C_MD_CONTENT_ID
					+ "='"
					+ uri.getPathSegments().get(1)+"' AND "
					+ BooksTable.C_MD_VERSION
					+ "="
					+ uri.getPathSegments().get(3)
					+ (!TextUtils.isEmpty(selection)?" AND ("+selection+")":""), selectionArgs);			
			break;
		default:
			throw new IllegalArgumentException("Unknown uri: " + uri);
		}
		getContext().getContentResolver().notifyChange(BOOKS_URI, null);
		getContext().getContentResolver().notifyChange(uri, null);		
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = matcher.match(uri);
		SQLiteDatabase dBase = db.getWritableDatabase();
		int rowsUpdated = 0;
		switch(uriType){
			case BOOKS:
				rowsUpdated = dBase.update(BooksTable.TABLE_NAME, values, selection, selectionArgs);
				break;
			case BOOKS_BY_CONTENT_ID:				
				rowsUpdated = dBase.update(BooksTable.TABLE_NAME, values, 
						BooksTable.C_MD_CONTENT_ID+"='"+uri.getLastPathSegment()
						+(!TextUtils.isEmpty(selection)?"' and ("+selection+")":"'"), selectionArgs);
				break;
			case BOOKS_BY_CONTENT_ID_AND_VERSION:
				rowsUpdated = dBase.update(BooksTable.TABLE_NAME, values, 
						BooksTable.C_MD_CONTENT_ID+"='"+uri.getPathSegments().get(1)+"' AND "+BooksTable.C_MD_VERSION+"="+uri.getPathSegments().get(3)
						+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":""), selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("unknown uri: "+uri);
		}
		getContext().getContentResolver().notifyChange(BOOKS_URI, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
