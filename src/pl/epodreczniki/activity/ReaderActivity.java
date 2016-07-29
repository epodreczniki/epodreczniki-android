package pl.epodreczniki.activity;

import java.io.File;
import java.lang.ref.WeakReference;

import pl.epodreczniki.R;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ReaderActivity extends AbstractUserAwareActivity {

	private static final String READER_PREFS = "epodreczniki.preferences.reader";

	private WebView webView;

	private ProgressBar progressBar;

	private String indexUrl;
	
	private String afterBlank;
	
	private NavigationHandler handler;
	
	private View navigation;
	
	private ImageButton[] navigationButtons = new ImageButton[5];

	public static final String EXTRA_URL = "pl.epodreczniki.reader.url";
	public static final String BOOK_TITLE = "Czytnik";
	private static final String JS_INTERFACE_NAME = "Android";

	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);			
		setContentView(R.layout.a_reader);		
		setTitle(getIntent().getStringExtra(BOOK_TITLE));
		handler = new NavigationHandler(this);		
		
		navigation = findViewById(R.id.ar_nav);
		navigationButtons[0] = (ImageButton) findViewById(R.id.ar_nav_a_minus);
		navigationButtons[0].setOnClickListener(new NavigationClickListener(0));
		navigationButtons[1] = (ImageButton) findViewById(R.id.ar_nav_a_plus);
		navigationButtons[1].setOnClickListener(new NavigationClickListener(1));
		navigationButtons[2] = (ImageButton) findViewById(R.id.ar_nav_toc);
		navigationButtons[2].setOnClickListener(new NavigationClickListener(2));
		navigationButtons[3] = (ImageButton) findViewById(R.id.ar_nav_prev);
		navigationButtons[3].setOnClickListener(new NavigationClickListener(3));
		navigationButtons[4] = (ImageButton) findViewById(R.id.ar_nav_next);
		navigationButtons[4].setOnClickListener(new NavigationClickListener(4));
		
		progressBar = (ProgressBar) findViewById(R.id.ar_pb);

		webView = (WebView) findViewById(R.id.ar_wv);		
		webView.clearCache(true);
		webView.addJavascriptInterface(new ReaderJSInterface(this),
				JS_INTERFACE_NAME);
		webView.getSettings().setAllowFileAccess(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			webView.getSettings().setAllowFileAccessFromFileURLs(true);
		}
		/*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			WebView.setWebContentsDebuggingEnabled(true);
		}*/
		webView.getSettings().setDomStorageEnabled(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url != null && url.startsWith("http")) {
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(url));
					startActivity(intent);
					return true;
				} else {
					afterBlank = url;
					view.loadUrl("about:blank");
					view.clearCache(false);
					return false;
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if("about:blank".equals(url)){
					view.loadUrl(afterBlank);
				}else{
					progressBar.setVisibility(View.GONE);					
				}				
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				disableButtons();
				if(!"about:blank".equals(url)){
					progressBar.setVisibility(View.VISIBLE);	
				}				
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				view.loadUrl("file:///android_asset/error.html");
			}

		});

		indexUrl = getIntent().getStringExtra(EXTRA_URL);

		final SharedPreferences prefs = getSharedPreferences(READER_PREFS,
				MODE_PRIVATE);

		final String lastViewed = prefs.getString(indexUrl, null);

		if (checkStoredUri(lastViewed)) {
			webView.loadUrl(lastViewed);
		} else {
			webView.loadUrl(indexUrl);
		}
	}

	private boolean checkStoredUri(String uri) {
		boolean res = false;
		if (uri != null) {
			res = new File(Uri.parse(uri).getPath()).exists();
		}
		return res;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		final SharedPreferences prefs = getSharedPreferences(READER_PREFS,
				MODE_PRIVATE);
		final SharedPreferences.Editor ed = prefs.edit();
		ed.putString(indexUrl, webView.getUrl());
		ed.apply();
	}
	
	void hideNavigation(){
		handler.sendHideMessage();
	}
	
	void changeButtonsState(final boolean[] buttons){
		handler.sendButtonsMessage(buttons);
	}
	
	void disableButtons(){
		handler.sendDisableMessage();
	}

	static class ReaderJSInterface {

		private final ReaderActivity ctx;

		public ReaderJSInterface(ReaderActivity ctx) {
			this.ctx = ctx;
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
				Log.e("JS_INTERFACE", "storing font size " + sizeInt);
				SharedPreferences prefs = ctx.getSharedPreferences(
						READER_PREFS, Context.MODE_PRIVATE);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putInt(Constants.KEY_FONT_SIZE, sizeInt);
				edit.apply();
			}
		}

		@JavascriptInterface
		public String getFontSize() {
			SharedPreferences prefs = ctx.getSharedPreferences(READER_PREFS,
					Context.MODE_PRIVATE);
			int fs = prefs.getInt(Constants.KEY_FONT_SIZE, 100);
			return String.valueOf(fs);
		}
		
		@JavascriptInterface
		public boolean isTeacher(){
			return Util.isTeacher(ctx);
		}
		
		@JavascriptInterface
		public boolean canPlayVideo(String url){			
			if(Util.isMobileOnlyNetwork(ctx) && !Util.isMobileNetworkAllowed(ctx)){
				Toast.makeText(ctx, R.string.toast_mobile_network_not_allowed, Toast.LENGTH_LONG).show();
				return false;
			}else if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
				final Intent i = new Intent(ctx,PlayerActivity.class);
				i.putExtra(PlayerActivity.EXTRA_VIDEO_URL, url);
				ctx.startActivity(i);
				return false;
			}
			return true;
		}
		
		@JavascriptInterface
		public void notifyButtonsState(boolean aMin, boolean aPlus, boolean toc, boolean prev, boolean next){
			ctx.changeButtonsState(new boolean[]{aMin,aPlus,toc,prev,next});
		}
		
		@JavascriptInterface
		public void notifyButtonsStateHide(){
			ctx.hideNavigation();
		}

	}
	
	static class NavigationHandler extends Handler{
		
		private static final int WHAT_BTN_STATE = 0;
		
		private static final int WHAT_HIDE = 1;
		
		private static final int WHAT_BTNS_DISABLE = 2;
		
		private static final String JS_HIDE_NAVIGATION = "javascript:(function(){try{document.getElementsByClassName('permanent-navigation')[0].style.display='none';}catch(e){}})()";
		
		private final WeakReference<ReaderActivity> actRef;				
		
		NavigationHandler(ReaderActivity act){
			actRef = new WeakReference<ReaderActivity>(act);
		}
		
		boolean sendButtonsMessage(boolean[] buttons){
			final Message msg = obtainMessage(WHAT_BTN_STATE);
			msg.obj = buttons;
			return sendMessage(msg);
		}
		
		boolean sendHideMessage(){
			return sendMessage(obtainMessage(WHAT_HIDE));
		}
		
		boolean sendDisableMessage(){
			return sendMessage(obtainMessage(WHAT_BTNS_DISABLE));
		}
		
		@Override
		public void handleMessage(Message msg) {		
			final ReaderActivity act = actRef.get();
			if(act!=null){
				switch(msg.what){
				case WHAT_BTN_STATE:
					if(msg.obj!=null && msg.obj instanceof boolean[]){
						boolean[] states = (boolean[]) msg.obj;
						if(states.length == act.navigationButtons.length){
							int cnt = 0;
							for(boolean b : states){
								act.navigationButtons[cnt++].setEnabled(b);
							}	
							act.navigation.setVisibility(View.VISIBLE);
							act.webView.loadUrl(JS_HIDE_NAVIGATION);
						}						
					}					
					
					break;
				case WHAT_HIDE:
					act.navigation.setVisibility(View.GONE);
					break;
				case WHAT_BTNS_DISABLE:
					for(ImageButton btn : act.navigationButtons){
						btn.setEnabled(false);
					}
					break;
				}
			}
		}		
		
		
	}
	
	class NavigationClickListener implements OnClickListener{

		private static final String JS_FMT = "javascript:(function(){clickNavigationButton(%d);})();";				
		
		private final String js;
		
		NavigationClickListener(int which){
			js = String.format(JS_FMT, which);
		}
		
		@Override
		public void onClick(View v) {
			webView.loadUrl(js);
		}
		
	}

}
