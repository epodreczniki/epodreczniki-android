package pl.epodreczniki.activity;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.DownloadManagerProblemDialog;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.util.TransferHelper;
import pl.epodreczniki.util.Util;
import android.app.AlertDialog;
import android.app.DownloadManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public abstract class AbstractBookManagingActivity extends AbstractUserAwareActivity {

	private final Handler handler = new NoManagerHandler(this);

	private AlertDialog networkDialog;

	@Override
	protected void onPause() {
		super.onPause();
		if (networkDialog != null) {
			networkDialog.dismiss();
		}
	}

	protected void requestTransfer(Book b) {
		int appVersion = Util.getAppVersion(this);
		Log.e("ABMA","required version: "+b.getAppVersion());
		if(appVersion<b.getAppVersion()){
			Toast.makeText(this, R.string.toast_newer_app_required, Toast.LENGTH_SHORT).show();
			return;
		}
		long spaceRequired = b.getSizeExtracted() != null ? 2 * b
				.getSizeExtracted() : -1;
		long spaceAvailable = Util.getAvailableSpace();
		if (spaceAvailable > spaceRequired) {
			Log.e("ABMA", "space ok, proceeding with download: "
					+ spaceRequired + " " + spaceAvailable);
			final boolean mobileAllowedByUser = Util
					.isMobileNetworkAllowed(this);
			final boolean isMobileOnlyNetwork = Util.isMobileOnlyNetwork(this);
			
			if (!mobileAllowedByUser && isMobileOnlyNetwork) {
				showQuestion(b);
			} else {		
				Toast.makeText(this, getResources().getString(R.string.toast_bookStartPreparingDownload), Toast.LENGTH_LONG).show();
				new Thread(new TransferRequester(this, b, false)).start();
			}

		} else {
			Log.e("ABMA", "not enough space: " + spaceRequired + " "
					+ spaceAvailable);
			Toast.makeText(this, R.string.not_enough_space, Toast.LENGTH_LONG)
					.show();
		}
	}

	protected void delete(Book b) {
		TransferHelper.delete(this, b);
	}

	protected void stop(Book b) {
		try {
			TransferHelper.stop(this, b);
		} catch (Exception e) {
			final DownloadManagerProblemDialog dial = DownloadManagerProblemDialog
					.newInstance();
			dial.show(getFragmentManager(), DownloadManagerProblemDialog.TAG);
		}
	}

	void showProblemDialog() {
		final DownloadManagerProblemDialog dial = DownloadManagerProblemDialog
				.newInstance();
		dial.show(getFragmentManager(), DownloadManagerProblemDialog.TAG);
	}

	void showQuestion(final Book b) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				R.string.net_dialog_message)
				.setTitle(R.string.net_dialog_title);
		builder.setPositiveButton(R.string.dialog_ans_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new Thread(new TransferRequester(
						AbstractBookManagingActivity.this, b, true)).start();
				Toast.makeText(AbstractBookManagingActivity.this, getResources().getString(R.string.toast_bookStartPreparingDownload), Toast.LENGTH_LONG).show();
				Log.e("ABMA", "override");
			}
		});
		builder.setNegativeButton(R.string.dialog_ans_no, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.e("ABMA", "do not override");
			}
		});
		if (networkDialog == null
				|| (networkDialog != null && !networkDialog.isShowing())) {
			networkDialog = builder.show();
		}

	}

	static class NoManagerHandler extends Handler {

		private final WeakReference<AbstractBookManagingActivity> actRef;

		NoManagerHandler(AbstractBookManagingActivity act) {
			actRef = new WeakReference<AbstractBookManagingActivity>(act);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final AbstractBookManagingActivity act = actRef.get();
			if (act != null) {
				act.showProblemDialog();				
			}
		}

	}

	static class TransferRequester implements Runnable {

		private final WeakReference<AbstractBookManagingActivity> actRef;

		private final Book book;

		private final boolean override;

		TransferRequester(AbstractBookManagingActivity act, Book book,
				boolean override) {
			this.actRef = new WeakReference<AbstractBookManagingActivity>(act);
			this.book = book;
			this.override = override;
		}

		@Override
		public void run() {
			final AbstractBookManagingActivity ctx = actRef.get();
			if (ctx != null) {
				Set<Long> alreadyThereTids = new HashSet<Long>();
				boolean alreadyThere = false;
				final Uri bookUri = Uri.parse(TransferHelper.API_BASE
						+ book.getZip());
				final DownloadManager dm = (DownloadManager) ctx
						.getSystemService(Context.DOWNLOAD_SERVICE);
				final DownloadManager.Query q = new DownloadManager.Query();
				q.setFilterByStatus(DownloadManager.STATUS_PAUSED
						| DownloadManager.STATUS_PENDING
						| DownloadManager.STATUS_RUNNING);
				Cursor dmc = dm.query(q);
				if (dmc != null) {
					final int uriIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_URI);
					final int tidIdx = dmc
							.getColumnIndex(DownloadManager.COLUMN_ID);
					while (dmc.moveToNext()) {
						final String uriStr = dmc.getString(uriIdx);
						final long tid = dmc.getLong(tidIdx);
						if (uriStr != null) {
							final Uri uri = Uri.parse(uriStr);
							if (uri.equals(bookUri)) {
								alreadyThereTids.add(tid);
								alreadyThere |= true;
							}
						}

					}
					dmc.close();
				}
				if (alreadyThere) {
					Log.e("TransferRequester", "already there");
					if (book.getTransferId() < 0) {
						Log.e("TransferRequester", "yet, we continue");
						long toDelete[] = new long[alreadyThereTids.size()];
						int cnt = 0;
						for (long l : alreadyThereTids) {
							toDelete[cnt++] = l;
						}
						try {
							dm.remove(toDelete);
						} catch (Exception e) {
							Log.e("TR", "download manager probably off");
						}
					}

				} else {
					Log.e("TransferRequester",
							"requesting transfer " + book.getMdContentId());
					try {
						TransferHelper.requestTransfer(ctx, book, override);
					} catch (Exception e) {
						ctx.handler.sendEmptyMessage(2);
					}
				}
			}
		}

	}

}
