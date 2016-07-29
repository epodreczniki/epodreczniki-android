package pl.epodreczniki.view;

import java.lang.ref.WeakReference;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class CircularProgressBar extends View{
	
	private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();
	
	private int progress = 0;
	
	private int desiredWidth,desiredHeight;
	
	private int side;
	
	private int thickness;
	
	private volatile int spinCnt=0;
	
	private Paint paint;
	
	private RectF oval;
	
	private Anim anim;
	
	private ValueAnimator valueAnimator;
	
	private boolean init = false;
	
	private boolean mIndeterminate = false;

	public CircularProgressBar(Context context){
		super(context);
	}
	
	public CircularProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	{
		paint = new Paint();
		paint.setAntiAlias(true);
		oval = new RectF();
		thickness = Math.round((getResources().getDisplayMetrics().xdpi/25.4f)/2);
		desiredWidth = Math.round((getResources().getDisplayMetrics().xdpi/25.4f)*8);
		desiredHeight = Math.round((getResources().getDisplayMetrics().ydpi/25.4f)*8);
	}
	
	public void setIndeterminate(boolean indeterminate){
		mIndeterminate = indeterminate;
	}
	
	public synchronized int getProgress(){
		return progress;
	}
	
	public synchronized void setProg(int progress){
		this.progress = progress>100?100:progress<0?0:progress;
	}
	
	public synchronized void setProgress(int progress){
		if(!init){
			setProg(progress);
			init = true;
		}else{
			if(valueAnimator!=null){
				valueAnimator.cancel();
				valueAnimator.setIntValues(getProgress(),progress);
			}else{
				valueAnimator = ValueAnimator.ofInt(getProgress(),progress);
				valueAnimator.setInterpolator(DEFAULT_INTERPOLATOR);
				valueAnimator.addUpdateListener(new AnimatorUpdateListener(){

					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						setProg((Integer) animation.getAnimatedValue());
					}
					
				});
			}
			valueAnimator.start();
		}		
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {	
		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int wSize = MeasureSpec.getSize(widthMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		int hSize = MeasureSpec.getSize(heightMeasureSpec);	
		int w=desiredWidth,h=desiredHeight;
		switch(wMode){
			case MeasureSpec.EXACTLY:
				w = wSize;
				break;
			case MeasureSpec.AT_MOST:
				w = Math.min(wSize, w);
				break;							
			}
		switch(hMode){
			case MeasureSpec.EXACTLY:
				h = hSize;
				break;
			case MeasureSpec.AT_MOST:
				h = Math.min(hSize, h);
				break;
		}
		w = resolveSizeAndState(w, widthMeasureSpec, 0);
		h = resolveSizeAndState(h, heightMeasureSpec, 0);
		side = Math.min(w,h);
				
		oval.left=0.08f*side+(w-side)/2;
		oval.top=0.08f*side+(h-side)/2;
		oval.right=side-0.08f*side+(w-side)/2;
		oval.bottom=side-0.08f*side+(h-side)/2;
		setMeasuredDimension(w, h);
	}
	
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.DST_OVER);
		paint.setColor(Color.TRANSPARENT);		
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setStrokeWidth(thickness);		
		canvas.drawArc(oval, 0, 360, true, paint);				
		paint.setStyle(Style.STROKE);
		paint.setColor(0xffe1e1e1);
		canvas.drawArc(oval, 0, 360, true, paint);
		paint.setColor(0x8833B5E5);		
		if(mIndeterminate){										
			canvas.drawArc(oval, spinCnt*4.5f, 45f, false, paint);
		}else{			
			canvas.drawArc(oval, -90 , (float)(3.6*progress), false, paint);		
		}		
	};
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(anim == null){
			anim = new Anim(this);
			anim.start();
		}
	};
	
	@Override
	protected void onDetachedFromWindow() {	
		super.onDetachedFromWindow();
		if(anim!=null){
			anim.interrupt();
			anim = null;
		}
		if(valueAnimator!=null){
			valueAnimator.cancel();
			valueAnimator = null;
		}
	}	
	
	static class Anim extends Thread{
		
		private final WeakReference<CircularProgressBar> viewRef;				
		
		Anim(CircularProgressBar view){
			viewRef = new WeakReference<CircularProgressBar>(view);
		}
		
		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted()){
				final CircularProgressBar npp = viewRef.get();
				if(npp!=null){
					npp.spinCnt = npp.spinCnt==79?0:npp.spinCnt+1;
					npp.postInvalidate();
				}else{
					Thread.currentThread().interrupt();
				}
				try{
					Thread.sleep(30);
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
				}
			}
		}
		
	}

}
