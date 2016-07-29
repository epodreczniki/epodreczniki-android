package pl.epodreczniki.fragment;

import java.util.UUID;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.SwipeReaderActivity;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.model.Note.LocationPart;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class NoteCreateDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "note.create.dialog";
	
	private static final String ARGS_PAGE_ID_KEY = "page.id.key";
	
	private static final String ARGS_LOCATION_KEY = "new.note.location.key";
	
	private static final String ARGS_LOCAL_USER_ID_KEY = "local.user.id.key";
	
	private static final String ARGS_HANDBOOK_ID_KEY = "handbook.id.key";
	
	private static final String ARGS_MODULE_ID_KEY = "module.id.key";	
	
	private static final String ARGS_NOTES_TO_MERGE_KEY = "notes.to.merge.key";
	
	private static final String ARGS_NOTE_TEXT = "note.text";
	
	private static final String SAVED_INST_PAGE_KEY = "si.page.key";
	
	private static final String SAVED_INST_SUBJECT_KEY = "si.subject.key";
	
	private static final String SAVED_INST_VALUE_KEY = "si.value.key";
	
	private static final String SAVED_INST_TYPE_KEY = "si.type.key";
	
	private String pageId;		
	
	private LocationPart[] location;
	
	private long localUserId;
	
	private String handbookId;
	
	private String moduleId;
	
	private String[] notesToMerge;
	
	private int page = 0;
	
	private String enteredSubject;
	
	private String enteredValue;
	
	private int chosenType = 4;
	
	private View page0Overlay;
	
	private View page1Edit;
	
	private View[] pages;
	
	private RadioGroup page1Group;
	
	private RadioButton page1GreenButton;
	
	private RadioButton page1BlueButton;
	
	private RadioButton page1RedButton;
	
	private EditText page1EtSubject;
	
	private EditText page1EtValue;
	
	private Button page1BtnSave;
	
	private Button page1BtnCancel;
	
	private QueryNotesToMergeTask task;
	
	public static NoteCreateDialog newInstance(String pageId, LocationPart[] location, long localUserId, String handbookId, String moduleId, String[] notesToMerge, String noteText){
		final Bundle bundle = new Bundle();
		bundle.putString(ARGS_PAGE_ID_KEY, pageId);
		bundle.putParcelableArray(ARGS_LOCATION_KEY, location);
		bundle.putLong(ARGS_LOCAL_USER_ID_KEY, localUserId);
		bundle.putString(ARGS_HANDBOOK_ID_KEY, handbookId);
		bundle.putString(ARGS_MODULE_ID_KEY, moduleId);
		bundle.putStringArray(ARGS_NOTES_TO_MERGE_KEY, notesToMerge);
		bundle.putString(ARGS_NOTE_TEXT, noteText);
		final NoteCreateDialog res = new NoteCreateDialog();
		res.setArguments(bundle);
		return res;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		final Bundle args = getArguments();
		if(args!=null){
			pageId = args.getString(ARGS_PAGE_ID_KEY);
			location = (LocationPart[]) args.getParcelableArray(ARGS_LOCATION_KEY);
			localUserId = args.getLong(ARGS_LOCAL_USER_ID_KEY);
			handbookId = args.getString(ARGS_HANDBOOK_ID_KEY);
			moduleId = args.getString(ARGS_MODULE_ID_KEY);
			notesToMerge = args.getStringArray(ARGS_NOTES_TO_MERGE_KEY);
			enteredSubject = args.getString(ARGS_NOTE_TEXT);			
		}
		if(savedInstanceState!=null){
			page = savedInstanceState.getInt(SAVED_INST_PAGE_KEY);
			enteredSubject = savedInstanceState.getString(SAVED_INST_SUBJECT_KEY);
			enteredValue = savedInstanceState.getString(SAVED_INST_VALUE_KEY);
			chosenType = savedInstanceState.getInt(SAVED_INST_TYPE_KEY);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle("Dodaj notatkę");
		final View res = inflater.inflate(R.layout.f_note_create, container, false);
		page0Overlay = res.findViewById(R.id.fncd_ll_page0);
		page1Edit = res.findViewById(R.id.fncd_ll_page1);
		page1Group = (RadioGroup) res.findViewById(R.id.fncd_rg_color);
		page1GreenButton = (RadioButton) res.findViewById(R.id.fncd_rb_green);
		page1GreenButton.setOnClickListener(this);
		page1BlueButton = (RadioButton) res.findViewById(R.id.fncd_rb_blue);
		page1BlueButton.setOnClickListener(this);
		page1RedButton = (RadioButton) res.findViewById(R.id.fncd_rb_red);
		page1RedButton.setOnClickListener(this);
		page1EtSubject = (EditText) res.findViewById(R.id.fncd_et_subject);
		page1EtSubject.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enteredSubject = page1EtSubject.getText().toString();
			}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		page1EtValue = (EditText) res.findViewById(R.id.fncd_et_value);
		page1EtValue.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enteredValue = page1EtValue.getText().toString();
			}			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		page1BtnSave = (Button) res.findViewById(R.id.fncd_btn_save);
		page1BtnSave.setOnClickListener(this);
		page1BtnCancel = (Button) res.findViewById(R.id.fncd_btn_cancel);
		page1BtnCancel.setOnClickListener(this);
		pages = new View[]{page0Overlay,page1Edit};
		if(isInitialized()){
			updateView();
		}else{
			task = new QueryNotesToMergeTask();
			task.execute();
		}
		return res;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {	
		super.onSaveInstanceState(outState);
		outState.putInt(SAVED_INST_PAGE_KEY, page);
		outState.putString(SAVED_INST_SUBJECT_KEY, enteredSubject);
		outState.putString(SAVED_INST_VALUE_KEY, enteredValue);
		outState.putInt(SAVED_INST_TYPE_KEY, chosenType);
	}
	
	@Override
	public void onClick(View v) {
		final int id = v.getId();
		switch(id){
		case R.id.fncd_btn_save:
			if(validateInput()){
				final Note.Builder b = new Note.Builder();
				for(LocationPart p : location){
					b.withLocationPart(p);
				}
				b.withLocalNoteId(UUID.randomUUID().toString())
				.withSubject(enteredSubject)
				.withValue(enteredValue)
				.withLocalUserId(localUserId)
			    .withHandbookId(handbookId)
			    .withModuleId(moduleId)
			    .withPageId(pageId);
				b.withType(chosenType);
				final SwipeReaderActivity act = (SwipeReaderActivity)NoteCreateDialog.this.getActivity();
				act.addNote(b.build(), notesToMerge);
				dismiss();
			}			
			break;
		case R.id.fncd_btn_cancel:
			dismiss();
			break;
		case R.id.fncd_rb_green:
			chosenType = 4;
			break;
		case R.id.fncd_rb_blue:
			chosenType = 5;
			break;
		case R.id.fncd_rb_red:
			chosenType = 6;
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
		return page>0;
	}	
	
	private void updateView(){
		switch(chosenType){
		case 4:
			page1Group.check(R.id.fncd_rb_green);
			break;
		case 5:
			page1Group.check(R.id.fncd_rb_blue);
			break;
		case 6:
			page1Group.check(R.id.fncd_rb_red);
			break;
		}
		page1EtSubject.setText(enteredSubject);
		page1EtValue.setText(enteredValue);
		if(pages[page].getVisibility()!=View.VISIBLE){
			for(int i=0; i<pages.length; i++){
				pages[i].setVisibility(page==i?View.VISIBLE:View.GONE);
			}
		}
		page1EtValue.requestFocus();
	}
	
	private void changePage(int page){
		this.page = page;
		updateView();
	}
	
	private boolean validateInput(){
		boolean res = true;
		if(page1EtSubject.getText().toString().trim().length()==0){
			page1EtSubject.setError("Notatka musi mieć tytuł");
			res = false;
		}
		return res;
	}
	
	class QueryNotesToMergeTask extends AsyncTask<Void,Void,String>{
		
		@Override
		protected String doInBackground(Void... params) {
			if(notesToMerge!=null){
				final Context ctx = getActivity();								
				if(ctx!=null){
					StringBuilder res = new StringBuilder();
					int colIdx = -1;
					for(String localNoteId : notesToMerge){
						Uri u = NotesProvider.UriHelper.noteByLocalNoteId(localNoteId);
						Cursor c = ctx.getContentResolver().query(u, NotesTable.COLUMNS, null, null, null);					
						if(c!=null){
							if(colIdx==-1){
								colIdx = c.getColumnIndex(NotesTable.C_VALUE);
							}
							if(c.moveToFirst() && colIdx!=-1){
								res.append(c.getString(colIdx)).append("\n");
							}
							c.close();
						}
					}
					return res.toString();
				}
			}					
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {		
			super.onPostExecute(result);
			if(result!=null){
				enteredValue = result;				
			}
			changePage(1);
		}
		
	}

}
