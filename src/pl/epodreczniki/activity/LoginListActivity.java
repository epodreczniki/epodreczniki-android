package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.LoginFormFragment;
import pl.epodreczniki.fragment.LoginListFragment;
import pl.epodreczniki.fragment.UserDialog;
import pl.epodreczniki.fragment.LoginFormFragment.LoginFormCallback;
import pl.epodreczniki.fragment.LoginListFragment.LoginListCallback;
import pl.epodreczniki.fragment.UserDialog.UserDialogCallback;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.CheckCredentialsTask;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.CheckCredentialsTask.CheckCredentialsCallback;
import pl.epodreczniki.util.Util;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;

public class LoginListActivity extends Activity implements LoginListCallback,LoginFormCallback,CheckCredentialsCallback,UserDialogCallback {	
	
	private boolean mTwoPane;
	
	private LoginListFragment listFragment;
	
	private LoginFormFragment formFragment;
	
	private View overlay;
	
	private CheckCredentialsTask task;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_login_list);
		mTwoPane = checkTwoPane();
		final FragmentManager fm = getFragmentManager();
		if(savedInstanceState==null){
			listFragment = LoginListFragment.newInstance(mTwoPane);
			fm.beginTransaction().add(R.id.all_list,listFragment).commit();
		}else{
			listFragment = (LoginListFragment) fm.findFragmentById(R.id.all_list);
			formFragment = (LoginFormFragment) fm.findFragmentById(R.id.all_form);
		}
		overlay = findViewById(R.id.all_overlay);
	}
	
	@Override
	protected void onPause() {	
		super.onPause();
		if(task!=null){
			task.cancel(true);
			task = null;
		}
	}

	private boolean checkTwoPane(){
		final View form = findViewById(R.id.all_form);
		return form!=null;
	}
	
	@Override
	public void showLoginFrom(User user) {
		if(mTwoPane){
			formFragment = LoginFormFragment.newInstance(user);
			getFragmentManager().beginTransaction().replace(R.id.all_form, formFragment).commit();
		}else{
			final Intent i = new Intent(this,LoginFormActivity.class);
			i.putExtra(LoginFormActivity.EXTRA_USER, user);
			startActivity(i);
		}
	}
	
	@Override
	public void showUserDialog() {
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

	@Override
	public void doLogin(User user,String enteredPassword,boolean skipPassword) {
		overlay.setVisibility(View.VISIBLE);
		if(skipPassword){
			handleLoginResult(user);
		}else{
			task = new CheckCredentialsTask(this, user, enteredPassword);
			task.execute();	
		}		
	}
	
	@Override
	public void handleLoginResult(User loggedInUser) {
		overlay.setVisibility(View.GONE);
		if(loggedInUser!=null){
			UserContext.login(loggedInUser,this);
			Intent i;
			if(Util.isTablet(this)){
				i = new Intent(this,BookGridActivity.class);				
			}else{
				final int preferredListType = loggedInUser.getSettings().getPreferredListType();
				if(preferredListType==Settings.LIST_TYPE_FLAT){
					i = new Intent(this,BookListActivity.class);					
				}else{				
					i = new Intent(this,BookPagerActivity.class);
				}				
			}
			startActivity(i);
			finish();
		}else{
			showLoginFailedDialog();
		}
	}
	
	@Override
	public void handleLoginCancel() {
		overlay.setVisibility(View.GONE);
	}
	
	@Override
	public void notifyAdminChanged() {
	}
	
	private void showLoginFailedDialog(){		
		new AlertDialog.Builder(this).setTitle("Błąd logowania")
			.setMessage("Podane hasło jest nieprawidłowe.")
			.setNeutralButton("Ok", null)
			.create()
			.show();		
	}
	
}
