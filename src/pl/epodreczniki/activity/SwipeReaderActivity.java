package pl.epodreczniki.activity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import pl.epodreczniki.R;
import pl.epodreczniki.activity.SwipeReaderActivity.ControlsAnimation.HideDirection;
import pl.epodreczniki.db.ExerciseStatesProvider;
import pl.epodreczniki.db.ExerciseStatesTable;
import pl.epodreczniki.db.NotesProvider;
import pl.epodreczniki.db.NotesTable;
import pl.epodreczniki.db.UsersProvider;
import pl.epodreczniki.db.UsersTable;
import pl.epodreczniki.fragment.BookmarkCreateDialog;
import pl.epodreczniki.fragment.BookmarkEditDialog;
import pl.epodreczniki.fragment.NoteCreateDialog;
import pl.epodreczniki.fragment.NoteListFragment;
import pl.epodreczniki.fragment.NoteViewEditDialog;
import pl.epodreczniki.fragment.RetainedNotesFragment;
import pl.epodreczniki.fragment.RetainedNotesFragment.NotesEnabled;
import pl.epodreczniki.fragment.RetainedTocFragment;
import pl.epodreczniki.fragment.RetainedTocFragment.TocEnabled;
import pl.epodreczniki.fragment.RetainedTocFragment.TocError;
import pl.epodreczniki.fragment.TocFragment;
import pl.epodreczniki.model.Book;
import pl.epodreczniki.model.ExerciseState;
import pl.epodreczniki.model.Navigable;
import pl.epodreczniki.model.Note;
import pl.epodreczniki.model.Page;
import pl.epodreczniki.model.Settings;
import pl.epodreczniki.model.Toc;
import pl.epodreczniki.model.User;
import pl.epodreczniki.model.Note.LocationPart;
import pl.epodreczniki.util.NotesUtil;
import pl.epodreczniki.util.UserContext;
import pl.epodreczniki.util.Util;
import pl.epodreczniki.view.DisableableViewPager;
import pl.epodreczniki.view.GestureWebView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class SwipeReaderActivity extends AbstractUserAwareActivity implements TocEnabled, Navigable, NotesEnabled {

	private static final String JS_FUNCTION_DECREASE_SIZE = "javascript:(function(){decreaseSize();})();";

	private static final String JS_FUNCTION_UPDATE_SIZE = "javascript:(function(){updateSize();})();";

	private static final String JS_FUNCTION_INCREASE_SIZE = "javascript:(function(){increaseSize();})();";

	private static final String JS_FUNCTION_NOTE_CREATE_CALLBACK_TMPL = "javascript:(function(){noteCreateCallback(%s,%s);})();";

	private static final String JS_FUNCTION_NOTE_DELETE_CALLBACK_TMPL = "javascript:(function(){noteDeleteCallback('%s');})();";

	private static final String JS_FUNCTION_NOTE_EDIT_CALLBACK_TMPL = "javascript:(function(){noteEditCallback(%s);})();";

	private static final String JS_FUNCTION_SHOW_NOTES_TMPL = "javascript:(function(){showNotes(%s)})();";

	private static final String JS_FUNCTION_STOP_PLAYBACK = "javascript:(function(){stopPlayback();})();";

	private static final String JS_FUNCTION_UPDATE_WOMI_STATE_TMPL = "javascript:(function(){updateWomiState('%s');})();";

	private static final String JS_FUNCTION_UPDATE_WOMI_NULL_STATE = "javascript:(function(){updateWomiState()})();";
	
	private static final String JS_FUNCTION_UPDATE_OQ_STATES_TMPL = "javascript:(function(){updateOpenQuestionsStates('%s')})();";
	
	private static final String JS_FUNCTION_UPDATE_OQ_NULL_STATES = "javascript:(function(){updateOpenQuestionsStates({})})();";

	public static final String EXTRA_BOOK = "pl.epodreczniki.swipereader.book";

	public static final String EXTRA_PAGE_ID = "pl.epodreczniki.swipereader.pageid";

	public static final String EXTRA_NOTE_ID = "pl.epodreczniki.swipereader.noteid";

	public static final String EXTRA_DEV_TIMESTAMP = "pl.epodreczniki.swipereader.timestamp";

	private static final String JS_INTEFACE_NAME = "Android";

	private Book book;

	private String initialPageId = null;

	private String timestamp;

	private boolean isTeacher;

	private Toc toc;

	private DrawerLayout drawer;

	private ActionBarDrawerToggle toggle;

	private RetainedTocFragment retainedTocFragment;

	private RetainedNotesFragment retainedNotesFragment;

	private TocFragment tocFragment;

	private FragmentTabHost mTabHost;

	private DisableableViewPager pager;

	private WebPagerAdapter adapter;

	private WebView currentWebView;

	private List<WebView> webViews = new ArrayList<WebView>();

	private View topBar;

	private TextView tvTitle;

	private ImageButton prevBtn;

	private ImageButton nextBtn;

	private ImageButton aPlusBtn;

	private ImageButton aMinusBtn;

	private ImageButton tocBtn;

	private ImageButton hideBtn;

	private ImageButton backBtn;

	private View overlay;

	private String jumpToNote;

	private ControlsAnimation ctlAnim;

	private Animation btnAnim;

	private volatile boolean isModalVisible = false;

	private volatile boolean bookmarkFlag = false;

	private ShowEditDialogTask editDialogTask;

	private QueryWomiStateTask queryWomiStateTask;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_swipe_reader);
		Log.e("SRA", "onCreate");
		book = getIntent().getParcelableExtra(EXTRA_BOOK);
		initialPageId = getIntent().getStringExtra(EXTRA_PAGE_ID);
		timestamp = getIntent().getStringExtra(EXTRA_DEV_TIMESTAMP);
		jumpToNote = getIntent().getStringExtra(EXTRA_NOTE_ID);		
		isTeacher = Util.isTeacher(this);
		pager = (DisableableViewPager) findViewById(R.id.asr_pager);
		pager.setOffscreenPageLimit(1);
		adapter = new WebPagerAdapter();		
		if (!checkUserLoggedIn()) {
			return;
		}		
		pager.setAdapter(adapter);
		topBar = findViewById(R.id.asr_top_bar);
		tvTitle = (TextView) findViewById(R.id.asr_title);
		if(!Util.isTablet(this)){
			tvTitle.setVisibility(View.INVISIBLE);
		}
		if (book != null) {
			tvTitle.setText(book.getMdTitle());
		}
		prevBtn = (ImageButton) findViewById(R.id.asr_btn_prev);
		prevBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pager.setCurrentItem(pager.getCurrentItem() - 1, true);
			}
		});
		nextBtn = (ImageButton) findViewById(R.id.asr_btn_next);
		nextBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pager.setCurrentItem(pager.getCurrentItem() + 1, true);
			}
		});
		hideBtn = (ImageButton) findViewById(R.id.asr_btn_hide);
		hideBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!ctlAnim.hideAll.isRunning() && !ctlAnim.showAll.isRunning()) {
					ctlAnim.setHideValues(0);
					ctlAnim.hideAll.start();
				}
			}
		});
		backBtn = (ImageButton) findViewById(R.id.asr_btn_back);
		backBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		aPlusBtn = (ImageButton) findViewById(R.id.asr_btn_a_plus);
		aPlusBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentWebView != null) {
					currentWebView.loadUrl(JS_FUNCTION_INCREASE_SIZE);
					for (WebView web : webViews) {
						if (web != currentWebView) {
							web.loadUrl(JS_FUNCTION_UPDATE_SIZE);
						}
					}
				}
			}
		});
		aMinusBtn = (ImageButton) findViewById(R.id.asr_btn_a_minus);
		aMinusBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentWebView != null) {
					currentWebView.loadUrl(JS_FUNCTION_DECREASE_SIZE);
					for (WebView web : webViews) {
						if (web != currentWebView) {
							web.loadUrl(JS_FUNCTION_UPDATE_SIZE);
						}
					}
				}
			}
		});
		tocBtn = (ImageButton) findViewById(R.id.asr_btn_toc);
		tocBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (drawer.isDrawerOpen(Gravity.RIGHT)) {
					drawer.closeDrawer(Gravity.RIGHT);
				} else {
					drawer.openDrawer(Gravity.RIGHT);
				}
			}
		});
		overlay = findViewById(R.id.asr_overlay);
		ControlsAnimation.Builder b = new ControlsAnimation.Builder();
		b.withViewAndDirection(topBar, HideDirection.UP);
		b.withViewAndDirection(prevBtn, HideDirection.LEFT);
		b.withViewAndDirection(nextBtn, HideDirection.RIGHT);
		ctlAnim = b.build();
		btnAnim = AnimationUtils.loadAnimation(this, R.anim.back_button_anim);
		final FragmentManager fm = getFragmentManager();
		mTabHost = (FragmentTabHost) findViewById(R.id.asr_tab_host);
		mTabHost.setup(this, fm, R.id.asr_tab_content);
		mTabHost.addTab(mTabHost.newTabSpec("toc").setIndicator("Spis treści", null), TocFragment.class, null);
		final Bundle noteListBundle = new Bundle();
		noteListBundle.putString(NoteListFragment.ARGS_MD_CONTENT_ID, getMdContentId());
		noteListBundle.putInt(NoteListFragment.ARGS_MD_VERSION, getMdVersion());
		noteListBundle.putInt(NoteListFragment.ARGS_MODE, NoteListFragment.MODE_USER_BOOK);
		noteListBundle.putLong(NoteListFragment.ARGS_LOCAL_USER_ID, UserContext.getCurrentUser().getLocalUserId());
		mTabHost.addTab(mTabHost.newTabSpec("notes").setIndicator("Notatki", null), NoteListFragment.class,
				noteListBundle);
		for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
			final TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
			if (tv != null) {
				tv.setTextColor(0xFF000000);
			}
		}		
		retainedTocFragment = (RetainedTocFragment) fm.findFragmentByTag(RetainedTocFragment.TAG);
		if (retainedTocFragment == null) {
			retainedTocFragment = new RetainedTocFragment();
			fm.beginTransaction().add(retainedTocFragment, RetainedTocFragment.TAG).commit();
		}
		retainedNotesFragment = (RetainedNotesFragment) fm.findFragmentByTag(RetainedNotesFragment.TAG);
		if (retainedNotesFragment == null) {
			retainedNotesFragment = new RetainedNotesFragment();
			fm.beginTransaction().add(retainedNotesFragment, RetainedNotesFragment.TAG).commit();
		}
		drawer = (DrawerLayout) findViewById(R.id.asr_drawer_layout);
		drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		toggle = new ActionBarDrawerToggle(this, drawer, R.drawable.toc, R.string.content_desc_drawer_open,
				R.string.content_desc_drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			}

		};
		drawer.setDrawerListener(toggle);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
			Util.isDev(this)){
			WebView.setWebContentsDebuggingEnabled(true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		for (WebView w : webViews) {
			w.loadUrl(JS_FUNCTION_STOP_PLAYBACK);
		}
		if (editDialogTask != null) {
			editDialogTask.cancel(true);
			editDialogTask = null;
		}
		if (queryWomiStateTask != null) {
			queryWomiStateTask.cancel(true);
			queryWomiStateTask = null;
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);	
		if (fragment instanceof TocFragment) {
			Log.e("SRA", "TocFragment");
			tocFragment = (TocFragment) fragment;
		}
	}

	@Override
	public void onTocDetached() {
		tocFragment = null;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		toggle.syncState();
	}

	@Override
	public void onBackPressed() {
		if (isModalVisible) {
			currentWebView.loadUrl("javascript:(function(){closeWindow();})();");
		} else {
			if (drawer.isDrawerOpen(Gravity.RIGHT)) {
				drawer.closeDrawer(Gravity.RIGHT);
			}
			backBtn.startAnimation(btnAnim);
		}
	}

	private void storeCurrentPage(int globalIdx) {		
		if(checkUserLoggedIn()){
			Util.storeLastPage(this, UserContext.getCurrentUser().getLocalUserId(), toc.getTocKey(), globalIdx);	
		}
		
		tocFragment.setHighlight(globalIdx);
	}	

	@Override
	public void showPage(String pageId, String mdContentId, Integer mdVersion) {
		int global = toc.getPageIndexById(pageId);
		int display = toc.getPageDisplayIndex(global, isTeacher);
		pager.setCurrentItem(display, false);
		storeCurrentPage(global);
		if (drawer.isDrawerOpen(Gravity.RIGHT)) {
			drawer.closeDrawer(Gravity.RIGHT);
		}
	}

	@Override
	public void showPage(String pageId, String mdContentId, Integer mdVersion, String noteId) {	
		int prevPage = pager.getCurrentItem();
		int global = toc.getPageIndexById(pageId);
		int display = toc.getPageDisplayIndex(global, isTeacher);
		final WebView w = getWebViewByPageId(pageId);
		pager.setCurrentItem(display, false);
		if (prevPage == display) {
			Log.e("SRA", "same page");
			w.loadUrl(String.format("javascript:(function(){jumpToNote('%s')})()", noteId));
		} else {
			if (w != null) {
				Log.e("SRA", "neighboring page");
				w.loadUrl(String.format("javascript:(function(){jumpToNote('%s')})()", noteId));
			} else {
				Log.e("SRA", "reload");
				jumpToNote = noteId;
			}
		}
		storeCurrentPage(global);
		if (drawer.isDrawerOpen(Gravity.RIGHT)) {
			drawer.closeDrawer(Gravity.RIGHT);
		}
	}

	@Override
	public void showNoteContent(Note note) {
		if (drawer.isDrawerOpen(Gravity.RIGHT)) {
			drawer.closeDrawer(Gravity.RIGHT);
		}
		showNoteEditDialog(note.getLocalNoteId());
	}

	@Override
	public String getMdContentId() {
		return Util.isDev(this) ? timestamp : book == null ? null : book.getMdContentId();
	}

	@Override
	public int getMdVersion() {
		return Util.isDev(this) ? 1 : book == null ? -1 : book.getMdVersion();
	}

	@Override
	public void onTocLoaded(Toc toc) {
		Log.e("SRA", "onTocLoaded");
		this.toc = toc;
		this.adapter.updatePages(toc.getPages(isTeacher));
		int global = 0;
		if (initialPageId != null) {
			global = toc.getPageIndexById(initialPageId);
			if (global == -1) {
				Toast.makeText(this, "Nie znaleziono strony", Toast.LENGTH_SHORT).show();
			}
			initialPageId = null;
		} else {
			if(checkUserLoggedIn()){
				global = Util.getLastPage(this,UserContext.getCurrentUser().getLocalUserId(),toc.getTocKey());
			}
		}
		final int display = toc.getPageDisplayIndex(global, isTeacher);
		if (display > -1) {
			Log.e("CURR_WEBVIEW", "maybe here also: " + display);
			this.pager.setCurrentItem(display, false);
			currentWebView = adapter.getCurrentWebView();
			Log.e("CURR_WEBVIEW", "!=null? " + (currentWebView != null));
		}
		tocFragment.setToc(toc,display);
	}

	@Override
	public void onTocError(TocError error) {
		Toast.makeText(this, "Nie udało się wczytać spisu treści", Toast.LENGTH_SHORT).show();
		finish();
	}

	@Override
	public ContentResolver getResolver() {
		return getContentResolver();
	}

	@Override
	public void showOverlay(boolean show) {
		if (overlay != null) {
			overlay.setVisibility(show ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void onNoteAdd(String pageId, Note note, String[] notesToMerge, boolean success) {
		final WebView web = getWebViewByPageId(pageId);
		if (web != null) {
			web.loadUrl(String.format(JS_FUNCTION_NOTE_CREATE_CALLBACK_TMPL, NotesUtil.noteToJsonString(note),
					NotesUtil.stringArrayToJson(notesToMerge)));
		}
		showOverlay(false);
	}

	@Override
	public void onNoteDelete(String pageId, String localNoteId, boolean success) {
		final WebView web = getWebViewByPageId(pageId);
		if (web != null) {
			if (success) {
				web.loadUrl(String.format(JS_FUNCTION_NOTE_DELETE_CALLBACK_TMPL, localNoteId));
			}
		}
		showOverlay(false);
	}

	@Override
	public void onNoteUpdate(String pageId, Note note, boolean success) {
		final WebView web = getWebViewByPageId(pageId);
		if (web != null) {
			if (success) {
				web.loadUrl(String.format(JS_FUNCTION_NOTE_EDIT_CALLBACK_TMPL, NotesUtil.noteToJsonString(note)));
			}
		}
		showOverlay(false);
	}

	private WebView getWebViewByPageId(String pageId) {
		for (WebView w : webViews) {
			final String pid = (String) w.getTag(R.id.webview_page_id);
			if (pid.equals(pageId)) {
				return w;
			}
		}
		return null;
	}

	private void showBookmarkCreateDialog(String pageId, LocationPart[] location, long localUserId, String handbookId,
			String moduleId, String[] notesToMerge, String bookmarkText) {
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(BookmarkCreateDialog.TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final BookmarkCreateDialog dial = BookmarkCreateDialog.newInstance(pageId, location, localUserId, handbookId,
				moduleId, notesToMerge, bookmarkText);
		dial.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.WhiteDialog);
		dial.show(getFragmentManager(), BookmarkCreateDialog.TAG);
	}

	private void showNoteCreateDialog(String pageId, LocationPart[] location, long localUserId, String handbookId,
			String moduleId, String[] notesToMerge, String noteText) {
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(NoteCreateDialog.TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final NoteCreateDialog dial = NoteCreateDialog.newInstance(pageId, location, localUserId, handbookId, moduleId,
				notesToMerge, noteText);
		dial.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.WhiteDialog);
		dial.show(getFragmentManager(), NoteCreateDialog.TAG);
	}

	private void showNoteEditDialog(String localNoteId) {
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(NoteViewEditDialog.TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final NoteViewEditDialog dial = NoteViewEditDialog.newInstance(localNoteId);
		dial.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.WhiteDialog);
		dial.show(getFragmentManager(), NoteViewEditDialog.TAG);
	}

	private void showBookmarkEditDialog(String localNoteId) {
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Fragment prev = getFragmentManager().findFragmentByTag(BookmarkEditDialog.TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		ft.commit();
		final BookmarkEditDialog dial = BookmarkEditDialog.getInstance(localNoteId);
		dial.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.WhiteDialog);
		dial.show(getFragmentManager(), BookmarkEditDialog.TAG);
	}

	private void startEditDialogTask(String localNoteId) {
		if (editDialogTask != null) {
			editDialogTask.cancel(true);
		}
		editDialogTask = new ShowEditDialogTask(this, localNoteId);
		editDialogTask.execute();
	}

	public void addNote(Note note, String[] notesToMerge) {
		retainedNotesFragment.addNote(note, notesToMerge);
	}

	public void updateNote(Note note) {
		retainedNotesFragment.updateNote(note);
	}

	public void deleteNote(Note note) {
		retainedNotesFragment.deleteNote(note);
	}

	public void setBookmarkFlag(boolean bookmarkFlag) {
		this.bookmarkFlag = bookmarkFlag;
	}

	class WebPagerAdapter extends PagerAdapter implements OnPageChangeListener {

		private Page[] pages;

		WebPagerAdapter() {
			pager.setOnPageChangeListener(this);
		}

		@Override
		public int getCount() {
			return pages == null ? 0 : pages.length;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		private Page getPageAt(int position) {
			if (pages == null || position < 0 || pages.length <= position) {
				return null;
			} else {
				return pages[position];
			}
		}

		public GestureWebView getCurrentWebView() {
			return (GestureWebView) pager.findViewWithTag(pager.getCurrentItem());
		}

		@SuppressLint({ "SetJavaScriptEnabled", "NewApi", "ClickableViewAccessibility" })
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View res = LayoutInflater.from(SwipeReaderActivity.this).inflate(R.layout.v_swipe_reader_pager_item,
					container, false);
			GestureWebView web = (GestureWebView) res

			.findViewById(R.id.vsrpi_web_view);
			web.setGestureDetector(
					new GestureDetector(SwipeReaderActivity.this, new GestureDetector.SimpleOnGestureListener() {
						@Override
						public boolean onDoubleTap(MotionEvent e) {
							if (!ctlAnim.showAll.isRunning()) {
								ctlAnim.setShowValues(5);
								ctlAnim.showAll.start();
							}
							return super.onDoubleTap(e);
						}
						
					}));			
			web.setTag(position);
			web.setTag(R.id.webview_module_id, getPageAt(position).getModuleId());
			web.setTag(R.id.webview_page_id, getPageAt(position).getPageId());
			web.setTag(R.id.webview_page_idx, toc.getPageIndexById(getPageAt(position).getPageId()));
			res.setTag(web);
			webViews.add(web);
			final View overlay = res.findViewById(R.id.vsrpi_overlay);
			web.getSettings().setAllowFileAccess(true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				web.getSettings().setAllowUniversalAccessFromFileURLs(true);
			}
			web.getSettings().setDomStorageEnabled(true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				web.getSettings().setAllowFileAccessFromFileURLs(true);
			}
			web.getSettings().setJavaScriptEnabled(true);
			web.clearCache(true);
			web.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageStarted(WebView view, String url, Bitmap favicon) {
				}

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url != null && url.startsWith("http")) {
						final Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url));
						view.getContext().startActivity(intent);
						return true;
					}
					return false;
				}

			});
			web.setWebChromeClient(new WebChromeClient());
			web.addJavascriptInterface(new JSInterface(web, overlay), JS_INTEFACE_NAME);
			String url = "file://"
					+ (Util.isDev(SwipeReaderActivity.this) ? Util.getExternalStorageDevDir(SwipeReaderActivity.this, timestamp).getPath()
							: book.getLocalPath())
					+ File.separator + "content" + File.separator + getPageAt(position).getPath();
			web.loadUrl(url);
			container.addView(res);
			return res;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			View o = (View) object;
			WebView w = (WebView) o.getTag();
			w.clearCache(true);
			w.onPause();
			w.loadUrl("about:blank");
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
				w.freeMemory();	
			}			
			w.removeAllViews();
			w.destroy();
			webViews.remove(w);
			container.removeView((View) object);
			Log.e("SRA", "destroyItem");
		}

		public void updatePages(Page[] pages) {
			this.pages = pages;
			notifyDataSetChanged();
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int position) {
			int idx = toc.getPageIndex(position, isTeacher);
			storeCurrentPage(idx);
			GestureWebView w = (GestureWebView) pager.findViewWithTag(position);
			Log.e("CURR_WEBVIEW", "set: " + position);
			if (currentWebView != null) {
				currentWebView.loadUrl(JS_FUNCTION_STOP_PLAYBACK);
			}
			currentWebView = w;
		}

	}

	class JSInterface {

		private final WebView web;

		private final View overlay;

		public JSInterface(WebView web, View overlay) {
			this.web = web;
			this.overlay = overlay;
		}

		@JavascriptInterface
		public void storeFontSize(String size) {
			int sizeInt = -1;
			try {
				sizeInt = Integer.parseInt(size);
			} catch (Exception e) {
				Log.e("JS_INTERFACE", "invalid value " + size);
			}
			if (sizeInt > -1) {
				if (checkUserLoggedIn()) {
					Settings newSettings = UserContext.getCurrentUser().getSettings().buildUpon().withFontSize(sizeInt)
							.build();
					User.Builder bldr = UserContext.getCurrentUser().buildUpon();
					bldr.withSettings(newSettings);
					UserContext.updateUser(bldr.build());
					new UpdateSettingsTask(getApplicationContext(), UserContext.getCurrentUser().getLocalUserId(),
							newSettings).execute();
				}
			}
		}

		@JavascriptInterface
		public String getFontSize() {
			int res = 100;
			if (checkUserLoggedIn()) {
				res = UserContext.getCurrentUser().getSettings().getFontSize();
			}
			Log.e("FONT SIZE", "" + res);
			return String.valueOf(res);
		}

		@JavascriptInterface
		public boolean isTeacher() {
			return Util.isTeacher(SwipeReaderActivity.this);
		}

		@JavascriptInterface
		public boolean canPlayVideo(String url) {
			if (Util.isMobileOnlyNetwork(SwipeReaderActivity.this)
					&& !Util.isMobileNetworkAllowed(SwipeReaderActivity.this)) {
				Toast.makeText(SwipeReaderActivity.this, R.string.toast_mobile_network_not_allowed, Toast.LENGTH_LONG)
						.show();
				return false;
			} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				final Intent i = new Intent(SwipeReaderActivity.this, PlayerActivity.class);
				i.putExtra(PlayerActivity.EXTRA_VIDEO_URL, url);
				startActivity(i);
				return false;
			}
			return true;
		}

		@JavascriptInterface
		public void notifyButtonsState(boolean aMin, boolean aPlus, boolean toc, boolean prev, boolean next) {
			pager.setPagingEnabled(true);
		}

		@JavascriptInterface
		public void notifyButtonsStateHide() {
			pager.setPagingEnabled(false);
		}

		@JavascriptInterface
		public void notifyFontButtonsEnabled(final boolean aminus, final boolean aplus) {
			if (web == currentWebView) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						aPlusBtn.setEnabled(aplus);
						aMinusBtn.setEnabled(aminus);
					}
				});
			}
		}

		@JavascriptInterface
		public void notifyModalWindowVisible() {
			pager.setPagingEnabled(false);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					isModalVisible = true;
					topBar.setVisibility(View.GONE);
					prevBtn.setVisibility(View.GONE);
					nextBtn.setVisibility(View.GONE);
				}
			});
		}

		@JavascriptInterface
		public void notifyModalWindowHidden() {
			pager.setPagingEnabled(true);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					isModalVisible = false;
					topBar.setVisibility(View.VISIBLE);
					prevBtn.setVisibility(View.VISIBLE);
					nextBtn.setVisibility(View.VISIBLE);
				}
			});
		}

		@JavascriptInterface
		public void openPageLink(String file, final String anchor) {
			final int i = toc.getPageDisplayIndex(file, Util.isTeacher(getApplicationContext()));
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					pager.setCurrentItem(i, true);
					if (anchor != null) {
						currentWebView.loadUrl("javascript:(function(){jumpToAnchor('" + anchor + "');})();");
					}
				}
			});
		}

		@JavascriptInterface
		public void notifyEverythingWasLoaded() {
			Log.e("SRA", "notifyEverythingWasLoaded");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					overlay.setVisibility(View.GONE);
					if (web == currentWebView && jumpToNote != null) {
						Log.e("JS", "jumping to note: " + jumpToNote);
						web.loadUrl(String.format("javascript:(function(){window.jumpToNote(\"%s\");})()", jumpToNote));
						jumpToNote = null;
					}
				}
			});
		}

		@JavascriptInterface
		public void openExternalWindow(String url, boolean showOverlay) {
			final Intent i = new Intent(SwipeReaderActivity.this, WomiPlayerActivity.class);
			i.putExtra(WomiPlayerActivity.EXTRA_WOMI_URL,
					new StringBuilder("file://")
							.append(Util.isDev(SwipeReaderActivity.this) ? Util.getExternalStorageDevDir(SwipeReaderActivity.this, timestamp)
									.getAbsolutePath() : book.getLocalPath())
							.append(File.separator).append(url).toString());
			i.putExtra(WomiPlayerActivity.EXTRA_SHOW_OVERLAY, showOverlay);
			startActivity(i);
		}

		@JavascriptInterface
		public void requestNotes() {
			if (checkUserLoggedIn()) {
				final long localUserId = UserContext.getCurrentUser().getLocalUserId();
				final String pageId = (String) web.getTag(R.id.webview_page_id);
				Log.e("JSINT", "starting load notes task " + pageId);
				new LoadNotesTask(SwipeReaderActivity.this, localUserId, getMdContentId(), getMdVersion(), pageId)
						.execute();
			}
		}

		@JavascriptInterface
		public String getNoteByLocalNoteId(String localNoteId) {
			return null;
		}

		@JavascriptInterface
		public void showMessage(final String message) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(SwipeReaderActivity.this, "Tutaj nie można dodać notatki.", Toast.LENGTH_LONG)
							.show();
				}
			});
		}

		@JavascriptInterface
		public void showNoteCreate(final String noteText, String location, String notesToMerge) {
			if (checkUserLoggedIn()) {
				final LocationPart[] loc = NotesUtil.noteLocationFromString(location);
				if (loc != null) {
					final long localUserId = UserContext.getCurrentUser().getLocalUserId();
					final String handbookId = getMdContentId() + ":" + getMdVersion();
					final String moduleId = (String) web.getTag(R.id.webview_module_id);
					final String pageId = (String) web.getTag(R.id.webview_page_id);
					final String[] ntm = NotesUtil.noteIdsFromString(notesToMerge);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (bookmarkFlag) {
								showBookmarkCreateDialog(pageId, loc, localUserId, handbookId, moduleId, ntm, noteText);
							} else {
								showNoteCreateDialog(pageId, loc, localUserId, handbookId, moduleId, ntm, noteText);
							}
						}
					});
				} else {
					showMessage("Niepoprawna lokalizacja notatki.");
				}
			}
		}

		@JavascriptInterface
		public void handleNoteClick(final String localNoteId) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					startEditDialogTask(localNoteId);
				}
			});
		}

		@JavascriptInterface
		public void copyToClipboard(final String selectedText) {
			ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData cd = ClipData.newPlainText("Z E-podręcznika", selectedText);
			clip.setPrimaryClip(cd);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(SwipeReaderActivity.this, "Skopiowano do schowka:\n" + selectedText,
							Toast.LENGTH_SHORT).show();
				}
			});
		}		

		@JavascriptInterface
		public void setStateForWomi(String womiId, String womiState) {
			new StoreWomiStateTask(SwipeReaderActivity.this, womiId, womiState).execute();
		}

		@JavascriptInterface
		public void getStateForWomi(String womiId) {
			queryWomiStateTask = new QueryWomiStateTask(SwipeReaderActivity.this, womiId);
			queryWomiStateTask.execute();
		}

		@JavascriptInterface
		public void setStateForOpenQuestion(String oqId, String oqState) {
			new StoreWomiStateTask(SwipeReaderActivity.this, oqId, oqState).execute();
		}

		@JavascriptInterface
		public void getStateForOpenQuestions(String oqIds) {
			final String pageId = (String) web.getTag(R.id.webview_page_id);
			new QueryOpenQuestionsTask(SwipeReaderActivity.this, oqIds, pageId).execute();
		}

	}

	@SuppressLint("RtlHardcoded")
	public static class ControlsAnimation {

		public static final int DURATION = 300;

		private List<View> views;

		private AnimatorSet showAll;

		private AnimatorSet hideAll;

		private ControlsAnimation(Builder builder) {
			views = builder.views;
			showAll = new AnimatorSet();
			AnimatorSet.Builder showBuilder = null;
			hideAll = new AnimatorSet();
			AnimatorSet.Builder hideBuilder = null;
			for (View v : views) {
				ObjectAnimator sa = (ObjectAnimator) v.getTag(R.id.show_animation);
				if (showBuilder == null) {
					showBuilder = showAll.play(sa);
				} else {
					showBuilder.with(sa);
				}
				ObjectAnimator ha = (ObjectAnimator) v.getTag(R.id.hide_animation);
				if (hideBuilder == null) {
					hideBuilder = hideAll.play(ha);
				} else {
					hideBuilder.with(ha);
				}
			}
			showAll.setDuration(DURATION);
			showAll.setInterpolator(new OvershootInterpolator());
			showAll.addListener(new AnimatorCancelListener());
			hideAll.setDuration(DURATION);
			hideAll.setInterpolator(new LinearInterpolator());
			hideAll.addListener(new AnimatorCancelListener());
		}

		void setHideValues(float percent) {
			for (View v : views) {
				ObjectAnimator h = (ObjectAnimator) v.getTag(R.id.hide_animation);
				h.setFloatValues(calculateStartValue(v, percent), calculateHideEndValue(v));
			}
		}

		void setShowValues(float percent) {
			for (View v : views) {
				ObjectAnimator s = (ObjectAnimator) v.getTag(R.id.show_animation);
				s.setFloatValues(calculateStartValue(v, percent), 0f);
			}
		}

		float calculateStartValue(View v, float start) {
			HideDirection d = (HideDirection) v.getTag(R.id.direction);
			switch (d) {
			case LEFT:
				return -start * v.getWidth();
			case RIGHT:
				return start * v.getWidth();
			case UP:
				return -start * v.getHeight();
			case DOWN:
				return start * v.getHeight();
			}
			return 0;
		}

		float calculateHideEndValue(View v) {
			HideDirection d = (HideDirection) v.getTag(R.id.direction);
			switch (d) {
			case LEFT:
				return -v.getWidth();
			case RIGHT:
				return v.getWidth();
			case UP:
				return -v.getHeight();
			case DOWN:
				return v.getHeight();
			}
			return 0;
		}

		static class Builder {

			List<View> views = new ArrayList<View>();

			Builder withViewAndDirection(View view, HideDirection dir) {
				ObjectAnimator show;
				ObjectAnimator hide;
				switch (dir) {
				case LEFT:
				case RIGHT:
					show = ObjectAnimator.ofFloat(view, "translationX", 0, 0);
					hide = ObjectAnimator.ofFloat(view, "translationX", 0, 0);
					break;
				case UP:
				case DOWN:
					show = ObjectAnimator.ofFloat(view, "translationY", 0, 0);
					hide = ObjectAnimator.ofFloat(view, "translationY", 0, 0);
					break;
				default:
					show = ObjectAnimator.ofFloat(view, "alpha", 0, 0);
					hide = ObjectAnimator.ofFloat(view, "alpha", 0, 0);

				}
				view.setTag(R.id.show_animation, show);
				view.setTag(R.id.hide_animation, hide);
				view.setTag(R.id.direction, dir);
				views.add(view);
				return this;
			}

			ControlsAnimation build() {
				return new ControlsAnimation(this);
			}
		}

		static enum HideDirection {
			LEFT, RIGHT, UP, DOWN
		}

	}

	static class AnimatorCancelListener extends AnimatorListenerAdapter {

		@Override
		public void onAnimationCancel(Animator animation) {
			animation.end();
		}

	}

	static class LoadNotesTask extends AsyncTask<Void, Void, String> {

		private final WeakReference<SwipeReaderActivity> actRef;

		private final long localUserId;

		private final String mdContentId;

		private final int mdVersion;

		private final String pageId;

		LoadNotesTask(SwipeReaderActivity act, long localUserId, String mdContentId, int mdVersion, String pageId) {
			this.actRef = new WeakReference<SwipeReaderActivity>(act);
			this.localUserId = localUserId;
			this.mdContentId = mdContentId;
			this.mdVersion = mdVersion;
			this.pageId = pageId;
		}

		@Override
		protected String doInBackground(Void... params) {
			final SwipeReaderActivity act = actRef.get();
			if (act != null) {
				return NotesUtil.getNotesForUserBookPageAsString(act, localUserId, mdContentId, mdVersion, pageId);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.e("LNT", "onPostExecute");
			final SwipeReaderActivity act = actRef.get();
			if (act != null) {
				final WebView web = act.getWebViewByPageId(pageId);
				if (web != null) {
					Log.e("LNT", "calling showNotes");
					web.loadUrl(String.format(JS_FUNCTION_SHOW_NOTES_TMPL, result));
				}
			}
		}

	}

	static class QueryWomiStateTask extends AsyncTask<Void, Void, String> {

		private SwipeReaderActivity ctx;

		private final String womiId;

		private String pageId;

		QueryWomiStateTask(SwipeReaderActivity ctx, String womiId) {
			this.ctx = ctx;
			this.womiId = womiId;
			this.pageId = (String) ctx.currentWebView.getTag(R.id.webview_page_id);
		}

		@Override
		protected String doInBackground(Void... params) {
			String res = null;
			final User currentUser = UserContext.getCurrentUser();
			if (currentUser != null) {
				final Cursor c = ctx.getContentResolver()
						.query(ExerciseStatesProvider.UriHelper.exerciseStateByUserBookWomi(
								currentUser.getLocalUserId(), ctx.getMdContentId(), ctx.getMdVersion(), womiId),
						ExerciseStatesTable.COLUMNS, null, null, null);
				if (c != null) {
					if (c.moveToFirst()) {
						ExerciseState.Builder bldr = Util.getExerciseStateBuilderFromCursor(c, c.getPosition());
						if (bldr != null) {
							ExerciseState state = bldr.build();
							res = state.getValue();
						}
					}
					c.close();
				}
			}
			return res;
		}

		@Override
		protected void onCancelled(String result) {
			ctx = null;
		}

		@Override
		protected void onPostExecute(String result) {
			String currentTag = (String) ctx.currentWebView.getTag(R.id.webview_page_id);
			Log.e("QWS", "updating womi state " + result);
			if (currentTag != null && currentTag.equals(pageId)) {
				if (result != null) {
					ctx.currentWebView.loadUrl(String.format(JS_FUNCTION_UPDATE_WOMI_STATE_TMPL, result));
				} else {
					ctx.currentWebView.loadUrl(JS_FUNCTION_UPDATE_WOMI_NULL_STATE);
				}
			}
			ctx = null;
		}

	}

	static class StoreWomiStateTask extends AsyncTask<Void, Void, Void> {

		private SwipeReaderActivity ctx;

		private final String womiId;

		private final String womiState64;

		StoreWomiStateTask(SwipeReaderActivity ctx, String womiId, String womiState64) {
			this.ctx = ctx;
			this.womiId = womiId;
			this.womiState64 = womiState64;
		}

		@Override
		protected Void doInBackground(Void... params) {
			User currentUser = UserContext.getCurrentUser();
			if (currentUser != null) {
				boolean alreadyExists = false;
				final Cursor c = ctx.getContentResolver()
						.query(ExerciseStatesProvider.UriHelper.exerciseStateByUserBookWomi(
								currentUser.getLocalUserId(), ctx.getMdContentId(), ctx.getMdVersion(), womiId),
						ExerciseStatesTable.COLUMNS, null, null, null);
				if (c != null) {
					if (c.moveToFirst()) {
						alreadyExists = true;
					}
					c.close();
				}
				ContentValues vals = new ContentValues();
				vals.put(ExerciseStatesTable.C_WOMI_ID, womiId);
				vals.put(ExerciseStatesTable.C_MD_CONTENT_ID, ctx.getMdContentId());
				vals.put(ExerciseStatesTable.C_MD_VERSION, ctx.getMdVersion());
				vals.put(ExerciseStatesTable.C_LOCAL_USER_ID, currentUser.getLocalUserId());
				vals.put(ExerciseStatesTable.C_VALUE, womiState64);
				Log.e("QWS", "storing womi state " + womiId);
				if (alreadyExists) {
					ctx.getContentResolver()
							.update(ExerciseStatesProvider.UriHelper.exerciseStateByUserBookWomi(
									currentUser.getLocalUserId(), ctx.getMdContentId(), ctx.getMdVersion(), womiId),
							vals, null, null);
				} else {
					ctx.getContentResolver().insert(ExerciseStatesProvider.UriHelper.EXERCISE_STATES_URI, vals);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			ctx = null;
		}

		@Override
		protected void onCancelled(Void result) {
			ctx = null;
		}

	}

	static class QueryOpenQuestionsTask extends AsyncTask<Void, Void, String> {

		private SwipeReaderActivity ctx;

		private final String pageId;
		
		private final String oqIds;				

		QueryOpenQuestionsTask(SwipeReaderActivity ctx, String oqIds, String pageId) {
			this.ctx = ctx;
			this.oqIds = oqIds;
			this.pageId = pageId;
		}

		@Override
		protected String doInBackground(Void... params) {
			final User currentUser = UserContext.getCurrentUser();
			if(currentUser!=null){
				Gson gson = new Gson();
				String[] ids = null;
				try{
					ids = gson.fromJson(oqIds, String[].class);
				}catch(Exception e){				
				}
				if(ids!=null && ids.length>0){
					for(int i = 0; i < ids.length; i++){
						ids[i] = String.format("'%s'", ids[i]);
					}
					String in = String.format("(%s)", TextUtils.join(",", ids));
					Log.e("SRA", "in clause: "+in);
					Cursor c = ctx.getContentResolver().query(
							ExerciseStatesProvider.UriHelper.exerciseStatesByBookUser(currentUser.getLocalUserId(), ctx.getMdContentId(), ctx.getMdVersion()), 
							new String[]{ExerciseStatesTable.C_WOMI_ID,ExerciseStatesTable.C_VALUE}, ExerciseStatesTable.C_WOMI_ID+" IN "+in,null, null);				
					Map<String,String> res = new HashMap<String,String>();
					if(c!=null){						
						while(c.moveToNext()){
							String womiId = c.getString(0);
							String value = c.getString(1);
							res.put(womiId, value);
						}
						c.close();
					}
					String jsonRes = gson.toJson(res);
					return Base64.encodeToString(jsonRes.getBytes(), Base64.NO_WRAP);
				}								
			}			
			return null;
		}
		
		@Override
		protected void onCancelled(String result) {			
			super.onCancelled(result);
			ctx = null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			WebView w  = ctx.getWebViewByPageId(pageId);
			if(w!=null){
				if(result!=null){
					w.loadUrl(String.format(JS_FUNCTION_UPDATE_OQ_STATES_TMPL,result));
				}else{
					w.loadUrl(JS_FUNCTION_UPDATE_OQ_NULL_STATES);
				}
			}
			ctx = null;
		}

	}

	static class UpdateSettingsTask extends AsyncTask<Void, Void, Void> {

		private Context ctx;

		private final long userId;

		private final Settings settings;

		UpdateSettingsTask(Context ctx, long userId, Settings settings) {
			this.ctx = ctx.getApplicationContext();
			this.userId = userId;
			this.settings = settings;
		}

		@Override
		protected Void doInBackground(Void... params) {
			ContentValues values = new ContentValues();
			values.put(UsersTable.C_SETTINGS, settings.toJsonString());
			ctx.getContentResolver().update(UsersProvider.UriHelper.userByLocalUserId(userId), values, null, null);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			ctx = null;
		}

		@Override
		protected void onCancelled(Void result) {
			super.onCancelled(result);
			ctx = null;
		}

	}

	static class ShowEditDialogTask extends AsyncTask<Void, Void, Note> {

		private SwipeReaderActivity ctx;

		private final String localNoteId;

		ShowEditDialogTask(SwipeReaderActivity ctx, String localNoteId) {
			this.ctx = ctx;
			this.localNoteId = localNoteId;
		}

		@Override
		protected Note doInBackground(Void... params) {
			Note res = null;
			if (ctx != null) {
				Cursor c = ctx.getContentResolver().query(NotesProvider.UriHelper.noteByLocalNoteId(localNoteId),
						NotesTable.COLUMNS, null, null, null);
				if (c != null) {
					if (c.moveToFirst()) {
						Note.Builder bldr = NotesUtil.getNoteBuilderFromCursor(c, c.getPosition());
						res = bldr.build();
					}
					c.close();
				}
			}
			return res;
		}

		@Override
		protected void onPostExecute(Note result) {
			super.onPostExecute(result);
			if (result != null) {
				if (result.isBookmark()) {
					ctx.showBookmarkEditDialog(localNoteId);
				} else {
					ctx.showNoteEditDialog(localNoteId);
				}
			}
			ctx = null;

		}

		@Override
		protected void onCancelled(Note result) {
			super.onCancelled(result);
			ctx = null;
		}

	}

}