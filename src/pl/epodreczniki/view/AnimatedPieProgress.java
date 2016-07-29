package pl.epodreczniki.view;

import java.lang.ref.WeakReference;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class AnimatedPieProgress extends SurfaceView implements SurfaceHolder.Callback {

	private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();
	
	private SurfaceHolder holder;

	private Anim anim;

	private int side;

	private int progress = 0;				
	
	private ValueAnimator valueAnimator;
	
	private int thickness;
	
	private int desiredWidth, desiredHeight;
	
	private boolean init = false;

	{
		holder = getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);
		setZOrderOnTop(true);
		holder.addCallback(this);		
		final DisplayMetrics dm = getResources().getDisplayMetrics();
		thickness = Math.round((dm.xdpi/25.4f)/2);
		desiredWidth = Math.round((dm.xdpi/25.4f)*8);
		desiredHeight = Math.round((dm.ydpi/25.4f)*8);
	}

	public AnimatedPieProgress(Context context) {
		super(context);
	}

	public AnimatedPieProgress(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public synchronized int getProgress() {
		return progress;
	}

	private synchronized void setProg(int progress) {
		this.progress = progress < 0 ? 0 : (progress > 100 ? 100 : progress);
	}
	
	public synchronized void setProgress(int progress) {
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
						setProg((Integer)animation.getAnimatedValue());
					}
					
				});
			}
			valueAnimator.start();	
		}		
	}

	SurfaceHolder getHldr() {
		return holder;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		progress = progress == 100 ? 0 : progress + 1;
		return super.onTouchEvent(event);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (anim == null) {
			anim = new Anim(this, new RectF(thickness, thickness, side - thickness, side - thickness));
			anim.start();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (anim != null) {
			anim.interrupt();
			anim = null;
		}
		if (valueAnimator != null){
			valueAnimator.cancel();
			valueAnimator = null;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int wSize = MeasureSpec.getSize(widthMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		int hSize = MeasureSpec.getSize(heightMeasureSpec);
		int w = desiredWidth, h = desiredHeight;
		switch (wMode) {
		case MeasureSpec.EXACTLY:
			w = wSize;
			break;
		case MeasureSpec.AT_MOST:
			w = Math.min(wSize, w);
			break;
		}
		switch (hMode) {
		case MeasureSpec.EXACTLY:
			h = hSize;
			break;
		case MeasureSpec.AT_MOST:
			h = Math.min(hSize, h);
			break;
		}
		w = resolveSizeAndState(w, widthMeasureSpec, 0);
		h = resolveSizeAndState(h, heightMeasureSpec, 0);
		side = Math.min(w, h);
		setMeasuredDimension(w, h);
	}

	static class Anim extends Thread {

		private WeakReference<AnimatedPieProgress> viewRef;

		private Paint paint;

		private RectF oval;

		private int spinCnt = 0;

		Anim(AnimatedPieProgress view, RectF oval) {
			viewRef = new WeakReference<AnimatedPieProgress>(view);
			paint = new Paint();			
			paint.setStyle(Style.FILL);
			paint.setAntiAlias(true);
			this.oval = oval;
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				final AnimatedPieProgress pp = viewRef.get();
				final SurfaceHolder hldr = pp.getHldr();
				if (hldr != null) {
					Canvas c = null;
					try {
						c = hldr.lockCanvas();
						synchronized (hldr) {
							if (c != null) {
								paint.setColor(0xFF33B5E5);
								paint.setStyle(Style.FILL);
								c.drawColor(Color.TRANSPARENT,
										android.graphics.PorterDuff.Mode.CLEAR);
								c.drawArc(oval, -90,
										(float) (3.6 * pp.getProgress()), true,
										paint);
								paint.setColor(Color.BLACK);
								paint.setStyle(Style.STROKE);
								paint.setStrokeWidth(oval.left-1);
								c.drawArc(oval, 0, 360, false, paint);
								paint.setStrokeWidth(oval.left-1);
								for (int i = 0; i < 96; i++) {
									paint.setColor(Color.rgb(2*i, 2*i, 2*i));
									c.drawArc(oval, 4 * spinCnt + i, 1, false,
											paint);
								}
								spinCnt = spinCnt == 89 ? 0 : spinCnt + 1;
							}
						}
					} finally {
						if (c != null) {
							hldr.unlockCanvasAndPost(c);
						}
					}
				} else {
					Thread.currentThread().interrupt();
				}
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

	}

}
