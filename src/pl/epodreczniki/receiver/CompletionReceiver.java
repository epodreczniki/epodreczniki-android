package pl.epodreczniki.receiver;

import pl.epodreczniki.service.CompletionService;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CompletionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
		if(completedId != -1){
			Intent i = new Intent(context,CompletionService.class);
			i.putExtra(CompletionService.EXTRA_DOWNLOAD_ID, completedId);
			context.startService(i);
		}
	}
	
}
