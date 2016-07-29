package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class DownloadManagerProblemDialog extends DialogFragment {

	public static final String TAG = "download_manager_problem.dialog.fragment";

	public static DownloadManagerProblemDialog newInstance() {
		final DownloadManagerProblemDialog res = new DownloadManagerProblemDialog();
		final Bundle bundle = new Bundle();
		res.setArguments(bundle);
		return res;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.fdmpd_title);
		final View res = inflater.inflate(R.layout.f_download_manager_problem_dialog, container, false);

		final Button btnOk = (Button) res.findViewById(R.id.fdmpd_btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri
						.parse("package:com.android.providers.downloads")));
				DownloadManagerProblemDialog.this.getFragmentManager().popBackStack();
				dismiss();
			}
		});
		final Button btnCancel = (Button) res.findViewById(R.id.fdmpd_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DownloadManagerProblemDialog.this.getFragmentManager().popBackStack();
				dismiss();
			}
		});
		return res;
	}

}
