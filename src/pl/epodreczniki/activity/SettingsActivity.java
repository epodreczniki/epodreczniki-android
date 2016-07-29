package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.LoginListFragment;
import pl.epodreczniki.fragment.ModifyPrivilegesDialog;
import pl.epodreczniki.fragment.SettingsFragment;
import pl.epodreczniki.fragment.UserDialog;
import pl.epodreczniki.fragment.LoginListFragment.LoginListCallback;
import pl.epodreczniki.fragment.UserDialog.UserDialogCallback;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.UserContext;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.view.MenuItem;
import android.widget.ListView;

public class SettingsActivity extends AbstractUserAwareActivity implements LoginListCallback, UserDialogCallback{

	private FragmentTabHost mTabHost;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_settings);
		final FragmentManager fm = getFragmentManager();
		mTabHost = (FragmentTabHost) findViewById(R.id.as_tab_host);
		mTabHost.setup(this, fm, R.id.as_tab_content);
		mTabHost.addTab(mTabHost.newTabSpec(SettingsFragment.TAG).setIndicator("Ogólne", null),SettingsFragment.class,null);
		final Bundle listBundle = new Bundle();
		listBundle.putInt(LoginListFragment.ARGS_LIST_MODE, ListView.CHOICE_MODE_NONE);
		listBundle.putBoolean(LoginListFragment.ARGS_ADMIN, true);
		mTabHost.addTab(mTabHost.newTabSpec(LoginListFragment.TAG).setIndicator("Użytkownicy",null),LoginListFragment.class,listBundle);		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayUseLogoEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		}
		return (super.onOptionsItemSelected(item));
	}

	@Override
	public void showLoginFrom(User user) {
		showPrivilegesDialog(user);
	}
	
	@Override
	public void showUserDialog() {
		if(checkUserLoggedIn()){
			if(UserContext.getCurrentUser().isInitialAccount()){
				showCreateAdminDialog();
			}else{			
				showCreateUserDialog();
			}	
		}		
	}
	
	@Override
	public void notifyAdminChanged() {
		LoginListFragment f = (LoginListFragment) getFragmentManager().findFragmentByTag(LoginListFragment.TAG);
		if(f!=null){
			f.updateUI();
		}
	}
	
	private void showCreateAdminDialog(){
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(UserDialog.TAG);
		if(prev!=null){
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final UserDialog dial = UserDialog.getCreateAdminDialog();		
		dial.show(getFragmentManager(), UserDialog.TAG);
	}
	
	private void showCreateUserDialog(){
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(UserDialog.TAG);
		if(prev!=null){
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final UserDialog dial = UserDialog.getCreateUserDialog();
		dial.show(getFragmentManager(), UserDialog.TAG);
	}
	
	private void showPrivilegesDialog(User user){
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(ModifyPrivilegesDialog.TAG);
		if(prev!=null){
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final ModifyPrivilegesDialog dial = ModifyPrivilegesDialog.getInstance(user);
		dial.show(getFragmentManager(), ModifyPrivilegesDialog.TAG);
	}
	
}