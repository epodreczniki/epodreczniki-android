package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
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
import android.widget.EditText;

public class ChangePasswordDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "change.password.dialog.tag";
	
	private static final String ARGS_USER = "cpd.args.user";		
	
	public static ChangePasswordDialog getInstance(User user){
		final Bundle b = new Bundle();
		b.putParcelable(ARGS_USER, user);
		final ChangePasswordDialog res = new ChangePasswordDialog();
		res.setArguments(b);
		return res;
	}
	
	private User mUser;
	
	private EditText password;
	
	private EditText confirmPassword;
	
	private Button cancel;
	
	private Button ok;
	
	public ChangePasswordDialog(){		
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
		getDialog().setTitle("Zmiana hasła");
		final View res = inflater.inflate(R.layout.f_change_password, container, false);
		password = (EditText) res.findViewById(R.id.fcp_password);
		confirmPassword = (EditText) res.findViewById(R.id.fcp_confirm_password);
		cancel = (Button) res.findViewById(R.id.fcp_cancel);
		cancel.setOnClickListener(this);
		ok = (Button) res.findViewById(R.id.fcp_ok);
		ok.setOnClickListener(this);
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

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.fcp_ok:
			if(verifyForm()){
				new ChangePasswordTask(getActivity(), mUser.getLocalUserId(), password.getText().toString()).execute();
				dismiss();
			}
			break;
		case R.id.fcp_cancel:
			dismiss();
			break;
		}
	}	
	
	boolean verifyForm(){
		if(password.getText().toString().trim().length()<Constants.MIN_PASSWORD_LENGTH){
			password.setError("Hasło powinno mieć co najmniej 6 znaków");
			return false;
		}
		if(!password.getText().toString().equals(confirmPassword.getText().toString())){
			confirmPassword.setError("Wpisane hasła różnią się");
			return false;
		}
		return true;
	}
	
	private static class ChangePasswordTask extends AsyncTask<Void, Void, Void>{

		private Context ctx;
		
		private final long userId;
		
		private final String password;
		
		public ChangePasswordTask(Context ctx,long userId, String password) {
			this.ctx = ctx.getApplicationContext();
			this.userId = userId;
			this.password = password;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			ContentValues vals = new ContentValues();
			String salt64 = CryptUtil.generateSalt64();
			String password64 = CryptUtil.generateKey64(password, salt64);
			vals.put(UsersTable.C_SALT_64, salt64);
			vals.put(UsersTable.C_PASSWORD_64, password64);
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
