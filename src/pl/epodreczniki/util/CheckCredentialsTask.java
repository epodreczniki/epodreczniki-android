package pl.epodreczniki.util;

import pl.epodreczniki.model.User;
import android.os.AsyncTask;

public class CheckCredentialsTask extends AsyncTask<Void,Void,User>{

	public interface CheckCredentialsCallback{
		
		void handleLoginResult(User loggedInUser);
		
		void handleLoginCancel();
		
	}
	
	private CheckCredentialsCallback callback;
	
	private User user;
	
	private String enteredPassword;
	
	public CheckCredentialsTask(CheckCredentialsCallback callback, User user, String enteredPassword){
		this.callback = callback;
		this.user = user;
		this.enteredPassword = enteredPassword;
	}

	@Override
	protected User doInBackground(Void... params) {
		return CryptUtil.verifyPassword(enteredPassword, user)?user:null;
	}
	
	@Override
	protected void onPostExecute(User result) {	
		super.onPostExecute(result);
		callback.handleLoginResult(result);
		callback = null;
	}
	
	@Override
	protected void onCancelled(User result) {	
		super.onCancelled(result);
		callback.handleLoginCancel();
		callback = null;
	}	
	
}
