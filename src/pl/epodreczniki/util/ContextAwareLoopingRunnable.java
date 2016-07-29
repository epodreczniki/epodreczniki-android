package pl.epodreczniki.util;

import android.content.Context;

public abstract class ContextAwareLoopingRunnable extends ContextAwareRunnable{
		
	public ContextAwareLoopingRunnable(Context ctx) {
		super(ctx);
	}

	protected abstract long getPollInterval();

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			final Context ctx = ctxRef.get();
			if (ctx != null) {
				doWork(ctx);
			}else{
				Thread.currentThread().interrupt();
			}
			try {
				Thread.sleep(getPollInterval());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}			
	}		

}
