package pl.epodreczniki.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import pl.epodreczniki.R;
import pl.epodreczniki.model.Navigable;
import pl.epodreczniki.model.Note;

public class NoteShowContentDialog extends DialogFragment{
	
	public static final String TAG = "nscd.tag";

	private static final String ARGS_NOTE = "nscd.args.note";
	
	private FrameLayout flSubjectWrapper;
	
	private TextView tvSubject;
	
	private TextView tvValue;
	
	private ImageButton btnGo;
	
	private Note note;
	
	private Navigable navigationActivity;
	
	public static NoteShowContentDialog newInstance(Note n){
		final NoteShowContentDialog res = new NoteShowContentDialog();
		final Bundle b = new Bundle();
		b.putParcelable(ARGS_NOTE, n);
		res.setArguments(b);
		return res;
	}
	
	@Override
	public void onAttach(Activity activity) {	
		super.onAttach(activity);
		navigationActivity = (Navigable) activity;
	}	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	
		final Bundle args = getArguments();
		note = args.getParcelable(ARGS_NOTE);		
		final View res = inflater.inflate(R.layout.f_note_show_content, container, false);
		flSubjectWrapper = (FrameLayout) res.findViewById(R.id.fnsc_fl_subject_wrap);
		switch(note.getType()){
		case 4:
			flSubjectWrapper.setBackgroundColor(getActivity().getResources().getColor(R.color.note_green));
			break;
		case 5:
			flSubjectWrapper.setBackgroundColor(getActivity().getResources().getColor(R.color.note_blue));
			break;
		case 6:
			flSubjectWrapper.setBackgroundColor(getActivity().getResources().getColor(R.color.note_red));
			break;
		}
		tvSubject = (TextView) res.findViewById(R.id.fnsc_tv_subject);
		tvSubject.setText(note.getSubject());
		tvValue = (TextView) res.findViewById(R.id.fnsc_tv_value);
		tvValue.setText(note.getValue());
		btnGo = (ImageButton) res.findViewById(R.id.fnsc_btn_go);
		btnGo.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				String[] ids = note.getHandbookId().split(":");
				String mdContentId = ids[0];
				int mdVersion = Integer.parseInt(ids[1]);
				navigationActivity.showPage(note.getPageId(), mdContentId, mdVersion);
				dismiss();
			}
		});
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
	public void onDetach() {	
		super.onDetach();
		Log.e("NSCD", "onDetach");
		navigationActivity = null;
	}
	
}
