package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.CryptUtil;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

public class ModifyPrivilegesDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "modify.privileges.dialog";
	
	private static final String ARGS_USER = "args.user";
	
	private User mUser;
	
	private CheckBox manageBooks;
	
	private CheckBox changePassword;
	
	private EditText password;
	
	private EditText confirmPassword;
	
	private Button ok;
	
	private Button cancel;
	
	public static ModifyPrivilegesDialog getInstance(User user){
		final Bundle b = new Bundle();
		final ModifyPrivilegesDialog res = new ModifyPrivilegesDialog();
		b.putParcelable(ARGS_USER, user);
		res.setArguments(b);
		return res;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getArguments()!=null){
			mUser = getArguments().getParcelable(ARGS_USER);
		}else{
			dismiss();
		}		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle(mUser.getUserName());
		final View res = inflater.inflate(R.layout.f_modify_privileges,container,false);
		manageBooks = (CheckBox) res.findViewById(R.id.fmp_manage_books);
		changePassword = (CheckBox) res.findViewById(R.id.fmp_change_password_switch);		
		changePassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				password.setEnabled(isChecked);
				confirmPassword.setEnabled(isChecked);
			}
		});
		password = (EditText) res.findViewById(R.id.fmp_password);
		confirmPassword = (EditText) res.findViewById(R.id.fmp_confirm);
		ok = (Button) res.findViewById(R.id.fmp_ok);
		ok.setOnClickListener(this);
		cancel = (Button) res.findViewById(R.id.fmp_cancel);
		cancel.setOnClickListener(this);
		if(savedInstanceState==null){
			updateUI();	
		}		
		return res;
	}	
	
	@Override
	public void onDismiss(DialogInterface dialog) {	
		super.onDismiss(dialog);
		final FragmentManager fm = getFragmentManager();
		if(fm!=null){
			fm.popBackStack();
		}
	}
	
	private void updateUI(){
		manageBooks.setChecked(mUser.getSettings().canManageBooks());
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.fmp_ok:
			if(verifyForm()){
				final Settings newSettings = mUser.getSettings().buildUpon().withManageBooks(manageBooks.isChecked()).build();
				new ModifyPrivilegesTask(getActivity().getApplicationContext(),mUser.getLocalUserId(),changePassword.isChecked()?password.getText().toString():null, newSettings).execute();
				dismiss();
			}
			break;
		case R.id.fmp_cancel:
			dismiss();
			break;
		}
	}
	
	boolean verifyForm(){
		if(changePassword.isChecked()){
			if(password.getText().toString().trim().length()<Constants.MIN_PASSWORD_LENGTH){
				password.setError("Hasło powinno mieć co najmniej 6 znaków");
				return false;
			}
			if(!password.getText().toString().equals(confirmPassword.getText().toString())){
				confirmPassword.setError("Wpisane hasła różnią się");
				return false;
			}	
		}
		return true;
	}
	
	private static class ModifyPrivilegesTask extends AsyncTask<Void,Void,Void>{

		private Context ctx;
		
		private final long userId;
		
		private final String password;
		
		private final Settings settings;
		
		ModifyPrivilegesTask(Context ctx,long userId,String password,Settings settings){
			this.ctx=ctx;
			this.userId=userId;
			this.password=password;
			this.settings=settings;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			ContentValues vals = new ContentValues();
			if(password!=null){
				String salt64 = CryptUtil.generateSalt64();
				String password64 = CryptUtil.generateKey64(password, salt64);
				vals.put(UsersTable.C_SALT_64, salt64);
				vals.put(UsersTable.C_PASSWORD_64, password64);
			}
			vals.put(UsersTable.C_SETTINGS, settings.toJsonString());
			ctx.getContentResolver().update(UsersProvider.UriHelper.userByLocalUserId(userId), vals, null, null);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			ctx = null;
		}
		
		@Override
		protected void onCancelled(Void result) {		
			super.onCancelled(result);
			ctx = null;
		}
		
	}

}
