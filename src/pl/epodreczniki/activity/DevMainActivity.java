package pl.epodreczniki.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.DevConfirmDialog;
import pl.epodreczniki.fragment.DevMainWorkerFragment;
import pl.epodreczniki.model.DevPackageInfo;
import pl.epodreczniki.model.DevPackageInfo.DevPackageFormat;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.User;
import pl.epodreczniki.service.DevDeleterService;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

public class DevMainActivity extends Activity implements OnClickListener, OnCheckedChangeListener, DevMainWorkerFragment.DownloadJsonCallbacks, DevMainWorkerFragment.DownloadCssCallbacks{

	public static final String DEV_PACKAGE_STATUS_KEY = "dev.package.status";
	
	public static final String DEV_PACKAGE_PREV_STATUS_KEY = "dev.package.prev.status";

	public static final String DEV_PACKAGE_TIMESTAMP_KEY = "dev.package.timestamp";

	public static final String DEV_TRANSFER_ID_KEY = "dev.transfer.id";
	
	public static final String DEV_ENTRIES_TOTAL = "dev.entries.total";
	
	public static final String DEV_ENTRIES_PROCESSED = "dev.entries.processed";

	public static final String DEV_SHOW_CONFIRM = "dev.show.confirm";
	
	public static final String DEV_CSS_URL_KEY = "dev.css.url";

	public static final int QR_REQUEST = 1886;

	public static final String EXTRA_RESULT_URL = "qr.result.url";

	public static final int STATUS_REMOTE = 0;
	
	public static final int STATUS_JSON_DOWNLOADING = 4;

	public static final int STATUS_DOWNLOADING = 1;
	
	public static final int STATUS_EXTRACTING = 2;

	public static final int STATUS_READY = 3;
	
	public static final int STATUS_CSS_DOWNLOADING = 5;
	
	public static final String LABEL_REMOTE = "Brak treści";
	
	public static final String LABEL_DOWNLOADING = "Pobieranie";
	
	public static final String LABEL_EXTRACTING = "Rozpakowywanie";
	
	public static final String LABEL_READY = "Treść gotowa";	

	private SharedPreferences prefs;
	
	private Handler handler = new StatusHandler(this);

	private Button cut;
	
	private Button scan;

	private Button open;
	
	private Button cancel;
	
	private ProgressBar pb;
	
	private TextView status;
	
	private TextView progress;
	
	private Switch teacherSwitch;
	
	private EditText alternativeCss;
	
	private Button cssSwap;
	
	private Thread updateThread;
	
	private Thread qdmThread;
	
