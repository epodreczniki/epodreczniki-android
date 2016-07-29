package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.SettingsActivity;
import pl.epodreczniki.model.User;
import pl.epodreczniki.util.CheckCredentialsTask;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.CheckCredentialsTask.CheckCredentialsCallback;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ReenterPasswordDialog extends DialogFragment implements CheckCredentialsCallback,OnClickListener{
	
	public static final String TAG = "reenter.password.dialog";
	
	private static final int STATE_FRESH = 0;
	
	private static final int STATE_IN_PROGRESS = 1;
	
	private EditText password;
	
	private Button button;
	
	private ProgressBar pb;
	
	private CheckCredentialsTask task;	
	
	public static ReenterPasswordDialog getInstance(){
		final ReenterPasswordDialog res = new ReenterPasswordDialog();
		return res;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle("Podaj hasło");
		final View res = inflater.inflate(R.layout.f_reenter_password, container,false);
		password = (EditText) res.findViewById(R.id.frp_password);
		button = (Button) res.findViewById(R.id.frp_btn);
		button.setOnClickListener(this);
		pb = (ProgressBar) res.findViewById(R.id.frp_pb);
		setState(STATE_FRESH);
		return res;
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if(task!=null){
			task.cancel(true);
			task = null;
		}
		FragmentManager fm = getFragmentManager();
		if(fm!=null){
			getFragmentManager().popBackStack();	
		}		
	}

	@Override
	public void handleLoginResult(User loggedInUser) {
		if(loggedInUser!=null){
			Intent i = new Intent(this.getActivity(),SettingsActivity.class);
			startActivity(i);
		}else{
			Toast.makeText(this.getActivity(), "Nieprawidłowe hasło", Toast.LENGTH_LONG).show();
		}
		task = null;
		dismiss();
	}

	@Override
	public void handleLoginCancel() {
		task = null;
	}

	@Override
	public void onClick(View v) {
		if(v.getId()==R.id.frp_btn){
			if(task==null){				
				setState(STATE_IN_PROGRESS);
				task = new CheckCredentialsTask(this, UserContext.getCurrentUser(), password.getText().toString());
				task.execute();
			}
		}
	}
	
	private void setState(int state){
		switch(state){
		case STATE_FRESH:
			pb.setVisibility(View.GONE);
			button.setEnabled(true);
			break;
		case STATE_IN_PROGRESS:
			pb.setVisibility(View.VISIBLE);
			button.setEnabled(false);
			break;
		}
	}

}
