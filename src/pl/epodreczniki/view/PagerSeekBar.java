package pl.epodreczniki.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class PagerSeekBar extends View{	

	private int max=1;
	
	private int progress=1;
	
	private Paint paint;
	
	private float thumbHeight;
	
	private static final int THUMB_COLOR = 0xFF33B5E5;
	
	private static final int BAR_COLOR = 0x8833B5E5;
	
	public PagerSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context,attrs,defStyleAttr);
	}
	
	public PagerSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);	
	}
	
	public PagerSeekBar(Context context){
		super(context);
	}
	
	{
		paint = new Paint();
		paint.setStyle(Style.FILL);
		final DisplayMetrics dm = getResources().getDisplayMetrics();
		thumbHeight = 0.3f*dm.ydpi/25.4f;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {	
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(BAR_COLOR);
		canvas.drawRect(0, getMeasuredHeight()-1, getMeasuredWidth(), getMeasuredHeight(), paint);
		float thumbWidth = (float)getMeasuredWidth()/(float)this.max;
		paint.setColor(THUMB_COLOR);
		canvas.drawRect((progress-1)*thumbWidth, 0, progress*thumbWidth, thumbHeight, paint);		
	}
	
	public int getMax(){
		return max;
	}
	
	public void setMax(int max){
		this.max = Math.max(1, max);			
		invalidate();
	}
	
	public int getProgress(){
		return progress;		
	}
	
	public void setProgress(int progress){
		this.progress = Math.max(1, progress);
		invalidate();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {		
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.max(1, (int)thumbHeight), MeasureSpec.EXACTLY));
	}

}
