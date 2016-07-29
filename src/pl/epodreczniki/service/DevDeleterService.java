package pl.epodreczniki.service;

import java.io.File;

import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class DevDeleterService extends IntentService{
	
	public static final String EXTRA_TIMESTAMP = "pl.epodreczniki.service.DeleterService.extra_dir";
	
	public static final String EXTRA_ZIP_ONLY = "pl.epodreczniki.service.DeleterService.extra_zip_only";
	
	public static final String EXTRA_RECOVERY = "pl.epodreczniki.service.DeleterService.extra_recovery";

	public DevDeleterService() {
		super("DevDeleterService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final boolean recovery = intent.getBooleanExtra(EXTRA_RECOVERY, false);
		if(recovery){
			Log.e("DELETER","performing recovery");
			File devDir = Util.getExternalStorageDevDir(this);
			for(File child : devDir.listFiles()){
				Util.delete(child);
			}
		}else{
			final String timestamp = intent.getStringExtra(EXTRA_TIMESTAMP);
			boolean res = false;
			if(timestamp!=null){
				final File dir = Util.getExternalStorageDevDir(this, timestamp);
				final boolean zipOnly = intent.getBooleanExtra(EXTRA_ZIP_ONLY, false);
				if(zipOnly){
					final File toDelete = new File(dir,Constants.DEV_FILE_NAME);				
					res = toDelete.delete();
				}else{
					res = Util.delete(dir);
				}
				int notesDeleted = getContentResolver().delete(NotesProvider.UriHelper.NOTES_URI, null, null);
				Log.e("DELETER","deleted notes: "+notesDeleted);
			}
			Log.e("DELETER",res?"delete successful":"delete failed");	
		}		
	}

}
