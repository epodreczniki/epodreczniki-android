package pl.epodreczniki.util;

import java.lang.ref.WeakReference;

import pl.epodreczniki.view.ImageProgressBar;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncBitmapLoader extends AsyncTask<String, Void, Bitmap> {

	private final WeakReference<Context> ctxRef;

	private final WeakReference<ImageProgressBar> imgRef;

	private String coverLocalPath;

	public AsyncBitmapLoader(Context ctx, ImageProgressBar img) {
		ctxRef = new WeakReference<Context>(ctx);
		imgRef = new WeakReference<ImageProgressBar>(img);
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		Bitmap res = null;
		if(params!=null && params.length>0){
			Uri u = Uri.parse(params[0]);
			coverLocalPath = u.getPath();						
			final Context ctx = ctxRef.get();		
			if (coverLocalPath!=null && ctx != null) {
				res = decode(ctx);
			}	
		}		
		return res;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		final ImageProgressBar img = imgRef.get();
		Context ctx = ctxRef.get();
		if(result != null && img != null){
			img.setImageBitmap(result);
			if(ctx!=null && coverLocalPath!=null){
				Log.e("CACHE", "adding to cache");
				ImageCache.get(ctx).put(coverLocalPath, result);
			}
		}
	}
	
	private Bitmap decode(Context ctx){
		Bitmap bm = null;
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;		
		BitmapFactory.decodeFile(coverLocalPath,options);		
		options.inSampleSize = ImageUtil.calculateInSampleSizeWithRequiredHeight(options, ImageCache.getLongerScreenEdge(ctx)/2);
		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		bm = BitmapFactory.decodeFile(coverLocalPath,options);
		return bm;		
	}	

}
