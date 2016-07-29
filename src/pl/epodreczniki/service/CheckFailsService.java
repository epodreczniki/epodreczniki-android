package pl.epodreczniki.service;

import java.io.File;
import java.util.ArrayList;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;

public class CheckFailsService extends IntentService{

	public CheckFailsService() {
		super("CheckFailsService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		checkFailedDeletes();
		checkFailedExtracts();
	}
	
	private void checkFailedExtracts(){		
		Log.e("CFS", "checking failed extracts");
		final Cursor c = getContentResolver().query(
				BooksProvider.BOOKS_URI,
				BooksTable.COLUMNS,
				BooksTable.C_STATUS+"="+BookStatus.EXTRACTING.getStringVal(),
				null,null
		);
		if(c!=null){
			while(c.moveToNext()){
				final Book b = Util.getBookBuilderFromCursor(c, c.getPosition()).build();
				Log.e("CFS", "got one: "+b.getMdContentId()+" "+b.getMdVersion());
				final File bookDir = Util.getExternalStorageBookDir(getApplicationContext(), b);
				final File bookContent = new File(bookDir,"content");
				boolean deleteRes = false;
				boolean bookContentExists = bookDir.exists() && bookDir.isDirectory();				
				if(bookContentExists){
					deleteRes = Util.delete(bookContent);
				}
				if(!bookContentExists || deleteRes){
					final String zipUriStr = b.getZipLocal();
					final File zipFile = checkZip(zipUriStr);
					final int parentMdVersion = getParentVersion(b);
					if(zipFile!=null){
						Log.e("CFS","trying to extract again");
						startExtractorService(b, parentMdVersion, zipUriStr);						
					}else{
						Log.e("CFS","there's no zip, reverting status");
						if(parentMdVersion==-1){
							updateStatusRemote(b);
						}else{
							updateStatusRemoteForUpdate(b, parentMdVersion);
						}
						try{
							final DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);		
							dm.remove(b.getTransferId());	
						}catch(Exception e){
							Log.e("CFS","problem with download manager");
						}	
					}					
				}else{
					Log.e("CFS", "Houston, we've got a problem.");
				}				
			}
			c.close();
		}
	}
	
	private void startExtractorService(Book b, int parentMdVersion, String zipUriStr){
		Intent i = new Intent(this, ExtractorService.class);
		i.putExtra(ExtractorService.EXTRA_CONTENT_ID, b.getMdContentId());
		i.putExtra(ExtractorService.EXTRA_VERSION, b.getMdVersion());
		i.putExtra(ExtractorService.EXTRA_PARENT_VERSION, parentMdVersion);
		i.putExtra(ExtractorService.EXTRA_TRANSFER_ID, b.getTransferId());
		i.putExtra(ExtractorService.EXTRA_ZIP_URI, zipUriStr);
		startService(i);
	}
	
	private int getParentVersion(Book b){
		int res = -1;
		final Cursor updateCursor = getContentResolver().query(Util.getUriForBooks(b.getMdContentId()), BooksTable.COLUMNS, BooksTable.C_STATUS+"=?", new String[]{BookStatus.UPDATE_DOWNLOADING.getStringVal()}, null);
		if(updateCursor!=null){
			if(updateCursor.getCount()>0){
				res = Util.getMdVersion(updateCursor, 0);
			}
			updateCursor.close();
		}
		return res;
	}	
	
	private File checkZip(String zipUriStr){
		Log.e("CFS","checking zip: "+zipUriStr);
		File res = null;
		if(zipUriStr!=null){
			final Uri zipUri = Uri.parse(zipUriStr);
			final String path = zipUri.getPath();
			if(path!=null){
				File f = new File(path);
				if(f.exists() && f.canRead()){
					res = f;
				}
			}			
		}
		return res;
	}
	
