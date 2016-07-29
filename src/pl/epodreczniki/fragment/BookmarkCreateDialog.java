package pl.epodreczniki.fragment;

import java.util.UUID;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.SwipeReaderActivity;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.model.Note.LocationPart;
import pl.epodreczniki.util.NotesUtil;
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

public class BookmarkCreateDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "bookmark.create.dialog";
	
	private static final String ARGS_PAGE_ID_KEY = "page.id.key";
	
	private static final String ARGS_LOCATION_KEY = "new.note.location.key";
	
	private static final String ARGS_LOCAL_USER_ID_KEY = "local.user.id.key";
	
	private static final String ARGS_HANDBOOK_ID_KEY = "handbook.id.key";
	
	private static final String ARGS_MODULE_ID_KEY = "module.id.key";	
	
	private static final String ARGS_NOTES_TO_MERGE_KEY = "notes.to.merge.key";
	
	private static final String ARGS_NOTE_TEXT = "note.text";
	
	private static final String SAVED_INST_PAGE_KEY = "si.page.key";
	
	private static final String SAVED_INST_SUBJECT_KEY = "si.subject.key";	
	
	private static final String SAVED_INST_TYPE_KEY = "si.type.key";
	
	private String pageId;
	
	private LocationPart[] location;
	
	private long localUserId;
	
	private String handbookId;
	
	private String moduleId;
	
	private String[] notesToMerge;
	
	private int page = 0;
	
	private String enteredSubject;	
	
	private int chosenType = 0;
	
	private View page0Overlay;
	
	private View page1Question;
	
	private View page2Edit;
	
	private Button page1BtnYes;
	
	private Button page1BtnNo;
	
	private RadioGroup page2Group;
	
	private RadioButton page2GreenButton;
	
	private RadioButton page2BlueButton;
	
	private RadioButton page2RedButton;
	
	private RadioButton page2YellowButton;
	
	private EditText page2Subject;
	
	private Button page2BtnSave;
	
	private Button page2BtnCancel;
	
	private View[] pages;
	
	private MergeCheckTask task;
	
	public static BookmarkCreateDialog newInstance(String pageId, LocationPart[] location, long localUserId, String handbookId, String moduleId, String[] notesToMerge, String noteText){
		final Bundle bundle = new Bundle();
		bundle.putString(ARGS_PAGE_ID_KEY, pageId);
		bundle.putParcelableArray(ARGS_LOCATION_KEY, location);
		bundle.putLong(ARGS_LOCAL_USER_ID_KEY, localUserId);
		bundle.putString(ARGS_HANDBOOK_ID_KEY, handbookId);
		bundle.putString(ARGS_MODULE_ID_KEY, moduleId);
		bundle.putStringArray(ARGS_NOTES_TO_MERGE_KEY, notesToMerge);
		bundle.putString(ARGS_NOTE_TEXT, noteText);
		final BookmarkCreateDialog res = new BookmarkCreateDialog();
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
			chosenType = savedInstanceState.getInt(SAVED_INST_TYPE_KEY);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle("Dodaj zakładkę");
	    final View res = inflater.inflate(R.layout.f_bookmark_create, container, false);
	    page0Overlay = res.findViewById(R.id.fbcd_ll_page0);
	    page1Question = res.findViewById(R.id.fbcd_ll_page1);
	    page1BtnNo = (Button) res.findViewById(R.id.fbcd_btn_no);
	    page1BtnNo.setOnClickListener(this);
	    page1BtnYes = (Button) res.findViewById(R.id.fbcd_btn_yes);
	    page1BtnYes.setOnClickListener(this);
	    page2Edit = res.findViewById(R.id.fbcd_ll_page2);
	    page2Group = (RadioGroup) res.findViewById(R.id.fbcd_rg_color);
	    page2GreenButton = (RadioButton) res.findViewById(R.id.fbcd_rb_green);
	    page2GreenButton.setOnClickListener(this);
	    page2BlueButton = (RadioButton) res.findViewById(R.id.fbcd_rb_blue);
	    page2BlueButton.setOnClickListener(this);
	    page2RedButton = (RadioButton) res.findViewById(R.id.fbcd_rb_red);
	    page2RedButton.setOnClickListener(this);
	    page2YellowButton = (RadioButton) res.findViewById(R.id.fbcd_rb_yellow);
	    page2YellowButton.setOnClickListener(this);
	    page2Subject = (EditText) res.findViewById(R.id.fbcd_et_subject);
	    page2Subject.addTextChangedListener(new TextWatcher() {			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enteredSubject = page2Subject.getText().toString();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	    page2BtnSave = (Button) res.findViewById(R.id.fbcd_btn_save);
	    page2BtnSave.setOnClickListener(this);
	    page2BtnCancel = (Button) res.findViewById(R.id.fbcd_btn_cancel);
	    page2BtnCancel.setOnClickListener(this);
	    pages = new View[]{page0Overlay,page1Question,page2Edit};
	    if(isInitialized()){
	    	updateView();
	    }else{
	    	task = new MergeCheckTask();
	    	task.execute();
	    }
		return res;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {		
		super.onSaveInstanceState(outState);
		outState.putInt(SAVED_INST_PAGE_KEY, page);
		outState.putString(SAVED_INST_SUBJECT_KEY, enteredSubject);		
		outState.putInt(SAVED_INST_TYPE_KEY, chosenType);
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
	
	@Override
	public void onClick(View v) {		
		final int id = v.getId();
		switch(id){
		case R.id.fbcd_btn_yes:
			changePage(2);
			break;
		case R.id.fbcd_btn_no:
			dismiss();
			break;
		case R.id.fbcd_btn_save:
			if(validateInput()){
				final Note.Builder b = new Note.Builder();
				for(LocationPart p : location){
					b.withLocationPart(p);
				}
				b.withLocalNoteId(UUID.randomUUID().toString())
				.withSubject(enteredSubject)
				.withLocalUserId(localUserId)
				.withHandbookId(handbookId)
				.withModuleId(moduleId)
				.withPageId(pageId)
				.withType(chosenType);
				final SwipeReaderActivity act = (SwipeReaderActivity)getActivity();
				act.addNote(b.build(), notesToMerge);
				dismiss();
			}
			break;
		case R.id.fbcd_btn_cancel:
			dismiss();
			break;
		case R.id.fbcd_rb_green:
			chosenType = 0;			
			break;
		case R.id.fbcd_rb_blue:
			chosenType = 1;			
			break;
		case R.id.fbcd_rb_red:
			chosenType = 2;			
			break;
		case R.id.fbcd_rb_yellow:
			chosenType = 3;			
			break;			
		}
	}
	
	private boolean isInitialized(){
		return page>0;
	}
	
	private void updateView(){		
		switch(chosenType){
		case 0:
			page2Group.check(R.id.fbcd_rb_green);
			break;
		case 1:
			page2Group.check(R.id.fbcd_rb_blue);		
			break;			
		case 2:
			page2Group.check(R.id.fbcd_rb_red);
			break;
		case 3:
			page2Group.check(R.id.fbcd_rb_yellow);
			break;
		}
		page2Subject.setText(enteredSubject);
		if(pages[page].getVisibility()!=View.VISIBLE){
			for(int i=0;i<pages.length;i++){
				pages[i].setVisibility(page==i?View.VISIBLE:View.GONE);
			}
		}		
	}
	
	private void changePage(int page){
		this.page = page;
		updateView();
	}
	
	private boolean validateInput(){
		boolean res = true;
		if(page2Subject.getText().toString().trim().length()==0){
			page2Subject.setError("Zakładka musi mieć tytuł");
			res = false;
		}
		return res;
	}
	
	class MergeCheckTask extends AsyncTask<Void,Void,Boolean>{
				
		@Override
		protected Boolean doInBackground(Void... params) {
			if(notesToMerge!=null){
				final Context ctx = getActivity();
				if(ctx!=null){
					boolean containsNotes = false;
					for(String localNoteId : notesToMerge){
						Uri u = NotesProvider.UriHelper.noteByLocalNoteId(localNoteId);
						Cursor c = ctx.getContentResolver().query(u, NotesTable.COLUMNS, null, null, null);						
						if(c!=null){							
							if(c.moveToFirst()){
								final Note n = NotesUtil.getNoteBuilderFromCursor(c, c.getPosition()).build();								
								containsNotes|=!n.isBookmark();
							}
							c.close();
						}
					}
					return containsNotes;
				}
			}
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if(result){
				changePage(1);
			}else{
				changePage(2);
			}
		}				
	}	

}
