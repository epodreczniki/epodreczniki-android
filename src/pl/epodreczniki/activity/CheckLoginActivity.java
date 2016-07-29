package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.CheckLoginFragment;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class CheckLoginActivity extends Activity implements CheckLoginFragment.CheckLoginCallback{
	
	private CheckLoginFragment checkLoginFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_check_login);
		final FragmentManager fm = getFragmentManager();
		checkLoginFragment = (CheckLoginFragment) fm.findFragmentByTag(CheckLoginFragment.TAG);
		if(checkLoginFragment==null){
			checkLoginFragment = new CheckLoginFragment();
			fm.beginTransaction().add(checkLoginFragment, CheckLoginFragment.TAG).commit();
		}
	}

	@Override
	public void onCheckResult(boolean showLoginForm) {	
		Log.e("CLA","result: "+showLoginForm);
		if(showLoginForm){
			Intent i = new Intent(this,LoginListActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(i);
			finish();
		}else{
			if(Util.isTablet(this)){
				Intent i = new Intent(this,BookGridActivity.class);
				startActivity(i);
				finish();
			}else{
				int preferredListType = Settings.LIST_TYPE_FLAT;
				if(UserContext.getCurrentUser()!=null){
					preferredListType = UserContext.getCurrentUser().getSettings().getPreferredListType();	
				}
				Intent i = new Intent(this,preferredListType==Settings.LIST_TYPE_FLAT?BookListActivity.class:BookPagerActivity.class);
				startActivity(i);
				finish();
			}			
		}
	}

}
