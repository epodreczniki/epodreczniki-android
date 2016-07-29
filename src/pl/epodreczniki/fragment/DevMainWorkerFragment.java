package pl.epodreczniki.fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import pl.epodreczniki.util.TransferHelper;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class DevMainWorkerFragment extends Fragment{
	
	public static final String TAG = "dev.main.worker.fragment";
	
	private DownloadJsonCallbacks callbacks;
	
	private DownloadCssCallbacks cssCallbacks;
	
	private DownloadJsonTask task;
	
	private DownloadCssTask cssTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		callbacks = (DownloadJsonCallbacks) activity;
		cssCallbacks = (DownloadCssCallbacks) activity;
	}
	
	@Override
	public void onDetach() {	
		super.onDetach();
		callbacks = null;
		cssCallbacks = null;
	}
	
	public void requestJson(String url){
		if(task!=null){
			task.cancel(true);
		}
		task = new DownloadJsonTask();
		task.execute(url);
	}
	
	public void requestCss(String url,String timestamp){
		if(cssTask!=null){
			cssTask.cancel(true);
		}
		cssTask = new DownloadCssTask();
		cssTask.execute(url,timestamp);
	}
	
	public void cancelCss(){
		if(cssTask!=null){
			cssTask.cancel(true);
		}else{
			if(cssCallbacks!=null){
				cssCallbacks.onCssCancel();	
			}			
		}		
	}
	
	public interface DownloadJsonCallbacks{
		
		void processResult(String result);
		
	}
	
	public interface DownloadCssCallbacks{
		void processCssResult(Integer result);
		void onCssCancel();
	}
	
	private class DownloadJsonTask extends AsyncTask<String,Void,String>{

		@Override
		protected String doInBackground(String... params) {			
			String res = null;			
			try{
				if(params!=null && params.length>0){
					if(TransferHelper.isJsonUrl(params[0])){
						res = TransferHelper.getDevDataFromUrl(params[0]);	
					}else{
						res = params[0];
					}					
				}				
			}catch(IOException e){
				Log.e("DEV",e.toString());
				Log.e("DEV","unable to get metadata from: "+Arrays.toString(params));
			}
			return res;
		}
		
		@Override
		protected void onCancelled(String result) {		
			super.onCancelled(result);
			if(callbacks!=null){
				callbacks.processResult(null);
			}
		}
		
		@Override
		protected void onPostExecute(String result) {		
			super.onPostExecute(result);
			if(callbacks!=null){
				callbacks.processResult(result);
			}
		}
		
	}
	
	private class DownloadCssTask extends AsyncTask<String,Void,Integer>{
		
		private static final int BUFFER_SIZE = 4096;

		@Override
		protected Integer doInBackground(String... params) {
			int res = -1;
			if(params!=null && params.length==2){
				final String url = params[0];
				final String timestamp = params[1];
				if(!TextUtils.isEmpty(url) && !TextUtils.isEmpty(timestamp)){
					InputStream is = null;
					FileOutputStream fos = null;
					try {
						URL theUrl = new URL(url);
						HttpURLConnection conn = (HttpURLConnection) theUrl.openConnection();
						boolean redirect = false;
						int status = conn.getResponseCode();
						if(status != HttpURLConnection.HTTP_OK){
							if(status == HttpURLConnection.HTTP_MOVED_TEMP
									|| status == HttpURLConnection.HTTP_MOVED_PERM
									|| status == HttpURLConnection.HTTP_SEE_OTHER){
								redirect = true;
							}
						}
						if(redirect){
							String newUrl = conn.getHeaderField("Location");
							conn = (HttpURLConnection) new URL(newUrl).openConnection();
							status = conn.getResponseCode();
						}						
						if(status == HttpURLConnection.HTTP_OK){
							is = conn.getInputStream();
							File tempCss = Util.getExternalStorageDevCssTempFile(getActivity(), timestamp);
							if(tempCss.exists() && tempCss.isFile()){
								tempCss.delete();
							}
							fos = new FileOutputStream(tempCss);
							int bytesRead = -1;
							byte[] buff = new byte[BUFFER_SIZE];
							while((bytesRead=is.read(buff))!=-1){
								fos.write(buff,0,bytesRead);
							}
							fos.close();
							is.close();
							boolean renameRes = tempCss.renameTo(Util.getExternalStorageDevCssFile(getActivity(), timestamp));
							if(renameRes){
								Log.e("DLD CSS","OK");
								res = 0;
							}else{
								Log.e("DLD CSS","FAILED");
							}																													
						}
					} catch (MalformedURLException e) {
						Log.e("DLD CSS","FAILED malformed url");
					} catch (IOException e) {
						Log.e("DLD CSS","FAILED io exception");
					} finally{
						Util.closeSilently(is);
						Util.closeSilently(fos);
					}
				}
			}						
			return res;
		}
		
		@Override
		protected void onCancelled(Integer result) {
			cssCallbacks.onCssCancel();
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			cssCallbacks.processCssResult(result);
		}
		
	}
	
}