	private void checkFailedDeletes(){		
		final Cursor c = getContentResolver().query(
				BooksProvider.BOOKS_URI, 
				BooksTable.COLUMNS, 
				BooksTable.C_STATUS +"="+BookStatus.DELETING.getStringVal(), null, null);
		if(c!=null){				
			while(c.moveToNext()){									
				final Book b = Util.getBookBuilderFromCursor(c, c.getPosition()).build();					
				final Cursor parentCursor = getContentResolver().query(Util.getUriForBooks(b.getMdContentId()), 
						BooksTable.COLUMNS, BooksTable.C_STATUS+"="+BookStatus.UPDATE_DELETING.getStringVal(), null, null);
				int parentMdVersion = -1;
				if(parentCursor!=null){
					if(parentCursor.moveToFirst()){
						final Book parent = Util.getBookBuilderFromCursor(parentCursor, parentCursor.getPosition()).build();
						parentMdVersion = parent.getMdVersion();
					}
					parentCursor.close();
				}
				Log.e("FD","cleaning up after failed delete "+b.getMdContentId()+" "+b.getMdVersion());
				
				final File bookDir = new File(new File(getApplicationContext().getExternalFilesDir(Constants.BOOKS_DIR), b.getMdContentId()), String.valueOf(b.getMdVersion()));
				if(bookDir.exists() && bookDir.isDirectory()){
					Util.delete(bookDir);
				}
				if(parentMdVersion==-1){
					if(isNewerVersionAvailable(b.getMdContentId())){
						Log.e("FD","newer version, deleting");
						getContentResolver().delete(Util.getUriForBook(b.getMdContentId(), b.getMdVersion()), null,null);
					}else{
						Log.e("FD","no newer version, updating");
						updateStatusRemote(b);
					}
				}else{
					afterFailedUpdate(b.getMdContentId(), b.getMdVersion(), parentMdVersion);
				}					
			}
			c.close();
		}
	}
	
	private boolean isNewerVersionAvailable(String mdContentId){
		final Cursor c = getContentResolver().query(Util.getUriForBooks(mdContentId), 
				BooksTable.COLUMNS, 
				BooksTable.C_STATUS+"="+BookStatus.REMOTE.getStringVal(), 
				null, null);
		boolean res = false;
		if(c!=null){
			res = c.moveToFirst();
			c.close();
		}
		return res;
	}
	
	private void updateStatusRemote(Book b){
		ContentValues vals = new ContentValues();
		vals.put(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal());
		vals.put(BooksTable.C_BYTES_SO_FAR, 0);
		vals.put(BooksTable.C_BYTES_TOTAL, 0);
		vals.put(BooksTable.C_TRANSFER_ID, -1);
		vals.put(BooksTable.C_LOCAL_PATH, "");
		getContentResolver().update(Util.getUriForBook(b.getMdContentId(), b.getMdVersion()), vals, null, null);
	}
	
	private void updateStatusRemoteForUpdate(Book b, int parentMdVersion){
		final Uri bookUri = Util.getUriForBook(b.getMdContentId(), b.getMdVersion());
		final Uri parentUri = Util.getUriForBook(b.getMdContentId(), parentMdVersion);
		if(bookUri!=null && parentUri!=null){
			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation.newUpdate(bookUri)
					.withValue(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal())
					.withValue(BooksTable.C_BYTES_SO_FAR, 0)
					.withValue(BooksTable.C_BYTES_TOTAL, 0)
					.withValue(BooksTable.C_TRANSFER_ID, -1)
					.withValue(BooksTable.C_LOCAL_PATH, "")
					.withValue(BooksTable.C_ZIP_LOCAL, "")
					.build()
					);
			ops.add(ContentProviderOperation.newUpdate(parentUri)
					.withValue(BooksTable.C_STATUS, BookStatus.READY.getIntVal())
					.build());
			try{
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
			}catch(Exception e){
				Log.e("CFS", "updateStatusRemoteForUpdate error");
			}
		}
	}
	
	private void afterFailedUpdate(String mdContentId, int mdVersion, int parentMdVersion){
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		final Uri parentUri = Util.getUriForBook(mdContentId, parentMdVersion);
		if(bookUri!=null && parentUri!=null){
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation.newUpdate(parentUri).withValue(BooksTable.C_STATUS, BookStatus.READY.getIntVal()).build());
			if(isNewerVersionAvailable(mdContentId)){ 
				ops.add(ContentProviderOperation.newDelete(bookUri).build());
			}else{
				ops.add(ContentProviderOperation.newUpdate(bookUri)
						.withValue(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal())
						.withValue(BooksTable.C_BYTES_SO_FAR, 0)
						.withValue(BooksTable.C_BYTES_TOTAL, 0)
						.withValue(BooksTable.C_TRANSFER_ID, -1)
						.withValue(BooksTable.C_LOCAL_PATH, "")
						.build()
						);
			}
			try {
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
			} catch(Exception e){
				Log.e("FD","error setting statuses after failed update");
			}				
		}
	}

}
