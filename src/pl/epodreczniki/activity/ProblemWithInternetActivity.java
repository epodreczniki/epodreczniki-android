package pl.epodreczniki.activity;

import java.lang.ref.WeakReference;

import pl.epodreczniki.R;
import pl.epodreczniki.util.ConnectionDetector;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;

public class ProblemWithInternetActivity extends Activity {

	private final Handler handler = new ConnectionHandler(this);

	private Thread poller;

	private ProgressBar pb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_problem_with_internet);

		pb = (ProgressBar) findViewById(R.id.apwi_pb);

		final ImageButton btnSettings = (ImageButton) findViewById(R.id.apwi_btn_settings);
		btnSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
			}

		});

	}

	void navigate() {
		if (Util.isTablet(ProblemWithInternetActivity.this)) {
			Intent i = new Intent(ProblemWithInternetActivity.this, BookGridActivity.class);
			startActivity(i);
		} else {
			Intent i = new Intent(ProblemWithInternetActivity.this, BookListActivity.class);
			startActivity(i);
		}
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (poller == null) {
			poller = new Thread(new ConnectionPoller(this));
			poller.start();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (poller != null) {
			poller.interrupt();
			poller = null;
		}
	}

	static class ConnectionPoller implements Runnable {

		private final WeakReference<ProblemWithInternetActivity> actRef;

		ConnectionPoller(ProblemWithInternetActivity activity) {
			actRef = new WeakReference<ProblemWithInternetActivity>(activity);
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					final ProblemWithInternetActivity act = actRef.get();
					if (act != null) {
						final Message msg = act.handler.obtainMessage(1, ConnectionDetector.isConnected(act) ? 1 : 0, -1);
						act.handler.sendMessage(msg);
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	static class ConnectionHandler extends Handler {

		private final WeakReference<ProblemWithInternetActivity> actRef;

		ConnectionHandler(ProblemWithInternetActivity activity) {
			actRef = new WeakReference<ProblemWithInternetActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final ProblemWithInternetActivity act = actRef.get();
			switch (msg.what) {
			case 1:
				if (msg.arg1 == 1) {
					if (act != null) {
						act.navigate();
					}
				}
				break;
			case 2:
				if (act != null && act.pb != null) {
					act.pb.setVisibility(View.VISIBLE);
				}
				break;
			case 3:
				if (act != null && act.pb != null) {
					act.pb.setVisibility(View.GONE);
				}
				break;
			}
		}

	}

}
