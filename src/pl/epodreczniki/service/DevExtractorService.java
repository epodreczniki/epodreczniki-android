package pl.epodreczniki.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import pl.epodreczniki.activity.DevMainActivity;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class DevExtractorService extends IntentService{
	
	public static final String EXTRA_FILE_URI = "DevExtractorService.extra_file_uri";

	private static final int BUFFER_SIZE = 64*1024;
	
	private SharedPreferences prefs;
	
	public DevExtractorService() {
		super("DevExtractorService");		
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String zipUri = intent.getStringExtra(EXTRA_FILE_URI);
		final long transferId = prefs.getLong(DevMainActivity.DEV_TRANSFER_ID_KEY, -1);
		String platformCssLocation = null;
		String platformJsLocation = null;
		if(zipUri!=null){
			final Uri u = Uri.parse(zipUri);
			final File file = new File(u.getPath());
			ZipFile zipFile = null;
			FileOutputStream fos = null;
			InputStream eis = null;
			try {
				zipFile = new ZipFile(file);
				final int totalEntries = zipFile.size();
				prefs.edit().putInt(DevMainActivity.DEV_ENTRIES_TOTAL, totalEntries).apply();
				final File destination = file.getParentFile();
				int entriesProcessed = 0;
				byte[] buffer = new byte[BUFFER_SIZE];
				Enumeration<?> entries = zipFile.entries();
				while (entries.hasMoreElements()) {
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
					++entriesProcessed;
					if(entriesProcessed%20==0){
						prefs.edit().putInt(DevMainActivity.DEV_ENTRIES_PROCESSED, entriesProcessed).apply();
					}
				}				
				if(processCss(platformCssLocation) && processJs(platformJsLocation)){
					prefs.edit().putInt(DevMainActivity.DEV_PACKAGE_STATUS_KEY, DevMainActivity.STATUS_READY).apply();
				}else{
					Log.e("DES","sum ting wong");
					deleteAndSetStatusRemote(file.getParentFile().getName());
				}									
			} catch (IOException e) {
				Log.e("DES","sum ting wong");
				deleteAndSetStatusRemote(file.getParentFile().getName());
			} finally {
				Util.closeZipFileSilently(zipFile);
				Util.closeSilently(fos);
				Util.closeSilently(eis);
				if(transferId!=-1){
					final DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);				
					dm.remove(transferId);	
				}
			}							
		}
	}	
	
	private void deleteAndSetStatusRemote(String timestamp){
		prefs.edit()
		.putInt(DevMainActivity.DEV_PACKAGE_STATUS_KEY, DevMainActivity.STATUS_REMOTE)
		.putLong(DevMainActivity.DEV_TRANSFER_ID_KEY, -1)
		.apply();
		final Intent i = new Intent(this,DevDeleterService.class);
		i.putExtra(DevDeleterService.EXTRA_TIMESTAMP, timestamp);
		startService(i);
	}
	
	private boolean processCss(String platformCssAbs){
		boolean res = true;
		if(platformCssAbs!=null){
			final String newLocation = platformCssAbs.replace(Constants.PLATFORM_CSS, Constants.GENERAL_CSS);			
			final File old = new File(platformCssAbs);
			res = old.renameTo(new File(newLocation));
			Log.e("DES","css rename result "+res);
		}
		return res;
	}	
	
	private boolean processJs(String platformJsAbs){
		Log.e("DES","Platform js abs: "+platformJsAbs);
		boolean res = true;
		if(!TextUtils.isEmpty(platformJsAbs)){
			final String newLocation = platformJsAbs.replace(Constants.PLATFORM_JS,Constants.JS_TARGET);
			Log.e("DES","renaming to: "+newLocation);
			final File old = new File(platformJsAbs);
			res = old.renameTo(new File(newLocation));
			Log.e("DES","js rename result: "+res);
		}		
		return res;
	}

}
