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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class BookmarkEditDialog extends DialogFragment implements OnClickListener{
	
	public static final String TAG = "bookmark.edit.dialog";
	
	private static final String ARGS_KEY_LOCAL_NOTE_ID = "bookmark.local.note.id.key";
	
	private static final String SAVED_PAGE = "saved.instance.page";
	
	private static final String SAVED_NOTE = "saved.instance.note";	
	
	private String localNoteId;
	
	private Note note;
	
	private View page0Overlay;
	
	private View page1Edit;
	
	private View page2Confirm;
	
	private View[] pages;
	
	private ImageButton page1Delete;
	
	private RadioGroup page1Color;
	
	private EditText page1Subject;
	
	private Button page1Save;
	
	private Button page1Cancel;
	
	private Button page2Yes;
	
	private Button page2No;
	
	private int page = 0;
	
	private InitTask task;
	
	public static BookmarkEditDialog getInstance(String localNoteId){
		final Bundle bundle = new Bundle();
		bundle.putString(ARGS_KEY_LOCAL_NOTE_ID, localNoteId);
		final BookmarkEditDialog res = new BookmarkEditDialog();
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
			page = savedInstanceState.getInt(SAVED_PAGE);
			note = savedInstanceState.getParcelable(SAVED_NOTE);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.f_bookmark_edit, container, false);
		page0Overlay = res.findViewById(R.id.fbed_ll_page0);		
		page1Edit = res.findViewById(R.id.fbed_ll_page1);
		page1Color = (RadioGroup) res.findViewById(R.id.fbed_rg_color);
		page1Subject = (EditText) res.findViewById(R.id.fbed_et_subject);
		page1Color = (RadioGroup) res.findViewById(R.id.fbed_rg_color);
		page1Delete = (ImageButton) res.findViewById(R.id.fbed_ib_delete);
		page1Delete.setOnClickListener(this);
		page1Save = (Button) res.findViewById(R.id.fbed_btn_save);
		page1Save.setOnClickListener(this);
		page1Cancel = (Button) res.findViewById(R.id.fbed_btn_cancel);
		page1Cancel.setOnClickListener(this);
		page2Confirm = res.findViewById(R.id.fbed_ll_page2);
		page2Yes = (Button) res.findViewById(R.id.fbed_btn_yes);
		page2Yes.setOnClickListener(this);
		page2No = (Button) res.findViewById(R.id.fbed_btn_no);
		page2No.setOnClickListener(this);
		pages = new View[]{page0Overlay,page1Edit,page2Confirm};
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
		outState.putInt(SAVED_PAGE, page);
		outState.putParcelable(SAVED_NOTE, note);
	}
	
	@Override
	public void onClick(View v) {
		final int id = v.getId();
		final SwipeReaderActivity activity = (SwipeReaderActivity) getActivity();
		switch(id){
		case R.id.fbed_btn_save:
			if(activity!=null){
				if(validateInput()){
					final Note.Builder bldr = Note.Builder.fromNote(note);
					bldr.withSubject(page1Subject.getText().toString());
					switch(page1Color.getCheckedRadioButtonId()){
					case R.id.fbed_rb_green:
						bldr.withType(0);
						break;
					case R.id.fbed_rb_blue:
						bldr.withType(1);
						break;
					case R.id.fbed_rb_red:
						bldr.withType(2);
						break;
					case R.id.fbed_rb_yellow:
						bldr.withType(3);
						break;
					}
					activity.updateNote(bldr.build());
					dismiss();
				}
			}
		case R.id.fbed_btn_cancel:
			dismiss();
			break;
		case R.id.fbed_btn_yes:
			if(activity!=null){
				activity.deleteNote(note);
				dismiss();
			}
			break;
		case R.id.fbed_btn_no:
			changePage(1);
			break;
		case R.id.fbed_ib_delete:
			changePage(2);
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
		if(note!=null){
			page1Subject.setText(note.getSubject());
			switch(note.getType()){
			case 0:
				page1Color.check(R.id.fbed_rb_green);
				break;
			case 1:
				page1Color.check(R.id.fbed_rb_blue);
				break;
			case 2:
				page1Color.check(R.id.fbed_rb_red);
				break;
			case 3:
				page1Color.check(R.id.fbed_rb_yellow);
				break;
			}
			if(pages[page].getVisibility()!=View.VISIBLE){
				for(int i=0; i<pages.length; i++){
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
		if(page1Subject.getText().toString().trim().length()==0){
			page1Subject.setError("Zakładka musi mieć tytuł");
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
			super.onPostExecute(result);
			if(result!=null){
				note = result;
				changePage(1);
			}else{
				Toast.makeText(getActivity(), "Nie ma takiej zakładki", Toast.LENGTH_SHORT).show();
				dismiss();
			}
		}
		
	}
	
}
