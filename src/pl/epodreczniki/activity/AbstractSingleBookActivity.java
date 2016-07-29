package pl.epodreczniki.activity;

import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.util.ContextAwareLoopingRunnable;
import pl.epodreczniki.util.Util;
import android.app.DownloadManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public abstract class AbstractSingleBookActivity extends AbstractBookManagingActivity {

	public static final String EXTRA_MD_CONTENT_ID = "extra_md_content_id";

	public static final String EXTRA_MD_VERSION = "extra_md_version";
	
	protected abstract void onBookUpdate(Book book);

	private String mdContentId;

	private Thread progressMonitor;

	protected String getMdContentId() {
		return mdContentId;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		if (extras == null || "".equals(mdContentId = extras.getString(EXTRA_MD_CONTENT_ID, ""))) {
			finish();
		} else {
			getLoaderManager().initLoader(0, null, new LoaderListener(mdContentId, this));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		startProgressMonitor();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopProgressMonitor();
	}

	private void startProgressMonitor() {
		if (progressMonitor == null) {
			progressMonitor = new Thread(new BookProgressMonitor(this, mdContentId));
			progressMonitor.start();
		}
	}

	private void stopProgressMonitor() {
		if (progressMonitor != null) {
			progressMonitor.interrupt();
			progressMonitor = null;
		}
	}

	static class LoaderListener implements LoaderCallbacks<Cursor> {

		private final String mdContentId;

		private final AbstractSingleBookActivity activity;

		private Cursor data;

		LoaderListener(String mdContentId, AbstractSingleBookActivity activity) {
			this.mdContentId = mdContentId;
			this.activity = activity;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(activity, Uri.withAppendedPath(BooksProvider.BOOKS_URI, mdContentId), BooksTable.COLUMNS, null, null,
					BooksTable.C_MD_CONTENT_ID + " ASC, " + BooksTable.C_STATUS + " DESC, " + BooksTable.C_MD_VERSION + " ASC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if (data != null) {
				this.data = data;
				Book.Builder b = null;
				while (data.moveToNext()) {
					Book.Builder tmpBook = Util.getBookBuilderFromCursor(data, data.getPosition());
					if (b == null) {
						b = tmpBook;
					} else {
						b.withVersion(tmpBook.build());
					}
				}
				data.moveToPosition(-1);
				activity.onBookUpdate(b.build());
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			if (data != null) {
				data.close();
				data = null;
			}
			Log.e("LOADER RESET", "LOADER RESET");
		}

	}

	static class BookProgressMonitor extends ContextAwareLoopingRunnable {

		private final Uri bookUri;

		public BookProgressMonitor(Context ctx, String mdContentId) {
			super(ctx);
			bookUri = Uri.withAppendedPath(BooksProvider.BOOKS_URI, mdContentId);
		}

		@Override
		protected void doWork(Context ctx) {
			Book b = getBook(ctx);
			if (b != null) {
				queryDownloadManager(b, ctx);
			}
		}

		@Override
		protected long getPollInterval() {
			return 1000;
		}

		private Book getBook(Context ctx) {
			Cursor c = ctx.getContentResolver().query(bookUri, BooksTable.COLUMNS, BooksTable.C_STATUS + "=?",
					new String[] { BookStatus.DOWNLOADING.getStringVal() }, null);
			Book res = null;
			if (c != null) {
				if (c.moveToFirst()) {
					res = Util.getBookBuilderFromCursor(c, c.getPosition()).build();
				}
				c.close();
			}
			return res;
		}

		private void updateBook(Context ctx, String mdContentId, Integer mdVersion, int bytesSoFar, int bytesTotal) {
			final Uri updateUri = Util.getUriForBook(mdContentId, mdVersion);
			final ContentValues vals = new ContentValues();
			vals.put(BooksTable.C_BYTES_SO_FAR, bytesSoFar);
			vals.put(BooksTable.C_BYTES_TOTAL, bytesTotal);
			ctx.getContentResolver().update(updateUri, vals, null, null);
		}

		private void queryDownloadManager(Book b, Context ctx) {
			Long transferId = null;
			if ((transferId = b.getTransferId()) != null) {
				DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
				DownloadManager.Query q = new DownloadManager.Query();
				q.setFilterById(transferId);
				Cursor dmc = dm.query(q);
				if (dmc != null && dmc.moveToFirst()) {
					if (dmc.moveToFirst()) {
						final int statusIdx = dmc.getColumnIndex(DownloadManager.COLUMN_STATUS);
						final int bytesSoFarIdx = dmc.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
						final int bytesTotalIdx = dmc.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
						final int status = dmc.getInt(statusIdx);
						if (status == DownloadManager.STATUS_RUNNING) {
							final Integer bytesSoFar = dmc.getInt(bytesSoFarIdx);
							final Integer bytesTotal = dmc.getInt(bytesTotalIdx);
							if (bytesSoFar != null && bytesTotal != null) {
								updateBook(ctx, b.getMdContentId(), b.getMdVersion(), bytesSoFar, bytesTotal);
							}
						}
					}
					dmc.close();
				} 
			}
		}

	}

}
