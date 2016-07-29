package pl.epodreczniki.fragment;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.util.NotesUtil;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RetainedNotesFragment extends Fragment{	
	
	public static final String TAG = "retained_notes_fragment";
	
	private volatile NotesEnabled noteActivity;
	
	private AddNoteTask addTask;
	
	private DeleteNoteTask deleteTask;
	
	private UpdateNoteTask updateTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return null;
	}	
	
	@Override
	public void onAttach(Activity activity) {	
		super.onAttach(activity);
		noteActivity = (NotesEnabled) activity;
	}
	
	@Override
	public void onDetach() {			
		noteActivity = null;
		if(addTask!=null){
			addTask.cancel(true);
			addTask=null;
		}
		if(deleteTask!=null){
			deleteTask.cancel(true);
			deleteTask=null;
		}
		if(updateTask!=null){
			updateTask.cancel(true);
			updateTask=null;
		}
		super.onDetach();
	}
	
	public void addNote(Note note, String[] notesToMerge){
		if(addTask!=null){
			addTask.cancel(true);
		}
		addTask = new AddNoteTask(note, notesToMerge);
		addTask.execute();
	}
	
	public void deleteNote(Note note){		
		if(deleteTask!=null){
			deleteTask.cancel(true);			
		}
		deleteTask = new DeleteNoteTask(note);
		deleteTask.execute();
	}
	
	public void updateNote(Note note){
		if(updateTask!=null){
			updateTask.cancel(true);
		}
		updateTask = new UpdateNoteTask(note);
		updateTask.execute();
	}
	
	public interface NotesEnabled{	
		
		ContentResolver getResolver();
		
		void showOverlay(boolean show);
		
		void onNoteAdd(String pageId, Note note, String[] notesToMerge, boolean success);
		
		void onNoteDelete(String pageId, String localNoteId, boolean success);			
		
		void onNoteUpdate(String pageId, Note note, boolean success);
		
	}
	
	private class AddNoteTask extends AsyncTask<Void,Void,Boolean>{
		
		private final Note note;
		
		private final String[] notesToMerge;
		
		private AddNoteTask(Note note, String[] notesToMerge){
			this.note = note;
			this.notesToMerge = notesToMerge;
		}
		
		@Override
		protected void onPreExecute() {		
			if(noteActivity!=null){
				noteActivity.showOverlay(true);
			}
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if(noteActivity!=null){
				final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();			
				if(notesToMerge!=null){
					for(String localNoteId : notesToMerge){
						ops.add(ContentProviderOperation.newDelete(NotesProvider.UriHelper.noteByLocalNoteId(localNoteId)).build());
					}
				}
				ops.add(ContentProviderOperation.newInsert(NotesProvider.UriHelper.NOTES_URI)
						.withValue(NotesTable.C_LOCAL_NOTE_ID, note.getLocalNoteId())
						.withValue(NotesTable.C_LOCAL_USER_ID, note.getLocalUserId())
						.withValue(NotesTable.C_NOTE_ID, note.getNoteId())
						.withValue(NotesTable.C_USER_ID, note.getUserId())
						.withValue(NotesTable.C_HANDBOOK_ID,note.getHandbookId())
						.withValue(NotesTable.C_MODULE_ID, note.getModuleId())
						.withValue(NotesTable.C_PAGE_ID, note.getPageId())
						.withValue(NotesTable.C_LOCATION, NotesUtil.noteLocationToJsonString(note.getLocation()))
						.withValue(NotesTable.C_SUBJECT, note.getSubject())
						.withValue(NotesTable.C_VALUE, note.getValue())
						.withValue(NotesTable.C_TYPE, note.getType())
						.withValue(NotesTable.C_ACCEPTED, note.getAccepted()!=null&&note.getAccepted()?"1":"0")
						.withValue(NotesTable.C_REFERENCE_TO, note.getReferenceTo())
						.withValue(NotesTable.C_REFERENCED_BY, note.getReferencedBy())
						.withValue(NotesTable.C_MODIFY_TIME, GregorianCalendar.getInstance().getTime().toString())
						.build()
						);
				try{
					noteActivity.getResolver().applyBatch(NotesProvider.UriHelper.AUTHORITY, ops);
					return true;
				}catch(Exception e){
					return false;
				}				
			}
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			boolean res = result!=null&&result;
			if(noteActivity!=null){
				noteActivity.onNoteAdd(note.getPageId(), note, notesToMerge, res);
			}
		}
		
		@Override
		protected void onCancelled(Boolean result) {		
			if(noteActivity!=null){
				noteActivity.showOverlay(false);
			}
		}
		
	}
	
	private class DeleteNoteTask extends AsyncTask<Void,Void,Boolean>{		
		
		private final Note note;				
		
		private DeleteNoteTask(Note note){			
			this.note = note;
		}
		
		@Override
		protected void onPreExecute() {
			if(noteActivity!=null){
				noteActivity.showOverlay(true);
			}
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if(noteActivity!=null){
				int rowsDeleted = noteActivity.getResolver().delete(NotesProvider.UriHelper.noteByLocalNoteId(note.getLocalNoteId()), null, null);
				return rowsDeleted==1;
			}
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			boolean res = result!=null&&result;
			if(noteActivity!=null){
				noteActivity.onNoteDelete(note.getPageId(), note.getLocalNoteId(), res);
			}
		}
		
		@Override
		protected void onCancelled(Boolean result) {		
			if(noteActivity!=null){
				noteActivity.showOverlay(false);
			}
		}
		
	}
	
	private class UpdateNoteTask extends AsyncTask<Void,Void,Boolean>{				
		
		private final Note note;
		
		private UpdateNoteTask(Note note){			
			this.note = note;
		}
		
		@Override
		protected void onPreExecute() {
			if(noteActivity!=null){
				noteActivity.showOverlay(true);
			}
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if(noteActivity!=null){
				final ContentValues vals = NotesUtil.noteToContentValues(note);
				final Uri uri = NotesProvider.UriHelper.noteByLocalNoteId(note.getLocalNoteId());
				int rowsUpdated = noteActivity.getResolver().update(uri, vals, null, null);
				return rowsUpdated==1;
			}			
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			final boolean res = result!=null&&result;
			if(noteActivity!=null){
				noteActivity.onNoteUpdate(note.getPageId(), note, res);
			}
		}
		
		@Override
		protected void onCancelled(Boolean result) {
			if(noteActivity!=null){
				noteActivity.showOverlay(false);
			}
		}
		
	}

}
