package pl.epodreczniki.util;

import android.graphics.BitmapFactory;

public class ImageUtil {

	private ImageUtil(){		
	}
	
	public static int calculateInSampleSizeWithRequiredHeight(BitmapFactory.Options options, int reqHeight){
		final int height = options.outHeight;
		int inSampleSize = 1;
		if(height>reqHeight){
			final int halfHeight = height/2;
			while(halfHeight/inSampleSize>reqHeight){
				inSampleSize*=2;
			}
		}
		return inSampleSize;
	}
	
}
