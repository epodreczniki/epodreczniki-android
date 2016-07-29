package pl.epodreczniki.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class ScanOverlay extends View{

	private Paint paint;
	
	private Rect bRect;
	
	private final PorterDuffXfermode CLEAR;
	
	public ScanOverlay(Context context, AttributeSet attrs){
		super(context,attrs);
	}
	
	public ScanOverlay(Context context){
		super(context);
	}
	
	{
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.RED);
		paint.setStrokeWidth(2);
		CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
		bRect = new Rect();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		paint.setXfermode(null);
		canvas.drawARGB(160, 0, 0, 0);		
		paint.setStyle(Style.STROKE);		
		canvas.drawRect(bRect, paint);
		paint.setStyle(Style.FILL);
		paint.setXfermode(CLEAR);
		canvas.drawRect(bRect, paint);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {	
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		float side = Math.min(getMeasuredHeight(), getMeasuredWidth())-48;
		float left = (getMeasuredWidth()-side)/2;
		float top = (getMeasuredHeight()-side)/2;
		bRect.left=(int)left;
		bRect.top=(int)top;
		bRect.right=(int)(left+side);
		bRect.bottom=(int)(top+side);		
	}
	
}
