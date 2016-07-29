package pl.epodreczniki.activity;

import pl.epodreczniki.R;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

public class WomiPlayerActivity extends AbstractUserAwareActivity {

	public static final String EXTRA_WOMI_URL = "ex.wpa.womi.url";
	
	public static final String EXTRA_SHOW_OVERLAY = "ex.wpa.womi.overlay";
	
	private static final int CHOOSE_PICTURE = 100;
	
	private static final int CHOOSE_PICTURE_LOLLIPOP = 101;
	
	private View overlay;
	
	private ValueCallback<Uri> mUploadCallback;
	
	private ValueCallback<Uri[]> mUploadArrayCallback;	
	
	private WebView web;
	
	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.a_womi_player);	
		boolean showOverlay = getIntent().getBooleanExtra(EXTRA_SHOW_OVERLAY, false);
		overlay = findViewById(R.id.awp_overlay);		
		overlay.setVisibility(showOverlay?View.VISIBLE:View.GONE);
		web = (WebView) findViewById(R.id.awp_web);
		web.getSettings().setDomStorageEnabled(true);
		web.getSettings().setJavaScriptEnabled(true);		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			web.getSettings().setAllowFileAccessFromFileURLs(true);			
		}
		web.setWebViewClient(new WebViewClient());
		web.setWebChromeClient(new WebChromeClient(){
						
			@SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> callback, String acceptedType){
				WomiPlayerActivity.this.showUploadDialog(callback);
			}
			
			@SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> callback, String acceptedType, String capture){
				WomiPlayerActivity.this.showUploadDialog(callback);
			}
						
			@Override
			public boolean onShowFileChooser(WebView webView,
					ValueCallback<Uri[]> filePathCallback,
					FileChooserParams fileChooserParams) {
				showUploadDialogLollipop(filePathCallback);				
				return true;
			}
		});
		web.addJavascriptInterface(new WomiPlayerJSInterface(), "Android");
		String url = getIntent().getStringExtra(EXTRA_WOMI_URL);
		web.clearCache(true);
		web.loadUrl(url);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onDestroy() {	
		super.onDestroy();
		Log.e("WPA", "onDestroy");
		web.clearCache(true);
		web.onPause();
		web.loadUrl("about:blank");
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT){
			web.freeMemory();	
		}		
		web.removeAllViews();
		web.destroy();
	}
	
	private void showUploadDialog(ValueCallback<Uri> uploadCallback){
		if(mUploadCallback==null){
			mUploadCallback = uploadCallback;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);		
	        i.addCategory(Intent.CATEGORY_OPENABLE);
	        i.setType("image/*");
	        startActivityForResult(i, CHOOSE_PICTURE);
		}
	}
	
	private void showUploadDialogLollipop(ValueCallback<Uri[]> uploadArrayCallback){
		if(mUploadArrayCallback==null){
			mUploadArrayCallback = uploadArrayCallback;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	        i.addCategory(Intent.CATEGORY_OPENABLE);
	        i.setType("image/*");
	        startActivityForResult(i, CHOOSE_PICTURE_LOLLIPOP);
		}		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		if(resultCode == Activity.RESULT_OK){
			if(requestCode==CHOOSE_PICTURE_LOLLIPOP){				
				if(mUploadArrayCallback!=null){
					if(data.getDataString()!=null){
						Uri[] res = new Uri[]{Uri.parse(data.getDataString())};							
						mUploadArrayCallback.onReceiveValue(res);		
					}	
				}				
			}
			if(requestCode==CHOOSE_PICTURE){
				if(mUploadCallback!=null){
					if(data.getData()!=null){
						mUploadCallback.onReceiveValue(data.getData());
					}
				}
			}
		}
		mUploadArrayCallback = null;		
		mUploadCallback = null;
	}
	
	class WomiPlayerJSInterface {
		
		@JavascriptInterface
		public void notifyEverythingWasLoaded(){
			Log.e("WPA","loaded");
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					overlay.setVisibility(View.GONE);
				}
				
			});					
		}
		
		@JavascriptInterface
		public void notifyEverythingWillBeLoaded(){
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					overlay.setVisibility(View.VISIBLE);
				}
				
			});
		}		
		
	}

}
