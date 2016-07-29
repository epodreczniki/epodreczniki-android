package pl.epodreczniki.fragment;

import java.util.ArrayList;
import java.util.List;

import pl.epodreczniki.R;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.model.Navigable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.util.NotesUtil;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class NoteListFragment extends Fragment {

	public static final String TAG = "note_list_fragment";

	public static final String ARGS_LOCAL_USER_ID = "args.local.user.id";
	
	public static final String ARGS_MD_CONTENT_ID = "args.md.content.id";
	
	public static final String ARGS_MD_VERSION = "args.md.version";
	
	public static final String ARGS_MODE = "args.mode";	
	
	public static final int MODE_USER = 0;
	
	public static final int MODE_USER_BOOK = 1;

	private String mdContentId;
	
	private int mdVersion;

	private long localUserId;
	
	private int mode;

	private Navigable navigationActivity;

	private ListView noteList;

	private View noData;
	
	private NotesAdapter adapter;

	public static NoteListFragment newInstance(long localUserId, String mdContentId, int mdVersion, int mode) {
		final Bundle b = new Bundle();
		b.putLong(ARGS_LOCAL_USER_ID, localUserId);
		b.putString(ARGS_MD_CONTENT_ID, mdContentId);
		b.putInt(ARGS_MD_VERSION, mdVersion);
		b.putInt(ARGS_MODE, mode);
		final NoteListFragment res = new NoteListFragment();
		res.setArguments(b);
		return res;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		navigationActivity = (Navigable) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			localUserId = getArguments().getLong(ARGS_LOCAL_USER_ID);
			mdContentId = getArguments().getString(ARGS_MD_CONTENT_ID);
			mdVersion = getArguments().getInt(ARGS_MD_VERSION);
			mode = getArguments().getInt(ARGS_MODE);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.f_note_list, container,
				false);
		noteList = (ListView) res.findViewById(R.id.fnl_lv_notes);
		noData = res.findViewById(R.id.fnl_tv_no_data);
		return res;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new NotesAdapter(this);
		noteList.setAdapter(adapter);
		getLoaderManager().initLoader(0, null, new LoaderListener(this));
	}

	@Override
	public void onDetach() {
		navigationActivity = null;
		super.onDetach();
	}
	
	private void onNotesUpdate(List<Note> notes){
		if(notes.size()==0){
			noteList.setVisibility(View.GONE);
			noData.setVisibility(View.VISIBLE);
		}else{
			noteList.setVisibility(View.VISIBLE);
			noData.setVisibility(View.GONE);
		}
		adapter.updateNotes(notes);
	}
	
	private void navigate(Note n){
		navigationActivity.showPage(n.getPageId(), NotesUtil.getMdContentIdFromHandbookId(n.getHandbookId()), NotesUtil.getMdVersionFromHandbookId(n.getHandbookId()),n.getLocalNoteId());
	}
	
	private void showContent(Note n){
		navigationActivity.showNoteContent(n);
	}

	static class LoaderListener implements LoaderCallbacks<Cursor> {

		private final NoteListFragment nlf;
		
		private Cursor data;

		LoaderListener(NoteListFragment nlf) {
			this.nlf = nlf;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Uri uri;
			if(nlf.mode==MODE_USER_BOOK){
				uri = NotesProvider.UriHelper.noteByLocalUserBook(
						nlf.localUserId, nlf.mdContentId,
						nlf.mdVersion);
			}else if(nlf.mode==MODE_USER){
				uri = NotesProvider.UriHelper.noteByLocalUser(nlf.localUserId);				
			}else{
				uri = NotesProvider.UriHelper.NOTES_URI;
			}			
			return new CursorLoader(nlf.getActivity(), uri, NotesTable.COLUMNS, null, null,
					NotesTable.C_HANDBOOK_ID + " ASC, " + NotesTable.C_PAGE_IDX
							+ " ASC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			if (data != null) {
				this.data = data;
				final List<Note> res = new ArrayList<Note>();
				while(data.moveToNext()){
					Note.Builder b = NotesUtil.getNoteBuilderFromCursor(data,data.getPosition());
					if(b!=null){
						res.add(b.build());
					}					
				}
				data.moveToPosition(-1);
				nlf.onNotesUpdate(res);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			if(data!=null){
				data.close();
				data = null;
			}
		}

	}
	
	private static class NotesAdapter extends BaseAdapter{

		private NoteListFragment nlf;
		
		private List<Note> data;
		
		private NotesAdapter(NoteListFragment nlf){
			this.nlf = nlf;
		}
		
		@Override
		public int getCount() {
			return data==null?0:data.size();
		}

		@Override
		public Note getItem(int position) {			
			return data!=null&&data.size()>position?data.get(position):null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Note note = getItem(position);
			View color;
			TextView subject;
			TextView value;
			ImageButton showContent;
			if(convertView == null){
				final LayoutInflater inflater = LayoutInflater.from(nlf.getActivity());
				convertView = inflater.inflate(R.layout.v_note_list_item, parent, false);
				color = convertView.findViewById(R.id.vnli_color);
				subject = (TextView) convertView.findViewById(R.id.vnli_subject);
				value = (TextView) convertView.findViewById(R.id.vnli_value);
				showContent = (ImageButton) convertView.findViewById(R.id.vnli_btn_show_content);
				convertView.setTag(R.id.vnli_color, color);
				convertView.setTag(R.id.vnli_subject, subject);
				convertView.setTag(R.id.vnli_value, value);				
				convertView.setTag(R.id.vnli_btn_show_content,showContent);
			}else{
				color = (View) convertView.getTag(R.id.vnli_color);
				subject = (TextView) convertView.getTag(R.id.vnli_subject);
				value= (TextView) convertView.getTag(R.id.vnli_value);
				showContent = (ImageButton) convertView.getTag(R.id.vnli_btn_show_content);
			}						
			color.setBackgroundColor(nlf.getResources().getColor(NotesUtil.getColorIdForNoteType(note.getType())));
			subject.setText(note.getSubject());
			value.setText(note.getValue());
			showContent.setVisibility(note.isBookmark()?View.GONE:View.VISIBLE);
			convertView.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					nlf.navigate(note);				
				}
			});
			showContent.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					nlf.showContent(note);
				}
			});
			convertView.setBackgroundColor(position%2!=0?0x10101010:0x00000000);
			return convertView;
		}
		
		public void updateNotes(List<Note> notes){
			data = notes;
			notifyDataSetChanged();
		}
		
	}

}
