package pl.epodreczniki.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class UpdateMarker extends View{
	
private final Paint paint;
	
	private final Path path;
	
	private final float xdpmm;
	
	private final float ydpmm;
	
	private static final int MILIMETERS = 3;

	public UpdateMarker(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public UpdateMarker(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UpdateMarker(Context context) {
		super(context);
	}
	
	{		
		paint = new Paint();
		paint.setColor(Color.RED);
		path = new Path();
		final DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
		xdpmm = dm.xdpi/25.4f;
		ydpmm = dm.ydpi/25.4f;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		path.reset();
		path.moveTo(0, 0);
		path.lineTo(getMeasuredWidth(), 0);
		path.lineTo(0, getMeasuredHeight());
		path.lineTo(0, 0);
		canvas.drawPath(path, paint);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {			
		setMeasuredDimension((int)(MILIMETERS*xdpmm), (int)(MILIMETERS*ydpmm));
	}
	
	
}
