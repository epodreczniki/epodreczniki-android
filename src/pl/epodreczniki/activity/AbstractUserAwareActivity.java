package pl.epodreczniki.activity;

import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.content.Intent;

public abstract class AbstractUserAwareActivity extends Activity{
	
	@Override
	protected void onResume() {	
		super.onResume();
		if(!checkUserLoggedIn()){
			return;
		}
	}
	
	protected boolean checkUserLoggedIn(){
		if(!UserContext.isLoggedIn()){			
			Intent i;
			if(Util.isDev(this)){
				i = new Intent(this,DevMainActivity.class);
			}else{
				i = new Intent(this,CheckLoginActivity.class);	
			}						
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			finish();
			return false;
		}
		return true;
	}

}
