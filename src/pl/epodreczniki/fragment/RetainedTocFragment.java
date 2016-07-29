package pl.epodreczniki.fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import pl.epodreczniki.model.Page;
import pl.epodreczniki.model.Toc;
import pl.epodreczniki.model.TocItem;
import pl.epodreczniki.util.Constants;
import pl.epodreczniki.util.Util;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RetainedTocFragment extends Fragment{
	
	public static final String TAG = "retained_toc_fragment";
	
	private static final String MD_CONTENT_ID_KEY = "md.content.id";
	
	private static final String MD_VERSION_KEY = "md.version";
	
	
	
	private volatile TocEnabled tocActivity;
	
	private String tocKey;
	
	private String mdContentId;
	
	private int mdVersion;
	
	private Map<String,Toc> tocs = new HashMap<String,Toc>();
	
	private LoadTocTask task;
	
	public interface TocEnabled{
		
		String getMdContentId();
		
		int getMdVersion();				
		
		void onTocLoaded(Toc toc);
		
		void onTocError(TocError error);
		
	}
	
	public static class TocResponse{
		
		private final Toc toc;
		
		private final TocError error;
		
		public TocResponse(Toc toc){
			this.toc = toc;
			this.error = null;
		}
		
		public TocResponse(TocError error){
			this.toc = null;
			this.error = error;
		}
		
		public Toc getToc(){
			return toc;
		}
		
		public TocError getError(){
			return error;
		}
		
	}
	
	public static enum TocError{
		NOT_FOUND, IO_ERROR, PARSE_ERROR		
	}

	@Override
	public void onAttach(Activity activity) {	
		super.onAttach(activity);
		tocActivity = (TocEnabled) activity;		
	}
	
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.e("RTF","onActivityCreated");		
		mdContentId = tocActivity.getMdContentId();		
		mdVersion = tocActivity.getMdVersion();
		if(mdContentId!=null && mdVersion>0){
			tocKey = mdContentId+"_"+mdVersion;				
		}
		if(!tocs.containsKey(tocKey)){
			task = new LoadTocTask();
		}
		if(task!=null){
			task.execute();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(tocKey!=null && tocs.containsKey(tocKey)){
			tocActivity.onTocLoaded(tocs.get(tocKey));
		}
	}
	
	@Override
	public void onDetach() {
		tocActivity = null;
		if(task!=null){
			task.cancel(true);
			task = null;
		}		
		super.onDetach();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {	
		super.onSaveInstanceState(outState);
		outState.putString(MD_CONTENT_ID_KEY, mdContentId);
		outState.putInt(MD_VERSION_KEY, mdVersion);
		Log.e("RTF","save instance state");
	}
	
	private class LoadTocTask extends AsyncTask<Void,Void,TocResponse>{		
		
		@Override
		protected TocResponse doInBackground(Void... params) {			
			TocResponse res = null;
			final File base = Util.isDev(getActivity())?Util.getExternalStorageDevDir(getActivity(), mdContentId):Util.getExternalStorageBookDir(getActivity(), mdContentId, mdVersion);			
			final File pagesFile = new File(base,Constants.PAGES_ENTRY);
			final File tocFile = new File(base,Constants.TOC_ENTRY);
			Gson gson = new Gson();
			BufferedReader pagesBr;
			BufferedReader tocBr;
			try {
				pagesBr = new BufferedReader(new FileReader(pagesFile));
				tocBr = new BufferedReader(new FileReader(tocFile));
				Page[] pages = gson.fromJson(pagesBr, Page[].class);
				TocItem[] tocItems = gson.fromJson(tocBr, TocItem[].class);
				Toc r = new Toc(pages,tocItems,mdContentId,mdVersion);
				res = new TocResponse(r);
			} catch(JsonSyntaxException e){
				res = new TocResponse(TocError.PARSE_ERROR);
				cancel(true);
			} catch(JsonIOException e){
				res = new TocResponse(TocError.IO_ERROR);
				cancel(true);
			} catch (FileNotFoundException e) {
				res = new TocResponse(TocError.NOT_FOUND);
				cancel(true);
			}						
			return res;
		}
		
		@Override
		protected void onCancelled(TocResponse result) {
			if(tocActivity!=null && result!=null){
				tocActivity.onTocError(result.getError());
			}
		}
		
		@Override
		protected void onPostExecute(TocResponse result) {		
			super.onPostExecute(result);
			if(tocActivity!=null && tocKey!=null){
				tocs.put(tocKey, result.getToc());
				tocActivity.onTocLoaded(result.getToc());
			}
		}
		
	}
	
}
