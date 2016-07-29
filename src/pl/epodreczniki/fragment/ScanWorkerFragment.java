package pl.epodreczniki.fragment;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import pl.epodreczniki.activity.DevScanActivity;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ScanWorkerFragment extends Fragment{

	public static final String TAG = "scan_worker_fragment";
	
	private DevScanActivity activity;
	
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
		this.activity = (DevScanActivity) activity;
	}
	
	@Override
	public void onDetach() {		
		super.onDetach();
		activity = null;
	}
	
	public void decode(byte[] bytes, int width, int height){
		new DecodeTask(bytes, width, height).execute();
	}
	
	private class DecodeTask extends AsyncTask<Void,Void,String>{

		private final byte[] bytes;
		
		private final int width;
		
		private final int height;
		
		DecodeTask(byte[] bytes, int width, int height){
			this.bytes = bytes;
			this.width = width;
			this.height = height;
		}
		
		@Override
		protected String doInBackground(Void... params) {
		    final MultiFormatReader multiFormatReader = new MultiFormatReader();
			Result rawResult = null;
            final PlanarYUVLuminanceSource source =
                    activity.buildLuminanceSource(bytes, width,
                            height, activity.getBoundingRect());
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {                	
                }
            }
			return rawResult==null?null:rawResult.toString();
		}
		
		@Override
		protected void onPostExecute(String result) {		
			if(activity!=null){
				if(result!=null){
					activity.sendSuccessMessage(result);
				}else{
					activity.sendFailMessage();
				}
			}
		}		
	}
	
}
