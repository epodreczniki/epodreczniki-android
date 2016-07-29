package pl.epodreczniki.view;

import pl.epodreczniki.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class FixedAspectRatioFrameLayout extends FrameLayout{

	private float ratio;
	
	public FixedAspectRatioFrameLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FixedAspectRatio, defStyle, 0);
		ratio = a.getFloat(R.styleable.FixedAspectRatio_ratio,1f);
		a.recycle();
	}

	public FixedAspectRatioFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.FixedAspectRatio);
		ratio = a.getFloat(R.styleable.FixedAspectRatio_ratio, 1f);
		a.recycle();
	}

	public FixedAspectRatioFrameLayout(Context context) {
		super(context);
		ratio = 1f;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {					
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec((int)(ratio*MeasureSpec.getSize(widthMeasureSpec)), MeasureSpec.EXACTLY));						
	}		
	
}
