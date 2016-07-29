package pl.epodreczniki.fragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.epodreczniki.R;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.CryptUtil;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UserDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "user.dialog";
	
	private static final String ARGS_USER = "user_dialog_args_user";
	
	private static final String ARGS_ACTION = "user_dialog_args_action";
	
	private static final int ACTION_CREATE_ADMIN = 0;
	
	private static final int ACTION_CREATE_USER = 1;
	
	private static final int ACTION_EDIT_USER = 2;	
	
	public interface UserDialogCallback{
		
		void notifyAdminChanged();
		
	}
	
	public static UserDialog getCreateAdminDialog(){
		final UserDialog res = new UserDialog();
		final Bundle b = new Bundle();
		b.putInt(ARGS_ACTION, ACTION_CREATE_ADMIN);
		res.setArguments(b);
		return res;
	}
	
	public static UserDialog getCreateUserDialog(){
		final UserDialog res = new UserDialog();
		final Bundle b = new Bundle();
		b.putInt(ARGS_ACTION, ACTION_CREATE_USER);
		res.setArguments(b);
		return res;
	}
	
	public static UserDialog getEditUserDialog(User user){
		final UserDialog res = new UserDialog();
		final Bundle b = new Bundle();
		b.putInt(ARGS_ACTION, ACTION_EDIT_USER);
		b.putParcelable(ARGS_USER, user);
		res.setArguments(b);
		return res;
	}
	
	public UserDialog(){	
	}
	
	private EditText userName;
	
	private EditText password;
	
	private EditText confirmPassword;
	
	private Button cancel;
	
	private Button ok;
	
	private View overlay;
	
	private int mAction;
	
	private User mUser;
	
	private ProcessDataTask mTask;
	
	private UserDialogCallback callback;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		mAction = getArguments().getInt(ARGS_ACTION);
		mUser = getArguments().getParcelable(ARGS_USER);
		Log.e("USERDIAL",getActivity().getClass().getName());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {		
		final View res = inflater.inflate(R.layout.f_user_dialog, container, false);
		userName = (EditText) res.findViewById(R.id.fuad_user_name);
		password = (EditText) res.findViewById(R.id.fuad_password);
		confirmPassword = (EditText) res.findViewById(R.id.fuad_password_confirm);
		ok = (Button) res.findViewById(R.id.fuad_ok);
		ok.setOnClickListener(this);
		cancel = (Button) res.findViewById(R.id.fuad_cancel);
		cancel.setOnClickListener(this);
		overlay = res.findViewById(R.id.fuad_overlay);
		switch(mAction){
			case ACTION_CREATE_ADMIN:
				getDialog().setTitle("Dodawanie administratora");
				break;
			case ACTION_CREATE_USER:
				getDialog().setTitle("Dodawanie użytkownika");
				break;
			case ACTION_EDIT_USER:
				getDialog().setTitle("Edycja danych użytkownika");
				break;
		}
		return res;
	}
	
	@Override
	public void onAttach(Activity activity) {	
		super.onAttach(activity);
		Log.e("UD_AAAA",this.toString());
		callback = (UserDialogCallback) activity;
	}
	
	@Override
	public void onDetach() {	
		super.onDetach();
		callback = null;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.fuad_cancel:
			dismiss();
			break;
		case R.id.fuad_ok:
			if(mTask!=null){
				mTask.cancel(true);
			}
			mTask = new ProcessDataTask(this);
			mTask.execute();			
			break;
		}
	}		
	
	@Override
	public void onDismiss(DialogInterface dialog) {		
		super.onDismiss(dialog);
		if(mTask!=null){
			mTask.cancel(true);
			mTask = null;
		}
		final FragmentManager fm = getFragmentManager();
		if(fm!=null){
			fm.popBackStack();	
		}		
	}	
	
	private static class ProcessDataTask extends AsyncTask<Void,Void,ProcessDataResult>{		
		
		private UserDialog dialog;
		
		ProcessDataTask(UserDialog dialog){
			this.dialog = dialog;
		}
		
		@Override
		protected void onPreExecute() {		
			super.onPreExecute();
			dialog.overlay.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected ProcessDataResult doInBackground(Void... params) {
			final Context ctx = dialog.getActivity();
			final ProcessDataResult res = new ProcessDataResult();
			if(ctx!=null){						
				final Pattern userNamePattern = Pattern.compile(Constants.RE_USER_NAME);
				final Matcher userNameMatcher = userNamePattern.matcher(dialog.userName.getText().toString());
				if(!userNameMatcher.matches()){				
					res.setErrorUserName("Nazwa użytkownika powinna mieć od 4 do 10 znaków i zawierać tylko litery, cyfry oraz kropki.");
					return res;
				}
				final Cursor c = ctx.getContentResolver().query(UsersProvider.UriHelper.userByUserName(dialog.userName.getText().toString()), UsersTable.COLUMNS, null, null, null);
				boolean userNameTaken = false;
				if(c!=null){
					if(c.getCount()>0){
						userNameTaken = true;
					}
					c.close();
				}
				if(userNameTaken){
					res.setErrorUserName("Nazwa użytkownika jest zajęta.");
					return res;
				}
				int passLength = dialog.password.getText().toString().trim().length();
				if(passLength<Constants.MIN_PASSWORD_LENGTH){
					res.setErrorPassword("Hasło powinno mieć co najmniej 6 znaków.");
					return res;
				}
				if(!dialog.password.getText().toString().equals(dialog.confirmPassword.getText().toString())){
					res.setErrorConfirmPassword("Wpisane hasła różnią się.");
					return res;
				}
				switch(dialog.mAction){
				case UserDialog.ACTION_CREATE_ADMIN:				
					if(UserContext.getCurrentUser()==null||UserContext.getCurrentUser().getLocalUserId()!=0L 
					|| UserContext.getCurrentUser().getAccountType()!=User.ACCOUNT_TYPE_INITIAL){
						res.setErrorMessage("Brak uprawnień.");
						return res;
					}
					String salt64adm = CryptUtil.generateSalt64();
					String pass64adm = CryptUtil.generateKey64(dialog.password.getText().toString(), salt64adm);
					Settings.Builder settingsBldr = UserContext.getCurrentUser().getSettings().buildUpon();
					settingsBldr.withPasswordRequired(true);
					ContentValues adminVals = new ContentValues();
					adminVals.put(UsersTable.C_USER_NAME, dialog.userName.getText().toString());
					adminVals.put(UsersTable.C_ACCOUNT_TYPE, User.ACCOUNT_TYPE_ADMIN);
					adminVals.put(UsersTable.C_PASSWORD_64, pass64adm);
					adminVals.put(UsersTable.C_SALT_64, salt64adm);
					adminVals.put(UsersTable.C_SETTINGS, settingsBldr.build().toJsonString());
					int rowsUpdated = ctx.getContentResolver().update(UsersProvider.UriHelper.userByLocalUserId(0L), adminVals, null, null);
					if(rowsUpdated!=1){
						res.setErrorMessage("Operacja zakończona niepowodzeniem.");
						return res;
					}else{
						final Cursor adminC = ctx.getContentResolver().query(UsersProvider.UriHelper.userByLocalUserId(0L), UsersTable.COLUMNS, null, null, null);
						if(adminC!=null){
							if(adminC.moveToFirst()){
								final User adm = Util.getUserBuilderFromCursor(adminC, adminC.getPosition()).build();
								UserContext.updateUser(adm);
								UserContext.setInvalidateOptions(true);
							}
							adminC.close();
						}
						
					}
					break;
				case UserDialog.ACTION_CREATE_USER:
					String salt64 = CryptUtil.generateSalt64();
					String pass64 = CryptUtil.generateKey64(dialog.password.getText().toString(), salt64);
					String settingsStr = Settings.getDefaultUserSettings().toJsonString();
					ContentValues userVals = new ContentValues();
					userVals.put(UsersTable.C_USER_NAME, dialog.userName.getText().toString());
					userVals.put(UsersTable.C_ACCOUNT_TYPE, User.ACCOUNT_TYPE_USER);
					userVals.put(UsersTable.C_PASSWORD_64, pass64);
					userVals.put(UsersTable.C_SALT_64, salt64);
					userVals.put(UsersTable.C_SETTINGS, settingsStr);
					try{
						ctx.getContentResolver().insert(UsersProvider.UriHelper.USERS_URI, userVals);	
					}catch (Exception e) {
						res.setErrorMessage("Operacja zakończona niepowodzeniem.");
						return res;
					}										
					break;
				case UserDialog.ACTION_EDIT_USER:
					if(dialog.mUser!=null){
						String editSalt64 = CryptUtil.generateSalt64();
						String editPass64 = CryptUtil.generateKey64(dialog.password.getText().toString(), editSalt64);
						ContentValues editVals = new ContentValues();
						editVals.put(UsersTable.C_USER_NAME, dialog.userName.getText().toString());
						editVals.put(UsersTable.C_PASSWORD_64, editPass64);
						editVals.put(UsersTable.C_SALT_64, editSalt64);
						int updated = ctx.getContentResolver().update(UsersProvider.UriHelper.userByLocalUserId(dialog.mUser.getLocalUserId()), editVals, null, null);
						if(updated!=1){
							res.setErrorMessage("Operacja zakończona niepowodzeniem");
							return res;
						}
					}else{
						res.setErrorMessage("Operacja zakończona niepowodzeniem.");
						return res;
					}					
					break;
				}							
			}else{
				res.setError();
			}
			return res;
		}
		
		@Override
		protected void onPostExecute(ProcessDataResult result) {		
			super.onPostExecute(result);
			dialog.overlay.setVisibility(View.GONE);
			if(result.success){
				if(dialog.mAction==ACTION_CREATE_ADMIN){
					dialog.callback.notifyAdminChanged();
				}
				dialog.dismiss();
			}else{
				showDataFeedback(result);
			}								
			dialog = null;
		}
		
		@Override
		protected void onCancelled(ProcessDataResult result) {		
			super.onCancelled(result);
			dialog.overlay.setVisibility(View.GONE);			
			dialog = null;
		}
		
		private void showDataFeedback(ProcessDataResult result){
			if(!result.success){
				if(dialog.getActivity()!=null){
					dialog.userName.setError(result.errorUserName);					
					dialog.password.setError(result.errorPassword);
					dialog.confirmPassword.setError(result.errorConfirmPassword);					
					if(result.errorMessage!=null){
						Toast.makeText(dialog.getActivity(), result.errorMessage, Toast.LENGTH_LONG).show();
					}	
				}				
			}
		}
		
	}
	
	static class ProcessDataResult{
		
		public ProcessDataResult() {
			success=true;
		}
		
		boolean success;
		
		String errorUserName;
		
		String errorPassword;
		
		String errorConfirmPassword;
		
		String errorMessage;
		
		void setErrorUserName(String errorUserName){
			success = false;
			this.errorUserName = errorUserName;
		}
		
		void setErrorPassword(String errorPassword){
			success = false;
			this.errorPassword = errorPassword;
		}
		
		void setErrorConfirmPassword(String errorConfirmPassword){
			success = false;
			this.errorConfirmPassword = errorConfirmPassword;
		}
		
		void setErrorMessage(String errorMessage){
			success = false;
			this.errorMessage = errorMessage;
		}
		
		void setError(){
			success = false;
		}
		
	}

}
