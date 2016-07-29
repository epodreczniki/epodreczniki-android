package pl.epodreczniki.util;

import java.lang.ref.WeakReference;

import android.content.Context;

public abstract class ContextAwareRunnable implements Runnable{	
	
	protected final WeakReference<Context> ctxRef;
	
	public ContextAwareRunnable(Context ctx){
		ctxRef = new WeakReference<Context>(ctx.getApplicationContext());
	}
	
	protected abstract void doWork(Context ctx);
	
	@Override
	public void run(){
		Context ctx = ctxRef.get();
		if(ctx != null){
			doWork(ctx);
		}
	}
	
}
