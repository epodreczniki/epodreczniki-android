package pl.epodreczniki.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import pl.epodreczniki.R;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class ExtractorService extends IntentService {

	public static final String EXTRA_ZIP_URI = "pl.epodreczniki.service.ExtractorService.extra_zip_uri";

	public static final String EXTRA_CONTENT_ID = "pl.epodreczniki.service.ExtractorService.extra_content_id";

	public static final String EXTRA_VERSION = "pl.epodreczniki.service.ExtractorService.extra_version";

	public static final String EXTRA_TRANSFER_ID = "pl.epodreczniki.service.ExtractorService.extra_transfer_id";

	public static final String EXTRA_PARENT_VERSION = "pl.epodreczniki.service.ExtractorService.extra_parent_version";

	private static final int BUFFER_SIZE = 64 * 1024;		
	
	private Handler handler;

	public ExtractorService() {
		super("ExtractorService");
	}
		
	@Override
	public void onCreate() {	
		super.onCreate();
		handler = new Handler();
	}
			
	@Override
	protected void onHandleIntent(Intent intent) {
		long startTime;		
		final String zipUri = intent.getStringExtra(EXTRA_ZIP_URI);
		final String mdContentId = intent.getStringExtra(EXTRA_CONTENT_ID);
		final long transferId = intent.getLongExtra(EXTRA_TRANSFER_ID, -1);
		final int mdVersion = intent.getIntExtra(EXTRA_VERSION, -1);
		final int parentMdVersion = intent.getIntExtra(EXTRA_PARENT_VERSION, -1);		
		String platformCssLocation = null;
		String platformJsLocation = null;
		if (zipUri != null) {
			updateExtractProgress(mdContentId, mdVersion, 0);
			showToast(R.string.toast_book_prepare, Toast.LENGTH_LONG);
			startTime = System.currentTimeMillis();
			final Uri u = Uri.parse(zipUri);
			ZipFile zipFile = null;
			FileOutputStream fos = null;
			InputStream eis = null;
			final File file = new File(u.getPath());
			try {
				zipFile = new ZipFile(file);
				int entriesTotal = getNumberOfEntries(file);
				int entriesSoFar = 0;
				final File destination = file.getParentFile();
				byte[] buffer = new byte[BUFFER_SIZE];
				Enumeration<?> entries = zipFile.entries();
				while(entries.hasMoreElements()){
					ZipEntry ze = (ZipEntry) entries.nextElement();
					final File dest = new File(destination,ze.getName());
					if(!ze.isDirectory()){
						dest.getParentFile().mkdirs();
						if(dest.getAbsolutePath().endsWith(Constants.PLATFORM_CSS)){
							platformCssLocation = dest.getAbsolutePath();
						}else if(dest.getAbsolutePath().endsWith(Constants.PLATFORM_JS)){
							platformJsLocation = dest.getAbsolutePath();
						}
						eis = zipFile.getInputStream(ze); 								
						fos = new FileOutputStream(dest);						
						int bytesRead;
						while((bytesRead = eis.read(buffer))>0){
							fos.write(buffer,0,bytesRead);
						}
						Util.closeSilently(fos);
						Util.closeSilently(eis);
					}
					++entriesSoFar;
					if(entriesSoFar%50==0){
						updateExtractProgress(mdContentId, mdVersion, extractPercentage(entriesSoFar, entriesTotal));
					}
				}
				if (processCss(platformCssLocation) && processJs(platformJsLocation)) {					
					if (parentMdVersion == -1) {
						finishDownload(mdContentId, mdVersion);
					} else {
						if (finishUpdate(mdContentId, mdVersion, parentMdVersion)) {
							deleteBook(mdContentId,parentMdVersion);
							deleteCover(mdContentId,parentMdVersion);
						}
					}
					Log.e("ES","extract+db time: "+(System.currentTimeMillis()-startTime));
				} else {
					if (parentMdVersion != -1) {
						failedUpdateBegin(mdContentId, mdVersion, parentMdVersion);						
						deleteBook(mdContentId, mdVersion);						
						failedUpdateEnd(mdContentId, mdVersion, parentMdVersion);
					}else{
						updateStatusDeleting(mdContentId, mdVersion);
						deleteBook(mdContentId, mdVersion);
						updateStatusRemote(mdContentId, mdVersion);						
					}	
					showToast(R.string.es_toast_preparingBookFailed,Toast.LENGTH_LONG);					
					Log.e("ES", "entry file not found");
				}				
			} catch (IOException e) {
				Log.e("ES", "error " + e.getMessage());
				showToast(R.string.es_toast_preparingBookFailed,Toast.LENGTH_LONG);				
				if (parentMdVersion != -1) {					
					failedUpdateBegin(mdContentId, mdVersion, parentMdVersion);
					deleteBook(mdContentId,mdVersion);					
					failedUpdateEnd(mdContentId, mdVersion, parentMdVersion);
				}else{					
					updateStatusDeleting(mdContentId, mdVersion);
					deleteBook(mdContentId, mdVersion);					
					updateStatusRemote(mdContentId, mdVersion);
				}	
			} finally{
				Util.closeZipFileSilently(zipFile);
				Util.closeSilently(fos);
				Util.closeSilently(eis);
				try {					
					DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
					dm.remove(transferId);
				} catch (Exception e) {
					Log.e("ES", "error turned off download manager" + e.getMessage());
				}
				if(file.exists()){
					Log.e("ES","your download manager is broken");
					file.delete();
				}
			}			
		}
	}			
	
	private int getNumberOfEntries(File f){
		ZipFile zf = null;
		int res = -1;
		try{
			zf = new ZipFile(f);
			res = zf.size();	
		} catch (IOException e) {
		} finally{
			if(zf!=null){
				try {
					zf.close();
				} catch (IOException e) {
				}				
			}			
		}	
		return res;
	}
	
	private void showToast(final int message, final int duration){		
		showToast(getResources().getString(message),duration);		
	}
	
	private void showToast(final String message, final int duration){
		if(message!=null){
			handler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(ExtractorService.this, message, duration).show();
				}			
			});			
		}		
	}
	
	private boolean processCss(String platformCssAbs){
		boolean res = true;
		if(platformCssAbs!=null){
			final String newLocation = platformCssAbs.replace(Constants.PLATFORM_CSS, Constants.GENERAL_CSS);			
			final File old = new File(platformCssAbs);
			res = old.renameTo(new File(newLocation));
			Log.e("ES","css rename result "+res);
		}
		return res;
	}	
	
	private boolean processJs(String platformJsAbs){
		Log.e("ES","Platform js abs: "+platformJsAbs);
		boolean res = true;
		if(!TextUtils.isEmpty(platformJsAbs)){
			final String newLocation = platformJsAbs.replace(Constants.PLATFORM_JS,Constants.JS_TARGET);
			Log.e("ES","renaming to: "+newLocation);
			final File old = new File(platformJsAbs);
			res = old.renameTo(new File(newLocation));
			Log.e("ES","js rename result: "+res);
		}		
		return res;
	}
	
	private static int extractPercentage(int soFar,int total){
		double res = 0;		
		if(soFar>-1 && total>0){
			res = (double)soFar*100/total;
		}
		return (int)res;
	}
	
	private void updateExtractProgress(String mdContentId, int mdVersion, int progress){
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if(bookUri != null){
			final ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_BYTES_SO_FAR, progress);
			getContentResolver().update(bookUri, vals, null, null);
		}
	}

	private void finishDownload(String mdContentId, int mdVersion) {
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if (bookUri != null) {
			ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_STATUS, BookStatus.READY.getIntVal());
			vals.put(BooksTable.C_LOCAL_PATH, Util.getExternalStorageBookDir(this, mdContentId, mdVersion).getAbsolutePath());
			vals.put(BooksTable.C_TRANSFER_ID, -1);
			getContentResolver().update(bookUri, vals, null, null);
		}
	}

	private boolean finishUpdate(String mdContentId, int mdVersion, int parentMdVersion) {
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		final Uri parentUri = Util.getUriForBook(mdContentId, parentMdVersion);
		if (bookUri != null && parentUri != null) {
			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation.newUpdate(bookUri).withValue(BooksTable.C_STATUS, BookStatus.READY.getIntVal())
					.withValue(BooksTable.C_LOCAL_PATH, Util.getExternalStorageBookDir(this, mdContentId, mdVersion)).withValue(BooksTable.C_TRANSFER_ID, -1).build());
			ops.add(ContentProviderOperation.newDelete(parentUri).build());
			try {
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
				return true;
			} catch (Exception e) {
				Log.e("ES", "error occurred while finishing update " + e.toString());
			}
		}
		return false;
	}

	private void failedUpdateEnd(String mdContentId, int mdVersion, int parentMdVersion) {
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		final Uri parentUri = Util.getUriForBook(mdContentId, parentMdVersion);
		if (bookUri != null && parentUri != null) {
			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			if(isNewerVersionAvailable(mdContentId)){
				ops.add(ContentProviderOperation.newDelete(bookUri).build());
			}else{
				ops.add(ContentProviderOperation.newUpdate(bookUri).withValue(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal())
						.withValue(BooksTable.C_BYTES_SO_FAR, 0).withValue(BooksTable.C_BYTES_TOTAL, 0).withValue(BooksTable.C_TRANSFER_ID, -1)
						.withValue(BooksTable.C_LOCAL_PATH, "").build());	
			}
			
			ops.add(ContentProviderOperation.newUpdate(parentUri).withValue(BooksTable.C_STATUS, BookStatus.READY.getIntVal()).build());
			try {
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
			} catch (Exception e) {
				Log.e("ES", "error occurred while finishing FAILED update " + e.toString());
			}
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
	
	private void failedUpdateBegin(String mdContentId, int mdVersion, int parentMdVersion){
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		final Uri parentUri = Util.getUriForBook(mdContentId, parentMdVersion);
		if(bookUri!=null && parentUri!=null){
			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation.newUpdate(parentUri).withValue(BooksTable.C_STATUS, BookStatus.UPDATE_DELETING.getIntVal()).build());
			ops.add(ContentProviderOperation.newUpdate(bookUri).withValue(BooksTable.C_STATUS, BookStatus.DELETING.getIntVal()).build());
			try{
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
			}catch(Exception e){
				Log.e("ES","failed update begin. setting statuses failed");
			}
		}
	}
	
	private void updateStatusDeleting(String mdContentId, int mdVersion){
		ContentValues vals = new ContentValues();
		vals.put(BooksTable.C_STATUS, BookStatus.DELETING.getIntVal());
		getContentResolver().update(Util.getUriForBook(mdContentId, mdVersion), vals, null, null);
	}
	
	private void updateStatusRemote(String mdContentId, int mdVersion){
		ContentValues vals = new ContentValues();
		vals.put(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal());
		vals.put(BooksTable.C_BYTES_SO_FAR, 0);
		vals.put(BooksTable.C_BYTES_TOTAL, 0);
		vals.put(BooksTable.C_TRANSFER_ID, -1);
		vals.put(BooksTable.C_LOCAL_PATH, "");
		getContentResolver().update(Util.getUriForBook(mdContentId, mdVersion), vals, null, null);
	}
	
	private void deleteBook(String mdContentId, int mdVersion){
		final File bookDir = new File(new File(getExternalFilesDir(Constants.BOOKS_DIR), mdContentId), String.valueOf(mdVersion));
		if (bookDir.exists() && bookDir.isDirectory()) {
			Util.delete(bookDir);
		}		
	}
	
	private void deleteCover(String mdContentId, int mdVersion){
		final File coverDir = new File(new File(getExternalFilesDir(Constants.COVERS_DIR), mdContentId), String.valueOf(mdVersion));
		if(coverDir.exists() && coverDir.isDirectory()){
			Util.delete(coverDir);
		}		
	}

}
