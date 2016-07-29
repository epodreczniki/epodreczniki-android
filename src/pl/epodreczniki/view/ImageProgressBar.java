package pl.epodreczniki.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageProgressBar extends ImageView{
	
	private final Paint paint;
	
	private final Rect rect;
	
	private int progress;
	
	public ImageProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ImageProgressBar(Context context){
		super(context);
	}
	
	{
		paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(0xC1000000);
		rect = new Rect();
		rect.left = 0;
		rect.top = 0;		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {	
		super.onDraw(canvas);
		rect.right = getMeasuredWidth();
		rect.bottom = (int)((100-progress)/100f*getMeasuredHeight());
		canvas.drawRect(rect, paint);
	}	
	
	public void setProgress(int progress){
		this.progress = progress;
		invalidate();
	}
	
}
