package pl.epodreczniki.service;

import java.io.File;
import java.util.ArrayList;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.DevMainActivity;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.model.CoverStatus;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class CompletionService extends IntentService {

	public static final String EXTRA_DOWNLOAD_ID = "CompletionService.extra_download_id";
	
	private Handler handler;

	public CompletionService() {
		super("CompletionService");
	}	

	@Override
	public void onCreate() {
		super.onCreate();
		handler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		long completedId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1);
		if (completedId != -1) {			
			if(Util.isDev(this)){
				checkDev(completedId);
			}else{
				if(!checkBooks(completedId)){
					checkCovers(completedId);	
				}				
			}			
		}
	}
	
	private void showToast(final int message, final int duration){
		showToast(getResources().getString(message),duration);
	}
	
	private void showToast(final String message, final int duration){
		if(message!=null){
			handler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(CompletionService.this, message, duration).show();
				}			
			});			
		}		
	}
	
	private void checkDev(long completedId){
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		long storedId = prefs.getLong(DevMainActivity.DEV_TRANSFER_ID_KEY, -1);
		if(completedId==storedId){
			final Cursor dmc = queryDownloadManager(completedId);
			if(dmc!=null){
				final int statusIdx = dmc
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				final int localUriIdx = dmc
						.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);			
				final int reasonIdx = dmc.getColumnIndex(DownloadManager.COLUMN_REASON);
				String fileUri = null;
				int status = DownloadManager.STATUS_FAILED;
				String reason = "";
				if (dmc.moveToFirst()) {
					fileUri = dmc.getString(localUriIdx);
					status = dmc.getInt(statusIdx);
					reason = dmc.getString(reasonIdx);
				}
				dmc.close();
				if (fileUri != null && status == DownloadManager.STATUS_SUCCESSFUL) {
					Log.e("COMPLETE","dev package download succesful");
					prefs.edit().putInt(DevMainActivity.DEV_PACKAGE_STATUS_KEY,DevMainActivity.STATUS_EXTRACTING).apply();
					final Intent i = new Intent(this,DevExtractorService.class);
					i.putExtra(DevExtractorService.EXTRA_FILE_URI, fileUri);
					startService(i);
				}else if(status == DownloadManager.STATUS_FAILED){						
					Log.e("COMPLETE","dev download failed. reason: "+reason);										
					prefs.edit().putInt(DevMainActivity.DEV_PACKAGE_STATUS_KEY, DevMainActivity.STATUS_REMOTE).apply();
				}else{
					Log.e("COMPLETE","different status");
				}				
			}
		}
	}
	
	private boolean checkBooks(long completedId){
		boolean res = false;
		final Cursor c = getContentResolver().query(
				BooksProvider.BOOKS_URI,
				BooksTable.COLUMNS,
				BooksTable.C_STATUS + "=? AND " + BooksTable.C_TRANSFER_ID
						+ "=?",
				new String[] { BookStatus.DOWNLOADING.getStringVal(),
						String.valueOf(completedId) }, null);
		if(c!=null){
			final String mdContentId = Util.getMdContentId(c,0);
			final Integer mdVersion = Util.getMdVersion(c,0);
			c.close();
			if(mdContentId!=null){
				final Cursor dmc = queryDownloadManager(completedId);
				if(dmc!=null){
					final int statusIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_STATUS);
					final int localUriIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
					final int bytesSoFarIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
					final int bytesTotalIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
					final int reasonIdx = dmc.
							getColumnIndex(DownloadManager.COLUMN_REASON);
					String fileUri = null;
					int status = DownloadManager.STATUS_FAILED;
					int bytesSoFar = 0;
					int bytesTotal = 0;
					int reason = -1;
					if (dmc.moveToFirst()) {
						fileUri = dmc.getString(localUriIdx);
						status = dmc.getInt(statusIdx);
						bytesSoFar = dmc.getInt(bytesSoFarIdx);
						bytesTotal = dmc.getInt(bytesTotalIdx);
						reason = dmc.getInt(reasonIdx);
					}
					dmc.close();
					Integer parentMdVersion = null;
					final Cursor updateCursor = getContentResolver().query(Util.getUriForBooks(mdContentId), BooksTable.COLUMNS, BooksTable.C_STATUS+"=?", new String[]{BookStatus.UPDATE_DOWNLOADING.getStringVal()}, null);
					if(updateCursor!=null){
						if(updateCursor.getCount()>0){
							parentMdVersion = Util.getMdVersion(updateCursor, 0);							
						}						
						updateCursor.close();
					}
					if (fileUri != null
							&& status == DownloadManager.STATUS_SUCCESSFUL) {
						Log.e("CS","download successful");
						final Intent i = new Intent(this,
								ExtractorService.class);
						i.putExtra(ExtractorService.EXTRA_ZIP_URI, fileUri);
						i.putExtra(ExtractorService.EXTRA_CONTENT_ID,
								mdContentId);
						i.putExtra(ExtractorService.EXTRA_TRANSFER_ID,
								completedId);
						i.putExtra(ExtractorService.EXTRA_VERSION, mdVersion);
						if(parentMdVersion!=null){
							i.putExtra(ExtractorService.EXTRA_PARENT_VERSION, parentMdVersion);
							markAsUpdateDownloaded(mdContentId, mdVersion, parentMdVersion, fileUri, bytesSoFar, bytesTotal);
						}else{
							markAsDownloaded(mdContentId, mdVersion, fileUri, bytesSoFar, bytesTotal);
						}											
						startService(i);
					}else if(status == DownloadManager.STATUS_FAILED){
						Log.e("CS","download failed: "+reason);
						showToast(R.string.completion_service_download_failed,Toast.LENGTH_LONG);						
						if(parentMdVersion!=null){
							updateFailed(mdContentId, mdVersion, parentMdVersion);
						}else{							
							downloadFailed(mdContentId, mdVersion);							
						}
						removeFromDownloadManager(completedId);
					}
				}
			}
		}
		return res;
	}

	private void checkCovers(long completedId) {
		Cursor c = getContentResolver().query(
				BooksProvider.BOOKS_URI,
				BooksTable.COLUMNS,
				BooksTable.C_COVER_STATUS + "=? AND "
						+ BooksTable.C_COVER_TRANSFER_ID + "=?",
				new String[] { CoverStatus.IN_PROGRESS.getStringVal(),
						String.valueOf(completedId) }, null);
		if (c != null) {
			final String mdContentId = Util.getMdContentId(c,0);
			final Integer mdVersion = Util.getMdVersion(c, 0);
			c.close();
			if (mdContentId != null) {
				Cursor dmc = queryDownloadManager(completedId);
				if (dmc != null) {
					final int statusIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_STATUS);
					final int localUriIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
					String coverUri = null;
					int status = DownloadManager.STATUS_FAILED;
					if (dmc.moveToFirst()) {
						status = dmc.getInt(statusIdx);
						coverUri = dmc.getString(localUriIdx);
					}
					dmc.close();
					if (status == DownloadManager.STATUS_FAILED) {
						markCoverAsRemote(mdContentId, mdVersion, completedId);
						
					} else if (status == DownloadManager.STATUS_SUCCESSFUL) {
						if (coverUri != null) {
							final File originalFile = new File(Uri.parse(coverUri).getPath());
							final String newUri = coverUri.replaceFirst(
									File.separator + Constants.COVERS_TMP
											+ File.separator, File.separator
											+ Constants.COVERS_DIR
											+ File.separator);
							final File newLocation = new File(Uri.parse(newUri).getPath());
							newLocation.getParentFile().mkdirs();							
							if (originalFile.renameTo(newLocation)) {								
								Log.e("CS","cover rename successful");
								Log.e("CS",""+newLocation);
								coverUri = newUri;
								markCoverAsReady(mdContentId, mdVersion, coverUri, completedId);
								Log.e("CS", "remove folders from covers_tmp");
								File versionDir = originalFile.getParentFile();
								if(versionDir!=null){
									File idDir = versionDir.getParentFile();
									if(idDir!=null){
										Util.delete(idDir);
									}
								}																
							}else{
								Log.e("CS","cover rename failed");
								removeFromDownloadManager(completedId);
							}														
						}
					}
				}
			}
		}
	}	
	
	private int markAsDownloaded(final String mdContentId, final Integer mdVersion, final String fileUri, int bytesSoFar, int bytesTotal){
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if(bookUri!=null && !TextUtils.isEmpty(fileUri)){
			final ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_STATUS, BookStatus.EXTRACTING.getIntVal());
			vals.put(BooksTable.C_ZIP_LOCAL, fileUri);
			vals.put(BooksTable.C_BYTES_SO_FAR, 0);
			vals.put(BooksTable.C_BYTES_TOTAL, bytesTotal);
			return getContentResolver().update(bookUri, vals, null, null);
		}		
		return 0;		
	}
	
	private int downloadFailed(final String mdContentId, final Integer mdVersion){
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if(bookUri!=null){
			final ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal());
			vals.put(BooksTable.C_ZIP_LOCAL, "");
			vals.put(BooksTable.C_BYTES_SO_FAR, 0);
			vals.put(BooksTable.C_BYTES_TOTAL, 0);
			vals.put(BooksTable.C_TRANSFER_ID, -1);
			return getContentResolver().update(bookUri, vals, null, null);
		}
		return 0;
	}
	
	private int markAsUpdateDownloaded(final String mdContentId, final Integer mdVersion, final Integer parentMdVersion, final String fileUri, int bytesSoFar, int bytesTotal){
		int res = 0;
		final Uri uri = Util.getUriForBook(mdContentId, mdVersion);
		final Uri parentUri = Util.getUriForBook(mdContentId, parentMdVersion);
		if(uri!=null && parentUri!=null){
			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation.newUpdate(uri)
					.withValue(BooksTable.C_STATUS, BookStatus.EXTRACTING.getIntVal())
					.withValue(BooksTable.C_ZIP_LOCAL, fileUri)
					.withValue(BooksTable.C_BYTES_SO_FAR, 0)
					.withValue(BooksTable.C_BYTES_TOTAL, bytesTotal)
					.build());
			ops.add(ContentProviderOperation.newUpdate(parentUri)
					.withValue(BooksTable.C_STATUS, BookStatus.UPDATE_EXTRACTING.getIntVal())
					.build());		
			try {
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
				res = 2;
			} catch (Exception e) {
				Log.e("CS","update downloaded error: "+e.toString());
			} 
		}
		return res;
	}	
	
	private int updateFailed(final String mdContentId, final Integer mdVersion, final Integer parentMdVersion){
		int res = 0;
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		final Uri parentUri = Util.getUriForBook(mdContentId, parentMdVersion);
		if(bookUri!=null && parentUri!=null){
			final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			ops.add(ContentProviderOperation.newUpdate(bookUri)
					.withValue(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal())
					.withValue(BooksTable.C_ZIP_LOCAL, "")
					.withValue(BooksTable.C_BYTES_SO_FAR, 0)
					.withValue(BooksTable.C_BYTES_TOTAL, 0)
					.withValue(BooksTable.C_TRANSFER_ID, -1)
					.build());
			ops.add(ContentProviderOperation.newUpdate(parentUri)
					.withValue(BooksTable.C_STATUS, BookStatus.READY.getIntVal())
					.build());
			try{
				getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
				res = 2;
			}catch(Exception e){
				Log.e("CS","update failed error: "+e.toString());
			}			
		}
		return res;
	}

	private int markCoverAsReady(final String mdContentId,
			final Integer mdVersion,
			final String coverUri, final long completedId) {
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if(bookUri!=null){
			removeFromDownloadManager(completedId);
			final ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_COVER_STATUS, CoverStatus.READY.getIntVal());
			vals.put(BooksTable.C_COVER_LOCAL_PATH, coverUri);
			return getContentResolver().update(bookUri,vals,null,null);
		}				
		return 0;
	}
	
	private int markCoverAsRemote(final String mdContentId,
			final Integer mdVersion, final long completedId){
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if(bookUri!=null){
			removeFromDownloadManager(completedId);
			final ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_COVER_STATUS,CoverStatus.REMOTE.getIntVal());
			vals.put(BooksTable.C_COVER_LOCAL_PATH, "");
			return getContentResolver().update(bookUri, vals, null , null);
		}
		return 0;
	}

	private Cursor queryDownloadManager(long completedId) {
		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		DownloadManager.Query q = new DownloadManager.Query();
		q.setFilterById(completedId);
		return dm.query(q);
	}

	private int removeFromDownloadManager(long id) {
		int res = 0;
		try{
			final DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);		
			res = dm.remove(id);	
		}catch(Exception e){
			Log.e("CS","problem with download manager");
		}		
		return res;
	}

}
