package pl.epodreczniki.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DisableableViewPager extends ViewPager {

	private volatile boolean enabled;

	public DisableableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		enabled = true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		return enabled ? super.onTouchEvent(arg0) : false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		return enabled ? super.onInterceptTouchEvent(arg0) : false;
	}

	@Override
	protected boolean canScroll(View arg0, boolean arg1, int dx, int x, int y) {		
		int width = getWidth();
		if(x < 8 || x > width-8){
			return false;
		}		
		return true;
	}

	public synchronized void setPagingEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
