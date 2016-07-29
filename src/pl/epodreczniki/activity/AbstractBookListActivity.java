package pl.epodreczniki.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.epodreczniki.R;
import pl.epodreczniki.db.BooksProvider;
import pl.epodreczniki.db.BooksTable;
import pl.epodreczniki.fragment.ReenterPasswordDialog;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.BookStatus;
import pl.epodreczniki.service.MetadataService;
import pl.epodreczniki.util.ConnectionDetector;
import pl.epodreczniki.util.ContextAwareLoopingRunnable;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public abstract class AbstractBookListActivity extends AbstractBookManagingActivity {

	private static final BookComparator COMPARATOR = new BookComparator();
	
	private Thread progressMonitor;

	private ProgressDialog progressDialog;
	
	private AlertDialog networkDialog;
	
	private LoaderCallbacks<Cursor> mLoaderCallbacks;
	
	private static boolean coldStart = true;

	private BroadcastReceiver metaBr = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			handleProgressDialog();
		}
	};

	private void handleProgressDialog() {
		if (MetadataService.isInProgress()) {
			Log.e("ABLA","showing progress dialog "+System.currentTimeMillis());
			showProgressDialog();
		} else {
			Log.e("ABLA","hiding progress dialog "+System.currentTimeMillis());
			hideProgressDialog();			
		}
	}

	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = ProgressDialog.show(this, getResources().getString(R.string.abla_pd_listRefresh), getResources().getString(R.string.abla_pd_pleaseWait));
		} else {
			if (!progressDialog.isShowing()) {
				progressDialog.show();
			}
		}
	}

	private void hideProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	protected abstract void onBooksUpdate(List<Book> books);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLoaderCallbacks = new LoaderListener(this);
		getLoaderManager().initLoader(0, null, mLoaderCallbacks);		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.net_dialog_message).setTitle(R.string.net_dialog_title);
		builder.setPositiveButton(R.string.dialog_ans_yes, new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getMetadata();							
			}						
		});
		builder.setNegativeButton(R.string.dialog_ans_no, new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {							
			}						
		});
		networkDialog = builder.create();
	}
	
	void refreshBookList(){
		final boolean isMobileAllowed = Util.isMobileNetworkAllowed(this);
		final boolean isMobileOnlyNetwork = Util.isMobileOnlyNetwork(this);
		if(!isMobileAllowed && isMobileOnlyNetwork){
			showNetworkDialog();
		}else{
			getMetadata();
		}			
	}
	
	void showNetworkDialog(){
		networkDialog.show();
	}
	
	void getMetadata(){
		Log.e("ABLA","getting metadata "+System.currentTimeMillis());		
		startService(new Intent(AbstractBookListActivity.this, MetadataService.class));
	}
	
	void showReenterPasswordDialog(){
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(ReenterPasswordDialog.TAG);
		if(prev!=null){
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final ReenterPasswordDialog dial = ReenterPasswordDialog.getInstance();
		dial.show(getFragmentManager(),ReenterPasswordDialog.TAG);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.books_list, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {		
		if(checkUserLoggedIn()){
			MenuItem itemSettings = menu.findItem(R.id.m_settings);
			MenuItem itemLogout = menu.findItem(R.id.m_logout);
			if(!UserContext.getCurrentUser().canOpenAdminSettings()){		
				itemSettings.setVisible(false);
			}
			if(UserContext.getCurrentUser().isInitialAccount()){			
				itemLogout.setVisible(false);
			}
		}			
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(!checkUserLoggedIn()){
			return true;
		}
		switch(item.getItemId()){
		case R.id.m_action_terms:
			startActivity(new Intent(this,TermsActivity.class));
			break;			
		case R.id.m_settings:
			if(UserContext.getCurrentUser().isInitialAccount()){
				startActivity(new Intent(this,SettingsActivity.class));
			}else if(UserContext.getCurrentUser().isAdmin()){
				showReenterPasswordDialog();	
			}			
			break;
		case R.id.m_profile:
			startActivity(new Intent(this,ProfileActivity.class));
			finish();
			break;
		case R.id.m_about:
			startActivity(new Intent(this,AboutActivity.class));
			break;
		case R.id.m_refreshBooksList:
			refreshBookList();
			break;	
		case R.id.m_logout:
			UserContext.logout(this);
			Intent i = new Intent(this,LoginListActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(i);
			finish();
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();		
		if (ConnectionDetector.isDbEmpty()) {
			if (ConnectionDetector.isConnected(this)) {
				if(!Util.isMobileNetworkAllowed(this)&&Util.isMobileOnlyNetwork(this)){
					networkDialog.show();
				}else{
					Log.e("ABLA","requesting metadata. db empty "+ConnectionDetector.isDbEmpty());
					startService(new Intent(this, MetadataService.class));
				}
			} else {
				startActivity(new Intent(this, ProblemWithInternetActivity.class));
				finish();
			}
		}else{
			if(coldStart){
				coldStart = false;
				if(Util.shouldCheckForUpdates(this)){
					if(ConnectionDetector.isConnected(this)){
						if(Util.isMobileNetworkAllowed(this)||!Util.isMobileOnlyNetwork(this)){
							Log.e("ABLA","requesting metadata on start");
							startService(new Intent(this,MetadataService.class));
						}
					}
				}
			}
		}		
		LocalBroadcastManager.getInstance(this).registerReceiver(metaBr, new IntentFilter(MetadataService.ACTION_STATUS_CHANGED));
		handleProgressDialog();
		startProgressMonitor();
		if(UserContext.shouldInvalidateOptions()){
			Log.e("ABL","INVALIDATING OPTIONS");
			invalidateOptionsMenu();
			UserContext.setInvalidateOptions(false);
		}				
		getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(metaBr);
		hideProgressDialog();
		stopProgressMonitor();
		if(networkDialog!=null){
			networkDialog.dismiss();
		}
	}

	private void startProgressMonitor() {
		if (progressMonitor == null) {
			progressMonitor = new Thread(new ProgressMonitor(this));
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

		private final AbstractBookListActivity activity;

		private Cursor data;

		LoaderListener(AbstractBookListActivity activity) {
			this.activity = activity;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(activity, BooksProvider.BOOKS_URI, BooksTable.COLUMNS, null, null, BooksTable.C_MD_CONTENT_ID + " ASC, "
					+ BooksTable.C_STATUS + " DESC, " + BooksTable.C_MD_VERSION + " ASC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			Log.e("ABLA", "LOADER FINISHED");
			if (data != null) {
				this.data = data;
				final List<Book.Builder> books = new ArrayList<Book.Builder>();
				while (data.moveToNext()) {
					Book.Builder b = Util.getBookBuilderFromCursor(data, data.getPosition());
					if (b != null) {
						Book.Builder parent = null;
						for (Book.Builder bookInList : books) {
							if (b.getMdContentId() != null && b.getMdContentId().equals(bookInList.getMdContentId())) {
								parent = bookInList;
								break;
							}
						}
						if (parent != null) {
							parent.withVersion(b.build());
						} else {
							books.add(b);
						}
					}
				}
				data.moveToPosition(-1);
				final List<Book> res = new ArrayList<Book>();
				for (Book.Builder bb : books) {
					res.add(bb.build());
				}
				List<Book> filtered = new ArrayList<Book>();
				if(activity.checkUserLoggedIn()){
					final String eduLevelFilter = UserContext.getCurrentUser().getSettings().getBookFilterEducationLevel();
					final String classFilter = UserContext.getCurrentUser().getSettings().getBookFilterClass();
					if(eduLevelFilter!=null && classFilter!=null){
						for(Book b : res){
							if(eduLevelFilter.equals(b.getEducationLevel())&&(b.getEpClass()==0||classFilter.equals(String.valueOf(b.getEpClass())))){
								filtered.add(b);
							}
						}
					}else{					
						filtered = res;
					}										
				}else{
					Log.e("ABLA", "user not logged in, returning all");
					filtered.addAll(res);
				}
				Collections.sort(filtered,COMPARATOR);
				activity.onBooksUpdate(filtered);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			Log.e("ABLA", "LOADER RESET");
			if (data != null) {
				data.close();
				data = null;
			}
		}

	}

	static class ProgressMonitor extends ContextAwareLoopingRunnable {

		public ProgressMonitor(Context ctx) {
			super(ctx);
		}

		@Override
		protected void doWork(Context ctx) {
			List<Book> booksInProgress = getBooksInProgress(ctx);
			ArrayList<ContentProviderOperation> updates = queryDownloadManager(booksInProgress, ctx);
			try {
				ctx.getContentResolver().applyBatch(BooksProvider.AUTHORITY, updates);
			} catch (Exception e) {
				Log.e("PROG_MON", e.toString());
			}
		}

		@Override
		protected long getPollInterval() {
			return 500;
		}

		private List<Book> getBooksInProgress(Context ctx) {
			Cursor c = ctx.getContentResolver().query(BooksProvider.BOOKS_URI, BooksTable.COLUMNS, BooksTable.C_STATUS + "=?",
					new String[] { BookStatus.DOWNLOADING.getStringVal() }, null);
			List<Book> res = new ArrayList<Book>();
			if (c != null) {
				while (c.moveToNext()) {
					Book b = Util.getBookBuilderFromCursor(c, c.getPosition()).build();
					res.add(b);
				}
				c.close();
			}
			return res;
		}

		private ArrayList<ContentProviderOperation> queryDownloadManager(List<Book> books, Context ctx) {
			ArrayList<ContentProviderOperation> res = new ArrayList<ContentProviderOperation>();
			if (books.size() < 1) {
				return res;
			}
			Map<Long, Book> booksByTransferId = new HashMap<Long, Book>();
			long[] tids = new long[books.size()];
			int cnt = 0;
			for (Book b : books) {
				booksByTransferId.put(b.getTransferId(), b);
				tids[cnt++] = b.getTransferId();
			}
			final DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
			final DownloadManager.Query q = new DownloadManager.Query();
			q.setFilterById(tids);
			Cursor dmc = dm.query(q);
			if (dmc != null) {
				final int transferIdIdx = dmc.getColumnIndex(DownloadManager.COLUMN_ID);
				final int statusIdx = dmc.getColumnIndex(DownloadManager.COLUMN_STATUS);
				final int bytesSoFarIdx = dmc.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
				final int bytesTotalIdx = dmc.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
				while (dmc.moveToNext()) {
					final int status = dmc.getInt(statusIdx);
					final long transferId = dmc.getLong(transferIdIdx);
					final Book book = booksByTransferId.get(transferId);
					if (book != null) {
						final String mdContentId = book.getMdContentId();
						final Integer mdVersion = book.getMdVersion();
						if (status == DownloadManager.STATUS_RUNNING) {
							final int bytesSoFar = dmc.getInt(bytesSoFarIdx);
							final int bytesTotal = dmc.getInt(bytesTotalIdx);
							final ContentProviderOperation cpo = ContentProviderOperation.newUpdate(Util.getUriForBook(mdContentId, mdVersion))
									.withValue(BooksTable.C_BYTES_SO_FAR, bytesSoFar).withValue(BooksTable.C_BYTES_TOTAL, bytesTotal).build();
							res.add(cpo);
						}
					}
				}
				dmc.close();
			}
			return res;
		}

	}
	
	static class BookComparator implements Comparator<Book>{
		
		@Override
		public int compare(Book lhs, Book rhs) {
			if(lhs.getSubject()==null&&rhs.getSubject()==null){
				return 0;
			}else{
				if(lhs.getSubject()==null){					
					return 1;
				}
				if(rhs.getSubject()==null){
					return -1;
				}				
			}
			return lhs.getSubject().compareTo(rhs.getSubject());
		}
		
	}

}
