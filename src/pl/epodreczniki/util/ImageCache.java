package pl.epodreczniki.util;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.LruCache;
import android.view.Display;
import android.view.WindowManager;

public class ImageCache {

	private static volatile LruCache<String,Bitmap> cache;
	
	private static int longerEdge = -1;
	
	public static LruCache<String,Bitmap> get(Context ctx){
		if(cache==null){
			synchronized(ImageCache.class){
				if(cache==null){					
					ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
					int sizeMB = am.getMemoryClass()/4;
					cache = new LruCache<String,Bitmap>(sizeMB*1024*1024){
						@Override
						protected int sizeOf(String key, Bitmap value) {
							return value.getByteCount();
						}						
					};
					ctx.registerComponentCallbacks(new ComponentCallbacks2() {
						
						@Override
						public void onLowMemory() {
						}
						
						@Override
						public void onConfigurationChanged(Configuration newConfig) {
						}
						
						@Override
						public void onTrimMemory(int level) {
							if (level >= TRIM_MEMORY_MODERATE) {
					            cache.evictAll();
					        }					        
						}
					});
				}
			}
		}
		return cache;
	}
	
	public static int getLongerScreenEdge(Context ctx){		
		if(longerEdge<0){
			synchronized(ImageCache.class){
				if(longerEdge<0){
					WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
					Display display = wm.getDefaultDisplay();
					Point dimensions = new Point();
					display.getSize(dimensions);
					longerEdge = dimensions.x>dimensions.y?dimensions.x:dimensions.y;
				}
			}
		}
		return longerEdge;		
	}
	
}
