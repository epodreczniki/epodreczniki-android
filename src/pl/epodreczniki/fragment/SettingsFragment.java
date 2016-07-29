package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.UserContext;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragment {
    
	public static final String TAG = "settings.fragment.tag";
	
	private PreferenceScreen mPrefScreen;
	
	private PreferenceCategory mLoginCategory;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                
        addPreferencesFromResource(R.xml.preferences);
        mPrefScreen = (PreferenceScreen) findPreference("pref.screen");
        mLoginCategory = (PreferenceCategory) findPreference("epodreczniki.preferences.login");
    }
	
	@Override
	public void onResume() {
		super.onResume();	
		User currentUser = UserContext.getCurrentUser();
		if(currentUser==null || !currentUser.isAdmin()){						
			mPrefScreen.removePreference(mLoginCategory);
		}else{			
			if(mPrefScreen.findPreference("epodreczniki.preferences.login")==null){
				mPrefScreen.addPreference(mLoginCategory);
			}
		}
		
	}
	
}