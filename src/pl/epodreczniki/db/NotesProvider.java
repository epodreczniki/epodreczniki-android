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

public class NotesProvider extends ContentProvider{			

	public static final String BOOKMARK_WHERE = NotesTable.C_TYPE+" in (0,1,2,3)";
	
	public static final String NOTE_WHERE = NotesTable.C_TYPE+" in (4,5,6)";
	
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
		qb.setTables(NotesTable.TABLE_NAME);
		final List<String> segments = uri.getPathSegments();
		switch(uriType){
		case UriHelper.NOTES:			
			break;
		case UriHelper.NOTES_BY_LOCAL_NOTE_ID:
			final String localId = uri.getLastPathSegment();						
			qb.appendWhere(NotesTable.C_LOCAL_NOTE_ID+"='"+localId+"'");
			break;
		case UriHelper.NOTES_BY_NOTE_ID:
			qb.appendWhere(NotesTable.C_NOTE_ID+"='"+uri.getLastPathSegment()+"'");
			break;
		case UriHelper.NOTES_BY_LOCAL_USER:
			final String byLocalUserUserId = uri.getLastPathSegment();
			Util.checkLong(byLocalUserUserId);
			qb.appendWhere(NotesTable.C_LOCAL_USER_ID + "="+byLocalUserUserId);
			break;		
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK:						
			final String byLocalUserBookUserId = segments.get(2);
			Util.checkLong(byLocalUserBookUserId);
			final String byLocalUserBookBookId = segments.get(4);
			qb.appendWhere(NotesTable.C_LOCAL_USER_ID+"="+byLocalUserBookUserId+" AND "+NotesTable.C_HANDBOOK_ID+"='"+byLocalUserBookBookId+"'");
			break;			
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK_MODULE:			
			final String byLocalUserBookModuleUserId = segments.get(2);
			Util.checkLong(byLocalUserBookModuleUserId);
			final String byLocalUserBookModuleBookId = segments.get(4);
			final String byLocalUserBookModuleModuleId = segments.get(6);
			qb.appendWhere(NotesTable.C_LOCAL_USER_ID+"="
					+byLocalUserBookModuleUserId+" AND "
					+NotesTable.C_HANDBOOK_ID+"='"+byLocalUserBookModuleBookId+"' AND "
					+NotesTable.C_MODULE_ID+"='"+byLocalUserBookModuleModuleId+"'");
			break;	
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK_PAGE:
			final String byLocalUserBookPageUserId = segments.get(2);
			Util.checkLong(byLocalUserBookPageUserId);
			final String byLocalUserBookPageBookId = segments.get(4);
			final String byLocalUserBookPagePageId = segments.get(6);
			qb.appendWhere(NotesTable.C_LOCAL_USER_ID+"="
					+byLocalUserBookPageUserId+" AND "
					+NotesTable.C_HANDBOOK_ID+"='"+byLocalUserBookPageBookId+"' AND "
					+NotesTable.C_PAGE_ID+"='"+byLocalUserBookPagePageId+"'");
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
		if(uriType==UriHelper.NOTES){			
			final long rowId = dBase.insert(NotesTable.TABLE_NAME, null, values);
			if(rowId>-1){
				final String localNoteId = values.getAsString(NotesTable.C_LOCAL_NOTE_ID);
				Uri res = UriHelper.noteByLocalNoteId(localNoteId);
				getContext().getContentResolver().notifyChange(UriHelper.NOTES_URI, null);
				getContext().getContentResolver().notifyChange(res, null);
				return res;
			}
			throw new SQLException("failed to insert note");
		}else{
			throw new IllegalArgumentException("Uri not supported for insertion: "+uri);
		}		
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int uriType = UriHelper.matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();
		final List<String> segments = uri.getPathSegments();
		int rowsDeleted = 0;
		switch(uriType){
		case UriHelper.NOTES:
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					selection, selectionArgs);
			break;
		case UriHelper.NOTES_BY_LOCAL_NOTE_ID:
			final String localId = uri.getLastPathSegment();		
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					NotesTable.C_LOCAL_NOTE_ID
					+"='"
					+localId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'")
					, selectionArgs);
			break;
		case UriHelper.NOTES_BY_NOTE_ID:
			final String noteId = uri.getLastPathSegment();
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					NotesTable.C_NOTE_ID
					+"='"
					+noteId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"), 
					selectionArgs);
			break;
		case UriHelper.NOTES_BY_LOCAL_USER:
			final String byLocalUserUserId = uri.getLastPathSegment();
			Util.checkLong(byLocalUserUserId);
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserUserId
					+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":""), 
					selectionArgs);					
			break;
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK:
			final String byLocalUserBookUserId = segments.get(2);
			Util.checkLong(byLocalUserBookUserId);
			final String byLocalUserBookBookId = segments.get(4);
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserBookUserId
					+" AND "
					+NotesTable.C_HANDBOOK_ID
					+"='"
					+byLocalUserBookBookId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"), 
					selectionArgs);
			break;
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK_MODULE:
			final String byLocalUserBookModuleUserId = segments.get(2);
			Util.checkLong(byLocalUserBookModuleUserId);
			final String byLocalUserBookModuleBookId = segments.get(4);
			final String byLocalUserBookModuleModuleId = segments.get(6);
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserBookModuleUserId
					+" AND "
					+NotesTable.C_HANDBOOK_ID
					+"='"
					+byLocalUserBookModuleBookId
					+"' AND "
					+NotesTable.C_MODULE_ID
					+"='"
					+byLocalUserBookModuleModuleId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"),					
					selectionArgs);
			break;
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK_PAGE:
			final String byLocalUserBookPageUserId = segments.get(2);
			Util.checkLong(byLocalUserBookPageUserId);
			final String byLocalUserBookPageBookId = segments.get(4);
			final String byLocalUserBookPagePageId = segments.get(6);
			rowsDeleted = dBase.delete(NotesTable.TABLE_NAME,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserBookPageUserId
					+" AND "
					+NotesTable.C_HANDBOOK_ID
					+"='"
					+byLocalUserBookPageBookId
					+"' AND "
					+NotesTable.C_MODULE_ID
					+"='"
					+byLocalUserBookPagePageId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"),					
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("unknown uri: "+uri);
		}
		getContext().getContentResolver().notifyChange(UriHelper.NOTES_URI, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {			
		int uriType = UriHelper.matcher.match(uri);
		final SQLiteDatabase dBase = db.getWritableDatabase();
		final List<String> segments = uri.getPathSegments();
		int rowsUpdated = 0;
		switch(uriType){
		case UriHelper.NOTES:		
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,
					selection, selectionArgs);
			break;
		case UriHelper.NOTES_BY_LOCAL_NOTE_ID:
			final String localId = uri.getLastPathSegment();			
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,
					NotesTable.C_LOCAL_NOTE_ID
					+"='"
					+localId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'")
					, selectionArgs);
			break;
		case UriHelper.NOTES_BY_NOTE_ID:
			final String noteId = uri.getLastPathSegment();
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,					
					NotesTable.C_NOTE_ID
					+"='"
					+noteId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"), 
					selectionArgs);
			break;		
		case UriHelper.NOTES_BY_LOCAL_USER:
			final String byLocalUserUserId = uri.getLastPathSegment();
			Util.checkLong(byLocalUserUserId);
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserUserId
					+(!TextUtils.isEmpty(selection)?" AND ("+selection+")":""), 
					selectionArgs);					
			break;
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK:
			final String byLocalUserBookUserId = segments.get(2);
			Util.checkLong(byLocalUserBookUserId);
			final String byLocalUserBookBookId = segments.get(4);
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserBookUserId
					+" AND "
					+NotesTable.C_HANDBOOK_ID
					+"='"
					+byLocalUserBookBookId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"), 
					selectionArgs);
			break;		
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK_MODULE:
			final String byLocalUserBookModuleUserId = segments.get(2);
			Util.checkLong(byLocalUserBookModuleUserId);
			final String byLocalUserBookModuleBookId = segments.get(4);
			final String byLocalUserBookModuleModuleId = segments.get(6);
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserBookModuleUserId
					+" AND "
					+NotesTable.C_HANDBOOK_ID
					+"='"
					+byLocalUserBookModuleBookId
					+"' AND "
					+NotesTable.C_MODULE_ID
					+"='"
					+byLocalUserBookModuleModuleId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"),					
					selectionArgs);
			break;
		case UriHelper.NOTES_BY_LOCAL_USER_BOOK_PAGE:
			final String byLocalUserBookPageUserId = segments.get(2);
			Util.checkLong(byLocalUserBookPageUserId);
			final String byLocalUserBookPageBookId = segments.get(4);
			final String byLocalUserBookPagePageId = segments.get(6);
			rowsUpdated = dBase.update(NotesTable.TABLE_NAME, values,
					NotesTable.C_LOCAL_USER_ID
					+"="
					+byLocalUserBookPageUserId
					+" AND "
					+NotesTable.C_HANDBOOK_ID
					+"='"
					+byLocalUserBookPageBookId
					+"' AND "
					+NotesTable.C_MODULE_ID
					+"='"
					+byLocalUserBookPagePageId
					+(!TextUtils.isEmpty(selection)?"' AND ("+selection+")":"'"),					
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("unknown uri: "+uri);
		}
		getContext().getContentResolver().notifyChange(UriHelper.NOTES_URI, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}
	
		
		
	public static class UriHelper{				
	
		public static final String SCHEME = "content";
		
		public static final String AUTHORITY = "pl.epodreczniki.notesprovider";
		
		public static final String SEG_NOTE = "note";
		
		public static final String SEG_BOOKMARK = "bookmark";
		
		public static final String SEG_LOCAL_NOTE_ID = "localid";
		
		public static final String SEG_NOTE_ID = "noteid";
		
		public static final String SEG_LOCAL_USER = "localuser";
		
		public static final String SEG_USER = "user";
		
		public static final String SEG_BOOK = "book";
		
		public static final String SEG_MODULE = "module";
		
		public static final String SEG_PAGE = "page";
		
		public static final int NOTES = 1;
		
		public static final int NOTES_BY_LOCAL_NOTE_ID = 2;
		
		public static final int NOTES_BY_NOTE_ID = 3;
		
		public static final int NOTES_BY_LOCAL_USER = 4;
		
		public static final int NOTES_BY_LOCAL_USER_BOOK = 5;
		
		public static final int NOTES_BY_LOCAL_USER_BOOK_MODULE = 6;
		
		public static final int NOTES_BY_LOCAL_USER_BOOK_PAGE = 7;		
		
		public static final Uri NOTES_URI = new Uri.Builder().scheme(SCHEME).authority(AUTHORITY).appendPath(SEG_NOTE).build();
		
		private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH){{
			addURI(AUTHORITY,notePathForMatcher(),NOTES);
			addURI(AUTHORITY,notePathForMatcher(SEG_LOCAL_NOTE_ID),NOTES_BY_LOCAL_NOTE_ID);
			addURI(AUTHORITY,notePathForMatcher(SEG_NOTE_ID),NOTES_BY_NOTE_ID);
			addURI(AUTHORITY,notePathForMatcher(SEG_LOCAL_USER),NOTES_BY_LOCAL_USER);
			addURI(AUTHORITY,notePathForMatcher(SEG_LOCAL_USER,SEG_BOOK),NOTES_BY_LOCAL_USER_BOOK);
			addURI(AUTHORITY,notePathForMatcher(SEG_LOCAL_USER,SEG_BOOK,SEG_MODULE),NOTES_BY_LOCAL_USER_BOOK_MODULE);
			addURI(AUTHORITY,notePathForMatcher(SEG_LOCAL_USER,SEG_BOOK,SEG_PAGE),NOTES_BY_LOCAL_USER_BOOK_PAGE);	
		}};
		
		public static Uri noteByLocalNoteId(String localNoteId){
			return NOTES_URI.buildUpon()
					.appendPath(SEG_LOCAL_NOTE_ID)
					.appendEncodedPath(localNoteId).build();
		}	
		
		public static Uri noteByNoteId(String noteId){
			return NOTES_URI.buildUpon()
			.appendPath(SEG_NOTE_ID)
			.appendEncodedPath(noteId).build();
		}			
		
		public static Uri noteByLocalUser(long localUserId){
			return NOTES_URI.buildUpon()
					.appendPath(SEG_LOCAL_USER)
					.appendEncodedPath(String.valueOf(localUserId)).build();
		}
		
		public static Uri noteByLocalUserBook(long localUserId,String mdContentId,int mdVersion){			
			return NOTES_URI.buildUpon()
					.appendPath(SEG_LOCAL_USER)
					.appendEncodedPath(String.valueOf(localUserId))
					.appendPath(SEG_BOOK)
					.appendEncodedPath(mdContentId+":"+mdVersion)
					.build();
		}			
		
		public static Uri noteByLocalUserBookModule(long localUserId,String mdContentId,int mdVersion,String mdModuleId){
			return NOTES_URI.buildUpon()
					.appendPath(SEG_LOCAL_USER)
					.appendEncodedPath(String.valueOf(localUserId))
					.appendPath(SEG_BOOK)
					.appendEncodedPath(mdContentId+":"+mdVersion)
					.appendPath(SEG_MODULE)
					.appendEncodedPath(mdModuleId)
					.build();					
		}		
		
		public static Uri noteByLocalUserBookPage(long localUserId,String mdContentId,int mdVersion,String pageId){
			return NOTES_URI.buildUpon()
					.appendPath(SEG_LOCAL_USER)
					.appendEncodedPath(String.valueOf(localUserId))
					.appendPath(SEG_BOOK)
					.appendEncodedPath(mdContentId+":"+mdVersion)
					.appendPath(SEG_PAGE)
					.appendEncodedPath(pageId)
					.build();
		}	
		
		public static String notePathForMatcher(String... segments){
			StringBuilder res = new StringBuilder(SEG_NOTE);
			for(String seg : segments){
				res.append("/").append(seg).append("/*");
			}
			return res.toString();
		}
		
		public static String bookmarkPathForMatcher(String... segments){
			StringBuilder res = new StringBuilder(SEG_BOOKMARK);
			for(String seg : segments){
				res.append("/").append(seg).append("/*");
			}
			return res.toString();
		}
		
		public static boolean isUriBookmark(int uriType){
			return uriType>100&&uriType<200;
		}
		
	}
	
	

}
