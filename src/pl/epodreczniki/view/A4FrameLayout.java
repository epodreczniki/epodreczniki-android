package pl.epodreczniki.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class A4FrameLayout extends FrameLayout{

	public A4FrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public A4FrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public A4FrameLayout(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {	
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension((int)(getMeasuredHeight()*0.7), getMeasuredHeight());
	}	
	
}
