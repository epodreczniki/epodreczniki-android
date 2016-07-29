package pl.epodreczniki.activity;


import pl.epodreczniki.R;
import pl.epodreczniki.service.CheckFailsService;
import pl.epodreczniki.service.CleanupService;
import pl.epodreczniki.service.DevDeleterService;
import pl.epodreczniki.util.ConnectionDetector;
import pl.epodreczniki.util.Util;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

public class SplashActivity extends Activity {		

	private static final int SPLASH_DELAY_MILIS = 1000;		

	private static boolean coldStart = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_splash);		
		if(coldStart){
			if(Util.isDev(this)){
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				final int status = prefs.getInt(DevMainActivity.DEV_PACKAGE_STATUS_KEY, -1);
				if(status==DevMainActivity.STATUS_EXTRACTING){
					final Intent i = new Intent(this,DevDeleterService.class);
					i.putExtra(DevDeleterService.EXTRA_RECOVERY, true);
					startService(i);				
					prefs.edit()
					.putInt(DevMainActivity.DEV_PACKAGE_STATUS_KEY, DevMainActivity.STATUS_REMOTE)
					.putString(DevMainActivity.DEV_PACKAGE_TIMESTAMP_KEY, null)
					.apply();	
				}				
			}else{
				startService(new Intent(this,CleanupService.class));
				startService(new Intent(this,CheckFailsService.class));					
				
			}			
			coldStart = false;		
		}
		if (!ConnectionDetector.isExternalStorageAvailable()) {
			startActivity(new Intent(this, ProblemWithStorageActivity.class));
			finish();
			return;
		}
		
		if(Util.isDev(this)){
			new Handler().postDelayed(new Runnable(){
				@Override
				public void run() {
					final Intent i = new Intent(SplashActivity.this,DevMainActivity.class);
					startActivity(i);
					finish();
				}
			}, SPLASH_DELAY_MILIS);
		}else{
			ConnectionDetector.checkDb(this);		
			final boolean accepted = Util.termsAccepted(this); 		
			if(savedInstanceState==null){
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						if (accepted) {
							Intent i = new Intent(SplashActivity.this,CheckLoginActivity.class);
							i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
							startActivity(i);
							finish();
						} else {
							Intent i = new Intent(SplashActivity.this,
									TermsActivity.class);
							i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
							startActivity(i);
							finish();
						}
					}
				}, SPLASH_DELAY_MILIS);	
			}	
		}							
	}	

}
