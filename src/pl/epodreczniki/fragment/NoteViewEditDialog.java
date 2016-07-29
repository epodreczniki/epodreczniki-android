package pl.epodreczniki.fragment;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.SwipeReaderActivity;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.util.NotesUtil;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class NoteViewEditDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "note.view.edit.dialog";
	
	private static final String SAVED_INST_KEY_PAGE = "page.key";
	
	private static final String SAVED_INST_KEY_NOTE = "note.key";
	
	private static final String ARGS_KEY_LOCAL_NOTE_ID = "local.note.id.key";
	
	private static final String[] TITLES = new String[]{"Proszę czekać...","Notatka","Edytuj notatkę","Potwierdź usunięcie"};
	
	private String localNoteId;		
	
	private Note note;
	
	private View page0Overlay;
	
	private View page1View;
	
	private View page2Edit;
	
	private View page3Confirm;
	
	private View[] pages;

	private FrameLayout page1FlSubjectWrap;
	
	private TextView page1TvSubject;
	
	private TextView page1TvValue;
	
	private ImageButton page1EditBtn;
	
	private ImageButton page1DeleteBtn;
	
	private RadioGroup page2Rg;		
	
	private EditText page2EtSubject;
	
	private EditText page2EtValue;
	
	private Button page2CancelBtn;
	
	private Button page2SaveBtn;
	
	private Button page3NoBtn;
	
	private Button page3YesBtn;
	
	private int page = 0;
	
	private InitTask task;
	
	public static NoteViewEditDialog newInstance(String localNoteId){
		final Bundle bundle = new Bundle();		
		bundle.putString(ARGS_KEY_LOCAL_NOTE_ID, localNoteId);		
		final NoteViewEditDialog res = new NoteViewEditDialog();
		res.setArguments(bundle);
		return res;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Bundle args = getArguments();
		if(args!=null){
			localNoteId = args.getString(ARGS_KEY_LOCAL_NOTE_ID);
		}
		if(savedInstanceState!=null){
			page = savedInstanceState.getInt(SAVED_INST_KEY_PAGE);
			note = savedInstanceState.getParcelable(SAVED_INST_KEY_NOTE);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.f_note_view_edit, container, false);
		page0Overlay = res.findViewById(R.id.fnve_ll_page0);
		page1View = res.findViewById(R.id.fnve_ll_page1);
		page2Edit = res.findViewById(R.id.fnve_ll_page2);
		page3Confirm = res.findViewById(R.id.fnve_ll_page3);
		page1FlSubjectWrap = (FrameLayout) res.findViewById(R.id.fnve_fl_subject_wrap);
		page1TvSubject = (TextView) res.findViewById(R.id.fnve_tv_subject);
		page1TvValue = (TextView) res.findViewById(R.id.fnve_tv_value);
		page1TvValue.setMovementMethod(new ScrollingMovementMethod());
		page1DeleteBtn = (ImageButton) res.findViewById(R.id.fnve_ib_delete);
		page1DeleteBtn.setOnClickListener(this);		
		page1EditBtn = (ImageButton) res.findViewById(R.id.fnve_ib_edit);
		page1EditBtn.setOnClickListener(this);
		page2Rg = (RadioGroup) res.findViewById(R.id.fnve_rg_color);		
		page2EtSubject = (EditText) res.findViewById(R.id.fnve_et_subject);
		page2EtValue = (EditText) res.findViewById(R.id.fnve_et_value);
		page2CancelBtn = (Button) res.findViewById(R.id.fnve_btn_cancel);
		page2CancelBtn.setOnClickListener(this);		
		page2SaveBtn = (Button) res.findViewById(R.id.fnve_btn_save);
		page2SaveBtn.setOnClickListener(this);
		page3NoBtn = (Button) res.findViewById(R.id.fnve_btn_no);
		page3NoBtn.setOnClickListener(this);
		page3YesBtn = (Button) res.findViewById(R.id.fnve_btn_yes);
		page3YesBtn.setOnClickListener(this);
		pages = new View[]{page0Overlay,page1View,page2Edit,page3Confirm};
		if(isInitialized()){
			updateView();
		}else{
			task = new InitTask();
			task.execute();
		}
		return res;
	}	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {		
		super.onSaveInstanceState(outState);
		outState.putInt(SAVED_INST_KEY_PAGE, page);
		outState.putParcelable(SAVED_INST_KEY_NOTE, note);
	}
	
	@Override
	public void onClick(View v) {
		final int id = v.getId();
		final SwipeReaderActivity activity = (SwipeReaderActivity) getActivity();
		switch(id){
		case R.id.fnve_ib_edit:
			changePage(2);
			break;
		case R.id.fnve_ib_delete:
			changePage(3);
			break;
		case R.id.fnve_btn_cancel:
			changePage(1);			
			if(activity!=null){
				final InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				if(manager!=null){
					manager.hideSoftInputFromWindow(page2Edit.getWindowToken(), 0);
					page1EditBtn.requestFocus();
				}
			}
			break;
		case R.id.fnve_btn_save:
			if(activity!=null){
				if(validateInput()){
					final Note.Builder bldr = Note.Builder.fromNote(note);
					bldr.withSubject(page2EtSubject.getText().toString());
					bldr.withValue(page2EtValue.getText().toString());
					switch(page2Rg.getCheckedRadioButtonId()){
					case R.id.fnve_rb_green:
						bldr.withType(4);
						break;
					case R.id.fnve_rb_blue:
						bldr.withType(5);
						break;
					case R.id.fnve_rb_red:
						bldr.withType(6);
						break;
					}
					activity.updateNote(bldr.build());
					dismiss();
				}
			}
			break;
		case R.id.fnve_btn_no:
			changePage(1);
			break;
		case R.id.fnve_btn_yes:			
			if(activity!=null){
				activity.deleteNote(note);
				dismiss();
			}
			break;
		}
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {	
		super.onDismiss(dialog);
		if(task!=null){
			task.cancel(true);
			task = null;
		}
		final FragmentManager fm = getFragmentManager();
		if(fm!=null){
			fm.popBackStack();	
		}		
	}		
	
	private boolean isInitialized(){
		return page>0&&note!=null;
	}		
	
	private void updateView(){
		getDialog().setTitle(TITLES[page]);
		if(note!=null){
			page1TvSubject.setText(note.getSubject());
			page1TvValue.setText(note.getValue());
			switch(note.getType()){
			case 4:
				page2Rg.check(R.id.fnve_rb_green);
				page1FlSubjectWrap.setBackgroundColor(getResources().getColor(R.color.note_green));
				break;
			case 5:
				page2Rg.check(R.id.fnve_rb_blue);
				page1FlSubjectWrap.setBackgroundColor(getResources().getColor(R.color.note_blue));
				break;
			case 6:
				page2Rg.check(R.id.fnve_rb_red);
				page1FlSubjectWrap.setBackgroundColor(getResources().getColor(R.color.note_red));
				break;
			}
			page2EtSubject.setText(note.getSubject());
			page2EtValue.setText(note.getValue());
			if(page==2){			
				page2EtValue.requestFocus();
			}
			page2EtValue.setSelection(page2EtValue.getText().length());
			if(pages[page].getVisibility()!=View.VISIBLE){
				for(int i=0;i<pages.length;i++){
					pages[i].setVisibility(page==i?View.VISIBLE:View.GONE);
				}
			}			
		}			
	}
	
	private void changePage(int page){
		this.page = page;
		updateView();
	}
	
	private boolean validateInput(){
		boolean res = true;
		if(page2EtSubject.getText().toString().trim().length()==0){
			page2EtSubject.setError("Notatka musi mieć tytuł");
			res = false;
		}
		return res;
	}	
	
	class InitTask extends AsyncTask<Void,Void,Note>{

		@Override
		protected Note doInBackground(Void... params) {
			final Context ctx = getActivity();
			Note res = null;
			if(ctx!=null){
				final Cursor c = ctx.getContentResolver().query(NotesProvider.UriHelper.noteByLocalNoteId(localNoteId), NotesTable.COLUMNS, null, null, null);
				if(c!=null){
					if(c.moveToFirst()){
						Note.Builder bldr = NotesUtil.getNoteBuilderFromCursor(c, c.getPosition()); 
						if(bldr!=null){
							res = bldr.build();
						}
					}
					c.close();
				}
			}
			return res;
		}
		
		@Override
		protected void onPostExecute(Note result) {
			if(result!=null){
				note = result;
				changePage(1);	
			}else{
				Toast.makeText(getActivity(), "Nie ma takiej notatki", Toast.LENGTH_SHORT).show();
				dismiss();
			}			
		}
		
	}
	
}
