package pl.epodreczniki.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import pl.epodreczniki.db.BooksDBHelper;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.ConnectionDetector;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;

public class CleanupService extends IntentService{

	private static final String DELETE_PREFIX = "__delete__";
	
	public CleanupService() {
		super("CleanupService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.e("CLEANUP", "service started");
		if(!ConnectionDetector.isExternalStorageAvailable()){
			Log.e("CLEANUP", "no external storage");
			return;
		}
		BooksDBHelper.get(this).getWritableDatabase();		
		File booksDir = getExternalFilesDir(Constants.BOOKS_DIR);			
		File[] booksInDir = booksDir==null?new File[0]:booksDir.listFiles();		
		booksInDir = booksInDir==null?new File[0]:booksInDir;
		Cursor c = getContentResolver().query(BooksProvider.BOOKS_URI, new String[]{BooksTable.C_MD_CONTENT_ID}, null, null, null); 
		List<String> mdContentIds = new ArrayList<String>();
		Log.e("CLEANUP", "bks: "+booksInDir.length);
		if(c!=null){
			while(c.moveToNext()){
				mdContentIds.add(c.getString(0));	
			}			
			c.close();
		}
		for(File f : booksInDir){
			if(f.isDirectory() && !f.getName().startsWith(DELETE_PREFIX)){			
				if(!mdContentIds.contains(f.getName())){
					f.renameTo(new File(booksDir,DELETE_PREFIX+f.getName()));
				}
			}
		}			
		File coversDir = getExternalFilesDir(Constants.COVERS_DIR);
		File[] coversInDir = coversDir==null?new File[0]:coversDir.listFiles();
		coversInDir = coversInDir==null?new File[0]:coversInDir;
		for(File f : coversInDir){
			if(f.isDirectory() && !f.getName().startsWith(DELETE_PREFIX)){
				if(!mdContentIds.contains(f.getName())){
					f.renameTo(new File(coversDir,DELETE_PREFIX+f.getName()));
				}
			}
		}
		File[] books = booksDir.listFiles();
		for(File book : books){
			if(book.isDirectory() && book.getName().startsWith(DELETE_PREFIX)){
				Log.e("CLEANUP", "deleting book: "+book.getName());
				boolean res = Util.delete(book);
				Log.e("CLEANUP", res?"SUCCESS":"FAILED");
			}
		}
		File[] covers = coversDir.listFiles();
		for(File cover : covers){
			if(cover.isDirectory() && cover.getName().startsWith(DELETE_PREFIX)){
				Log.e("CLEANUP", "deleting cover: "+cover.getName());
				boolean res = Util.delete(cover);
				Log.e("CLEANUP", res?"SUCCESS":"FAILED");
			}
		}
		Cursor zipCursor = getContentResolver().query(BooksProvider.BOOKS_URI, new String[]{BooksTable.C_ZIP_LOCAL}, BooksTable.C_STATUS+"=?", new String[]{BookStatus.READY.getStringVal()}, null);
		List<File> zips = new ArrayList<File>();
		if(zipCursor!=null){
			while(zipCursor.moveToNext()){
				String zipUrlStr = zipCursor.getString(0);
				if(zipUrlStr!=null){
					String path = Uri.parse(zipUrlStr).getPath();
					if(path!=null){
						File f = new File(path);
						if(f.exists()){
							zips.add(f);
						}
					}
				}
			} 
			zipCursor.close();
		}
		for(File f : zips){
			Log.e("CLEANUP", "deleting zip: "+f.getPath());
			f.delete();
		}
	}	

}
