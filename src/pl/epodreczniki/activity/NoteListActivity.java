package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import pl.epodreczniki.fragment.NoteListFragment;
import pl.epodreczniki.fragment.NoteShowContentDialog;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.Navigable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.util.UserContext;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

public class NoteListActivity extends AbstractUserAwareActivity implements Navigable{
	
	public static final String EXTRA_BOOK = "nla_extra_book";
	
	private Book book;
	
	private NoteListFragment noteListFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_note_list);
		if(!checkUserLoggedIn()){
			return;
		}
		final Bundle extras = getIntent().getExtras();
		if(extras==null || (book = extras.getParcelable(EXTRA_BOOK)) == null){
			finish();
			return;
		}					
		if(savedInstanceState == null){
			final Bundle noteListBundle = new Bundle();			
			noteListBundle.putString(NoteListFragment.ARGS_MD_CONTENT_ID, book.getMdContentId());
			noteListBundle.putInt(NoteListFragment.ARGS_MD_VERSION, book.getMdVersion());
			noteListBundle.putLong(NoteListFragment.ARGS_LOCAL_USER_ID, UserContext.getCurrentUser().getLocalUserId());
			noteListBundle.putInt(NoteListFragment.ARGS_MODE, NoteListFragment.MODE_USER_BOOK);
			noteListFragment = new NoteListFragment();
			noteListFragment.setArguments(noteListBundle);
			getFragmentManager().beginTransaction().add(R.id.anl_rl_root, noteListFragment).commit();
		}else{
			noteListFragment = (NoteListFragment) getFragmentManager().findFragmentById(R.id.anl_rl_root);
		}
	}

	@Override
	public void showPage(String pageId, String mdContentId, Integer mdVersion) {
		final Intent i = new Intent(this,SwipeReaderActivity.class);		
		i.putExtra(SwipeReaderActivity.EXTRA_BOOK, book);	
		i.putExtra(SwipeReaderActivity.EXTRA_PAGE_ID, pageId);							
		startActivity(i);
		finish();
	}
	
	@Override
	public void showPage(String pageId, String mdContentId, Integer mdVersion, String noteId) {
		final Intent i = new Intent(this,SwipeReaderActivity.class);
		i.putExtra(SwipeReaderActivity.EXTRA_BOOK,  book);
		i.putExtra(SwipeReaderActivity.EXTRA_PAGE_ID, pageId);
		i.putExtra(SwipeReaderActivity.EXTRA_NOTE_ID, noteId);
		startActivity(i);
		finish();	
	}
	
	@Override
	public void showNoteContent(Note note) {
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(NoteShowContentDialog.TAG);
		if(prev!=null){
			ft.remove(prev);			
		}
		ft.addToBackStack(null);
		ft.commit();
		final NoteShowContentDialog dial = NoteShowContentDialog.newInstance(note);
		dial.setStyle(DialogFragment.STYLE_NO_TITLE,R.style.WhiteDialog);
		dial.show(getFragmentManager(), NoteShowContentDialog.TAG);
	}
	
	@Override
	public void onTocDetached() {
	}	
	
}
