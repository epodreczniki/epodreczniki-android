package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.BookDetailsActivity;
import pl.epodreczniki.activity.BookGridActivity;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.util.Constants;
import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class UpdateDialog extends DialogFragment{
	
	private static final String BUNDLE_KEY = "book.to.update";
	
	public static final String TAG = "update.dialog.fragment";
	
	private Book book;
	
	public static UpdateDialog newInstance(Book b){
		final UpdateDialog res = new UpdateDialog();
		final Bundle bundle = new Bundle();
		bundle.putParcelable(BUNDLE_KEY, b);
		res.setArguments(bundle);
		return res;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		book = getArguments().getParcelable(BUNDLE_KEY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_update_confirm);
		final View res = inflater.inflate(R.layout.f_update_dialog, container, false);
		final TextView titleTv = (TextView) res.findViewById(R.id.fud_title);
		titleTv.setText(book.getNewestVersion().getMdTitle());
		final TextView newVersionTv = (TextView) res.findViewById(R.id.fud_tv_new_version);
		newVersionTv.setText(String.valueOf(book.getNewestVersion().getMdVersion()));
		final TextView sizeTv = (TextView) res.findViewById(R.id.fud_tv_size);
		sizeTv.setText(String.format("%.2f MB", (float)book.getNewestVersion().getSizeExtracted()/(1024*1024)));
		final TextView licenseTv = (TextView) res.findViewById(R.id.fud_tv_license);
		licenseTv.setText(Html.fromHtml(String.format(Constants.LINK_WRAP, book.getNewestVersion().getMdLicense(), book.getNewestVersion().getMdLicense())));
		licenseTv.setMovementMethod(LinkMovementMethod.getInstance());
		final Button btnOk = (Button) res.findViewById(R.id.fud_btn_ok);
		btnOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				final Activity act = UpdateDialog.this.getActivity();
				if(act instanceof BookDetailsActivity){
					Log.e("UPDATE DIALOG","requesting transfer from details");
					((BookDetailsActivity)act).requestXfer(book);
				}else if(act instanceof BookGridActivity){
					((BookGridActivity)act).requestXfer(book);
				}				
				UpdateDialog.this.getFragmentManager().popBackStack();
				dismiss();
			}			
		});
		final Button btnCancel = (Button) res.findViewById(R.id.fud_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {	
				UpdateDialog.this.getFragmentManager().popBackStack();
				dismiss();
			}			
		});
		return res;
	}	
	
}
