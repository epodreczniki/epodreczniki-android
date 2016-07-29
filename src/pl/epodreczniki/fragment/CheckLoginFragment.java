package pl.epodreczniki.fragment;

import java.util.ArrayList;
import java.util.List;

import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CheckLoginFragment extends Fragment{
	
	public static final String TAG = "check.login.fragment";
	
	private CheckLoginTask task;
	
	private CheckLoginCallback callback;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return null;
	}
	
	@Override
	public void onAttach(Activity activity) {	
		super.onAttach(activity);
		callback = (CheckLoginCallback)activity;
		task = new CheckLoginTask();
		task.execute();
	}
	
	@Override
	public void onDetach() {
		task.cancel(true);
		task = null;
		callback = null;
		super.onDetach();
	}
	
	public interface CheckLoginCallback{
		void onCheckResult(boolean showLoginForm);
	}
	
	class CheckLoginTask extends AsyncTask<Void, Void, Boolean>{				

		private User mUser;
		
		@Override
		protected Boolean doInBackground(Void... params) {
			boolean showLogin = false;
			final Activity ctx = getActivity();
			final List<User> users = new ArrayList<User>();
			if(ctx!=null){
				final Cursor c = ctx.getContentResolver().query(UsersProvider.UriHelper.USERS_URI, UsersTable.COLUMNS, null, null, null);
				if(c!=null){
					while(c.moveToNext()){
						final User.Builder b = Util.getUserBuilderFromCursor(c, c.getPosition());
						if(b!=null){
							users.add(b.build());
						}
					}
					c.close();
				}
				if(users.size()==1){
					mUser = users.get(0);
					if(mUser.getAccountType()==User.ACCOUNT_TYPE_INITIAL || !mUser.getSettings().isPasswordRequired()){						
						showLogin = false;
					}else{
						showLogin = true;
					}
				}else{
					showLogin = true;
				}
			}			
			
			return showLogin;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {		
			super.onPostExecute(result);
			if(!result){
				final Activity ctx = getActivity();
				if(ctx!=null){
					UserContext.login(mUser,ctx);	
				}				
			}
			if(callback!=null){
				callback.onCheckResult(result);
			}
		}
		
	}

}
