package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.LoginFormFragment;
import pl.epodreczniki.fragment.LoginFormFragment.LoginFormCallback;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.CheckCredentialsTask;
import pl.epodreczniki.util.CheckCredentialsTask.CheckCredentialsCallback;
import pl.epodreczniki.util.UserContext;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class LoginFormActivity extends Activity implements LoginFormCallback,CheckCredentialsCallback{	

	public static final String EXTRA_USER = "lfa_extra_user";
	
	private User user;
	
	private LoginFormFragment formFragment;
	
	private View overlay;
	
	private CheckCredentialsTask task;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_login_form);
		if(getIntent().getExtras()==null){
			finish();
			return;
		}
		user = getIntent().getExtras().getParcelable(EXTRA_USER);
		if(user==null){
			finish();
			return;
		}
		if(savedInstanceState==null){
			formFragment = LoginFormFragment.newInstance(user);
			getFragmentManager().beginTransaction().add(R.id.alf_form, formFragment).commit();
		}else{
			formFragment = (LoginFormFragment) getFragmentManager().findFragmentById(R.id.alf_form);
		}
		overlay = findViewById(R.id.alf_overlay);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(task!=null){
			task.cancel(true);
			task = null;
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent i = new Intent(this,LoginListActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(i);
		finish();
	}
	
	@Override
	public void doLogin(User user, String enteredPassword,boolean skipPassword) {
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
			if(loggedInUser.getSettings().getPreferredListType()==Settings.LIST_TYPE_FLAT){
				i = new Intent(this,BookListActivity.class);				
			}else{
				i = new Intent(this,BookPagerActivity.class);
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
	
	private void showLoginFailedDialog(){		
		new AlertDialog.Builder(this).setTitle("Błąd logowania")
			.setMessage("Podane hasło jest nieprawidłowe.")
			.setNeutralButton("Ok", null)			
			.create()
			.show();		
	}
	
}
