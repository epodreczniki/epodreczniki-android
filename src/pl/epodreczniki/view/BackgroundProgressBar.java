package pl.epodreczniki.view;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class BackgroundProgressBar extends View{

	private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();
	
	public static final int MIN = 0;
	
	public static final int MAX = 100;
	
	private int progress = 0;
	
	private int extProgress = 0;
	
	private final Paint paint;	
	
	private ValueAnimator valueAnimator;
	
	private ValueAnimator extAnimator;
	
	private float barHeight;
	
	private boolean init = false;
	
	public BackgroundProgressBar(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);		
	}

	public BackgroundProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}	
	
	{
		paint = new Paint();		
		paint.setColor(0xFF33B5E5);
		barHeight = (getContext().getResources().getDisplayMetrics().ydpi/25.4f)/2;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		paint.setColor(0xfff3f3f3);
		canvas.drawRect(0,0,getMeasuredWidth(),getMeasuredHeight(), paint);
		if(extProgress>0){
			final int width = getMeasuredWidth()*extProgress/100;					
			canvas.drawRect(0,getMeasuredHeight()-barHeight,width,getMeasuredHeight(), paint);		
			paint.setColor(0xFF33B5E5);
			canvas.drawRect(width,getMeasuredHeight()-barHeight,getMeasuredWidth(),getMeasuredHeight(), paint);
		}else{
			final int width = getMeasuredWidth()*progress/100;
			paint.setColor(0xFF33B5E5);
			canvas.drawRect(0,getMeasuredHeight()-barHeight,width,getMeasuredHeight(), paint);	
		}		
	}	
	
	public void setProgress(int progress){					
		if(!init){
			setProg(progress);
			init=true;
		}else{
			if(valueAnimator!=null){
				valueAnimator.cancel();
				valueAnimator.setIntValues(this.progress,progress);
			}else{
				valueAnimator = ValueAnimator.ofInt(this.progress,progress);
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
	
	public void setExtractProgress(int progress){
		if(extProgress==0){
			setExtractProg(progress);
		}else{
			if(extAnimator!=null){
				extAnimator.cancel();
				extAnimator.setIntValues(this.extProgress,progress);
			}else{
				extAnimator = ValueAnimator.ofInt(this.extProgress,progress);
				extAnimator.setInterpolator(DEFAULT_INTERPOLATOR);
				extAnimator.addUpdateListener(new AnimatorUpdateListener(){

					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						setExtractProg((Integer) animation.getAnimatedValue());
					}
					
				});
			}
			extAnimator.start();
		}
	}
	
	public void reset(){
		this.progress=0;
		this.extProgress=0;
		invalidate();
	}
	
	private void setProg(int progress){
		this.progress = progress>MAX?MAX:(progress<MIN?MIN:progress);
		invalidate();
	}
	
	private void setExtractProg(int extProgress){
		this.extProgress = extProgress>MAX?MAX:(extProgress<MIN?MIN:extProgress);
		invalidate();
	}
	
	@Override
	protected void onDetachedFromWindow() {	
		super.onDetachedFromWindow();
		if(valueAnimator!=null){
			valueAnimator.cancel();
			valueAnimator = null;
		}
	}

}
