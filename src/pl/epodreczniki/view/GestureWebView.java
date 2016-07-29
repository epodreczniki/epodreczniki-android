package pl.epodreczniki.view;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.SwipeReaderActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ActionMode.Callback;
import android.webkit.WebView;

public class GestureWebView extends WebView{
	
	private static final String JS_FUNCTION_START_ADD_NOTE = "javascript:(function(){startAddNote(true)})();";
	
	private static final String JS_FUNCTION_COPY_TO_CLIPBOARD = "javascript:(function(){copyToClipboard();})();";

	private GestureDetector detector;
	
	private ActionMode mActionMode;
	
	public GestureWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public GestureWebView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	
	@Override
	public ActionMode startActionMode(Callback callback) {
		if(mActionMode == null){
			mActionMode = super.startActionMode(new NotesCallback());
		}
		return mActionMode;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {	
		return detector==null||detector.onTouchEvent(event)||super.onTouchEvent(event);
	}
	
	public void setGestureDetector(GestureDetector detector){
		this.detector = detector;
	}	
	
	class NotesCallback implements ActionMode.Callback{

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.notes, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()){
			case R.id.m_notes_add:
				((SwipeReaderActivity)getContext()).setBookmarkFlag(false);
				GestureWebView.this.loadUrl(JS_FUNCTION_START_ADD_NOTE);
				mode.finish();
				return true;	
			case R.id.m_notes_add_bookmark:
				((SwipeReaderActivity)getContext()).setBookmarkFlag(true);
				GestureWebView.this.loadUrl(JS_FUNCTION_START_ADD_NOTE);
				mode.finish();
				return true;
			case R.id.m_notes_copy:
				GestureWebView.this.loadUrl(JS_FUNCTION_COPY_TO_CLIPBOARD);
				mode.finish();
				return true;
			default:
				mode.finish();
			}			
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			clearFocus();
			mActionMode = null;
		}
		
	}

}
