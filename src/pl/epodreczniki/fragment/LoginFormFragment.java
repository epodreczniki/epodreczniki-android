package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.model.User;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class LoginFormFragment extends Fragment{

	private static final String ARGS_USER = "lff_args_user";
	
	public interface LoginFormCallback{
		void doLogin(User user,String enteredPassword, boolean skipPassword);
	}
	
	public static final LoginFormFragment newInstance(User user){
		final LoginFormFragment res = new LoginFormFragment();
		final Bundle b = new Bundle();
		b.putParcelable(ARGS_USER, user);		
		res.setArguments(b);
		return res;
	}
	
	private User user;
	
	private LoginFormCallback callback;
	
	private EditText login;
	
	private EditText password;
	
	private Button button;	
	
	@Override
	public void onAttach(Activity activity) {	
		super.onAttach(activity);
		if(activity instanceof LoginFormCallback){
			callback = (LoginFormCallback) activity;
		}else{
			throw new RuntimeException("wrong activity");
		}
	}
	
	@Override
	public void onDetach() {
		callback = null;
		super.onDetach();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		user = getArguments().getParcelable(ARGS_USER);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.f_login_form, container, false);
		if(user==null){
			res.setVisibility(View.INVISIBLE);
		}else{
			res.setVisibility(View.VISIBLE);
			login = (EditText) res.findViewById(R.id.flf_login);
			login.setText(user.getUserName());
			login.setKeyListener(null);
			password = (EditText) res.findViewById(R.id.flf_password);
			password.requestFocus();
			final boolean skipPassword = user.isRegularUser() && !user.getSettings().isPasswordRequired();
			if(skipPassword){
				password.setVisibility(View.GONE);
			}					
			button = (Button) res.findViewById(R.id.flf_button);
			button.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					callback.doLogin(user, password.getText().toString(),skipPassword);
				}
			});
		}
			
		return res;
	}
	
}