	private DevMainWorkerFragment workerFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_dev_main);
		fakeLogin();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		status = (TextView) findViewById(R.id.adm_status);
		progress = (TextView) findViewById(R.id.adm_progress);
		cut = (Button) findViewById(R.id.adm_btn_cut);
		cut.setOnClickListener(this);
		scan = (Button) findViewById(R.id.adm_btn_scan);
		scan.setOnClickListener(this);
		open = (Button) findViewById(R.id.adm_btn_open);
		open.setOnClickListener(this);
		cancel = (Button) findViewById(R.id.adm_btn_cancel);
		cancel.setOnClickListener(this);
		pb = (ProgressBar) findViewById(R.id.adm_pb);
		teacherSwitch = (Switch) findViewById(R.id.adm_sw_teacher);
		teacherSwitch.setOnCheckedChangeListener(this);
		alternativeCss = (EditText) findViewById(R.id.adm_et_css);
		final String altCssUrl = prefs.getString(DEV_CSS_URL_KEY, null);
		if(altCssUrl!=null){
			alternativeCss.setText(altCssUrl);
		}
		alternativeCss.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				prefs.edit().putString(DEV_CSS_URL_KEY, s.toString()).apply();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {		
			}
		});
		cssSwap = (Button) findViewById(R.id.adm_btn_css_swap);
		cssSwap.setOnClickListener(this);
		final FragmentManager fm = getFragmentManager();
		workerFragment = (DevMainWorkerFragment) fm.findFragmentByTag(DevMainWorkerFragment.TAG);
		if(workerFragment == null){
			workerFragment = new DevMainWorkerFragment();
			fm.beginTransaction().add(workerFragment, DevMainWorkerFragment.TAG).commit();
		}
		refreshUI();
	}
	
	@Override
	protected void onResume() {	
		super.onResume();
		if(updateThread == null){
			updateThread = new Thread(new StatusChecker(this));
			updateThread.start();
		}
		if(qdmThread==null){
			qdmThread = new Thread(new QueryDownloadManager(this));
			qdmThread.start();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(updateThread != null){
			updateThread.interrupt();
			updateThread = null;
		}
		if(qdmThread!=null){
			qdmThread.interrupt();
			qdmThread = null;
		}
	}

	@Override
	public void onClick(View v) {
		if (v == scan) {			
			startActivityForResult(new Intent(DevMainActivity.this,
					DevScanActivity.class), QR_REQUEST);
		} else if (v == open) {			
			final Intent i = new Intent(this, SwipeReaderActivity.class);			
			i.putExtra(SwipeReaderActivity.EXTRA_DEV_TIMESTAMP, prefs.getString(DEV_PACKAGE_TIMESTAMP_KEY, null));
			startActivity(i);
		} else if (v == cancel){
			cancelDownload();
		} else if (v == cssSwap){
			prefs.edit().putInt(DEV_PACKAGE_STATUS_KEY, STATUS_CSS_DOWNLOADING).apply();
			workerFragment.requestCss(alternativeCss.getText().toString(),prefs.getString(DEV_PACKAGE_TIMESTAMP_KEY, null));
		} else if (v == cut){
			workerFragment.cancelCss();
		}
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		prefs.edit().putBoolean(Constants.PREF_IS_TEACHER, isChecked).apply();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == QR_REQUEST) {
			if (resultCode == RESULT_OK) {				
				String jsonUrl = data.getStringExtra(EXTRA_RESULT_URL);
				if(jsonUrl.startsWith("//")){
					jsonUrl = "http:"+jsonUrl;
				}
				int currentStatus = prefs.getInt(DEV_PACKAGE_STATUS_KEY, STATUS_REMOTE);
				prefs.edit().putInt(DEV_PACKAGE_PREV_STATUS_KEY, currentStatus).putInt(DEV_PACKAGE_STATUS_KEY, STATUS_JSON_DOWNLOADING).apply();
				Log.e("DMA", "set status to json downloading");
				workerFragment.requestJson(jsonUrl);				
			}
		}
	}
	
	private void fakeLogin(){
		User.Builder b = new User.Builder();
		b.withAccountType(User.ACCOUNT_TYPE_INITIAL)
		.withLocalUserId(0L)
		.withUserName("Testowy")
		.withSettings(Settings.fromJsonString(Settings.getInitialSettings()));
		User u = b.build();
		UserContext.login(u, this);
	}		
	
	private void cancelDownload(){
		long id = prefs.getLong(DEV_TRANSFER_ID_KEY, -1);
		if(id!=-1){
			DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			dm.remove(id);
			prefs.edit().putInt(DEV_PACKAGE_STATUS_KEY, STATUS_REMOTE).apply();
		}
	}

	private void showConfirmDialog(String url) {
		final FragmentManager fm = getFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
		final Fragment prev = fm.findFragmentByTag(DevConfirmDialog.TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final DevConfirmDialog dialog = DevConfirmDialog.newInstance(url);
		
		dialog.show(fm, DevConfirmDialog.TAG);
	}

	public void requestTransfer(String url) {	
		Log.e("DEV","requesting transfer with url: "+url);
		if(url!=null){			
			url = url.trim();
			if(url.startsWith("http")||url.startsWith("https")){
				final String prevTimestamp = prefs.getString(DEV_PACKAGE_TIMESTAMP_KEY, null);
				deletePrevious(prevTimestamp);
				final DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
				final Uri uri = Uri.parse(url);		
				final String timestamp = String.valueOf(System.currentTimeMillis());
				final File dir = Util.getExternalStorageDevDir(this, timestamp);
				final String filePath = timestamp+File.separator+Constants.DEV_FILE_NAME;
				dir.mkdirs();
				final DownloadManager.Request req = new DownloadManager.Request(uri);
				req.setDestinationInExternalFilesDir(this, Constants.DEV_DIR, filePath);
				req.setTitle(getString(R.string.th_downloading_dev));
				long id = dm.enqueue(req);
				transferEnqueued(timestamp, id);
				refreshUI();	
			}else{
				Toast.makeText(this, "Nieprawidłowy adres", Toast.LENGTH_SHORT).show();
				restorePreviousStatus();
			}
		}else{
			Toast.makeText(this, "Nieprawidłowy adres", Toast.LENGTH_SHORT).show();
			restorePreviousStatus();
		}	
	}
	
	public void restorePreviousStatus(){
		Log.e("DMA","restoring previous status");
		int previouStatus = prefs.getInt(DEV_PACKAGE_PREV_STATUS_KEY, STATUS_REMOTE);		
		Log.e("DMA","previous status: "+previouStatus);
		prefs.edit().putInt(DEV_PACKAGE_STATUS_KEY, previouStatus).putInt(DEV_PACKAGE_PREV_STATUS_KEY, STATUS_REMOTE).apply();
	}	
	
	@Override
	public void processResult(String result) {
		Log.e("DMA","processing result");
		if(result == null){
			Toast.makeText(this, "Pobieranie zakończone niepowodzeniem", Toast.LENGTH_SHORT).show();
			restorePreviousStatus();
			return;
		}
		boolean wasUrl = false;
		try {
			URL u = new URL(result);			
			if(u.getProtocol().startsWith("http")){
				wasUrl = true;
				Log.e("DMA",result);
				showConfirmDialog(result);	
			}else{
				restorePreviousStatus();
			}			
		} catch (MalformedURLException e) {
			restorePreviousStatus();
		}
		if(!wasUrl){
			Log.e("DMA","wasJson");
			final Gson gson = new Gson();
			try{
				final DevPackageInfo dpi = gson.fromJson(result, DevPackageInfo.class);
				if(dpi!=null){
					final int requiredVersion = dpi.getAppVersion();
					if(requiredVersion>Util.getAppVersion(this)){
						Toast.makeText(this, "Treść wymaga nowszej wersji aplikacji.", Toast.LENGTH_LONG).show();
						restorePreviousStatus();
					}else{
						if(!dpi.getState().isReady()){
							Toast.makeText(this, "Treść nie jest gotowa. Spróbuj później.", Toast.LENGTH_LONG).show();
							restorePreviousStatus();
						}else{
							final Map<Integer,String> fmts = new TreeMap<Integer,String>();
							final Pattern pattern = Pattern.compile(Constants.RE_RES_INT);
							int maxRes = -1;
							for(DevPackageFormat format : dpi.getFormats()){
								final String formatStr = format.getFormat();
								if(!TextUtils.isEmpty(formatStr)){
									if(formatStr.matches(Constants.RE_ZIP)){								
										Matcher m = pattern.matcher(formatStr);
										if(m.find()){
											final int resolution = Integer.parseInt(m.group());
											final StringBuilder urlBuilder = new StringBuilder(format.getUrl());
											if(urlBuilder.toString().startsWith("//")){
												urlBuilder.insert(0,"http:");
											}
											if(resolution>maxRes){
												maxRes = resolution;
											}
											fmts.put(resolution, urlBuilder.toString());
										}
									}
								}
							}
							String chosenUrl = null;
							final int longerEdge = Util.getLongerEdge(this);
							for(Map.Entry<Integer, String> entry : fmts.entrySet()){
								if(longerEdge<=entry.getKey()){
									chosenUrl = entry.getValue();
									break;
								}
							}
							if(chosenUrl!=null){				
								Log.e("DMA","showConfirmDialog: "+chosenUrl);
								showConfirmDialog(chosenUrl);
							}else{
								Log.e("DMA","showConfirmDialog: "+fmts.get(maxRes));
								showConfirmDialog(fmts.get(maxRes));
							}	
						}					
					}	
				}else{
					Toast.makeText(this, "Nieprawidłowy format metadanych.", Toast.LENGTH_LONG).show();
					restorePreviousStatus();
				}					
			}catch(JsonSyntaxException e){
				Toast.makeText(this, "Nieprawidłowy format metadanych.", Toast.LENGTH_LONG).show();
				restorePreviousStatus();
			}			
		}		
	}
	
	private void transferEnqueued(String timestamp, long id){
		Log.e("DEV", "transfer enqueued: "+id);
		prefs.edit()
		.putInt(DEV_PACKAGE_STATUS_KEY, STATUS_DOWNLOADING)
		.putLong(DEV_TRANSFER_ID_KEY, id)
		.putString(DEV_PACKAGE_TIMESTAMP_KEY, timestamp).apply();		
	}
	
	private void deletePrevious(String timestamp){
		final Intent i = new Intent(this,DevDeleterService.class);
		i.putExtra(DevDeleterService.EXTRA_TIMESTAMP, timestamp);
		startService(i);
	}

	private void refreshUI() {
		final int s = prefs.getInt(DEV_PACKAGE_STATUS_KEY, 0);
		final int entriesTotal = prefs.getInt(DEV_ENTRIES_TOTAL, 0);
		final int entriesProcessed = prefs.getInt(DEV_ENTRIES_PROCESSED, 0);
		switch (s) {
		case STATUS_REMOTE:
			cut.setVisibility(View.GONE);
			open.setEnabled(false);
			scan.setEnabled(true);
			cancel.setEnabled(false);
			pb.setVisibility(View.GONE);
			progress.setVisibility(View.INVISIBLE);
			status.setText(LABEL_REMOTE);
			alternativeCss.setEnabled(false);
			cssSwap.setEnabled(false);
			break;
		case STATUS_DOWNLOADING:
			cut.setVisibility(View.GONE);
			open.setEnabled(false);
			scan.setEnabled(false);
			cancel.setEnabled(true);
			pb.setVisibility(View.VISIBLE);
			progress.setVisibility(View.INVISIBLE);
			status.setText(LABEL_DOWNLOADING);
			alternativeCss.setEnabled(false);
			cssSwap.setEnabled(false);
			break;
		case STATUS_EXTRACTING:
			cut.setVisibility(View.GONE);
			open.setEnabled(false);
			scan.setEnabled(false);
			cancel.setEnabled(false);
			pb.setVisibility(View.VISIBLE);
			progress.setVisibility(View.VISIBLE);
			progress.setText(entriesProcessed+"/"+entriesTotal);
			status.setText(LABEL_EXTRACTING);
			alternativeCss.setEnabled(false);
			cssSwap.setEnabled(false);
			break;
		case STATUS_READY:
			cut.setVisibility(View.GONE);
			open.setEnabled(true);
			scan.setEnabled(true);
			cancel.setEnabled(false);
			pb.setVisibility(View.GONE);
			progress.setVisibility(View.INVISIBLE);
			status.setText(LABEL_READY);
			alternativeCss.setEnabled(true);
			cssSwap.setEnabled(true);
			break;
		case STATUS_JSON_DOWNLOADING:
			cut.setVisibility(View.GONE);
			open.setEnabled(false);
			scan.setEnabled(false);
			cancel.setEnabled(false);
			pb.setVisibility(View.VISIBLE);
			progress.setVisibility(View.INVISIBLE);
			status.setText("Pobieranie metadanych");
			alternativeCss.setEnabled(false);
			cssSwap.setEnabled(false);
			break;
		case STATUS_CSS_DOWNLOADING:
			cut.setVisibility(View.VISIBLE);
			open.setEnabled(false);
			scan.setEnabled(false);
			cancel.setEnabled(false);
			pb.setVisibility(View.VISIBLE);
			progress.setVisibility(View.INVISIBLE);
			status.setText("Pobieranie css");
			alternativeCss.setEnabled(false);
			cssSwap.setEnabled(false);
			break;
		}
	}
	
	static class StatusChecker implements Runnable{

		private final WeakReference<DevMainActivity> ctxRef;
		
		StatusChecker(DevMainActivity ctx){
			ctxRef = new WeakReference<DevMainActivity>(ctx);
		}
		
		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()){
				final DevMainActivity act = ctxRef.get();
				if(act!=null){
					act.handler.sendEmptyMessage(0);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}		
	}
	
	static class QueryDownloadManager implements Runnable{

		private final DevMainActivity ctx;
		
		QueryDownloadManager(DevMainActivity ctx){
			this.ctx = ctx;
		}		
		
		@Override
		public void run() {			
			while(!Thread.currentThread().isInterrupted()){
				int status = ctx.prefs.getInt(DEV_PACKAGE_STATUS_KEY, -1);
				if(status==STATUS_DOWNLOADING){
					long transferId = ctx.prefs.getLong(DEV_TRANSFER_ID_KEY, -1);
					if(transferId!=-1){
						doQuery(transferId);
					}	
				}				
				try{
					Thread.sleep(3000);
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
				}
			}
		}
		
		private void doQuery(long transferId){
			DownloadManager dm = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);
			DownloadManager.Query q = new DownloadManager.Query();
			q.setFilterById(transferId);
			Cursor dmc = dm.query(q);
			if(dmc!=null){
				final int statusIdx = dmc
						.getColumnIndex(DownloadManager.COLUMN_STATUS);						
				final int reasonIdx = dmc.getColumnIndex(DownloadManager.COLUMN_REASON);
				if(dmc.moveToFirst()){
					int status = dmc.getInt(statusIdx);
					String reason = dmc.getString(reasonIdx);
					Log.e("QDM","Current status is: "+status);
					Log.e("QDM","Reason is: "+reason);
				}
				dmc.close();
			}
		}
		
	}
	
	static class StatusHandler extends Handler{
		
		private final WeakReference<DevMainActivity> ctxRef;
		
		StatusHandler(DevMainActivity ctx){
			ctxRef = new WeakReference<DevMainActivity>(ctx);
		}
		
		@Override
		public void handleMessage(Message msg) {		
			super.handleMessage(msg);
			final DevMainActivity act = ctxRef.get();
			if(act!=null){
				act.refreshUI();
			}
		}
		
	}

	@Override
	public void processCssResult(Integer result) {
		if(result!=0){
			Toast.makeText(this,"Nie udało się podmienić CSS",Toast.LENGTH_SHORT).show();
		}else{
			Toast.makeText(this,"CSS podmieniono pomyślnie",Toast.LENGTH_SHORT).show();
		}
		prefs.edit().putInt(DEV_PACKAGE_STATUS_KEY, STATUS_READY).apply();
	}

	@Override
	public void onCssCancel() {
		prefs.edit().putInt(DEV_PACKAGE_STATUS_KEY, STATUS_READY).apply();
	}
	
}
