package pl.epodreczniki.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import pl.epodreczniki.R;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.model.CoverStatus;
import android.app.DownloadManager;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public final class TransferHelper {

	private static final String HEAD_ACCEPT = "Accept";

	private static final String HEAD_ACCEPT_VALUE = "application/json; application/psnc.epo.api-v2.1";
		
	public static final String COLLECTIONS_PATH = "";	
	
	public static final String API_BASE = "url to api";		

	private TransferHelper() {
	}

	public static void stop(Context ctx, Book b) {
		DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		final BookStatus stat = BookStatus.fromInteger(b.getStatus());
		Log.e("TH stop", "stopping book with status " + stat);
		Log.e("TH stop", "stopping book with transferid " + b.getTransferId());
		long tid = -1;
		if (stat == BookStatus.DOWNLOADING) {
			tid = b.getTransferId();
		} else if (stat == BookStatus.UPDATE_DOWNLOADING) {
			if (b.getVersions().size() > 0) {
				tid = b.getVersions().get(0).getTransferId();
			} else {
				Log.e("TH stop", "empty versions for update, this should never happen");
			}
		}
		if (tid != -1) {
			Log.e("TH stop", "should see this message");
			final int removed = dm.remove(tid);
			Log.e("TH stop", "num of stopped downloads: " + removed);
			new Thread(new StopUpdater(ctx, b)).start();			
		}
	}

	public static void delete(Context ctx, Book b) {
		if (BookStatus.fromInteger(b.getStatus()).equals(BookStatus.READY)) {
			new Thread(new BookDeleter(ctx.getApplicationContext(), b)).start();
		}
	}

	public static void deleteZip(Context ctx, long transferId, String zipPath) {
		new Thread(new ZipDeleter(ctx.getApplicationContext(), transferId, zipPath)).start();
	}	
	
	private static boolean isAbsolute(String test){
		return test.startsWith("http://")||test.startsWith("https://");
	}

	private static long rt(Context ctx, Book b, boolean override) {
		final DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);	
		final Uri uri = isAbsolute(b.getZip())?Uri.parse(b.getZip()):Uri.parse(API_BASE + b.getZip());
		Log.e("TH", "DOWNLOAD URI: " + uri);
		final File dir = Util.getExternalStorageBookDir(ctx, b);
		final String filePath = b.getMdContentId() + File.separator + b.getMdVersion() + File.separator + "book.zip";		
		dir.mkdirs();
		final DownloadManager.Request req = new DownloadManager.Request(uri);
		if(override){	
			Log.e("TH","ignoring global settings");
			req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI|DownloadManager.Request.NETWORK_MOBILE);
		}else{
			req.setAllowedNetworkTypes(Util.isMobileNetworkAllowed(ctx)?DownloadManager.Request.NETWORK_WIFI|DownloadManager.Request.NETWORK_MOBILE:DownloadManager.Request.NETWORK_WIFI);			
		}
				
		req.setAllowedOverRoaming(false);
		req.setDestinationInExternalFilesDir(ctx, Constants.BOOKS_DIR, filePath);
		req.setTitle(ctx.getResources().getString(R.string.th_downloadingBook));
		req.setDescription(b.getMdTitle());
		return dm.enqueue(req);
	}

	public static void requestTransfer(Context ctx, Book b, boolean override) {
		final BookStatus status = BookStatus.fromInteger(b.getStatus());
		if (status == BookStatus.REMOTE) {
			long id = rt(ctx, b, override);
			new Thread(new DownloadUpdater(ctx, b, id)).start();
		} else if (status == BookStatus.READY) {
			if (b.isUpdateAvailable()) {
				final Book update = b.getVersions().get(0);
				if (BookStatus.fromInteger(update.getStatus()) == BookStatus.REMOTE) {
					long id = rt(ctx, update, override);
					new Thread(new UpdateUpdater(ctx, b, update, id)).start();
				}
			}
		}
	}

	public static void requestCoverDownload(Context ctx, Book b) {
		final DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		final Uri uri = isAbsolute(b.getCover())?Uri.parse(b.getCover()):Uri.parse(API_BASE + b.getCover());
		final DownloadManager.Request req = new DownloadManager.Request(uri);
		final String filePath = b.getMdContentId() + File.separator + b.getMdVersion() + File.separator + "cover"
				+ b.getCover().substring(b.getCover().lastIndexOf("."));		
		final File dir = new File(ctx.getExternalFilesDir(Constants.COVERS_TMP), filePath).getParentFile();
		dir.mkdirs();
		req.setDestinationInExternalFilesDir(ctx, Constants.COVERS_TMP, filePath);
		req.setAllowedNetworkTypes(Util.isMobileNetworkAllowed(ctx)?DownloadManager.Request.NETWORK_MOBILE|DownloadManager.Request.NETWORK_WIFI:DownloadManager.Request.NETWORK_WIFI);
		req.setAllowedOverRoaming(false);
		req.setTitle(ctx.getResources().getString(R.string.th_downloadingCover));
		req.setDescription(b.getMdTitle());
		req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
		long id = dm.enqueue(req);
		markCoverAsInProgress(ctx, b.getMdContentId(), b.getMdVersion(), id);
	}

	private static void markCoverAsInProgress(Context ctx, String mdContentId, Integer mdVersion, long id) {
		final Uri bookUri = Util.getUriForBook(mdContentId, mdVersion);
		if (bookUri != null) {
			ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_COVER_STATUS, CoverStatus.IN_PROGRESS.getStringVal());
			vals.put(BooksTable.C_COVER_TRANSFER_ID, id);
			ctx.getContentResolver().update(bookUri, vals, null, null);
		}
	}

	public static String getMetaData() throws IOException {		
		return getDataFromUrl(API_BASE + COLLECTIONS_PATH);		
	}
	
	public static String getDataFromUrl(String url) throws IOException{
		BufferedReader br = null;
		try {
			URL theUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
			conn.setRequestProperty(HEAD_ACCEPT, HEAD_ACCEPT_VALUE);
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(5000);
			StringBuilder sb = new StringBuilder();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} finally {
			if (br != null) {
				Util.closeSilently(br);
			}
		}
	}
	
	public static boolean isJsonUrl(String url) throws IOException{
		URL theUrl = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
		boolean redirect = false;
		int status = conn.getResponseCode();
		if (status != HttpURLConnection.HTTP_OK) {
			if (status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER)
			redirect = true;
		}
		if (redirect) {				
			String newUrl = conn.getHeaderField("Location");		 
			conn = (HttpURLConnection) new URL(newUrl).openConnection();
		}			
		conn.setConnectTimeout(6000);
		conn.setReadTimeout(5000);
		String contentType = conn.getHeaderField(Constants.HEADER_CONTENT_TYPE);
		return Constants.CONTENT_TYPE_JSON.equalsIgnoreCase(contentType);
	}
	
	public static String getDevDataFromUrl(String url) throws IOException{
		BufferedReader br = null;
		try {
			URL theUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
			boolean redirect = false;
			int status = conn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				if (status == HttpURLConnection.HTTP_MOVED_TEMP
					|| status == HttpURLConnection.HTTP_MOVED_PERM
						|| status == HttpURLConnection.HTTP_SEE_OTHER)
				redirect = true;
			}
			if (redirect) {		
				Log.e("TH","redirected");
				String newUrl = conn.getHeaderField("Location");		 
				conn = (HttpURLConnection) new URL(newUrl).openConnection();
			}			
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(5000);
			StringBuilder sb = new StringBuilder();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} finally {
			if (br != null) {
				Util.closeSilently(br);
			}
		}
	}

	static class DownloadUpdater extends ContextAwareRunnable {

		private final Book book;

		private final long transferId;

		DownloadUpdater(Context ctx, Book book, long transferId) {
			super(ctx);
			this.book = book;
			this.transferId = transferId;
		}

		@Override
		protected void doWork(Context ctx) {
			if (book != null) {
				Log.e("DU", "" + book.getMdContentId() + " " + book.getMdVersion());
				final Uri bookUri = Util.getUriForBook(book.getMdContentId(), book.getMdVersion());
				final ContentValues vals = new ContentValues();
				vals.put(BooksTable.C_STATUS, BookStatus.DOWNLOADING.getIntVal());
				vals.put(BooksTable.C_TRANSFER_ID, transferId);
				ctx.getContentResolver().update(bookUri, vals, null, null);
			}
		}

	}

	static class UpdateUpdater extends ContextAwareRunnable {

		private final Book parent;

		private final Book update;

		private final long transferId;

		UpdateUpdater(Context ctx, Book parent, Book update, long transferId) {
			super(ctx);
			this.parent = parent;
			this.update = update;
			this.transferId = transferId;
		}

		@Override
		protected void doWork(Context ctx) {
			if (parent != null && update != null) {
				final Uri parentUri = Util.getUriForBook(parent.getMdContentId(), parent.getMdVersion());
				final Uri updateUri = Util.getUriForBook(update.getMdContentId(), update.getMdVersion());
				if (parentUri != null && updateUri != null) {
					ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
					ops.add(ContentProviderOperation.newUpdate(parentUri).withValue(BooksTable.C_STATUS, BookStatus.UPDATE_DOWNLOADING.getIntVal())
							.build());
					ops.add(ContentProviderOperation.newUpdate(updateUri).withValue(BooksTable.C_STATUS, BookStatus.DOWNLOADING.getIntVal())
							.withValue(BooksTable.C_TRANSFER_ID, transferId).build());
					try {
						ctx.getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
					} catch (Exception e) {
						Log.e("UU", "error occured " + e.toString());
					}
				}
			}
		}

	}

	static class StopUpdater extends ContextAwareRunnable {

		private final Book book;

		StopUpdater(Context ctx, Book book) {
			super(ctx);
			this.book = book;
		}

		@Override
		protected void doWork(Context ctx) {
			if (book != null) {
				final Uri bookUri = Util.getUriForBook(book.getMdContentId(), book.getMdVersion());
				if (bookUri != null) {
					final BookStatus status = BookStatus.fromInteger(book.getStatus());
					if (status == BookStatus.DOWNLOADING) {
						if (book.isUpdateAvailable()) {
							ctx.getContentResolver().delete(bookUri, null, null);
						} else {
							final ContentValues vals = new ContentValues();
							vals.put(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal());
							vals.put(BooksTable.C_BYTES_SO_FAR, 0);
							vals.put(BooksTable.C_BYTES_TOTAL, 0);
							vals.put(BooksTable.C_TRANSFER_ID, -1);
							ctx.getContentResolver().update(bookUri, vals, null, null);
						}
					} else if (status == BookStatus.UPDATE_DOWNLOADING) {
						final Book update = book.getVersions().get(0);
						final Uri updateUri = Util.getUriForBook(update.getMdContentId(), update.getMdVersion());
						if (updateUri != null) {
							final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
							ops.add(ContentProviderOperation.newUpdate(bookUri).withValue(BooksTable.C_STATUS, BookStatus.READY.getIntVal()).build());
							if (book.getVersions().size() > 1) {								
								ops.add(ContentProviderOperation.newDelete(updateUri).build());
							} else {
								ops.add(ContentProviderOperation.newUpdate(updateUri).withValue(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal())
										.withValue(BooksTable.C_TRANSFER_ID, -1).withValue(BooksTable.C_BYTES_SO_FAR, 0).withValue(BooksTable.C_BYTES_TOTAL, 0).build());

							}
							try {
								ctx.getContentResolver().applyBatch(BooksProvider.AUTHORITY, ops);
							} catch (Exception e) {
								Log.e("Stop updater", "error occurred: " + e.toString());
							}
						}
					}
				}
			}

		}
	}

	static class DeleteUpdater extends ContextAwareRunnable {

		private final Book book;

		DeleteUpdater(Context ctx, Book book) {
			super(ctx);
			this.book = book;
		}

		@Override
		protected void doWork(Context ctx) {
			if (book != null) {
				final Uri bookUri = Util.getUriForBook(book.getMdContentId(), book.getMdVersion());
				if (bookUri != null) {
					if (book.isUpdateAvailable()) {
						Log.e("DU", "del: " + book.getMdVersion());
						ctx.getContentResolver().delete(bookUri, null, null);
					} else {
						Log.e("DU", "status change: " + book.getMdVersion());
						final ContentValues vals = new ContentValues();
						vals.put(BooksTable.C_STATUS, BookStatus.REMOTE.getIntVal());
						vals.put(BooksTable.C_BYTES_SO_FAR, 0);
						vals.put(BooksTable.C_BYTES_TOTAL, 0);
						ctx.getContentResolver().update(bookUri, vals, null, null);
					}
				}
			}
		}
	}

	static class BookDeleter extends ContextAwareRunnable {

		private final Book book;

		BookDeleter(Context ctx, Book book) {
			super(ctx);
			this.book = book;
		}

		@Override
		protected void doWork(Context ctx) {
			if (book != null) {
				final String mdContentId = book.getMdContentId();
				final Integer mdVersion = book.getMdVersion();
				if (!TextUtils.isEmpty(mdContentId) && mdVersion != null) {
					setStatusDeleting(ctx);					
					final File bookDir = Util.getExternalStorageBookDir(ctx, mdContentId, mdVersion);
					if (bookDir.exists()){
						if(bookDir.isDirectory()) {
							if (Util.delete(bookDir)) {
								updateDatabase(ctx);
							}
						}
					}else{
						updateDatabase(ctx);
					}
				}

			}
		}
		
		private void setStatusDeleting(Context ctx){
			ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_STATUS, BookStatus.DELETING.getIntVal());
			final Uri bookUri = Util.getUriForBook(book.getMdContentId(), book.getMdVersion());
			if(bookUri!=null){
				Log.e("BD","setting status to deleting "+book.getMdContentId());
				ctx.getContentResolver().update(bookUri, vals, null, null);
			}
		}

		private void updateDatabase(Context ctx) {
			new Thread(new DeleteUpdater(ctx, book)).start();
		}

	}

	static class Cleaner extends ContextAwareRunnable {

		private final String mdContentId;

		private final Integer mdVersion;

		Cleaner(Context ctx, String mdContentId, Integer mdVersion) {
			super(ctx);
			this.mdContentId = mdContentId;
			this.mdVersion = mdVersion;
		}

		@Override
		protected void doWork(Context ctx) {			
			if (!TextUtils.isEmpty(mdContentId) && mdVersion != null && mdVersion > 0) {
				final File bookDir = Util.getExternalStorageBookDir(ctx, mdContentId, mdVersion);
				if (bookDir.exists() && bookDir.isDirectory()) {
					Util.delete(bookDir);
				}
			}
		}
				
	}

	static class ZipDeleter extends ContextAwareRunnable {

		private final long transferId;

		private final String zipPath;

		ZipDeleter(Context ctx, long transferId, String zipPath) {
			super(ctx);
			this.transferId = transferId;
			this.zipPath = zipPath;
		}

		@Override
		protected void doWork(Context ctx) {
			final DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
			final int removed = dm.remove(transferId);
			Log.e("ZD", "removed from dm: " + removed);
			if (zipPath != null) {
				final File zip = new File(zipPath);
				if (zip.exists() && zip.isFile()) {
					Log.e("ZD", zip.delete() ? "delete successful" : "delete failed");
				} else {
					Log.e("ZD", "nothing to delete");
				}
			}
		}

	}

}
