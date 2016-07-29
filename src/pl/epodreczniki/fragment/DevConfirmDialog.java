package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.DevMainActivity;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class DevConfirmDialog extends DialogFragment implements OnClickListener{
	
	private static final String BUNDLE_KEY = "dev.download.url";
	
	public static final String TAG = "dev.confirm.dialog.fragment";
	
	private Button btnOk;
	
	private Button btnCancel;
	
	private String url;
	
	private boolean restorePreviousState = true;
	
	public static DevConfirmDialog newInstance(String url){				
		final DevConfirmDialog res = new DevConfirmDialog();
		final Bundle b = new Bundle();
		b.putString(BUNDLE_KEY, url);
		res.setArguments(b);
		return res;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		this.url = getArguments().getString(BUNDLE_KEY);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle("Potwierdź pobieranie");
		final View res = inflater.inflate(R.layout.f_dev_confirm_dialog,container,false);
		btnOk = (Button) res.findViewById(R.id.fdcd_ok);
		btnOk.setOnClickListener(this);
		btnCancel = (Button) res.findViewById(R.id.fdcd_cancel);
		btnCancel.setOnClickListener(this);
		return res;
	}

	@Override
	public void onClick(View v) {
		final Activity act = getActivity();
		if(v==btnOk){			
			if(act instanceof DevMainActivity){
				restorePreviousState = false;
				((DevMainActivity )act).requestTransfer(url);
			}
		}			
		dismiss();
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {	
		super.onDismiss(dialog);
		if(restorePreviousState){
			final Activity act = getActivity();
			if(act instanceof DevMainActivity){				
				((DevMainActivity )act).restorePreviousStatus();
			}
		}
		final FragmentManager fm = getFragmentManager();
		if(fm!=null){
			getFragmentManager().popBackStack();	
		}		
	}
	
}
